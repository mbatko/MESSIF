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
 * Defines a modifiable ordered index interface on objects.
 * This index allows to add objects and remove them using {@link messif.buckets.Removable#remove}
 * method of the search results.
 * Index's order is defined by {@link IndexComparator} that can be accessed via
 * {@link #comparator() comparator()} method.
 * 
 * @param <C> the type keys this index is ordered by
 * @param <T> the type of objects stored in this index
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface ModifiableOrderedIndex<C, T> extends OrderedIndex<C, T>, ModifiableIndex<T>, Addible<T> {

    @Override
    public ModifiableSearch<T> search(C key, boolean restrictEqual) throws IllegalStateException;

    @Override
    public ModifiableSearch<T> search(Collection<? extends C> keys) throws IllegalStateException;

    @Override
    public ModifiableSearch<T> search(C from, C to) throws IllegalStateException;

    @Override
    public ModifiableSearch<T> search(C startKey, C from, C to) throws IllegalStateException;
}
