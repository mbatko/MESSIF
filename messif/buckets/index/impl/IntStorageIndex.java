/*
 *  IntStorageIndex
 * 
 */

package messif.buckets.index.impl;

import java.io.Serializable;
import messif.buckets.BucketStorageException;
import messif.buckets.index.IndexComparator;
import messif.buckets.storage.IntAddress;
import messif.buckets.storage.IntStorage;
import messif.buckets.storage.IntStorageIndexed;
import messif.buckets.storage.IntStorageSearch;
import messif.buckets.storage.Lock;
import messif.buckets.storage.Lockable;

/**
 * Implementation of a single index over a {@link IntStorage storage with integer addresses}.
 * The addresses provided by the storage are kept in internal sorted array
 * that allows fast access to data in the storage. Objects are indexed
 * according to the given {@link IndexComparator}.
 * 
 * @param <K> the type of keys this index is ordered by
 * @param <T> the type of objects stored in this collection
 * @author xbatko
 */
public class IntStorageIndex<K, T> extends AbstractArrayIndex<K, T> implements Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 2L;

    //****************** Attributes ******************//

    /** Storage associated with this index */
    private final IntStorage<T> storage;

    /** Index of addresses into the storage */
    private int[] index;

    /** Comparator imposing natural order of this index */
    private final IndexComparator<K, T> comparator;


    //****************** Constructor ******************//

    /**
     * Creates a new instance of IntStorageIndex for the specified storage.
     * @param storage the storage to associate with this index
     * @param comparator the comparator imposing natural order of this index
     */
    public IntStorageIndex(IntStorage<T> storage, IndexComparator<K, T> comparator) {
        this.storage = storage;
        this.comparator = comparator;
        this.index = new int[0];
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
        int[] newIndex = new int[index.length + 1];
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
        int[] newIndex = new int[index.length - 1];
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
    protected IntStorageSearch<T> createOrderedSearch(int initialIndex, int minIndex, int maxIndex) {
        return new IntStorageOrderedModifiableSearch(maxIndex, initialIndex, minIndex);
    }

    @Override
    protected <C> IntStorageSearch<T> createFullScanSearch(IndexComparator<? super C, ? super T> comparator, C from, C to) {
        if (storage instanceof IntStorageIndexed)
            return ((IntStorageIndexed<T>)storage).search(comparator, from, to);
        else
            return new IntStorageFullScanModifiableSearch<C>(comparator, from, to);
    }

    /**
     * Internal class that implements ordered search for this index.
     */
    protected class IntStorageOrderedModifiableSearch extends OrderedModifiableSearch implements IntStorageSearch<T> {
        /** Lock object for this search */
        private final Lock lock;
        /**
         * Creates a new instance of IntStorageOrderedModifiableSearch that starts searching
         * from the specified position and is bound by the given minimal and maximal positions.
         *
         * @param initialIndex the position where to start this iterator
         * @param minIndex minimal position (inclusive) that this iterator will access
         * @param maxIndex maximal position (inclusive) that this iterator will access
         */
        protected IntStorageOrderedModifiableSearch(int initialIndex, int minIndex, int maxIndex) {
            super(initialIndex, minIndex, maxIndex);
            this.lock = storage instanceof Lockable ? ((Lockable)storage).lock(true) : null;
        }
        @Override
        protected void finalize() throws Throwable {
            if (this.lock != null)
                this.lock.unlock();
            super.finalize();
        }
        public int getCurrentObjectIntAddress() throws IllegalStateException {
            return index[getCurentObjectIndex()];
        }
        public IntAddress<T> getCurrentObjectAddress() throws IllegalStateException {
            return new IntAddress<T>(storage, index[getCurentObjectIndex()]);
        }
    }

    /**
     * Internal class that implements full-scan search for this index.
     * @param <C> type of boundaries used while comparing objects
     */
    protected class IntStorageFullScanModifiableSearch<C> extends FullScanModifiableSearch<C> implements IntStorageSearch<T> {
        /** Lock object for this search */
        private final Lock lock;
        /**
         * Creates a new instance of IntStorageFullScanModifiableSearch for the
         * specified search comparator and [from,to] bounds.
         * @param comparator the comparator that defines the
         * @param from the lower bound on returned objects, i.e. objects greater or equal are returned
         * @param to the upper bound on returned objects, i.e. objects smaller or equal are returned
         */
        protected IntStorageFullScanModifiableSearch(IndexComparator<? super C, ? super T> comparator, C from, C to) {
            super(comparator, from, to);
            this.lock = storage instanceof Lockable ? ((Lockable)storage).lock(true) : null;
        }
        @Override
        protected void finalize() throws Throwable {
            if (this.lock != null)
                this.lock.unlock();
            super.finalize();
        }
        public int getCurrentObjectIntAddress() throws IllegalStateException {
            return index[getCurentObjectIndex()];
        }
        public IntAddress<T> getCurrentObjectAddress() throws IllegalStateException {
            return new IntAddress<T>(storage, index[getCurentObjectIndex()]);
        }
    }
}
