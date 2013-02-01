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
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
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

    @Override
    public final ModifiableSearch<T> search() throws IllegalStateException {
        return search((IndexComparator<Object, Object>)null, null, null);
    }

    @Override
    public final ModifiableSearch<T> search(K key, boolean restrictEqual) throws IllegalStateException {
        return search(key, restrictEqual?key:null, restrictEqual?key:null);
    }

    @Override
    public final ModifiableSearch<T> search(K from, K to) throws IllegalStateException {
        return search((K)null, from, to);
    }

    @Override
    public final ModifiableSearch<T> search(K startKey, K from, K to) throws IllegalStateException {
        return new IndexedBoundedSearch(acquireSearchLock(), startKey, from, to);
    }

    @Override
    public final ModifiableSearch<T> search(Collection<? extends K> keys) throws IllegalStateException {
        return new IndexedKeySearch(acquireSearchLock(), keys);
    }

    @Override
    public final <C> ModifiableSearch<T> search(IndexComparator<? super C, ? super T> comparator, C key) throws IllegalStateException {
        return search(comparator, Collections.singletonList(key));
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <C> ModifiableSearch<T> search(IndexComparator<? super C, ? super T> comparator, C from, C to) throws IllegalStateException {
        if (comparator != null && comparator.equals(comparator()))
            return new IndexedBoundedSearch(acquireSearchLock(), null, (K)from, (K)to); // This cast IS checked by the comparator
        else
            return new FullScanSearch<C>(acquireSearchLock(), comparator, from, to);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <C> ModifiableSearch<T> search(IndexComparator<? super C, ? super T> comparator, Collection<? extends C> keys) throws IllegalStateException {
        if (comparator != null && comparator.equals(comparator()))
            return new IndexedKeySearch(acquireSearchLock(), (Collection)keys); // This cast IS checked by the comparator
        else
            return new FullScanSearch<C>(acquireSearchLock(), comparator, keys);
    }


    //****************** Search implementation ******************//

    /**
     * Internal class that implements full-scan search for this index.
     * @param <C> type of boundaries used while comparing objects
     */
    private class FullScanSearch<C> extends AbstractSearch<C, T> implements ModifiableSearch<T> {

        //****************** Attributes ******************//

        /** Lock object for this search */
        private final Lock lock;
        /** Minimal (inclusive) index that this search can access in the index array */
        protected int minIndex = 0;
        /** Maximal (inclusive) index that this search can access in the index array */
        protected int maxIndex = size() - 1;
        /** Index of an element to be returned by subsequent call to next */
        protected int cursor = minIndex;
        /**
         * Index of element returned by most recent call to next or
         * previous. It is reset to -1 if this element is deleted by a call
         * to remove.
         */
        protected int lastRet = -1;


        //****************** Constructor ******************//

        /**
         * Creates a new instance of ArrayIndexModifiableSearch for the specified search comparator and keys to search.
         * Any object the key of which is equal (according to the given comparator) to any of the keys
         * is returned.
         *
         * @param lock the search lock on the index (if not supported <tt>null</tt> can be provided)
         * @param comparator the comparator that is used to compare the keys
         * @param keys list of keys to search for
         */
        @SuppressWarnings("unchecked")
        protected FullScanSearch(Lock lock, IndexComparator<? super C, ? super T> comparator, Collection<? extends C> keys) {
            super(comparator, keys);
            this.lock = lock;
        }

        /**
         * Creates a new instance of Search for the specified search comparator and lower and upper key bounds.
         * Any object the key of which is within interval <code>[fromKey, toKey]</code>
         * is returned.
         *
         * @param lock the search lock on the index (if not supported <tt>null</tt> can be provided)
         * @param comparator the comparator that is used to compare the keys
         * @param fromKey the lower bound on the searched object keys (inclusive)
         * @param toKey the upper bound on the searched object keys (inclusive)
         */
        @SuppressWarnings("unchecked")
        protected FullScanSearch(Lock lock, IndexComparator<? super C, ? super T> comparator, C fromKey, C toKey) {
            super(comparator, fromKey, toKey);
            this.lock = lock;
        }

        @Override
        protected void finalize() throws Throwable {
            close();
            super.finalize();
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

        @Override
        public void remove() throws IllegalStateException, BucketStorageException {
            if (lastRet == -1)
                throw new IllegalStateException();

            AbstractArrayIndex.this.remove(lastRet);
            if (lastRet < cursor)
                cursor--;
            maxIndex--;
            lastRet = -1;
        }

        @Override
        public void close() {
            if (lock != null)
                lock.unlock();
        }
    }

    /**
     * Internal class that extends the search by using the index.
     */
    private class IndexedBoundedSearch extends FullScanSearch<K> {
        /**
         * Creates a new instance of ArrayIndexBoundModifiableSearch.
         * This search returns any object the key of which is within interval
         * <code>[fromKey, toKey]</code>.
         * <p>
         * The indexed binary search is used to locate the startKey, fromKey and toKey values
         * and no comparator is used during the search afterwards.
         * </p>
         *
         * @param lock the search lock on the index (if not supported <tt>null</tt> can be provided)
         * @param startKey the key of object where the search should start (if <tt>null</tt>, start searches from the {@code from} key)
         * @param fromKey the lower bound on the searched object keys (inclusive)
         * @param toKey the upper bound on the searched object keys (inclusive)
         */
        protected IndexedBoundedSearch(Lock lock, K startKey, K fromKey, K toKey) {
            super(lock, null, fromKey, toKey);

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

            // Search for starting key
            if (startKey == null || startKey == fromKey)
                cursor = minIndex;
            else if (startKey == toKey)
                cursor = maxIndex;
            else
                cursor = binarySearch(startKey, minIndex, maxIndex, false);
        }
    }

    /**
     * Internal class that extends the search by using the index.
     */
    private class IndexedKeySearch extends FullScanSearch<K> {
        /** Index of the key that was last searched */
        protected int currentKey;

        /**
         * Creates a new instance of ArrayIndexKeyModifiableSearch.
         * This search returns any object the key of which is any of the given {@code keys}.
         * <p>
         * The indexed binary search is used to locate the keys.
         * Note that only one object is returned for each key.
         * </p>
         *
         * @param lock the search lock on the index (if not supported <tt>null</tt> can be provided)
         * @param keys list of keys to search for
         */
        protected IndexedKeySearch(Lock lock, Collection<? extends K> keys) {
            super(lock, null, keys);
        }

        @Override
        protected T readNext() throws BucketStorageException {
            if (currentKey >= getKeyCount())
                return null;
            int newPos = binarySearch(getKey(currentKey++), 0, size() - 1, true);
            cursor = (newPos < 0) ? maxIndex : newPos;
            return super.readNext();
        }

        @Override
        protected T readPrevious() throws BucketStorageException {
            if (currentKey <= 0)
                return null;
            int newPos = binarySearch(getKey(--currentKey), 0, size() - 1, true);
            cursor = (newPos < 0) ? minIndex : newPos;
            return super.readNext();
        }

        @Override
        public boolean skip(int count) throws IllegalStateException {
            if (count < 0) {
                if (currentKey + count < 0)
                    return false;
                currentKey += count + 1;
                return previous();
            } else {
                if (currentKey + count >= getKeyCount())
                    return false;
                currentKey += count - 1;
                return next();
            }
        }

    }
}
