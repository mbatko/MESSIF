/*
 * GetObjectByLocator.java
 *
 * Created on 18.7.2007, 17:13:03
 */

package messif.operations;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import messif.objects.AbstractObject;
import messif.objects.GenericObjectIterator;
import messif.objects.LocalAbstractObject;
import messif.objects.MeasuredAbstractObjectList;

/**
 * This query retrieves from the structure a set of objects given their locators.
 * 
 * @author <a href="mailto:xnovak8@fi.muni.cz">xnovak8@fi.muni.cz</a> David Novak, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
@AbstractOperation.OperationName("Get object by locator")
public class GetObjectByLocatorOperation extends QueryOperation {
    
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;    

    
    /****************** Query request attributes ******************/

    /** The locator of the desired object */
    public final String locator;

    
    /****************** Query answer attributes ******************/
    
    /** The answered object */
    protected AbstractObject answer = null;

    
    /****************** Constructors ******************/
    
    /**
     * Create a new instance of GetObjectByLocatorOperation for a specified locator.
     * @param locator the locator to be searched by this operation
     */
    @AbstractOperation.OperationConstructor({"The object locator"})
    public GetObjectByLocatorOperation(String locator) {
        this.locator = locator;
    }

    
    /****************** Implementation of query evaluation ******************/
    
    /**
     * Evaluate this query on a given set of objects.
     * @param objects set of objects to evaluate the operation on
     * @return number of objects satisfying the query (should be zero or one object if the locator is unique)
     */    
    public int evaluate(GenericObjectIterator<LocalAbstractObject> objects) {
        // Iterate through all supplied objects
        try {
            addToAnswer(objects.getObjectByLocator(locator), LocalAbstractObject.UNKNOWN_DISTANCE);
            return 1;
        } catch (NoSuchElementException e) {
            // If there was no object with the specified locator
            return 0;
        }
    }

    /**
     * Returns the number of objects in this query answer.
     * For this operation, only 0 or 1 can be returned.
     * 
     * @return the number of objects in this query answer
     */
    public int getAnswerCount() {
        return (answer == null)?0:1;
    }

    /**
     * Returns an iterator over all objects in the answer to this query.
     * @return an iterator over all objects in the answer to this query
     */
    public Iterator<AbstractObject> getAnswer() {
        return Collections.singletonList(answer).iterator();
    }

    /**
     * Returns an iterator over pairs of objects and their distances from the query object of this query. 
     * The object of a pair is accessible through {@link messif.objects.MeasuredAbstractObjectList.Pair#getObject}.
     * The associated distance of a pair is accessible through {@link messif.objects.MeasuredAbstractObjectList.Pair#getDistance}.
     * 
     * @return an iterator over pairs of objects and their distances from the query object of this query
     */
    public Iterator<MeasuredAbstractObjectList.Pair<AbstractObject>> getAnswerDistances() {
        MeasuredAbstractObjectList<AbstractObject> list = new MeasuredAbstractObjectList<AbstractObject>();
        list.add(answer, LocalAbstractObject.UNKNOWN_DISTANCE);
        return list.iterator();
    }

    /**
     * Returns the object that is the answer to this query. 
     * @return the object that is the answer to this query
     */
    public AbstractObject getAnswerObject() {
        return answer;
    }

    /**
     * Add an object with a measured distance to the answer.
     * 
     * @param object the object to add
     * @param distance the distance of the object
     * @return <code>true</code> if the <code>object</code> has been added to the answer. Otherwise <code>false</code>.
     */
    public boolean addToAnswer(AbstractObject object, float distance) { 
        if (answer != object) {
            answer = object;
            return true;
        } else
            return false;
    }

    /**
     * Reset the current query answer.
     */
    @Override
    public void resetAnswer() {
        answer = null;
    }

    
    /*********************  The operation error code management   ***************/
    
    /**
     * Returns <tt>true</tt> if this operation has finished successfuly.
     * Otherwise, <tt>false</tt> is returned - the operation was either unsuccessful or is has not finished yet.
     *
     * @return <tt>true</tt> if this operation has finished successfuly
     */
    @Override
    public boolean wasSuccessful() {
        return this.errValue.equals(OperationErrorCode.RESPONSE_RETURNED);
    }

    /** End operation successfully */
    @Override
    public void endOperation() {
        this.errValue.equals(OperationErrorCode.RESPONSE_RETURNED);
    }
    
    /**
     * Returns a string representation of this operation.
     * @return a string representation of this operation.
     */
    @Override
    public String toString() {
        return new StringBuffer("Get object by locator query <").append(locator).append("> returned object: ").append(answer).toString();
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
        answer.clearSurplusData();
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
        // The argument obj is always GetObjectByLocatorOperation or its descendant, because it has only abstract ancestors
        return locator.equals(((GetObjectByLocatorOperation)obj).locator);
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return locator.hashCode();
    }

}
