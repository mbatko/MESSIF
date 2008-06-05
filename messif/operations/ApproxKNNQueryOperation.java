/*
 * ApproxKNNQueryOperation.java
 *
 * Created on 21. cerven 2007, 16:52
 *
 */

package messif.operations;

import messif.objects.LocalAbstractObject;

/**
 * Approximate k-nearest neighbors query.
 *
 * @author xbatko
 */
@AbstractOperation.OperationName("Approximate k-nearest neighbors query")
public class ApproxKNNQueryOperation extends kNNQueryOperation {
    
    /** Class serial id for serialization */
    private static final long serialVersionUID = 3L;
    
    /** Maximal percentage of local data to be explored */
    public int localSearchParam;
    
    /** Type of the local approx. parameter: PERCENTAGE, ABS_OBJ_COUNT, ABS_DC_COUNT. */
    public static enum LocalSearchType {
        PERCENTAGE,
        ABS_OBJ_COUNT,
        ABS_DC_COUNT
    }
    
    /** Local approx. parameter. */
    public LocalSearchType localSearchType;
    
    /** Radius for which the answer is guaranteed */
    public float radiusGuaranteed;
    
    
    /** Creates a new instance of ApproxKNNQueryOperation
     * @param queryObject query object
     * @param k number of objects to be returned
     * @param localSearchParam local search parameter - typically approximation parameter
     * @param localSearchType type of the local search parameter
     * @param radiusGuaranteed radius for which the answer is guaranteed
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects", "Local search param", "Type of <br/>local search param", "guaranteed radius <br/>(-1 to switch off)"})
    public ApproxKNNQueryOperation(LocalAbstractObject queryObject, int k, int localSearchParam, LocalSearchType localSearchType, float radiusGuaranteed) {
        super(queryObject, k);
        this.localSearchParam = localSearchParam;
        this.localSearchType = localSearchType;
        this.radiusGuaranteed = radiusGuaranteed;
    }
    
}
