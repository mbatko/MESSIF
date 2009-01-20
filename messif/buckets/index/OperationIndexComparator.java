/*
 *  OperationIndexComparator
 * 
 */

package messif.buckets.index;

import messif.objects.LocalAbstractObject;
import messif.operations.QueryOperation;

/**
 * A comparison function, which imposes a <i>total ordering</i> on some
 * collection of keys. Objects stored in the index are compared using
 * keys for ordering; the {@link #extractKey} extracts a key for this
 * comparator from any indexed object. The imposed order also corresponds
 * to a certain {@link QueryOperation}, which can be obtained by
 * {@link #createIndexOperation}.
 * 
 * <p>
 * This IndexComparator is restricted to {@link LocalAbstractObject} only,
 * so that the query operation can work for them.
 * </p>
 * @param <K> the type of the key arguments of the comparison
 * @author xbatko
 * @see java.util.Comparator
 */
public interface OperationIndexComparator<K> extends IndexComparator<K, LocalAbstractObject> {

    /**
     * Creates a query operation for the given key.
     * The returned operation represents the same ordering as this index comparator.
     * @param key a key for which to create an operation
     * @return a new instance of query operation for the given key
     */
    public QueryOperation<?> createIndexOperation(K key);
}
