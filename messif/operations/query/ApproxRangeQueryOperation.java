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

/**
 * Approximate range query with specific early termination parameters
 * and support for obtaining some guarantees on results.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("Approximate range query")
public class ApproxRangeQueryOperation extends RangeQueryOperation implements Approximate {    

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /** Type of the local approximation parameter used. */
    protected LocalSearchType localSearchType;

    /**
     * Value of the local approximation parameter. 
     * Its interpretation depends on the value of {@link #localSearchType}.
     */
    protected int localSearchParam;

    /**
     * Radius for which the answer is guaranteed as correct.
     * It is specified in the constructor and can influence the level of approximation.
     * An algorithm evaluating this query can also change this value, so it can
     * notify about the guarantees of evaluation.
     */
    protected float radiusGuaranteed;

    /**
     * Creates a new instance of ApproxRangeQueryOperation for a given query object and maximal number of objects to return.
     * The approximation parameters are set to reasonable default values.
     * {@link AnswerType#NODATA_OBJECTS} will be returned in the result.
     * @param queryObject query object
     * @param r query radius
     */
    @AbstractOperation.OperationConstructor({"Query object", "Query radius"})
    public ApproxRangeQueryOperation(LocalAbstractObject queryObject, float r) {
        this(queryObject, r, AnswerType.NODATA_OBJECTS);
    }

    /**
     * Creates a new instance of ApproxRangeQueryOperation for a given query object and radius.
     * The approximation parameters are set to reasonable default values.
     * @param queryObject the object to which the nearest neighbors are searched
     * @param r query radius
     * @param answerType the type of objects this operation stores in its answer
     */
    @AbstractOperation.OperationConstructor({"Query object", "Query radius", "Answer type"})
    public ApproxRangeQueryOperation(LocalAbstractObject queryObject, float r, AnswerType answerType) {
        this(queryObject, r, answerType, 25, LocalSearchType.PERCENTAGE, LocalAbstractObject.UNKNOWN_DISTANCE);
    }

    /**
     * Creates a new instance of ApproxRangeQueryOperation for a given query object,
     * radius and parameters that control the approximation.
     * {@link AnswerType#NODATA_OBJECTS} will be returned in the result.
     * @param queryObject query object
     * @param r query radius
     * @param localSearchParam local search parameter - typically approximation parameter
     * @param localSearchType type of the local search parameter
     * @param radiusGuaranteed radius within which the answer is required to be guaranteed as correct
     */
    @AbstractOperation.OperationConstructor({"Query object", "Query radius", "Local search param", "Type of <br/>local search param", "guaranteed radius <br/>(-1 to switch off)"})
    public ApproxRangeQueryOperation(LocalAbstractObject queryObject, float r, int localSearchParam, LocalSearchType localSearchType, float radiusGuaranteed) {
        super(queryObject, r);
        this.localSearchParam = localSearchParam;
        this.localSearchType = localSearchType;
        this.radiusGuaranteed = radiusGuaranteed;
    }
 
    /**
     * Creates a new instance of ApproxRangeQueryOperation for a given query object,
     * radius and parameters that control the approximation.
     * @param queryObject query object
     * @param r query radius
     * @param answerType the type of objects this operation stores in its answer
     * @param localSearchParam local search parameter - typically approximation parameter
     * @param localSearchType type of the local search parameter
     * @param radiusGuaranteed radius within which the answer is required to be guaranteed as correct
     */
    @AbstractOperation.OperationConstructor({"Query object", "Query radius", "Answer type", "Local search param", "Type of <br/>local search param", "guaranteed radius <br/>(-1 to switch off)"})
    public ApproxRangeQueryOperation(LocalAbstractObject queryObject, float r, AnswerType answerType, int localSearchParam, LocalSearchType localSearchType, float radiusGuaranteed) {
        super(queryObject, r, answerType);
        this.localSearchParam = localSearchParam;
        this.localSearchType = localSearchType;
        this.radiusGuaranteed = radiusGuaranteed;
    }

    /**
     * Creates a new instance of ApproxRangeQueryOperation for a given query object,
     * radius and parameters that control the approximation.
     * @param queryObject query object
     * @param r query radius
     * @param maxAnswerSize sets the maximal answer size
     * @param answerType the type of objects this operation stores in its answer
     * @param localSearchParam local search parameter - typically approximation parameter
     * @param localSearchType type of the local search parameter
     * @param radiusGuaranteed radius within which the answer is required to be guaranteed as correct
     */
    @AbstractOperation.OperationConstructor({"Query object", "Query radius", "Maximal answer size", "Answer type", "Local search param", "Type of <br/>local search param", "guaranteed radius <br/>(-1 to switch off)"})
    public ApproxRangeQueryOperation(LocalAbstractObject queryObject, float r, int maxAnswerSize, AnswerType answerType, int localSearchParam, LocalSearchType localSearchType, float radiusGuaranteed) {
        super(queryObject, r, answerType, maxAnswerSize);
        this.localSearchParam = localSearchParam;
        this.localSearchType = localSearchType;
        this.radiusGuaranteed = radiusGuaranteed;
    }

    /**
     * Creates a new instance of ApproxRangeQueryOperation for a given query object,
     * radius and parameters that control the approximation.
     * @param queryObject query object
     * @param r query radius
     * @param maxAnswerSize sets the maximal answer size
     * @param answerType the type of objects this operation stores in its answer
     * @param localSearchParam local search parameter - typically approximation parameter
     * @param localSearchType type of the local search parameter
     * @param radiusGuaranteed radius within which the answer is required to be guaranteed as correct
     * @param answerCollection collection to be used as answer (it must be empty, otherwise it will be cleared)
     */
    @AbstractOperation.OperationConstructor({"Query object", "Query radius", "Maximal answer size", "Answer type", "Local search param", "Type of <br/>local search param", "guaranteed radius <br/>(-1 to switch off)", "Answer collection"})
    public ApproxRangeQueryOperation(LocalAbstractObject queryObject, float r, int maxAnswerSize, AnswerType answerType, int localSearchParam, LocalSearchType localSearchType, float radiusGuaranteed, RankedSortedCollection answerCollection) {
        super(queryObject, r, answerType, maxAnswerSize, true, answerCollection);
        this.localSearchParam = localSearchParam;
        this.localSearchType = localSearchType;
        this.radiusGuaranteed = radiusGuaranteed;
    }

    @Override
    public LocalSearchType getLocalSearchType() {
        return localSearchType;
    }

    @Override
    public int getLocalSearchParam() {
        return localSearchParam;
    }

    @Override
    public void setLocalSearchType(LocalSearchType localSearchType) {
        this.localSearchType = localSearchType;
    }

    @Override
    public void setLocalSearchParam(int localSearchParam) {
        this.localSearchParam = localSearchParam;
    }

    @Override
    public float getRadiusGuaranteed() {
        return radiusGuaranteed;
    }

    @Override
    public void setRadiusGuaranteed(float radiusGuaranteed) {
        this.radiusGuaranteed = radiusGuaranteed;
    }

    /**
     * Returns a string representation of this operation.
     * @return a string representation of this operation.
     */
    @Override
    public String toString() {
        return new StringBuffer("Approximate Range query <").
                append(getQueryObject()).append(',').
                append(radius).append(',').
                append(radiusGuaranteed).append("> returned ").
                append(getAnswerCount()).append(" objects").
                toString();
    }

}
