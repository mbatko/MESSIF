package messif.operations.query;

import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.util.AggregationFunction;
import messif.operations.AbstractOperation;
import messif.operations.QueryOperation;


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
 * @see AggregationFunction
 * @author xbatko
 */
@AbstractOperation.OperationName("Combined top-k query")
public class TopCombinedQueryOperation extends AggregationFunctionQueryOperation {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 85603L;

    //****************** Attributes ******************//

    /** Number of sorted access objects to retrieve */
    protected final int numberOfInitialSA;

    /**
     * Progressive flag for the number of initial sorted accesses.
     * If set to <tt>true</tt>, the number of numberOfInitialSA is multiplied by {@link #k k}.
     */
    protected final boolean numberOfInitialSAProgressive;

    /** Number of random accesses to execute */
    protected final int numberOfRandomAccesses;

    /** Query operation to execute for sorted accesses */
    protected final Class<? extends QueryOperation> initialSAQueryClass;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of TopCombinedQueryOperation.
     * The query object should be {@link messif.objects.MetaObject} in order to query multiple lists.
     * The parameter names for the aggregation should match the names in the {@link messif.objects.MetaObject}.
     * 
     * @param queryObject the query object
     * @param k the number of results to retrieve
     * @param numberOfInitialSA the number of initial sorted access objects
     * @param numberOfInitialSAProgressive flag whether the <code>numberOfInitialSA</code> is a multiplier of <code>k</code> (<tt>true</tt>) or an absolute number (<tt>false</tt>)
     * @param numberOfRandomAccesses the maximal number of random accesses
     * @param initialSAQueryClass the query operation used to retrieve sorted access objects
     * @param aggregationFunction the aggregation function for combining the distances from sorted lists
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects", "Number of initial sorted access objects", "Progressive sorted access flag", "Number of random accesses", "Query operation for sorted access", "Aggregation function"})
    public TopCombinedQueryOperation(LocalAbstractObject queryObject, int k, int numberOfInitialSA, boolean numberOfInitialSAProgressive, int numberOfRandomAccesses, Class<? extends QueryOperation> initialSAQueryClass, AggregationFunction aggregationFunction) {
        super(queryObject, k, aggregationFunction);
        this.numberOfInitialSA = numberOfInitialSA;
        this.numberOfInitialSAProgressive = numberOfInitialSAProgressive;
        this.numberOfRandomAccesses = numberOfRandomAccesses;
        this.initialSAQueryClass = initialSAQueryClass;
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
            return numberOfInitialSA;
        case 3:
            return numberOfInitialSAProgressive;
        case 4:
            return numberOfRandomAccesses;
        case 5:
            return initialSAQueryClass;
        case 6:
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
        return 7;
    }

    /**
     * Returns the number of initial sorted access objects to retrieve.
     * @return the number of initial sorted access objects to retrieve
     */
    public int getNumberOfInitialSA() {
        return numberOfInitialSA;
    }

    /**
     * Returns the progressive flag for the number of initial sorted accesses.
     * If set to <tt>true</tt>, the number of numberOfInitialSA is multiplied by {@link #k k}.
     * @return the progressive flag for the number of initial sorted accesses
     */
    public boolean isNumberOfInitialSAProgressive() {
        return numberOfInitialSAProgressive;
    }

    /**
     * Returns the number of random accesses to execute.
     * @return the number of random accesses to execute
     */
    public int getNumberOfRandomAccesses() {
        return numberOfRandomAccesses;
    }

    /**
     * Returns the class of the query operation to execute for initial sorted accesses.
     * @return the class of the query operation to execute for initial sorted accesses
     */
    public Class<? extends QueryOperation> getInitialSAQueryClass() {
        return initialSAQueryClass;
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

        if (!super.dataEquals(obj))
            return false;

        if (numberOfInitialSA != castObj.numberOfInitialSA)
            return false;
        if (numberOfRandomAccesses != castObj.numberOfRandomAccesses)
            return false;
        return initialSAQueryClass.equals(castObj.initialSAQueryClass);
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return (super.dataHashCode() << 8) + numberOfInitialSA + numberOfRandomAccesses;
    }

}
