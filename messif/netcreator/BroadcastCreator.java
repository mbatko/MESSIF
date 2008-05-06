/*
 * NetworkNodePool.java
 *
 * Created on 1. kveten 2004, 14:37
 */

package messif.netcreator;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import messif.network.Message;
import messif.network.MessageDispatcher;
import messif.network.NetworkNode;
import messif.network.ThreadInvokingReceiver;

/**
 *
 * @author  xbatko
 */
public class BroadcastCreator extends NetworkNodeDispatcher {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    /****************** Internal data ******************/
    protected Set<NetworkNode> pool = null;
    protected transient ThreadInvokingReceiver receiver;

    /** Returns operation mode of the pool (active or passive) */
    public boolean isActive() { return pool != null; }
    
    /****************** Constructors ******************/

    /** Creates a new "passive" or "active" instance of NetworkNodePool */
    public BroadcastCreator(MessageDispatcher messageDisp, Startable[] startables) throws InstantiationException {
        super(messageDisp, startables);
        
        // Create thread receiver for messages and start receiving
        receiver = new ThreadInvokingReceiver(this, "receive");
        messageDisp.registerReceiver(receiver);

        try {
            // Automatically in active mode if startables are null (i.e. nothing to start)
            if (startables != null) {
                // Send notify "I'm here" to others
                messageDisp.sendMessage(new MessageImHere());
            } else {
                // Activate receiving without ImUsed message and empty pool
                activate(new HashSet<NetworkNode>(), false);
            }
        } catch (IOException e) {
            throw new InstantiationException(e.getMessage());
        } catch (CantStartException e) {
            throw new InstantiationException(e.getMessage());
        }
    }
    
    /** Send "I'm used" message on cleanup to remove from other pools */
    protected void finalize() throws Throwable, IOException {
        // Send "I'm used message"
        messageDisp.sendMessage(new MessageImUsed());
        
        super.finalize();
    }

    public void setMessageDispatcher(MessageDispatcher messageDisp) {
        super.setMessageDispatcher(messageDisp);

        // Create thread receiver for messages and start receiving
        receiver = new ThreadInvokingReceiver(this, "receive");
        messageDisp.registerReceiver(receiver);        
    }
    
    /****************** Activation routine ******************/
        
    /** Internal handler used to change from passive (not used) to active (used) state */
    protected synchronized void activate(Set<NetworkNode> newPool, boolean sendUsed) throws IOException, CantStartException {
        // Initialize pool holder
        if (newPool == null) throw new IOException("Can't activate with null initial list");
        if (isActive()) throw new IOException("Can't activate pool twice");

        // Send "I'm used message"
        if (sendUsed)
            messageDisp.sendMessage(new MessageImUsed());

        // Remove our node from the pool and set pool holder
        newPool.remove(messageDisp.getNetworkNode());
        pool = newPool;
        
        // Start all startables
        start();
    }
    
    
    /****************** Nedwork node utilization ******************/

    /** Initialize one of registered free servers, so they can be used */
    public synchronized NetworkNode create() throws InstantiationException {
        if (!isActive()) throw new InstantiationException("Can't create nodes in passive mode");
        
        // Traverse whole pool
        for (Iterator<NetworkNode> i = pool.iterator(); i.hasNext();) {
            NetworkNode node = i.next();
            i.remove();

            try {
                // Send "Use" message
                Message msg = messageDisp.sendMessageWaitReply(new MessageActivate(pool), node).getFirstReply();

                // Get response type
                if ((msg instanceof MessageActivateResponse) && ((MessageActivateResponse)msg).isSuccess())
                    return node; // Remote server responded positively
            } catch (IOException e) {} // Remote server is dead
        }

        // All servers full
        throw new InstantiationException("There is no free server available");
    }
    
    /** "Use" message recieving */
    public synchronized void receive(MessageActivate msg) throws IOException {
        if (!isActive()) {
            try {
                // Switch to active
                activate(msg.getServerList(), true);

                // Send positive response
                messageDisp.replyMessage(new MessageActivateResponse(MessageActivateResponse.ACTIVATED, msg));

                log.info("Activated by: " + msg.getSender());
            } catch (CantStartException e) {
                log.severe(e);

                // Send negative response
                messageDisp.replyMessage(new MessageActivateResponse(MessageActivateResponse.CANT_START, msg));
            }
        } else {
            // Already active
            messageDisp.replyMessage(new MessageActivateResponse(MessageActivateResponse.ALREADY_ACTIVE, msg));
        }
    }


    /****************** Pool add/remove messages receiving ******************/
    
    /** "I'm here" message recieving */
    public synchronized void receive(MessageImHere msg) throws IOException {
        // Ignore if not in active mode
        if (!isActive()) return;
        
        NetworkNode node = msg.getSender();
        
        // Ignore message from current node
        if (node.equals(messageDisp.getNetworkNode())) return;
        
        // Add network node to the list
        pool.add(node);
        log.info("New free network node: " + node);
    }
    
    /** "I'm used" message recieving */
    public synchronized void receive(MessageImUsed msg) throws IOException {
        // Ignore if not in active mode
        if (!isActive()) return;
        
        NetworkNode node = msg.getSender();
        
        // Ignore message from current node
        if (node.equals(messageDisp.getNetworkNode())) return;
        
        // Remove server node from the list
        pool.remove(node);
        log.info("Delete free network node: " + node);
    }

}
