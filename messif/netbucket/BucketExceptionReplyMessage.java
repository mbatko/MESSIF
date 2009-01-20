/*
 * BucketReplyMessage.java
 *
 */

package messif.netbucket;

import messif.buckets.BucketStorageException;

/**
 * Message for returning exception thrown while manipulating objects on a remote bucket.
 * @author xbatko
 */
public class BucketExceptionReplyMessage extends BucketReplyMessage {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Bucket storage exception to pass back to the originator */
    private final BucketStorageException bucketException;
    /** Runtime exception to pass back to the originator */
    private final RuntimeException runtimeException;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of BucketExceptionReplyMessage for a given bucket storage exception.
     * @param message the original message this message is response to
     * @param bucketException the exception to pass back to the originator
     * @throws NullPointerException if the <code>bucketException</code> is <tt>null</tt>
     */
    public BucketExceptionReplyMessage(BucketRequestMessage message, BucketStorageException bucketException) throws NullPointerException {
        super(message);
        if (bucketException == null)
            throw new NullPointerException();
        this.bucketException = bucketException;
        this.runtimeException = null;
    }

    /**
     * Creates a new instance of BucketReplyMessage for a given runtime exception.
     * @param message the original message this message is response to
     * @param runtimeException the exception to pass back to the originator
     * @throws NullPointerException if the <code>runtimeException</code> is <tt>null</tt>
     */
    public BucketExceptionReplyMessage(BucketRequestMessage message, RuntimeException runtimeException) throws NullPointerException {
        super(message);
        if (runtimeException == null)
            throw new NullPointerException();
        this.bucketException = null;
        this.runtimeException = runtimeException;
    }


    //****************** Attribute access methods ******************//

    /**
     * Returns the bucket storage exception stored in this message.
     * If the message has a runtime exception instead, it is thrown instead of returning
     * the bucket storage exception.
     * @return the bucket storage exception
     * @throws RuntimeException if this message contains a {@link RuntimeException}
     */
    public BucketStorageException getException() throws RuntimeException {
        if (runtimeException != null)
            throw runtimeException;
        return bucketException;
    }

}
