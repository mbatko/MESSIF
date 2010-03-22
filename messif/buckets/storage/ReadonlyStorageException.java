/*
 *  ReadonlyStorageException
 * 
 */

package messif.buckets.storage;

import messif.buckets.StorageFailureException;

/**
 * Exception that indicates that a write operation was requested on a
 * read-only storage.
 * 
 * @author xbatko
 */
public class ReadonlyStorageException extends StorageFailureException {
    /** class serial id for serialization */
    static final long serialVersionUID = 8785489987624795491L;

    /**
     * Creates a {@code ReadonlyStorageException} with no detail message.
     */
    public ReadonlyStorageException() {
	super(null);
    }

    /**
     * Creates a {@code ReadonlyStorageException} with the specified detail message.
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public ReadonlyStorageException(String message) {
	super(message, null);
    }

}
