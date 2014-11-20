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
import messif.utility.ModifiableParametric;
import messif.utility.Parametric;

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
    private final Classifier<? super RankingQueryOperation, C> classifier;
    /** Algorithm that supplies the similar objects */
    private final Algorithm algorithm;
    /** Name of the parameter to put the executed operation into when classifying */
    private final String executedOperationParameter;

    /**
     * Creates a new kNN classifier.
     * @param classifier the classifier used to compute the object classification
     * @param algorithm the algorithm that supplies the similar objects
     * @param executedOperationParameter the name of the parameter to put the executed operation into when classifying
     */
    public RankingQueryOperationClassifier(Classifier<? super RankingQueryOperation, C> classifier, Algorithm algorithm, String executedOperationParameter) {
        this.classifier = classifier;
        this.algorithm = algorithm;
        this.executedOperationParameter = executedOperationParameter;
    }

    @Override
    public Classification<C> classify(LocalAbstractObject object, Parametric parameters) throws ClassificationException {
        try {
            RankingQueryOperation op = algorithm.executeOperation(createOperation(object));
            if (parameters instanceof ModifiableParametric && executedOperationParameter != null)
                ((ModifiableParametric)parameters).setParameter(executedOperationParameter, op);
            return classifier.classify(op, parameters);
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

    /**
     * Returns the executed operation stored by this classifier in the given parameters.
     * @param parameters the parameters to get the executed operation from
     * @return the executed operation or <tt>null</tt> if no operation was stored in the parameters
     */
    public RankingQueryOperation getExecutedOperation(Parametric parameters) {
        if (executedOperationParameter == null || parameters == null)
            return null;
        return parameters.getParameter(executedOperationParameter, RankingQueryOperation.class);
    }

}
