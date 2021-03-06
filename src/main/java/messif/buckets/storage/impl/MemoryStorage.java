/*
 *  This file is part of MESSIF library.
 *
 *  MESSIF library is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MESSIF library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MESSIF library.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.buckets.storage.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import messif.buckets.BucketStorageException;
import messif.buckets.index.IndexComparator;
import messif.buckets.index.impl.AbstractSearch;
import messif.buckets.storage.IntAddress;
import messif.buckets.storage.IntStorageIndexed;
import messif.buckets.storage.IntStorageSearch;
import messif.buckets.storage.InvalidAddressException;
import messif.utility.Convert;

/**
 * Memory based storage.
 * The objects in this storage are stored in an internal array in the order
 * of insertion. The address is the position within the internal array.
 * 
 * @param <T> the class of objects stored in this storage
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class MemoryStorage<T> implements IntStorageIndexed<T>, Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Constants ******************//

    /** Percentage of the capacity that is added when internal array is resized */
    private static final float SIZE_INCREASE_FACTOR = 0.3f;

    /** Default initial capacity */
    private static final int INITIAL_CAPACITY = 16;


    //****************** Attributes ******************//

    /** Class of objects that the this storage works with */
    private final Class<? extends T> storedObjectsClass;

    /** Array buffer into which objects are stored */
    private Object[] items;

    /** Size of the actually used storage */
    private int size;

    /** Number of deleted objects, i.e. "nulled" items */
    private int deleted;


    //****************** Constructor ******************//

    /**
     * Constructs an empty memory storage with the specified initial capacity.
     * 
     * @param storedObjectsClass the class of objects that the storage will work with
     * @param initialCapacity the initial capacity of the storage
     * @throws IllegalArgumentException if the specified initial capacity is invalid
     */
    public MemoryStorage(Class<? extends T> storedObjectsClass, int initialCapacity) throws IllegalArgumentException {
        if (initialCapacity < 1)
            throw new IllegalArgumentException("Illegal capacity: " + initialCapacity);
        if (storedObjectsClass == null)
            throw new IllegalArgumentException("Stored object class cannot be null");
        this.storedObjectsClass = storedObjectsClass;
        this.items = new Object[initialCapacity];
    }

    /**
     * Constructs an empty memory storage.
     * The initial capacity of the internal storage is set to 16 objects.
     * 
     * @param storedObjectsClass the class of objects that the storage will work with
     * @throws IllegalArgumentException if the specified initial capacity is invalid
     */
    public MemoryStorage(Class<? extends T> storedObjectsClass) {
        this(storedObjectsClass, INITIAL_CAPACITY);
    }

    @Override
    public void finalize() throws Throwable {
        super.finalize();
    }

    @Override
    public void destroy() throws Throwable {
        this.size = 0;
        this.deleted = 0;
        this.items = null; // This is correct, since this storage is destroyed and no data should be added afterwards
    }


    //****************** Factory method ******************//
    
    /**
     * Creates a new memory storage. The additional parameters are specified in the parameters map with
     * the following recognized key names:
     * <ul>
     *   <li><em>initialCapacity</em> - the initial capacity of the storage</li>
     *   <li><em>oneStorage</em> - if <tt>true</tt>, the storage is created only once
     *              and this created instance is used in subsequent calls</li>
     * </ul>
     * 
     * @param <T> the class of objects that the new storage will work with
     * @param storedObjectsClass the class of objects that the new storage will work with
     * @param parameters list of named parameters (see above)
     * @return a new memory storage instance
     * @throws IllegalArgumentException if the parameters specified are invalid (null values, etc.)
     */
    public static <T> MemoryStorage<T> create(Class<T> storedObjectsClass, Map<String, Object> parameters) throws IllegalArgumentException {
        MemoryStorage<T> storage = null;
        boolean oneStorage = Convert.getParameterValue(parameters, "oneStorage", Boolean.class, false);
        
        if (oneStorage)
            storage = castToMemoryStorage(storedObjectsClass, parameters.get("storage"));

        if (storage == null)
            storage = new MemoryStorage<T>(storedObjectsClass, Convert.getParameterValue(parameters, "initialCapacity", Integer.class, INITIAL_CAPACITY));

        if (oneStorage)
            parameters.put("storage", storage);

        return storage;
    }

    /**
     * Cast the provided object to {@link MemoryStorage} with generics typing.
     * The objects stored in the storage must be of the same type as the <code>storageObjectsClass</code>.
     * 
     * @param <E> the class of objects stored in the storage
     * @param storageObjectsClass the class of objects stored in the storage
     * @param object the storage instance
     * @return the generics-typed {@link MemoryStorage} object
     * @throws ClassCastException if passed <code>object</code> is not a {@link MemoryStorage} or the storage objects are incompatible
     */
    public static <E> MemoryStorage<E> castToMemoryStorage(Class<E> storageObjectsClass, Object object) throws ClassCastException {
        if (object == null)
            return null;

        @SuppressWarnings("unchecked")
        MemoryStorage<E> storage = (MemoryStorage)object; // This IS checked on the following line
        if (storage.getStoredObjectsClass() != storageObjectsClass)
            throw new ClassCastException("Storage " + object + " works with incompatible objects");
        return storage;
    }


    //****************** Data access methods ******************//

    /**
     * Returns the class of objects that the this storage works with.
     * @return the class of objects that the this storage works with
     */
    public Class<? extends T> getStoredObjectsClass() {
        return storedObjectsClass;
    }

    /**
     * Returns the number of elements in this storage.
     * @return the number of elements in this storage
     */
    @Override
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


    //****************** Storage methods implementation ******************//

    @Override
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
    @Override
    public T read(int address) throws BucketStorageException {
        try {
            return (T)items[address];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new InvalidAddressException("Cannot access object on address " + address + " in storage " + super.toString());
        }
    }

    @Override
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


    //****************** Default index implementation ******************//

    @Override
    public boolean add(T object) throws BucketStorageException {
        return store(object) != null;
    }

    @Override
    public IntStorageSearch<T> search() throws IllegalStateException {
        return new MemoryStorageSearch<Object>(null, Collections.emptyList());
    }

    @Override
    public <C> IntStorageSearch<T> search(IndexComparator<? super C, ? super T> comparator, C key) throws IllegalStateException {
        return new MemoryStorageSearch<C>(comparator, Collections.singletonList(key));
    }

    @Override
    public <C> IntStorageSearch<T> search(IndexComparator<? super C, ? super T> comparator, Collection<? extends C> keys) throws IllegalStateException {
        return new MemoryStorageSearch<C>(comparator, keys);
    }

    @Override
    public <C> IntStorageSearch<T> search(IndexComparator<? super C, ? super T> comparator, C from, C to) throws IllegalStateException {
        return new MemoryStorageSearch<C>(comparator, from, to);
    }

    /**
     * Implements the basic search in the memory storage.
     * All objects in the storage are searched from the first one to the last.
     * 
     * @param <C> the type the boundaries used by the search
     */
    private class MemoryStorageSearch<C> extends AbstractSearch<C, T> implements IntStorageSearch<T> {
        /** Current position in the storage's array */
        private int currentIndexPosition = -1;

        /**
         * Creates a new instance of the IndexedMemoryStorageSearch.
         * If {@code keyBounds} is <tt>false</tt>, this search will look for any object
         * that equals (according to the given comparator) to any of the keys.
         * Otherwise, the objects that are within interval <code>[keys[0]; keys[1]]</code>
         * are returned.
         *
         * @param comparator the comparator that is used to compare the keys
         * @param keys list of keys to search for
         */
        private MemoryStorageSearch(IndexComparator<? super C, ? super T> comparator, Collection<? extends C> keys) {
            super(comparator, keys);
        }

        /**
         * Creates a new instance of MemoryStorageSearch for the specified search comparator and [from,to] bounds.
         * @param comparator the comparator that compares the <code>keys</code> with the stored objects
         * @param fromKey the lower bound on the searched keys
         * @param toKey the upper bound on the searched keys
         */
        private MemoryStorageSearch(IndexComparator<? super C, ? super T> comparator, C fromKey, C toKey) {
            super(comparator, fromKey, toKey);
        }

        @Override
        protected T readNext() throws BucketStorageException {
            T object;
            do {
                if (currentIndexPosition >= size - 1)
                    return null;
                object = read(++currentIndexPosition);
            } while (object == null); // skip null objects, since they are deleted

            return object;
        }

        @Override
        protected T readPrevious() throws BucketStorageException {
            T object;
            do {
                if (currentIndexPosition <= 0)
                    return null;
                object = read(--currentIndexPosition);
            } while (object == null); // skip null objects, since they are deleted

            return object;
        }

        @Override
        public IntAddress<T> getCurrentObjectAddress() throws IllegalStateException {
            return new IntAddress<T>(MemoryStorage.this, getCurrentObjectIntAddress());
        }

        @Override
        public int getCurrentObjectIntAddress() throws IllegalStateException {
            if (currentIndexPosition < 0 || currentIndexPosition > size - 1)
                throw new IllegalStateException("There is no current object");
            return currentIndexPosition;
        }

        @Override
        public void remove() throws IllegalStateException, BucketStorageException {
            MemoryStorage.this.remove(getCurrentObjectIntAddress());
        }

        @Override
        public void close() {
        }
    }

}
