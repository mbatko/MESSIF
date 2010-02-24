/*
 *  StorageIndexed
 * 
 */

package messif.buckets.storage;

import java.util.List;
import messif.buckets.index.IndexComparator;
import messif.buckets.index.ModifiableIndex;

/**
 * Interface of a generic storage that supports searching.
 * 
 * @param <T> the class of objects stored in this storage
 * @author xbatko
 */
public interface StorageIndexed<T> extends ModifiableIndex<T>, Storage<T> {
    public StorageSearch<T> search() throws IllegalStateException;
    public <C> StorageSearch<T> search(IndexComparator<? super C, ? super T> comparator, C key) throws IllegalStateException;
    public <C> StorageSearch<T> search(IndexComparator<? super C, ? super T> comparator, List<? extends C> keys) throws IllegalStateException;
    public <C> StorageSearch<T> search(IndexComparator<? super C, ? super T> comparator, C from, C to) throws IllegalStateException;
}
