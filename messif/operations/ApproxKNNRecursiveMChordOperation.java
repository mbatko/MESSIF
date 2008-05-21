
package messif.operations;

import java.util.Arrays;
import java.util.List;
import messif.objects.LocalAbstractObject;

/**
 * Approximate k-nearest neighbors query parametrized for the Recursive M-Chord with M-Tree in individual peers.
 * This query extends the standard approximate k-nearest neighbors query
 * with settable approximation parameters. 
 * 
 * @author David Novak, FI Masaryk University, Brno, Czech Republic; <a href="mailto:david.novak@fi.muni.cz">david.novak@fi.muni.cz</a>
 */
@AbstractOperation.OperationName("Approximate kNN Query parametrized for Recursive M-Chord")
public class ApproxKNNRecursiveMChordOperation extends ApproxKNNQueryOperation {

    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;
        
    /** If greater than 0 then taken as the fixed number of clusters to be visited by the operation. */
    public int clustersToVisit;

    /** 
     * If greater than or equal to 1 then the clusters are taken in the order by the "score" and it is searched for a "score gap"
     * greater than "basicDifferenceConst * previous score". The basicDifferenceConst is reduced by dividing
     * differenceDivisionConst in each step of the loop.
     */
    public float basicDifferenceConst;
    
    /** 
     * If greater than or equal to 1 then the clusters are taken in the order by the "score" and it is searched for a "score gap"
     * greater than "basicDifferenceConst * previous score". The basicDifferenceConst is reduced by dividing
     * differenceDivisionConst in each step of the loop.
     */
    public float differenceDivisionConst;

    /** The maximal score of a cluster to be included in the search. */
    public float maxClusterScore;
    
    /** Maximal number of clusters to be visited by this operation.  */
    public int maxClustersToVisit;
    
    /** 
     * An array of maximal numbers of peers to be visited within each cluster; the clusters are taken in the order 
     *  of the cluster score.
     */
    public List <Integer> peersInClusters;

    /** Maximal percentage of local data to be explored */
    public int maxLocalDataPercentage;
    
    
    /** Radius for which the answer is guaranteed */
    public float radiusGuaranteed;
    
    
    /**
     * Creates a new instance of ApproxKNNRecursiveMChordOperation with default parameters.
     * @param queryObject query object
     * @param k number of objects to be returned
     */
    @AbstractOperation.OperationConstructor({"Query object", "# of nearest objects"})
    public ApproxKNNRecursiveMChordOperation(LocalAbstractObject queryObject, int k) {
        this(queryObject, k, 0, 0.045f, 1.125f, 1.06f, 10, new Integer[] {7, 5, 3, 1}, 30, LocalAbstractObject.UNKNOWN_DISTANCE);
    }
    
    /**
     * Creates a new instance of ApproxKNNRecursiveMChordOperation with full parameters.
     * @param queryObject query object
     * @param k number of objects to be returned
     * @param clustersToVisit if greater than 0 then taken as the fixed number of clusters to be visited by the operation
     * @param basicDifferenceConst {@link ApproxKNNRecursiveMChordOperation#basicDifferenceConst}
     * @param differenceDivisionConst {@link ApproxKNNRecursiveMChordOperation#differenceDivisionConst}
     * @param maximalClusterScore the maximal score of a cluster to be included in the search
     * @param maximumClustersToVisit maximal number of clusters to be visited by this operation
     * @param peersInClusters n array of maximal numbers of peers to be visited within each cluster; the clusters are taken in the order of the cluster score.
     * @param maxLocalDataPercentage maximal percentage of local data to be explored
     * @param radiusGuaranteed radius for which the answer is guaranteed
     */
    @AbstractOperation.OperationConstructor({"Query object", "# of objects", "Fixed # clusters<br/>to visit", "Gap seeking<br/>diff. const", "diff. division<br/>const", 
            "max cluster<br/>score", "max # clusters<br/>to visit", "list of # of peers<br/>to visit per cluster", "Max % of local data<br/>to be explored", "guaranteed radius <br/>(-1 to switch off)"})
    public ApproxKNNRecursiveMChordOperation(LocalAbstractObject queryObject, int k, int clustersToVisit, float basicDifferenceConst, float differenceDivisionConst, 
            float maximalClusterScore, int maximumClustersToVisit, Integer[] peersInClusters, int maxLocalDataPercentage, float radiusGuaranteed) {
        super(queryObject, k);
        this.clustersToVisit = clustersToVisit;
        this.basicDifferenceConst = basicDifferenceConst;
        this.differenceDivisionConst = differenceDivisionConst;
        this.maxClusterScore = maximalClusterScore;
        this.maxClustersToVisit = maximumClustersToVisit;
        this.peersInClusters = Arrays.asList(peersInClusters);
        this.maxLocalDataPercentage = maxLocalDataPercentage;
        this.radiusGuaranteed = radiusGuaranteed;
    }

    /** 
     * Merge information about the visited nodes to an info string.
     * @param operation the operation to update answer from
     */
    @Override
    public void updateAnswer(AbstractOperation operation) {
        super.updateAnswer(operation);
        if (operation instanceof ApproxKNNRecursiveMChordOperation) {
            ApproxKNNRecursiveMChordOperation casted = (ApproxKNNRecursiveMChordOperation) operation;
            if (casted.radiusGuaranteed < radiusGuaranteed)
                radiusGuaranteed = casted.radiusGuaranteed;
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
                return basicDifferenceConst;
            case 4: 
                return differenceDivisionConst;
            case 5:
                return maxClusterScore;
            case 6:
                return maxClustersToVisit;
            case 7:
                return peersInClusters;
            case 8:
                return maxLocalDataPercentage;
            case 9:
                return radiusGuaranteed;
            default:
                throw new IndexOutOfBoundsException("ApproxKNNMRecursiveMChordOperation has only nine arguments");
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
                append("; # of visited peers in each cluster: ").append(Arrays.toString(peersInClusters.toArray())).
                append("; max local data to explore: ").append(maxLocalDataPercentage).
                append("%; guaranteed radius: ").append(radiusGuaranteed).
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
        if (!(obj instanceof ApproxKNNRecursiveMChordOperation) || !super.dataEqualsImpl(obj))
            return false;

        ApproxKNNRecursiveMChordOperation castObj = (ApproxKNNRecursiveMChordOperation)obj;

        if (clustersToVisit != castObj.clustersToVisit)
            return false;
        if (basicDifferenceConst != castObj.basicDifferenceConst)
            return false;
        if (differenceDivisionConst != castObj.differenceDivisionConst)
            return false;
        if (maxClusterScore != castObj.maxClusterScore)
            return false;
        if (maxClustersToVisit != castObj.maxClustersToVisit)
            return false;
        if (maxLocalDataPercentage != castObj.maxLocalDataPercentage)
            return false;
        return (radiusGuaranteed == castObj.radiusGuaranteed);
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return super.dataHashCode() << 8 + clustersToVisit + (int) basicDifferenceConst + (int) differenceDivisionConst + (int) maxClusterScore + maxClustersToVisit + maxLocalDataPercentage + (int)radiusGuaranteed;
    }

}
