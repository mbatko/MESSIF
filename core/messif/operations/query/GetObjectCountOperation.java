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

import messif.buckets.BucketDispatcher;
import messif.buckets.LocalBucket;
import messif.operations.AbstractOperation;
import messif.operations.OperationErrorCode;


/**
 * Operation for retrieving the number of objects stored in indexing structure.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("Get object count query")
public class GetObjectCountOperation extends AbstractOperation {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /****************** Answer attributes ******************/

    /** The number of objects counted by this operation */
    protected int objectCount;


    /****************** Constructors ******************/

    /** Creates a new instance of GetAllObjectsQuery */
    @AbstractOperation.OperationConstructor({})
    public GetObjectCountOperation() {
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
        throw new IndexOutOfBoundsException("GetObjectCountOperation has no arguments");
    }

    /**
     * Returns number of arguments that were passed while constructing this instance.
     * @return number of arguments that were passed while constructing this instance
     */
    @Override
    public int getArgumentCount() {
        return 0;
    }


    /****************** Default implementation of query evaluation ******************/

    /**
     * Evaluate this query on a given bucket dispatcher.
     * Object counts stored in all buckets maintained by the provided dispatcher is added to this operation's answer.
     *
     * @param dispatcher the bucket dispatcher to update answer from
     * @return number of objects added to this operation, i.e. the actual dispatcher's object count
     */
    public int evaluate(BucketDispatcher dispatcher) {
        int count = dispatcher.getObjectCount();
        objectCount += count;
        return count;
    }

    /**
     * Evaluate this query on a given bucket.
     * Object count stored in this bucket is added to this operation's answer.
     *
     * @param bucket the bucket to update answer from
     * @return number of objects added to this operation, i.e. the actual bucket's object count
     */
    public int evaluate(LocalBucket bucket) {
        int count = bucket.getObjectCount();
        objectCount += count;
        return count;
    }


    /****************** Answer methods ******************/

    /**
     * Returns the number of objects counted by this operation.
     * @return the number of objects
     */
    public int getAnswerCount() { 
        return objectCount;
    }

    /**
     * Add the specified count to the answer of this operation.
     * @param objectCount the count to add
     */
    public void addToAnswer(int objectCount) { 
        this.objectCount += objectCount;
    }

    /**
     * Returns <tt>true</tt> if this operation has finished successfuly.
     * Otherwise, <tt>false</tt> is returned - the operation was either unsuccessful or is has not finished yet.
     *
     * @return <tt>true</tt> if this operation has finished successfuly
     */
    public boolean wasSuccessful() {
        return getErrorCode().equals(OperationErrorCode.RESPONSE_RETURNED);
    }


    /**
     * End operation successfully.
     */
    public void endOperation() {
        endOperation(OperationErrorCode.RESPONSE_RETURNED);
    }


    /****************** Answer methods ******************/

    /**
     * Prints out the object count in a fancy way.
     * @return object count in textual representation
     */
    @Override
    public String toString() {
        return "Get object count operation returned " + getAnswerCount() + " objects";
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
        // The argument obj is always GetObjectCountOperation or its descendant, because it has only abstract ancestors
        return true;
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return 0;
    }

}
