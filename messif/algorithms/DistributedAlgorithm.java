/*
 * DistributedAlgorithm.java
 *
 * Created on 20. kveten 2004, 23:34
 */

package messif.algorithms;

import java.lang.reflect.InvocationTargetException;
import messif.executor.SingleMethodExecutor;
import messif.netcreator.Startable;
import messif.network.MessageDispatcher;
import messif.network.NavigationElement;
import messif.network.NetworkNode;
import messif.operations.AbstractOperation;
import messif.statistics.OperationStatistics;
import messif.statistics.StatisticCounter;
import messif.statistics.StatisticRefCounter;
import messif.utility.Logger;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import messif.statistics.StatisticTimer;
import messif.statistics.Statistics;

/**
 *  Abstract distributed algorithm framework with support for request/reply messaging and respective operation executive.
 *
 *  Distributed algorithm subclass should override createRequestMessage
 *  and createResponseMessage to add algorithm specific message extensions.
 *
 *  Operation methods should have two arguments - the operation (a subclass of AbstractOperation) and
 *  the request message (a subclass of DistAlgRequestMessage). Operation method should process
 *  the operation and use standard methods of message dispatcher (sendMessage family) to process the operation.
 *  The original message that triggered this processing is the second argument of the executed
 *  methods and can be null if the method was called locally (i.e. processing start).
 *
 * @author  xbatko
 */
public abstract class DistributedAlgorithm extends Algorithm implements Serializable, Startable {
    /** class id for serialization */
    private static final long serialVersionUID = 4L;

    /** Logger */
    protected static Logger log = Logger.getLoggerEx("messif.algorithm");

    /** Message dispatcher for this distributed algorithm */
    protected transient MessageDispatcher messageDisp;


    /****************** Constructors ******************/
    
    /** Creates a new instance of DistributedAlgorithm */
    public DistributedAlgorithm(String algorithmName, int port, int broadcastPort) throws IllegalArgumentException {
        super(algorithmName);
        
        try {
            // Start Message dispatcher
            createMessageDispatcher(port, broadcastPort);
        } catch (IOException e) {
            log.severe(e);
            throw new IllegalArgumentException("Can't start message dispatcher: " + e.getMessage());
        }
    }
    
    /** Creates a new instance of DistributedAlgorithm */
    public DistributedAlgorithm(String algorithmName, int port) throws IllegalArgumentException {
        this(algorithmName, port, 0); // Do not start broadcast capabilities by default
    }
    
    /** Creates a new instance of DistributedAlgorithm */
    public DistributedAlgorithm(String algorithmName) throws IllegalArgumentException {
        this(algorithmName, 0);
    }
    
    /** Creates a new instance of DistributedAlgorithm */
    public DistributedAlgorithm(String algorithmName, DistributedAlgorithm parent, int newId) throws IllegalArgumentException {
        super(algorithmName);

        // Start Message dispatcher
        createMessageDispatcher(parent, newId);
    }

    /** Creates a new instance of DistributedAlgorithm with a message dispatcher already created */
    public DistributedAlgorithm(String algorithmName, MessageDispatcher messageDisp) throws IllegalArgumentException {
        super(algorithmName);

        // Only set the message dispatcher
        this.messageDisp = messageDisp;
    }    

    /****************** Destructor ******************/
    
    /** Public destructor to stop the algorithm.
     *  This should be overriden in order to clean up.
     */
    public void finalize() throws Throwable {
        super.finalize();
        messageDisp.closeSockets();
    }


    /****************** Name enrichment ******************/

    /**
     * Returns the name of this algorithm with host:port of its message dispatcher
     * @return the name of this algorithm with host:port of its message dispatcher
     */
    public String getName() {
        return super.getName() + " at " + getThisNode().getHost().getHostName() + ":" + getThisNode().getPort();
    }


    /****************** Message dispatcher ******************/

    /**
     * Returns the network node of this distributed algorithm
     * @return the network node of this distributed algorithm
     */
    public NetworkNode getThisNode() {
        return messageDisp.getNetworkNode();
    }

