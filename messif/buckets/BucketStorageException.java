/*
 *  BucketStorageException
 * 
 */

package messif.buckets;

/**
 * The ancestor of all <code>Throwables</code> that indicate an illegal
 * condition occurred while operating with buckets.
 * 
 * @author xbatko
 */
public abstract class BucketStorageException extends Exception {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;    

    //****************** Attributes ******************//

    /** Bucket error code associated with this exception */
    private final BucketErrorCode errorCode;


    //****************** Constructors ******************//

    /**
     * Constructs a new exception with <code>null</code> as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     * @param errorCode the bucket error code associated with this exception
     */
    public BucketStorageException(BucketErrorCode errorCode) {
	super();
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param errorCode the bucket error code associated with this exception
     * @param   message   the detail message. The detail message is saved for 
     *          later retrieval by the {@link #getMessage()} method.
     */
    public BucketStorageException(BucketErrorCode errorCode, String message) {
	super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new exception with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * <code>cause</code> is <i>not</i> automatically incorporated in
     * this exception's detail message.
     *
     * @param errorCode the bucket error code associated with this exception
     * @param  message the detail message (which is saved for later retrieval
     *         by the {@link #getMessage()} method).
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     */
    public BucketStorageException(BucketErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new exception with the specified cause and a detail
     * message of <tt>(cause==null ? null : cause.toString())</tt>.
     *
     * @param errorCode the bucket error code associated with this exception
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     */
    public BucketStorageException(BucketErrorCode errorCode, Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
    }


    //****************** Attribute access methods ******************//

    /**
     * Returns the bucket error code associated with this exception.
     * @return the bucket error code associated with this exception
     */
    public BucketErrorCode getErrorCode() {
        return errorCode;
    }

}
