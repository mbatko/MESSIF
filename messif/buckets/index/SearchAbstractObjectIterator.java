/*
 *  This file is part of MESSIF library.
 *
 *  MESSIF library is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MESSIF library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MESSIF library.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.buckets.index;

import java.util.NoSuchElementException;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;

/**
 * Provides a bridge between {@link Search} and {@link AbstractObjectIterator}.
 * 
 * @param <T> the class of the iterated objects
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
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
