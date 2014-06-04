/*
 * This file is part of MESSIF library.
 *
 * MESSIF library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MESSIF library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.algorithms.impl;

import java.util.HashMap;
import messif.algorithms.Algorithm;
import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;
import messif.objects.NoDataObject;
import messif.objects.classification.Classification;
import messif.objects.classification.ClassificationException;
import messif.objects.classification.ClassificationWithConfidence;
import messif.objects.classification.Classifier;
import messif.operations.RankingSingleQueryOperation;
import messif.utility.ModifiableParametricBase;

/**
 * Algorithm wrapper for a {@link Classifier}.
 * The algorithm process any {@link RankingSingleQueryOperation} by passing its
 * query object to the classifier and converting the the resulting classification
 * items into {@link AbstractObject}.
 *
 * @param <C> the class of instances that represent the classification categories
 * @author Michal Batko <batko@fi.muni.cz>
 */
public class ClassifierAlgorithm<C> extends Algorithm {
    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;

    /** Wrapped classifier that is used for query execution */
    private final Classifier<? super LocalAbstractObject, C> classifier;

    /**
     * Creates a new instance of ClassifierAlgorithm for the given classifier.
     * @param classifier the wrapped classifier that is used for query execution
     */
    @AlgorithmConstructor(description = "create classifier algorithm wrapper", arguments = {"the classifier to wrap"})
    public ClassifierAlgorithm(Classifier<? super LocalAbstractObject, C> classifier) {
        super("Annotation");
        this.classifier = classifier;
    }

    /**
     * Execution of any {@link RankingSingleQueryOperation}.
     * The operation query object is passed to the classifier and
     * the resulting classification is converted to operation answer.
     * @param op the operation the query object of which to classify
     * @throws ClassificationException if there was an error creating the classification
     */
    public void classify(RankingSingleQueryOperation op) throws ClassificationException {
        Classification<C> classification = classify(op.getQueryObject());

        // Convert the classification items to result
        for (C item : classification) {
            op.addToAnswer(
                    itemToObject(item, classification),
                    itemToDistance(item, classification),
                    null
            );
        }
    }

    /**
     * Create the classification for the given object.
     * @param object the object for which to create the classification
     * @return the the classification for the given object
     * @throws ClassificationException if there was an error creating the classification
     */
    protected Classification<C> classify(LocalAbstractObject object) throws ClassificationException {
        return classifier.classify(object, new ModifiableParametricBase(new HashMap<String, Object>()));
    }

    /**
     * Convert a classification item to operation answer object.
     * Default implementation converts the given item to string which is used as
     * locator for a {@link NoDataObject}.
     *
     * @param item the classification item to convert
     * @param classification the classification from which the object originates
     * @return a converted operation answer object
     */
    protected AbstractObject itemToObject(C item, Classification<C> classification) {
        return new NoDataObject(item.toString());
    }

    /**
     * Retrieve operation answer distance for a given classification item.
     * Default implementation returns one minus the normalized confidence
     * of the given item or {@link LocalAbstractObject#UNKNOWN_DISTANCE unknown distance}
     * if the given classification is not providing confidence.
     *
     * @param item the classification item to get the distance for
     * @param classification the classification from which the object originates
     * @return a converted operation answer object
     */
    protected float itemToDistance(C item, Classification<C> classification) {
        if (!(classification instanceof ClassificationWithConfidence))
            return LocalAbstractObject.UNKNOWN_DISTANCE;
        ClassificationWithConfidence<C> classificationWithConfidence = (ClassificationWithConfidence<C>)classification;

        // Return one minus normalized confidence of the given item
        return 1 - (classificationWithConfidence.getConfidence(item) - classificationWithConfidence.getLowestConfidence()) /
                   (classificationWithConfidence.getLowestConfidence() - classificationWithConfidence.getHighestConfidence());
    }
}
