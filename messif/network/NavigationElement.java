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

import java.io.Serializable;
import java.util.Collection;
import messif.statistics.OperationStatistics;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a message <i>hop</i> from one network node to another one.
 * The navigation element is automatically created whenever a message is sent over the network.
 * The source and the destination network node is recorded as well as the list of other
 * nodes, where the message has been forwarded so far and the nodes to wait await reply from and so on.
 * 
 * <p>
 * Also, the navigation element can contain the {@link OperationStatistics} from the sender node.
 * However, the statistics are added only if requested.
 * </p>
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class NavigationElement implements Serializable {
    /** class id for serialization */
    private static final long serialVersionUID = 2L;


    /****************** Attributes ******************/

    /** The sender of the message for this particular <i>hop</i> */
    private NetworkNode sender = null;
    /** The destination of the message for this particular <i>hop</i> */
    private NetworkNode destination = null;
    /** The destinations, where the message was sent so far using the {@link MessageDispatcher#sendMessage sendMessage} during this particular <i>hop</i> */
    private final Set<NetworkNode> notWaitingDestinations = new HashSet<NetworkNode>();
    /** The destinations, where the message was sent so far using the {@link MessageDispatcher#sendMessageWaitReply sendMessageWaitReply} during this particular <i>hop</i> */
    private final Set<NetworkNode> waitingDestinations = new HashSet<NetworkNode>();
    /** The flag if the message was sent as reply */
    private boolean reply = false;
    /** The flag if this message was marked to skip the message waiting for the sender node */
    private boolean skipWaiting = false;
    
    /** OperationStatistics gathered during this particular <i>hop</i> */
    protected OperationStatistics statistics = null;


    /****************** Constructors ******************/

    /** Create new instance of NavigationElement */
    protected NavigationElement() {
    }

    /**
     * Create new instance of NavigationElement and copy all attributes from sourceElement
     * @param sourceElement the source NavigationElement to copy attributes from
     */
    protected NavigationElement(NavigationElement sourceElement) {
        this.sender = sourceElement.sender;
        this.notWaitingDestinations.addAll(sourceElement.notWaitingDestinations);
        this.waitingDestinations.addAll(sourceElement.waitingDestinations);
        this.reply = sourceElement.reply;
        this.skipWaiting = sourceElement.skipWaiting;
        this.statistics = sourceElement.statistics;
        this.destination = sourceElement.destination;
    }


    /****************** Attribute getters ******************/

    /**
     * Returns the OperationStatistics gathered during this particular <i>hop</i>.
     * @return the OperationStatistics gathered during this particular <i>hop</i>
     */
    public final OperationStatistics getStatistics() {
        return statistics;
    }

    /**
     * Returns the sender of the message for this particular <i>hop</i>.
     * @return the sender of the message for this particular <i>hop</i>
     */
    public final NetworkNode getSender() {
        return sender;
    }

    /**
     * Returns the destination of the message for this particular <i>hop</i>.
     * @return the destination of the message for this particular <i>hop</i>
     */
    public final NetworkNode getDestination() {
        return destination;
    }

    /**
     * Returns <tt>true</tt> if the node for this navigation element is waiting for reply.
     * @return <tt>true</tt> if the node for this navigation element is waiting for reply
     */
    public final boolean isWaiting() {
        return waitingDestinations.size() > 0;
    }

    /**
     * Returns <tt>true</tt> if the node for this navigation element is replying.
     * @return <tt>true</tt> if the node for this navigation element is replying
     */
    public final boolean isReply() {
        return reply;
    }

    /**
     * Returns <tt>true</tt> if the node for this navigation element is skiping reply waiting.
     * @return <tt>true</tt> if the node for this navigation element is skiping reply waiting
     */
    public final boolean isSkipping() {
        return skipWaiting;
    }


    /**
     * Returns the list of destinations that this path element's node doesn't wait for reply from.
     * @return the list of destinations that this path element's node doesn't wait for reply from
     */
    final Set<NetworkNode> getNotWaitingDestinations() {
        return notWaitingDestinations;
    }


    /****************** Attribute setters ******************/

    /**
     * Sets the reference to the OperationStatistics gathered during this particular <i>hop</i>.
     * @param statistics the reference to the OperationStatistics
     */
    protected void setStatistics(OperationStatistics statistics) {
        this.statistics = statistics;
    }

    /**
     * Sets the sender of the message for this particular <i>hop</i>.
     * @param sender the sender of the message
     * @throws IllegalArgumentException if the message was already sent from another node
     */
    protected void setSender(NetworkNode sender) throws IllegalArgumentException {
        if (this.sender != null && !this.sender.equals(sender))
            throw new IllegalArgumentException("This message was already sent from " + this.sender + ". Trying to resend it from other node " + sender);
        
        this.sender = sender;
    }

    /**
     * Sets this particular <i>hop</i> as a reply to the destination.
     * @param destination the destination this reply is going to
     */
    protected void setReply(NetworkNode destination) {
        if (reply)
            throw new IllegalArgumentException("This message was already replied");
        if (this.skipWaiting)
            throw new IllegalArgumentException("This message promised not to reply");

        reply = true;
        this.destination = destination;
    }

    /**
     * Adds the node to the list of waiting nodes at this particular <i>hop</i>.
     * That is, the message was sent using sendMessageWaitReply method.
     * @param destination the destination this message is going to
     */
    protected void addWaitingDestination(NetworkNode destination) {
        this.destination = destination;
        waitingDestinations.add(destination);
    }

    /**
     * Adds the node to the list of forwarded nodes at this particular <i>hop</i>.
     * That is, the message was sent using sendMessage method.
     * @param destination the destination this message is going to
     */
    protected void addNotWaitingDestination(NetworkNode destination) {
        this.destination = destination;
        notWaitingDestinations.add(destination);
    }

    /**
     * Flag that the sender of this particular <i>hop</i> will not reply.
     */
    protected void skipWaiting() {
        skipWaiting = true;
    }
    
    /**
     * Returns the string representation of this navigation element.
     * @return the string representation of this navigation element
     */
    @Override
    public String toString() {
        StringBuffer strbuf = new StringBuffer();
        if (reply)
            strbuf.append("REPLY: ");
        else {
            strbuf.append("FORWARD ");
            if (skipWaiting)
                strbuf.append("(don't wait): ");
            else strbuf.append("(will answer): ");
        }
        strbuf.append("from: " + sender + " to: " + destination);
        if (! notWaitingDestinations.isEmpty())
            strbuf.append("; not waiting dests: "+notWaitingDestinations);
        if (! waitingDestinations.isEmpty())
            strbuf.append("; waiting dests: "+waitingDestinations);
        
        return strbuf.toString();
    }
}
