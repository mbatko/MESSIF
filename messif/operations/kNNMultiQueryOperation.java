/*
 * kNNMultiQueryOperation
 *
 */

package messif.operations;

import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;

/**
 * K-nearest neighbors query operation with multiple query objects.
 * Retrieves <code>k</code> objects that are nearest to the specified query objects
 * (according to the distance measure).
 * 
 * @author petra.budikova@fi.muni.cz
 */
public class kNNMultiQueryOperation extends RankingQueryOperation {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Query object */
    private final LocalAbstractObject[] queryObjects;

    /** Number of nearest objects to retrieve */
    private final int k;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of kNNQueryOperation for given query objects and maximal number of objects to return.
     * Objects added to answer are updated to {@link AnswerType#REMOTE_OBJECTS remote objects}.
     * @param queryObjects the objects to which the nearest neighbors are searched
     * @param k the number of nearest neighbors to retrieve
     */
    @AbstractOperation.OperationConstructor({"Query objects", "Number of nearest objects"})
    public kNNMultiQueryOperation(LocalAbstractObject[] queryObjects, int k) {
        this(queryObjects, k, AnswerType.REMOTE_OBJECTS);
    }

    /**
     * Creates a new instance of kNNQueryOperation for given query objects and maximal number of objects to return.
     * @param queryObjects the objects to which the nearest neighbors are searched
     * @param k the number of nearest neighbors to retrieve
     * @param answerType the type of objects this operation stores in its answer
     */
    @AbstractOperation.OperationConstructor({"Query objects", "Number of nearest objects", "Answer type"})
    public kNNMultiQueryOperation(LocalAbstractObject[] queryObjects, int k, AnswerType answerType) {
        this(queryObjects, k, false, answerType);
    }

    /**
     * Creates a new instance of kNNQueryOperation for given query objects and maximal number of objects to return.
     * @param queryObjects the objects to which the nearest neighbors are searched
     * @param k the number of nearest neighbors to retrieve
     * @param storeMetaDistances if <tt>true</tt>, all processed {@link messif.objects.MetaObject meta objects} will
     *          store their {@link messif.objects.util.RankedAbstractMetaObject sub-distances} in the answer
     * @param answerType the type of objects this operation stores in its answer
     */
    @AbstractOperation.OperationConstructor({"Query objects", "Number of nearest objects", "Store the meta-object subdistances?", "Answer type"})
    public kNNMultiQueryOperation(LocalAbstractObject[] queryObjects, int k, boolean storeMetaDistances, AnswerType answerType) {
        super(answerType, k, storeMetaDistances);
        this.queryObjects = queryObjects;
        this.k = k;
    }


    //****************** Attribute access ******************//

    /**
     * Returns the query objects of this operation.
     * @return the query objects of this operation
     */
    public LocalAbstractObject[] getQueryObjects() {
        return queryObjects.clone();
    }

    /**
     * Returns the number of nearest objects to retrieve.
     * @return the number of nearest objects to retrieve
     */
    public int getK() {
        return k;
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
            return queryObjects;
        case 1:
            return k;
        default:
            throw new IndexOutOfBoundsException("kNNQueryOperation has only two arguments");
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
        // Iterate through all supplied objects
        while (objects.hasNext()) {
            // Get current object
            LocalAbstractObject object = objects.next();

            float distance = 0;
            for (int i = 0; i < queryObjects.length; i++) {
                distance += queryObjects[i].getDistance(object);
            }
            addToAnswer(object, distance, null);
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
        for (int i = 0; i < queryObjects.length; i++)
            queryObjects[i].clearSurplusData();
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
        // The argument obj is always kNNQueryOperation or its descendant, because it has only abstract ancestors
        kNNMultiQueryOperation castObj = (kNNMultiQueryOperation)obj;

        if (k != castObj.k)
            return false;

        if (queryObjects.length != castObj.queryObjects.length)
            return false;

        for (int i = 0; i < queryObjects.length; i++)
            if (!queryObjects[i].dataEquals(castObj.queryObjects[i]))
                return false;

        return true;
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        int hc = k;
        for (int i = 0; i < queryObjects.length; i++)
            hc += queryObjects[i].dataHashCode() << i;
        return hc;
    }

}
