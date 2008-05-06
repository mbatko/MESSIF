/*
 * Host.java
 *
 * Created on August 3, 2006, 12:51
 */

package messif.loadbalancing;

import java.util.concurrent.atomic.AtomicInteger;
import messif.algorithms.Algorithm;
import messif.algorithms.AlgorithmMethodException;
import messif.algorithms.DistAlgReplyMessage;
import messif.algorithms.DistAlgRequestMessage;
import messif.algorithms.DistributedAlgorithm;
import messif.buckets.CapacityFullException;
import messif.buckets.MemoryStorageBucketNoDups;
import messif.loadbalancing.HostList.HostLoad;
import messif.loadbalancing.replication.Replica;
import messif.loadbalancing.replication.ReplicateOperation;
import messif.loadbalancing.replication.UnifyOperation;
import messif.netcreator.CantStartException;
import messif.network.InvokingReceiver;
import messif.network.Message;
import messif.network.MessageDispatcher;
import messif.network.NetworkNode;
import messif.network.ReplyMessage;
import messif.operations.AbstractOperation;
import messif.statistics.StatisticSlidingSimpleRefCounter;
import messif.utility.Logger;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import messif.statistics.Statistics;

/**
 * This is class for a physical Host which contains several logical "nodes".
 *
 * @author xnovak8@fi David Novak
 */
public class Host extends DistributedAlgorithm implements Serializable {
    /** class id for serialization */
    private static final long serialVersionUID = 9L;
    
    /** Logger */
    protected static Logger log = Logger.getLoggerEx("Peer");
    
    /** Receiver - invoke "receive" */
    protected transient InvokingReceiver receiver = null;
    
    /** The gossip module */
    protected final GossipModule gossipModule;
    
    /** The load balancing module contains the balancing rules themselves */
    protected final BalancingModule balancingModule;
    
    /** Returns the messageDispatcher to all classes in the package */
    protected MessageDispatcher getMessageDisp() { return messageDisp; }
    
    /****************** Constructors ******************/
    
    /** Creates a new instance of Host given a port and broadcast-port */
    @Algorithm.AlgorithmConstructor(description = "Load-balancing host (with name)", arguments = {"Host name", "port", "broadcast port", "class of this layer", "parameter for node constructor"})
    public Host(String hostName, int port, int broadcastPort, Class<? extends Algorithm> nodeClass, String parameter) throws InstantiationException {
        super(hostName, port, broadcastPort);
        
        // Load-balancing settings
        LBSettings.loadSettings();
        
        // statistics
        initStatistics();
        
        gossipModule = new GossipModule(this);
        balancingModule = new BalancingModule(this, gossipModule, messageDisp);
        try {
            start(); // start receiving messages
        } catch (CantStartException ex) {
            log.severe(ex);
            throw new InstantiationException(ex.getMessage());
        } // start receiving messages
        
        // create the very first node
        if (nodeClass != null) {
            try {
                Constructor<? extends Algorithm> constr = nodeClass.getConstructor(Host.class, String.class);
                constr.newInstance(this, parameter);
            } catch (SecurityException ex) {
                log.severe(ex); throw new InstantiationException(ex.getMessage());
            } catch (IllegalArgumentException ex) {
                log.severe(ex); throw new InstantiationException(ex.getMessage());
            } catch (InstantiationException ex) {
                log.severe(ex); throw new InstantiationException(ex.getMessage());
            } catch (IllegalAccessException ex) {
                log.severe(ex); throw new InstantiationException(ex.getMessage());
            } catch (InvocationTargetException ex) {
                log.severe(ex); throw new InstantiationException(ex.getMessage());
            } catch (NoSuchMethodException ex) {
                log.severe(ex); throw new InstantiationException(ex.getMessage());
            }
        }
    }
    
    /** Store the existing host info */
    private NetworkNode existingHost = null;
    
    /** Creates a new instance of Host given a port and broadcast port and existing host */
    @Algorithm.AlgorithmConstructor(description = "Load-balancing host", arguments = {"host name", "port", "broadcast port", "existing host"})
    public Host(String hostName, int port, int broadcastPort, NetworkNode existingHost) throws InstantiationException {
        this(hostName, port, broadcastPort, existingHost, null, null);
    }
    /** Creates a new instance of Host given a port and broadcast port and existing host - create the node of given class - works good only for MChord at the moment */
    @Algorithm.AlgorithmConstructor(description = "Load-balancing host", arguments = {"host name", "port", "broadcast port", "existing host", "class of this layer", "parameter for node constructor"})
    public Host(String hostName, int port, int broadcastPort, NetworkNode existingHost, Class<? extends Algorithm> nodeClass, Boolean parameter) throws InstantiationException {
        this(hostName, port, broadcastPort, null, null);
        this.existingHost = existingHost;
        try {
            startBalancing();
            // Notify the known host about myself
            HostLoad myHostLoad = new HostLoad(this);
            DistAlgReplyMessage reply = (DistAlgReplyMessage) messageDisp.sendMessageWaitReply(new DistAlgRequestMessage(new NotifyOperation(myHostLoad)), existingHost).getFirstReply();
            if (!( (NotifyOperation)reply.getOperation()).loadBalancing)
                stopBalancing(true);
            log.info("After NotifyOperation:\n"+this.toString());
        } catch (IOException ex) {
            log.severe(ex);
            throw new InstantiationException(ex.getMessage());
        }
        
        // create a node immediatelly - this trick work only for one special constructor of MChord
        if (nodeClass != null) {
            try {
                Constructor<? extends Algorithm> constr = nodeClass.getConstructor(Host.class, NetworkNode.class, Boolean.class);
                constr.newInstance(this, existingHost, parameter);
            } catch (SecurityException ex) {
                log.severe(ex); throw new InstantiationException(ex.getMessage());
            } catch (IllegalArgumentException ex) {
                log.severe(ex); throw new InstantiationException(ex.getMessage());
            } catch (InstantiationException ex) {
                log.severe(ex); throw new InstantiationException(ex.getMessage());
            } catch (IllegalAccessException ex) {
                log.severe(ex); throw new InstantiationException(ex.getMessage());
            } catch (InvocationTargetException ex) {
                log.severe(ex); throw new InstantiationException(ex.getMessage());
            } catch (NoSuchMethodException ex) {
                log.severe(ex); throw new InstantiationException(ex.getMessage());
            }
        }
    }
    
