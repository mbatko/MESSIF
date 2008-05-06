/*
 * BucketDeleteRequestMessage.java
 *
 * Created on 4. kveten 2003, 13:50
 */

package messif.netbucket;

import messif.network.Message;

/**
 *
 * @author  xbatko
 */
public class BucketDeleteRequestMessage extends Message {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    protected final int bucketID;
    
    public int getBucketID() {
        return bucketID;
    }
    
    /****************** Constructors ******************/
    
    /** Creates a new instance of MessageInitUse from supplied data */
    public BucketDeleteRequestMessage(int bucketID) {
        this.bucketID = bucketID;
    }
    
}
