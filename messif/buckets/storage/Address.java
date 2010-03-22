/*
 *  Address
 * 
 */

package messif.buckets.storage;

import java.io.Serializable;
import messif.buckets.BucketStorageException;

/**
 * Interface of a generic storage address.
 * An address can be retrieved by storing an object into a {@link Storage} via
 * the {@link Storage#store} method.
 * 
 * @param <T> the class of objects that this address points to
 * @see Storage
 * @author xbatko
 */
public interface Address<T> extends Serializable {

    /**
     * Reads the object stored at this address from the associated storage.
     * @return the object retrieved
     * @throws BucketStorageException if there was an error reading the data
     */
    public T read() throws BucketStorageException;

    /**
     * Removes the object stored at this address from the associated storage.
     * This operation is optional and need not be implemented.
     * 
     * @throws BucketStorageException if there was an error deleting an object
     * @throws UnsupportedOperationException if this storage does not support removal of objects
     */
    public void remove() throws BucketStorageException, UnsupportedOperationException;

}
