
package messif.operations;

import messif.objects.LocalAbstractObject;

/**
 * Approximative kNN query for presentations. It sets the parameters to use 20 % of the cluster and 25%-approx local search.
 * 
 * @author David Novak, FI Masaryk University, Brno, Czech Republic; <a href="mailto:xnovak8@fi.muni.cz">xnovak8@fi.muni.cz</a>
 */
@AbstractOperation.OperationName("Approximative kNN query for presentations")
public class ApproxKNNQueryOperationPresentable extends ApproxKNNQueryOperationParams {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    /** Creates a new instance of ApproxKNNQueryOperationPresentable
     * @param queryObject query object
     * @param k number of nearest objects to be retured
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects"})
    public ApproxKNNQueryOperationPresentable(LocalAbstractObject queryObject, int k) {
        super(queryObject, Math.min(100, k), 0, 20, null, 25, ApproxKNNQueryOperation.LocalSearchType.PERCENTAGE, LocalAbstractObject.UNKNOWN_DISTANCE);
        this.ceilingClustersToVisit = 4;
        this.ceilingNodesToVisit = 7;
    }
    
    /**
     * Returns argument that was passed while constructing instance.
     * If the argument is not stored within operation, <tt>null</tt> is returned.
     * @param index index of an argument passed to constructor
     * @return argument that was passed while constructing instance
     * @throws IndexOutOfBoundsException if index parameter is out of range
     */
    @Override
    public Object getArgument(int index) throws IndexOutOfBoundsException {
        switch (index) {
        case 0:
            return queryObject;
        case 1:
            return k;
        default:
            throw new IndexOutOfBoundsException("ApproxKNNQueryOperationPresentable has only two arguments");
        }
    }
    
    /**
     * Returns number of arguments that were passed while constructing this instance.
     * @return number of arguments that were passed while constructing this instance
     */
    @Override
    public int getArgumentCount() {
        return 2;
    }    
    

}
