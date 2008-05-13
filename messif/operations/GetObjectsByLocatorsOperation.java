/*
 * GetObjectByLocator.java
 *
 * Created on 18.7.2007, 17:13:03
 */

package messif.operations;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import messif.objects.AbstractObject;
import messif.objects.GenericObjectIterator;
import messif.objects.LocalAbstractObject;
import messif.objects.MeasuredAbstractObjectList;

/**
 * This operation returns objects with given locators.
 * 
  * @author <a href="mailto:xnovak8@fi.muni.cz">xnovak8@fi.muni.cz</a> David Novak, Faculty of Informatics, Masaryk University, Brno, Czech Republic
*/
@AbstractOperation.OperationName("Get objects by locators")
public class GetObjectsByLocatorsOperation extends QueryOperation {
    
    /** Class serial id for serialization */
    private static final long serialVersionUID = 2L;    

    
    /****************** Query request attributes ******************/

    /** The locators of the desired objects */
    public final Set<String> locators;

    /** The object to compute distances to; if <tt>null</tt>, UNKNOWN_DISTANCE will be used in answer */
    public final LocalAbstractObject queryObjectForDistances;

    /** Flag whether to have full objects in the answer or only the RemoteAbstractObjects */
    public final boolean requireFullObjects;


    /****************** Query answer attributes ******************/

    /** The list of objects forming the answer of this query */
    protected final MeasuredAbstractObjectList<AbstractObject> answer;


    /****************** Constructors ******************/

    /**
     * Create a new instance of GetObjectsByLocatorsOperation with the specified locators.
     * 
     * @param locators the collection of locators to be found
     * @param queryObjectForDistances the query object to use for computing distances
     * @param requireFullObjects flag whether to have full objects in the answer or only the RemoteAbstractObjects
     * @param maxAnswerCount the limit for the number of objects kept in this operation's answer
     */
    @AbstractOperation.OperationConstructor({"The collection of locators", "The object to compute answer distances to", "Are full objects required", "Limit for number of objects in answer"})
    public GetObjectsByLocatorsOperation(Collection<String> locators, LocalAbstractObject queryObjectForDistances, boolean requireFullObjects, int maxAnswerCount) {
        this.locators = (locators == null)?new HashSet<String>():new HashSet<String>(locators);
        this.queryObjectForDistances = queryObjectForDistances;
        this.requireFullObjects = requireFullObjects;
        this.answer = new MeasuredAbstractObjectList<AbstractObject>(maxAnswerCount);
    }

    /**
     * Create a new instance of GetObjectsByLocatorsOperation with the specified locators.
     * 
     * @param locators the collection of locators to be found
     * @param queryObjectForDistances the query object to use for computing distances
     * @param requireFullObjects flag whether to have full objects in the answer or only the RemoteAbstractObjects
     */
    @AbstractOperation.OperationConstructor({"The collection of locators", "The object to compute answer distances to", "Are full objects required"})
    public GetObjectsByLocatorsOperation(Collection<String> locators, LocalAbstractObject queryObjectForDistances, boolean requireFullObjects) {
        this(locators, queryObjectForDistances, requireFullObjects, Integer.MAX_VALUE);
    }

    /**
     * Create a new instance of GetObjectsByLocatorsOperation with the specified locators.
     * @param locators the collection of locators to search for
     */
    @AbstractOperation.OperationConstructor({"The collection of locators"})
    public GetObjectsByLocatorsOperation(Collection<String> locators) {
        this(locators, null, true);
    }

    /** Create a new instance of GetObjectsByLocatorsOperation with empty locators set. */
    public GetObjectsByLocatorsOperation() {
        this(null);
    }

    /**
     * Create a new instance of GetObjectsByLocatorsOperation with empty locators set.
     * 
     * @param queryObjectForDistances the query object to use for computing distances
     * @param requireFullObjects flag whether to have full objects in the answer or only the RemoteAbstractObjects
     */
    public GetObjectsByLocatorsOperation(LocalAbstractObject queryObjectForDistances, boolean requireFullObjects, int maxAnswerCount) {
        this(null, queryObjectForDistances, requireFullObjects, maxAnswerCount);
    }

    /**
     * Create a new instance of GetObjectsByLocatorsOperation with empty locators set.
     * 
     * @param queryObjectForDistances the query object to use for computing distances
     * @param requireFullObjects flag whether to have full objects in the answer or only the RemoteAbstractObjects
     */
    public GetObjectsByLocatorsOperation(LocalAbstractObject queryObjectForDistances, boolean requireFullObjects) {
        this(queryObjectForDistances, requireFullObjects, Integer.MAX_VALUE);
    }


    /****************** Management of the set of locators ********************/
    
    /**
     * Add a locator to this query.
     * @param locator the locator to be added
     */
    public void addLocator(String locator) {
        locators.add(locator);
    }

    /**
     * Replace the current locators of this query with the provided collection.
     * @param locators the new collection of locators
     */
    public void setLocators(Collection<String> locators) {
        this.locators.clear();
        if (locators != null)
            this.locators.addAll(locators);
    }
    
    /**
     * Check whether the set of locators contains given locator.
     * 
     * @param locator the locator to be checked 
     * @return <code>true</code> if the set of locators to be found contains the given <code>locator</code>
     */
    public boolean hasLocator(String locator) {
        return locators.contains(locator);
    }
    
    /****************** Implementation of query evaluation ******************/
    
    /**
     * Evaluate this query on a given set of objects.
     * The objects found by this evaluation are added to answer of this query via {@link #addToAnswer}.
     *
     * @param objects the collection of objects on which to evaluate this query
     * @return number of objects satisfying the query
     */
    public int evaluate(GenericObjectIterator<LocalAbstractObject> objects) {
        int count = 0;
        try {
            while (!locators.isEmpty()) {
                LocalAbstractObject object = objects.getObjectByAnyLocator(locators, true);
                addToAnswer(object, LocalAbstractObject.UNKNOWN_DISTANCE);
                count++;
            }
        } catch (NoSuchElementException e) { }
        
        return count;
    }

    /**
     * Returns the number of objects in this query answer.
     * @return the number of objects in this query answer
     */
    public int getAnswerCount() {
        return answer.size();
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
     * The distances are computed if the constructor parameter <code>queryObjectForDistances</code>
     * was not <tt>null</tt>. Otherwise, the distances returned are always zero.
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
        if ((queryObjectForDistances != null) && (distance == LocalAbstractObject.UNKNOWN_DISTANCE))
            distance = queryObjectForDistances.getDistance(object.getLocalAbstractObject());

        return answer.add( ((requireFullObjects) ? object : object.getRemoteAbstractObject()), distance);
    }

    /**
     * Reset the current query answer.
     */
    @Override
    public void resetAnswer() {
        answer.clear();
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
        return new StringBuffer("Get object by locator query ").append(locators.toString()).append(" returned ").append(getAnswerCount()).append(" objects").toString();
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
        
        // Clear query object if there is one
        if (queryObjectForDistances != null)
            queryObjectForDistances.clearSurplusData();

        // Clear the answered objects if they are not just RemoteAbstractObjects
        if (requireFullObjects)
            for (MeasuredAbstractObjectList.Pair<AbstractObject> pair : answer)
                pair.getObject().clearSurplusData();
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
        // The argument obj is always DeleteOperation or its descendant, because it has only abstract ancestors
        return locators.equals(((GetObjectsByLocatorsOperation)obj).locators);
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return locators.hashCode();
    }

}
