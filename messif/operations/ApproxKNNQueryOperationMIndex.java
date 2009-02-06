
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
    private static final long serialVersionUID = 20202L;
        
    /** If greater than 0 then taken as the fixed number of clusters to be visited by the operation. */
    protected int clustersToVisit;

    /** M-Index level to be used for generating the initial set of clusters to be visited. */
    protected int levelForVariants;
    
    /** This is an answer parameter: # of visited nodes/peers */
    protected long visitedNodes = 0l;


    // ************************************ Getters and setters  ******************************* //

    public int getClustersToVisit() {
        return clustersToVisit;
    }

    public int getLevelForVariants() {
        return levelForVariants;
    }

    public long getVisitedNodes() {
        return visitedNodes;
    }

    public void setVisitedNodes(long visitedNodes) {
        this.visitedNodes = visitedNodes;
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
        this(queryObject, k, answerType, 10, 2, 5000, ApproxKNNQueryOperation.LocalSearchType.ABS_OBJ_COUNT, LocalAbstractObject.UNKNOWN_DISTANCE);
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
        this(queryObject, k, AnswerType.REMOTE_OBJECTS, 10, 2, localSearchParam, localSearchType, LocalAbstractObject.UNKNOWN_DISTANCE);
    }

    /**
     * Creates a new instance of ApproxKNNQueryOperationMIndex with default parameters for distributed processing
     *  and specify parameters for centralized approximation.
     * @param queryObject query object
     * @param k number of objects to be returned
     * @param clustersToVisit if greater than 0 then taken as the fixed number of clusters to be visited by the operation
     * @param levelForVariants M-Index level to be used for generating the initial set of clusters to be visited
     * @param localSearchParam local search parameter - typically approximation parameter
     * @param localSearchType type of the local search parameter
     */
    @AbstractOperation.OperationConstructor({"Query object", "# of nearest objects",  "Local search param", "Type of <br/>local search param"})
    public ApproxKNNQueryOperationMIndex(LocalAbstractObject queryObject, int k, int clustersToVisit, int levelForVariants, int localSearchParam, LocalSearchType localSearchType) {
        this(queryObject, k, AnswerType.REMOTE_OBJECTS, clustersToVisit, levelForVariants, localSearchParam, localSearchType, LocalAbstractObject.UNKNOWN_DISTANCE);
    }
    
    /**
     * Creates a new instance of ApproxKNNQueryOperationMIndex with full parameters.
     * {@link AnswerType#REMOTE_OBJECTS} will be returned in the result.
     * @param queryObject query object
     * @param k number of objects to be returned
     * @param answerType the type of objects this operation stores in its answer
     * @param clustersToVisit if greater than 0 then taken as the fixed number of clusters to be visited by the operation
     * @param levelForVariants M-Index level to be used for generating the initial set of clusters to be visited
     * @param localSearchParam local search parameter - typically approximation parameter
     * @param localSearchType type of the local search parameter
     * @param radiusGuaranteed radius for which the answer is guaranteed
     */
    @AbstractOperation.OperationConstructor({"Query object", "# of objects", "Fixed # clusters<br/>to visit", "M-Index level to <br/>generate cluster <br/>variants for", "Local search param", "Type of <br/>local search param", "guaranteed radius <br/>(-1 to switch off)"})
    public ApproxKNNQueryOperationMIndex(LocalAbstractObject queryObject, int k, AnswerType answerType, int clustersToVisit, int levelForVariants, int localSearchParam, LocalSearchType localSearchType, float radiusGuaranteed) {
        super(queryObject, k, answerType, localSearchParam, localSearchType, radiusGuaranteed);
        this.clustersToVisit = clustersToVisit;
        this.levelForVariants = levelForVariants;
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
                return levelForVariants;
            case 4:
                return localSearchParam;
            default:
                throw new IndexOutOfBoundsException("ApproxKNNQueryOperationMIndex has only 5 arguments");
        }
    }

    /**
     * Returns number of arguments that were passed while constructing this instance.
     * @return number of arguments that were passed while constructing this instance
     */
    @Override
    public int getArgumentCount() {
        return 5;
    }    
    
    /**
     * Returns the information about this operation.
     * @return the information about this operation
     */
    @Override
    public String toString() {
        return new StringBuffer(super.toString()).
                append("\nclusters to visit: ").append(clustersToVisit).
                append("; M-Index level for variants: ").append(levelForVariants).
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

        return ((clustersToVisit == castObj.clustersToVisit) && (levelForVariants == castObj.levelForVariants) && (localSearchParam == castObj.localSearchParam)
                && (localSearchType == castObj.localSearchType) && (radiusGuaranteed == castObj.radiusGuaranteed));
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return super.dataHashCode() << 8 + clustersToVisit + levelForVariants + localSearchParam + (int)radiusGuaranteed;
    }

}
