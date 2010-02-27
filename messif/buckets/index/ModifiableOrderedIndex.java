/*
 *  Index
 * 
 */

package messif.buckets.index;

import java.util.Collection;
import messif.buckets.Addible;

/**
 * Defines a modifiable ordered index interface on objects.
 * This index allows to add objects and remove them using {@link messif.buckets.Removable#remove}
 * method of the search results.
 * Index's order is defined by {@link IndexComparator} that can be accessed via
 * {@link #comparator() comparator()} method.
 * 
 * @param <C> the type keys this index is ordered by
 * @param <T> the type of objects stored in this index
 * @author xbatko
 */
public interface ModifiableOrderedIndex<C, T> extends OrderedIndex<C, T>, ModifiableIndex<T>, Addible<T> {

    @Override
    public ModifiableSearch<T> search(C key, boolean restrictEqual) throws IllegalStateException;

    @Override
    public ModifiableSearch<T> search(Collection<? extends C> keys) throws IllegalStateException;

    @Override
    public ModifiableSearch<T> search(C from, C to) throws IllegalStateException;

    @Override
    public ModifiableSearch<T> search(C startKey, C from, C to) throws IllegalStateException;
}
