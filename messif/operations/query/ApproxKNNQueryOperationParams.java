package messif.operations.query;

import messif.objects.LocalAbstractObject;
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;

/**
 * Parametrized approximate k-nearest neighbors query.
 * This query extends the standard approximate k-nearest neighbors query
 * with settable approximation parameters. The parameters are tailored
 * for the M-Chord and M-Tree indexing structures, but can be used by
 * other techniques as well.
 * 
 * @author David Novak, FI Masaryk University, Brno, Czech Republic; <a href="mailto:xnovak8@fi.muni.cz">xnovak8@fi.muni.cz</a>
 */
@AbstractOperation.OperationName("Parametrized Approximate kNN Query")
public class ApproxKNNQueryOperationParams extends ApproxKNNQueryOperation {

    /** Class serial id for serialization. */
    private static final long serialVersionUID = 4L;
    
    /** Number of clusters to visit */
    public int clustersToVisit;

    /** Percentage of nodes to visit in each cluster */
    public int nodesToVisitPercentage;
    
    /** Number of nodes to visit in each cluster. */
    public String nodesToVisit = null;
    
    /** Internal ceiling of the number of clusters to visit.  */
    public int ceilingClustersToVisit = Integer.MAX_VALUE;

    /** Internal ceiling of the number of nodes to visit in a cluster.  */
    public int ceilingNodesToVisit = Integer.MAX_VALUE;

    /**
     * Creates a new instance of ApproxKNNQueryOperationParams.
     * The parameters are set to reasonable default values.
     * {@link AnswerType#REMOTE_OBJECTS} will be returned in the result.
     * @param queryObject query object
     * @param k number of objects to be retured
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects"})
    public ApproxKNNQueryOperationParams(LocalAbstractObject queryObject, int k) {
        this(queryObject, k, AnswerType.REMOTE_OBJECTS);
    }
    
    /**
     * Creates a new instance of ApproxKNNQueryOperationParams.
     * The approximation parameters are set to reasonable default values.
     * @param queryObject the object to which the nearest neighbors are searched
     * @param k the number of nearest neighbors to retrieve
     * @param answerType the type of objects this operation stores in its answer
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects", "Answer type"})
    public ApproxKNNQueryOperationParams(LocalAbstractObject queryObject, int k, AnswerType answerType) {
        this(queryObject, k, answerType, 0, 20, null, 25, ApproxKNNQueryOperation.LocalSearchType.PERCENTAGE, LocalAbstractObject.UNKNOWN_DISTANCE);
        this.ceilingClustersToVisit = 4;
        this.ceilingNodesToVisit = 7;
    }

    /**
     * Creates a new instance of ApproxKNNQueryOperationParams.
     * {@link AnswerType#REMOTE_OBJECTS} will be returned in the result.
     * @param queryObject query object
     * @param k number of objects to be returned
     * @param clustersToVisit number of clusters to be visited by this approx. query; if is equal to 0, then determined adaptively
     * @param nodesToVisitPercentage percentage of individual clusters to be visited by the query
     * @param nodesToVisit this is a output string to determine particular clusters visited and nodes within the cluster
     * @param localSearchParam local search parameter - typically approximation parameter
     * @param localSearchType type of the local search parameter
     * @param radiusGuaranteed radius for which the answer is guaranteed
     */
    @AbstractOperation.OperationConstructor({"Query object", "# of nearest objects", "Number of clusters<br/>to visit", "% of nodes to<br/>visit in each cluster",  "Nodes visited in particular <br/> clusters: [cluster: number]", "Local search param", "Type of <br/>local search param", "The minimal radius of guaranteed answer (-1 to switch off)"})
    public ApproxKNNQueryOperationParams(LocalAbstractObject queryObject, int k, int clustersToVisit, int nodesToVisitPercentage, String nodesToVisit, int localSearchParam, LocalSearchType localSearchType, float radiusGuaranteed) {
        super(queryObject, k, localSearchParam, localSearchType, radiusGuaranteed);
        this.clustersToVisit = clustersToVisit;
        this.nodesToVisitPercentage = nodesToVisitPercentage;
        this.nodesToVisit = nodesToVisit;
    }

