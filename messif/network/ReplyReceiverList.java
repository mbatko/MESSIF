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
 *
 * @author xbatko
 */
public class ReplyReceiverList implements Receiver {
    
    /** List of receivers currently registered */
    protected Map<Message, ReplyReceiver> receivers;
    
    /****************** Constructors ******************/

    /** Creates a new instance of ReplyReceiverList */
    public ReplyReceiverList() {
        this.receivers = new HashMap<Message, ReplyReceiver>();
    }

    /****************** Reply waiting creator ******************/
    
    public synchronized <E extends ReplyMessage> ReplyReceiver<E> createReplyReceiver(Message msg, Class<E> replyClass, boolean removeOnAccept) throws IllegalArgumentException {
        // Get receiver for message msg from internal registry
        ReplyReceiver receiver = receivers.get(msg);
        
        // If receiver is not found, create a new one
        if (receiver == null) {
            ReplyReceiver<E> newReceiver = new ReplyReceiver<E>(replyClass, removeOnAccept?null:this);
            receivers.put(msg, newReceiver);
            return newReceiver;
        } else if (replyClass.isAssignableFrom(receiver.replyClass)) {
            // Must check if it is responsible for the reply class
            return (ReplyReceiver<E>)receiver; // this cast IS checked on the previous line
        } else throw new IllegalArgumentException("There is a receiver waiting for this message, but has incompatible reply class");
    }

    protected synchronized boolean deregisterReceiver(ReplyReceiver receiver) {
        Iterator<ReplyReceiver> iter = receivers.values().iterator();
        while (iter.hasNext())
            if (receiver == iter.next()) {
                iter.remove();
                return true;
            }
        return false;
    }
            
    /****************** Reply accepting creator ******************/

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
