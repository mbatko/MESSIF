/*
 *  AbstractStorageIndex
 * 
 */

package messif.buckets.index.impl;

import java.io.Serializable;
import messif.buckets.index.ModifiableIndex;
import messif.buckets.storage.Storage;

/**
 *
 * @param <T>
 * @author xbatko
 */
public abstract class AbstractStorageIndex<T> implements ModifiableIndex<T>, Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /** Storage associated with this index */
    protected final Storage<T> storage;

    /**
     * Creates a new instance of AbstractStorageIndex for the specified storage.
     * @param storage the storage to associate with this index
     */
    protected AbstractStorageIndex(Storage<T> storage) {
        this.storage = storage;
    }

}
