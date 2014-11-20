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
import messif.objects.LocalAbstractObject;
import messif.objects.classification.Classifier;
import messif.operations.Approximate;
import messif.operations.RankingQueryOperation;
import messif.operations.query.ApproxKNNQueryOperation;
import messif.utility.Parametric;

/**
 * Implementation of a classifier by approximate k-nearest neighbors operation.
 * The classification of an object is inferred from its most similar objects.
 * The actual classification is computed from the retrieved nearest neighbors
 * by a given {@link Classifier}.
 *
 * @param <C> the class of instances that represent the classification categories
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ApproxKNNOperationClassifier<C> extends RankingQueryOperationClassifier<C> {

    /** Number of nearest neighbors to retrieve */
    private final int k;
    /** Type of the local approximation parameter used. */
    private final Approximate.LocalSearchType localSearchType;
    /**
     * Value of the local approximation parameter. 
     * Its interpretation depends on the value of {@link #localSearchType}.
     */
    private final int localSearchParam;
    /**
     * Radius for which the answer is guaranteed as correct.
     * It is specified in the constructor and can influence the level of approximation.
     * An algorithm evaluating this query can also change this value, so it can
     * notify about the guarantees of evaluation.
     */
    private final float radiusGuaranteed;

    /**
     * Creates a new kNN classifier.
     * @param classifier the classifier used to compute the object classification
     * @param k the number of nearest neighbors to retrieve
     * @param localSearchParam local search parameter - typically approximation parameter
     * @param localSearchType type of the local search parameter
     * @param radiusGuaranteed radius within which the answer is required to be guaranteed as correct
     * @param algorithm the algorithm that supplies the similar objects
     * @param executedOperationParameter the name of the parameter to put the executed operation into when classifying
     */
    public ApproxKNNOperationClassifier(Classifier<? super RankingQueryOperation, C> classifier, int k, int localSearchParam, Approximate.LocalSearchType localSearchType, float radiusGuaranteed, Algorithm algorithm, String executedOperationParameter) {
        super(classifier, algorithm, executedOperationParameter);
        this.k = k;
        this.localSearchParam = localSearchParam;
        this.localSearchType = localSearchType;
        this.radiusGuaranteed = radiusGuaranteed;
    }

    @Override
    protected RankingQueryOperation createOperation(LocalAbstractObject object) {
        return new ApproxKNNQueryOperation(object, k, localSearchParam, localSearchType, radiusGuaranteed);
    }

    @Override
    public ApproxKNNQueryOperation getExecutedOperation(Parametric parameters) {
        return (ApproxKNNQueryOperation)super.getExecutedOperation(parameters);
    }
}
