/*
 *  AbstractStorageIndex
 * 
 */

package messif.buckets.index.impl;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import messif.buckets.BucketStorageException;
import messif.buckets.index.IndexComparator;
import messif.buckets.index.ModifiableIndex;
import messif.buckets.index.ModifiableSearch;
import messif.buckets.storage.Address;
import messif.buckets.storage.Storage;
import messif.utility.SortedCollection;

/**
 *
 * @param <K> 
 * @param <T>
 * @author xbatko
 */
public class AddressStorageIndex<K, T> implements ModifiableIndex<T>, Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /** Storage associated with this index */
    protected final Storage<T> storage;

    /** Index of addresses into the storage */
    private final SortedCollection<Address<T>> index;

    /**
     * Creates a new instance of AddressStorageIndex for the specified storage.
     * @param storage the storage to associate with this index
     * @param comparator the comparator imposing natural order of this index
     */
    public AddressStorageIndex(Storage<T> storage, IndexComparator<K, T> comparator) {
        this.storage = storage;
        this.index = new SortedCollection<Address<T>>(new InternalComparator(comparator));
    }

    public boolean add(T object) throws BucketStorageException {
        return index.add(storage.store(object));
    }

    public int size() {
        return index.size();
    }

    public ModifiableSearch<?, T> search() throws IllegalStateException {
        return new GenericModifiableSearch<K>(null, null, null);
    }

    public <C> ModifiableSearch<C, T> search(IndexComparator<C, T> comparator, C from, boolean restrictEqual) {
        return new GenericModifiableSearch<C>(comparator, from, restrictEqual);
    }

    public <C> ModifiableSearch<C, T> search(IndexComparator<C, T> comparator, C from, C to) {
        return new GenericModifiableSearch<C>(comparator, from, to);
    }

    private class GenericModifiableSearch<C> extends ModifiableSearch<C, T> {

        private final Iterator<Address<T>> iterator;
        private Address<T> currentAddress = null;

        public GenericModifiableSearch(IndexComparator<C, T> comparator, C from, C to) {
            super(comparator, from, to);
            this.iterator = AddressStorageIndex.this.index.iterator();
        }

        public GenericModifiableSearch(IndexComparator<C, T> comparator, C from, boolean restrictEqual) {
            super(comparator, restrictEqual?from:null, restrictEqual?from:null);
            this.iterator = AddressStorageIndex.this.index.iterator();
        }

        @Override
        protected T readNext() throws BucketStorageException {
            if (!iterator.hasNext())
                return null;
            currentAddress = iterator.next();
            return currentAddress.read();
        }

        @Override
        protected T readPrevious() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void remove() throws IllegalStateException, BucketStorageException {
            if (currentAddress == null)
                throw new IllegalStateException("Cannot remove object before next() or previous() is called");
            currentAddress.remove();
            currentAddress = null;
            iterator.remove();
        }

    }

    private class InternalComparator implements Comparator<Address<T>>, Serializable {
        /** class serial id for serialization */
        private static final long serialVersionUID = 23101L;

        private final IndexComparator<K, T> comparator;
        public InternalComparator(IndexComparator<K, T> comparator) {
            this.comparator = comparator;
        }

        public int compare(Address<T> o1, Address<T> o2) {
            try {
                return comparator.compare(comparator.extractKey(o1.read()), o2.read());
            } catch (BucketStorageException e) {
                throw new IllegalStateException(e);
            }
        }
    }

}