    /** Public destructor to stop the algorithm.
     *  This should be overriden in order to clean up.
     */
    public void finalize() throws Throwable {
        Statistics.removeStatistic(busyLoad.getName());
        Statistics.removeStatistic(singleLoad.getName());
        super.finalize();
    }
    
    /** Start method - register the receiver here */
    public void start() throws CantStartException {
        try {
            receiver = new HostReceiver(this, "receive");
            messageDisp.registerReceiver(receiver);
            
            // start all the underlying nodes as well - they do not receive the activation message
            for (DistributedAlgorithm node : nodes.values())
                node.start();
            timerPrint.schedule(new TimerTaskPrint(), 1000, LBSettings.BALANCING_DELTA_T);
        } catch (InstantiationException ex) {
            throw new CantStartException(ex.getMessage());
        }
    }
    
    /** Create new Host from String - "name,port,broadcast_port" */
    public static Host valueOf(String str) {
        String[] data = str.split(","); // <name,port,broadcast_port>
        if (data.length != 3)
            return null;
        try {
            return new Host(data[0], Integer.valueOf(data[1]), Integer.valueOf(data[2]), null, null);
        } catch (NumberFormatException ex) {
            log.severe(ex);
            return null;
        } catch (InstantiationException ex) {
            log.severe(ex);
            return null;
        }
    }
    
    /**************************** Overrides **********************************/
    
    /**
     * This method creates the message dispatcher and sets the <code>meesageDisp</code> field.
     * @return the created message dispatcher
     */
    protected MessageDispatcher createMessageDispatcher(int port, int broadcastPort) throws IOException {
        return messageDisp = new GossipMessageDispatcher(this, port, broadcastPort);
    }
    
    /********************** (De)serialization ************************************/
    
    /** Serialization - stop the balancing before and start again after the serialization */
    private void writeObject(ObjectOutputStream out) throws IOException {
        boolean start = stopBalancing();
        out.defaultWriteObject();
        
        out.writeObject(nextNodeID);
        
        if (start)
            startBalancing();
    }
    
    /** Deserialization method - create the message dispatcher from the restored values "port", "broadcastport"
     * (for the top-most node). Create the activation receiver to wait for "start" message.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // Load-balancing settings
        if (! LBSettings.loadSettings())
            log.warning("Error reading the LB settings file");
        log.info("Load window: "+LBSettings.BUSY_LOAD_WINDOW);
        
        in.defaultReadObject();
        
        busyLoad.setWindowSizeMilis(LBSettings.BUSY_LOAD_WINDOW);
        singleLoad.setMaxNumberOfValues(LBSettings.SINGLE_LOAD_AVG);
        
        balancingModule.messageDisp = messageDisp;
        
        nextNodeID = (AtomicInteger) in.readObject();
        
        loadBalancing = false;
        waitingForHost = null;
        
        timerPrint = new Timer();
        deletedDispatchers = Collections.synchronizedMap(new HashMap<NetworkNode, MessageDispatcher>());
        undeliveredMessages = new HashMap<NetworkNode, List<Message>>();
    }
    
    /********************** Load-balancing statistics management *******************/
    
    /** Busy statistic of this peer */
    protected StatisticSlidingSimpleRefCounter busyLoad;
    /** returns the actual value of this hosts' waiting costs.
     *   if not enough time cycles then "Dont_know", */
    public double getBusyLoad() {
        if (waitingForHost != null)
            return LBSettings.LOAD_DONT_KNOW;
        if (nodes.isEmpty())
            return 0;
        if (! busyLoad.checkUsedTime())
            return LBSettings.LOAD_DONT_KNOW;
        return busyLoad.getSum();
    }
    
    /** Single load of the whole peer. */
    protected StatisticSlidingSimpleRefCounter singleLoad;
    /** Single load of the whole peer. */
    public double getSingleLoad() {
        if (waitingForHost != null)
            return LBSettings.LOAD_DONT_KNOW;
        if (nodes.isEmpty())
            return 0;
        if (singleLoad.getCnt() < singleLoad.getMaxNumberOfValues())
            return LBSettings.LOAD_DONT_KNOW;
        return singleLoad.getAvg();
    }
    
    /** Return the sum of the storage load of all nodes at this host */
    public double getDataLoad() {
        long storage = 0;
        for (BalancedDistributedAlgorithm node : nodes.values())
            storage += node.getDataLoad();
        return storage;
    }
    
    /** Init the statistics. Called from constructor & deserialization. */
    public void initStatistics() {
        busyLoad = StatisticSlidingSimpleRefCounter.getStatistics("busyLoad_"+messageDisp.getNetworkNode());
        busyLoad.setWindowSizeMilis(LBSettings.BUSY_LOAD_WINDOW);
        singleLoad = StatisticSlidingSimpleRefCounter.getStatistics("singleLoad_"+messageDisp.getNetworkNode());
        singleLoad.setMaxNumberOfValues(LBSettings.SINGLE_LOAD_AVG);
        
        // "bind" the statistics of the nodes to the peer statistics (no nodes should be registered, but who knows...)
        for (BalancedDistributedAlgorithm node : nodes.values()) {
            busyLoad.multiBindTo(node.busyLoad);
            singleLoad.multiBindTo(node.singleLoad);
        }
    }
    
    /** Clear all statistics including the averages obtained by gossiping */
    public void resetStatistics() {
        for (BalancedDistributedAlgorithm subnode : nodes.values())
            subnode.resetStatistics();
        busyLoad.reset();
        singleLoad.reset();
        gossipModule.clearGossiping();
        waitingForHost = null;
    }
    
    /********************** Starting and stopping of load-balancing *****************/
    
    /** this flag is <b>true</b> iff the load-balancing process is on. Default: off */
    protected transient boolean loadBalancing = false;
    
    /** This field is set to the host that informed us about the fact it will use me for load balancing */
    protected transient NetworkNode waitingForHost = null;
    /** This method checks whether we are waiting for this host (returns false if not) and sets the waitingForHost to null */
    protected boolean checkWaitingForHost(NetworkNode node) {
        if (! node.equals(waitingForHost)) {
            log.warning("Host "+node+" tries to use me for balancing without asking me before");
            return false;
        }
        return true;
    }
    
