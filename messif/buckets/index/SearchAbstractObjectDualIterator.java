
package messif.buckets.index;

import java.util.NoSuchElementException;
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
public class SearchAbstractObjectDualIterator<T extends LocalAbstractObject> extends AbstractObjectIterator<T> {

    //****************** Attributes ******************//

    /** Wrapped search instance */
    private final Search<?, T> searchFwd;
    /** Wrapped search instance */
    private final Search<?, T> searchBck;
    /** Flag for remembering if next() has been called on <code>search</code> and its result */
    private int hasNext;
    /** Flag which search result to use (forward = true, backward = false) */
    private boolean fwdIsCurrent;
    /** Maximal number of iterations */
    private final int limit;
    /** Current number of iterations */
    private int count;

    //****************** Constructor ******************//

    /**
     * Creates a new instance of SearchAbstractObjectIterator for the specified {@link Search} instance.
     * @param search the {@link Search} instance to wrap by this iterator
     * @param limit limit the number of iterations (zero means unlimited)
     * @throws CloneNotSupportedException if there was an error clonning the search
     */
    public SearchAbstractObjectDualIterator(Search<?, T> search, int limit) throws CloneNotSupportedException {
        this.searchFwd = search;
        this.searchBck = search.clone();
        this.hasNext = -1;
        this.fwdIsCurrent = false;
        this.limit = (limit <= 0)?Integer.MAX_VALUE:limit;
        this.count = 0;
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

    /**
     * Returns the current number of iterations.
     * @return the current number of iterations
     */
    public int getCount() {
        return count;
    }

    /**
     * Returns the maximal number of iterations.
     * Zero means unlimited.
     * @return the maximal number of iterations
     */
    public int getLimit() {
        return (limit == Integer.MAX_VALUE)?0:limit;
    }


    //****************** Overrides ******************//

    public T getCurrentObject() {
        return fwdIsCurrent?searchFwd.getCurrentObject():searchBck.getCurrentObject();
    }

    public boolean hasNext() {
        if (hasNext == -1) {
            if (!fwdIsCurrent) {
                hasNext = searchFwd.next()?1:0; // Perform search
                if (hasNext == 0) {
                    hasNext = searchBck.previous()?1:0;
                } else {
                    fwdIsCurrent = true;
                }
            } else {
                hasNext = searchBck.previous()?1:0; // Perform search
                if (hasNext == 0) {
                    hasNext = searchFwd.next()?1:0;
                } else {
                    fwdIsCurrent = false;
                }
            }

            // Increment iterations count
            if (hasNext == 1)
                count++;
        }

        return (count <= limit)?(hasNext == 1):false;
     }

    public T next() throws NoSuchElementException {
        if (!hasNext())
            throw new NoSuchElementException("There are no more objects");
        hasNext = -1;
        
        return getCurrentObject();
    }
    
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("This iterator does not support removal");
    }

}
