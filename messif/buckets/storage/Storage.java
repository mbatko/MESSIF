/*
 *  Storage
 * 
 */

package messif.buckets.storage;

import java.io.Serializable;
import messif.buckets.BucketStorageException;

/**
 * Interface of a generic storage.
 * The {@link #store} method stores the provided object into the storage
 * and returns its address. This address can be used to retrieve or remove the
 * object back at any time later.
 * 
 * @param <T> the class of objects stored in this storage
 * @author xbatko
 */
public interface Storage<T> extends Serializable {

    /**
     * Stores an object in this storage.
     * The address returned by this call can be used to retrieve or remove the object.
     * 
     * @param object the object to store
     * @return the address where the object has been stored
     * @throws BucketStorageException if there was an error writing the data
     */
    public Address<T> store(T object) throws BucketStorageException;

}
