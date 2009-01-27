
package messif.operations;

import java.util.Arrays;
import java.util.List;
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
    private static final long serialVersionUID = 20201L;
        
    /** If greater than 0 then taken as the fixed number of clusters to be visited by the operation. */
    public int clustersToVisit;

    /** 
     * If greater than 0 then the clusters are taken in the order by the "score" and it is searched for a "score gap"
     * greater than basicDifferenceConst. The basicDifferenceConst is reduced by dividing by
     * differenceDivisionConst in each step of the loop.
     */
    public float basicDifferenceConst;
    
    /** 
     * The basicDifferenceConst is reduced by dividing by differenceDivisionConst in each step of the loop.
     * {@link ApproxKNNQueryOperationMIndex#basicDifferenceConst}
     */
    public float differenceDivisionConst;

    /** The maximal score of a cluster to be included in the search. */
    public float maxClusterScore;
    
    /** Maximal number of clusters to be visited by this operation.  */
    public int maxClustersToVisit;
    
    /** 
     * An array of maximal numbers of nodes/peers to be visited within each cluster; the clusters are taken in the order 
     *  of the cluster score.
     */
    public List <Integer> nodesInClusters;
    
    /**
     * This is an answer parameter: # of visited nodes/peers
     */
    public long visitedNodes = 0l;
    
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
        this(queryObject, k, answerType, 0, 0.045f, 1.125f, 1.06f, 10, new Integer[] {7, 5, 3, 1}, 6000, ApproxKNNQueryOperation.LocalSearchType.ABS_OBJ_COUNT, LocalAbstractObject.UNKNOWN_DISTANCE);
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
        this(queryObject, k, AnswerType.REMOTE_OBJECTS, 0, 0.045f, 1.125f, 1.06f, 10, new Integer[] {7, 5, 3, 1}, localSearchParam, localSearchType, LocalAbstractObject.UNKNOWN_DISTANCE);
    }
    
    /**
     * Creates a new instance of ApproxKNNQueryOperationMIndex with full parameters.
     * {@link AnswerType#REMOTE_OBJECTS} will be returned in the result.
     * @param queryObject query object
     * @param k number of objects to be returned
     * @param clustersToVisit if greater than 0 then taken as the fixed number of clusters to be visited by the operation
     * @param basicDifferenceConst {@link ApproxKNNQueryOperationMIndex#basicDifferenceConst}
     * @param differenceDivisionConst {@link ApproxKNNQueryOperationMIndex#differenceDivisionConst}
     * @param maximalClusterScore the maximal score of a cluster to be included in the search
     * @param maximumClustersToVisit maximal number of clusters to be visited by this operation
     * @param nodesInClusters n array of maximal numbers of peers to be visited within each cluster; the clusters are taken in the order of the cluster score.
     * @param localSearchParam local search parameter - typically approximation parameter
     * @param localSearchType type of the local search parameter
     * @param radiusGuaranteed radius for which the answer is guaranteed
     */
    @AbstractOperation.OperationConstructor({"Query object", "# of objects", "Fixed # clusters<br/>to visit", "Gap seeking<br/>diff. const", "diff. division<br/>const", 
            "max cluster<br/>score", "max # clusters<br/>to visit", "list of # of nodes/peers<br/>to visit per cluster", "Local search param", "Type of <br/>local search param", "guaranteed radius <br/>(-1 to switch off)"})
    public ApproxKNNQueryOperationMIndex(LocalAbstractObject queryObject, int k, int clustersToVisit, float basicDifferenceConst, float differenceDivisionConst, 
            float maximalClusterScore, int maximumClustersToVisit, Integer[] nodesInClusters, int localSearchParam, LocalSearchType localSearchType, float radiusGuaranteed) {
        super(queryObject, k, localSearchParam, localSearchType, radiusGuaranteed);
        this.clustersToVisit = clustersToVisit;
        this.basicDifferenceConst = basicDifferenceConst;
        this.differenceDivisionConst = differenceDivisionConst;
        this.maxClusterScore = maximalClusterScore;
        this.maxClustersToVisit = maximumClustersToVisit;
        this.nodesInClusters = Arrays.asList(nodesInClusters);
    }
        
    /**
     * Creates a new instance of ApproxKNNQueryOperationMIndex with full parameters.
     * @param queryObject query object
     * @param k number of objects to be returned
     * @param answerType the type of objects this operation stores in its answer
     * @param clustersToVisit if greater than 0 then taken as the fixed number of clusters to be visited by the operation
     * @param basicDifferenceConst {@link ApproxKNNQueryOperationMIndex#basicDifferenceConst}
     * @param differenceDivisionConst {@link ApproxKNNQueryOperationMIndex#differenceDivisionConst}
     * @param maximalClusterScore the maximal score of a cluster to be included in the search
     * @param maximumClustersToVisit maximal number of clusters to be visited by this operation
     * @param nodesInClusters n array of maximal numbers of peers to be visited within each cluster; the clusters are taken in the order of the cluster score.
     * @param localSearchParam local search parameter - typically approximation parameter
     * @param localSearchType type of the local search parameter
     * @param radiusGuaranteed radius for which the answer is guaranteed
     */
    @AbstractOperation.OperationConstructor({"Query object", "# of objects", "Answer type", "Fixed # clusters<br/>to visit", "Gap seeking<br/>diff. const", "diff. division<br/>const", 
            "max cluster<br/>score", "max # clusters<br/>to visit", "list of # of nodes/peers<br/>to visit per cluster", "Local search param", "Type of <br/>local search param", "guaranteed radius <br/>(-1 to switch off)"})
    public ApproxKNNQueryOperationMIndex(LocalAbstractObject queryObject, int k, AnswerType answerType, int clustersToVisit, float basicDifferenceConst, float differenceDivisionConst, 
            float maximalClusterScore, int maximumClustersToVisit, Integer[] nodesInClusters, int localSearchParam, LocalSearchType localSearchType, float radiusGuaranteed) {
        super(queryObject, k, answerType, localSearchParam, localSearchType, radiusGuaranteed);
        this.clustersToVisit = clustersToVisit;
        this.basicDifferenceConst = basicDifferenceConst;
        this.differenceDivisionConst = differenceDivisionConst;
        this.maxClusterScore = maximalClusterScore;
        this.maxClustersToVisit = maximumClustersToVisit;
        this.nodesInClusters = Arrays.asList(nodesInClusters);
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
                return visitedNodes;
            case 3:
                return clustersToVisit;
            case 4:
                return basicDifferenceConst;
            case 5: 
                return differenceDivisionConst;
            case 6:
                return maxClusterScore;
            case 7:
                return maxClustersToVisit;
            case 8:
                return nodesInClusters;
            case 9:
                return localSearchParam;
            default:
                throw new IndexOutOfBoundsException("ApproxKNNQueryOperationMIndex has only 10 arguments");
        }
    }

    /**
     * Returns number of arguments that were passed while constructing this instance.
     * @return number of arguments that were passed while constructing this instance
     */
    @Override
    public int getArgumentCount() {
        return 10;
    }    
    
    /**
     * Returns the information about this operation.
     * @return the information about this operation
     */
    @Override
    public String toString() {
        return new StringBuffer(super.toString()).
                append("\nclusters to visit: ").append(clustersToVisit).
                append("; basic difference const.: ").append(basicDifferenceConst).
                append("; difference division const.: ").append(differenceDivisionConst).
                append("; max cluster score.: ").append(maxClusterScore).
                append(";\nmax # of clusters to visit: ").append(maxClustersToVisit).
                append("; # of visited nodes/peers in each cluster: ").append(Arrays.toString(nodesInClusters.toArray())).
                append("; local search param: ").append(localSearchParam).
                append("; guaranteed radius: ").append(radiusGuaranteed).
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

        return ((clustersToVisit == castObj.clustersToVisit) && (basicDifferenceConst == castObj.basicDifferenceConst) 
                && (differenceDivisionConst == castObj.differenceDivisionConst) && (maxClusterScore == castObj.maxClusterScore) 
                && (maxClustersToVisit == castObj.maxClustersToVisit) && (localSearchParam == castObj.localSearchParam) 
                && (localSearchType == castObj.localSearchType) && (radiusGuaranteed == castObj.radiusGuaranteed));
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return super.dataHashCode() << 8 + clustersToVisit + (int) basicDifferenceConst + (int) differenceDivisionConst + (int) maxClusterScore + maxClustersToVisit + localSearchParam + (int)radiusGuaranteed;
    }

}
