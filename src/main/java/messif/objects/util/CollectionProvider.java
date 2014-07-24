/*
 * This file is part of MESSIF library.
 *
 * MESSIF library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MESSIF library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.objects.util;

import java.util.Collection;

/**
 * Interface for instances that provide collections.
 * @param <T> the type of objects stored in the collections
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface CollectionProvider<T> {
    /**
     * Returns the number of collections.
     * @return the number of collections
     */
    public int getCollectionCount();

    /**
     * Returns the collection with the given index.
     * Note that the returned collection (typically) cannot be modified.
     * @param index the index of the collection to return
     * @return the collection with the given index
     * @throws IndexOutOfBoundsException if the given index is negative or
     *          greater or equal to {@link #getCollectionCount()}
     */
    public Collection<T> getCollection(int index) throws IndexOutOfBoundsException;

    /**
     * Returns the class of the objects stored in the collections.
     * @return the class of the objects stored in the collections
     */
    public Class<? extends T> getCollectionValueClass();    
}
