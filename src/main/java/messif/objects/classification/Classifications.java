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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Comparator;
import messif.utility.SortedCollection;

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
     * @param confidence the confidence to set for the objects that do not have one
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

    /**
     * Returns the classification method of the given classifier.
     * @param classifier the classifier to get the classification method for
     * @return the classification method of the given classifier
     * @throws NullPointerException if the given classifier is <tt>null</tt> 
     */
    public static Method getClassifierClassifyMethod(Classifier<?, ?> classifier) throws NullPointerException {
        for (Method method : classifier.getClass().getMethods()) {
            if (method.getName().equals("classify")) {
                Class<?>[] methodArgTypes = method.getParameterTypes();
                if (methodArgTypes.length == 2)
                    return method;
            }
        }
        throw new InternalError("This should never happen - class that implements the Classifier interface MUST have a 'classify' method");
    }

    /**
     * Returns a {@link Comparator} for sorting categories of the given {@link Classification} according to confidences.
     * The ordering is from the lowest to the highest confidence (or vice versa if {@highestFirst = true}) as specified by the classification.
     *
     * @param <C> the class of instances that represent the classification categories
     * @param classification the classification the categories of which to sort
     * @param highestFirst flag whether to reverse the order to show the items with the highest confidence first
     * @return an instance of the category sorting comparator
     */
    public static <C> Comparator<C> getCategoriesConfidenceComparator(final ClassificationWithConfidence<C> classification, boolean highestFirst) {
        final int multiplier;
        if (classification.getLowestConfidence() <= classification.getHighestConfidence()) {
            multiplier = highestFirst ? -1 : 1;
        } else {
            multiplier = highestFirst ? 1 : -1;
        }
        return new Comparator<C>() {
            @Override
            public int compare(C o1, C o2) {
                return multiplier * Float.compare(classification.getConfidence(o1), classification.getConfidence(o2));
            }
        };
    }

    /**
     * Returns all categories of the given classification sorted by the given comparator.
     * @param <C> the class of instances that represent the classification categories
     * @param classification the classification the categories of which to sort
     * @param comparator the comparator to use for sorting categories
     * @return a sorted collection of all categories
     */
    public static <C> Collection<C> getSortedCategories(ClassificationWithConfidence<C> classification, Comparator<? super C> comparator) {
        SortedCollection<C> ret = new SortedCollection<C>(classification.size(), comparator);
        for (C c : classification)
            ret.add(c);
        return ret;
    }

    /**
     * Returns all categories of the given classification sorted by confidences.
     * The ordering is from the lowest confidence to the highest as specified by the classification.
     * @param <C> the class of instances that represent the classification categories
     * @param classification the classification the categories of which to sort
     * @return a sorted collection of all categories
     */
    public static <C> Collection<C> getSortedCategories(ClassificationWithConfidence<C> classification) {
        return getSortedCategories(classification, false);
    }

    /**
     * Returns all categories of the given classification sorted by confidences.
     * The ordering is from the lowest confidence to the highest as specified by the classification.
     * @param <C> the class of instances that represent the classification categories
     * @param classification the classification the categories of which to sort
     * @param highestFirst flag whether to reverse the order to show the items with the highest confidence first
     * @return a sorted collection of all categories
     */
    public static <C> Collection<C> getSortedCategories(ClassificationWithConfidence<C> classification, boolean highestFirst) {
        return getSortedCategories(classification, getCategoriesConfidenceComparator(classification, highestFirst));
    }

    /**
     * Returns the highest/lowest confidence from the classes in the given classification.
     * @param <C> the class of instances that represent the classification categories
     * @param classification the classification the extreme confidence to get
     * @param greaterThan the comparison direction
     * @param startFrom the smallest/highest confidence to initialize computation of the extreme from
     * @return the highest/lowest confidence
     */
    public static <C> float getExtremeConfidence(ClassificationWithConfidence<C> classification, boolean greaterThan, float startFrom) {
        for (C c : classification) {
            float confidence = classification.getConfidence(c);
            if (greaterThan) {
                if (confidence > startFrom)
                    startFrom = confidence;
            } else {
                if (confidence < startFrom)
                    startFrom = confidence;
            }
        }
        return startFrom;
    }
}
