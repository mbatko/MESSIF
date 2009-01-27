/*
 *  Search
 * 
 */

package messif.buckets.index;

import java.util.Comparator;
import messif.buckets.BucketStorageException;

/**
 * This class represents a search on an index.
 * Search parameters are specified in the constructor, namely the objects
 * are searched using the specified <code>comparator</code> that is checked on the
 * [<code>from</code>, <code>to</code>] boundaries. Specifically, all objects
 * that are bigger or equal to <code>from</code> and smaller or equal to <code>to</code>
 * are returned, i.e.
 * {@link Comparator#compare comparator.compare}<code>(from, o) &lp= 0</code> and
 * {@link Comparator#compare comparator.compare}<code>(to, o) == 0</code> holds.
 * 
 * <p>
 * The {@link Comparator#compare comparator.compare} method will always have
 * the <code>from/to</code> attributes passesed as the first argument and
 * the object that is checked as the second argument.
 * </p>
 * 
 * @param <C> the type the boundaries used by the search
 * @param <T> the type of objects that this {@link Search} searches for
 * @author xbatko
 * @see Index
 */
public abstract class Search<C, T> implements Cloneable {

    /** Comparator used for search */
    private final IndexComparator<C, T> comparator;
    /** Lower bound on search */
    private final C from;
    /** Upper bound on search */
    private final C to;
    /** Object returned by the previous call to next/prev */
    private T currentObject = null;

    /**
     * Creates a new instance of Search for the specified search comparator and [from,to] bounds.
     * @param comparator the comparator that defines the 
     * @param from the lower bound on returned objects, i.e. objects greater or equal are returned
     * @param to the upper bound on returned objects, i.e. objects smaller or equal are returned
     */
    protected Search(IndexComparator<C, T> comparator, C from, C to) {
        this.comparator = comparator;
        this.from = from;
        this.to = to;
    }

    /**
     * Returns the object found by the last search. That is, returns the object
     * found by the last call to {@link #next} or {@link #previous}. If these
     * methods returned <tt>false</tt>, <tt>null</tt> will be returned.
     * 
     * @return the object found by the last search
     */
    public T getCurrentObject() {
        return currentObject;
    }

    /**
     * Searches for the next object (forward search) and returns <tt>false</tt>
     * if none is found. Otherwise, the found object can be retrieved by
     * {@link #getCurrentObject()}.
     * 
     * @return <tt>true</tt> if a next satisfying object is found
     * @throws IllegalStateException if there was a problem retrieving the next object from the underlying storage
     */
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

    /**
     * Searches for the previous object (backward search) and returns <tt>false</tt>
     * if none is found. Otherwise, the found object can be retrieved by
     * {@link #getCurrentObject()}.
     * 
     * @return <tt>true</tt> if a previous satisfying object is found
     * @throws IllegalStateException if there was a problem retrieving the next object from the underlying storage
     */
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
            return from == null || comparator.compare(from, object) == 0; // from = object
        } else {
            // Do boundary check
            if (from != null && comparator.compare(from, object) > 0) // from > object
                return false;
            if (to != null && comparator.compare(to, object) < 0) // to < object
                return false;
            return true;
        }
    }

    /**
     * Returns the next sibling object of the current one.
     * No checks on boundaries are required.
     * 
     * @return the next sibling object of the current one
     * @throws BucketStorageException if there was a problem retrieving the next object from the underlying storage
     */
    protected abstract T readNext() throws BucketStorageException;

    /**
     * Returns the previous sibling object of the current one.
     * No checks on boundaries are required.
     * 
     * @return the previous sibling object of the current one
     * @throws BucketStorageException if there was a problem retrieving the previous object from the underlying storage
     */
    protected abstract T readPrevious() throws BucketStorageException;

    @Override
    @SuppressWarnings("unchecked")
    public Search<C, T> clone() throws CloneNotSupportedException {
        return (Search<C, T>)super.clone(); // This cast is checked since this is clonning
    }

}
