/*
 *  Index
 * 
 */

package messif.buckets.index;

import java.util.Collection;

/**
 * Defines an index interface on objects.
 * 
 * @param <T> the type of objects stored in this index
 * @author xbatko
 */
public interface Index<T> {

    /**
     * Returns current number of objects in this index.
     * @return current number of objects in this index
     */
    public int size();

    /**
     * Returns a search for all objects in this index.
     * @return a search for all objects in this index
     * @throws IllegalStateException if there was an error initializing the search on this index
     */
    public Search<T> search() throws IllegalStateException;

    /**
     * Returns a search for objects in this index that have any of the specified keys.
     * The equality is checked exclusively by using the specified comparator, thus
     * <code>key</code> need not necessarily be of the same class as the objects stored
     * in this index and also consistency with {@link java.lang.Object#equals equals} is not required.
     *
     * <p>
     * Note that objects are <i>not</i> necessarily returned in the order defined by the comparator
     * </p>
     *
     * @param <C> the type of the key used by the search
     * @param comparator compares the <code>key</code> with the stored objects
     * @param key the key to search for
     * @return a search for objects in this index
     * @throws IllegalStateException if there was an error initializing the search on this index
     */
    public <C> Search<T> search(IndexComparator<? super C, ? super T> comparator, C key) throws IllegalStateException;

    /**
     * Returns a search for objects in this index that have any of the specified keys.
     * The equality is checked exclusively by using the specified comparator, thus
     * <code>key</code> need not necessarily be of the same class as the objects stored
     * in this index and also consistency with {@link java.lang.Object#equals equals} is not required.
     *
     * <p>
     * Note that objects are <i>not</i> necessarily returned in the order defined by the comparator
     * </p>
     *
     * @param <C> the type of the keys used by the search
     * @param comparator compares the <code>keys</code> with the stored objects
     * @param keys the keys to search for (at least one key must be given)
     * @return a search for objects in this index
     * @throws IllegalStateException if there was an error initializing the search on this index
     */
    public <C> Search<T> search(IndexComparator<? super C, ? super T> comparator, Collection<? extends C> keys) throws IllegalStateException;

    /**
     * Returns a search for objects in this index that are within the specified key-range.
     * The key boundaries <code>[from, to]</code> need not necessarily be of the same
     * class as the objects stored in this index, however, the comparator must be
     * able to compare the boundaries and the internal objects.
     * 
     * <p>
     * Note that objects are <i>not</i> returned in the order defined by the comparator
     * </p>
     * 
     * @param <C> the type the boundaries used by the search
     * @param comparator compares the boundaries <code>[from, to]</code> with the stored objects
     * @param from the lower bound on the searched objects, i.e. objects greater or equal are returned
     * @param to the upper bound on the searched objects, i.e. objects smaller or equal are returned
     * @return a search for objects in this index
     * @throws IllegalStateException if there was an error initializing the search on this index
     */
    public <C> Search<T> search(IndexComparator<? super C, ? super T> comparator, C from, C to) throws IllegalStateException;

}
