/*
 * DuplicateObjectException.java
 *
 */

package messif.buckets;

/**
 * Thrown to indicate that the bucket already contains the inserted object.
 *
 * @author  xbatko
 */
public class DuplicateObjectException extends BucketStorageException {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;    

    /**
     * Creates a new instance of <code>DuplicateObjectException</code> without detail message.
     */
    public DuplicateObjectException() {
        super(BucketErrorCode.OBJECT_DUPLICATE);
    }

    /**
     * Constructs an instance of <code>DuplicateObjectException</code> with the specified detail message.
     * @param msg the detail message
     */
    public DuplicateObjectException(String msg) {
        super(BucketErrorCode.OBJECT_DUPLICATE, msg);
    }
}
