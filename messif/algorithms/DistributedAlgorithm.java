/*
 * DistributedAlgorithm.java
 *
 * Created on 20. kveten 2004, 23:34
 */

package messif.algorithms;

import messif.netcreator.Startable;
import messif.network.MessageDispatcher;
import messif.network.NavigationElement;
import messif.network.NetworkNode;
import messif.operations.AbstractOperation;
import messif.statistics.OperationStatistics;
import messif.statistics.StatisticCounter;
import messif.statistics.StatisticRefCounter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import messif.network.Message;
import messif.network.ReplyMessage;
import messif.network.ReplyReceiver;
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
public abstract class DistributedAlgorithm extends Algorithm implements Startable {
    /** class id for serialization */
    private static final long serialVersionUID = 5L;

    //****************** Attributes ******************//

    /** Message dispatcher for this distributed algorithm */
    protected final MessageDispatcher messageDisp;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of DistributedAlgorithm.
     * @param algorithmName the name of this algorithm
     * @param port the TCP/UDP port on which this distributed algorithm communicates
     * @param broadcastPort the UDP multicast port that this distributed algorithm uses for broadcast
     * @throws IllegalArgumentException if the prototype returned by {@link #getExecutorParamClasses getExecutorParamClasses}
     *      has no items or there was a problem starting message dispatcher
     */
    public DistributedAlgorithm(String algorithmName, int port, int broadcastPort) throws IllegalArgumentException {
        super(algorithmName);
        
        try {
            // Start Message dispatcher
            this.messageDisp = new MessageDispatcher(port, broadcastPort);
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getClass().toString(), e);
            throw new IllegalArgumentException("Can't start message dispatcher: " + e.getMessage());
        }
    }

    /**
     * Creates a new instance of DistributedAlgorithm without broadcast capabilities.
     * @param algorithmName the name of this algorithm
     * @param port the TCP/UDP port on which this distributed algorithm communicates
     * @throws IllegalArgumentException if the prototype returned by {@link #getExecutorParamClasses getExecutorParamClasses}
     *      has no items or there was a problem starting message dispatcher
     */
    public DistributedAlgorithm(String algorithmName, int port) throws IllegalArgumentException {
        this(algorithmName, port, 0);
    }

    /**
     * Creates a new instance of DistributedAlgorithm without broadcast capabilities.
     * The TCP/UDP port on which this distributed algorithm communicates is selected by the
     * operating system and can be queried through {@link #getThisNode()}.
     * @param algorithmName the name of this algorithm
     * @throws IllegalArgumentException if the prototype returned by {@link #getExecutorParamClasses getExecutorParamClasses}
     *      has no items or there was a problem starting message dispatcher
     */
    public DistributedAlgorithm(String algorithmName) throws IllegalArgumentException {
        this(algorithmName, 0);
    }

    /**
     * Creates a new instance of DistributedAlgorithm with a higher-level message dispatcher queue.
     * The TCP/UDP port on which this distributed algorithm communicates as well as
     * the broadcast capabilities are linked to the specified message dispatcher.
     * @param algorithmName the name of this algorithm
     * @param parentDispatcher the higher level dispatcher this algorithm's dispacher is connected to
     * @param nodeID the sub-identification of this algorithm's dispatcher for the higher level
     * @throws IllegalArgumentException if the prototype returned by {@link #getExecutorParamClasses getExecutorParamClasses}
     *      has no items or there was a problem starting message dispatcher
     */
    public DistributedAlgorithm(String algorithmName, MessageDispatcher parentDispatcher, int nodeID) throws IllegalArgumentException {
        super(algorithmName);

        // Start Message dispatcher
        this.messageDisp = new MessageDispatcher(parentDispatcher, nodeID);
    }

    /**
     * Compatibility deserialization method that reads this object having version 4.
     * This works with the special <code>ObjectInputStreamConverter</code> class from utils.
     * @param in the stream from which to read the serialized algorithm
     * @throws IOException if there was an I/O error reading the stream
     * @throws ClassNotFoundException if some of the classes stored in the stream cannot be resolved
     */
    private void readObjectVer4(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int port = in.readInt();
        int broadcastPort = in.readInt();

        try {
            // Reopen file channel (set it through reflection to overcome the "final" flag)
            Field field = DistributedAlgorithm.class.getDeclaredField("messageDisp");
            field.setAccessible(true);
            field.set(this, new MessageDispatcher(port, broadcastPort));
        } catch (Exception e) {
            throw new IOException(e.toString());
        }
    }


