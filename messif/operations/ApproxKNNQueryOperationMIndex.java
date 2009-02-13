
package messif.operations;

import messif.objects.LocalAbstractObject;

/**
 * Approximate k-nearest neighbors query parametrized for the Metric Index with M-Tree in individual peers.
 * This query extends the standard approximate k-nearest neighbors query with settable approximation parameters. 
 * 
 * @author David Novak, FI Masaryk University, Brno, Czech Republic; <a href="mailto:david.novak@fi.muni.cz">david.novak@fi.muni.cz</a>
 */
@AbstractOperation.OperationName("Approximate kNN Query parametrized for Metric Index")
public class ApproxKNNQueryOperationMIndex extends ApproxKNNQueryOperation {

    /** Class serial id for serialization. */
    private static final long serialVersionUID = 20203L;
        
    /** If greater than 0 then taken as the fixed number of clusters to be visited by the operation. */
    protected int initialLevelClusterNumber;

    /** M-Index level to be used for generating the initial set of clusters to be visited. */
    protected int initialLevel;

    /** minClustersForBranching minimal fixed number of sub-clusters to be visited within a cluster (internal node) */
    protected int minClustersForBranching;

    /** maxClustersForBranching maximal fixed number of sub-clusters to be visited within a cluster (internal node) */
    protected int maxClustersForBranching;

    /** limitBranchingByPenalty if this flag is true then each super-cluster brings a penalty from the upper level and limits the sub-clusters penalties by this value */
    protected boolean limitBranchingByPenalty;

    /** Maximal number of peers to visit within the best leaf-node cluster */
    protected int maxPeersForBestCluster;

    /** Maximal number of peers to visit within (other than best) leaf-node cluster */
    protected int maxPeersForClusters;

    // ************************************ Getters and setters  ******************************* //

    public int getInitialLevel() {
        return initialLevel;
    }

    public int getInitialLevelClusterNumber() {
        return initialLevelClusterNumber;
    }

    public boolean isLimitBranchingByPenalty() {
        return limitBranchingByPenalty;
    }

    public int getMaxClustersForBranching() {
        return maxClustersForBranching;
    }

    public int getMinClustersForBranching() {
        return minClustersForBranching;
    }

    public int getMaxPeersForBestCluster() {
        return maxPeersForBestCluster;
    }

    public int getMaxPeersForClusters() {
        return maxPeersForClusters;
    }

    /**
     * Creates a new instance of ApproxKNNQueryOperationMIndex with default parameters.
     * The approximation parameters are set to reasonable default values.
     * {@link AnswerType#REMOTE_OBJECTS} will be returned in the result.
     * @param queryObject query object
     * @param k number of objects to be returned
     */
    @AbstractOperation.OperationConstructor({"Query object", "# of nearest objects"})
    public ApproxKNNQueryOperationMIndex(LocalAbstractObject queryObject, int k) {
        this(queryObject, k, AnswerType.REMOTE_OBJECTS);
    }
    
    /**
     * Creates a new instance of ApproxKNNQueryOperationMIndex with default parameters.
     * The approximation parameters are set to reasonable default values.
     * @param queryObject query object
     * @param k number of objects to be returned
     * @param answerType the type of objects this operation stores in its answer
     */
    @AbstractOperation.OperationConstructor({"Query object", "# of nearest objects", "Answer type"})
    public ApproxKNNQueryOperationMIndex(LocalAbstractObject queryObject, int k, AnswerType answerType) {
        this(queryObject, k, 5000, ApproxKNNQueryOperation.LocalSearchType.ABS_OBJ_COUNT);
    }

    /**
     * Creates a new instance of ApproxKNNQueryOperationMIndex with default parameters for distributed processing
     *  and specify parameters for centralized approximation.
     * @param queryObject query object
     * @param k number of objects to be returned
     * @param localSearchParam local search parameter - typically approximation parameter
     * @param localSearchType type of the local search parameter
     */
    @AbstractOperation.OperationConstructor({"Query object", "# of nearest objects",  "Local search param", "Type of <br/>local search param"})
    public ApproxKNNQueryOperationMIndex(LocalAbstractObject queryObject, int k, int localSearchParam, LocalSearchType localSearchType) {
        this(queryObject, k, 6, 3, true, 1, 2, 1, 1, localSearchParam, localSearchType);
    }

    /**
     * Creates a new instance of ApproxKNNQueryOperationMIndex with default parameters for distributed processing
     *  and specify parameters for centralized approximation.
     * @param queryObject query object
     * @param k number of objects to be returned
     * @param initialLevelClusterNumber if greater than 0 then taken as the fixed number of clusters to be visited by the operation
     * @param initialLevel M-Index level to be used for generating the initial set of clusters to be visited
     * @param limitBranchingByPenalty if this flag is true then each super-cluster brings a penalty from the upper level and limits the sub-clusters penalties by this value
     * @param minClustersForBranching minimal fixed number of sub-clusters to be visited within a cluster (internal node)
     * @param maxClustersForBranching maximal fixed number of sub-clusters to be visited within a cluster (internal node)
     * @param maxPeersForBestCluster Maximal number of peers to visit within the best leaf-node cluster
     * @param maxPeersForClusters Maximal number of peers to visit within (other than best) leaf-node cluster
     * @param localSearchParam local search parameter - typically approximation parameter
     * @param localSearchType type of the local search parameter
     */
    @AbstractOperation.OperationConstructor({"Query object", "# of nearest objects",  "Local search param", "Type of <br/>local search param"})
    public ApproxKNNQueryOperationMIndex(LocalAbstractObject queryObject, int k, int initialLevelClusterNumber, int initialLevel, boolean limitBranchingByPenalty, int minClustersForBranching, int maxClustersForBranching, int maxPeersForBestCluster, int maxPeersForClusters, int localSearchParam, LocalSearchType localSearchType) {
        this(queryObject, k, initialLevelClusterNumber, initialLevel, limitBranchingByPenalty, minClustersForBranching, maxClustersForBranching, maxPeersForBestCluster, maxPeersForClusters, localSearchParam, localSearchType, AnswerType.REMOTE_OBJECTS, LocalAbstractObject.UNKNOWN_DISTANCE);
    }
    
