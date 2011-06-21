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
package messif.objects.classification.impl;

import java.util.Iterator;
import messif.objects.LocalAbstractObject;
import messif.objects.classification.ClassificationException;
import messif.objects.classification.ClassificationWithConfidence;
import messif.objects.classification.ClassificationWithConfidenceBase;
import messif.objects.classification.Classifications;
import messif.objects.classification.Classifier;
import messif.objects.util.RankedAbstractObject;
import messif.utility.Parametric;

/**
 * Implementation of a classifier that computes a {@link ClassificationWithConfidence}
 * from the given distance-ranked objects. The categories are derived from
 * the {@link Parametric} parameter with the given name. The confidences of the
 * respective categories are the distance-ranks of the respective objects that
 * provide them. The overall classification is computed as the minimal distance-rank
 * of each category.
 * 
 * @param <C> the class of instances that represent the classification categories
 * @see Classifications#convertToClassificationWithConfidence
 * @see ClassificationWithConfidenceBase#updateAllConfidences
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ParametricRankedObjectDistanceClassifier<C> implements Classifier<Iterator<? extends RankedAbstractObject>, C> {

    /** Class of instances that represent the classification categories */
    private final Class<? extends C> categoriesClass;
    /** Name of the {@link Parametric} parameter that contains the classification categories */
    private final String categoriesParameterName;

    /**
     * Creates a new instance of ParametricRankedObjectDistanceClassifier.
     * @param categoriesClass the class of instances that represent the classification categories
     * @param categoriesParameterName the name of the {@link Parametric} parameter that contains the classification categories
     */
    public ParametricRankedObjectDistanceClassifier(Class<? extends C> categoriesClass, String categoriesParameterName) {
        this.categoriesClass = categoriesClass;
        this.categoriesParameterName = categoriesParameterName;
    }

    @Override
    public ClassificationWithConfidence<C> classify(Iterator<? extends RankedAbstractObject> iterator) throws ClassificationException {
        ClassificationWithConfidenceBase<C> ret = new ClassificationWithConfidenceBase<C>(categoriesClass, LocalAbstractObject.MAX_DISTANCE, LocalAbstractObject.MIN_DISTANCE);
        while (iterator.hasNext())
            ret.updateAllConfidences(getClassification(iterator.next()));
        return ret;
    }

    /**
     * Retrieves the classification from the object.
     * Note that the encapsulated {@link RankedAbstractObject#getObject() object}
     * must implement the {@link Parametric} interface.
     * Note also that the rank of the object is used as the confidence.
     * @param object the ranked object for which to get the classification
     * @return the classification of the given ranked object or <tt>null</tt> if
     *      the parameter is <tt>null</tt> or cannot be converted to classification
     */
    protected ClassificationWithConfidence<C> getClassification(RankedAbstractObject object) {
        return Classifications.convertToClassificationWithConfidence(
            ((Parametric)object.getObject()).getParameter(categoriesParameterName),
            categoriesClass,
            object.getDistance(),
            LocalAbstractObject.MAX_DISTANCE, LocalAbstractObject.MIN_DISTANCE
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Iterator<RankedAbstractObject>> getClassifiedClass() {
        return (Class)Iterator.class;
    }

    @Override
    public Class<? extends C> getCategoriesClass() {
        return categoriesClass;
    }
    
}
