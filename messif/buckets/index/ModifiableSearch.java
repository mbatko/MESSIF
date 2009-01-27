/*
 *  ModifiableSearch
 * 
 */

package messif.buckets.index;

import messif.buckets.Removable;

/**
 *
 * @author xbatko
 */
public abstract class ModifiableSearch<C, T> extends Search<C, T> implements Removable<T> {

    /**
     * Creates a new instance of Search for the specified search comparator and [from,to] bounds.
     * @param comparator the comparator that defines the 
     * @param from the lower bound on returned objects, i.e. objects greater or equal are returned
     * @param to the upper bound on returned objects, i.e. objects smaller or equal are returned
     */
    protected ModifiableSearch(IndexComparator<C, T> comparator, C from, C to) {
        super(comparator, from, to);
    }

}
