/*
 * TopCombinedQueryOperation.java
 *
 * Created on 21. cerven 2007, 16:46
 *
 */

package messif.operations;

import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.util.ThresholdFunction;


/**
 * Top-k combined query operation.
 * Allows to retrieve the best-matching <code>k</code> objects from several sorted lists
 * (usually results of k-nearest neighbor queries). The aggregation function for combining the
 * distances in respective sorted lists can be specified as a "plug-in".
 * 
 * <p>
 * The <i>threshold algorithm</i> is used to actualy evaluate this query.
 * </p>
 * 
 * @see messif.objects.MetaObject
 * @see ThresholdFunction
 * @author xbatko
 */
@AbstractOperation.OperationName("Combined top-k query")
public class TopCombinedQueryOperation extends RankingQueryOperation {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 2L;

    //****************** Attributes ******************//

    /** Query object (accessible directly) */
    public final MetaObject queryObject;
    /** Number of nearest (top) objects to retrieve (accessible directly) */
    public final int k;
    /** Number of sorted access objects to retrieve (accessible directly) */
    public final int sortedAccessInitial;
    /**
     * Progressive flag for the number of initial sorted accesses.
     * If set to <tt>true</tt>, the number of sortedAccessInitial is multiplied by {@link #k k}.
     */
    public final boolean progressiveSortedAccessInitial;
    /** Number of random accesses to execute (accessible directly) */
    public final int numberOfRandomAccess;
    /** Query operation to execute for sorted accesses (accessible directly) */
    public final Class<? extends QueryOperation> sortedQuery;
    /** Threshold function to measure the overall similarity with (accessible directly) */
    public final ThresholdFunction thresholdFunction;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of TopCombinedQueryOperation.
     * The query object should be {@link messif.objects.MetaObject} in order to query multiple lists.
     * The parameter names for the aggregation should match the names in the {@link messif.objects.MetaObject}.
     * 
     * @param queryObject the query object
     * @param k the number of results to retrieve
     * @param sortedAccessInitial the number of initial sorted access objects
     * @param progressiveSortedAccessInitial flag whether the <code>sortedAccessInitial</code> is a multiplier of <code>k</code> (<tt>true</tt>) or an absolute number (<tt>false</tt>)
     * @param numberOfRandomAccess the maximal number of random accesses
     * @param sortedQuery the query operation used to retrieve sorted access objects
     * @param thresholdFunction the aggregation function for combining the distances from sorted lists
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects", "Number of initial sorted access objects", "Progressive sorted access flag", "Number of random accesses", "Query operation for sorted access", "Aggregation function"})
    public TopCombinedQueryOperation(LocalAbstractObject queryObject, int k, int sortedAccessInitial, boolean progressiveSortedAccessInitial, int numberOfRandomAccess, Class<? extends QueryOperation> sortedQuery, ThresholdFunction thresholdFunction) {
        this.queryObject = (MetaObject)queryObject;
        this.k = k;
        this.sortedAccessInitial = sortedAccessInitial;
        this.progressiveSortedAccessInitial = progressiveSortedAccessInitial;
        this.numberOfRandomAccess = numberOfRandomAccess;
        this.sortedQuery = sortedQuery;
        this.thresholdFunction = thresholdFunction;
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
            return sortedAccessInitial;
        case 3:
            return progressiveSortedAccessInitial;
        case 4:
            return numberOfRandomAccess;
        case 5:
            return sortedQuery;
        case 6:
            return thresholdFunction;
        default:
            throw new IndexOutOfBoundsException("TopCombinedQueryOperation has only four arguments");
        }
    }

    /**
     * Returns number of arguments that were passed while constructing this instance.
     * @return number of arguments that were passed while constructing this instance
     */
    @Override
    public int getArgumentCount() {
        return 7;
    }


    //****************** Implementation of query evaluation ******************//

    /**
     * Evaluate this query on a given set of objects.
     * The objects found by this evaluation are added to answer of this query via {@link #addToAnswer}.
     *
     * @param objects the collection of objects on which to evaluate this query
     * @return number of objects satisfying the query
     */
    @Override
    public int evaluate(AbstractObjectIterator<? extends LocalAbstractObject> objects) {
        int beforeCount = getAnswerCount();
        float[] descriptorDistances = new float[thresholdFunction.getParameterNames().length];

        while (objects.hasNext()) {
            // Get current object
            MetaObject object = (MetaObject)objects.next();

            // Compute overall distance (the object must be MetaObject otherwise ClassCastException is thrown)
            float distance = thresholdFunction.getDistance(queryObject, object, descriptorDistances);

            // Object satisfies the query (i.e. distance is smaller than radius)
            addToAnswer(object, distance, descriptorDistances.clone());
        }

        return getAnswerCount() - beforeCount;
    }

    //****************** Overrides ******************//

    /**
     * Clear non-messif data stored in operation.
     * This method is intended to be called whenever the operation is
     * sent back to client in order to minimize problems with unknown
     * classes after deserialization.
     */
    @Override
    public void clearSurplusData() {
        super.clearSurplusData();
        queryObject.clearSurplusData();
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
        // The argument obj is always TopCombinedQueryOperation or its descendant, because it has only abstract ancestors
        TopCombinedQueryOperation castObj = (TopCombinedQueryOperation)obj;

        if (!queryObject.dataEquals(castObj.queryObject))
            return false;
        if (k != castObj.k)
            return false;
        if (sortedAccessInitial != castObj.sortedAccessInitial)
            return false;
        if (numberOfRandomAccess != castObj.numberOfRandomAccess)
            return false;
        if (!sortedQuery.equals(castObj.sortedQuery))
            return false;
        return thresholdFunction.equals(castObj.thresholdFunction);
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return (queryObject.dataHashCode() << 8) + k;
    }

}
