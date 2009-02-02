/*
 *  IndexedMemoryStorage
 * 
 */

package messif.buckets.storage.impl;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import messif.buckets.BucketStorageException;
import messif.buckets.StorageFailureException;
import messif.buckets.index.IndexComparator;
import messif.buckets.index.ModifiableIndex;
import messif.buckets.index.ModifiableSearch;
import messif.buckets.index.impl.AbstractSearch;
import messif.objects.nio.BinarySerializator;
import messif.objects.nio.ByteBufferFileInputStream;

/**
 *
 * @param <T> 
 * @author xbatko
 */
public class IndexedDiskStorage<T> extends DiskStorage<T> implements ModifiableIndex<T> {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    public IndexedDiskStorage(Class<? extends T> storedObjectsClass, File file, int bufferSize, boolean bufferDirect, long startPosition, long maximalLength, BinarySerializator serializator) throws IOException {
        super(storedObjectsClass, file, bufferSize, bufferDirect, startPosition, maximalLength, serializator);
    }

    public int size() {
        return objectCount;
    }

    public boolean add(T object) throws BucketStorageException {
        return store(object) != null;
    }

    public ModifiableSearch<T> search() throws IllegalStateException {
        return new IndexedDiskStorageSearch<Object>();
    }

    public <C> ModifiableSearch<T> search(IndexComparator<C, T> comparator, C key) throws IllegalStateException {
        return new IndexedDiskStorageSearch<C>(comparator, key, key);
    }

    public <C> ModifiableSearch<T> search(IndexComparator<C, T> comparator, C from, C to) throws IllegalStateException {
        return new IndexedDiskStorageSearch<C>(comparator, from, to);
    }

    private class IndexedDiskStorageSearch<C> extends AbstractSearch<C, T> implements ModifiableSearch<T> {
        private final ByteBufferFileInputStream inputStream;
        private int lastObjectSize = -1;

        public IndexedDiskStorageSearch() throws IllegalStateException {
            super(null, null, null);
            try {
                this.inputStream = openInputStream();
            } catch (IOException e) {
                throw new IllegalStateException("Cannot initialize disk storage search", e);
            }
        }

        public IndexedDiskStorageSearch(IndexComparator<C, T> comparator, C from, C to) throws IllegalStateException {
            super(comparator, from, to);
            try {
                this.inputStream = openInputStream();
            } catch (IOException e) {
                throw new IllegalStateException("Cannot initialize disk storage search", e);
            }
        }

        @Override
        protected T readNext() throws BucketStorageException {
            try {
                return serializator.readObject(inputStream, storedObjectsClass);
            } catch (EOFException e) {
                return null;
            } catch (IOException e) {
                throw new StorageFailureException("Cannot read next object from disk storage", e);
            }
        }

        @Override
        protected T readPrevious() throws BucketStorageException {
            throw new UnsupportedOperationException("This is not supported by the disk storage, use index");
        }

        public void remove() throws IllegalStateException, BucketStorageException {
            if (lastObjectSize == -1)
                throw new IllegalStateException("There is no object to be removed");
            throw new UnsupportedOperationException("This is not supported, yet...");
        }

    }

}
