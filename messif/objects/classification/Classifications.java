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
 * Utility methods for classifications.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class Classifications {
    /**
     * Safe-casts the given object to classification.
     * @param <C> the class of instances that represent the classification categories
     * @param object the object to cast to classification
     * @param categoriesClass the class of instances that represent the classification categories
     * @return the object type-safely cast to classification
     * @throws ClassCastException if the given object does not implement the {@link Classification} interface or the categories are represented by a different class
     */
    public static <C> Classification<C> castToClassification(Object object, Class<? extends C> categoriesClass) throws ClassCastException {
        if (object == null)
            return null;
        @SuppressWarnings("unchecked")
        Classification<C> classification = (Classification<C>)object; // This cast IS checked on the next line
        if (classification.getStoredClass() == categoriesClass)
            return classification;
        throw new ClassCastException("Classification on different classes cannot be cast: " + classification.getStoredClass() + " and " + categoriesClass);
    }

    /**
     * Converts the given object to classification.
     * If the object is already a classification, is is returned as type-safe cast.
     * If the object is instance of the {@code categoriesClass}, a static array, or an {@link Iterable}
     * it is encapsulated into {@link ClassificationBase}. Note that the objects from
     * the {@link Iterable} and static arrays are type-checked to be compatible with {@code C}
     * and <tt>null</tt> items are silently ignored.
     *
     * @param <C> the class of instances that represent the classification categories
     * @param object the object to cast to classification
     * @param categoriesClass the class of instances that represent the classification categories
     * @return the object converted to classification
     * @throws ClassCastException if the given object does not implement the {@link Classification} interface or the categories are represented by a different class
     */
    public static <C> Classification<C> convertToClassification(Object object, Class<? extends C> categoriesClass) {
        if (object instanceof Classification) {
            return castToClassification(object, categoriesClass);
        } else if (categoriesClass.isInstance(object)) {
            return new ClassificationBase<C>(categoriesClass, 1).add(categoriesClass.cast(object));
        } else if (object instanceof Iterable) {
            return new ClassificationBase<C>(categoriesClass).addAll((Iterable<?>)object, false);
        } else if (object.getClass().isArray()) {
            return new ClassificationBase<C>(categoriesClass).addArray(object, false);
        }
        return null;
    }

    /**
     * Converts the given object to classification with confidence.
     * If the object is a classification with confidence, is is returned as type-safe cast.
     * If the object is instance of the {@code categoriesClass}, a static array, or an {@link Iterable}
     * it is encapsulated into {@link ClassificationWithConfidenceBase} and populated with
     * the respective objects and the given {@code confidence}. Note that the objects from
     * the {@link Iterable} and static arrays are type-checked to be compatible with {@code C}
     * and <tt>null</tt> items are silently ignored.
     *
     * @param <C> the class of instances that represent the classification categories
     * @param object the object to cast to classification
     * @param categoriesClass the class of instances that represent the classification categories
     * @param confidence the confidence to set for the objects that does not have one
     * @param lowestConfidence the lowest possible confidence of this classification
     * @param highestConfidence the highest possible confidence of this classification
     * @return the object converted to classification
     * @throws ClassCastException if the given object does not implement the {@link ClassificationWithConfidence} interface or the categories are represented by a different class
     */
    public static <C> ClassificationWithConfidence<C> convertToClassificationWithConfidence(Object object, Class<? extends C> categoriesClass, float confidence, float lowestConfidence, float highestConfidence) {
        if (object == null) {
            return null;
        } else if (object instanceof ClassificationWithConfidence) {
            return (ClassificationWithConfidence<C>)castToClassification(object, categoriesClass);
        } else if (object instanceof Classification) { // It is not a classification with confidence, so the confidence is added
            return new ClassificationWithConfidenceBase<C>(categoriesClass, lowestConfidence, highestConfidence).
                    addAll(castToClassification(object, categoriesClass), confidence, false);
        } else if (categoriesClass.isInstance(object)) {
            return new ClassificationWithConfidenceBase<C>(categoriesClass, lowestConfidence, highestConfidence, 1).add(categoriesClass.cast(object), confidence);
        } else if (object instanceof Iterable) {
            return new ClassificationWithConfidenceBase<C>(categoriesClass, lowestConfidence, highestConfidence).addAll((Iterable<?>)object, confidence, false);
        } else if (object.getClass().isArray()) {
            return new ClassificationWithConfidenceBase<C>(categoriesClass, lowestConfidence, highestConfidence).addArray(object, confidence, false);
        }
        return null;
    }
}
