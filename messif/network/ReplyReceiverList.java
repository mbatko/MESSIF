/*
 * ReplyReceiverList.java
 *
 * Created on 10. kveten 2003, 23:54
 */

package messif.network;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * List of {@link ReplyReceiver reply receivers} that are waiting for messages to arrive.
 * This list itself is a receiver that accepts reply message that any of the stored
 * {@link ReplyReceiver reply receivers} accepts. The new receivers are added
 * by supplying the message for which {@link ReplyMessage reply messages} are
 * expected.
 * 
 * @author xbatko
 */
class ReplyReceiverList implements Receiver {

    //****************** Attributes ******************//

    /** List of receivers currently registered */
    protected Map<Message, ReplyReceiver> receivers;


    //****************** Constructors ******************//

    /** Creates a new instance of ReplyReceiverList */
    public ReplyReceiverList() {
        this.receivers = new HashMap<Message, ReplyReceiver>();
    }


    //****************** Reply waiting creator ******************//

    /**
     * Creates a new reply receiver for the given message or returns an existing one.
     * @param <E> the class of {@link ReplyMessage reply messages} that are expected
     * @param msg the message for which {@link ReplyMessage reply messages} are expected
     * @param replyClass the class of {@link ReplyMessage reply messages} that are expected
     * @param removeOnAccept if <tt>true</tt>, the reply receiver is removed from this list when the last message is received
     *          (which is faster, but may lead to unregistering the receiver too early if multiple destinations are used),
     *          otherwise the receiver is unregistered when the {@link ReplyReceiver#getReplies() replies} are first requested.
     * @return the {@link ReplyReceiver reply receiver} that can be used to gather the received {@link ReplyMessage reply messages}
     * @throws ClassCastException if there is already a receiver for the supplied message, but it waits for a different reply class
     */
    public synchronized <E extends ReplyMessage> ReplyReceiver<E> createReplyReceiver(Message msg, Class<E> replyClass, boolean removeOnAccept) throws ClassCastException {
        // Get receiver for message msg from internal registry
        ReplyReceiver<E> receiver = ReplyReceiver.cast(receivers.get(msg), replyClass);
        
        // If receiver is not found, create a new one
        if (receiver == null) {
            receiver = new ReplyReceiver<E>(replyClass, removeOnAccept?null:this);
            receivers.put(msg, receiver);
        }

        return receiver;
    }

    /**
     * Removes the receiver from this list.
     * Note that the same receiver instance as for creation must be supplied.
     *
     * @param receiver the receiver to remove
     * @return <tt>true</tt> if the receiver was in the list (and thus was removed)
     */
    synchronized boolean deregisterReceiver(ReplyReceiver receiver) {
        Iterator<ReplyReceiver> iter = receivers.values().iterator();
        while (iter.hasNext())
            if (receiver == iter.next()) {
                iter.remove();
                return true;
            }
        return false;
    }


    //****************** Reply accepting creator ******************//

    public synchronized boolean acceptMessage(Message msg, boolean allowSuperclass) {
        // Get reply receiver for this message (Message hashcode & equals is ID driven)
        ReplyReceiver receiver = receivers.get(msg);
        if (receiver == null)
            return false;
        
        // We have reply receiver for this message, let's process the message
        if (!receiver.acceptMessage(msg, allowSuperclass))
            return false;
        
        // Remove the receiver from list if it is finished
        if (receiver.isReadyToRemove())
            receivers.remove(msg);
        
        return true;
    }
    
}