    //****************** Destructor ******************//

    @Override
    public void finalize() throws Throwable {
        messageDisp.closeSockets();
        super.finalize();
    }


    //****************** Name enrichment ******************//

    /**
     * Returns the name of this algorithm with host:port of its message dispatcher
     * @return the name of this algorithm with host:port of its message dispatcher
     */
    @Override
    public String getName() {
        return super.getName() + " at " + getThisNode().getHost().getHostName() + ":" + getThisNode().getPort();
    }


    //****************** Message dispatcher ******************//

    /**
     * Returns the network node of this distributed algorithm.
     * @return the network node of this distributed algorithm
     */
    public NetworkNode getThisNode() {
        return messageDisp.getNetworkNode();
    }

    /**
     * Returns the message dispatcher of this distributed algorithm.
     * @return the message dispatcher of this distributed algorithm
     */
    public MessageDispatcher getMessageDispatcher() {
        return messageDisp;
    }


    //****************** Operation execution ******************//
    
    /**
     * Execute algorithm operation from received message.
     * @param msg the received message that holds the operation
     * @throws AlgorithmMethodException if there was an error executing operation
     */
    protected void receiveRequest(DistAlgRequestMessage msg) throws AlgorithmMethodException {
        try {
            execute(false, msg.getOperation(), msg);
        } catch (Exception e) {
            throw new AlgorithmMethodException(e);
        }
    }

    /**
     * Given a just-arrived message, this method registers (binds) DC, DC.Savings and BlockReads statistics
     *   for current thread.
     * @param msg new arrived message
     * @return collection of registered statistics
     * @throws InstantiationException
     */
    protected Collection<Statistics> setupMessageStatistics(Message msg) throws InstantiationException {
        Collection<Statistics> registeredStats = new ArrayList<Statistics>();
        registeredStats.add(msg.registerBoundStat("DistanceComputations"));
        registeredStats.add(msg.registerBoundStat("DistanceComputations.Savings"));
        registeredStats.add(msg.registerBoundStat("BlockReads"));

        return registeredStats;
    }

    /**
     * Unbind given statistics.
     * @param stats set of stats to unbind
     */
    protected void deregisterMessageStatistics(Collection<Statistics> stats) {
        if (stats == null)
            return;
        for (Statistics statistics : stats) {
            statistics.unbind();
        }
    }

    /**
     * Execute operation on this algorithm.
     * @param <T> the type of executed operation
     * @param operation the operation to execute on this algorithm
     * @return the executed operation (same as the argument)
     * @throws AlgorithmMethodException if the execution has thrown an exception
     * @throws NoSuchMethodException if the operation is unsupported (there is no method for the operation)
     */
    @Override
    public <T extends AbstractOperation> T executeOperation(T operation) throws AlgorithmMethodException, NoSuchMethodException {
        execute(Statistics.isEnabledGlobally(), operation, null);
        return operation;
    }

    /**
     * Execute algorithm operation on background.
     * <i>Note:</i> Method {@link #waitBackgroundExecuteOperation} MUST be called in the future to release resources.
     * @param operation the operation to execute on this algorithm
     * @param updateStatistics set to <tt>true</tt> if the operations statistic should be updated after the operation finishes its background execution
     * @throws NoSuchMethodException if the operation is unsupported (there is no method for the operation)
     */
    @Override
    public void backgroundExecuteOperation(AbstractOperation operation, boolean updateStatistics) throws NoSuchMethodException {
        backgroundExecute(updateStatistics, operation, null);
    }


    //****************** Operation method specifier ******************//

    /**
     * This method should return an array of additional parameters that are needed for operation execution.
     * The list must be consistent with the parameters array passed to {@link #execute} and {@link #backgroundExecute}.
     * @return array of additional parameters that are needed for operation execution
     */
    @Override
    protected Class[] getExecutorParamClasses() {
        return new Class[] { AbstractOperation.class, DistAlgRequestMessage.class };
    }


    //****************** Navigation processing support ******************//

