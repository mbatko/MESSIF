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
package messif.network;

import messif.statistics.OperationStatistics;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import messif.statistics.Statistics;

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
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class Message implements Serializable, Cloneable {

    /** Class version id for serialization */
    private static final long serialVersionUID = 3L;


    //****************** Message ID ******************//

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


    //****************** Navigation Path Attributes ******************//

    /** Navigation path this message has gone through */
    protected final List<NavigationElement> navigationPath = new ArrayList<NavigationElement>();

    /** Actual navigation element */
    protected NavigationElement actualNavigationElement = new NavigationElement();


    //****************** Constructors ******************//

    /**
     * Creates a new instance of Message.
     * The new identifier is assigned to the message.
     */
    protected Message() {
        messageID = lastMessageID.incrementAndGet();
    }

    /**
     * Creates a new instance of Message copying the messageID and the navigation path.
     * This constructor is accessible only from ReplyMessage, because a new message must
     * have a new ID except for the reply message.
     * @param originalMessage the original message from which to get the messageID and the navigation path
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
     * <p>
     * This method <b>must</b> be overridden whenever not-immutable attributes are added to subclasses.
     * The <tt>CloneNotSupportedException</tt> is never thrown by this class, but might not be true for its subclasses.
     * </p>
     *
     * <p>
     * WARNING: The navigation path of this message is not cloned,
     *  it rather holds the same reference for all the clones!
     *  Therefore, adding a path element to one message adds it to all of them.
     *  This is a correct behavior (used by forwarding).
     * </p>
     *
     * @return a clone of this message
     * @throws CloneNotSupportedException if this instance cannot be cloned
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }


    //****************** Serialization (accessible only from within network framework package) ******************//
    
    /**
     * Deserialization from network socket.
     * The actual navigation element is added to the navigation path and
     * a new element is created.
     *
     * @param in the object input stream from which to read this message's data
     * @throws IOException if there was a problem reading the data from the stream
     * @throws ClassNotFoundException if an unknown class was encountered while reading objects from the stream
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // Shift the navigation path actual element, since it is on a new network node
        navigationPath.add(actualNavigationElement);
        actualNavigationElement = new NavigationElement();        
    }


    //****************** Actual Navigation Element Handling ******************//
    
    /**
     * Adds a not-waiting destination to the actual navigation element.
     *
     * @param sender the sender of the message
     * @param destination the destination of the message
     * @param skipWaiting flag that this (sender) node will not provide a reply for this message
     */
    void addNotWaitingDestination(NetworkNode sender, NetworkNode destination, boolean skipWaiting) {
        actualNavigationElement.setSender(sender);
        if (skipWaiting)
            actualNavigationElement.skipWaiting();
        actualNavigationElement.addNotWaitingDestination(destination);
    }

    /**
     * Adds a waiting destination to the actual navigation element.
     *
     * @param sender the sender of the message
     * @param destination the destination of the message
     */
    void addWaitingDestination(NetworkNode sender, NetworkNode destination) {
        actualNavigationElement.setSender(sender);
        actualNavigationElement.addWaitingDestination(destination);
    }
    
    /**
     * Updates this message navigation path to reply and returns the reply destination node.
     * @param sender the sender of the message
     * @return the node that is waiting for a reply to this message
     */
    NetworkNode setReply(NetworkNode sender) {        
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

    /**
     * Set statistics in actual navigation element.
     * A reference to current thread's operation statistics is added.
     * @return the added operation statistics object
     */
    protected OperationStatistics setNavigationPathStatistics() {
        OperationStatistics statistics = OperationStatistics.getLocalThreadStatistics();

        // Register statistics
        actualNavigationElement.setStatistics(statistics);
        
        return statistics;
    }

    /**
     * Add a statistic to the actual navigation element and bind it to some global statistic.
     * @param name the name of a global statistic
     * @param asName the name of the new bound statistic
     * @return the bound statistic of the actual navigation element
     * @throws InstantiationException if the statistic with the specified name was not found in global statistics
     */
    public Statistics registerBoundStat(String name, String asName) throws InstantiationException {
        return setNavigationPathStatistics().registerBoundStat(name, asName);
    }

    /**
     * Add a statistic to the actual navigation element and bind it to some global statistic.
     * String "NavigationElement.<code>name</code>" will be used as a name for the bound statistic.
     *
     * @param name the name of a global statistic
     * @return the bound statistic of the actual navigation element
     * @throws InstantiationException if the statistic with the specified name was not found in global statistics
     */
    public Statistics registerBoundStat(String name) throws InstantiationException {
        return registerBoundStat(name, "NavigationElement." + name);
    }

    /** Deregisters all operation statistics in this thread. */
    public void deregisterOperStats() {
        setNavigationPathStatistics().unbindAllStats();
    }


    //****************** Navigation Path Handling ******************//

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


    //****************** Equality handling ******************//

    /**
     * Indicates whether some other object is "equal to" this message.
     * The {@code obj} is equal to this message if and only if it is an instance of Message
     * that has the same ID and the same original sender.
     *
     * <p>
     * The <code>equals</code> method cannot be overridden to avoid bugs while
     * receiving reply messages. If you need to use a different hashing on messages,
     * use a wrapper class.
     * </p>
     *
     * @param obj the reference object with which to compare
     * @return <code>true</code> if this message is the same as the {@code obj} argument; <code>false</code> otherwise
     */
    @Override
    public final boolean equals(Object obj) {
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
     * The hash code of a message is its ID (the low part of the long).
     *
     * <p>
     * The <code>hashCode</code> method cannot be overridden to avoid bugs while 
     * receiving reply messages. If you need to use a different hashing on messages,
     * use a wrapper class.
     * </p>
     *
     * @return a hash code value for this message
     */
    @Override
    public final int hashCode() {
        return (int)messageID;
    }


    //****************** String representation ******************//

    /**
     * Returns a string representation of this message.
     * @return a string representation of this message
     */
    @Override
    public String toString() {
        return new StringBuffer(getClass().getSimpleName()).append("(ID:").append(messageID).append(", from: ").append(getOriginalSender()).append(")").toString();
    }

}
