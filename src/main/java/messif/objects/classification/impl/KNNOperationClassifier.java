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
import messif.operations.RankingQueryOperation;
import messif.operations.query.KNNQueryOperation;
import messif.utility.Parametric;

/**
 * Implementation of a classifier by k-nearest neighbors operation.
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
public class KNNOperationClassifier<C> extends RankingQueryOperationClassifier<C> {

    /** Number of nearest neighbors to retrieve */
    private final int k;

    /**
     * Creates a new kNN classifier.
     * @param classifier the classifier used to compute the object classification
     * @param k the number of nearest neighbors to retrieve
     * @param algorithm the algorithm that supplies the similar objects
     * @param executedOperationParameter the name of the parameter to put the executed operation into when classifying
     */
    public KNNOperationClassifier(Classifier<? super RankingQueryOperation, C> classifier, int k, Algorithm algorithm, String executedOperationParameter) {
        super(classifier, algorithm, executedOperationParameter);
        this.k = k;
    }

    @Override
    protected RankingQueryOperation createOperation(LocalAbstractObject object) {
        return new KNNQueryOperation(object, k);
    }

    @Override
    public KNNQueryOperation getExecutedOperation(Parametric parameters) {
        return (KNNQueryOperation)super.getExecutedOperation(parameters);
    }
}
