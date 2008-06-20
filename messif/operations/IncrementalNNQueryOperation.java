/*
 * IncrementalNNQueryOperation.java
 *
 * Created on September 5, 2006, 11:29
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package messif.operations;

import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;
import messif.objects.MeasuredAbstractObjectList;
import java.util.Iterator;
import messif.netbucket.RemoteAbstractObject;
import messif.objects.GenericObjectIterator;
import messif.utility.ErrorCode;

/**
 * Incremental Nearest Neighbor Search.
 * This operation returns only {@link messif.netbucket.RemoteAbstractObject references} to the original
 * object.
 * 
 * The behavior of an algorithm implementing this operation must hold the following contract:<br>
 * 1/ If there are some objects to pending, the operation must be ended with 
 *    {@link messif.operations.OperationErrorCode#HAS_NEXT HAS_NEXT} error code by calling
 *    {@link messif.operations.AbstractOperation#endOperation(ErrorCode) endOperation} method.<br>
 * 2/ If all objects have been returned in previous calls to the search or in this call, the operation
 *    must be ended with {@link messif.operations.OperationErrorCode#RESPONSE_RETURNED RESPONSE_RETURNED} error code.
 *    This eror code is set automaticly if {@link messif.operations.QueryOperation#endOperation(ErrorCode) endOperation} 
 *    is not called with a specific value.<br>
 * 
 * This contract is similar to the behavior of iterators.
 *
 * The call to <code>wasSuccessful()</code> returns <code>true</code> if the error code was set either to HAS_NEXT or RESPONSE_RETURNED.
 *
 * @author Vlastislav Dohnal, xdohnal@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
@AbstractOperation.OperationName("incremental nearest neighbors query")
public class IncrementalNNQueryOperation extends QueryOperation {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 2L;
    
    
    //****************** Query request attributes ******************/
    
    /** kNN query object (accessible directly) */
    public final LocalAbstractObject queryObject;
    
    /** Minimum number of objects returned by this query.
     * Default value is 1.
     * It can be set to any larger value in order to optimize the process of 
     * searching for nearest neighbors. This feature is usually used by 
     * distributed incremental search algoritms.
     */
    public final int minNN;
    
    /** 
     * The number of nearest neighbors added to the answer since the last call to endOperation().
     * I.e., how many NN were added in one evaluation of this operation.
     */
    protected int nnAddedToAnswer;
    
    
    //****************** Query answer attributes ******************/

    /** The answer list of this operation */
    protected final MeasuredAbstractObjectList<AbstractObject> answer;
     
    
    //****************** Constructors ******************/

    /**
     * Creates a new instance of IncrementalNNQueryOperation.
     * @param queryObject the object to which the nearest neighbors are searched
     * @param minNN the minimal number of nearest neighbors to retrieve
     */
    @AbstractOperation.OperationConstructor({"Query object", "Minimum number of nearest objects"})
    public IncrementalNNQueryOperation(LocalAbstractObject queryObject, int minNN) {
        this.queryObject = queryObject;
        this.minNN = minNN;
        this.answer = new MeasuredAbstractObjectList<AbstractObject>();
        this.nnAddedToAnswer = 0;
    }
    
    /**
     * Creates a new instance of IncrementalNNQueryOperation.
     * @param queryObject the object to which the nearest neighbors are searched
     */
    @AbstractOperation.OperationConstructor({"Query object"})
    public IncrementalNNQueryOperation(LocalAbstractObject queryObject) {
        this(queryObject, 1);
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
            return minNN;
        default:
            throw new IndexOutOfBoundsException("IncrementalNNQueryOperation has only two arguments");
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


    /**
     * Returns <code>true</code> if this operation has finished successfuly.
     * Otherwise, <code>false</code> is returned - the operation was either unsuccessful or is has not finished yet.
     *
     * @return <code>true</code> if this operation has finished successfuly
     */
    @Override
    public boolean wasSuccessful() {
        return errValue.equals(OperationErrorCode.RESPONSE_RETURNED) || errValue.equals(OperationErrorCode.HAS_NEXT);
    }

    /** End operation successfully */
    @Override
    public void endOperation() {
        errValue = OperationErrorCode.RESPONSE_RETURNED;
        this.nnAddedToAnswer = 0;
    }
    
    /**
     * End operation with a specific error code.
     * @param errValue the error code to set
     * @throws IllegalArgumentException if the specified error value is <tt>null</tt> or {@link ErrorCode#NOT_SET}
     */
    @Override
    public void endOperation(ErrorCode errValue) throws IllegalArgumentException {
        super.endOperation(errValue);
        this.nnAddedToAnswer = 0;
    }

    
    //****************** Default implementation of query evaluation ******************/
    
    /**
     * Evaluate this query on a given set of objects.
     * The objects found by this evaluation are added to answer of this query via {@link #addToAnswer}.
     * Note that the incremental kNN search can't use filtering, because the maximal radius is unknown
     *
     * @param objects the collection of objects on which to evaluate this query
     * @return number of objects satisfying the query
     */
    public int evaluate(GenericObjectIterator<LocalAbstractObject> objects) {
        int beforeSize = getAnswerCount();

        // Iterate through all supplied objects
        while (objects.hasNext()) {
            LocalAbstractObject object = objects.next();
            
            // Get distance to query object
            float distance = queryObject.getDistance(object);

            // Object satisfies the query (i.e. distance is smaller than radius)
            addToAnswer(object, distance);
        }

        return getAnswerCount() - beforeSize;
    }

    
    //****************** Answer methods ******************/

    /**
     * Returns the number of objects in this query answer.
     * @return the number of objects in this query answer
     */
    public int getAnswerCount() {
        return answer.size();
    }

    /**
     * Returns <tt>true</tt> if the minimum number of objects has been inserted to 
     * the answer during one evaluation of this operation.
     * @return <tt>true</tt> if the current answer has a minimal number of objects
     */
    public boolean isFilledEnough() {
        return nnAddedToAnswer >= minNN;
    }

    /**
     * Returns an iterator over all objects in the answer to this query.
     * @return an iterator over all objects in the answer to this query
     */
    public Iterator<AbstractObject> getAnswer() { 
        return answer.objects();
    }
    
    /**
     * Returns an iterator over pairs of objects and their distances from the query object of this query. 
     * The object of a pair is accessible through {@link messif.objects.MeasuredAbstractObjectList.Pair#getObject}.
     * The associated distance of a pair is accessible through {@link messif.objects.MeasuredAbstractObjectList.Pair#getDistance}.
     * 
     * @return an iterator over pairs of objects and their distances from the query object of this query
     */
    public Iterator<MeasuredAbstractObjectList.Pair<AbstractObject>> getAnswerDistances() {
        return answer.iterator();
    }

    /**
     * Add an object with a measured distance to the answer.
     * 
     * @param object the object to add
     * @param distance the distance of the object
     * @return <code>true</code> if the <code>object</code> has been added to the answer. Otherwise <code>false</code>.
     */
    public boolean addToAnswer(AbstractObject object, float distance) { 
        answer.add(object.getRemoteAbstractObject(), distance);
        ++nnAddedToAnswer;
        return true;
    }
    
    /**
     * Add all objects with distances from the passed iterator to the answer of this operation.
     *
     * @param iterator iterator over object-distance pairs that should be added to this operation's answer
     * @return <code>true</code> if at least one object has been added to the answer. Otherwise <code>false</code>.
     */
    @Override
    public int addToAnswer(Iterator<MeasuredAbstractObjectList.Pair<AbstractObject>> iterator) { 
        int retVal = 0;
        while (iterator.hasNext()) {
            MeasuredAbstractObjectList.Pair<AbstractObject> pair = iterator.next();
            if (RemoteAbstractObject.class.isInstance(pair.getObject()))
                answer.add(pair);
            else
                answer.add(pair.getObject().getRemoteAbstractObject(), pair.getDistance());
            retVal++;
            nnAddedToAnswer++;
        }
        return retVal;
    }

    /**
     * Reset the current query answer.
     */
    @Override
    public void resetAnswer() {
        answer.clear();
    }

    /**
     * Returns a string representation of this operation.
     * @return a string representation of this operation
     */
    @Override
    public String toString() {
        return new StringBuffer("IncrementalNN query <").append(queryObject).append(',').append(minNN).append("> returned "
                ).append(getAnswerCount()).append(" objects (max distance is ").append(answer.getLastDistance()).append(")").toString();
    }

    /**
     * Clear non-messif data stored in operation.
     * This method is intended to be called whenever the operation is
     * sent back to client in order to minimize problems with unknown
     * classes after deserialization.
     */
    @Override
    public void clearSuplusData() {
        super.clearSuplusData();
        queryObject.clearSurplusData();
    }


    /****************** Equality driven by object data ******************/

    /** 
     * Indicates whether some other operation has the same data as this one.
     * @param   obj   the reference object with which to compare.
     * @return  <code>true</code> if this object has the same data as the obj
     *          argument; <code>false</code> otherwise.
     */
    @Override
    protected boolean dataEqualsImpl(AbstractOperation obj) {
        // The argument obj is always kNNQueryOperation or its descendant, because it has only abstract ancestors
        IncrementalNNQueryOperation castObj = (IncrementalNNQueryOperation)obj;

        if (!queryObject.dataEquals(castObj.queryObject))
            return false;

        return minNN == castObj.minNN;
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return (queryObject.dataHashCode() << 8) + minNN;
    }

}