    /** set the load balancing flag to false, store the actual timestamp in order to recover
     * in a correct way from balancing pause
     * @return boolean whether balancing was really stopped (only if it was running)
     */
    public synchronized boolean stopBalancing() {
        return stopBalancing(false);
    }
    public synchronized boolean stopBalancing(boolean clearStatistics) {
        boolean retVal = loadBalancing;
        if (retVal) {
            timerLoadBalancing.cancel();
            timerLoadBalancing.purge();
            
            gossipModule.stopGossiping();
            loadBalancing = false;
            log.info("Stopping load balancing at host: "+getThisNode());
        }
        if (clearStatistics)
            resetStatistics();
        return retVal;
    }
    
    /** recover from the balancing pause - pretend that the balancing has never been paused */
    public synchronized void startBalancing() {
        startBalancing(false);
    }
    public synchronized void startBalancing(boolean clearStatistics) {
        if (! loadBalancing) {
            timerLoadBalancing = new Timer();
            timerLoadBalancing.schedule(new TimerTaskBalancing(), LBSettings.BALANCING_DELTA_T, LBSettings.BALANCING_DELTA_T);
            gossipModule.startGossiping();
            loadBalancing = true;
            log.info("Starting load balancing at host: "+getThisNode());
        }
        if (clearStatistics)
            resetStatistics();
        log.info("Gossip module: "+gossipModule);
    }
    
    /** Message to start/stop the load-balancing process */
    public final void receive(MessageStartStopBalancing msg) {
        if (msg.start)
            startBalancing();
        else stopBalancing();
        
        if (msg.clearStatistics)
            resetStatistics();
    }
    
    /** Broadcast a MessageStartStopBalancing message to stop/start the balancing process in whole network
     * @param start if true then start, otherwise stop the balancing process
     */
    @Deprecated
    public void startStopBalancingNetwork(boolean start, boolean clearStatistics) throws AlgorithmMethodException {
        try {
            log.info("Broadcasting load-balancing start/stop msg");
            messageDisp.sendMessage(new MessageStartStopBalancing(start, clearStatistics));
        } catch (IOException e) {
            log.severe(e);
            throw new AlgorithmMethodException(e.getMessage());
        }
    }
    /** Wrapper with clearStatistics = false */
    public void startStopBalancingNetwork(boolean start) throws AlgorithmMethodException {
        startStopBalancingNetwork(start, false);
    }
    
    /*******************************   Balancing    *************************************/
    
    /** Periodically do the load balancing action */
    private transient Timer timerLoadBalancing;
    /** TimerTask class to Periodically do the load balancing action */
    private class TimerTaskBalancing extends TimerTask {
        protected TimerTaskBalancing() { }
        public void run() {
            // DO the load balancing now - run it in a separate thread in order to finish this "run" immediately
            new Thread() {
                public void run() {
                    if (! balancingModule.loadBalancing())
                        log.info("No balancing action done");
                }
            }.start();
            
            // if I'm an empty peer, then periodically let somebody know that I'm here
            if (nodes.isEmpty() && (existingHost != null)) {
                try {
                    gossipModule.unloadedPeers.clear();
                    gossipModule.loadedPeers.clear();
                    messageDisp.sendMessageWaitReply(new DistAlgRequestMessage(new NotifyOperation(new HostLoad(Host.this))), existingHost);
                } catch (IOException ex) {
                    log.severe(ex);
                }
            }
        }
    }
    
    /****************** Management of the logical nodes ****************/
    
    /** List of ids of the logical nodes */
    protected Set<Integer> nodeIds = new HashSet<Integer>();
    
    /** Counter of unique ids of the logical nodes */
    private static AtomicInteger nextNodeID = new AtomicInteger(0);
    /** Returns a unique ID for a logical node */
    public static int getUniqueID() {
        return nextNodeID.addAndGet(1);
    }
    
    /** Set of nodes (distributed algorithms) identified by integer ID */
    protected Map<NetworkNode, BalancedDistributedAlgorithm> nodes = new HashMap<NetworkNode, BalancedDistributedAlgorithm>();
    
    /** Get iterator over the subnodes */
    public Iterator<BalancedDistributedAlgorithm> getNodes() { return nodes.values().iterator(); }
    
    /** add a logical node */
    protected void addNode(BalancedDistributedAlgorithm node) {
        synchronized (nodes) {
            nodes.put(node.getThisNode(), node);
            
            // "bind" the statistics of the node to peer statistics
            busyLoad.multiBindTo(node.busyLoad);
            singleLoad.multiBindTo(node.singleLoad);
            
            Integer id = node.getThisNode().getNodeID();
            if (id == null) {
                log.severe("Adding a node which hasn't got any nodeID");
                return;
            }
            nodeIds.add(id);
        }
    }
    
    /**
     * Prepare the node removal - stop receiving all non-reply messages to the node.
     */
    protected void preRemoveNode(BalancedDistributedAlgorithm node) {
        deletedDispatchers.put(node.getThisNode(), node.getMessageDisp());
        nodeIds.remove(node.getThisNode().getNodeID());
    }
    
    /**
     * Node removal did not succeed - receive the messages again.
     */
    protected void undoRemoveNode(BalancedDistributedAlgorithm node) {
        nodeIds.add(node.getThisNode().getNodeID());
        deletedDispatchers.remove(node.getThisNode());
    }
    
    /**
     * Remove the node from the hierarchy - from the subnodes list and from the message dispatcher as well
     */
    protected void removeNode(BalancedDistributedAlgorithm node) {
        synchronized (nodes) {
            // "unbind" the statistics of the node to peer statistics
            busyLoad.multiUnbindFrom(node.busyLoad);
            singleLoad.multiUnbindFrom(node.singleLoad);
            
            nodeIds.remove(node.getThisNode().getNodeID());
            nodes.remove(node.getThisNode());
            
            try {
                node.finalize();
            } catch (Throwable ex) {
                log.severe(ex);
            }
        }
    }
    
