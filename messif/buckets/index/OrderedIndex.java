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

/**
 * Defines an ordered index interface on objects.
 * The order is defined by {@link IndexComparator} that can be accessed via
 * {@link #comparator() comparator()} method.
 * 
 * @param <C> the type keys this index is ordered by
 * @param <T> the type of objects stored in this index
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface OrderedIndex<C, T> extends Index<T> {

    /**
     * Returns the comparator that defines order of this index.
     * @return the comparator that defines order of this index
     */
    public IndexComparator<C, T> comparator();

    /**
     * Returns a search for objects in this index using the internal {@link #comparator()} of this index.
     * If the <code>restrictEqual</code> is <tt>true</tt>, the search returns only
     * objects that are comparator-equal to <code>key</code>.
     * 
     * <p>
     * Objects are returned in the order defined by this index.
     * </p>
     * 
     * @param key the key to search for
     * @param restrictEqual if <tt>true</tt>, the search is restricted
     *          only to objects that are comparator-equal to <code>key</code>
     * @return a search for objects in this index
     * @throws IllegalStateException if there was an error initializing the search on this index
     */
    public Search<T> search(C key, boolean restrictEqual) throws IllegalStateException;

    /**
     * Returns a search for objects in this index using the internal {@link #comparator()} of this index.
     * All objects that are {@link #comparator() comparator}-equal to any of the given {@code keys} are returned.
     *
     * <p>
     * Objects are returned in the order defined by this index.
     * </p>
     *
     * @param keys the keys to search for
     * @return a search for objects in this index
     * @throws IllegalStateException if there was an error initializing the search on this index
     */
    public Search<T> search(Collection<? extends C> keys) throws IllegalStateException;

    /**
     * Returns a search for objects in this index using the internal {@link #comparator()} of this index.
     * All objects from the interval <code>[from, to]</code> are returned. If a <tt>null</tt>
     * value is specified as a boundary, that bound is not restricted. That means that
     * {@code search(x, null)} will return all objects from this index that are bigger than
     * or equal to {@code x}.
     *
     * <p>
     * Objects are returned in the order defined by this index.
     * </p>
     * 
     * @param from the lower bound on the searched objects, i.e. objects greater or equal are returned
     * @param to the upper bound on the searched objects, i.e. objects smaller or equal are returned
     * @return a search for objects in this index
     * @throws IllegalStateException if there was an error initializing the search on this index
     */
    public Search<T> search(C from, C to) throws IllegalStateException;

    /**
     * Returns a search for objects in this index using the internal {@link #comparator()} of this index.
     * All objects from the interval <code>[from, to]</code> are returned. Search
     * starts at the object nearest to the given {@code startKey}. If a <tt>null</tt>
     * value is specified as a boundary, that bound is not restricted. That means that
     * {@code search(start, x, null)} will return all objects from this index that are bigger than
     * or equal to {@code x} starting at object with key {@code start}.
     * <p>
     * Objects are returned in the order defined by this index.
     * </p>
     * 
     * @param startKey the key from which to start the search
     * @param from the lower bound on the searched objects, i.e. objects greater or equal are returned
     * @param to the upper bound on the searched objects, i.e. objects smaller or equal are returned
     * @return a search for objects in this index
     * @throws IllegalStateException if there was an error initializing the search on this index
     */
    public Search<T> search(C startKey, C from, C to) throws IllegalStateException;

}