    /**
     * Creates a new instance of ApproxKNNQueryOperationParams.
     * @param queryObject query object
     * @param k number of objects to be returned
     * @param answerType the type of objects this operation stores in its answer
     * @param clustersToVisit number of clusters to be visited by this approx. query; if is equal to 0, then determined adaptively
     * @param nodesToVisitPercentage percentage of individual clusters to be visited by the query
     * @param nodesToVisit this is a output string to determine particular clusters visited and nodes within the cluster
     * @param localSearchParam local search parameter - typically approximation parameter
     * @param localSearchType type of the local search parameter
     * @param radiusGuaranteed radius for which the answer is guaranteed
     */
    @AbstractOperation.OperationConstructor({"Query object", "# of nearest objects", "Answer type", "Number of clusters<br/>to visit", "% of nodes to<br/>visit in each cluster",  "Nodes visited in particular <br/> clusters: [cluster: number]", "Local search param", "Type of <br/>local search param", "The minimal radius of guaranteed answer (-1 to switch off)"})
    public ApproxKNNQueryOperationParams(LocalAbstractObject queryObject, int k, AnswerType answerType, int clustersToVisit, int nodesToVisitPercentage, String nodesToVisit, int localSearchParam, LocalSearchType localSearchType, float radiusGuaranteed) {
        super(queryObject, k, answerType, localSearchParam, localSearchType, radiusGuaranteed);
        this.clustersToVisit = clustersToVisit;
        this.nodesToVisitPercentage = nodesToVisitPercentage;
        this.nodesToVisit = nodesToVisit;
    }

    /** 
     * Merge information about the visited nodes to an info string.
     * @param operation the operation to update answer from
     */
    @Override
    protected void updateFrom(ApproxKNNQueryOperation operation) {
        super.updateFrom(operation);
        if (operation instanceof ApproxKNNQueryOperationParams) { // FIXME: This is an ugly hack...
            String otherNodesToVisit = ((ApproxKNNQueryOperationParams) operation).nodesToVisit;
            if (otherNodesToVisit != null) {
                if (nodesToVisit == null)
                    nodesToVisit = otherNodesToVisit;
                else
                    nodesToVisit += "; " + otherNodesToVisit;
            }
        }
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
        case 2:
            return clustersToVisit;
        case 3:
            return nodesToVisitPercentage;
        case 4:
            return nodesToVisit;
        case 5:
            return localSearchParam;
        case 6:
            return localSearchType;
        case 7:
            return radiusGuaranteed;
        default:
            throw new IndexOutOfBoundsException("ApproxKNNQueryOperationParams has only six arguments");
        }
    }

    /**
     * Returns number of arguments that were passed while constructing this instance.
     * @return number of arguments that were passed while constructing this instance
     */
    @Override
    public int getArgumentCount() {
        return 8;
    }    
    
    /**
     * Returns the information about this operation.
     * @return the information about this operation
     */
    @Override
    public String toString() {
        return new StringBuffer(super.toString()).
                append("\nClusters: ").append(clustersToVisit).
                append("; nodes in cluster: ").append(nodesToVisitPercentage).
                append("%; local search param: ").append(localSearchParam).
                append("%; guaranteed radius: ").append(radiusGuaranteed).
                toString();
    }


    //****************** Equality driven by operation data ******************//

    /** 
     * Indicates whether some other operation has the same data as this one.
     * @param   obj   the reference object with which to compare.
     * @return  <code>true</code> if this object has the same data as the obj
     *          argument; <code>false</code> otherwise.
     */
    @Override
    protected boolean dataEqualsImpl(AbstractOperation obj) {
        if (!(obj instanceof ApproxKNNQueryOperationParams) || !super.dataEqualsImpl(obj))
            return false;

        ApproxKNNQueryOperationParams castObj = (ApproxKNNQueryOperationParams)obj;

        if ((clustersToVisit != castObj.clustersToVisit) || (nodesToVisitPercentage != castObj.nodesToVisitPercentage) || (localSearchParam != castObj.localSearchParam)
                || (localSearchType != castObj.localSearchType) || (radiusGuaranteed != castObj.radiusGuaranteed))
            return false;
        if (nodesToVisit == null)
            return castObj.nodesToVisit == null;
        return nodesToVisit.equals(castObj.nodesToVisit);
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return super.dataHashCode() << 8 + clustersToVisit + nodesToVisitPercentage + localSearchParam + (int)radiusGuaranteed;
    }

}
