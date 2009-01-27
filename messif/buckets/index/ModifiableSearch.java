/*
 *  ModifiableSearch
 * 
 */

package messif.buckets.index;

import messif.buckets.Removable;

/**
 * Represents a modifiable search.
 * That is, this search that can remove objects once found.
 * 
 * @param <T> the type of objects that this {@link ModifiableSearch} searches for
 * @author xbatko
 */
public interface ModifiableSearch<T> extends Search<T>, Removable<T> {

}
