/*
 * BucketRemoveReplyMessage.java
 *
 * Created on 4. kveten 2003, 13:50
 */

package messif.netbucket;

/**
 * Message for returning results of a remote bucket removal.
 * @author  xbatko
 */
public class BucketRemoveReplyMessage extends BucketReplyMessage {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    private final boolean removed;

    //****************** Constructors ******************//
    
    /**
     * Creates a new instance of BucketRemoveReplyMessage for the supplied data.
     *
     * @param message the original message this message is response to
     */
    public BucketRemoveReplyMessage(BucketRemoveRequestMessage message, boolean removed) {
        super(message);
        this.removed = removed;
    }

    public boolean getRemoved() {
        return removed;
    }
}
