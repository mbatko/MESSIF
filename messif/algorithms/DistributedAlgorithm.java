/*
 * DistributedAlgorithm.java
 *
 * Created on 20. kveten 2004, 23:34
 */

package messif.algorithms;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import messif.network.MessageDispatcher;
import messif.network.NetworkNode;
import messif.network.ReplyReceiver;
import messif.operations.AbstractOperation;

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
public abstract class DistributedAlgorithm extends Algorithm {
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
            log.severe(e);
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

    /**
     * Public destructor to stop the algorithm.
     * This should be overriden in order to clean up.
     * @throws Throwable if there was an error finalizing
     */
    @Override
    public void finalize() throws Throwable {
        super.finalize();
        messageDisp.closeSockets();
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
     * Execute operation on this algorithm.
     * @param <T> the type of executed operation
     * @param operation the operation to execute on this algorithm
     * @return the executed operation (same as the argument)
     * @throws AlgorithmMethodException if the execution has thrown an exception
     * @throws NoSuchMethodException if the operation is unsupported (there is no method for the operation)
     */
    @Override
    public <T extends AbstractOperation> T executeOperation(T operation) throws AlgorithmMethodException, NoSuchMethodException {
        execute(false, operation, null);
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
     * The list must be consistent with the {@link #getSingleOperationExecutorParams}.
     * 
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
        for (DistAlgReplyMessage msg : replyMessages)
            targetOperation.updateFrom(msg.getOperation());
    }

}