    /** Once a node was migrated or deleted, this method either forward the message or returns
     *   the "NodeDoesNotExist" message to the sender */
    public void nodeDoesNotExist(NetworkNode destination, Message msg) {
        Integer id = destination.getNodeID();
        try {
            synchronized (migratedNodes) {
                if (nodeIds.contains(id))
                    log.warning("Method nodeDoesNotExist should be called only if the node was migrated or left the system");
                if (migratedNodes.containsKey(destination)) {
                    if  (migratedNodes.get(destination) != null) {
                        log.info("Forwarding request to a migrated node ("+destination+" -> "+migratedNodes.get(destination));
                        deletedDispatchers.get(destination).sendMessage(msg, migratedNodes.get(destination), true);
                    } else {
                        if (! undeliveredMessages.containsKey(destination))
                            undeliveredMessages.put(destination, new ArrayList<Message>());
                        undeliveredMessages.get(destination).add(msg);
                    }
                } else {
                    // return message saying that target node does not exist at this host
                    NodeDoesntExistMessage reply = new NodeDoesntExistMessage(msg, destination);
                    NetworkNode sender = msg.getSender();
                    if (sender != null) {
                        // find the dispatcher of the deleted or migrated node and send the message from that dispatcher
                        MessageDispatcher disp = deletedDispatchers.get(destination);
                        if (disp == null)
                            disp = getMessageDisp();
                        if (! sender.equals(disp.replyMessage(reply)))
                            disp.sendMessage(reply, sender);
                        log.info("Sending NodeDoesntExistMessage: "+destination+" (msg id: "+reply.getMessageID()+")");
                    }
                }
            }
        } catch (IOException e) {
            log.severe(e);
        } catch (IndexOutOfBoundsException ignore) { }
    }
    
    /** Returns true if the given number is ID of one of the nodes at this host.
     * If such node doesn't exist, this method accepts the message and replies a message
     *   NodeDoesntExistMessage
     */
    public boolean targetNodeExists(Message msg) {
        try {
            NetworkNode actualDest = msg.getDestination();
            if (actualDest != null) {
                Integer id = actualDest.getNodeID();
                if (id == null)
                    return true;
                if (nodeIds.contains(id))
                    return true;
                // check, whether it message is a reply message and to be received by one of the deleted (or migrated) nodes
                if (ReplyMessage.class.isAssignableFrom(msg.getClass())) {
                    MessageDispatcher disp = deletedDispatchers.get(actualDest);
                    if (disp != null) {
                        if (disp.acceptMessage(msg, false))
                            return false;
                        if (disp.acceptMessage(msg, true))
                            return false;
                    }
                }
                nodeDoesNotExist(actualDest, msg);
                return false;
            }
        } catch (IndexOutOfBoundsException ignore) { }
        return true;
    }
    
    /**************************************   Operation handlers   ****************************************/
    
    /** Receving a DistAlgRequestMessage - execute the operaion */
    public final void receive(DistAlgRequestMessage message) {
        try {
            if (message.getOperation() != null)
                receiveRequest(message);
            else {
                /** Empty message - only for gossiping */
                if (log.isLoggable(Level.FINE))
                    log.fine("Received gossip message from "+message.getSender());
                messageDisp.replyMessage(new DistAlgReplyMessage(message));
            }
        } catch (AlgorithmMethodException e) {
            log.severe(e);
        } catch (IOException ex) {
            log.severe(ex);
        }
    }
    
    /** Processes info about new host joining the network */
    public void notify(NotifyOperation operation, DistAlgRequestMessage request) throws AlgorithmMethodException {
        log.info("Processing NotifyOperation from: "+operation.hostLoad);
        if (! loadBalancing)
            gossipModule.addHostToLists(operation.hostLoad);
        operation.loadBalancing = loadBalancing;
        operation.endOperation();
        try {
            messageDisp.replyMessage(new DistAlgReplyMessage(request));
        } catch (IOException ex) {
            log.severe(ex);
            throw new AlgorithmMethodException(ex.getMessage());
        }
    }
    
    /** Process the offer of making a balancing action to the sender */
    public void processBalancingOffer(BalancingOfferOperation operation, DistAlgRequestMessage request) throws AlgorithmMethodException {
        log.info("Processing a balancing offer from peer "+request.getSender());
        try {
            if ((waitingForHost != null) || (getBusyLoad() == LBSettings.LOAD_DONT_KNOW) || (getDataLoad() == 0) ||
                    (! balancingModule.processBalancingOffer(operation, request) && (! operation.wasSuccessful())) ) {
                operation.endOperation(LoadBalancingErrorCode.WONT_BALANCE);
                messageDisp.replyMessage(new DistAlgReplyMessage(request));
            }
        } catch (IOException ex) {
            log.severe(ex);
            throw new AlgorithmMethodException(ex.getMessage());
        }
    }
    
    /** Processes a plea for using this host for load balancing */
    public synchronized void isHostSuitable(SuitableHostOperation operation, DistAlgRequestMessage request) throws AlgorithmMethodException {
        // if this is the empty operation - saying that the announced operation is canceled
        NetworkNode askingPeer = request.getSender();
        if (operation.cancelOperation) {
            if (askingPeer.equals(waitingForHost)) {
                waitingForHost = null;
                log.info("Peer "+askingPeer+" cancels the announced load-balancing operation");
            }
            return;
        }
        double dataLoad = getDataLoad();
        if (waitingForHost != null)
            operation.endOperation(LoadBalancingErrorCode.NOT_SUITABLE);
        else if (dataLoad == 0)
            operation.endOperation();
        else if (operation.checkUnderAvg) {
            if (getBusyLoad() < gossipModule.avgWaitLoadEst())
                operation.endOperation();
            else operation.endOperation(LoadBalancingErrorCode.NOT_SUITABLE);
        } else if (operation.replica != null) {
            BalancedDistributedAlgorithm replica = nodes.get(operation.replica);
            if (replica == null)
                log.warning("Trying to remove a replica that does not exist at this peer: "+operation.replica);
            double busyLoad = getBusyLoad();
            if ((replica == null) || (busyLoad == LBSettings.LOAD_DONT_KNOW) || (busyLoad - replica.getBusyLoad() < 0.5 * gossipModule.avgWaitLoadEst()))
                operation.endOperation(LoadBalancingErrorCode.NOT_SUITABLE);
            else operation.endOperation();
        } else {
            double busyLoad = getBusyLoad();
            //double singleLoad = getSingleLoad();
            if ((busyLoad == LBSettings.LOAD_DONT_KNOW) || (operation.freshPeerRequested) ||
                    (busyLoad + operation.addedBusyLoad > 2 * gossipModule.avgWaitLoadEst()))
                operation.endOperation(LoadBalancingErrorCode.NOT_SUITABLE);
            // if the busy or single load would cause an overload of this peer
            //else if
            //|| ((singleLoad != LBSettings.LOAD_DONT_KNOW) && (singleLoad + operation.addedSingleLoad > 2 * gossipModule.avgProcLoadEst()))
            //)
            //operation.endOperation(LoadBalancingErrorCode.NOT_SUITABLE);
            else operation.endOperation();
        }
        
        if (operation.wasSuccessful()) {
            waitingForHost = askingPeer;
            log.info("Peer "+askingPeer+" will use me for load-balancing: "+toString());
        }
        try {
            messageDisp.replyMessage(new DistAlgReplyMessage(request));
        } catch (IOException ex) {
            log.severe(ex);
            throw new AlgorithmMethodException(ex.getMessage());
        }
    }
    
