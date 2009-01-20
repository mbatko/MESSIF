/*
 *  AbstractStorageIndex
 * 
 */

package messif.buckets.index.impl;

import java.io.Serializable;
import messif.buckets.BucketStorageException;
import messif.buckets.index.IndexComparator;
import messif.buckets.index.ModifiableIndex;
import messif.buckets.index.ModifiableSearch;
import messif.buckets.storage.IntStorage;

/**
 *
 * @param <K> 
 * @param <T>
 * @author xbatko
 */
public class IntStorageIndex<K, T> implements ModifiableIndex<T>, Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /** Indexed data address translation */
    private int[] index;

    /** Storage associated with this index */
    private final IntStorage<T> storage;

    /** Comparator imposing natural order of this index */
    private final IndexComparator<K, T> comparator;

    public IntStorageIndex(IntStorage<T> storage, IndexComparator<K, T> comparator) {
        this.storage = storage;
        this.comparator = comparator;
        this.index = new int[0];
    }

    /**
     * Returns the index where the object <code>o</code> should be inserted.
     * The order is defined by the index's comparator.
     * @param o the object to get the insertion point for
     * @return the index where the object <code>o</code> should be inserted
     * @throws BucketStorageException if there was an error reading an object from the underlying storage
     */
    private int insertionPoint(K o) throws BucketStorageException {
	int low = 0;
	int high = index.length - 1;

	while (low <= high) {
	    int mid = (low + high) >>> 1;
	    int cmp = comparator.compare(o, storage.read(index[mid]));

	    if (cmp <= 0) // Inserted object is smaller than or equal to the current middle
		high = mid - 1;
            else // Inserted object is greater than the current middle
		low = mid + 1;
	}
	return low;
    }

    @SuppressWarnings("unchecked")
    private <B> int initialPosition(IndexComparator<B, T> comparator, B key) throws IllegalStateException {
        if (key != null && comparator.equals(this.comparator)) {
            try {
                return insertionPoint((K)key); // This cast IS checked, because the comparators are equal
            } catch (BucketStorageException e) {
                throw new IllegalStateException("Error initializing search", e);
            }
        } else {
            return 0;
        }
    }


    public synchronized boolean add(T object) throws BucketStorageException {
        int address = storage.store(object).getAddress();

        // Search for the position where the object is added into index
        int pos = insertionPoint(comparator.extractKey(object));

        // Insert the address into pos
        int[] newIndex = new int[index.length + 1];
        System.arraycopy(index, 0, newIndex, 0, pos);
        System.arraycopy(index, pos, newIndex, pos + 1, index.length - pos);
        newIndex[pos] = address;
        index = newIndex;

        return true;
    }

    public int size() {
        return index.length;
    }

    public ModifiableSearch<K, T> search() throws IllegalStateException {
        return new IntIndexModifiableSearch<K>();
    }

    public <C> ModifiableSearch<C, T> search(IndexComparator<C, T> comparator, C from, boolean restrictEqual) throws IllegalStateException {
        return new IntIndexModifiableSearch<C>(comparator, from, restrictEqual);
    }

    public <C> ModifiableSearch<C, T> search(IndexComparator<C, T> comparator, C from, C to) throws IllegalStateException {
        return new IntIndexModifiableSearch<C>(comparator, from, to);
    }

    private class IntIndexModifiableSearch<B> extends ModifiableSearch<B, T> {
        private int nextIndexPosition;
        private int prevIndexPosition;

        public IntIndexModifiableSearch() {
            super(null, null, null);
            this.nextIndexPosition = 0;
            this.prevIndexPosition = -1;
        }

        public IntIndexModifiableSearch(IndexComparator<B, T> comparator, B from, B to) throws IllegalStateException {
            super(comparator, from, to);
            this.nextIndexPosition = initialPosition(comparator, from);
            this.prevIndexPosition = this.nextIndexPosition - 1;
        }

        public IntIndexModifiableSearch(IndexComparator<B, T> comparator, B from, boolean restrictEqual) throws IllegalStateException {
            super(comparator, restrictEqual?from:null, restrictEqual?from:null);
            this.nextIndexPosition = initialPosition(comparator, from);
            this.prevIndexPosition = this.nextIndexPosition - 1;
        }

        @Override
        protected T readNext() throws BucketStorageException {
            // Pos is last index - no next items
            if (nextIndexPosition >= index.length)
                return null;
            // Advance positions and return object from storage
            prevIndexPosition = nextIndexPosition - 1;
            return storage.read(index[nextIndexPosition++]);
        }

        @Override
        protected T readPrevious() throws BucketStorageException {
            // Pos is first index - no prev items
            if (prevIndexPosition < 0)
                return null;
            // Advance positions and return object from storage
            nextIndexPosition = prevIndexPosition + 1;
            return storage.read(index[prevIndexPosition--]);
        }

        public synchronized void remove() throws IllegalStateException, BucketStorageException {
            if (prevIndexPosition == nextIndexPosition || nextIndexPosition > index.length)
                throw new IllegalStateException("Cannot remove object before next() or previous() is called");
            storage.remove(index[nextIndexPosition - 1]);

            // Remove the address on pos
            int[] newIndex = new int[index.length - 1];
            System.arraycopy(index, 0, newIndex, 0, nextIndexPosition - 1);
            System.arraycopy(index, nextIndexPosition, newIndex, nextIndexPosition - 1, index.length - nextIndexPosition);
            index = newIndex;

            // Set next to current position
            nextIndexPosition--;
        }

    }

}
