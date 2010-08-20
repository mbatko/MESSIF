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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import messif.buckets.BucketStorageException;
import messif.buckets.index.IndexComparator;
import messif.buckets.index.Lock;
import messif.buckets.index.Lockable;
import messif.buckets.storage.LongStorage;

/**
 * Implementation of a single index over a {@link LongStorage storage with long addresses}.
 * The addresses provided by the storage are kept in internal sorted array
 * that allows fast access to data in the storage. Objects are indexed
 * according to the given {@link IndexComparator}.
 * 
 * @param <K> the type of keys this index is ordered by
 * @param <T> the type of objects stored in this collection
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class LongStorageIndex<K, T> extends AbstractArrayIndex<K, T> implements Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 2L;

    //****************** Constants ******************//

    /** Number of items in the array before the posponed sorting is enabled */
    private static final int POSTPONED_SORT_SIZE = 10000;
    /** Number of items to allocate for one increment of the unsorted array part */
    private static final int POSTPONED_INCREMENT_SIZE = 1000;

    //****************** Attributes ******************//

    /** Storage associated with this index */
    private final LongStorage<T> storage;
    /** Index of addresses into the storage */
    private long[] index;
    /** Comparator imposing natural order of this index */
    private final IndexComparator<K, T> comparator;
    /** Size of the used unsorted part of the array */
    private transient int unsortedSizeUsed;
    /** Total size of the unsorted part of the array */
    private transient int unsortedSizeTotal;
    /** Flag to avoid repetitive sorting when binary searching */
    private transient boolean beingSorted;


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
        return index.length - unsortedSizeTotal + unsortedSizeUsed;
    }

    /**
     * Searches for the point where to insert the object <code>object</code>.
     * Note that insertion point is only looked up in the sorted part of the array
     * and the unsorted part is ignored.
     *
     * @param object the object to insert
     * @return the point in the array where to put the object
     * @throws BucketStorageException if there was a problem determining the point
     */
    protected int insertionPoint(T object) throws BucketStorageException {
        return binarySearch(comparator.extractKey(object), 0, index.length - unsortedSizeTotal - 1, false);
    }

    public boolean add(T object) throws BucketStorageException {
        if (size() > POSTPONED_SORT_SIZE) {
            if (unsortedSizeTotal == unsortedSizeUsed) {
                long[] newIndex = new long[index.length + POSTPONED_INCREMENT_SIZE];
                System.arraycopy(index, 0, newIndex, 0, index.length);
                index = newIndex;
                unsortedSizeTotal += POSTPONED_INCREMENT_SIZE;
            }
            index[size()] = storage.store(object).getAddress();
            unsortedSizeUsed++;
        } else {
            // Search for the position where the object is added into index
            int pos = insertionPoint(object);

            // Make place for the address in the index at pos
            long[] newIndex = new long[index.length + 1];
            System.arraycopy(index, 0, newIndex, 0, pos);
            System.arraycopy(index, pos, newIndex, pos + 1, index.length - pos);

            // Store the object into storage and its address into the index and commit the changes
            newIndex[pos] = storage.store(object).getAddress();
            index = newIndex;
        }

        return true;
    }

    /**
     * Sorts the unsorted part of the array.
     * @throws BucketStorageException if there was a problem reading the data from the storage
     */
    protected synchronized void sort() throws BucketStorageException {
        beingSorted = true;
        try {
            while (unsortedSizeUsed > 0) {
                // Retrieve last unsorted object address
                int lastUnsortedIndex = index.length - unsortedSizeTotal + unsortedSizeUsed - 1;
                long address = index[lastUnsortedIndex];

                // Search for the position where the object is added into index
                int pos = insertionPoint(storage.read(address));

                // Move index data to make space
                System.arraycopy(index, pos, index, pos + 1, lastUnsortedIndex - pos);
                index[pos] = address;
                unsortedSizeUsed--;
                unsortedSizeTotal--;
            }
        } finally {
            beingSorted = false;
        }
    }

    @Override
    protected boolean remove(int i) {
        if (i < 0 || i >= size())
            return false;
        
        try {
            if (unsortedSizeUsed > 0)
                sort();
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
            if (unsortedSizeUsed > 0) {
                synchronized (this) { // This is necessary to block other read operations until the array is sorted
                    if (!beingSorted)
                        sort();
                }
            }

            return storage.read(index[i]);
        } catch (BucketStorageException e) {
            throw new IllegalStateException("Cannot read object from storage", e);
        }
    }

    @Override
    protected Lock acquireSearchLock() {
        return storage instanceof Lockable ? ((Lockable)storage).lock(true) : null;
    }


    //****************** Serialization ******************//

    /**
     * Java native serialization method.
     * @param out the stream to serialize this object to
     * @throws IOException if there was an error writing to stream {@code out}
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Sort array if needed
        try {
            sort();
        } catch (BucketStorageException e) {
            throw new IOException(e);
        }

        // Shrink array
        if (unsortedSizeTotal > 0) {
            long[] newIndex = new long[index.length - unsortedSizeTotal];
            System.arraycopy(index, 0, newIndex, 0, newIndex.length);
            index = newIndex;
        }

        out.defaultWriteObject();
    }

}
