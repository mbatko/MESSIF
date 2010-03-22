/*
 * ListingQueryOperation.java
 *
 * Created on 3. cervenec 2008, 13:16
 */

package messif.operations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import messif.objects.AbstractObject;
import messif.objects.util.RankedAbstractObject;


/**
 * The base class for query operations that return unsorted collections of {@link AbstractObject objects}.
 * These are, for example, operations that access objects from a bucket.
 * 
 * @author xbatko
 */
public abstract class ListingQueryOperation extends QueryOperation<AbstractObject> {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** List holding the answer of this query */
    private List<AbstractObject> answer;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of ListingQueryOperation.
     * An instance of {@link ArrayList} is used to store the answer.
     * Objects added to answer are {@link AnswerType#CLEARED_OBJECTS cleared and clonned}.
     */
    protected ListingQueryOperation() {
        this(AnswerType.CLEARED_OBJECTS, new ArrayList<AbstractObject>());
    }

    /**
     * Creates a new instance of ListingQueryOperation.
     * An instance of {@link ArrayList} is used to store the answer.
     * @param answerType the type of objects this operation stores in its answer
     */
    protected ListingQueryOperation(AnswerType answerType) {
        this(answerType, new ArrayList<AbstractObject>());
    }

    /**
     * Creates a new instance of ListingQueryOperation.
     * @param answerType the type of objects this operation stores in its answer
     * @param answer the collection used for storing the answer
     */
    protected ListingQueryOperation(AnswerType answerType, List<AbstractObject> answer) {
        super(answerType);
        this.answer = answer;
    }


    //****************** Clonning ******************//
    
    /**
     * Create a duplicate of this operation.
     * The answer of the query is not clonned.
     *
     * @return a clone of this operation
     * @throws CloneNotSupportedException if the operation instance cannot be cloned
     */
    @Override
    public ListingQueryOperation clone() throws CloneNotSupportedException {
        ListingQueryOperation operation = (ListingQueryOperation)super.clone();
        operation.answer = new ArrayList<AbstractObject>();
        return operation;
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
     * @return the number of objects in this query answer
     */
    @Override
    public int getAnswerCount() {
        return answer.size();
    }
    
    /**
     * Returns an iterator over all objects in the answer to this query.
     * @return an iterator over all objects in the answer to this query
     */
    @Override
    public Iterator<AbstractObject> getAnswer() {
        return answer.iterator();
    }

    @Override
    public Iterator<AbstractObject> getAnswer(int skip, final int count) {
        final ListIterator<AbstractObject> listIterator = answer.listIterator(skip);
        return new Iterator<AbstractObject>() {
            private int returnedCount = 0;
            public boolean hasNext() {
                return returnedCount < count && listIterator.hasNext();
            }
            public AbstractObject next() {
                AbstractObject ret = listIterator.next();
                returnedCount++;
                return ret;
            }
            public void remove() {
                throw new UnsupportedOperationException("Not supported");
            }
        };
    }

    @Override
    public Iterator<AbstractObject> getAnswerObjects() {
        return getAnswer();
    }


    //****************** Answer manipulation methods ******************//

    /**
     * Add an object to the answer.
     * The object is updated according to {@link #answerType}.
     * @param object the object to add
     * @return <code>true</code> if the <code>object</code> has been added to the answer. Otherwise <code>false</code>.
     * @throws IllegalArgumentException if the object cannot be added to the answer, e.g. because it cannot be clonned
     */
    public boolean addToAnswer(AbstractObject object) throws IllegalArgumentException {
        try {
            if (object == null)
                return false;
            return answer.add(answerType.update(object));
        } catch (CloneNotSupportedException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Reset the current query answer.
     * All objects from the answer are deleted, {@link #getAnswerCount()} will return zero.
     */
    @Override
    public void resetAnswer() {
        answer.clear();
    }

    /**
     * Update the error code and answer of this operation from another operation.
     * @param operation the source operation from which to get the update
     * @throws IllegalArgumentException if the answer of the specified operation is incompatible with this one
     */
    @Override
    public final void updateFrom(AbstractOperation operation) throws IllegalArgumentException {
        super.updateFrom(operation);
        if (operation instanceof SingletonQueryOperation)
            updateFrom((SingletonQueryOperation)operation);
        else if (operation instanceof ListingQueryOperation)
            updateFrom((ListingQueryOperation)operation);
        else if (operation instanceof RankingQueryOperation)
            updateFrom((RankingQueryOperation)operation);
        else
            throw new IllegalArgumentException(getClass().getSimpleName() + " cannot be updated from " + operation.getClass().getSimpleName());
    }

    /**
     * Update the answer of this operation from a {@link SingletonQueryOperation}.
     * @param operation the source operation from which to get the update
     */
    protected void updateFrom(SingletonQueryOperation operation) {
        addToAnswer(operation.getAnswerObject());
    }

    /**
     * Update the answer of this operation from a {@link ListingQueryOperation}.
     * @param operation the source operation from which to get the update
     */
    protected void updateFrom(ListingQueryOperation operation) {
        for (AbstractObject object : operation.answer)
            addToAnswer(object);
    }

    /**
     * Update the answer of this operation from a {@link RankingQueryOperation}.
     * @param operation the source operation from which to get the update
     */
    protected void updateFrom(RankingQueryOperation operation) {
        Iterator<? extends RankedAbstractObject> iter = operation.getAnswer();
        while (iter.hasNext())
            addToAnswer(iter.next().getObject());
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
        for (AbstractObject obj : answer)
            obj.clearSurplusData();
    }

}
