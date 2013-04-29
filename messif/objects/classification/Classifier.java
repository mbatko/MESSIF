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

import messif.utility.Parametric;

/**
 * Establish a basic interface for classifiers.
 * The given object is {@link #classify(java.lang.Object) classified}
 * into zero, one, or several categories {@code C}.
 *
 * @param <T> the class of instances that are classified
 * @param <C> the class of instances that represent the classification categories
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 * @see Classification
 * @see ClassificationWithConfidence
 */
public interface Classifier<T, C> {

    /**
     * Classifies the given {@code object} into zero, one, or several categories {@code C}.
     * @param object the object to classify
     * @param parameters additional parameters for the classification;
     *          the values for the parameters are specific to a given classifier
     *          implementation and can be updated during the process if they are {@link messif.utility.ModifiableParametric}
     * @return a set of categories the object belongs to
     * @throws ClassificationException if there was an error classifying the object
     */
    public Classification<C> classify(T object, Parametric parameters) throws ClassificationException;

    /**
     * Returns the class of instances that represent the classification categories (classes).
     * @return the class of instances that represent the classification categories
     */
    public Class<? extends C> getCategoriesClass();

}
