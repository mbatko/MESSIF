/*
 * OccupationLowException.java
 *
 * Created on 13. kveten 2005, 12:05
 */

package messif.buckets;

/**
 * This exception indicates that storing or reading object from bucket is not possible
 * due to lower layer storage exception.
 * @author xbatko
 */
public class StorageFailureException extends BucketStorageException {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;    

    /**
     * Creates a new instance of OccupationLowException
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     */
    public StorageFailureException(Throwable cause) {
        super(BucketErrorCode.STORAGE_FAILURE, cause);
    }

    /**
     * Creates a new instance of OccupationLowException with the specified detail message.
     *
     * @param msg the detail message
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     */
    public StorageFailureException(String msg, Throwable cause) {
        super(BucketErrorCode.STORAGE_FAILURE, msg, cause);
    }

    /**
     * Returns a short description of this throwable.
     * If there was a "cause" throwable, it is appended to the string.
     * @return a string representation of this throwable.
     */
    @Override
    public String toString() {
        Throwable cause = getCause();
        String ret = super.toString();
        if (cause != null)
            ret = ret + " (" + cause.toString() + ")";
        return ret;
    }

}
