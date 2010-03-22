/*
 *  LongAddress
 * 
 */

package messif.buckets.storage;

import messif.buckets.BucketStorageException;

/**
 * Implementation of {@link Address} for a storage that uses long addresses.
 * 
 * @param <T> the class of objects that this address points to
 * @see Storage
 * @author xbatko
 */
public final class LongAddress<T> implements Address<T> {

    /** Class serial id for serialization. */
    private static final long serialVersionUID = 23101L;

    /** Storage associated with this address */
    private final LongStorage<T> storage;
    /** Actual address in the storage this object points to */
    private final long address;

    /**
     * Creates a new instance of IntAddress on the specified int storage.
     * @param storage the storage on which this address is valid
     * @param address the int address in the storage this IntAddress points to
     */
    public LongAddress(LongStorage<T> storage, long address) {
        this.storage = storage;
        this.address = address;
    }

    /**
     * Returns the associated long address into the storage.
     * @return the associated long address into the storage
     */
    public long getAddress() {
        return address;
    }

    public T read() throws BucketStorageException {
        return storage.read(address);
    }

    public void remove() throws BucketStorageException, UnsupportedOperationException {
        storage.remove(address);
    }
}
