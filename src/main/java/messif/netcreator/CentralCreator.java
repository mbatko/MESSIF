/*
 *  This file is part of MESSIF library.
 *
 *  MESSIF library is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MESSIF library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MESSIF library.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.netcreator;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import messif.network.MessageDispatcher;
import messif.network.NetworkNode;
import messif.network.ThreadInvokingReceiver;

/**
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class CentralCreator extends NetworkNodeDispatcher {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    /****************** Internal data ******************/
    protected final Set<NetworkNode> pool;
    protected transient ThreadInvokingReceiver receiver;
    protected final NetworkNode centralNode;
    protected boolean active;

    public boolean isActive() {
        return active;
    }

    public NetworkNode getCentralNode() {
        return centralNode;
    }

    /****************** Constructors ******************/

    /** Creates a new "slave" instance of CentralCreator */
    public CentralCreator(MessageDispatcher messageDisp, Startable[] startables, NetworkNode centralNode) throws InstantiationException {
        super(messageDisp, startables);
        
        // Create thread receiver for messages and start receiving
        receiver = new ThreadInvokingReceiver(this, "receive");
        messageDisp.registerReceiver(receiver);

        this.centralNode = centralNode;
        this.pool = null;
        this.active = false;

        try {
            // Send notify "I'm here" to central node
            messageDisp.sendMessage(new MessageImHere(), centralNode);
        } catch (IOException e) {
            throw new InstantiationException(e.getMessage());
        }
    }

    /** Creates a new "master" instance of CentralCreator */
    public CentralCreator(MessageDispatcher messageDisp) throws InstantiationException {
        super(messageDisp, null);

        // Create new pool for this central node
        pool = new HashSet<NetworkNode>();
        this.centralNode = null;
        this.active = true;

        // Create thread receiver for messages and start receiving
        receiver = new ThreadInvokingReceiver(this, "receive");
        messageDisp.registerReceiver(receiver);
    }


    /** Send "I'm used" message on cleanup to remove from other pools */
    @Override
    protected void finalize() throws Throwable, IOException {
        // Send "I'm used message"
        if (centralNode != null)
            messageDisp.sendMessage(new MessageImUsed(), centralNode);
        
        super.finalize();
    }


    @Override
    public void setMessageDispatcher(MessageDispatcher messageDisp) {
        super.setMessageDispatcher(messageDisp);

        // Create thread receiver for messages and start receiving
        receiver = new ThreadInvokingReceiver(this, "receive");
        messageDisp.registerReceiver(receiver);        
    }


    /****************** Nedwork node utilization ******************/

    /** Initialize one of registered free servers, so they can be used */
    @Override
    public synchronized NetworkNode create() throws InstantiationException {
        // If this is the central node
        if (pool != null) {
            // Traverse whole pool
            for (Iterator<NetworkNode> i = pool.iterator(); i.hasNext();) {
                NetworkNode node = i.next();
                i.remove();

                try {
                    // Send "Use" message
                    MessageActivateResponse msg = messageDisp.sendMessageWaitSingleReply(new MessageActivate(null), MessageActivateResponse.class, node);

                    // Get response type
                    if (msg.isSuccess())
                        return node; // Remote server responded positively
                } catch (IOException e) {
                    // Remote server is dead
                } catch (ClassCastException e) {
                    throw new InternalError("This should never happen");
                }
            }

            // All servers full
            throw new InstantiationException("There is no free server available");
        } else {
            // Otherwise, send an activation request to central node
            try {
                MessageActivateResponse msg = (MessageActivateResponse)messageDisp.sendMessageWaitReply(new MessageActivate(null), centralNode).getFirstReply();
                if (msg.isSuccess())
                    return msg.getActivatedNode();
                throw new InstantiationException("There is no free server available");
            } catch (IOException e) {
                throw new InstantiationException("Central node is not responding");
            } catch (ClassCastException e) {
                // this should never happen
                throw new InstantiationException("Central node responded with wrong message");
            }
        }
    }
    
    /** "Use" message recieving */
    public synchronized void receive(MessageActivate msg) throws IOException {
        // If this is a central node receiving activation request
        if (pool != null) {
            try {
                messageDisp.replyMessage(new MessageActivateResponse(create(), msg));
            } catch (InstantiationException e) {
                messageDisp.replyMessage(new MessageActivateResponse(MessageActivateResponse.CANT_START, msg));
            }
        } else if (!isActive()) {
            try {
                start();
                active = true;

                // Send positive response
                messageDisp.replyMessage(new MessageActivateResponse(messageDisp.getNetworkNode(), msg));

                log.info("Activated by: " + msg.getSender());
            } catch (CantStartException e) {
                log.log(Level.SEVERE, e.getClass().toString(), e);

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
        // Ignore if not central mode
        if (pool == null) return;
        
        NetworkNode node = msg.getSender();
        
        // Ignore message from current node
        if (node.equals(messageDisp.getNetworkNode())) return;
        
        // Add network node to the list
        pool.add(node);
        log.info("New free network node: " + node);
    }
    
    /** "I'm used" message recieving */
    public synchronized void receive(MessageImUsed msg) throws IOException {
        // Ignore if not central mode
        if (pool == null) return;
        
        NetworkNode node = msg.getSender();
        
        // Ignore message from current node
        if (node.equals(messageDisp.getNetworkNode())) return;
        
        // Remove server node from the list
        pool.remove(node);
        log.info("Delete free network node: " + node);
    }

}
