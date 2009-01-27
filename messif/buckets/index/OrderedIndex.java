/*
 *  OrderedIndex
 * 
 */

package messif.buckets.index;

/**
 * Defines an ordered index interface on objects.
 * The order is defined by {@link IndexComparator} that can be accessed via
 * {@link #comparator() comparator()} method.
 * 
 * @param <C> the type keys this index is ordered by
 * @param <T> the type of objects stored in this index
 * @author xbatko
 */
public interface OrderedIndex<C, T> extends Index<T> {

    /**
     * Returns the comparator that defines order of this index.
     * @return the comparator that defines order of this index
     */
    public IndexComparator<C, T> comparator();

    /**
     * Returns a search for objects in this index using a specified comparator.
     * The parameter <code>from</code> need not necessarily be of the same
     * class as the objects stored in this index, however, the comparator must be
     * able to compare it with the stored objects.
     * 
     * <p>
     * Objects are returned in the order defined by this index.
     * If the <code>restrictEqual</code> is <tt>true</tt>, the search is restricted
     * only to objects that are comparator-equal to <code>from</code>.
     * </p>
     * 
     * @param key the starting point of the search
     * @param restrictEqual if <tt>true</tt>, the search is restricted
     *          only to objects that are equal to <code>from</code>
     * @return a search for objects in this index
     * @throws IllegalStateException if there was an error initializing the search on this index
     */
    public Search<C, T> search(C key, boolean restrictEqual) throws IllegalStateException;

    /**
     * Returns a search for objects in this index using a specified comparator.
     * The boundaries <code>[from, to]</code> need not necessarily be of the same
     * class as the objects stored in this index, however, the comparator must be
     * able to compare the boundaries and the internal objects.
     * <p>
     * Objects are returned in the order defined by this index.
     * </p>
     * 
     * @param from the lower bound on the searched objects, i.e. objects greater or equal are returned
     * @param to the upper bound on the searched objects, i.e. objects smaller or equal are returned
     * @return a search for objects in this index
     * @throws IllegalStateException if there was an error initializing the search on this index
     */
    public Search<C, T> search(C from, C to) throws IllegalStateException;

    /**
     * Returns a search for objects in this index using a specified comparator.
     * The boundaries <code>[from, to]</code> need not necessarily be of the same
     * class as the objects stored in this index, however, the comparator must be
     * able to compare the boundaries and the internal objects. Search starts with
     * the object nearest to the <code>startKey</code>
     * <p>
     * Objects are returned in the order defined by this index.
     * </p>
     * 
     * @param startKey the key from which to start the search
     * @param from the lower bound on the searched objects, i.e. objects greater or equal are returned
     * @param to the upper bound on the searched objects, i.e. objects smaller or equal are returned
     * @return a search for objects in this index
     * @throws IllegalStateException if there was an error initializing the search on this index
     */
    public Search<C, T> search(C startKey, C from, C to) throws IllegalStateException;

}
