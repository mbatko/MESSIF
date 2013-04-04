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

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

/**
 * Basic implementation of the {@link ClassificationWithConfidence} interface.
 * Note that class uses a {@link LinkedHashMap} as its internal storage,
 * so it is more memory-consuming than the {@link ClassificationBase}.
 * @param <C> the class of instances that represent the classification categories
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ClassificationWithConfidenceBase<C> extends ClassificationBase<C> implements ClassificationWithConfidence<C> {
    /** Serial version for {@link java.io.Serializable} */
    private static final long serialVersionUID = 1L;

    //****************** Constants *************//

    /** Constant representing an unknown confidence */
    public static final float UNKNOWN_CONFIDENCE = Float.NEGATIVE_INFINITY;


    //****************** Attributes *************//

    /** Classification holder; keys represent categories, values represent the confidences */
    private final Map<C, Float> confidenceMap;
    /** The lowest possible confidence of this classification */
    private final float lowestConfidence;
    /** The highest possible confidence of this classification */
    private final float highestConfidence;


    //****************** Constructors *************//

    /**
     * Creates an empty classification.
     * @param categoriesClass the class of instances that represent the classification categories
     * @param lowestConfidence the lowest possible confidence of this classification
     * @param highestConfidence the highest possible confidence of this classification
     */
    public ClassificationWithConfidenceBase(Class<? extends C> categoriesClass, float lowestConfidence, float highestConfidence) {
        this(categoriesClass, lowestConfidence, highestConfidence, 0);
    }

    /**
     * Creates an empty classification with the specified initial capacity.
     * @param categoriesClass the class of instances that represent the classification categories
     * @param lowestConfidence the lowest possible confidence of this classification
     * @param highestConfidence the highest possible confidence of this classification
     * @param initialCapacity the initial capacity of the classification
     */
    public ClassificationWithConfidenceBase(Class<? extends C> categoriesClass, float lowestConfidence, float highestConfidence, int initialCapacity) {
        this(categoriesClass, lowestConfidence, highestConfidence, new LinkedHashMap<C, Float>(initialCapacity));
    }

    /**
     * Creates an empty classification with the given storage instance.
     * @param categoriesClass the class of instances that represent the classification categories
     * @param lowestConfidence the lowest possible confidence of this classification
     * @param highestConfidence the highest possible confidence of this classification
     * @param storage the instance responsible for holding the objects in this classification
     * @throws NullPointerException if the {@code categoriesClass} or {@code storage} is <tt>null</tt>
     * @throws IllegalArgumentException if the specified initial capacity is negative
     */
    protected ClassificationWithConfidenceBase(Class<? extends C> categoriesClass, float lowestConfidence, float highestConfidence, Map<C, Float> storage) throws NullPointerException, IllegalArgumentException {
        super(categoriesClass, storage.keySet());
        this.confidenceMap = storage;
        this.lowestConfidence = lowestConfidence;
        this.highestConfidence = highestConfidence;
    }


    //****************** Update methods *************//

    /**
     * {@inheritDoc}
     * Category is added with {@link #UNKNOWN_CONFIDENCE unknown confidence}.
     */
    @Override
    public final ClassificationWithConfidenceBase<C> add(C category) {
        return add(category, UNKNOWN_CONFIDENCE);
    }

    /**
     * {@inheritDoc}
     * Categories are added with {@link #UNKNOWN_CONFIDENCE unknown confidence}.
     */
    @Override
    public final ClassificationWithConfidenceBase<C> addAll(Iterator<?> categories, boolean ignoreIncompatibleCategory) throws ClassCastException {
        return addAll(categories, UNKNOWN_CONFIDENCE, ignoreIncompatibleCategory);
    }

    /**
     * {@inheritDoc}
     * Categories are added with {@link #UNKNOWN_CONFIDENCE unknown confidence}.
     */
    @Override
    public final ClassificationWithConfidenceBase<C> addAll(Iterable<?> categories, boolean ignoreIncompatibleCategory) throws ClassCastException {
        return addAll(categories, UNKNOWN_CONFIDENCE, ignoreIncompatibleCategory);
    }

    /**
     * {@inheritDoc}
     * Categories are added with {@link #UNKNOWN_CONFIDENCE unknown confidence}.
     */
    @Override
    public final ClassificationWithConfidenceBase<C> addArray(Object array, boolean ignoreIncompatibleCategory) throws ClassCastException, IllegalArgumentException {
        return addArray(array, UNKNOWN_CONFIDENCE, ignoreIncompatibleCategory);
    }

    /**
     * Adds the given category with confidence to this classification.
     * Note that <tt>null</tt> category is silently ignored.
     * @param category the category to add
     * @param confidence the confidence of the category to add
     * @return this instance to allow chaining
     * @throws IllegalArgumentException if the confidence is not within the bounds
     */
    public ClassificationWithConfidenceBase<C> add(C category, float confidence) throws IllegalArgumentException {
        if (confidence != UNKNOWN_CONFIDENCE) {
            if (lowestConfidence < highestConfidence) {
                if (confidence < lowestConfidence || confidence > highestConfidence)
                    throw new IllegalArgumentException("Confidence " + confidence + " is not within [" + lowestConfidence + ";" + highestConfidence + "]");
            } else {
                if (confidence > lowestConfidence || confidence < highestConfidence)
                    throw new IllegalArgumentException("Confidence " + confidence + " is not within [" + highestConfidence + ";" + lowestConfidence + "]");
            }
        }
        if (category != null)
            confidenceMap.put(category, confidence);
        return this;
    }

    /**
     * Adds the categories provided by the iterator to this classification with the given confidence.
     * Note that the objects from the iterator are type-checked to be compatible with {@code C}
     * and <tt>null</tt> items are silently ignored.
     *
     * @param categories an iterator over the categories to add
     * @param confidence the confidence of the added categories
     * @param ignoreIncompatibleCategory flag whether to silently ignore incompatible categories
     *          from the iterator (<tt>true</tt>) or throw a {@link ClassCastException} (<tt>false</tt>)
     * @return this instance to allow chaining
     * @throws ClassCastException if there was an object incompatible with this classification categories
     */
    public ClassificationWithConfidenceBase<C> addAll(Iterator<?> categories, float confidence, boolean ignoreIncompatibleCategory) throws ClassCastException {
        while (categories.hasNext())
            add(castToStored(categories.next(), ignoreIncompatibleCategory), confidence);
        return this;
    }

    /**
     * Adds the categories provided by the {@link Iterable} to this classification with the given confidence.
     * Note that the objects from the {@link Iterable} are type-checked to be compatible with {@code C}
     * and <tt>null</tt> items are silently ignored.
     *
     * @param categories an {@link Iterable} over the categories to add
     * @param confidence the confidence of the added categories
     * @param ignoreIncompatibleCategory flag whether to silently ignore incompatible categories
     *          from the iterator (<tt>true</tt>) or throw a {@link ClassCastException} (<tt>false</tt>)
     * @return this instance to allow chaining
     * @throws ClassCastException if there was an object incompatible with this classification categories
     */
    public ClassificationWithConfidenceBase<C> addAll(Iterable<?> categories, float confidence, boolean ignoreIncompatibleCategory) throws ClassCastException {
        return addAll(categories.iterator(), confidence, ignoreIncompatibleCategory);
    }

    /**
     * Adds all the categories with confidence from the given classification into this.
     *
     * @param classification the classification with which to update this one
     * @return this instance to allow chaining
     * @throws IllegalArgumentException if the confidence is not within the bounds
     */
    public ClassificationWithConfidenceBase<C> addAll(ClassificationWithConfidence<C> classification) throws IllegalArgumentException {
        for (C category : classification)
            add(category, classification.getConfidence(category));
        return this;
    }

    /**
     * Adds the categories from a static array to this classification with the given confidence.
     * Note that the objects from the array are type-checked to be compatible with {@code C}
     * and <tt>null</tt> items are silently ignored.
     *
     * @param array a static array with the categories to add
     * @param confidence the confidence of the added categories
     * @param ignoreIncompatibleCategory flag whether to silently ignore incompatible categories
     *          from the array (<tt>true</tt>) or throw a {@link ClassCastException} (<tt>false</tt>)
     * @return this instance to allow chaining
     * @throws ClassCastException if there was an object incompatible with this classification categories
     * @throws IllegalArgumentException if the object {@code array} is not a static array
     */
    public ClassificationWithConfidenceBase<C> addArray(Object array, float confidence, boolean ignoreIncompatibleCategory) throws ClassCastException, IllegalArgumentException {
        int size = Array.getLength(array);
        for (int i = 0; i < size; i++)
            add(castToStored(Array.get(array, i), ignoreIncompatibleCategory), confidence);
        return this;
    }

    /**
     * Returns whether the confidence value should be updated with the new value.
     * @param currentValue the current confidence value
     * @param newValue the new confidence value
     * @return <tt>true</tt> if the confidence value should be updated
     */
    protected boolean isUpdatingConfidence(Float currentValue, float newValue) {
        if (currentValue == null || currentValue.isInfinite()) // Infinite is unknown
            return true;
        if (lowestConfidence <= highestConfidence) {
            return currentValue < newValue;
        } else {
            return currentValue > newValue;
        }
    }

    /**
     * Updates confidence of the given category.
     * If the category is not in this classification, it is added with the given confidence.
     * If the current confidence of the given category in this classification is lower than
     * the given one, the confidence is updated to the new value.
     * Otherwise, the classification is not updated.
     *
     * @param category the category for which to update the confidence
     * @param confidence the new confidence value
     * @return <tt>true</tt> if this classification was modified
     * @throws IllegalArgumentException if the confidence is not within the bounds
     */
    public boolean updateConfidence(C category, float confidence) throws IllegalArgumentException {
        if (isUpdatingConfidence(getConfidence(category), confidence)) {
            add(category, confidence);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Updates the all the categories with confidence in this classification.
     *
     * @param classification the classification with which to update this one
     * @return <tt>true</tt> if this classification was modified
     * @throws IllegalArgumentException if the confidence is not within the bounds
     */
    public boolean updateAllConfidences(ClassificationWithConfidence<C> classification) throws IllegalArgumentException {
        boolean ret = false;
        for (C category : classification) {
            if (updateConfidence(category, classification.getConfidence(category)))
                ret = true;
        }
        return ret;
    }


    //****************** Classification interface methods *************//

    @Override
    public float getConfidence(C category) throws NoSuchElementException {
        Float confidence = confidenceMap.get(category);
        if (confidence == null)
            return UNKNOWN_CONFIDENCE;
        return confidence;
    }

    @Override
    public float getLowestConfidence() {
        return lowestConfidence;
    }

    @Override
    public float getHighestConfidence() {
        return highestConfidence;
    }


    //****************** String conversion *************//

    @Override
    public String toString() {
        if (confidenceMap == null || confidenceMap.isEmpty())
            return "[]";
        StringBuilder str = new StringBuilder();
        str.append("[");
        Iterator<Entry<C, Float>> iterator = confidenceMap.entrySet().iterator();
        do { // Iterator has at least one item - see isEmpty check above
            Map.Entry<C, Float> entry = iterator.next();
            str.append(entry.getKey());
            if (entry.getValue() != null && !entry.getValue().isInfinite())
                str.append('(').append(entry.getValue()).append(')');

            if (iterator.hasNext())
                str.append(", ");
            else
                break;
        } while (true); // No need to check again for hasNext
        str.append("]");
        return str.toString();
    }

}
