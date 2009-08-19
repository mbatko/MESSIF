/*
 *  AbstractArrayIndex
 * 
 */

package messif.buckets.index.impl;

import messif.buckets.BucketStorageException;
import messif.buckets.index.IndexComparator;
import messif.buckets.index.ModifiableOrderedIndex;
import messif.buckets.index.ModifiableSearch;
import messif.buckets.storage.Lock;
import messif.utility.SortedArrayData;

/**
 * Implementation of index that stores the indexed data in a sorted array.
 * Access to the actual array is abstract through the {@link #add add},
 * {@link #remove remove} and {@link #get get} methods.
 * 
 * <p>
 * All search methods are correctly implemented using binary search on
 * the array whenever possible.
 * </p>
 * 
 * @param <K> the type of keys this index is ordered by
 * @param <T> the type of objects stored in this collection
 * @author xbatko
 */
public abstract class AbstractArrayIndex<K, T> extends SortedArrayData<K, T> implements ModifiableOrderedIndex<K, T> {

    //****************** Modification methods ******************//

    /**
     * Removes the element at the specified position in this collection.
     * @param index index of the element to remove
     * @return <tt>false</tt> if the object was not removed (e.g. because there is no object with this index)
     */
    protected abstract boolean remove(int index);

    /**
     * Locks this index and returns a lock object if it is supported.
     * A <tt>null</tt> is returned otherwise.
     * The called must call the {@link Lock#unlock()} method if this method has returned non-null.
     * @return a lock on this index or <tt>null</tt>
     */
    protected abstract Lock lock();

    @Override
    public void finalize() throws Throwable {
        super.finalize();
    }


    //****************** Search methods ******************//

    public ModifiableSearch<T> search() throws IllegalStateException {
        return new OrderedModifiableSearch(0, 0, size() - 1, lock());
    }

    public ModifiableSearch<T> search(K key, boolean restrictEqual) throws IllegalStateException {
        return search(key, restrictEqual?key:null, restrictEqual?key:null);
    }

    public ModifiableSearch<T> search(K from, K to) throws IllegalStateException {
        return search((K)null, from, to);
    }

    public ModifiableSearch<T> search(K startKey, K from, K to) throws IllegalStateException {
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
            while (fromIndex > 0 && comparator().indexCompare(from, get(fromIndex - 1)) == 0)
                fromIndex--;

        // Expand upper boundary forward for all items on which the comparator returns zero
        if (to != null)
            while ((toIndex < size() - 1) && comparator().indexCompare(to, get(toIndex + 1)) == 0)
                toIndex++;

        return new OrderedModifiableSearch(startIndex, fromIndex, toIndex, lock());
    }

    public <C> ModifiableSearch<T> search(IndexComparator<? super C, ? super T> comparator, C key) throws IllegalStateException {
        return search(comparator, key, key);
    }

    @SuppressWarnings("unchecked")
    public <C> ModifiableSearch<T> search(IndexComparator<? super C, ? super T> comparator, C from, C to) throws IllegalStateException {
        if (comparator.equals(comparator()))
            return search((K)from, (K)from, (K)to); // This cast IS checked, because the comparators are equal
        else
            return new FullScanModifiableSearch<C>(comparator, from, to, lock());
    }


    //****************** Search implementation ******************//

    /** Internal class that implements ordered search for this index */
    private class OrderedModifiableSearch implements ModifiableSearch<T> {
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
            if (searchLock != null)
                searchLock.unlock();
            super.finalize();
        }

        public T getCurrentObject() {
            return currentObject;
        }

        public boolean next() throws IllegalStateException {
            if (cursor > maxIndex)
                return false;
            currentObject = get(cursor);
            lastRet = cursor++;
            return true;
        }

        public boolean previous() throws IllegalStateException {
            if (cursor <= minIndex)
                return false;
            currentObject = get(cursor - 1);
            lastRet = --cursor;
            return true;
        }

        public boolean skip(int count) throws IllegalStateException {
            if (count < 0 && cursor + count >= minIndex) {
                cursor += count + 1;
                currentObject = get(cursor - 1);
                lastRet = --cursor;
                return true;
            }

            if (count > 0 && cursor + count - 1 <= maxIndex) {
                cursor += count - 1;
                currentObject = get(cursor);
                lastRet = cursor++;
                return true;
            }

            return false;
        }

        public void remove() throws IllegalStateException {
            if (lastRet == -1)
                throw new IllegalStateException();

            AbstractArrayIndex.this.remove(lastRet);
            if (lastRet < cursor)
                cursor--;
            maxIndex--;
            lastRet = -1;
        }

        @Override
        public OrderedModifiableSearch clone() throws CloneNotSupportedException {
            return (OrderedModifiableSearch)super.clone();
        }
    }

    /**
     * Internal class that implements full-scan search for this index.
     * @param <C> type of boundaries used while comparing objects
     */
    private class FullScanModifiableSearch<C> extends AbstractSearch<C, T> implements ModifiableSearch<T> {
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
            if (searchLock != null)
                searchLock.unlock();
            super.finalize();
        }

        @Override
        protected T readNext() throws BucketStorageException {
            if (cursor >= size())
                return null;
            return get(lastRet = cursor++);
        }

        @Override
        protected T readPrevious() throws BucketStorageException {
            if (cursor <= 0)
                return null;
            return get(lastRet = --cursor);
        }

        public void remove() throws IllegalStateException, BucketStorageException {
            if (lastRet == -1)
                throw new IllegalStateException();

            AbstractArrayIndex.this.remove(lastRet);
            if (lastRet < cursor)
                cursor--;
            lastRet = -1;
        }
    }
}
