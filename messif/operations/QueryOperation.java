/*
 * QueryOperation.java
 *
 * Created on 22. cervenec 2004, 13:16
 */

package messif.operations;

import java.util.Iterator;
import messif.objects.AbstractObject;
import messif.objects.GenericObjectIterator;
import messif.objects.LocalAbstractObject;
import messif.objects.MeasuredAbstractObjectList;

/**
 * The base class for query operations.
 * Query operations retrive data from indexing structures according to their specification.
 * Thus, these operations have methods for checking their answer.
 * 
 * <p>
 * Note that query operations should not modify the indexed data.
 * </p>
 * 
 * @author  Vlastislav Dohnal, xdohnal@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
public abstract class QueryOperation extends AbstractOperation {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;
    
    /**
     * Returns <tt>true</tt> if this operation has finished successfuly.
     * Otherwise, <tt>false</tt> is returned - the operation was either unsuccessful or is has not finished yet.
     *
     * @return <tt>true</tt> if this operation has finished successfuly
     */
    public boolean wasSuccessful() {
        return errValue.equals(OperationErrorCode.RESPONSE_RETURNED);
    }

    /** End operation successfully */
    public void endOperation() {
        errValue = OperationErrorCode.RESPONSE_RETURNED;
    }
    
    
    /**
     * Evaluate this query on a given set of objects.
     * The objects found by this evaluation are added to answer of this query via {@link #addToAnswer}.
     *
     * @param objects the collection of objects on which to evaluate this query
     * @return number of objects satisfying the query
     */
    public abstract int evaluate(GenericObjectIterator<LocalAbstractObject> objects);
    

    /**
     * Returns the number of objects in this query answer.
     * @return the number of objects in this query answer
     */
    public abstract int getAnswerCount();
    
    /**
     * Returns an iterator over all objects in the answer to this query.
     * @return an iterator over all objects in the answer to this query
     */
    public abstract Iterator<AbstractObject> getAnswer();
    
    /**
     * Returns an iterator over pairs of objects and their distances from the query object of this query. 
     * The object of a pair is accessible through {@link messif.objects.MeasuredAbstractObjectList.Pair#getObject}.
     * The associated distance of a pair is accessible through {@link messif.objects.MeasuredAbstractObjectList.Pair#getDistance}.
     * 
     * @return an iterator over pairs of objects and their distances from the query object of this query
     */
    public abstract Iterator<MeasuredAbstractObjectList.Pair<AbstractObject>> getAnswerDistances();
    
    /**
     * Add an object with a measured distance to the answer.
     * 
     * @param object the object to add
     * @param distance the distance of the object
     * @return <code>true</code> if the <code>object</code> has been added to the answer. Otherwise <code>false</code>.
     */
    public abstract boolean addToAnswer(AbstractObject object, float distance);

    /**
     * Add all objects with distances from the passed iterator to the answer of this operation.
     *
     * @param iterator iterator over object-distance pairs that should be added to this operation's answer
     * @return <code>true</code> if at least one object has been added to the answer. Otherwise <code>false</code>.
     */
    public boolean addToAnswer(Iterator<MeasuredAbstractObjectList.Pair<AbstractObject>> iterator) {
        boolean retVal = false;
        while (iterator.hasNext()) {
            MeasuredAbstractObjectList.Pair<AbstractObject> pair = iterator.next();
            if (addToAnswer(pair.getObject(), pair.getDistance()))
                retVal = true;
        }
        return retVal;
    }

    
    /**
     * Reset the current query answer.
     */
    public abstract void resetAnswer();

    /**
     * Update all answer data of this operation from another operation.
     * This method is used to merge answers from multiple operations into one.
     *
     * @param operation the operation to update answer from
     */
    @Override
    public void updateAnswer(AbstractOperation operation) {
        super.updateAnswer(operation);
        if (operation instanceof QueryOperation)
            updateAnswer((QueryOperation)operation);
    }

    /**
     * Update query answer data of this operation from another query operation.
     * This method should not be used directly, use {@link AbstractOperation#updateAnswer} instead.
     *
     * @param operation the operation to update answer from
     */
    protected void updateAnswer(QueryOperation operation) {
        // Updates of query operations
        addToAnswer(operation.getAnswerDistances());
    }

}
