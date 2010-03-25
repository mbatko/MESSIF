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
package messif.buckets;

import java.util.NoSuchElementException;

/**
 * Interface for classes that supports removal of a current object.
 * 
 * @param <T> the type of removable objects
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface Removable<T> {

    /**
     * Returns the current object (that can be removed).
     * @return the current object (that can be removed)
     * @throws NoSuchElementException if there is no current object
     */
    T getCurrentObject() throws NoSuchElementException;

    /**
     * Removes the current object.
     *
     * @throws IllegalStateException there is no current object to be removed
     *          or the current object has been removed (e.g. by a previous
     *          call to {@link #remove()})
     * @throws BucketStorageException if there was an error removing the object
     */
    void remove() throws IllegalStateException, BucketStorageException;
}
