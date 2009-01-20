/*
 * SingletonQueryOperation.java
 *
 * Created on 6. kveten 2004, 17:31
 */

package messif.operations;

import messif.objects.AbstractObject;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 * The base class for query operations that return a single {@link AbstractObject object}.
 * These are, for example, operations that retrieve objects by ID or locator.
 *
 * @author xbatko
 */
public abstract class SingletonQueryOperation extends QueryOperation<AbstractObject> {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Answer of this query, will be <tt>null</tt> if the object cannot be found */
    private AbstractObject answer;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of SingletonQueryOperation.
     * The object stored in answer is {@link AnswerType#CLEARED_OBJECTS cleared and clonned}.
     */
    protected SingletonQueryOperation() {
        this(AnswerType.CLEARED_OBJECTS);
    }

    /**
     * Creates a new instance of ListingQueryOperation.
     * @param answerType the type of object this operation stores in its answer
     */
    protected SingletonQueryOperation(AnswerType answerType) {
        super(answerType);
    }


    //****************** Answer access methods ******************//

    /**
     * Returns the class of objects this operation stores in its answer.
     * @return the class of objects this operation stores in its answer
     */
    @Override
    public Class<? extends AbstractObject> getAnswerClass() {
        return AbstractObject.class;
    }

    /**
     * Returns the number of objects in this query answer.
     * For this operation, only 0 or 1 can be returned.
     * @return the number of objects in this query answer
     */
    @Override
    public int getAnswerCount() { 
        return (answer == null)?0:1;
    }
    
    /**
     * Returns an iterator over all objects in the answer to this query.
     * @return an iterator over all objects in the answer to this query
     */
    @Override
    public Iterator<AbstractObject> getAnswer() {
        List<AbstractObject> list;
        if (answer == null)
            list = Collections.emptyList();
        else
            list = Collections.singletonList(answer);
        return list.iterator();
    }

    @Override
    public Iterator<AbstractObject> getAnswerObjects() {
        return getAnswer();
    }

    /**
     * Returns an object that is the answer to this query.
     * @return an object that is the answer to this query
     */
    public AbstractObject getAnswerObject() {
        return answer;
    }


    //****************** Answer modification methods ******************//

    /**
     * Add an object to the answer.
     * The object is updated according to {@link #answerType}.
     * @param object the object to add
     * @return <code>true</code> if the <code>object</code> has been added to the answer. Otherwise <code>false</code>.
     * @throws IllegalArgumentException if the object cannot be added to the answer, e.g. because it cannot be clonned
     */
    public boolean addToAnswer(AbstractObject object) throws IllegalArgumentException {
        try {
            answer = answerType.update(object);
            return true;
        } catch (CloneNotSupportedException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Reset the current query answer.
     */
    @Override
    public void resetAnswer() {
        answer = null;
    }

    /**
     * Update the error code and answer of this operation from another operation.
     * Answer of this operation is updated only it is not set yet.
     * @param operation the source operation from which to get the update
     * @throws IllegalArgumentException if the answer of the specified operation is incompatible with this one
     */
    @Override
    public void updateFrom(AbstractOperation operation) throws IllegalArgumentException {
        super.updateFrom(operation);
        if (!(operation instanceof SingletonQueryOperation))
            throw new IllegalArgumentException(getClass().getSimpleName() + " cannot be updated from " + operation.getClass().getSimpleName());
        if (answer == null)
            addToAnswer(((SingletonQueryOperation)operation).answer);
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
        answer.clearSurplusData();
    }

}
