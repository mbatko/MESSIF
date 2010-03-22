/*
 *  BucketProcessQueryReplyMessage
 * 
 */

package messif.netbucket;

import messif.operations.QueryOperation;

/**
 * Message for returning results of a query processed on a remote bucket.
 * @author xbatko
 */
public class BucketProcessQueryReplyMessage extends BucketReplyMessage {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Query operation processed on a remote bucket */
    private final QueryOperation query;


    //****************** Constructor ******************//
    
    /**
     * Creates a new instance of BucketProcessQueryReplyMessage for the supplied data.
     *
     * @param message the original message this message is response to
     * @param query the query operation processed on a remote bucket
     */
    public BucketProcessQueryReplyMessage(BucketProcessQueryRequestMessage message, QueryOperation query) {
        super(message);
        this.query = query;
    }


    //****************** Attribute access methods ******************//

    /**
     * Returns the query operation processed on a remote bucket.
     * @return the query operation processed on a remote bucket
     */
    public QueryOperation getQuery() {
        return query;
    }

}
