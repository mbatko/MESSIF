/*
 *  StorageIndexed
 * 
 */

package messif.buckets.storage;

import java.util.Collection;
import messif.buckets.index.IndexComparator;
import messif.buckets.index.ModifiableIndex;

/**
 * Interface of a generic storage that supports searching.
 * 
 * @param <T> the class of objects stored in this storage
 * @author xbatko
 */
public interface StorageIndexed<T> extends ModifiableIndex<T>, Storage<T> {
    @Override
    public StorageSearch<T> search() throws IllegalStateException;
    @Override
    public <C> StorageSearch<T> search(IndexComparator<? super C, ? super T> comparator, C key) throws IllegalStateException;
    @Override
    public <C> StorageSearch<T> search(IndexComparator<? super C, ? super T> comparator, Collection<? extends C> keys) throws IllegalStateException;
    @Override
    public <C> StorageSearch<T> search(IndexComparator<? super C, ? super T> comparator, C from, C to) throws IllegalStateException;
}
