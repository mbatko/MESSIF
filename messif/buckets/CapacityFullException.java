/*
 * BucketCapacityFullException.java
 *
 * Created on 4. kveten 2003, 14:36
 */

package messif.buckets;

/**
 * Thrown to indicate that the hard capacity limit was exceeded.
 *
 * @author  xbatko
 */
public class CapacityFullException extends BucketStorageException {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;    

    /**
     * Creates a new instance of <code>BucketCapacityFullException</code> without detail message.
     */
    public CapacityFullException() {
        super(BucketErrorCode.HARDCAPACITY_EXCEEDED, "No free space to allocate");
    }
        
    /**
     * Creates a new instance of <code>BucketCapacityFullException</code> without detail message.
     * @param msg detailed message
     */
    public CapacityFullException(String msg) {
        super(BucketErrorCode.HARDCAPACITY_EXCEEDED, msg);
    }
    
}
