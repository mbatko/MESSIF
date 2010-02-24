/*
 *  Index
 * 
 */

package messif.buckets.index;

import java.util.List;
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

    public ModifiableSearch<T> search() throws IllegalStateException;

    public <C> ModifiableSearch<T> search(IndexComparator<? super C, ? super T> comparator, C key) throws IllegalStateException;

    public <C> ModifiableSearch<T> search(IndexComparator<? super C, ? super T> comparator, List<? extends C> keys) throws IllegalStateException;

    public <C> ModifiableSearch<T> search(IndexComparator<? super C, ? super T> comparator, C from, C to) throws IllegalStateException;

    /**
     * Finalize this index. All transient resources associated with this
     * index are released.
     * After this method is called, the store and retrieval methods' behavior is unpredictable.
     *
     * @throws Throwable if there was an error while cleaning
     */
    public void finalize() throws Throwable;

    /**
     * Destroy this index. This method releases all resources (transient and persistent)
     * associated with this index.
     * After this method is called, the store and retrieval methods' behavior is unpredictable.
     *
     * @throws Throwable if there was an error while cleaning
     */
    public void destroy() throws Throwable;

}
