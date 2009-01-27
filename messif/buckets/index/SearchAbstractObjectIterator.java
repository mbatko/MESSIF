
package messif.buckets.index;

import java.util.NoSuchElementException;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;

/**
 * Provides a bridge between {@link Search} and {@link AbstractObjectIterator}.
 * 
 * @param <T> the class of the iterated objects
 * @author xbatko
 */
public class SearchAbstractObjectIterator<T extends LocalAbstractObject> extends AbstractObjectIterator<T> {

    //****************** Attributes ******************//

    /** Wrapped search instance */
    protected final Search<T> search;
    /** Flag for remembering if next() has been called on <code>search</code> and its result */
    protected int hasNext;
    /** Maximal number of iterations */
    private final int limit;
    /** Current number of iterations */
    private int count;

    //****************** Constructor ******************//

    /**
     * Creates a new instance of SearchAbstractObjectIterator for the specified {@link Search} instance.
     * @param search the {@link Search} instance to wrap by this iterator
     * @param limit limit the number of iterations (zero means unlimited)
     */
    public SearchAbstractObjectIterator(Search<T> search, int limit) {
        this.search = search;
        this.hasNext = -1;
        this.limit = limit;
        this.count = 0;
    }

    /**
     * Creates a new instance of SearchAbstractObjectIterator for the specified {@link Search} instance.
     * @param search the {@link Search} instance to wrap by this iterator
     */
    public SearchAbstractObjectIterator(Search<T> search) {
        this(search, Integer.MAX_VALUE);
    }


    //****************** Attribute access methods ******************//

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

    /**
     * Returns <tt>true</tt> if the current number of iterations has reached its maximum.
     * @return <tt>true</tt> if the current number of iterations has reached its maximum
     */
    public final boolean isLimitReached() {
        return (count > limit);
    }


    //****************** Overrides ******************//

    public T getCurrentObject() {
        return search.getCurrentObject();
    }

    public boolean hasNext() {
        // Check limit
        if (isLimitReached())
            return false;

        // If the next was called, thus hasNext is not decided yet
        if (hasNext == -1)
            hasNext = search.next()?1:0; // Perform search

        return hasNext == 1;
    }

    public T next() throws NoSuchElementException {
        if (!hasNext())
            throw new NoSuchElementException("There are no more objects");
        hasNext = -1;
        count++;
        
        return getCurrentObject();
    }
    
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("This iterator does not support removal");
    }

}