    /** Create a new logical node using specified constructor and given actual parameters */
    public void createNode(CreateNodeOperation operation, DistAlgRequestMessage request) throws AlgorithmMethodException {
        try {
            BalancedDistributedAlgorithm newNode = null;
            try {
                if (! checkWaitingForHost(request.getSender()))
                    operation.endOperation(LoadBalancingErrorCode.ERROR_NOT_ASKED);
                else {
                    Constructor constructor = operation.nodeClass.getConstructor(operation.constructorTypes);
                    if (operation.constructorParameters.length != operation.constructorTypes.length)
                        throw new AlgorithmMethodException("The actual parameters do not match the constructor types");
                    Object[] actualParams = operation.constructorParameters.clone();
                    for (int i=0; i<operation.constructorTypes.length; i++) {
                        if (operation.constructorTypes[i].equals(Host.class))
                            actualParams[i] = this;
                    }
                    newNode = (BalancedDistributedAlgorithm) constructor.newInstance(actualParams);
                    
                    operation.endOperation();
                    log.info("This node successfully created here\n"+newNode);
                }
            } catch (Throwable ex) {
                log.severe(ex);
                operation.endOperation(LoadBalancingErrorCode.ERROR_NODE_CREATION);
            }
            try {
                messageDisp.replyMessage(new DistAlgReplyMessage(request));
                
                // create the replicas at the peers the original node used to have replicas at
                if ((newNode != null) && (operation.replicationPeers != null) && (! operation.replicationPeers.isEmpty()))
                    replicateAllSilent(newNode, operation.replicationPeers);
            } catch (IOException ex) {
                log.severe(ex);
                throw new AlgorithmMethodException(ex.getMessage());
            }
        } finally {
            balancingAction();
            waitingForHost = null;
        }
    }
    
    /** Process the request for placing a new node at this host */
    public void placeNode(MigrateOperation operation, DistAlgRequestMessage request) throws AlgorithmMethodException {
        try {
            try {
                if (! checkWaitingForHost(request.getSender()))
                    operation.endOperation(LoadBalancingErrorCode.ERROR_NOT_ASKED);
                else {
                    BalancedDistributedAlgorithm node = operation.node;
                    
                    node.host = this;
                    node.createMessageDispatcher(this, getUniqueID());
                    node.start();
                    addNode(node);
                    
                    node.migrate(operation.origId, node.getThisNode());
                    
                    // notify the replicas about the migration
                    node.migrateNotifyReplicas(operation.origId, node.getThisNode());
                    
                    operation.newId = node.getThisNode();
                    operation.endOperation();
                    log.info("This node successfully migrated here\n"+node);
                    // now check, whether some of the nodes already stored at this peer are not to be joined with this node
                    if (getThisNode().equalsIgnoreNodeID(node.getNodeToMerge()) && (! node.getThisNode().equals(node.getNodeToMerge()))) {
                        // then merge the two nodes
                        log.info("Two nodes to be merged appeared at one peer");
                        BalancedDistributedAlgorithm leaveNode = nodes.get(node.getNodeToMerge());
                        if (leaveNode != null)
                            leave(leaveNode);
                    }
                }
            } catch (CantStartException ex) {
                log.severe(ex);
                operation.endOperation(LoadBalancingErrorCode.ERROR_NODE_MIGRATION);
            }
            
            try {
                operation.node = null; // do not send the node back to the original host
                messageDisp.replyMessage(new DistAlgReplyMessage(request));
            } catch (IOException ex) {
                log.severe(ex);
                throw new AlgorithmMethodException(ex.getMessage());
            }
        } finally {
            balancingAction();
            waitingForHost = null;
        }
    }
    
    /** Process the request for placing a replica at this host */
    public void placeReplica(ReplicateOperation operation, DistAlgRequestMessage request) throws AlgorithmMethodException {
        try {
            try {
                if ((! operation.silent) && (! checkWaitingForHost(request.getSender())))
                    operation.endOperation(LoadBalancingErrorCode.ERROR_NOT_ASKED);
                else {
                    Replica replica = new Replica(this, operation.replicatedNode, MemoryStorageBucketNoDups.class);
                    
                    operation.replicaId = replica.getThisNode();
                    operation.endOperation();
                    log.info("Replica wrapper successfully created at this peer");
                }
            } catch (InstantiationException ex) {
                log.severe(ex);
                operation.endOperation(LoadBalancingErrorCode.ERROR_NODE_CREATION);
            }
            try {
                messageDisp.replyMessage(new DistAlgReplyMessage(request));
            } catch (IOException ex) {
                log.severe(ex); throw new AlgorithmMethodException(ex.getMessage());
            }
        } finally {
            balancingAction();
            if (! operation.silent)
                waitingForHost = null;
        }
    }
    
    /** Remove a replica from this peer */
    public void removeReplica(UnifyOperation operation, DistAlgRequestMessage request) throws AlgorithmMethodException {
        try {
            try {
                if ((! operation.silent) && (! checkWaitingForHost(request.getSender())))
                    operation.endOperation(LoadBalancingErrorCode.ERROR_NOT_ASKED);
                else {
                    Replica replica = (Replica) nodes.get(operation.replicaId);
                    if (replica == null)
                        operation.endOperation(LoadBalancingErrorCode.ERROR_REPLICA_REMOVAL);
                    else {
                        // remove the id - all requests will be refused now
                        preRemoveNode(replica);
                        removeNode(replica);
                        operation.endOperation();
                        log.info("Replica "+operation.replicaId+" successfully removed from this host");
                    }
                }
            } catch (ClassCastException ex) {
                log.warning("Node "+operation.replicaId+" is not a replica: "+ex.toString());
                operation.endOperation(LoadBalancingErrorCode.ERROR_REPLICA_REMOVAL);
            }
            
            try {
                messageDisp.replyMessage(new DistAlgReplyMessage(request));
            } catch (IOException ex) {
                log.severe(ex);
                throw new AlgorithmMethodException(ex.getMessage());
            }
        } finally {
            balancingAction();
            if (! operation.silent)
                waitingForHost = null;
        }
    }
    