    /** 
     * This method creates the message dispatcher and sets the <code>meesageDisp</code> field. 
     * @return the created message dispatcher
      */
    protected MessageDispatcher createMessageDispatcher(int port, int broadcastPort) throws IOException {
        return messageDisp = new MessageDispatcher(port, broadcastPort);
    }

    /** 
     * This method creates the message dispatcher given a parent dispatcher
     * and a new id of the new NetworkNode and sets the <code>meesageDisp</code> field. 
     * @return the created message dispatcher
      */
    protected MessageDispatcher createMessageDispatcher(DistributedAlgorithm parent, int newId) {
        return messageDisp = new MessageDispatcher(parent.messageDisp, newId);
    }

    /** Store the message dispatcher to given output stream - store the port & broadcast port. Is overriden by BalancedDA */
    protected void writeMessageDisp(ObjectOutputStream out) throws IOException {
        out.writeInt(messageDisp.getNetworkNode().getPort());
        out.writeInt(messageDisp.getBroadcastPort());
    }
    
    /** read the message dispatcher from given input stream - read the port & broadcast port. Is overriden by BalancedDA */
    protected void readMessageDisp(ObjectInputStream in) throws IOException {
        createMessageDispatcher(in.readInt(), in.readInt());
    }

    /**
     * Returns the message dispather of this distributed algorithm.
     * @return the message dispather of this distributed algorithm
     */
    public MessageDispatcher getMessageDispatcher() {
        return messageDisp;
    }


    /****************** Serialization ******************/

