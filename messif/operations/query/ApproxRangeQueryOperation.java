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
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;

/**
 * Approximate range query with specific early termination parameters
 * and support for obtaining some guarantees on results.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("Approximate range query")
public class ApproxRangeQueryOperation extends RangeQueryOperation {    

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /** Type of the local approximation parameter: PERCENTAGE, ABS_OBJ_COUNT, ABS_DC_COUNT.
     * It can be view as a type of stop condition for early termination strategy of approximation.
     */
    public static enum LocalSearchType {
        /** Stop after inspecting given percentage of data.
         * {@link #localSearchParam} holds the value between 0-100.
         */
        PERCENTAGE,
        /** Stop after inspecting the specific number of objects.
         * {@link #localSearchParam} is the number of objects.
         */
        ABS_OBJ_COUNT,
        /** Stop after the specific number of evaluations of distance functions.
         * {@link #localSearchParam} is the threshold on the number of distance computations.
         */
        ABS_DC_COUNT
    }

    /** Type of the local approximation parameter used. */
    protected final LocalSearchType localSearchType;

    /**
     * Value of the local approximation parameter. 
     * Its interpretation depends on the value of {@link #localSearchType}.
     */
    protected final int localSearchParam;

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
     * Returns currently set type of approximation, see {@link #localSearchType}.
     * 
     * @return current type of approximation
     */
    public LocalSearchType getLocalSearchType() {
        return localSearchType;
    }

    /**
     * Returns the currently set value of approximation threshold. The interpretation
     * of this value depends on the currently set type {@link #localSearchType}.
     * 
     * @return threshold value set
     */
    public int getLocalSearchParam() {
        return localSearchParam;
    }

    /**
     * Set a different value of radius within which the results are guaranteed as correct.
     * An evaluation algorithm is completely responsible for setting the correct value.
     * 
     * @param radiusGuaranteed new value of radius
     */
    public void setRadiusGuaranteed(float radiusGuaranteed) {
        this.radiusGuaranteed = radiusGuaranteed;
    }

    /**
     * Returns a currently set value of radius within which the results are guaranteed as correct.
     * An evaluation algorithm is completely responsible for setting the correct value.
     * 
     * @return value of radius
     */
    public float getRadiusGuaranteed() {
        return radiusGuaranteed;
    }

    /**
     * Returns a string representation of this operation.
     * @return a string representation of this operation.
     */
    @Override
    public String toString() {
        return new StringBuffer("Approximate Range query <").
                append(queryObject).append(',').
                append(radius).append(',').
                append(radiusGuaranteed).append("> returned ").
                append(getAnswerCount()).append(" objects").
                toString();
    }

}