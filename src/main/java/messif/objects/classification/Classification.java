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
package messif.objects.classification;

import java.util.Iterator;

/**
 * Represents a result of a {@link Classifier classification}.
 * The result is a list of categories that the target of the classification belongs to.
 * Depending on the implementation the confidence levels may be specified.
 *
 * @param <C> the class of instances that represent the classification categories
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface Classification<C> extends Iterable<C> {
    /**
     * Returns the class of instances that represent the classification categories (classes).
     * @return the class of instances that represent the classification categories
     */
    public Class<? extends C> getStoredClass();

    /**
     * Returns the number of categories of this classification.
     * @return the number of categories of this classification
     */
    public int size();

    /**
     * Returns whether this classification contains a given category or not.
     * @param category the category to search for
     * @return <tt>true</tt> if the given category is present in this classification
     * @throws NullPointerException if the given category is <tt>null</tt>
     */
    public boolean contains(C category) throws NullPointerException;

    /**
     * Returns an iterator over all categories of this classification.
     * @return an iterator over all categories of this classification
     */
    @Override
    public Iterator<C> iterator();
}
