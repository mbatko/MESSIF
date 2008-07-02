/*
 * GetObjectQueryOperation.java
 *
 * Created on 6. kveten 2004, 17:31
 */

package messif.operations;

import messif.objects.AbstractObject;
import messif.objects.util.MeasuredAbstractObjectList;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.LocalAbstractObject;
import messif.objects.MeasuredAbstractObject;
import messif.objects.UniqueID;


/**
 * Operation for retriving an instance of object having the desired ID (passed in constructor).
 *
 * @author  Michal Batko, xbatko@fi.muni.cz
 */
@AbstractOperation.OperationName("Get object query")
public class GetObjectQueryOperation extends QueryOperation {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /****************** Query request attributes ******************/
    
    /** Object ID (accessible directly) */
    public final UniqueID objectID;
    
    
    /****************** Query answer attributes ******************/

    /** The object that is the answer to this query */
    protected AbstractObject answer = null;
     
    
    /****************** Constructors ******************/

    /**
     * Creates a new instance of GetObjectQueryOperation for the specified object ID.
     * @param objectID the object ID to search for
     */
    @AbstractOperation.OperationConstructor({"Object ID"})
    public GetObjectQueryOperation(UniqueID objectID) {
        this.objectID = objectID;
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


    /****************** Default implementation of query evaluation ******************/
    
    /**
     * Evaluate this query on a given set of objects.
     * The objects found by this evaluation are added to answer of this query via {@link #addToAnswer}.
     *
     * @param objects the collection of objects on which to evaluate this query
     * @return number of objects satisfying the query
     */
    public int evaluate(AbstractObjectIterator<LocalAbstractObject> objects) {
        // Iterate through all supplied objects
        try {
            addToAnswer(objects.getObjectByID(objectID), LocalAbstractObject.UNKNOWN_DISTANCE);
            return 1;
        } catch (NoSuchElementException e) {
            // If there was no object with the specified ID
            return 0;
        }
    }

    
    /****************** Answer methods ******************/

    /**
     * Returns the number of objects in this query answer.
     * For this operation, only 0 or 1 can be returned.
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
    public Iterator<MeasuredAbstractObject<?>> getAnswerDistances() {
        MeasuredAbstractObjectList<AbstractObject> list = new MeasuredAbstractObjectList<AbstractObject>();
        list.add(answer, LocalAbstractObject.UNKNOWN_DISTANCE);
        return list.iterator();
    }

    /**
     * Returns an object that is the answer to this query.
     * @return an object that is the answer to this query
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

    /**
     * Returns a string representation of this operation.
     * @return a string representation of this operation.
     */
    @Override
    public String toString() {
        return new StringBuffer("Get object query <").append(objectID).append("> returned ").append(getAnswerCount()).append(" objects").toString();
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
