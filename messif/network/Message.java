/*
 * Message.java
 *
 * Created on 3. kveten 2003, 15:35
 */

package messif.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The ancestor of all messages sent using a {@link MessageDispatcher} for communication with other network nodes.
 * Every message is assigned a per-host unique ID that together with the sender's <code>NetworkNode</code> is univerlally unique
 * identifier. Every message also stores its navigation path, i.e. the list of network nodes it was sent through.
 * 
 * <p>
 * An algorithm usually uses descendants of the <code>Message</code> to perform specific tasks.
 * </p>
 *
 * @see ReplyMessage
 *
 * @author  xbatko
 */
public abstract class Message implements Serializable, Cloneable {

    /** Class version id for serialization */
    private static final long serialVersionUID = 3L;


    /****************** Message ID ******************/

    /** Automatic message ID counter (per network node) */
    private final static AtomicLong lastMessageID = new AtomicLong(1);

    /** The message ID */
    protected final long messageID;

    /**
     * Returns the identifier of this message.
     * @return the identifier of this message
     */
    public long getMessageID() {
        return messageID;
    }


    /****************** Navigation Path Attributes ******************/

    /** Navigation path this message has gone through */
    protected final List<NavigationElement> navigationPath = new ArrayList<NavigationElement>();

    /** Actual navigation element */
    protected NavigationElement actualNavigationElement = new NavigationElement();


    /****************** Constructors ******************/

    /**
     * Creates a new instance of Message.
     * The new identifier is assigned to the message.
     */
    protected Message() {
        messageID = lastMessageID.incrementAndGet();
    }

    /**
     * Creates a new instance of Message copying the messageID and navigation path.
     * This constructor is accessible only from ReplyMessage, because a new message must
     * have a new ID except for the reply message.
     */
    Message(Message originalMessage) { // DO NOT MAKE THIS CONSTRUCTOR PROTECTED OR PUBLIC!!!
        messageID = originalMessage.messageID;
        for (NavigationElement element : originalMessage.navigationPath)
            navigationPath.add(new NavigationElement(element));
        
        actualNavigationElement = new NavigationElement(originalMessage.actualNavigationElement);
    }
    
    /**
     * Returns a clone of this message.
     * This can be useful when forwarding a sightly modified message to different nodes while waiting for the response.
     * 
     * This method <b>must</b> be overriden whenever not-immutable attributes are added to subclasses.
     * The <tt>CloneNotSupportedException</tt> is never thrown by this class, but might not be true for its subclasses.
     * 
     * @return a clone of this message
     * @throws CloneNotSupportedException if this instance cannot be cloned
     */
    public Object clone() throws CloneNotSupportedException {
        /* WARNING: Attribute navigationPath is not clonned, it rather holds the same reference for 
                    all the clones! Therefore, adding path element to one message adds it to all of them.
                    This is correct behaviour for the forwarding. */
        return super.clone();
    }


    /****************** Serialization (accessible only from within network framework package) ******************/
    
