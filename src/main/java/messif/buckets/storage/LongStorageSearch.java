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
package messif.buckets.storage;

import messif.buckets.index.ModifiableSearch;

/**
 * This interface represents a {@link ModifiableSearch} that supports getting
 * {@link LongStorage}'s address of the found object.
 * 
 * @param <T> the type of objects that are looked up
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface LongStorageSearch<T> extends StorageSearch<T> {

    /**
     * Returns the address of the object found by the last search. That is, if method {@link #next}
     * or {@link #previous} has returned <tt>true</tt>, this method returns the address of the matching
     * object. If <tt>false</tt> has been returned, this method throws an {@link IllegalStateException}.
     *
     * <p>
     * Note that even though the address can be used to retrieve the actual object,
     * the {@link #getCurrentObject()} should be used instead, since it is usually faster.
     * </p>
     * @return the address of the object found by the last search
     * @throws IllegalStateException if there is no current object (next/previous method was not called or returned <tt>false</tt>)
     */
    public long getCurrentObjectLongAddress() throws IllegalStateException;

    @Override
    public LongAddress<T> getCurrentObjectAddress() throws IllegalStateException;
}