    /**
     * Creates a request message used by this algorithm.
     * @param operation the operation for which to create the request message
     * @return a new request message with the specified operation
     */
    protected DistAlgRequestMessage createRequestMessage(AbstractOperation operation) {
        return new DistAlgRequestMessage(operation);
    }

    /**
     * Creates a reply message used by this algorithm.
     * @param msg the request message for which to create a response
     * @return a new reply message for the specified request message
     */
   protected DistAlgReplyMessage createReplyMessage(DistAlgRequestMessage msg) {
        return new DistAlgReplyMessage(msg);
    }

    /**
     * Processes navigation when there will be no local processing.
     * If the request message is specified, it is simply forwarded to the specified node.
     * Otherwise, a new {@link DistAlgRequestMessage} is created, sent to the specified node
     * and the method waits for all the responses. This method blocks until all the
     * responses are gathered.
     * @param operation the operation that is processed
     * @param request the request from which the the operation arrived (the first node has <tt>null</tt> request)
     * @param node the destination node where to forward the request
     * @throws IOException if there was an I/O error during sending or receiving messages
     */
    protected void navigationNoProcessing(AbstractOperation operation, DistAlgRequestMessage request, NetworkNode node) throws IOException {
        // If there is no processing, the forward node cannot be null
        if (node == null)
            throw new IOException("Navigation processing error while evaluating " + operation + ": forwarding to null node requested");

        // If the request is null, this is a first node and thus we will wait for replies
        if (request == null) {
            navigationAfterProcessing(operation, null, messageDisp.sendMessageWaitReply(createRequestMessage(operation), DistAlgReplyMessage.class, true, node));
        } else {
            // Otherwise, just forward the messages
            messageDisp.sendMessage(request, node, true);
        }
    }

    /**
     * Processes navigation when there will be no local processing.
     * If the request message is specified, it is simply forwarded to all the nodes.
     * Otherwise, a new {@link DistAlgRequestMessage} is created, sent to the specified nodes
     * and the method waits for all the responses. This method blocks until all the
     * responses are gathered.
     * @param operation the operation that is processed
     * @param request the request from which the the operation arrived (the first node has <tt>null</tt> request)
     * @param nodes the destination nodes where to forward the request
     * @throws IOException if there was an I/O error during sending or receiving messages
     */
    protected void navigationNoProcessing(AbstractOperation operation, DistAlgRequestMessage request, Collection<NetworkNode> nodes) throws IOException {
        // If there is no processing, the forward node cannot be null
        if (nodes == null || nodes.isEmpty())
            throw new IOException("Navigation processing error while evaluating " + operation + ": forwarding to null nodes requested");

        // If the request is null, this is a first node and thus we will wait for replies
        if (request == null) {
            navigationAfterProcessing(operation, null, messageDisp.sendMessageWaitReply(createRequestMessage(operation), DistAlgReplyMessage.class, true, nodes));
        } else {
            // Otherwise, just forward the messages
            messageDisp.sendMessage(request, nodes, true);
        }
    }

