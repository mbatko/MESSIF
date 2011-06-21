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

import java.util.NoSuchElementException;

/**
 * Represents a result of a {@link Classifier classification} that contains a confidence level.
 * The result is a list of categories that the target of the classification belongs to.
 * Depending on the implementation the confidence levels may be specified.
 *
 * @param <C> the class of instances that represent the classification categories
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface ClassificationWithConfidence<C> extends Classification<C> {

    /**
     * Returns the confidence for the given category.
     * The confidence is a number between {@link #getLowestConfidence()} and
     * {@link #getHighestConfidence()} that represents the classification's
     * confidence about a particular category assignment.
     *
     * @param category the category for which to get the confidence
     * @return the confidence of the given category
     * @throws NoSuchElementException if the category is not in this classification
     */
    public float getConfidence(C category) throws NoSuchElementException;

    /**
     * Returns the lowest possible confidence of this classification.
     * Note that this can be a maximal distance if this classification works with distances.
     * @return the lowest possible confidence of this classification
     */
    public float getLowestConfidence();

    /**
     * Returns the highest possible confidence of this classification.
     * Note that this can be zero if this classification works with distances.
     * @return the highest possible confidence of this classification
     */
    public float getHighestConfidence();
}
