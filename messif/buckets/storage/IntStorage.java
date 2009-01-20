/*
 *  IntStorage
 * 
 */

package messif.buckets.storage;

import messif.buckets.BucketStorageException;

/**
 * Interface for storage that uses int addresses.
 * The {@link #store} method stores the provided object into the storage
 * and returns its address. This address can be used to {@link #readInt read}
 * or {@link #removeInt remove} the object at any time later.
 * 
 * @param <T> the class of objects stored in this storage
 * @author xbatko
 */
public interface IntStorage<T> extends Storage<T> {

    /**
     * Stores an object in this storage.
     * The address returned by this call can be used to retrieve or remove the object.
     * 
     * @param object the object to store
     * @return the address where the object has been stored
     * @throws BucketStorageException if there was an error writing the data
     */
    public IntAddress<T> store(T object) throws BucketStorageException;

    /**
     * Reads the object stored at the specified address in this storage.
     * @param address the address of the object to read
     * @return the object retrieved
     * @throws BucketStorageException if there was an error reading the data
     */
    public abstract T read(int address) throws BucketStorageException;

    /**
     * Removes the object stored at the specified address in this storage.
     * This operation is optional and need not be implemented.
     * 
     * @param address the address of the object to remove
     * @throws BucketStorageException if there was an error deleting an object
     * @throws UnsupportedOperationException if this storage does not support removal of objects
     */
    public abstract void remove(int address) throws BucketStorageException, UnsupportedOperationException;

}