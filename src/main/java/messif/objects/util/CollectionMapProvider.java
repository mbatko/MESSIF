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
 * Interface for instances that provide collections that are accessed by object keys.
 * @param <K> the type of keys used for accessing the collections
 * @param <T> the type of objects stored in the collections
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface CollectionMapProvider<K, T> extends CollectionProvider<T> {
    /**
     * Returns the collection with the given key.
     * If no collection is available for the given key, <tt>null</tt> is returned.
     * Note that the returned collection (typically) cannot be modified.
     * @param key the key of the collection to return
     * @return the collection with the given key or <tt>null</tt>
     */
    public Collection<T> getCollectionByKey(K key);

    /**
     * Returns the class of the collection keys.
     * @return the class of the collection keys
     */
    public Class<? extends K> getCollectionKeyClass();
}
