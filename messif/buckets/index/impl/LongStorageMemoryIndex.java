
package messif.buckets.index.impl;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import messif.buckets.BucketStorageException;
import messif.buckets.index.IndexComparator;
import messif.buckets.index.ModifiableOrderedIndex;
import messif.buckets.storage.StorageSearch;
import messif.buckets.index.impl.LongStorageMemoryIndex.KeyAddressPair;
import messif.buckets.storage.Lock;
import messif.buckets.storage.Lockable;
import messif.buckets.storage.LongAddress;
import messif.buckets.storage.impl.DiskStorage;
import messif.utility.SortedArrayData;

/**
 * Implementation of disk (long) index that stores the indexed data in a sorted array and keeps the
 * keys to be compared always in memory.
 * 
 * <p>
 * All search methods are correctly implemented using binary search on
 * the array whenever possible.
 * </p>
 * 
 * @param <K> the type of keys this index is ordered by
 * @param <T> the type of objects stored in this collection
 * @author xbatko (xnovak8)
 */
public class LongStorageMemoryIndex<K, T> extends SortedArrayData<K, KeyAddressPair<K>> implements ModifiableOrderedIndex<K, T>, Serializable {

    /** Class serial id for serialization. */
    private static final long serialVersionUID = 102302L;


    //****************** Attributes ******************//

    /** Storage associated with this index */
    private DiskStorage<T> storage;

    /** Index of addresses into the storage */
    private ArrayList<KeyAddressPair<K>> index;

    /** Comparator imposing natural order of this index */
    private final IndexComparator<K, T> comparator;


    //****************** Constructor ******************//

    /**
     * Creates a new instance of LongStorageMemoryIndex for the specified storage.
     * @param storage the storage to associate with this index
     * @param comparator the comparator imposing natural order of this index
     */
    public LongStorageMemoryIndex(DiskStorage<T> storage, IndexComparator<K, T> comparator) {
        this.storage = storage;
        this.comparator = comparator;
        this.index = new ArrayList<KeyAddressPair<K>>();
    }

    @Override
    public void finalize() throws Throwable {
        if (storage != null) {
            // Reordering on destroy
            if (storage.isModified()) {
                DiskStorage<T> oldStorage = storage;

                // Name of the storage file
                File oldStorageFile = oldStorage.getFile();
                File newStorageFile = File.createTempFile(oldStorageFile.getName(), DiskStorage.FILENAME_SUFFIX, oldStorageFile.getParentFile());

                reorderStorage(newStorageFile);
                oldStorage.destroy();
                System.err.println("Reordering on " + oldStorageFile + " to file " + newStorageFile + " finished");
            }
            storage.finalize();
        }
        super.finalize();
    }

