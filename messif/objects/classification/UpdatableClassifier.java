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

/**
 * Extension of the {@link Classifier} that allows to add or remove objects from the classifier.
 *
 * @param <T> the class of instances that are classified
 * @param <C> the class of instances that represent the classification categories
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
*/
public interface UpdatableClassifier<T, C> extends Classifier<T, C> {
    /**
     * Adds a classified object to this classifier.
     * @param object the object to add
     * @param classification the object's classification
     * @return <tt>true</tt> if the classifier was modified
     * @throws ClassificationException if there was a problem adding the object to this classifier
     */
    public boolean addClasifiedObject(T object, C classification) throws ClassificationException;

    /**
     * Removes a classified object from this classifier.
     * @param object the object to remove
     * @return <tt>true</tt> if the classifier was modified
     * @throws ClassificationException if there was a problem removing the object from this classifier
     */
    public boolean removeClasifiedObject(T object) throws ClassificationException;
}
