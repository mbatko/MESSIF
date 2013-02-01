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
import messif.algorithms.Algorithm;
import messif.algorithms.AlgorithmMethodException;
import messif.objects.LocalAbstractObject;
import messif.objects.classification.Classification;
import messif.objects.classification.ClassificationException;
import messif.objects.classification.Classifier;
import messif.objects.classification.UpdatableClassifier;
import messif.objects.util.RankedAbstractObject;
import messif.operations.RankingQueryOperation;
import messif.operations.data.DeleteOperation;
import messif.operations.data.InsertOperation;

/**
 * Abstract implementation of a classifier that executes a {@link RankingQueryOperation}
 * on an encapsulated algorithm and computes the classification using encapsulated
 * classifier that processes the iterator of {@link RankedAbstractObject}s.
 *
 * @param <C> the class of instances that represent the classification categories
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class RankingQueryOperationClassifier<C> implements UpdatableClassifier<LocalAbstractObject, C> {

    /** Classifier used to compute the object classification */
    private final Classifier<? super Iterator<? extends RankedAbstractObject>, C> classifier;
    /** Algorithm that supplies the similar objects */
    private final Algorithm algorithm;

    /**
     * Creates a new kNN classifier.
     * @param classifier the classifier used to compute the object classification
     * @param algorithm the algorithm that supplies the similar objects
     */
    public RankingQueryOperationClassifier(Classifier<? super Iterator<? extends RankedAbstractObject>, C> classifier, Algorithm algorithm) {
        this.classifier = classifier;
        this.algorithm = algorithm;
    }

    @Override
    public Classification<C> classify(LocalAbstractObject object) throws ClassificationException {
        try {
            RankingQueryOperation op = algorithm.executeOperation(createOperation(object));
            return classifier.classify(op.getAnswer());
        } catch (AlgorithmMethodException e) {
            throw new ClassificationException("There was an error executing KNN query", e.getCause());
        } catch (NoSuchMethodException e) {
            throw new ClassificationException("Specified algorithm does not support KNN queries", e);
        }
    }

    /**
     * Creates a ranking operation to be executed to get the candidate list for classification.
     * @param object the object to classify
     * @return a new instance of the ranking operation to execute
     */
    protected abstract RankingQueryOperation createOperation(LocalAbstractObject object);

    @Override
    public boolean addClasifiedObject(LocalAbstractObject object, C classification) throws ClassificationException {
        try {
            InsertOperation op = algorithm.executeOperation(new InsertOperation(object));
            return op.wasSuccessful();
        } catch (AlgorithmMethodException e) {
            throw new ClassificationException("There was an error executing insert operation", e.getCause());
        } catch (NoSuchMethodException e) {
            throw new ClassificationException("Specified algorithm does not support insertion", e);
        }
    }

    @Override
    public boolean removeClasifiedObject(LocalAbstractObject object) throws ClassificationException {
        try {
            DeleteOperation op = algorithm.executeOperation(new DeleteOperation(object));
            return op.wasSuccessful();
        } catch (AlgorithmMethodException e) {
            throw new ClassificationException("There was an error executing delete operation", e.getCause());
        } catch (NoSuchMethodException e) {
            throw new ClassificationException("Specified algorithm does not support deletion", e);
        }
    }

    @Override
    public Class<? extends C> getCategoriesClass() {
        return classifier.getCategoriesClass();
    }

    @Override
    public Class<? extends LocalAbstractObject> getClassifiedClass() {
        return LocalAbstractObject.class;
    }

}
