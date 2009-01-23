/*
 *  Search
 * 
 */

package messif.buckets.index;

/**
 * This interface represents an initialized search on an index.
 * It allows to browse the data of an index - use {@link #next()} and {@link #previous()}
 * methods to gather the next or previous object of this search. If <tt>true</tt>
 * is returned, the next/previous found object can be retrieved by
 * {@link #getCurrentObject()}.
 * 
 * 
 * @param <T> the type of objects that this {@link Search} searches for
 * @author xbatko
 * @see Index
 */
public interface Search<T> extends Cloneable {

    /**
     * Returns the object found by the last search. That is, returns the object
     * found by the last call to {@link #next} or {@link #previous}. If these
     * methods returned <tt>false</tt>, <tt>null</tt> will be returned.
     * 
     * @return the object found by the last search
     */
    public T getCurrentObject();

    /**
     * Searches for the next object (forward search) and returns <tt>false</tt>
     * if none is found. Otherwise, the found object can be retrieved by
     * {@link #getCurrentObject()}.
     * 
     * @return <tt>true</tt> if a next satisfying object is found
     * @throws IllegalStateException if there was a problem retrieving the next object from the underlying storage
     */
    public boolean next() throws IllegalStateException;

    /**
     * Searches for the previous object (backward search) and returns <tt>false</tt>
     * if none is found. Otherwise, the found object can be retrieved by
     * {@link #getCurrentObject()}.
     * 
     * @return <tt>true</tt> if a previous satisfying object is found
     * @throws IllegalStateException if there was a problem retrieving the next object from the underlying storage
     */
    public boolean previous() throws IllegalStateException;

    /**
     * Creates and returns a copy of this search.
     * The new search instance retains the search state at the time of clonning,
     * thus continuing the search via calls to {@link #next} or {@link #previous}
     * will return the same values as for the original search.
     * 
     * <p>
     * In practice, the clonned search is often used to do the search in both
     * directions from the same starting point.
     * </p>
     *
     * @return a clonned instance of this search
     * @throws CloneNotSupportedException if this search cannot be cloned
     */
    public Search<T> clone() throws CloneNotSupportedException;

}
