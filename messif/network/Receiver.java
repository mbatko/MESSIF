/*
 * Receiver.java
 *
 * Created on 15. kveten 2003, 12:04
 */

package messif.network;

/**
 * Receiver allows to accept messages received by the {@link MessageDispatcher}.
 * Once a receiver is registered through {@link MessageDispatcher#registerReceiver registerReceiver}
 * method, every message that arrives at the dispatcher is passed to the registered receivers through
 * {@link #acceptMessage acceptMessage} in sequence until a receiver accepts the message.
 *
 * @see MessageDispatcher
 * @author  xbatko
 */
public interface Receiver {

    /**
     * Offers a message to this receiver for acceptance.
     * This method is called by {@link MessageDispatcher} when new message arrives.
     * <tt>True</tt> is returned, if this receiver accepts the offered message
     * (and the processing of the message is stopped at message dispatcher's level).
     *
     * @param msg the message offered for acceptance
     * @param allowSuperclass First, the message is offered with <tt>allowSuperclass</tt> set to <tt>false</tt>.
     *                        If no receiver accepts it, another offering round is issued with <tt>allowSuperclass</tt>
     *                        set to <tt>true</tt> (so the receiver can relax its acceptance conditions).
     * @return <tt>true</tt> if the message is accepted by this receiver; otherwise, the processing
     *         continues by offering the message to the next receiver
     */
    public boolean acceptMessage(Message msg, boolean allowSuperclass);
        
}
