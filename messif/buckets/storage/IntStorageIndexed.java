/*
 *  IntStorageIndexed
 * 
 */

package messif.buckets.storage;

import java.util.List;
import messif.buckets.index.IndexComparator;

/**
 * Interface of an integer storage that supports searching.
 * 
 * @param <T> the class of objects stored in this storage
 * @author xbatko
 */
public interface IntStorageIndexed<T> extends StorageIndexed<T>, IntStorage<T> {
    public IntStorageSearch<T> search() throws IllegalStateException;
    public <C> IntStorageSearch<T> search(IndexComparator<? super C, ? super T> comparator, C key) throws IllegalStateException;
    public <C> IntStorageSearch<T> search(IndexComparator<? super C, ? super T> comparator, List<? extends C> keys) throws IllegalStateException;
    public <C> IntStorageSearch<T> search(IndexComparator<? super C, ? super T> comparator, C from, C to) throws IllegalStateException;
}
