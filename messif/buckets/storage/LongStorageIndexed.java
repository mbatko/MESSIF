/*
 *  IntStorageIndexed
 * 
 */

package messif.buckets.storage;

import java.util.Collection;
import messif.buckets.index.IndexComparator;

/**
 * Interface of a long storage that supports searching.
 * 
 * @param <T> the class of objects stored in this storage
 * @author xbatko
 */
public interface LongStorageIndexed<T> extends StorageIndexed<T>, LongStorage<T> {
    @Override
    public LongStorageSearch<T> search() throws IllegalStateException;
    @Override
    public <C> LongStorageSearch<T> search(IndexComparator<? super C, ? super T> comparator, C key) throws IllegalStateException;
    @Override
    public <C> LongStorageSearch<T> search(IndexComparator<? super C, ? super T> comparator, Collection<? extends C> keys) throws IllegalStateException;
    @Override
    public <C> LongStorageSearch<T> search(IndexComparator<? super C, ? super T> comparator, C from, C to) throws IllegalStateException;
}
