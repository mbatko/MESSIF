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

import java.io.Serializable;
import messif.buckets.BucketStorageException;
import messif.buckets.index.IndexComparator;
import messif.buckets.index.Lock;
import messif.buckets.index.Lockable;
import messif.buckets.storage.IntStorage;

/**
 * Implementation of a single index over a {@link IntStorage storage with integer addresses}.
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

    @Override
    public void destroy() throws Throwable {
        storage.destroy();
    }


    //****************** Comparator methods ******************//

    @Override
    public IndexComparator<K, T> comparator() {
        return comparator;
    }

    @Override
    protected int compare(K key, T object) throws ClassCastException {
        return comparator.indexCompare(key, object);
    }


    //****************** Index access methods ******************//

    @Override
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

    @Override
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
    protected Lock acquireSearchLock() {
        return storage instanceof Lockable ? ((Lockable)storage).lock(true) : null;
    }

}
