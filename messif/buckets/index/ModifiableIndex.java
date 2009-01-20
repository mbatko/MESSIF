/*
 *  Index
 * 
 */

package messif.buckets.index;

import messif.buckets.Addible;

/**
 * Defines a modifiable index interface on objects.
 * This index allows to add objects and remove them using {@link messif.buckets.Removable#remove}
 * method of the search results.
 * 
 * @param <T> the type of objects stored in this index
 * @author xbatko
 */
public interface ModifiableIndex<T> extends Index<T>, Addible<T> {

    /**
     * Returns a search for all objects in this index.
     * Objects are returned in the natural order defined by this index's comparator.
     * @return a search for all objects in this index
     * @throws IllegalStateException if there was an error initializing the search on this index
     */
    public ModifiableSearch<?, T> search() throws IllegalStateException;

    /**
     * Returns a search for objects in this index using a specified comparator.
     * The parameter <code>from</code> need not necessarily be of the same
     * class as the objects stored in this index, however, the comparator must be
     * able to compare it with the stored objects.
     * 
     * <p>
     * Objects are returned in the natural order defined by this index's comparator.
     * If the <code>restrictEqual</code> is <tt>true</tt>, the search is restricted
     * only to objects that are comparator-equal to <code>from</code>.
     * </p>
     * 
     * @param <C> the type the boundaries used by the search
     * @param comparator compares the <code>from</code> with the stored objects
     * @param from the starting point of the search
     * @param restrictEqual if <tt>true</tt>, the search is restricted
     *          only to objects that are equal to <code>from</code>
     * @return a search for objects in this index
     * @throws IllegalStateException if there was an error initializing the search on this index
     */
    public <C> ModifiableSearch<C, T> search(IndexComparator<C, T> comparator, C from, boolean restrictEqual) throws IllegalStateException;

    /**
     * Returns a search for objects in this index using a specified comparator.
     * The boundaries <code>[from, to]</code> need not necessarily be of the same
     * class as the objects stored in this index, however, the comparator must be
     * able to compare the boundaries and the internal objects.
     * <p>
     * Objects are returned in the natural order defined by this index's comparator.
     * </p>
     * 
     * @param <C> the type the boundaries used by the search
     * @param comparator compares the boundaries <code>[from, to]</code> with the stored objects
     * @param from the lower bound on the searched objects, i.e. objects greater or equal are returned
     * @param to the upper bound on the searched objects, i.e. objects smaller or equal are returned
     * @return a search for objects in this index
     * @throws IllegalStateException if there was an error initializing the search on this index
     */
    public <C> ModifiableSearch<C, T> search(IndexComparator<C, T> comparator, C from, C to) throws IllegalStateException;

}