    /**
     * Creates a new instance of ApproxKNNQueryOperationMIndex with full parameters.
     * {@link AnswerType#REMOTE_OBJECTS} will be returned in the result.
     * @param queryObject query object
     * @param k number of objects to be returned
     * @param answerType the type of objects this operation stores in its answer
     * @param initialLevelClusterNumber if greater than 0 then taken as the fixed number of clusters to be visited by the operation
     * @param initialLevel M-Index level to be used for generating the initial set of clusters to be visited
     * @param limitBranchingByPenalty if this flag is true then each super-cluster brings a penalty from the upper level and limits the sub-clusters penalties by this value
     * @param minClustersForBranching minimal fixed number of sub-clusters to be visited within a cluster (internal node)
     * @param maxPeersForBestCluster Maximal number of peers to visit within the best leaf-node cluster
     * @param maxPeersForClusters Maximal number of peers to visit within (other than best) leaf-node cluster 
     * @param maxClustersForBranching maximal fixed number of sub-clusters to be visited within a cluster (internal node)
     * @param localSearchParam local search parameter - typically approximation parameter
     * @param localSearchType type of the local search parameter
     * @param radiusGuaranteed radius for which the answer is guaranteed
     */
    @AbstractOperation.OperationConstructor({"Query object", "# of objects", "Fixed # clusters<br/>to visit", "M-Index level to <br/>generate cluster <br/>variants for", "Local search param", "Type of <br/>local search param", "guaranteed radius <br/>(-1 to switch off)"})
    public ApproxKNNQueryOperationMIndex(LocalAbstractObject queryObject, int k, int initialLevelClusterNumber, int initialLevel, boolean limitBranchingByPenalty, int minClustersForBranching, int maxClustersForBranching, int maxPeersForBestCluster, int maxPeersForClusters, int localSearchParam, LocalSearchType localSearchType, AnswerType answerType, float radiusGuaranteed) {
        super(queryObject, k, answerType, localSearchParam, localSearchType, radiusGuaranteed);
        this.initialLevelClusterNumber = initialLevelClusterNumber;
        this.initialLevel = initialLevel;
        this.limitBranchingByPenalty = limitBranchingByPenalty;
        this.minClustersForBranching = minClustersForBranching;
        this.maxClustersForBranching = maxClustersForBranching;
        this.maxPeersForBestCluster = maxPeersForBestCluster;
        this.maxPeersForClusters = maxPeersForClusters;
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
                return initialLevelClusterNumber;
            case 3:
                return initialLevel;
            case 4:
                return minClustersForBranching;
            case 5:
                return maxClustersForBranching;
            case 6:
                return limitBranchingByPenalty;
            case 7:
                return localSearchParam;
            default:
                throw new IndexOutOfBoundsException("ApproxKNNQueryOperationMIndex has only " + getArgumentCount() +" arguments");
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
                append("\n initial clusters to visit: ").append(initialLevelClusterNumber).append(" at level: ").append(initialLevel).
                append("; [min,max] branching: [").append(minClustersForBranching).append(",").append(maxClustersForBranching).
                append("]; ").append((limitBranchingByPenalty ? "YES" : "NO")).append(" use the penalty limiting").
                append("; local search param: ").append(localSearchParam).append("; guaranteed radius: ").append(radiusGuaranteed).
                toString();
    }


    /****************** Equality driven by operation data ******************/

    /** 
     * Indicates whether some other operation has the same data as this one.
     * @param   obj   the reference object with which to compare.
     * @return  <code>true</code> if this object has the same data as the obj
     *          argument; <code>false</code> otherwise.
     */
    @Override
    protected boolean dataEqualsImpl(AbstractOperation obj) {
        if (!(obj instanceof ApproxKNNQueryOperationMIndex) || !super.dataEqualsImpl(obj))
            return false;

        ApproxKNNQueryOperationMIndex castObj = (ApproxKNNQueryOperationMIndex)obj;

        return ((initialLevelClusterNumber == castObj.initialLevelClusterNumber) && (initialLevel == castObj.initialLevel) && (localSearchParam == castObj.localSearchParam)
                && (localSearchType == castObj.localSearchType) && (radiusGuaranteed == castObj.radiusGuaranteed));
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return super.dataHashCode() << 8 + initialLevelClusterNumber + initialLevel + localSearchParam + (int)radiusGuaranteed;
    }

}
