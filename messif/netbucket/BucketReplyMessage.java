/*
 * BucketReplyMessage.java
 *
 */

package messif.netbucket;

import messif.network.ReplyMessage;

/**
 * Generic message for returning results of an object manipulation on a remote bucket.
 * @author xbatko
 */
public class BucketReplyMessage extends ReplyMessage {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Constructors ******************//

    /**
     * Creates a new instance of BucketReplyMessage.
     * @param message the original message this message is response to
     */
    protected BucketReplyMessage(BucketRequestMessage message) {
        super(message);
    }

}
