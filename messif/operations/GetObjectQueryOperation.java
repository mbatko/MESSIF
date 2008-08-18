/*
 * GetObjectQueryOperation.java
 *
 * Created on 6. kveten 2004, 17:31
 */

package messif.operations;

import java.util.NoSuchElementException;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.LocalAbstractObject;
import messif.objects.UniqueID;


/**
 * Operation for retriving an instance of object having the desired ID (passed in constructor).
 *
 * @author xbatko
 */
@AbstractOperation.OperationName("Get object query")
public class GetObjectQueryOperation extends SingletonQueryOperation {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 2L;

    //****************** Attributes ******************//

    /** Identifier for which to retrieve object */
    protected final UniqueID objectID;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of GetObjectQueryOperation for the specified object ID.
     * @param objectID the object ID to search for
     */
    @AbstractOperation.OperationConstructor({"Object ID"})
    public GetObjectQueryOperation(UniqueID objectID) {
        super();
        this.objectID = objectID;
    }

    /**
     * Creates a new instance of GetObjectQueryOperation for the specified object ID.
     * @param answerType the type of objects this operation stores in its answer
     * @param objectID the object ID to search for
     */
    protected GetObjectQueryOperation(AnswerType answerType, UniqueID objectID) {
        super(answerType);
        this.objectID = objectID;
    }


    //****************** Parameter access methods ******************//

    /**
     * Returns the identifier for which to retrieve object.
     * @return the identifier for which to retrieve object
     */
    public UniqueID getObjectID() {
        return objectID;
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
        if (index != 0)
            throw new IndexOutOfBoundsException("GetObjectQueryOperation has only one argument");
        return objectID;
    }

    /**
     * Returns number of arguments that were passed while constructing this instance.
     * @return number of arguments that were passed while constructing this instance
     */
    @Override
    public int getArgumentCount() {
        return 1;
    }


    //****************** Default implementation of query evaluation ******************//

    /**
     * Evaluate this query on a given set of objects.
     * The objects found by this evaluation are added to answer of this query via {@link #addToAnswer}.
     *
     * @param objects the collection of objects on which to evaluate this query
     * @return number of objects satisfying the query
     */
    @Override
    public int evaluate(AbstractObjectIterator<? extends LocalAbstractObject> objects) {
        // Iterate through all supplied objects
        try {
            addToAnswer(objects.getObjectByID(objectID));
            return 1;
        } catch (NoSuchElementException e) {
            // If there was no object with the specified ID
            return 0;
        }
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
        // The argument obj is always GetObjectQueryOperation or its descendant, because it has only abstract ancestors
        return objectID.equals(((GetObjectQueryOperation)obj).objectID);
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return objectID.hashCode();
    }

}
