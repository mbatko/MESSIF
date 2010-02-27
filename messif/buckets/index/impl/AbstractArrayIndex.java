/*
 *  AbstractArrayIndex
 * 
 */

package messif.buckets.index.impl;

import java.util.Collection;
import java.util.Collections;
import messif.buckets.BucketStorageException;
import messif.buckets.index.IndexComparator;
import messif.buckets.index.Lock;
import messif.buckets.index.ModifiableOrderedIndex;
import messif.buckets.index.ModifiableSearch;
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
 * @param <T> the type of objects stored in this index
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
     * Locks this index for searching and returns a lock object if it is supported.
     * A <tt>null</tt> is returned otherwise.
     * The called must call the {@link Lock#unlock()} method if this method has returned non-null.
     * @return a lock on this index or <tt>null</tt>
     */
    protected abstract Lock acquireSearchLock();

    @Override
    public void finalize() throws Throwable {
        super.finalize();
    }


    //****************** Search methods ******************//

    public final ModifiableSearch<T> search() throws IllegalStateException {
        return search((IndexComparator<Object, Object>)null, null, null);
    }

    public final ModifiableSearch<T> search(K key, boolean restrictEqual) throws IllegalStateException {
        return search(key, restrictEqual?key:null, restrictEqual?key:null);
    }

    public final ModifiableSearch<T> search(K from, K to) throws IllegalStateException {
        return search(comparator(), from, to);
    }

    public final ModifiableSearch<T> search(K startKey, K from, K to) throws IllegalStateException {
        return search(comparator(), startKey, from, to);
    }

    public final ModifiableSearch<T> search(Collection<? extends K> keys) throws IllegalStateException {
        return search(comparator(), keys);
    }

    public final <C> ModifiableSearch<T> search(IndexComparator<? super C, ? super T> comparator, C key) throws IllegalStateException {
        return search(comparator, Collections.singletonList(key));
    }

    public final <C> ModifiableSearch<T> search(IndexComparator<? super C, ? super T> comparator, C from, C to) throws IllegalStateException {
        return search(comparator, null, from, to);
    }

    public final <C> ModifiableSearch<T> search(IndexComparator<? super C, ? super T> comparator, Collection<? extends C> keys) throws IllegalStateException {
        return new ArrayIndexModifiableSearch<C>(acquireSearchLock(), comparator, keys);
    }

    /**
     * Returns a search for objects in this index that are within the specified key-range.
     * The key boundaries <code>[from, to]</code> need not necessarily be of the same
     * class as the objects stored in this index, however, the comparator must be
     * able to compare the boundaries and the internal objects.
     * The search begins on object that has key {@code start}.
     *
     * <p>
     * Note that objects are <i>not</i> returned in the order defined by the comparator
     * </p>
     *
     * @param <C> the type the boundaries used by the search
     * @param comparator compares the boundaries <code>[from, to]</code> with the stored objects
     * @param start the key of object where the search should start (if <tt>null</tt>, start searches from the {@code from} key)
     * @param from the lower bound on the searched objects, i.e. objects greater or equal are returned
     * @param to the upper bound on the searched objects, i.e. objects smaller or equal are returned
     * @return a search for objects in this index
     * @throws IllegalStateException if there was an error initializing the search on this index
     */
    public final <C> ModifiableSearch<T> search(IndexComparator<? super C, ? super T> comparator, K start, C from, C to) throws IllegalStateException {
        return new ArrayIndexModifiableSearch<C>(acquireSearchLock(), comparator, start, from, to);
    }


    //****************** Search implementation ******************//

    /**
     * Internal class that implements full-scan search for this index.
     * @param <C> type of boundaries used while comparing objects
     */
    protected class ArrayIndexModifiableSearch<C> extends AbstractSearch<C, T> implements ModifiableSearch<T> {

        //****************** Attributes ******************//

        /** Lock object for this search */
        private final Lock lock;
        /** Minimal (inclusive) index that this search can access in the index array */
        private int minIndex;
        /** Maximal (inclusive) index that this search can access in the index array */
        private int maxIndex;
        /** Index of an element to be returned by subsequent call to next */
        private int cursor;
        /**
         * Index of element returned by most recent call to next or
         * previous. It is reset to -1 if this element is deleted by a call
         * to remove.
         */
        private int lastRet = -1;


        //****************** Constructor ******************//

        /**
         * Creates a new instance of ArrayIndexModifiableSearch for the specified search comparator and keys to search.
         * Any object the key of which is equal (according to the given comparator) to any of the keys
         * is returned.
         * <p>
         * Note that the indexed data are used if the given comparator is compatible with the index comparator.
         * </p>
         *
         * @param lock the search lock on the index (if not supported <tt>null</tt> can be provided)
         * @param comparator the comparator that is used to compare the keys
         * @param keys list of keys to search for
         */
        @SuppressWarnings("unchecked")
        protected ArrayIndexModifiableSearch(Lock lock, IndexComparator<? super C, ? super T> comparator, Collection<? extends C> keys) {
            super(comparator, keys);
            this.lock = lock;
            if (comparator != null && comparator.equals(comparator()))
                initializeIndexes((K)getKey(0), (K)getKey(getKeyCount() - 1)); // This IS checked by the comparator
            else
                maxIndex = size() - 1;
            cursor = minIndex;
        }

        /**
         * Creates a new instance of Search for the specified search comparator and lower and upper key bounds.
         * Any object the key of which is within interval <code>[fromKey, toKey]</code>
         * is returned.
         * <p>
         * Note that the indexed data are used if the given comparator is compatible with the index comparator.
         * </p>
         *
         * @param lock the search lock on the index (if not supported <tt>null</tt> can be provided)
         * @param comparator the comparator that is used to compare the keys
         * @param startKey the key of object where the search should start (if <tt>null</tt>, start searches from the {@code from} key)
         * @param fromKey the lower bound on the searched object keys (inclusive)
         * @param toKey the upper bound on the searched object keys (inclusive)
         */
        @SuppressWarnings("unchecked")
        protected ArrayIndexModifiableSearch(Lock lock, IndexComparator<? super C, ? super T> comparator, K startKey, C fromKey, C toKey) {
            super(comparator == null || comparator.equals(comparator()) ? null : comparator, fromKey, toKey);
            this.lock = lock;
            if (comparator != null && comparator.equals(comparator()))
                initializeIndexes((K)fromKey, (K)toKey); // This IS checked by the comparator
            else
                maxIndex = size() - 1;

            // Search for starting key
            if (startKey == null || startKey == fromKey)
                cursor = minIndex;
            else if (startKey == toKey)
                cursor = maxIndex;
            else
                cursor = binarySearch(startKey, minIndex, maxIndex, false);
        }

        @Override
        protected void finalize() throws Throwable {
            close();
            super.finalize();
        }

        /**
         * Initialize minimal and maximal indexes by searching for the {@code fromKey}
         * and {@code toKey} using the binary search. Note that this method is only
         * used if the comparator is compatible with the order of the index. Otherwise
         * the binary search cannot be used.
         * @param fromKey the key of the lower boundary
         * @param toKey the key of the upper boundary
         */
        private void initializeIndexes(K fromKey, K toKey) {
            // Search for lower boundary key
            minIndex = (fromKey == null) ? 0 : binarySearch(fromKey, 0, size() - 1, true);

            // If "fromKey" key was not found
            if (minIndex < 0) {
                if (fromKey == toKey) {// There is no object that can be accessed ("from" is the same as "to" and neither can be found)
                    minIndex = 0;
                    maxIndex = -1;
                    return;
                } else {
                    minIndex = -minIndex - 1;
                }
            }

            // Search for upper boundary key
            if (toKey == null)
                maxIndex = size() - 1;
            else if (toKey == fromKey)
                maxIndex = minIndex;
            else
                maxIndex = binarySearch(toKey, minIndex, size() - 1, true);
            // If "toKey" is not found, high limit is the index by one lower
            if (maxIndex < 0)
                maxIndex = -maxIndex - 2;

            IndexComparator<K, T> comparator = comparator();

            // Expand lower boundary backwards for all items on which the comparator returns zero
            if (fromKey != null)
                while (minIndex > 0 && comparator.indexCompare(fromKey, get(minIndex - 1)) == 0)
                    minIndex--;

            // Expand upper boundary forward for all items on which the comparator returns zero
            if (toKey != null)
                while ((maxIndex < size() - 1) && comparator.indexCompare(toKey, get(maxIndex + 1)) == 0)
                    maxIndex++;
        }


        //****************** Search interface implementations ******************//

        /**
         * Returns the index of the element returned by most recent call to next or
         * previous. If this element is deleted by a call to remove or neither next nor
         * previous methods were called, -1 is returned.
         * @return the index of the current object in the index
         */
        protected int getCurentObjectIndex() {
            return lastRet;
        }

        @Override
        protected T readNext() throws BucketStorageException {
            if (cursor > maxIndex)
                return null;
            lastRet = cursor++;
            return get(lastRet);
        }

        @Override
        protected T readPrevious() throws BucketStorageException {
            if (cursor <= minIndex)
                return null;
            lastRet = --cursor;
            return get(lastRet);
        }

        @Override
        public boolean skip(int count) throws IllegalStateException {
            // If there is a comparator defined, we must check keys
            if (getComparator() != null)
                return super.skip(count);

            // No comparator, we can seek faster
            if (count < 0 && cursor + count >= minIndex) { // Skip backwards
                cursor += count + 1;
                return previous();
            }

            if (count > 0 && cursor + count - 1 <= maxIndex) { // Skip forward
                cursor += count - 1;
                return next();
            }

            return false;
        }

        public void remove() throws IllegalStateException, BucketStorageException {
            if (lastRet == -1)
                throw new IllegalStateException();

            AbstractArrayIndex.this.remove(lastRet);
            if (lastRet < cursor)
                cursor--;
            maxIndex--;
            lastRet = -1;
        }

        public void close() {
            if (lock != null)
                lock.unlock();
        }
    }
}
