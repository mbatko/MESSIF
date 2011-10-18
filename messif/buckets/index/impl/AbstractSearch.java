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

import java.util.Comparator;
import java.util.Collection;
import messif.buckets.BucketStorageException;
import messif.buckets.index.Index;
import messif.buckets.index.IndexComparator;
import messif.buckets.index.Search;

/**
 * This class represents a search on an index.
 * Search parameters are specified in the constructor, namely the objects
 * are searched using the specified <code>comparator</code> that is checked on the
 * [<code>from</code>, <code>to</code>] boundaries. Specifically, all objects
 * that are bigger or equal to <code>from</code> and smaller or equal to <code>to</code>
 * are returned, i.e.
 * {@link Comparator#compare comparator.indexCompare}<code>(from, o) &lp= 0</code> and
 * {@link Comparator#compare comparator.indexCompare}<code>(to, o) == 0</code> holds.
 * 
 * <p>
 * The {@link Comparator#compare comparator.indexCompare} method will always have
 * the <code>from/to</code> attributes passesed as the first argument and
 * the object that is checked as the second argument.
 * </p>
 * 
 * @param <C> the type the boundaries used by the search
 * @param <T> the type of objects that this {@link Search} searches for
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 * @see Index
 */
public abstract class AbstractSearch<C, T> implements Search<T>, Cloneable {

    //****************** Attributes ******************//

    /** Comparator used for search */
    private final IndexComparator<? super C, ? super T> comparator;
    /** Keys to search */
    private final Object[] keys;
    /** Flag whether the specified keys are bounds or not */
    private final boolean keyBounds;
    /** Object returned by the previous call to next/prev */
    private T currentObject = null;


    //****************** Constructor ******************//

    /**
     * Creates a new instance of Search for the specified search comparator and keys to search.
     * Any object the key of which is equal (according to the given comparator) to any of the keys
     * is returned.
     *
     * @param comparator the comparator that is used to compare the keys
     * @param keys list of keys to search for
     */
    protected AbstractSearch(IndexComparator<? super C, ? super T> comparator, Collection<? extends C> keys) {
        this.comparator = comparator;
        this.keys = keys == null ? new Object[0] : keys.toArray();
        keyBounds = false;
    }

    /**
     * Creates a new instance of Search for the specified search comparator and lower and upper key bounds.
     * Any object the key of which is within interval <code>[fromKey, toKey]</code>
     * is returned.
     *
     * @param comparator the comparator that is used to compare the keys
     * @param fromKey the lower bound on the searched object keys (inclusive)
     * @param toKey the upper bound on the searched object keys (inclusive)
     */
    protected AbstractSearch(IndexComparator<? super C, ? super T> comparator, C fromKey, C toKey) {
        this.comparator = comparator;
        this.keys = new Object[] { fromKey, toKey };
        keyBounds = true;
    }


    //****************** Search interface implementations ******************//

    /**
     * Returns the comparator that this search uses on keys.
     * @return the comparator that this search uses on keys
     */
    public IndexComparator<? super C, ? super T> getComparator() {
        return comparator;
    }

    /**
     * Returns <tt>true</tt> if the searched keys are treated as bounds.
     * Or <tt>false</tt> if the search returns object only for the given keys.
     * @return <tt>true</tt> if the searched keys are treated as bounds or
     *          <tt>false</tt> if the search returns object only for the given keys
     */
    protected boolean isKeyBounds() {
        return keyBounds;
    }

    /**
     * Returns the number of keys that this search currently searches for.
     * @return the number of keys
     */
    protected int getKeyCount() {
        return keys.length;
    }

    /**
     * Returns the keys that this search currently searches for.
     * Note that a copy of the array is returned.
     * @return the array of keys
     */
    protected Object[] getKeys() {
        return keys.clone();
    }

    /**
     * Returns the key with specified index.
     * The index must be greater than or equal to 0 and less than {@link #getKeyCount()}.
     * @param index the index of the key to return
     * @return a searched key
     * @throws IndexOutOfBoundsException if the specified index is less than zero or bigger than or equal to {@link #getKeyCount()}
     */
    @SuppressWarnings("unchecked")
    protected C getKey(int index) throws IndexOutOfBoundsException {
        return (C)keys[index];
    }

    @Override
    public T getCurrentObject() {
        return currentObject;
    }

    @Override
    public boolean next() throws IllegalStateException {
        try {
            for (currentObject = readNext(); currentObject != null; currentObject = readNext()) {
                if (checkKeys(currentObject))
                    return true;
            }
        } catch (BucketStorageException e) {
            throw new IllegalStateException("Error reading next object from the underlying storage", e);
        }

        return false;
    }

    @Override
    public boolean skip(int count) throws IllegalStateException {
        while (count < 0 && previous())
            count++;
        while (count > 0 && next())
            count--;
        return count == 0;
    }

    @Override
    public boolean previous() throws IllegalStateException {
        try {
            for (currentObject = readPrevious(); currentObject != null; currentObject = readPrevious()) {
                if (checkKeys(currentObject))
                    return true;
            }
        } catch (BucketStorageException e) {
            throw new IllegalStateException("Error reading previous object from the underlying storage", e);
        }

        return false;
    }


    //****************** Searching methods ******************//

    /**
     * Checks if the specified object satisfies the given keys (either boundaries
     * or equality). If the comparator is <tt>null</tt> or no keys are specified,
     * the check succeeds.
     * 
     * @param object the object to check the boundaries for
     * @return <tt>true</tt> if object satisfies the keys
     */
    protected boolean checkKeys(T object) {
        // No boundaries checks if comparator is null
        if (comparator == null || keys.length == 0)
            return true;

        // If there is only one key, we do just equality test
        if (keyBounds) {
            // Do boundary check
            C from = getKey(0);
            if (from != null && comparator.indexCompare(from, object) > 0) // from > object
                return false;
            C to = getKey(1);
            if (to != null && comparator.indexCompare(to, object) < 0) // to < object
                return false;
            return true;
        } else {
            for (int i = 0; i < getKeyCount(); i++)
                if (comparator.indexCompare(getKey(i), object) == 0)
                    return true;
            return false;
        }
    }

    /**
     * Returns the next sibling object of the current one.
     * No checks on boundaries are required.
     * If there is no next object, <tt>null</tt> is returned.
     * 
     * @return the next sibling object of the current one
     * @throws BucketStorageException if there was a problem retrieving the next object from the underlying storage
     */
    protected abstract T readNext() throws BucketStorageException;

    /**
     * Returns the previous sibling object of the current one.
     * No checks on boundaries are required.
     * If there is no previous object, <tt>null</tt> is returned.
     * 
     * @return the previous sibling object of the current one
     * @throws BucketStorageException if there was a problem retrieving the previous object from the underlying storage
     */
    protected abstract T readPrevious() throws BucketStorageException;


    //****************** Cloning ******************//

    @Override
    @SuppressWarnings("unchecked")
    public AbstractSearch<C, T> clone() throws CloneNotSupportedException {
        return (AbstractSearch<C, T>)super.clone(); // This cast is checked since this is cloning
    }

}