    /** Serialization method - store the "port" and "broadcastPort" of the node on top to restore the message dispatcher */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        
        writeMessageDisp(out);
    }

    /** Deserialization method - create the message dispatcher from the restored values "port", "broadcastport"
     * (for the top-most node).
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        
        readMessageDisp(in);
    }


    /****************** Operation execution ******************/
    
    /** Execute algorithm operation from received message */
    protected void receiveRequest(DistAlgRequestMessage msg) throws AlgorithmMethodException {
        if (maximalConcurrentOperations > 0)
            runningOperations.acquireUninterruptibly();
        Statistics navigElDistComp = null;
        try {
            // Setup statistics
            navigElDistComp = msg.registerBoundStat("DistanceComputations");
            
            operationExecutor.execute(msg.getOperation(), msg);
        } catch (Exception e) {
            throw new AlgorithmMethodException(e);
        } finally {
            if (navigElDistComp != null)
                navigElDistComp.unbind();
            if (maximalConcurrentOperations > 0)
                runningOperations.release();
        }
    }
    
    /** Execute algorithm operation on demand */
    public void executeOperation(AbstractOperation operation) throws AlgorithmMethodException, NoSuchMethodException {
        if (maximalConcurrentOperations > 0)
            runningOperations.acquireUninterruptibly();
        Statistics navigElDistComp = null;
        try {
            // Create new request message
            DistAlgRequestMessage msg = createRequestMessage(operation);
            
            // Setup statistics
            navigElDistComp = msg.registerBoundStat("DistanceComputations");
            
            if (Statistics.isEnabledGlobally()) {
                // Measure time of execution (as an operation statistic)
                StatisticTimer operationTime = OperationStatistics.getOpStatistics("OperationTime", StatisticTimer.class);
                operationTime.start();
                operationExecutor.execute(operation, msg);
                operationTime.stop();
            } else operationExecutor.execute(operation, msg);
        } catch (InvocationTargetException e) {
            throw new AlgorithmMethodException(e.getCause());
        } catch (InstantiationException e) {
            throw new AlgorithmMethodException("There is no DistanceComputations statistics global counter yet!?!");
        } finally {
            if (navigElDistComp != null)
                navigElDistComp.unbind();
            if (maximalConcurrentOperations > 0)
                runningOperations.release();
        }
    }
    
    /** Execute algorithm operation on background */
    public void backgroundExecuteOperation(AbstractOperation operation, boolean updateStatistics) throws NoSuchMethodException {
        if (maximalConcurrentOperations > 0)
            runningOperations.acquireUninterruptibly(); // Release for this acquire is in the waitBackgroundExecuteOperation
        try {
            // Create new request message
            DistAlgRequestMessage msg = createRequestMessage(operation);
            bgExecutionList.get().backgroundExecute(
                    updateStatistics,
                    new Object[]{operation, msg},
                    new SingleMethodExecutor(msg, "registerBoundStat", "DistanceComputations"), 
                    new SingleMethodExecutor(msg, "deregisterOperStats")
            );
        } catch (NoSuchMethodException e) {
            if (maximalConcurrentOperations > 0)
                runningOperations.release();
            throw e;
        } catch (RuntimeException e) {
            if (maximalConcurrentOperations > 0)
                runningOperations.release();
            throw e;
        }
    }
    
    /** Create the request message - can be overriden by descendants */
    protected DistAlgRequestMessage createRequestMessage(AbstractOperation operation) {
        return new DistAlgRequestMessage(operation);
    }
    
    
    /****************** Operation selection ******************/
    
    /** This method should return an array of additional parameters, that are needed for operation execution.
     *  The list must be consistent (speaking in terms of types) with the executeOperation method.
     */
    @Override
    protected Class[] getExecutorParamClasses() {
        Class[] rtv = { AbstractOperation.class, DistAlgRequestMessage.class };
        return rtv;
    }
    
    /****************** Reply merging functions ******************/
    
    /** Update supplied operation answer with partial answers from reply messages
     *  @param targetOperation the operation that should be updated
     *  @param replyMessages the list of reply messages received with partial answers
     */
    public static void mergeOperationsFromReplies(AbstractOperation targetOperation, Collection<? extends DistAlgReplyMessage> replyMessages) {
        for (DistAlgReplyMessage msg : replyMessages)
            targetOperation.updateAnswer(msg.getOperation());
    }

    /** Update supplied statistics with partial statistics from reply messages
     *  @param targetStatistics the operation statistics object that should be updated
     *  @param replyMessages the list of reply messages received with partial statistics
     */
    public void mergeStatisticsFromReplies(OperationStatistics targetStatistics, Collection<? extends DistAlgReplyMessage> replyMessages) {
        mergeStatisticsFromReplies(targetStatistics, replyMessages, 0);
    }
    
    /** Update supplied statistics with partial statistics from reply messages
     *  @param targetStatistics the operation statistics object that should be updated
     *  @param replyMessages the list of reply messages received with partial statistics
     *  @param localDC says the number of DC performed by the local search on this node, if any
     */
    public void mergeStatisticsFromReplies(OperationStatistics targetStatistics, Collection<? extends DistAlgReplyMessage> replyMessages, long localDC) {
        if (replyMessages.isEmpty())
            return;

        StatisticCounter totalMsgs = targetStatistics.getStatisticCounter("Total.Messages");
        
        // first, create the nodesMap - maximum over node's DCs on given positions in all replies
        SortedMap<Integer, Map<NetworkNode, Long>> nodesMap = new TreeMap<Integer, Map<NetworkNode, Long>>();
        for (DistAlgReplyMessage reply : replyMessages) {
            // create a map of node->DC taken as max over all nodes in all replies
            int i = 0;
            NetworkNode previousNode = null;
            for (Iterator<NavigationElement> it = reply.getPathElements(); it.hasNext(); i++) {
                NavigationElement el = it.next();
                Map<NetworkNode, Long> positionMap = nodesMap.get(i);
                if (positionMap == null)
                    nodesMap.put(i, positionMap = new HashMap<NetworkNode, Long>());
                
                NetworkNode node = el.getSender();
                Long actualNodeDC = positionMap.get(node);
                if (actualNodeDC != null) // sum the DC of hosts that appear more than once in the navigation path
                    positionMap.put(node, Math.max(actualNodeDC, (el.getStatistics()==null)?0:el.getStatistics().getStatisticCounter("NavigationElement.DistanceComputations").get()));
                else {
                    positionMap.put(node, (el.getStatistics()==null)?0:el.getStatistics().getStatisticCounter("NavigationElement.DistanceComputations").get());
                    if ((previousNode != null) && ! previousNode.equalsIgnoreNodeID(node))
                        totalMsgs.add();
                }
                previousNode = node;
            }
            // add the reply messages (if not from the same host)
            if ((previousNode != null) && ! previousNode.equalsIgnoreNodeID(getThisNode()))
                totalMsgs.add();
        }
        
        // calculate sum of DCs computed on the whole PEER
        StatisticRefCounter peersDC = targetStatistics.getStatisticRefCounter("Peers.DistanceComputations");
        for (Map<NetworkNode, Long> position : nodesMap.values()) {
            for (Map.Entry<NetworkNode, Long> nodeMaxDC : position.entrySet())
                peersDC.add(new NetworkNode(nodeMaxDC.getKey(), false), nodeMaxDC.getValue());
            // calculate total messages as sum of sizes of the positions in the "nodesMap"
        }
        
        // statistics
        StatisticRefCounter peersParDC = targetStatistics.getStatisticRefCounter("PeersParallel.DistanceComputations");
        StatisticCounter hopCount = targetStatistics.getStatisticCounter("HopCount");
        hopCount.reset();
        
        // iterate over the replies again and calculate parallel DC for all hosts
        for (DistAlgReplyMessage reply : replyMessages) {
            // create a map of node->DC because messages can create loops (in the terms of "hosts")
            Set<NetworkNode> visitedHosts = new HashSet<NetworkNode>();
            long parDCs = 0;
            int replyLength = 0;
            NetworkNode previousHost = null;
            for (Iterator<NavigationElement> it = reply.getPathElements(); it.hasNext(); ) {
                NavigationElement el = it.next();
                NetworkNode peer = new NetworkNode(el.getSender(), false);
                if (visitedHosts.add(peer))
                    peersParDC.min(peer, peersDC.get(peer) + parDCs);
                parDCs += (el.getStatistics()==null)?0:el.getStatistics().getStatisticCounter("NavigationElement.DistanceComputations").get();
                
                // messaging
                if ((previousHost != null) && (! previousHost.equals(el.getSender())))
                    replyLength++;
                previousHost = peer;
            }
            if (! getThisNode().equals(previousHost))
                replyLength++;
            hopCount.max(replyLength);
        }
        
        // create the ParallelDC statics now as a max over all hostsParallel DCs
        StatisticCounter parDC = targetStatistics.getStatisticCounter("Parallel.DistanceComputations");
        for (Object obj : peersParDC.getKeys())
            parDC.max(peersParDC.get(obj));
        if (localDC > 0)
            parDC.max(localDC);
        
        // create total distance computations statistics
        long totalDC = 0;
        for (Object host : peersDC.getKeys())
            totalDC += peersDC.get(host);
        targetStatistics.getStatisticCounter("Total.DistanceComputations").add(totalDC);
        targetStatistics.getStatisticCounter("Total.DistanceComputations").add(localDC);
        
        
        // the peers that answered (are at the end of the paths) are put into a separate map
        StatisticRefCounter answeringPeers = targetStatistics.getStatisticRefCounter("AnsweringPeers.DistanceComputations");
        for (DistAlgReplyMessage reply : replyMessages) {
            NetworkNode peer = new NetworkNode(reply.getSender(), false);
            StatisticCounter counter = peersDC.remove(peer);
            if (counter != null)
                answeringPeers.add(peer, counter.get());
        }
        // add this node statistics
        if (localDC > 0) {
            NetworkNode peer = new NetworkNode(getThisNode(), false);
            answeringPeers.add(peer, localDC);
        }
        
        // remove the hosts parallel statistic and
        targetStatistics.removeStatistic("PeersParallel.DistanceComputations");
        targetStatistics.removeStatistic("Peers.DistanceComputations");
        
        // add statistic "AnsweringNodes"
        targetStatistics.getStatisticCounter("AnsweringNodes").add(replyMessages.size());
        if (localDC > 0)
            targetStatistics.getStatisticCounter("AnsweringNodes").add();
    }
    
}
