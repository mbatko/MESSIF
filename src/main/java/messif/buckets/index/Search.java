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
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 * @see Index
 */
public interface Search<T> extends Cloneable {

    /**
     * Returns the object found by the last search. That is, if method {@link #next}
     * or {@link #previous} has returned <tt>true</tt>, this method returns the matching
     * object. If <tt>false</tt> has been returned, this method throws an {@link IllegalStateException}.
     * 
     * @return the object found by the last search
     * @throws IllegalStateException if there is no current object (next/previous method was not called or returned <tt>false</tt>)
     */
    public T getCurrentObject() throws IllegalStateException;

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
     * Skips <code>count</code> objects using {@link #next()} or {@link #previous()}
     * search and returns <tt>false</tt> if <code>count</code> objects cannot be skipped.
     * Otherwise, the found object can be retrieved by {@link #getCurrentObject()}.
     *
     * <p>
     * Note that this is equivalent to calling {@link #next()} or {@link #previous()}
     * while <tt>true</tt> is returned up to <code>count</code> times. So if the
     * method returns <tt>false</tt>, the {@link #getCurrentObject()} may not return
     * a valid object.
     * </p>
     *
     * @param count number of objects to skip, 
     *      i.e. the number of calls to {@link #next()} if count is positive
     *      or {@link #previous()} if count is negative
     * @return <tt>true</tt> if the <code>count</code> objects has been skipped
     * @throws IllegalStateException if there was a problem retrieving the next/previous object from the underlying storage
     */
    public boolean skip(int count) throws IllegalStateException;

    /**
     * Creates and returns a copy of this search.
     * The new search instance retains the search state at the time of cloning,
     * thus continuing the search via calls to {@link #next} or {@link #previous}
     * will return the same values as for the original search.
     * 
     * <p>
     * In practice, the cloned search is often used to do the search in both
     * directions from the same starting point.
     * </p>
     *
     * @return a cloned instance of this search
     * @throws CloneNotSupportedException if this search cannot be cloned
     */
    public Search<T> clone() throws CloneNotSupportedException;

    /**
     * Closes this search indicating that no objects will be retrieved.
     * Note that after this method is called, the searching methods
     * ({@link #next()}, {@link #previous()}, etc.) should <em>not</em> be called.
     */
    public void close();
}
