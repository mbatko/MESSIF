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
package messif.operations.query;

import messif.objects.LocalAbstractObject;
import messif.objects.util.RankedSortedCollection;
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;
import messif.operations.Approximate;
import messif.operations.RankingQueryOperation;

/**
 * Approximate k-nearest neighbors query with specific early termination parameters
 * and support for obtaining some guarantees on results.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("Approximate k-nearest neighbors query")
public class ApproxKNNQueryOperation extends KNNQueryOperation implements Approximate {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 3L;

    //****************** Attributes ******************//

    /** Type of the local approximation parameter used. */
    protected LocalSearchType localSearchType;

    /**
     * Value of the local approximation parameter. 
     * Its interpretation depends on the value of {@link #localSearchType}.
     */
    protected int localSearchParam;

    /** Radius for which the answer is guaranteed as correct.
     * It is specified in the constructor and can influence the level of approximation.
     * An algorithm evaluating this query can also change this value, so it can
     * notify about the guarantees of evaluation.
     */
    protected float radiusGuaranteed;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of ApproxkNNQueryOperation for a given query object and maximal number of objects to return.
     * The approximation parameters are set to reasonable default values.
     * {@link AnswerType#NODATA_OBJECTS} will be returned in the result.
     * @param queryObject query object
     * @param k number of objects to be returned
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects"})
    public ApproxKNNQueryOperation(LocalAbstractObject queryObject, int k) {
        this(queryObject, k, AnswerType.NODATA_OBJECTS);
    }

    /**
     * Creates a new instance of ApproxkNNQueryOperation for a given query object and maximal number of objects to return.
     * The approximation parameters are set to reasonable default values.
     * @param queryObject the object to which the nearest neighbors are searched
     * @param k the number of nearest neighbors to retrieve
     * @param answerType the type of objects this operation stores in its answer
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects", "Answer type"})
    public ApproxKNNQueryOperation(LocalAbstractObject queryObject, int k, AnswerType answerType) {
        this(queryObject, k, answerType, 0, LocalSearchType.USE_STRUCTURE_DEFAULT, LocalAbstractObject.UNKNOWN_DISTANCE);
    }

    /**
     * Creates a new instance of ApproxKNNQueryOperation for a given query object,
     * maximal number of objects to return and parameters that control the approximation.
     * @param queryObject query object
     * @param k number of objects to be returned
     * @param localSearchParam local search parameter - typically approximation parameter
     * @param localSearchType type of the local search parameter
     * @param radiusGuaranteed radius within which the answer is required to be guaranteed as correct
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects", "Local search param", "Type of <br/>local search param", "guaranteed radius <br/>(-1 to switch off)"})
    public ApproxKNNQueryOperation(LocalAbstractObject queryObject, int k, int localSearchParam, LocalSearchType localSearchType, float radiusGuaranteed) {
        super(queryObject, k);
        this.localSearchParam = localSearchParam;
        this.localSearchType = localSearchType;
        this.radiusGuaranteed = radiusGuaranteed;
    }

    /**
     * Creates a new instance of ApproxKNNQueryOperation for a given query object,
     * maximal number of objects to return and parameters that control the approximation.
     * @param queryObject query object
     * @param k number of objects to be returned
     * @param answerType the type of objects this operation stores in its answer
     * @param localSearchParam local search parameter - typically approximation parameter
     * @param localSearchType type of the local search parameter
     * @param radiusGuaranteed radius within which the answer is required to be guaranteed as correct
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects", "Answer type", "Local search param", "Type of <br/>local search param", "guaranteed radius <br/>(-1 to switch off)"})
    public ApproxKNNQueryOperation(LocalAbstractObject queryObject, int k, AnswerType answerType, int localSearchParam, LocalSearchType localSearchType, float radiusGuaranteed) {
        super(queryObject, k, answerType);
        this.localSearchParam = localSearchParam;
        this.localSearchType = localSearchType;
        this.radiusGuaranteed = radiusGuaranteed;
    }

    /**
     * Creates a new instance of ApproxKNNQueryOperation for a given query object,
     * maximal number of objects to return and parameters that control the approximation.
     * @param queryObject query object
     * @param k number of objects to be returned
     * @param storeMetaDistances if <tt>true</tt>, all processed {@link messif.objects.MetaObject meta objects} will
     *          store their {@link messif.objects.util.RankedAbstractMetaObject sub-distances} in the answer
     * @param answerType the type of objects this operation stores in its answer
     * @param localSearchParam local search parameter - typically approximation parameter
     * @param localSearchType type of the local search parameter
     * @param radiusGuaranteed radius within which the answer is required to be guaranteed as correct
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects", "Store the meta-object subdistances?", "Answer type", "Local search param", "Type of <br/>local search param", "guaranteed radius <br/>(-1 to switch off)"})
    public ApproxKNNQueryOperation(LocalAbstractObject queryObject, int k, boolean storeMetaDistances, AnswerType answerType, int localSearchParam, LocalSearchType localSearchType, float radiusGuaranteed) {
        super(queryObject, k, storeMetaDistances, answerType);
        this.localSearchParam = localSearchParam;
        this.localSearchType = localSearchType;
        this.radiusGuaranteed = radiusGuaranteed;
    }

    /**
     * Creates a new instance of ApproxKNNQueryOperation for a given query object,
     * maximal number of objects to return and parameters that control the approximation.
     * @param queryObject query object
     * @param k number of objects to be returned
     * @param storeMetaDistances if <tt>true</tt>, all processed {@link messif.objects.MetaObject meta objects} will
     *          store their {@link messif.objects.util.RankedAbstractMetaObject sub-distances} in the answer
     * @param answerType the type of objects this operation stores in its answer
     * @param localSearchParam local search parameter - typically approximation parameter
     * @param localSearchType type of the local search parameter
     * @param radiusGuaranteed radius within which the answer is required to be guaranteed as correct
     * @param answerCollection collection to be used as answer (it must be empty, otherwise it will be cleared)
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects", "Store the meta-object subdistances?", "Answer type", "Local search param", "Type of <br/>local search param", "guaranteed radius <br/>(-1 to switch off)", "Answer collection"})
    public ApproxKNNQueryOperation(LocalAbstractObject queryObject, int k, boolean storeMetaDistances, AnswerType answerType, int localSearchParam, LocalSearchType localSearchType, float radiusGuaranteed, RankedSortedCollection answerCollection) {
        super(queryObject, k, storeMetaDistances, answerType, answerCollection);
        this.localSearchParam = localSearchParam;
        this.localSearchType = localSearchType;
        this.radiusGuaranteed = radiusGuaranteed;
    }


    //****************** Attribute access ******************//

    @Override
    public LocalSearchType getLocalSearchType() {
        return localSearchType;
    }

    @Override
    public void setLocalSearchParam(int localSearchParam) {
        this.localSearchParam = localSearchParam;
    }

    @Override
    public void setLocalSearchType(LocalSearchType localSearchType) {
        this.localSearchType = localSearchType;
    }

    @Override
    public int getLocalSearchParam() {
        return localSearchParam;
    }

    @Override
    public void setRadiusGuaranteed(float radiusGuaranteed) {
        this.radiusGuaranteed = radiusGuaranteed;
    }

    @Override
    public float getRadiusGuaranteed() {
        return radiusGuaranteed;
    }


    //****************** Answer updating overrides ******************//

    /**
     * Update query answer data of this operation from another query operation.
     * Additionally, if the <code>operation</code> is approximate kNN query, the
     * radius guaranteed is also updated.
     * @param operation the operation to update answer from
     */
    @Override
    protected void updateFrom(RankingQueryOperation operation) {
        super.updateFrom(operation);
        if (operation instanceof ApproxKNNQueryOperation)
            updateFrom((ApproxKNNQueryOperation)operation);
    }

    /**
     * Updates the guaranteed radius from another approximate kNN query.
     * That is, if the guaranteed radius of the other query is smaller,
     * this query's one is reduced.
     * @param operation the operation to update answer from
     */
    protected void updateFrom(ApproxKNNQueryOperation operation) {
        if (radiusGuaranteed > operation.radiusGuaranteed)
            radiusGuaranteed = operation.radiusGuaranteed;
    }

    /**
     * Returns the information about this operation.
     * @return the information about this operation
     */
    @Override
    public String toString() {
        return new StringBuffer(super.toString()).
                append("; local search param: ").append(localSearchParam).append(" of type: ").append(localSearchType.toString()).toString();
    }
    
}
