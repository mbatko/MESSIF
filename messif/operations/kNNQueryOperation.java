/*
 * kNNQueryOperation.java
 *
 * Created on 6. kveten 2004, 17:31
 */

package messif.operations;

import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;

/**
 * K-nearest neighbors query operation.
 * Retrieves <code>k</code> objects that are nearest to the specified query object
 * (according to the distance measure).
 * 
 * @author  Vlastislav Dohnal, xdohnal@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
@AbstractOperation.OperationName("k-nearest neighbors query")
public class kNNQueryOperation extends RankingQueryOperation {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Query object */
    protected final LocalAbstractObject queryObject;

    /** Number of nearest objects to retrieve */
    protected final int k;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of kNNQueryOperation for a given query object and maximal number of objects to return.
     * Objects added to answer are updated to {@link AnswerType#REMOTE_OBJECTS remote objects}.
     * @param queryObject the object to which the nearest neighbors are searched
     * @param k the number of nearest neighbors to retrieve
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects"})
    public kNNQueryOperation(LocalAbstractObject queryObject, int k) {
        this(queryObject, k, AnswerType.REMOTE_OBJECTS);
    }

    /**
     * Creates a new instance of kNNQueryOperation for a given query object and maximal number of objects to return.
     * @param queryObject the object to which the nearest neighbors are searched
     * @param k the number of nearest neighbors to retrieve
     * @param answerType the type of objects this operation stores in its answer
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects", "Answer type"})
    public kNNQueryOperation(LocalAbstractObject queryObject, int k, AnswerType answerType) {
        super(answerType, k);
        this.queryObject = queryObject;
        this.k = k;
    }


    //****************** Attribute access ******************//

    /**
     * Returns the query object of this k-NN query.
     * @return the query object of this k-NN query
     */
    public LocalAbstractObject getQueryObject() {
        return queryObject;
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
            return queryObject;
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

            if (queryObject.excludeUsingPrecompDist(object, getAnswerThreshold()))
                continue;

            addToAnswer(queryObject, object, getAnswerThreshold());
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
        // The argument obj is always kNNQueryOperation or its descendant, because it has only abstract ancestors
        kNNQueryOperation castObj = (kNNQueryOperation)obj;

        if (!queryObject.dataEquals(castObj.queryObject))
            return false;

        return k == castObj.k;
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
