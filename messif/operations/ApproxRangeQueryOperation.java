/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package messif.operations;

import messif.objects.LocalAbstractObject;

/**
 * Approximate range query with specific early termination parameters
 * and support for obtaining some guarantees on results.
 *
 * @author Vlastislav Dohnal, Masaryk University, dohnal@fi.muni.cz
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
    
    /** Value of the local approximation parameter. 
     * Its interpretation depends on the value of {@link #localSearchType}.
     */
    protected final int localSearchParam;
    
    /** Radius for which the answer is guaranteed as correct.
     * It is specified in the constructor and can influence the level of approximation.
     * An algorithm evaluating this query can also change this value, so it can
     * notify about the guarantees of evaluation.
     */
    protected float radiusGuaranteed;

    
    /** Creates a new instance of ApproxRangeQueryOperation
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
    
    
}
