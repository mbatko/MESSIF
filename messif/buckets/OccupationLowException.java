/*
 * OccupationLowException.java
 *
 * Created on 13. kveten 2005, 12:05
 */

package messif.buckets;

/**
 * This exception indicates that the removal of an object from bucket is not possible,
 * because the minimal capacity limit was reached.
 * @author xbatko
 */
public class OccupationLowException extends BucketStorageException {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;    

    /**
     * Creates a new instance of OccupationLowException
     */
    public OccupationLowException() {
        super(BucketErrorCode.LOWOCCUPATION_EXCEEDED);
    }
    
    /**
     * Creates a new instance of OccupationLowException with the specified detail message.
     *
     * @param msg the detail message
     */
    public OccupationLowException(String msg) {
        super(BucketErrorCode.LOWOCCUPATION_EXCEEDED, msg);
    }
    
}
