/*
 * GossipMessageDispatcher.java
 *
 * Created on September 11, 2006, 14:51
 */

package messif.loadbalancing;

import java.util.WeakHashMap;
import messif.network.Message;
import messif.network.MessageDispatcher;
import messif.network.NetworkNode;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ConcurrentModificationException;
import messif.network.ReplyMessage;

/**
 * This is a message dispatcher enriched by setting gossip information to the
 * messages to be send.
 *
 * @author xnovak8
 */
public class GossipMessageDispatcher extends MessageDispatcher {
    
    /************************ pointer to the host to process the gossip messages ************************/
    
    /** this message dispatcher has link to the "host" which knows the statistics */
    protected final Host host;
    
    
    /*************************  Constructors - enriched by pointer to the host ********************/
    
    /** Creates a new server instance of GossipMessageDispatcher */
    public GossipMessageDispatcher(Host host, int port) throws IOException {
        super(port);
        this.host = host;
    }
    
    /** Creates a new server instance of GossipMessageDispatcher */
    public GossipMessageDispatcher(Host host, int port, int broadcastPort) throws IOException {
        super(port, broadcastPort);
        this.host = host;
    }
    
    /** Creates a new server instance of GossipMessageDispatcher connected to a higher level dispatcher */
    public GossipMessageDispatcher(Host host, MessageDispatcher parentDispatcher, int appendNodeID) {
        super(parentDispatcher, appendNodeID);
        this.host = host;
    }
    
    
    /************************* Process the gossip information in the send and accept methods  *************************/
    
    /** Hash map of the arrived messages + gossips that are to be returned to the msg originator */
    //WeakHashMap<Message, HostList> msgGossips = new WeakHashMap<Message, HostList>();
    
    /** Puts message into an object output stream */
    protected void putMessageIntoStream(Message msg, ObjectOutputStream stream, NetworkNode destinationNode) throws IOException {
        super.putMessageIntoStream(msg, stream, destinationNode);
        Gossip gossip;
        if ((destinationNode == null) || (getNetworkNode().equalsIgnoreNodeID(destinationNode)))
            gossip = null;
        else {
            //if (ReplyMessage.class.isAssignableFrom(msg.getClass()))
            //    gossip = host.gossipModule.getGossip(msgGossips.get(msg));
            //else 
            gossip = host.gossipModule.getGossip();
        }
        stream.writeObject(gossip);
    }
    
    /** Retrieve serialized message from object stream */
    protected Message getMessageFromStream(ObjectInputStream in) throws IOException {
        Message message = super.getMessageFromStream(in);
        try {
            Gossip gossip = (Gossip) in.readObject();
            if (gossip != null)
                host.gossipModule.processGossip(gossip);
                //msgGossips.put(message,host.gossipModule.processGossip(gossip));
        } catch (ClassNotFoundException e) {
            throw new IOException(e.getMessage());
        }
        return message;
    }
    
}