    /** Executing method that is generally for any node at this host */
    public void operationAtAnyNode(AbstractOperation operation, DistAlgRequestMessage request) throws AlgorithmMethodException {
        BalancedDistributedAlgorithm subnode = null;
        try {
            synchronized (nodes) {
                // find any node which is not a replica
                for (BalancedDistributedAlgorithm alg : nodes.values()) {
                    if (! Replica.class.isAssignableFrom(alg.getClass())) {
                        subnode = alg; break;
                    }
                }
                // if all nodes are replicas, then select a replica
                if ((subnode == null) && (nodes.size() > 0)) {
                    // update the existingHost to the replica's master node
                    existingHost = ((Replica) nodes.values().iterator().next()).getReplicatedNode();
                }
            }
            if (subnode == null) {
                if (existingHost != null) {
                    log.info("Forwarding operation "+operation+" to a non-empty peer "+existingHost);
                    AbstractOperation oper = ((DistAlgReplyMessage) messageDisp.sendMessageWaitReply(new OperationRequestMessage(operation), existingHost).getFirstReply()).getOperation();
                    operation.updateAnswer(oper);
                    log.info("Forwarded operation: "+oper);
                    return;
                } else {
                    synchronized (nodes) {
                        while (subnode == null) {
                            try {
                                nodes.wait(3000);
                            } catch (InterruptedException ignore) { }
                            for (BalancedDistributedAlgorithm alg : nodes.values()) {
                                if (! Replica.class.isAssignableFrom(alg.getClass())) {
                                    subnode = alg; break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            log.severe(ex); throw new AlgorithmMethodException(ex);
        }
        try {
            // execute the operation at this node
            subnode.executeOperation(operation);
        } catch (NoSuchMethodException e) {
            throw new AlgorithmMethodException(e);
        } catch (AlgorithmMethodException e) {
            if (e.getCause() != null && NodeRemovedException.class.isAssignableFrom(e.getCause().getClass())) {
                log.info(e.getMessage());
                // if the node was removed during the query processing then process it once again on a different node
                operationAtAnyNode(operation, request);
            } else
                throw e;
        }
    }
    
    /** Receving a OperationRequestMessage - execute the operaion at any node*/
    public final void receive(OperationRequestMessage message) {
        try {
            executeOperation(message.getOperation());
        } catch (AlgorithmMethodException ex) {
            log.severe(ex);
        } catch (NoSuchMethodException ex) {
            log.severe(ex);
        }
        try {
            messageDisp.replyMessage(new DistAlgReplyMessage(message));
        } catch (IOException ex) {
            log.severe(ex);
        }
    }
    
    /**********************  Balancing operations *****************************/
    
    /** Reset some statistics when the load-balancing action is performed at this host */
    protected void balancingAction() {
        balancingAction(null);
    }
    protected void balancingAction(BalancedDistributedAlgorithm node) {
        busyLoad.reset();
        singleLoad.reset();
        if (node != null)
            node.resetStatistics();
    }
    
    /** Split given node and place the new node at given host */
    public boolean split(BalancedDistributedAlgorithm node, NetworkNode host) {
        log.info("Running Split operation to  "+ host +" at "+node);
        balancingAction(node);
        CreateNodeOperation createOperation;
        try {
            createOperation = node.splitNode();
        } catch (UnsupportedOperationException e) {
            log.warning("Performing split: "+e.getMessage());
            createOperation = null;
        }
        if (createOperation == null) {
            log.info("Node cannot be split");
            // send the empty operation to the host
            try {
                messageDisp.sendMessage(new DistAlgRequestMessage(new SuitableHostOperation(true)), host);
            } catch (IOException ex) {
                log.severe(ex); return false;
            }
            return false;
        }
        // remove the replicas first
        Collection<NetworkNode> replicationPeers = unifyAllSilent(node);
        
        // make the new peer create replicas at the same peers immediately after creating it
        createOperation.replicationPeers = replicationPeers;
        
        DistAlgRequestMessage splitRequest = new DistAlgRequestMessage(createOperation);
        try {
            DistAlgReplyMessage reply = (DistAlgReplyMessage) messageDisp.sendMessageWaitReply(splitRequest, host).getFirstReply();
            if (reply.getOperation().wasSuccessful()) {
                // create the replicas again
                if (! replicationPeers.isEmpty())
                    replicateAllSilent(node, replicationPeers);
                return true;
            } else {
                log.warning("Error splitting node "+node.getThisNode()+": "+reply.getOperation().getErrorCode());
                return false;
            }
        } catch (IOException ex) {
            log.severe(ex); return false;
        }
    }
    
    /** Remove given node completely - the data is expected to be moved elsewhere
     * @return false if the node couldn'e leave for some reason
     */
    public boolean leave(BalancedDistributedAlgorithm node) {
        log.info("Starting LEAVE load-balancing action on\n"+node);
        balancingAction();
        // remove the replicas first
        unifyAllSilent(node);
        
        // remove the id - all requests will be refused now
        preRemoveNode(node);
        // the data volume to be sent over network
        long nodeData = node.getDataLoad();
        NetworkNode mergingPeer = new NetworkNode(node.getMergingNode(), false);
        boolean rtv = node.leave();
        if (rtv) {
            log.info("This node has left the system\n"+node);
            log.info("BALANCING: LEAVE: sending "+nodeData+" from "+getThisNode()+" to "+mergingPeer);
            removeNode(node);
        } else {
            undoRemoveNode(node);
            log.warning("Node "+node+" cannot leave the system");
        }
        // let the peer the node has merged with, that the balancing action is over
        try {
            messageDisp.sendMessage(new DistAlgRequestMessage(new SuitableHostOperation(true)), mergingPeer);
        } catch (IOException e) {
            log.severe(e);
        }
        return rtv;
    }
    
    /** Map of nodes that have been migrated - the "new destination" can be "null" */
    private Map<NetworkNode, NetworkNode> migratedNodes = Collections.synchronizedMap(new HashMap<NetworkNode, NetworkNode>());
    
    /** List of undelivered messages - waiting for migrated node setting */
    private transient Map<NetworkNode, List<Message>> undeliveredMessages = new HashMap<NetworkNode, List<Message>>();
    
    /** List of message dispatchers of the removed nodes. They are used for receiving the reply messages. */
    private transient Map<NetworkNode, MessageDispatcher> deletedDispatchers = Collections.synchronizedMap(new HashMap<NetworkNode, MessageDispatcher>());
    
    /** Migrate given node to specifed host */
    public boolean migrate(BalancedDistributedAlgorithm node, NetworkNode host) {
        balancingAction();
        
        // new behaviour: wait until the node migrates and then forward the request to the new host
        // TODO: check this with requests to replicas!
        migratedNodes.put(node.getThisNode(), (NetworkNode) null);
        
        // remove the id - all requests will be refused now
        preRemoveNode(node);
        node.host = null;
        // the data volume to be sent over network
        long nodeData = node.getDataLoad();
        DistAlgRequestMessage migrateRequest = new DistAlgRequestMessage(new MigrateOperation(node, node.getThisNode()));
        try {
            MigrateOperation operation = (MigrateOperation) ((DistAlgReplyMessage) messageDisp.sendMessageWaitReply(migrateRequest, host).getFirstReply()).getOperation();
            boolean rtv = operation.wasSuccessful();
            if (rtv) {
                log.info("This node has been migrated to host "+host+"\n"+node);
                log.info("BALANCING: MIGRATE: sending "+nodeData+" from "+getThisNode()+" to "+host);
                
                synchronized (migratedNodes) {
                    migratedNodes.put(operation.origId, operation.newId);
                    // forward the undelivered message to the new node
                    List<Message> messages = undeliveredMessages.remove(operation.origId);
                    if (messages != null) {
                        for (Message msg : messages) {
                            log.info("Forwarding request to a migrated node ("+operation.origId+" -> "+operation.newId);
                            node.getMessageDisp().sendMessage(msg, operation.newId, true);
                        }
                    }
                }
                removeNode(node);
            } else {
                log.warning("Error migrating "+node+" to host "+host);
                synchronized (migratedNodes) {
                    node.host = this;
                    undoRemoveNode(node);
                    migratedNodes.remove(operation.origId);
                    List<Message> messages = undeliveredMessages.remove(operation.origId);
                    if (messages != null) {
                        for (Message msg : messages) {
                            log.info("Delivering the message to "+operation.origId);
                            messageDisp.sendMessage(msg, operation.origId, true);
                        }
                    }
                }
                // let the host know, that the balancing action will not be finished
                messageDisp.sendMessage(new DistAlgRequestMessage(new SuitableHostOperation(true)), host);
            }
            return rtv;
        } catch (IOException ex) {
            log.severe(ex);
            return false;
        }
    }
    
    /**
     * Create a replica of given node at given peer.
     * @return the new replica's id
     */
    public NetworkNode replicate(BalancedDistributedAlgorithm node, NetworkNode host) {
        log.info("Running Replicate operation of  node "+ node+" to peer "+host);
        balancingAction(node);
        try {
            //Collection<LocalBucket> buckets = node.getStorage().getAllBuckets();
            Message msg = new DistAlgRequestMessage(new ReplicateOperation(node.getThisNode()));
            ReplicateOperation operation = (ReplicateOperation) ((DistAlgReplyMessage) messageDisp.sendMessageWaitReply(msg, host).getFirstReply()).getOperation();
            
            if (operation.wasSuccessful()) {
                log.info("Replica for node "+node.getThisNode()+" wrapper successfully created at "+operation.replicaId);
                log.info("BALANCING: REPLICATE: sending "+node.getDataLoad()+" from "+getThisNode()+" to "+host);
                node.storageDispatcher.createReplica(operation.replicaId);
                //replicateDataInThread(node, operation.replicaId);
                log.info("Replication operation finished: "+operation.replicaId);
            } else
                log.warning("Error creating replica wrapper at peer "+host);
            return operation.replicaId;
        } catch (IOException ex) {
            log.severe(ex);
            return null;
        } catch (CapacityFullException e) {
            log.severe(e);
            return null;
        }
    }
    
    /**
     * The internal method for unnoticed replication of given node to given peers
     */
    private void replicateAllSilent(BalancedDistributedAlgorithm node, Collection<NetworkNode> peers) {
        try {
            Message msg = new DistAlgRequestMessage(new ReplicateOperation(node.getThisNode(), true));
            List<DistAlgReplyMessage> replies = (List<DistAlgReplyMessage>) messageDisp.sendMessageWaitReply(msg, peers).getReplies();
            for (DistAlgReplyMessage reply : replies) {
                if (reply.getOperation().wasSuccessful())
                    //replicateDataInThread(node, ((ReplicateOperation) reply.getOperation()).replicaId);
                    node.storageDispatcher.createReplica(((ReplicateOperation) reply.getOperation()).replicaId);
                else log.warning("Error creating replica wrapper at peer "+reply.getSender());
            }
        } catch (IOException ex) {
            log.severe(ex);
        } catch (CapacityFullException e) {
            log.severe(e);
        } catch (ClassCastException e) {
            log.severe(e);
        } catch (InterruptedException e) {
            log.severe(e);
        }
    }
    
    /**
     * Remove a replica of given node from given network node.
     * @return false if there is no such replica
     */
    public boolean unify(BalancedDistributedAlgorithm node, NetworkNode replicaId) {
        try {
            if (! node.storageDispatcher.removeReplica(replicaId))
                return false;
            balancingAction(node);
            
            Message msg = new DistAlgRequestMessage(new UnifyOperation(replicaId));
            NetworkNode host = new NetworkNode(replicaId, false);
            UnifyOperation operation = (UnifyOperation) ((DistAlgReplyMessage) messageDisp.sendMessageWaitReply(msg, host).getFirstReply()).getOperation();
            log.info("BALANCING: UNIFY: sending "+0+" from "+host+" to "+getThisNode());
            
            return operation.wasSuccessful();
        } catch (IOException ex) {
            log.severe(ex);
            return false;
        }
    }
    
    /**
     * The internal method for unnoticed and unchecked removal of all replicas of given node
     *
     * @return the list of peers hosting the replicas
     */
    private Collection<NetworkNode> unifyAllSilent(BalancedDistributedAlgorithm node) {
        Collection<NetworkNode> peers = new ArrayList<NetworkNode>();
        try {
            for (NetworkNode replicaId : node.storageDispatcher.getAllReplicaNodes()) {
                node.storageDispatcher.removeReplica(replicaId);
                Message msg = new DistAlgRequestMessage(new UnifyOperation(replicaId, true));
                NetworkNode peer = new NetworkNode(replicaId, false);
                UnifyOperation operation = (UnifyOperation) ((DistAlgReplyMessage) messageDisp.sendMessageWaitReply(msg, peer).getFirstReply()).getOperation();
                if (! operation.wasSuccessful())
                    log.warning("ERROR deleting a replica "+replicaId+": "+operation.getErrorCode());
                peers.add(peer);
            }
            return peers;
        } catch (IOException ex) {
            log.severe(ex);
            return peers;
        }
    }
    
    /** Returns a random node that I know in the system (for gossiping) */
    public NetworkNode getRandomNode() {
        BalancedDistributedAlgorithm subnode = null;
        synchronized (nodes) {
            // find any node which is not a replica
            for (BalancedDistributedAlgorithm alg : nodes.values()) {
                if (! Replica.class.isAssignableFrom(alg.getClass())) {
                    subnode = alg; break;
                }
            }
        }
        if (subnode != null)
            return subnode.getRandomNode();
        else return gossipModule.unloadedPeers.getRandomHost();
    }
    
    /****************************************   balancing operations for manual balancing    ************************/
    /** Split ARBITRARY (first) node (non-replica) and place the new node at given host. The host is notified about being used for balacing */
    public boolean split(NetworkNode host) {
        BalancedDistributedAlgorithm subnode = null;
        for (BalancedDistributedAlgorithm alg : nodes.values()) {
            if (! Replica.class.isAssignableFrom(alg.getClass())) {
                subnode = alg; break;
            }
        }
        if (subnode == null)
            return false;
        try {
            //waitingLoad.reset();
            SuitableHostOperation op = (SuitableHostOperation) ((DistAlgReplyMessage) messageDisp.sendMessageWaitReply(
                    new DistAlgRequestMessage(new SuitableHostOperation(true,0,0)), host).getFirstReply()).getOperation();
            if (op.wasSuccessful())
                return split(subnode, host);
            return false;
        } catch (IOException ex) {
            log.severe(ex); return false;
        }
    }
    
    /** Remove the first node at this host (arbitrary) which is not a replica */
    public boolean leave() {
        BalancedDistributedAlgorithm subnode = null;
        for (BalancedDistributedAlgorithm alg : nodes.values()) {
            if (! Replica.class.isAssignableFrom(alg.getClass())) {
                subnode = alg; break;
            }
        }
        if (subnode == null)
            return false;
        return leave(subnode);
    }
    
    public List<Class<AbstractOperation>> getSupportedOperations() {
        BalancedDistributedAlgorithm subnode = null;
        synchronized (nodes) {
            // find any node which is not a replica
            for (BalancedDistributedAlgorithm alg : nodes.values()) {
                if (! Replica.class.isAssignableFrom(alg.getClass())) {
                    subnode = alg; break;
                }
            }
        }
        if (subnode == null) {
            return new ArrayList<Class<AbstractOperation>>();
        } else {
            return subnode.getSupportedOperations();
        }
    }
    /** Temporary method to change all locators from .xml to .png */
/*    public void changeStorage() {
        for (BalancedDistributedAlgorithm node : nodes.values()) {
            //System.out.println(node.toString()+" storage:\n");
            for (Bucket bucket : node.getStorageDispatcher().getAllBuckets()) {
                for (GenericAbstractObjectIterator<AbstractObject> it = bucket.getAllObjects(); it.hasNext(); ) {
                    AbstractObject obj = it.next();
                    String locator = obj.getLocatorURI().toString();
                    if (obj.getLocatorURI().toString().endsWith(".xml")) {
                        obj.setLocatorURI(obj.getLocatorURI().toString().substring(0, ))
                        log.info("Changing to "+obj.getLocatorURI());
                    }
                }
            }
        }
    }*/
    
    /****************************************   toString & periodical printing **********************************/
    /** toString method - prints info about the nodes and the load */
    public String toString() {
        StringBuffer buf = new StringBuffer("Peer '"+algorithmName+"' at "+getThisNode()+", hosting "+nodes.size()+" node(s):\n");
        for (BalancedDistributedAlgorithm node : nodes.values()) {
            buf.append("    "+node.getThisNode()+" busy-load: "+(int) node.getBusyLoad()+", single-load: "+(int) node.getSingleLoad()+", data-load: "+node.getDataLoad()+"\n");//+((node.replicas.isEmpty())?"":"; replicas: "+node.replicas)+"\n");
            buf.append("        "+node+"\n");
        }
        buf.append("busy-load: "+(int) getBusyLoad()+", single-load: "+(int)getSingleLoad()+", data-load: "+getDataLoad()+"\n");
        buf.append(gossipModule.toString());
        if (waitingForHost != null)
            buf.append("\nPeer "+waitingForHost+" will do balancing action on me");
        if (! loadBalancing)
            buf.append("\nThe load balancing is OFF");
        if (receiver == null)
            buf.append("\nNetwork not activated yet");
        return buf.toString();
    }
    
    /** periodically print info */
    private transient Timer timerPrint = new Timer();
    /** TimerTask class to print periodically info about this algorithm */
    private class TimerTaskPrint extends TimerTask {
        int counter = 0;
        protected TimerTaskPrint() { }
        // periodically print info about the algorithm to log
        public void run() {
            // print the load info
            log.info("BALANCING: peer "+getThisNode()+": busy-load: "+(int) getBusyLoad()+", single-load: "+(int)getSingleLoad()+", data-load: "+getDataLoad()+", avg_busy-load: "+(int) gossipModule.avgWaitLoadEst()+", avg_data-load: "+(int) gossipModule.avgDataLoadEst());
            if (counter++ % 3 == 0)
                log.info(Host.this.toString());
        }
    }
}