    /** Deserialization from network socket */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // Shift the navigation path actual element, since it is on a new network node
        navigationPath.add(actualNavigationElement);
        actualNavigationElement = new NavigationElement();        
    }


    /****************** Actual Navigation Element Handling ******************/
    
    /** */
    protected void addNotWaitingDestination(NetworkNode sender, NetworkNode destination, boolean skipWaiting) {
        actualNavigationElement.setSender(sender);
        if (skipWaiting)
            actualNavigationElement.skipWaiting();
        actualNavigationElement.addNotWaitingDestination(destination);
    }

    /** */
    protected void addWaitingDestination(NetworkNode sender, NetworkNode destination) {
        actualNavigationElement.setSender(sender);
        actualNavigationElement.addWaitingDestination(destination);
    }
    
    /** Updates this message navigation path to reply and returns the reply destination node */
    protected NetworkNode setReply(NetworkNode sender) {        
        // Get traversing (backwards) iterator
        ListIterator<NavigationElement> iterator = navigationPath.listIterator(navigationPath.size());
        
        // Count reply messages
        int replies = 1;
        
        // Traverse the path backwards
        while (iterator.hasPrevious()) {
            NavigationElement element = iterator.previous();
            
            // If reply message is found, increase counter
            if (element.isReply())
                replies++;
            else if (element.isWaiting())
                replies--;
            
            // We have found the first node that has no reply message in the list
            if (replies <= 0) {
                actualNavigationElement.setSender(sender);
                actualNavigationElement.setReply(element.getSender());
                return element.getSender();
            }
        }
        
        // Too many replies or we are at the beginning
        return null;
    }


    /****************** Navigation Path Handling ******************/

    /**
     * Returns the network node from which this message arrived.
     * If this message was not sent yet, <tt>null</tt> is returned.
     *
     * @return the network node from which this message arrived or <tt>null</tt> if this message was not sent yet
     */
    public NetworkNode getSender() {
        if (navigationPath.isEmpty())
            return null;
        return navigationPath.get(navigationPath.size() - 1).getSender();
    }

    /**
     * Returns the network node, on which this message was created and from which it was sent in the begining.
     * If this message was not sent yet, <tt>null</tt> is returned.
     * @return the network node, on which this message was created or <tt>null</tt> if this message was not sent yet
     */
    public NetworkNode getOriginalSender() {
        if (navigationPath.isEmpty())
            return actualNavigationElement.getSender();
        return navigationPath.get(0).getSender();
    }

    /**
     * Returns the network node to which this message arrived.
     * If this message was not sent yet, <tt>null</tt> is returned.
     *
     * @return the network node to which this message arrived or <tt>null</tt> if this message was not sent yet
     */
    public NetworkNode getDestination() {
        if (navigationPath.isEmpty())
            return null;
        return navigationPath.get(navigationPath.size() - 1).getDestination();
    }

    /**
     * Returns an iterator through all navigation path elements
     * @return an iterator through all navigation path elements
     */
    public Iterator<NavigationElement> getPathElements() {
        return navigationPath.iterator();
    }

    /**
     * Returns the length of the navigation path. Zero means that the message was not sent yet.
     * NOTE: The length covers both forwards and replies!
     * @return the length of the navigation path
     */
    public int getNavigationPathLength() {
        return navigationPath.size();
    }

    /**
     * Returns the list of senders stored in the navigation path and the actual sender.
     * @return the list of senders stored in the navigation path and the actual sender
     */
    public List<NetworkNode> getSenderList() {
        List<NetworkNode> path = new ArrayList<NetworkNode>();
        for (NavigationElement el : navigationPath)
            path.add(el.getSender());
        if (actualNavigationElement.getSender() != null)
            path.add(actualNavigationElement.getSender());
        return path;
    }


    /****************** Equality handling ******************/

    /**
     * Indicates whether some other object is "equal to" this message.
     * The obj is equal to this message if and only if it is an instance of Message
     * that has the same ID and the same original sender.
     *
     * @param obj the reference object with which to compare
     * @return <code>true</code> if this message is the same as the obj argument; <code>false</code> otherwise
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Message))
            return false;
        Message msg = (Message)obj;
        if (messageID != msg.messageID)
            return false;
        if (getOriginalSender() == null)
            return msg.getOriginalSender() == null;
        return getOriginalSender().equals(msg.getOriginalSender());
    }

    /**
     * Returns a hash code value for this message. 
     * The hashcode of a message is its ID (the low part of the long).
     *
     * @return a hash code value for this message
     */
    public int hashCode() {
        return (int)messageID;
    }
    
    /****************** String representation ******************/

    /**
     * Returns a string representation of this message.
     * @return a string representation of this message
     */
    public String toString() {
        return new StringBuffer(getClass().getSimpleName()).append("(ID:").append(messageID).append(", from: ").append(getOriginalSender()).append(")").toString();
    }
    
}