    public void destroy() throws Throwable {
        storage.destroy();
        storage = null;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    // ******************     Comparator methods      ****************** //

    public IndexComparator<K, T> comparator() {
        return comparator;
    }

    @Override
    protected int compare(K key, KeyAddressPair<K> object) throws ClassCastException {
        return comparator.compare(key, object.key);
    }

    //****************** Reorder support ******************//

    /**
     * Switches this index to a new storage in which the data are ordered according
     * the this index's current order.
     *
     * @param newFile the file where the new storage is created
     * @throws IOException if there was a problem writing the the new storage
     * @throws BucketStorageException if there was a problem reading objects from the old storage or writing them to the new one
     */
    public void reorderStorage(File newFile) throws IOException, BucketStorageException {
        // Create new storage
        DiskStorage<T> newStorage = new DiskStorage<T>(storage, newFile);
        ArrayList<KeyAddressPair<K>> newIndex = new ArrayList<KeyAddressPair<K>>();

        synchronized (storage) {
            // Copy data to the new storage
            for (KeyAddressPair<K> indexPair : index)
                newIndex.add(new KeyAddressPair<K>(indexPair.key, newStorage.store(storage.read(indexPair.position)).getAddress()));

            // Switch to new storage
            this.storage = newStorage;
            this.index = newIndex;
        }
    }


    // ******************     Index access methods     ****************** //

    /**
     * Given a storage position, this method simply returns object on given position in the storeage.
     * @param position storage position
     * @return object on given position in the storeage
     */
    private T getObject(long position) {
        try {
            return storage.read(position);
        } catch (BucketStorageException ex) {
            throw new IllegalStateException("Cannot read object from storage", ex);
        }
    }

    public int size() {
        return index.size();
    }

    /**
     * Searches for the point where to insert the object <code>object</code>.
     * @param key key of the object to be inserted
     * @return the point in the array where to put the object
     * @throws BucketStorageException if there was a problem determining the point
     */
    protected int insertionPoint(K key) throws BucketStorageException {
        return binarySearch(key, 0, index.size() - 1, false);
    }

    public boolean add(T object) throws BucketStorageException {
        // Search for the position where the object is added into index
        K key = comparator.extractKey(object);
        int pos = insertionPoint(key);

        index.add(pos, new KeyAddressPair<K>(key, storage.store(object).getAddress()));

        return true;
    }

    /**
     * Removes the element at the specified position in this collection - from both index and storage.
     * @param i index of the element to remove
     * @return <tt>false</tt> if the object was not removed (e.g. because there is no object with this index)
     */
    protected boolean remove(int i) {
        if (i < 0 || i >= index.size())
            return false;

        try {
            // remove the object from the storage
            storage.remove(index.get(i).position);

            // remove the key from the index
            index.remove(i);

            return true;
        } catch (BucketStorageException e) {
            throw new IllegalStateException("Cannot remove object from storage", e);
        }
    }

    @Override
    protected KeyAddressPair<K> get(int i) throws IndexOutOfBoundsException, IllegalStateException {
        return index.get(i);
    }

    /**
     * Locks this index and returns a lock object if it is supported.
     * A <tt>null</tt> is returned otherwise.
     * The called must call the {@link Lock#unlock()} method if this method has returned non-null.
     * @return a lock on this index or <tt>null</tt>
     */
    protected Lock lock() {
        if (storage instanceof Lockable)
            return ((Lockable)storage).lock(true);
        else
            return null;
    }


    //****************** Search methods ******************//

    public StorageSearch<T> search() throws IllegalStateException {
        return new OrderedModifiableSearch(0, 0, size() - 1, lock());
    }

    public StorageSearch<T> search(K key, boolean restrictEqual) throws IllegalStateException {
        return search(key, restrictEqual?key:null, restrictEqual?key:null);
    }

    public StorageSearch<T> search(K from, K to) throws IllegalStateException {
        return search((K)null, from, to);
    }

    public StorageSearch<T> search(K startKey, K from, K to) throws IllegalStateException {
        // Search for lower boundary key
        int fromIndex = (from == null)?0:binarySearch(from, 0, size() - 1, true);        

        // If from not found and to is the same, there is no object that can be accessed
        if (fromIndex < 0) {
            if (from == to)
                return new OrderedModifiableSearch(0, 0, -1, lock());
            else
                fromIndex = -fromIndex - 1;
        }
        
        // Search for upper boundary key
        int toIndex;
        if (to == null)
            toIndex = size() - 1;
        else if (to == from)
            toIndex = fromIndex;
        else
            toIndex = binarySearch(to, fromIndex, size() - 1, true);
        // If "to" is not found, high limit is the index by one lower
        if (toIndex < 0)
            toIndex = -toIndex - 2;

        // Search for starting key
        int startIndex;
        if (startKey == null || startKey == from)
            startIndex = fromIndex;
        else if (startKey == to)
            startIndex = toIndex;
        else
            startIndex = binarySearch(startKey, fromIndex, toIndex, false);

        // Expand lower boundary backwards for all items on which the comparator returns zero
        if (from != null)
            while (fromIndex > 0 && comparator().compare(from, get(fromIndex - 1).key) == 0)
                fromIndex--;

        // Expand upper boundary forward for all items on which the comparator returns zero
        if (to != null)
            while ((toIndex < size() - 1) && comparator().compare(to, get(toIndex + 1).key) == 0)
                toIndex++;

        return new OrderedModifiableSearch(startIndex, fromIndex, toIndex, lock());
    }

    public <C> StorageSearch<T> search(IndexComparator<? super C, ? super T> comparator, C key) throws IllegalStateException {
        return search(comparator, key, key);
    }

    @SuppressWarnings("unchecked")
    public <C> StorageSearch<T> search(IndexComparator<? super C, ? super T> comparator, C from, C to) throws IllegalStateException {
        if (comparator.equals(comparator()))
            return search((K)from, (K)from, (K)to); // This cast IS checked, because the comparators are equal
        else
            return new FullScanModifiableSearch<C>(comparator, from, to, lock());
    }


    //****************** Search implementation ******************//

    /** Internal class that implements ordered search for this index */
    private class OrderedModifiableSearch implements StorageSearch<T> {

        /** Minimal (inclusive) index that this iterator will access in the array */
        private final int minIndex;

        /** Maximal (inclusive) index that this iterator will access in the array */
        private int maxIndex;

        /** Index of an element to be returned by subsequent call to next */
        private int cursor = 0;

        /**
         * Index of element returned by most recent call to next or
         * previous. It is reset to -1 if this element is deleted by a call
         * to remove.
         */
        private int lastRet = -1;

        /** Object found by the last search */
        private T currentObject;

        /** Lock object for this search */
        private final Lock searchLock;

        /**
         * Creates a new internal list iterator that starts from the specified position.
         * The iteration is bound by the min/max limits.
         * @param cursor the position where to start this iterator
         * @param minIndex minimal (inclusive) position that this iterator will access
         * @param maxIndex maximal (inclusive) position that this iterator will access
         * @param searchLock the lock object for the search - its {@link Lock#unlock()}
         *          method is called when this search is finalized
         */
        OrderedModifiableSearch(int cursor, int minIndex, int maxIndex, Lock searchLock) {
            this.cursor = cursor;
            this.minIndex = minIndex;
            this.maxIndex = maxIndex;
            this.searchLock = searchLock;
        }

        @Override
        protected void finalize() throws Throwable {
            if (searchLock != null) {
                searchLock.unlock();
            }
            super.finalize();
        }

        public T getCurrentObject() {
            return currentObject;
        }

        public LongAddress<T> getCurrentObjectAddress() throws IllegalStateException {
            if (lastRet == -1)
                throw new IllegalStateException();
            return new LongAddress<T>(storage, index.get(lastRet).position);
        }

        public boolean next() throws IllegalStateException {
            if (cursor > maxIndex) {
                return false;
            }
            currentObject = getObject(get(cursor).position);
            lastRet = cursor++;
            return true;
        }

        public boolean previous() throws IllegalStateException {
            if (cursor <= minIndex) {
                return false;
            }
            currentObject = getObject(get(cursor - 1).position);
            lastRet = --cursor;
            return true;
        }

        public boolean skip(int count) throws IllegalStateException {
            if (count < 0 && cursor + count >= minIndex) {
                cursor += count + 1;
                currentObject = getObject(get(cursor - 1).position);
                lastRet = --cursor;
                return true;
            }

            if (count > 0 && cursor + count - 1 <= maxIndex) {
                cursor += count - 1;
                currentObject = getObject(get(cursor).position);
                lastRet = cursor++;
                return true;
            }

            return false;
        }

        public void remove() throws IllegalStateException {
            if (lastRet == -1) {
                throw new IllegalStateException();
            }

            LongStorageMemoryIndex.this.remove(lastRet);
            if (lastRet < cursor) {
                cursor--;
            }
            maxIndex--;
            lastRet = -1;
        }

        @Override
        @SuppressWarnings("unchecked")
        public OrderedModifiableSearch clone() throws CloneNotSupportedException {
            return (OrderedModifiableSearch) super.clone();
        }

        public void close() {
        }
    }

    /**
     * Internal class that implements full-scan search for this index.
     * @param <C> type of boundaries used while comparing objects
     */
    private class FullScanModifiableSearch<C> extends AbstractSearch<C, T> implements StorageSearch<T> {

        /** Index of an element to be returned by subsequent call to next */
        private int cursor = 0;
        /**
         * Index of element returned by most recent call to next or
         * previous. It is reset to -1 if this element is deleted by a call
         * to remove.
         */
        private int lastRet = -1;
        
        /** Lock object for this search */
        private final Lock searchLock;

        /**
         * Creates a new instance of FullScanModifiableSearch for the specified search comparator and [from,to] bounds.
         * @param comparator the comparator that defines the 
         * @param from the lower bound on returned objects, i.e. objects greater or equal are returned
         * @param to the upper bound on returned objects, i.e. objects smaller or equal are returned
         * @param searchLock the lock object for the search - its {@link Lock#unlock()}
         *          method is called when this search is finalized
         */
        public FullScanModifiableSearch(IndexComparator<? super C, ? super T> comparator, C from, C to, Lock searchLock) {
            super(comparator, from, to);
            this.searchLock = searchLock;
        }

        @Override
        protected void finalize() throws Throwable {
            if (searchLock != null) {
                searchLock.unlock();
            }
            super.finalize();
        }

        @Override
        protected T readNext() throws BucketStorageException {
            if (cursor >= size()) {
                return null;
            }
            return getObject(get(lastRet = cursor++).position);
        }

        @Override
        protected T readPrevious() throws BucketStorageException {
            if (cursor <= 0) {
                return null;
            }
            return getObject(get(lastRet = --cursor).position);
        }

        public LongAddress<T> getCurrentObjectAddress() throws IllegalStateException {
            if (lastRet == -1)
                throw new IllegalStateException();
            return new LongAddress<T>(storage, index.get(lastRet).position);
        }

        public void remove() throws IllegalStateException, BucketStorageException {
            if (lastRet == -1) {
                throw new IllegalStateException();
            }

            LongStorageMemoryIndex.this.remove(lastRet);
            if (lastRet < cursor) {
                cursor--;
            }
            lastRet = -1;
        }
        public void close() {
        }
    }
    

    /**
     * Class encapsulating the key and long position in the storage.
     * 
     * @param <K> the type of keys this index is ordered by
     */
    protected static class KeyAddressPair<K> implements Serializable {

        /** Class serial id for serialization. */
        private static final long serialVersionUID = 102401L;

        public final K key;
        
        public final long position;

        public KeyAddressPair(K key, long position) {
            this.key = key;
            this.position = position;
        }
    }


}
