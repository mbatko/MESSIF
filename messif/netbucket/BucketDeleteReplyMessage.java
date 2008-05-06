/*
 * BucketDeleteReplyMessage.java
 *
 * Created on 4. kveten 2003, 13:50
 */

package messif.netbucket;

import messif.network.Message;
import messif.network.NetworkNode;
import messif.network.ReplyMessage;
import messif.utility.ErrorCode;

/**
 *
 * @author  xbatko
 */
public class BucketDeleteReplyMessage extends ReplyMessage {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /****************** Message extensions ******************/
    protected final boolean deleted;
    
    public boolean isDeleted() {
        return deleted;
    }
    
    /****************** Constructors ******************/
    
    /** Creates a new instance of BucketDeleteReplyMessage from supplied data */
    public BucketDeleteReplyMessage(BucketDeleteRequestMessage responseToMessage, boolean deleted) {
        super(responseToMessage);
        this.deleted = deleted;
    }
       
}
