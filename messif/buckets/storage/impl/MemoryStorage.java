/*
 *  MemoryStorage
 * 
 */

package messif.buckets.storage.impl;

import java.io.Serializable;
import messif.buckets.BucketStorageException;
import messif.buckets.storage.IntAddress;
import messif.buckets.storage.IntStorage;
import messif.buckets.storage.InvalidAddressException;

/**
 * Memory based storage.
 * The objects in this storage are stored in an internal array in the order
 * of insertion. The address is the position within the internal array.
 * 
 * @param <T> the class of objects stored in this storage
 * @author xbatko
 */
public class MemoryStorage<T> implements IntStorage<T>, Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Constants ******************//

    /** Percentage of the capacity that is added when internal array is resized */
    private static final float SIZE_INCREASE_FACTOR = 0.3f;

    //****************** Attributes ******************//

    /** Array buffer into which objects are stored */
    private Object[] items;

    /** Size of the actually used storage */
    private int size;

    /** Number of deleted objects, i.e. "nulled" items */
    private int deleted;


    //****************** Constructor ******************//

    /**
     * Constructs an empty memory storage with the specified initial capacity.
     * @param initialCapacity the initial capacity of the storage
     * @throws IllegalArgumentException if the specified initial capacity is invalid
     */
    public MemoryStorage(int initialCapacity) throws IllegalArgumentException {
        if (initialCapacity < 1)
            throw new IllegalArgumentException("Illegal capacity: " + initialCapacity);
        this.items = new Object[initialCapacity];
    }

    /**
     * Constructs an empty memory storage.
     * The initial capacity of the internal storage is set to 16 objects.
     * @throws IllegalArgumentException if the specified initial capacity is invalid
     */
    public MemoryStorage() {
        this(16);
    }


    //****************** Data access methods ******************//

    /**
     * Returns the number of elements in this storage.
     * @return the number of elements in this storage
     */
    public int size() {
        return size - deleted;
    }

    /**
     * Returns <tt>true</tt> if this storage contains no elements.
     * @return <tt>true</tt> if this storage contains no elements
     */
    public boolean isEmpty() {
	return size() == 0;
    }

    /**
     * Returns the maximal currently allocated address.
     * @return the maximal currently allocated address
     */
    protected int maxAddress() {
        return size - 1;
    }


    //****************** Storage methods implementation ******************//

    public synchronized IntAddress<T> store(T object) {
        // If array is too small to hold new item
        if (size == items.length) {
            // Create new array and copy the old data to it
            Object[] oldItems = items;
            items = new Object[size + 1 + (int)(size * SIZE_INCREASE_FACTOR)];
            System.arraycopy(oldItems, 0, items, 0, size);
        }

        // Add object
        items[size] = object;
        return new IntAddress<T>(this, size++);
    }

    @SuppressWarnings("unchecked")
    public T read(int address) throws BucketStorageException {
        try {
            return (T)items[address];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new InvalidAddressException("Cannot access object on address " + address + " in storage " + super.toString());
        }
    }

    public synchronized void remove(int address) throws BucketStorageException, UnsupportedOperationException {
        try {
            items[address] = null;

            // Update size
            if (address == size - 1) {
                // Deleting last object in the buffer
                size--;
                // Check if the end of the array is empty
                while (size > 0 && items[size - 1] == null) {
                    size--;
                    deleted--;
                }
            } else {
                deleted++;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new InvalidAddressException("Cannot access object on address " + address + " in storage " + super.toString());
        }
    }


    //****************** String conversion ******************//

    /**
     * Returns a string representation of this storage. The string
     * representation consists of a list of the stored objects in the
     * order of insertion, enclosed in square brackets
     * (<tt>"[]"</tt>). Adjacent elements are separated by the characters
     * <tt>", "</tt> (comma and space).  Elements are converted to strings as
     * by {@link String#valueOf(Object)}.
     *
     * @return a string representation of this storage
     */
    @Override
    public String toString() {
        if (isEmpty())
            return "[]";

	StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append(items[0]); // This is correct since collection is not empty
	for (int i = 1; i < size; i++)
            sb.append(", ").append(items[i]);
        return sb.append(']').toString();
    }

}
