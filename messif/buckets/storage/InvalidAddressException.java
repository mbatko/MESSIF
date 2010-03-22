/*
 *  InvalidAddressException
 * 
 */

package messif.buckets.storage;

import messif.buckets.StorageFailureException;

/**
 * Exception that indicates that an invalid address has been used while
 * accessing a storage.
 * 
 * @author xbatko
 */
public class InvalidAddressException extends StorageFailureException {
    /** class serial id for serialization */
    static final long serialVersionUID = 8785487981624795432L;

    /**
     * Creates an {@code InvalidAddressException} with no detail message.
     */
    public InvalidAddressException() {
	super(null);
    }

    /**
     * Creates an {@code InvalidAddressException} with the specified detail message.
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public InvalidAddressException(String message) {
	super(message, null);
    }

}
