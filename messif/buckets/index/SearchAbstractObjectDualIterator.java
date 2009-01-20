
package messif.buckets.index;

import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;

/**
 * Provides a bridge between {@link Search} and {@link AbstractObjectIterator}.
 * This iterator will go always one object forward then one object backward
 * from the initial object of the search.
 * 
 * @param <T> the class of the iterated objects
 * @author xbatko
 */
public class SearchAbstractObjectDualIterator<T extends LocalAbstractObject> extends SearchAbstractObjectIterator<T> {

    //****************** Attributes ******************//

    /** Wrapped search instance */
    private final Search<?, T> searchBck;
    /** Flag which search result to use (forward = true, backward = false) */
    private boolean fwdIsCurrent;

    //****************** Constructor ******************//

    /**
     * Creates a new instance of SearchAbstractObjectIterator for the specified {@link Search} instance.
     * @param search the {@link Search} instance to wrap by this iterator
     * @param limit limit the number of iterations (zero means unlimited)
     * @throws CloneNotSupportedException if there was an error clonning the search
     */
    public SearchAbstractObjectDualIterator(Search<?, T> search, int limit) throws CloneNotSupportedException {
        super(search, limit);
        this.searchBck = search.clone();
        this.fwdIsCurrent = false;
    }

    /**
     * Creates a new instance of SearchAbstractObjectIterator for the specified {@link Search} instance.
     * Number of iterations is not limited.
     * @param search the {@link Search} instance to wrap by this iterator
     * @throws CloneNotSupportedException if there was an error clonning the search
     */
    public SearchAbstractObjectDualIterator(Search<?, T> search) throws CloneNotSupportedException {
        this(search, Integer.MAX_VALUE);
    }


    //****************** Overrides ******************//

    @Override
    public T getCurrentObject() {
        return fwdIsCurrent?search.getCurrentObject():searchBck.getCurrentObject();
    }

    @Override
    public boolean hasNext() {
        if (isLimitReached())
            return false;

        if (hasNext == -1) {
            if (!fwdIsCurrent) {
                hasNext = search.next()?1:0; // Perform search
                if (hasNext == 0) {
                    hasNext = searchBck.previous()?1:0;
                } else {
                    fwdIsCurrent = true;
                }
            } else {
                hasNext = searchBck.previous()?1:0; // Perform search
                if (hasNext == 0) {
                    hasNext = search.next()?1:0;
                } else {
                    fwdIsCurrent = false;
                }
            }
        }

        return hasNext == 1;
     }
}
