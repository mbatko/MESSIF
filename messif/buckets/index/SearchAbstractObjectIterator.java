
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
    private final Search<?, T> search;
    
    /** Flag for remembering if next() has been called on <code>search</code> and its result */
    private int hasNext;

    /** Number of objects accessed during iteration by this iterator. */
    private int accessedObjects = 0;

    public int getAccessedObjects() {
        return accessedObjects;
    }

    //****************** Constructor ******************//

    /**
     * Creates a new instance of SearchAbstractObjectIterator for the specified {@link Search} instance.
     * @param search the {@link Search} instance to wrap by this iterator
     */
    public SearchAbstractObjectIterator(Search<?, T> search) {
        this.search = search;
        this.hasNext = -1;
    }


    //****************** Overrides ******************//

    public T getCurrentObject() {
        return search.getCurrentObject();
    }

    public boolean hasNext() {
        // If the next was called, thus hasNext is not decided yet
        if (hasNext == -1)
            hasNext = search.next()?1:0; // Perform search

        return hasNext == 1;
    }

    public T next() throws NoSuchElementException {
        if (!hasNext())
            throw new NoSuchElementException("There are no more objects");
        hasNext = -1;
        accessedObjects++;
        
        return search.getCurrentObject();
    }
    
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("This iterator does not support removal");
    }

}
