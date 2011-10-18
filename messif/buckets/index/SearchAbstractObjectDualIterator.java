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

import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;

/**
 * Provides a bridge between {@link Search} and {@link AbstractObjectIterator}.
 * This iterator will go always one object forward then one object backward
 * from the initial object of the search.
 * 
 * @param <T> the class of the iterated objects
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class SearchAbstractObjectDualIterator<T extends LocalAbstractObject> extends SearchAbstractObjectIterator<T> {

    //****************** Attributes ******************//

    /** Wrapped search instance */
    private final Search<T> searchBck;
    /** Flag which search result to use (forward = true, backward = false) */
    private boolean fwdIsCurrent;

    //****************** Constructor ******************//

    /**
     * Creates a new instance of SearchAbstractObjectIterator for the specified {@link Search} instance.
     * @param search the {@link Search} instance to wrap by this iterator
     * @param limit limit the number of iterations (zero means unlimited)
     * @throws CloneNotSupportedException if there was an error cloning the search
     */
    public SearchAbstractObjectDualIterator(Search<T> search, int limit) throws CloneNotSupportedException {
        super(search, limit);
        this.searchBck = search.clone();
        this.fwdIsCurrent = false;
    }

    /**
     * Creates a new instance of SearchAbstractObjectIterator for the specified {@link Search} instance.
     * Number of iterations is not limited.
     * @param search the {@link Search} instance to wrap by this iterator
     * @throws CloneNotSupportedException if there was an error cloning the search
     */
    public SearchAbstractObjectDualIterator(Search<T> search) throws CloneNotSupportedException {
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
