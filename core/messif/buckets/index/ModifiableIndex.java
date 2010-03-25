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

import java.util.Collection;
import messif.buckets.Addible;

/**
 * Defines a modifiable index interface on objects.
 * This index allows to add objects and remove them using {@link messif.buckets.Removable#remove}
 * method of the search results.
 * 
 * @param <T> the type of objects stored in this index
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface ModifiableIndex<T> extends Index<T>, Addible<T> {

    @Override
    public ModifiableSearch<T> search() throws IllegalStateException;

    @Override
    public <C> ModifiableSearch<T> search(IndexComparator<? super C, ? super T> comparator, C key) throws IllegalStateException;

    @Override
    public <C> ModifiableSearch<T> search(IndexComparator<? super C, ? super T> comparator, Collection<? extends C> keys) throws IllegalStateException;

    @Override
    public <C> ModifiableSearch<T> search(IndexComparator<? super C, ? super T> comparator, C from, C to) throws IllegalStateException;

    /**
     * Finalize this index. All transient resources associated with this
     * index are released.
     * After this method is called, the store and retrieval methods' behavior is unpredictable.
     *
     * @throws Throwable if there was an error while cleaning
     */
    public void finalize() throws Throwable;

    /**
     * Destroy this index. This method releases all resources (transient and persistent)
     * associated with this index.
     * After this method is called, the store and retrieval methods' behavior is unpredictable.
     *
     * @throws Throwable if there was an error while cleaning
     */
    public void destroy() throws Throwable;

}
