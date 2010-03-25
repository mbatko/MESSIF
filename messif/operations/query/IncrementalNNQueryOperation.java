/*
 *  This file is part of MESSIF library.
 *
 *  MESSIF library is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MESSIF library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MESSIF library.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.operations.query;

import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.util.RankedAbstractObject;
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;
import messif.operations.OperationErrorCode;
import messif.operations.RankingQueryOperation;
import messif.utility.ErrorCode;


/**
 * Incremental Nearest Neighbor Search.
 * This operation returns only {@link messif.objects.NoDataObject references} to the original
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
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("incremental nearest neighbors query")
public class IncrementalNNQueryOperation extends RankingQueryOperation {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 2L;

    //****************** Attributes ******************//

    /** kNN query object */
    protected final LocalAbstractObject queryObject;

    /**
     * Minimum number of objects returned by this query.
     * Default value is 1.
     * It can be set to any larger value in order to optimize the process of 
     * searching for nearest neighbors. This feature is usually used by 
     * distributed incremental search algoritms.
     */
    protected final int minNN;

    /** 
     * The number of nearest neighbors added to the answer since the last call to endOperation().
     * I.e., how many NN were added in one evaluation of this operation.
     */
    protected int nnAddedToAnswer;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of IncrementalNNQueryOperation.
     * At least one next nearest neighbor will be returned for each execution.
     * {@link AnswerType#NODATA_OBJECTS} will be returned in the result.
     * @param queryObject the object to which the nearest neighbors are searched
     */
    @AbstractOperation.OperationConstructor({"Query object"})
    public IncrementalNNQueryOperation(LocalAbstractObject queryObject) {
        this(queryObject, 1);
    }

    /**
     * Creates a new instance of IncrementalNNQueryOperation.
     * {@link AnswerType#NODATA_OBJECTS} will be returned in the result.
     * @param queryObject the object to which the nearest neighbors are searched
     * @param minNN the minimal number of nearest neighbors to retrieve
     */
    @AbstractOperation.OperationConstructor({"Query object", "Minimum number of nearest objects"})
    public IncrementalNNQueryOperation(LocalAbstractObject queryObject, int minNN) {
        this(queryObject, minNN, AnswerType.NODATA_OBJECTS);
    }

    /**
     * Creates a new instance of IncrementalNNQueryOperation.
     * @param queryObject the object to which the nearest neighbors are searched
     * @param minNN the minimal number of nearest neighbors to retrieve
     * @param answerType the type of objects this operation stores in its answer
     */
    @AbstractOperation.OperationConstructor({"Query object", "Minimum number of nearest objects", "Answer type"})
    public IncrementalNNQueryOperation(LocalAbstractObject queryObject, int minNN, AnswerType answerType) {
        this.queryObject = queryObject;
        this.minNN = minNN;
        this.nnAddedToAnswer = 0;
    }


    //****************** Attribute access ******************//

    /**
     * Returns the kNN query object.
     * @return the kNN query object
     */
    public LocalAbstractObject getQueryObject() {
        return queryObject;
    }

    /**
     * Returns the minimum number of objects returned by this query.
     * @return the minimum number of objects returned by this query
     */
    public int getMinNN() {
        return minNN;
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


    //****************** End operation overrides ******************//

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

    
    //****************** Implementation of query evaluation ******************/
    
    /**
     * Evaluate this query on a given set of objects.
     * The objects found by this evaluation are added to answer of this query via {@link #addToAnswer}.
     * Note that the incremental kNN search can't use filtering, because the maximal radius is unknown
     *
     * @param objects the collection of objects on which to evaluate this query
     * @return number of objects satisfying the query
     */
    @Override
    public int evaluate(AbstractObjectIterator<? extends LocalAbstractObject> objects) {
        int beforeSize = getAnswerCount();

        // Iterate through all supplied objects
        while (objects.hasNext())
            addToAnswer(queryObject, objects.next(), getAnswerThreshold());

        return getAnswerCount() - beforeSize;
    }

    
    //****************** Overrides ******************//

    /**
     * Returns <tt>true</tt> if the minimum number of objects has been inserted to 
     * the answer during one evaluation of this operation.
     * @return <tt>true</tt> if the current answer has a minimal number of objects
     */
    public boolean isFilledEnough() {
        return nnAddedToAnswer >= minNN;
    }

    @Override
    public RankedAbstractObject addToAnswer(LocalAbstractObject queryObject, LocalAbstractObject object, float distThreshold) {
        RankedAbstractObject addedObject = super.addToAnswer(queryObject, object, distThreshold);
        if (addedObject != null)
            nnAddedToAnswer++;
        return addedObject;
    }

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


    //****************** Equality driven by object data ******************//

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
