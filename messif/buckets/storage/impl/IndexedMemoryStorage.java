/*
 *  IndexedMemoryStorage
 * 
 */

package messif.buckets.storage.impl;

import messif.buckets.BucketStorageException;
import messif.buckets.index.IndexComparator;
import messif.buckets.index.ModifiableIndex;
import messif.buckets.index.ModifiableSearch;
import messif.buckets.index.impl.AbstractSearch;

/**
 *
 * @param <T> 
 * @author xbatko
 */
public class IndexedMemoryStorage<T> extends MemoryStorage<T> implements ModifiableIndex<T> {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    public IndexedMemoryStorage() {
    }

    public IndexedMemoryStorage(int initialCapacity) {
        super(initialCapacity);
    }

    public boolean add(T object) throws BucketStorageException {
        return store(object) != null;
    }

    public ModifiableSearch<T> search() throws IllegalStateException {
        return new IndexedMemoryStorageSearch<Object>();
    }

    public <C> ModifiableSearch<T> search(IndexComparator<C, T> comparator, C key) throws IllegalStateException {
        return new IndexedMemoryStorageSearch<C>(comparator, key, key);
    }

    public <C> ModifiableSearch<T> search(IndexComparator<C, T> comparator, C from, C to) throws IllegalStateException {
        return new IndexedMemoryStorageSearch<C>(comparator, from, to);
    }

    private class IndexedMemoryStorageSearch<C> extends AbstractSearch<C, T> implements ModifiableSearch<T> {
        private int currentIndexPosition = -1;

        public IndexedMemoryStorageSearch() {
            super(null, null, null);
        }

        public IndexedMemoryStorageSearch(IndexComparator<C, T> comparator, C from, C to) throws IllegalStateException {
            super(comparator, from, to);
        }

        @Override
        protected T readNext() throws BucketStorageException {
            T object = null;

            // Advance position (and skip null objects, since they are deleted)
            while (object == null && currentIndexPosition < maxAddress())
                object = read(++currentIndexPosition);

            return object;
        }

        @Override
        protected T readPrevious() throws BucketStorageException {
            T object = null;

            // Advance position (and skip null objects, since they are deleted)
            while (object == null && currentIndexPosition > 0)
                object = read(--currentIndexPosition);

            return object;
        }

        public void remove() throws IllegalStateException, BucketStorageException {
            if (currentIndexPosition < 0 || currentIndexPosition > maxAddress())
                throw new IllegalStateException("Cannot remove object before next() or previous() is called");
            IndexedMemoryStorage.this.remove(currentIndexPosition);
        }

    }

}