    /**
     * Processes navigation before the local processing.
     * If the request message is specified, it is simply forwarded to all the nodes.
     * Otherwise, a new {@link DistAlgRequestMessage} is created, sent to the specified nodes
     * and the reply receiver for waiting for their responses is returned (i.e. the processing is not blocked).
     * @param operation the operation that is processed
     * @param request the request from which the the operation arrived (the first node has <tt>null</tt> request)
     * @param nodes the destination nodes where to forward the request
     * @return receiver that allows waiting for the responses
     * @throws IOException if there was an I/O error during sending messages
     */
    protected ReplyReceiver<? extends DistAlgReplyMessage> navigationBeforeProcessing(AbstractOperation operation, DistAlgRequestMessage request, Collection<NetworkNode> nodes) throws IOException {
        if (nodes == null) {
            // If the request is null, this is a first node and thus we will wait for replies
            if (request == null) {
                return messageDisp.sendMessageWaitReply(createRequestMessage(operation), DistAlgReplyMessage.class, true, nodes);
            } else {
                // Otherwise, just forward the messages
                messageDisp.sendMessage(request, nodes, false);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Processes navigation before the local processing.
     * If the request message is specified, it is simply forwarded to the specified node.
     * Otherwise, a new {@link DistAlgRequestMessage} is created, sent to the specified node
     * and the reply receiver for waiting for their responses is returned (i.e. the processing is not blocked).
     * @param operation the operation that is processed
     * @param request the request from which the the operation arrived (the first node has <tt>null</tt> request)
     * @param node the destination node where to forward the request
     * @return receiver that allows waiting for the responses
     * @throws IOException if there was an I/O error during sending messages
     */
    protected ReplyReceiver<? extends DistAlgReplyMessage> navigationBeforeProcessing(AbstractOperation operation, DistAlgRequestMessage request, NetworkNode node) throws IOException {
        if (node != null) {
            // If the request is null, this is a first node and thus we will wait for replies
            if (request == null) {
                return messageDisp.sendMessageWaitReply(createRequestMessage(operation), DistAlgReplyMessage.class, true, node);
            } else {
                // Otherwise, just forward the messages
                messageDisp.sendMessage(request, node, false);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Processes navigation after the local processing.
     * If the receiver is <tt>null</tt> and the request is not <tt>null</tt>, a reply is created and sent back.
     * Otherwise, the receiver is used to wait for all the responses and this method blocks until they are gathered.
     * @param operation the operation that is processed
     * @param request the request from which the the operation arrived (the first node has <tt>null</tt> request)
     * @param receiver the receiver that is used for waiting for messages (can be <tt>null</tt>)
     * @return the number of messages received and processed
     * @throws IOException if there was an I/O error during sending messages
     */
    protected int navigationAfterProcessing(AbstractOperation operation, DistAlgRequestMessage request, ReplyReceiver<? extends DistAlgReplyMessage> receiver) throws IOException {
        if (receiver != null) {
            try {
                List<? extends DistAlgReplyMessage> replies = receiver.getReplies();
                mergeOperationsFromReplies(operation, replies);
                mergeStatisticsFromReplies(OperationStatistics.getLocalThreadStatistics(), replies);
                return replies.size();
            } catch (InterruptedException e) {
                throw new IOException("Interrupted while waiting for replies (" + receiver + ")");
            }
        } else {
            if (request != null)
                messageDisp.replyMessage(createReplyMessage(request));
            return 0;
        }
    }


    //****************** Reply merging functions ******************//

    /** Update supplied operation answer with partial answers from reply messages
     *  @param targetOperation the operation that should be updated
     *  @param replyMessages the list of reply messages received with partial answers
     */
    public static void mergeOperationsFromReplies(AbstractOperation targetOperation, Collection<? extends DistAlgReplyMessage> replyMessages) {
        for (DistAlgReplyMessage msg : replyMessages) {
            targetOperation.updateFrom(msg.getOperation());
        }
    }

    /** Update supplied statistics with partial statistics from reply messages
     *  @param targetStatistics the operation statistics object that should be updated
     *  @param replyMessages the list of reply messages received with partial statistics
     */
    public void mergeStatisticsFromReplies(OperationStatistics targetStatistics, Collection<? extends ReplyMessage> replyMessages) {
        mergeStatisticsFromReplies(targetStatistics, replyMessages, OperationStatistics.getLocalThreadStatistics());
    }
    
    /** Update supplied statistics with partial statistics from reply messages
     *  @param targetStatistics the operation statistics object that should be updated
     *  @param replyMessages the list of reply messages received with partial statistics
     *  @param localStats statistics of local processing of the operation on this node
     */
    public void mergeStatisticsFromReplies(OperationStatistics targetStatistics, Collection<? extends ReplyMessage> replyMessages, OperationStatistics localStats) {
        if (replyMessages.isEmpty())
            return;

        StatisticCounter totalMsgs = targetStatistics.getStatisticCounter("Total.Messages");
        
        // first, create the nodesMap - maximum over node's DCs on given positions in all replies
        SortedMap<Integer, Map<NetworkNode, OperationStatistics>> nodesMap = new TreeMap<Integer, Map<NetworkNode, OperationStatistics>>();
        if (localStats != null) {
            Map<NetworkNode, OperationStatistics> statsOfFirstSender = new HashMap<NetworkNode, OperationStatistics>();
            statsOfFirstSender.put(getThisNode(), localStats);
            nodesMap.put(0, statsOfFirstSender);
        }
        for (ReplyMessage reply : replyMessages) {
            // create a map of node->DC taken as max over all nodes in all replies
            int i = 0;
            NetworkNode previousNode = null;
            for (Iterator<NavigationElement> it = reply.getPathElements(); it.hasNext(); i++) {
                NavigationElement el = it.next();
                Map<NetworkNode, OperationStatistics> positionMap = nodesMap.get(i);
                if (positionMap == null)
                    nodesMap.put(i, positionMap = new HashMap<NetworkNode, OperationStatistics>());
                
                NetworkNode node = el.getSender();
                OperationStatistics actualNodeStatistics = positionMap.get(node);
                if (actualNodeStatistics != null) { // sum the DC of peers that appear more than once in the navigation path
                    //positionMap.put(node, Math.max(actualNodeStatistics, (el.getStatistics()==null)?0:el.getStatistics().getStatisticCounter("NavigationElement.DistanceComputations").get()));
                    if ((el.getStatistics() != null) && 
                        (actualNodeStatistics.getStatisticCounter("NavigationElement.DistanceComputations").get() < el.getStatistics().getStatisticCounter("NavigationElement.DistanceComputations").get())) {
                        positionMap.put(node, el.getStatistics());
                    }
                } else {
                    //positionMap.put(node, (el.getStatistics()==null)?0:el.getStatistics().getStatisticCounter("NavigationElement.DistanceComputations").get());
                    if (el.getStatistics() != null) {
                        positionMap.put(node, el.getStatistics());
                    }
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
        StatisticCounter totalDC = targetStatistics.getStatisticCounter("Total.DistanceComputations");
        StatisticCounter totalDCSavings = targetStatistics.getStatisticCounter("Total.DistanceComputations.Savings");
        StatisticCounter totalBlockReads = targetStatistics.getStatisticCounter("Total.BlockReads");
        for (Map<NetworkNode, OperationStatistics> position : nodesMap.values()) {
            for (Map.Entry<NetworkNode, OperationStatistics> nodeMaxDC : position.entrySet()) {
                long value = nodeMaxDC.getValue().getStatisticCounter("NavigationElement.DistanceComputations").get();
                peersDC.add(new NetworkNode(nodeMaxDC.getKey(), false), value);
                totalDC.add(value);
                value = nodeMaxDC.getValue().getStatisticCounter("NavigationElement.DistanceComputations.Savings").get();
                totalDCSavings.add(value);
                value = nodeMaxDC.getValue().getStatisticCounter("NavigationElement.BlockReads").get();
                totalBlockReads.add(value);
            }
            // calculate total messages as sum of sizes of the positions in the "nodesMap"
        }

        // statistics
        StatisticRefCounter peersParDC = targetStatistics.getStatisticRefCounter("PeersParallel.DistanceComputations");
        StatisticCounter hopCount = targetStatistics.getStatisticCounter("HopCount");
        
        // iterate over the replies again and calculate parallel DC for all hosts
        for (ReplyMessage reply : replyMessages) {
            // create a map of node->DC because messages can create loops (in the terms of "hosts")
            Set<NetworkNode> visitedPeers = new HashSet<NetworkNode>();
            long parDCs = 0;
            int replyLength = 0;
            NetworkNode previousHost = null;
            for (Iterator<NavigationElement> it = reply.getPathElements(); it.hasNext(); ) {
                NavigationElement el = it.next();
                NetworkNode peer = new NetworkNode(el.getSender(), false);
                if (visitedPeers.add(peer))
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
                
        // the peers that answered (are at the end of the paths) are put into a separate map
        StatisticRefCounter answeringPeers = targetStatistics.getStatisticRefCounter("AnsweringPeers.DistanceComputations");
        for (ReplyMessage reply : replyMessages) {
            NetworkNode peer = new NetworkNode(reply.getSender(), false);
            answeringPeers.add(peer, peersDC.get(peer));
        }
        if ((localStats != null) && (localStats.getStatisticCounter("DistanceComputations").get() > 0)) {
            answeringPeers.add(getThisNode(), localStats.getStatisticCounter("DistanceComputations").get());
        }
        // add statistic "AnsweringNodes"
        targetStatistics.getStatisticCounter("AnsweringPeers").add(answeringPeers.getKeyCount());
        
        // remove the hosts parallel statistic and
        targetStatistics.removeStatistic("PeersParallel.DistanceComputations");
        targetStatistics.removeStatistic("Peers.DistanceComputations");        
    }
    
}
