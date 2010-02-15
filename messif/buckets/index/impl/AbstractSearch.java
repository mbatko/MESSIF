/*
 *  Search
 * 
 */

package messif.buckets.index.impl;

import java.util.Comparator;
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
 * @author xbatko
 * @see Index
 */
public abstract class AbstractSearch<C, T> implements Search<T>, Cloneable {

    //****************** Attributes ******************//

    /** Comparator used for search */
    protected final IndexComparator<? super C, ? super T> comparator;
    /** Lower bound on search */
    protected final C from;
    /** Upper bound on search */
    protected final C to;
    /** Object returned by the previous call to next/prev */
    private T currentObject = null;


    //****************** Constructor ******************//

    /**
     * Creates a new instance of Search for the specified search comparator and [from,to] bounds.
     * @param comparator the comparator that is used to compare the bounds
     * @param from the lower bound on returned objects, i.e. objects greater or equal are returned
     * @param to the upper bound on returned objects, i.e. objects smaller or equal are returned
     */
    protected AbstractSearch(IndexComparator<? super C, ? super T> comparator, C from, C to) {
        this.comparator = comparator;
        this.from = from;
        this.to = to;
    }


    //****************** Search interface implementations ******************//

    public T getCurrentObject() {
        return currentObject;
    }

    public boolean next() throws IllegalStateException {
        try {
            for (currentObject = readNext(); currentObject != null; currentObject = readNext()) {
                if (checkBounds(currentObject))
                    return true;
            }
        } catch (BucketStorageException e) {
            throw new IllegalStateException("Error reading next object from the underlying storage", e);
        }

        return false;
    }

    public boolean skip(int count) throws IllegalStateException {
        while (count < 0 && previous())
            count++;
        while (count > 0 && next())
            count--;
        return count == 0;
    }

    public boolean previous() throws IllegalStateException {
        try {
            for (currentObject = readPrevious(); currentObject != null; currentObject = readPrevious()) {
                if (checkBounds(currentObject))
                    return true;
            }
        } catch (BucketStorageException e) {
            throw new IllegalStateException("Error reading previous object from the underlying storage", e);
        }

        return false;
    }


    //****************** Searching methods ******************//

    /**
     * Checks if the specified object is withing <code>[from, to]</code> bounds.
     * If the boundary is <tt>null</tt>, the check succeeds.
     * 
     * @param object the object to check the boundaries for
     * @return <tt>true</tt> if object is within <code>[from, to]</code>
     */
    protected boolean checkBounds(T object) {
        // No boundaries checks if comparator is null
        if (comparator == null)
            return true;

        // If from/to boundaries are the same object, we do just equality test
        if (from == to) {// this is correct (no equals!)
            return from == null || comparator.indexCompare(from, object) == 0; // from = object
        } else {
            // Do boundary check
            if (from != null && comparator.indexCompare(from, object) > 0) // from > object
                return false;
            if (to != null && comparator.indexCompare(to, object) < 0) // to < object
                return false;
            return true;
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


    //****************** Clonning ******************//

    @Override
    @SuppressWarnings("unchecked")
    public AbstractSearch<C, T> clone() throws CloneNotSupportedException {
        return (AbstractSearch<C, T>)super.clone(); // This cast is checked since this is clonning
    }

}
