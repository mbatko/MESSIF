/*
 *  LongStorageIndex
 * 
 */

package messif.buckets.index.impl;

import java.io.Serializable;
import java.util.List;
import messif.buckets.BucketStorageException;
import messif.buckets.index.IndexComparator;
import messif.buckets.storage.Lock;
import messif.buckets.storage.Lockable;
import messif.buckets.storage.LongAddress;
import messif.buckets.storage.LongStorage;
import messif.buckets.storage.LongStorageSearch;

/**
 * Implementation of a single index over a {@link LongStorage storage with long addresses}.
 * The addresses provided by the storage are kept in internal sorted array
 * that allows fast access to data in the storage. Objects are indexed
 * according to the given {@link IndexComparator}.
 * 
 * @param <K> the type of keys this index is ordered by
 * @param <T> the type of objects stored in this collection
 * @author xbatko
 */
public class LongStorageIndex<K, T> extends AbstractArrayIndex<K, T> implements Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 2L;

    //****************** Attributes ******************//

    /** Storage associated with this index */
    private final LongStorage<T> storage;

    /** Index of addresses into the storage */
    private long[] index;

    /** Comparator imposing natural order of this index */
    private final IndexComparator<K, T> comparator;


    //****************** Constructor ******************//

    /**
     * Creates a new instance of IntStorageIndex for the specified storage.
     * @param storage the storage to associate with this index
     * @param comparator the comparator imposing natural order of this index
     */
    public LongStorageIndex(LongStorage<T> storage, IndexComparator<K, T> comparator) {
        this.storage = storage;
        this.comparator = comparator;
        this.index = new long[0];
    }

    @Override
    public void finalize() throws Throwable {
        storage.finalize();
        super.finalize();
    }

    public void destroy() throws Throwable {
        storage.destroy();
    }


    //****************** Comparator methods ******************//

    public IndexComparator<K, T> comparator() {
        return comparator;
    }

    @Override
    protected int compare(K key, T object) throws ClassCastException {
        return comparator.indexCompare(key, object);
    }


    //****************** Index access methods ******************//

    public int size() {
        return index.length;
    }

    /**
     * Searches for the point where to insert the object <code>object</code>.
     * @param object the object to insert
     * @return the point in the array where to put the object
     * @throws BucketStorageException if there was a problem determining the point
     */
    protected int insertionPoint(T object) throws BucketStorageException {
        return binarySearch(comparator.extractKey(object), 0, index.length - 1, false);
    }

    public boolean add(T object) throws BucketStorageException {
        // Search for the position where the object is added into index
        int pos = insertionPoint(object);

        // Make place for the address in the index at pos
        long[] newIndex = new long[index.length + 1];
        System.arraycopy(index, 0, newIndex, 0, pos);
        System.arraycopy(index, pos, newIndex, pos + 1, index.length - pos);

        // Store the object into storage and its address into the index and commit the changes
        newIndex[pos] = storage.store(object).getAddress();
        index = newIndex;

        return true;
    }

    @Override
    protected boolean remove(int i) {
        if (i < 0 || i >= index.length)
            return false;
        
        try {
            storage.remove(index[i]);
        } catch (BucketStorageException e) {
            throw new IllegalStateException("Cannot remove object from storage", e);
        }

        // Remove the address on pos
        long[] newIndex = new long[index.length - 1];
        System.arraycopy(index, 0, newIndex, 0, i);
        System.arraycopy(index, i + 1, newIndex, i, index.length - i - 1);
        index = newIndex;

        return true;
    }

    @Override
    protected T get(int i) throws IndexOutOfBoundsException, IllegalStateException {
        try {
            return storage.read(index[i]);
        } catch (BucketStorageException e) {
            throw new IllegalStateException("Cannot read object from storage", e);
        }
    }

    @Override
    protected LongStorageSearch<T> createOrderedSearch(int initialIndex, int minIndex, int maxIndex) {
        return new LongStorageOrderedModifiableSearch(initialIndex, minIndex, maxIndex);
    }

    @Override
    protected <C> LongStorageSearch<T> createFullScanSearch(IndexComparator<? super C, ? super T> comparator, boolean keyBounds, List<? extends C> keys) {
        return new LongStorageFullScanModifiableSearch<C>(comparator, keyBounds, keys);
    }

    /**
     * Internal class that implements ordered search for this index.
     */
    protected class LongStorageOrderedModifiableSearch extends OrderedModifiableSearch implements LongStorageSearch<T> {
        /** Lock object for this search */
        private final Lock lock;
        /**
         * Creates a new instance of LongStorageOrderedModifiableSearch that starts searching
         * from the specified position and is bound by the given minimal and maximal positions.
         *
         * @param initialIndex the position where to start this iterator
         * @param minIndex minimal position (inclusive) that this iterator will access
         * @param maxIndex maximal position (inclusive) that this iterator will access
         */
        protected LongStorageOrderedModifiableSearch(int initialIndex, int minIndex, int maxIndex) {
            super(initialIndex, minIndex, maxIndex);
            this.lock = storage instanceof Lockable ? ((Lockable)storage).lock(true) : null;
        }
        @Override
        protected void finalize() throws Throwable {
            if (this.lock != null)
                this.lock.unlock();
            super.finalize();
        }
        public long getCurrentObjectLongAddress() throws IllegalStateException {
            return index[getCurentObjectIndex()];
        }
        public LongAddress<T> getCurrentObjectAddress() throws IllegalStateException {
            return new LongAddress<T>(storage, index[getCurentObjectIndex()]);
        }
    }

    /**
     * Internal class that implements full-scan search for this index.
     * @param <C> type of boundaries used while comparing objects
     */
    protected class LongStorageFullScanModifiableSearch<C> extends FullScanModifiableSearch<C> implements LongStorageSearch<T> {
        /** Lock object for this search */
        private final Lock lock;
        /**
         * Creates a new instance of LongStorageFullScanModifiableSearch for the
         * specified search comparator and [from,to] bounds.
         * @param comparator the comparator that compares the <code>keys</code> with the stored objects
         * @param keyBounds if <tt>true</tt>, the {@code keys} must have exactly two values that represent
         *          the lower and the upper bounds on the searched value
         * @param keys list of keys to search for
         */
        protected LongStorageFullScanModifiableSearch(IndexComparator<? super C, ? super T> comparator, boolean keyBounds, List<? extends C> keys) {
            super(comparator, keyBounds, keys);
            this.lock = storage instanceof Lockable ? ((Lockable)storage).lock(true) : null;
        }
        @Override
        protected void finalize() throws Throwable {
            close();
            super.finalize();
        }
        public long getCurrentObjectLongAddress() throws IllegalStateException {
            return index[getCurentObjectIndex()];
        }
        public LongAddress<T> getCurrentObjectAddress() throws IllegalStateException {
            return new LongAddress<T>(storage, index[getCurentObjectIndex()]);
        }
        @Override
        public void close() {
            if (this.lock != null)
                this.lock.unlock();
            super.close();
        }
    }
}
