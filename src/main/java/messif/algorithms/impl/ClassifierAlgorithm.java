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
import messif.objects.classification.Classifications;
import messif.objects.classification.Classifier;
import messif.operations.RankingQueryOperation;
import messif.utility.ModifiableParametricBase;

/**
 * Algorithm wrapper for a {@link Classifier}.
 * The algorithm process any {@link RankingQueryOperation} by passing it to the classifier
 * and converting the the resulting classification items into {@link AbstractObject}.
 *
 * @param <C> the class of instances that represent the classification categories
 * @author Michal Batko <batko@fi.muni.cz>
 */
public class ClassifierAlgorithm<C> extends Algorithm {
    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;

    /** Wrapped classifier that is used for query execution */
    private final Classifier<? super RankingQueryOperation, C> classifier;

    /**
     * Creates a new instance of ClassifierAlgorithm for the given classifier.
     * The classifier must accept an instance of {@link RankingQueryOperation} as its argument.
     * @param classifier the wrapped classifier that is used for query execution
     */
    @AlgorithmConstructor(description = "create classifier algorithm wrapper", arguments = {"the classifier to wrap"})
    public ClassifierAlgorithm(Classifier<? super RankingQueryOperation, C> classifier) {
        super("Annotation");
        this.classifier = classifier;
    }

    /**
     * Execution of any {@link RankingQueryOperation}.
     * The operation is passed to the classifier and
     * the resulting classification is converted to operation answer.
     * @param op the operation the query object of which to classify
     * @throws ClassificationException if there was an error creating the classification
     */
    public void classify(RankingQueryOperation op) throws ClassificationException {
        Classification<C> classification = classifier.classify(op, new ModifiableParametricBase(new HashMap<String, Object>()));
        if (classification instanceof ClassificationWithConfidence)
            classificationWithConfidenceToAnswer((ClassificationWithConfidence<C>)classification, op);
        else
            classificationToAnswer(classification, op);
        op.endOperation();
    }

    /**
     * Converts the given classification to the operation answer.
     * Since the classification does not provide confidences, the distances
     * will be set to {@link LocalAbstractObject#UNKNOWN_DISTANCE}.
     * @param classification the classification to convert
     * @param op the operation the answer of which to fill
     */
    protected void classificationToAnswer(Classification<C> classification, RankingQueryOperation op) {
        // Convert the classification items to result
        for (C item : classification) {
            op.addToAnswer(
                    itemToObject(item, classification),
                    LocalAbstractObject.UNKNOWN_DISTANCE,
                    null
            );
        }
    }

    /**
     * Converts the given classification to the operation answer.
     * The classification ordering is preserved according to its lowest/highest confidence.
     * Note that the resulting distance is normalized from zero to one.
     *
     * @param classification the classification to convert
     * @param op the operation the answer of which to fill
     */
    protected void classificationWithConfidenceToAnswer(ClassificationWithConfidence<C> classification, RankingQueryOperation op) {
        boolean greaterThan = classification.getLowestConfidence() < classification.getHighestConfidence();
        float lowestConfidence = Classifications.getExtremeConfidence(classification, !greaterThan, classification.getHighestConfidence());
        float highestConfidence = Classifications.getExtremeConfidence(classification, greaterThan, classification.getLowestConfidence());
        // Convert the classification items to result
        for (C item : classification) {
            op.addToAnswer(
                    itemToObject(item, classification),
                    greaterThan ?
                            1 - (classification.getConfidence(item) - lowestConfidence) / (highestConfidence - lowestConfidence):
                            (classification.getConfidence(item) - highestConfidence) / (lowestConfidence - highestConfidence),
                    null
            );
        }
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
}
