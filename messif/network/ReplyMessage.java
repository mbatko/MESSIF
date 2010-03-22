/*
 * ReplyMessage.java
 *
 * Created on 3. kveten 2003, 15:35
 */

package messif.network;


/**
 * The ancestor of all reply messages that are returned back during communication with other network nodes.
 * The reply message is alwas a reply for some other message and it inherits the message ID and all
 * the navigation path from the original message. Specifically, the reply message is a continuation
 * of an existing message that can be sent back to a waiting node. Only ReplyMessage can be returned from
 * {@link ReplyReceiver}, thus whenever we want to inform the original node about our findings, we must transform
 * the "forward" message with the ReplyMessage.
 * 
 * @author  xbatko
 */
public abstract class ReplyMessage extends Message {

    /** Class version id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Constructors ******************//

    /**
     * Creates a new instance of ReplyMessage.
     * @param message the original message this message is response to
     */
    protected ReplyMessage(Message message) {
        super(message);
    }


    //****************** Clonning ******************//

    /**
     * Always throws CloneNotSupportedException exception, because conning is not supported for replies.
     * @return nothing, because this method always throws CloneNotSupportedException
     * @throws CloneNotSupportedException if this instance cannot be cloned
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Reply messages cannot be cloned");
    }


    //****************** String representation ******************//

    /**
     * Returns a string representation of this response message.
     * @return a string representation of this response message
     */
    @Override
    public String toString() {
        return "ReplyMessage (ID:" + messageID + ") <<" + getClass().getName() + ">>";
    }

}
