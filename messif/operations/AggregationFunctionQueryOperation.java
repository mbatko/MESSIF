
package messif.operations;

import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.util.AggregationFunction;


/**
 * Aggregation function kNN query operation.
 * Allows to retrieve the best-matching <code>k</code> objects (metaobjects) from any
 * storage using the {@link messif.objects.util.AggregationFunction} function to evalute the 
 * distance between the query object and the objects stored.
 * 
 * @see messif.objects.MetaObject
 * @see AggregationFunction
 * @author david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("Aggregation-function query")
public class AggregationFunctionQueryOperation extends RankingQueryOperation {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 29601L;

    //****************** Attributes ******************//

    /** Query object (accessible directly) */
    protected final MetaObject queryObject;

    /** Number of nearest (top) objects to retrieve */
    protected final int k;

    /** Threshold function for measuring the overall similarity */
    protected final AggregationFunction aggregationFunction;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of AggregationFunctionQueryOperation.
     * The query object should be {@link messif.objects.MetaObject} to make the agg. function meaningful
     * The parameter names for the aggregation should match the names in the {@link messif.objects.MetaObject}.
     * 
     * @param queryObject the query object
     * @param k the number of results to retrieve
     * @param aggregationFunction the aggregation function for combining the distances from sorted lists
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects",  "Aggregation function"})
    public AggregationFunctionQueryOperation(LocalAbstractObject queryObject, int k, AggregationFunction aggregationFunction) {
        this(queryObject, k, aggregationFunction, AnswerType.REMOTE_OBJECTS, true);
    }

    /**
     * Creates a new instance of AggregationFunctionQueryOperation.
     * The query object should be {@link messif.objects.MetaObject} to make the agg. function meaningful
     * The parameter names for the aggregation should match the names in the {@link messif.objects.MetaObject}.
     *
     * @param queryObject the query object
     * @param k the number of results to retrieve
     * @param aggregationFunction the aggregation function for combining the distances from sorted lists
     * @param answerType the type of objects this operation stores in its answer
     * @param storeMetaDistances if <tt>true</tt>, all processed {@link MetaObject meta objects} will

     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects", "Aggregation function", "Answer type", "store also the sub-distances?"})
    public AggregationFunctionQueryOperation(LocalAbstractObject queryObject, int k, AggregationFunction aggregationFunction, AnswerType answerType, boolean storeMetaDistances) {
        super(answerType, k, storeMetaDistances);
        this.queryObject = (MetaObject)queryObject;
        this.k = k;
        this.aggregationFunction = aggregationFunction;
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
            return aggregationFunction;
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
        return 3;
    }

    /**
     * Returns the query (meta) object of this query operation.
     * @return the query (meta) object of this query operation
     */
    public MetaObject getQueryObject() {
        return queryObject;
    }

    /**
     * Returns the number of nearest (top) objects to retrieve.
     * @return the number of nearest (top) objects to retrieve
     */
    public int getK() {
        return k;
    }

    /**
     * Returns the threshold function for measuring the overall similarity.
     * @return the threshold function for measuring the overall similarity
     */
    public AggregationFunction getThresholdFunction() {
        return aggregationFunction;
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

        while (objects.hasNext()) {
            // Get current object
            MetaObject object = (MetaObject)objects.next();

            // Prepare array for subdistances
            float[] descriptorDistances;
            if (isStoringMetaDistances())
                descriptorDistances = new float[aggregationFunction.getParameterNames().length];
            else
                descriptorDistances = null;

            // Compute overall distance (the object must be MetaObject otherwise ClassCastException is thrown)
            float distance = aggregationFunction.getDistance(queryObject, object, descriptorDistances);

            // Object satisfies the query (i.e. distance is smaller than radius)
            addToAnswer(object, distance, descriptorDistances);
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
        AggregationFunctionQueryOperation castObj = (AggregationFunctionQueryOperation)obj;

        if (!queryObject.dataEquals(castObj.queryObject))
            return false;
        if (k != castObj.k)
            return false;
        return aggregationFunction.equals(castObj.aggregationFunction);
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
