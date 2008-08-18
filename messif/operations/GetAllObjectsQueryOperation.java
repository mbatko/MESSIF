/*
 * GetAllObjectsQueryOperation.java
 *
 * Created on 6. kveten 2004, 17:31
 */

package messif.operations;

import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;


/**
 * Operation for retrieving all objects locally stored (organized by an algorithm).
 * Is is usually applied to centralized algorithms only.
 *
 * @author  xbatko
 */
@AbstractOperation.OperationName("Get all objects query")
public class GetAllObjectsQueryOperation extends ListingQueryOperation {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;


    /****************** Constructors ******************/

    /**
     * Creates a new instance of GetAllObjectsQuery.
     * Stored objects will be {@link messif.netbucket.RemoteAbstractObject}.
     */
    @AbstractOperation.OperationConstructor({})
    public GetAllObjectsQueryOperation() {
        super();
    }

    /**
     * Creates a new instance of GetAllObjectsQuery.
     * @param answerType the type of objects this operation stores in its answer
     */
    public GetAllObjectsQueryOperation(AnswerType answerType) {
        super(answerType);
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
        throw new IndexOutOfBoundsException("GetAllObjectsQueryOperation has no arguments");
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
     * Evaluate this query on a given set of objects.
     * The objects found by this evaluation are added to answer of this query via {@link #addToAnswer}.
     *
     * @param objects the collection of objects on which to evaluate this query
     * @return number of objects satisfying the query
     */
    @Override
    public int evaluate(AbstractObjectIterator<? extends LocalAbstractObject> objects) {
        int count = 0;
        while (objects.hasNext())
            if (addToAnswer(objects.next()))
                count++;
        return count;
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
