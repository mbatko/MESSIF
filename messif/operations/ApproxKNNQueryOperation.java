/*
 * ApproxKNNQueryOperation.java
 *
 * Created on 21. cerven 2007, 16:52
 *
 */

package messif.operations;

import messif.objects.LocalAbstractObject;

/**
 * Approximate k-nearest neighbors query with specific early termination parameters
 * and support for obtaining some guarantees on results.
 *
 * @author xbatko
 */
@AbstractOperation.OperationName("Approximate k-nearest neighbors query")
public class ApproxKNNQueryOperation extends kNNQueryOperation {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 3L;

    //****************** Attributes ******************//

    /**
     * Enumeration of types of the stop condition for approximation's early termination strategy.
     */
    public static enum LocalSearchType {
        /**
         * Stop after inspecting given percentage of data.
         * {@link #localSearchParam} holds the value between 0-100.
         */
        PERCENTAGE,
        /**
         * Stop after inspecting the specific number of objects.
         * {@link #localSearchParam} is the number of objects.
         */
        ABS_OBJ_COUNT,
        /**
         * Stop after the specific number of evaluations of distance functions.
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

    /** Radius for which the answer is guaranteed as correct.
     * It is specified in the constructor and can influence the level of approximation.
     * An algorithm evaluating this query can also change this value, so it can
     * notify about the guarantees of evaluation.
     */
    protected float radiusGuaranteed;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of ApproxKNNQueryOperation.
     * The parameters are set to reasonable default values.
     * @param queryObject query object
     * @param k number of objects to be returned
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects"})
    public ApproxKNNQueryOperation(LocalAbstractObject queryObject, int k) {
        this(queryObject, k, 25, ApproxKNNQueryOperation.LocalSearchType.PERCENTAGE, LocalAbstractObject.UNKNOWN_DISTANCE);
    }

    /**
     * Creates a new instance of ApproxKNNQueryOperation
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


    //****************** Attribute access ******************//

    /**
     * Returns the {@link LocalSearchType type of the local approximation} parameter used.
     * @return the {@link LocalSearchType type of the local approximation} parameter used
     */
    public LocalSearchType getLocalSearchType() {
        return localSearchType;
    }

    /**
     * Returns the value of the local approximation parameter.
     * Its interpretation depends on the value of {@link #getLocalSearchType() local search type}.
     * @return the value of the local approximation parameter
     */
    public int getLocalSearchParam() {
        return localSearchParam;
    }

    /**
     * Set a different value of radius within which the results are guaranteed as correct.
     * An evaluation algorithm is completely responsible for setting the correct value.
     * @param radiusGuaranteed new guaranteed radius value
     */
    public void setRadiusGuaranteed(float radiusGuaranteed) {
        this.radiusGuaranteed = radiusGuaranteed;
    }

    /**
     * Returns a currently set value of radius within which the results are guaranteed as correct.
     * An evaluation algorithm is completely responsible for setting the correct value.
     * @return the value of the currently guaranteed radius
     */
    public float getRadiusGuaranteed() {
        return radiusGuaranteed;
    }


    //****************** Answer updating overrides ******************//

    /**
     * Update query answer data of this operation from another query operation.
     * The error code of this operation is updated using {@link #updateErrorCode}.
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

}
