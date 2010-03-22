package messif.operations.query;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import messif.objects.LocalAbstractObject;
import messif.objects.util.RankedAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;
import messif.operations.RankingQueryOperation;

/**
 * This operation returns objects with given locators.
 * 
  * @author <a href="mailto:xnovak8@fi.muni.cz">xnovak8@fi.muni.cz</a> David Novak, Faculty of Informatics, Masaryk University, Brno, Czech Republic
*/
@AbstractOperation.OperationName("Get objects by locators")
public class GetObjectsByLocatorsOperation extends RankingQueryOperation {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 3L;

    //****************** Attributes ******************//

    /** The locators of the desired objects */
    protected final Set<String> locators;

    /** The object to compute distances to; if <tt>null</tt>, UNKNOWN_DISTANCE will be used in answer */
    protected final LocalAbstractObject queryObjectForDistances;


    //****************** Constructors ******************//

    /**
     * Create a new instance of GetObjectsByLocatorsOperation with the specified locators.
     * 
     * @param locators the collection of locators to be found
     * @param queryObjectForDistances the query object to use for computing distances
     * @param answerType the type of objects this operation stores in its answer
     * @param maxAnswerSize the limit for the number of objects kept in this operation's answer
     */
    @AbstractOperation.OperationConstructor({"The collection of locators", "The object to compute answer distances to", "Answer type", "Limit for number of objects in answer"})
    public GetObjectsByLocatorsOperation(Collection<String> locators, LocalAbstractObject queryObjectForDistances, AnswerType answerType, int maxAnswerSize) {
        super(answerType, maxAnswerSize);
        this.locators = (locators == null)?new HashSet<String>():new HashSet<String>(locators);
        this.queryObjectForDistances = queryObjectForDistances;
    }

    /**
     * Create a new instance of GetObjectsByLocatorsOperation with the specified locators.
     * 
     * @param locators the collection of locators to be found
     * @param queryObjectForDistances the query object to use for computing distances
     * @param answerType the type of objects this operation stores in its answer
     */
    @AbstractOperation.OperationConstructor({"The collection of locators", "The object to compute answer distances to", "Answer type"})
    public GetObjectsByLocatorsOperation(Collection<String> locators, LocalAbstractObject queryObjectForDistances, AnswerType answerType) {
        this(locators, queryObjectForDistances, answerType, Integer.MAX_VALUE);
    }

    /**
     * Create a new instance of GetObjectsByLocatorsOperation with the specified locators.
     * 
     * @param locators the collection of locators to be found
     * @param queryObjectForDistances the query object to use for computing distances
     */
    @AbstractOperation.OperationConstructor({"The collection of locators", "The object to compute answer distances to"})
    public GetObjectsByLocatorsOperation(Collection<String> locators, LocalAbstractObject queryObjectForDistances) {
        this(locators, queryObjectForDistances, AnswerType.REMOTE_OBJECTS, Integer.MAX_VALUE);
    }

    /**
     * Create a new instance of GetObjectsByLocatorsOperation with the specified locators.
     * @param locators the collection of locators to search for
     */
    @AbstractOperation.OperationConstructor({"The collection of locators"})
    public GetObjectsByLocatorsOperation(Collection<String> locators) {
        this(locators, null);
    }

    /**
     * Create a new instance of GetObjectsByLocatorsOperation with an empty locators set.
     */
    public GetObjectsByLocatorsOperation() {
        this(null);
    }

    /**
     * Create a new instance of GetObjectsByLocatorsOperation with empty locators set.
     * 
     * @param queryObjectForDistances the query object to use for computing distances
     * @param answerType the type of objects this operation stores in its answer
     * @param maxAnswerCount the limit for the number of objects kept in this operation's answer
     */
    public GetObjectsByLocatorsOperation(LocalAbstractObject queryObjectForDistances, AnswerType answerType, int maxAnswerCount) {
        this(null, queryObjectForDistances, answerType, maxAnswerCount);
    }

    /**
     * Create a new instance of GetObjectsByLocatorsOperation with empty locators set.
     * 
     * @param queryObjectForDistances the query object to use for computing distances
     * @param answerType the type of objects this operation stores in its answer
     */
    public GetObjectsByLocatorsOperation(LocalAbstractObject queryObjectForDistances, AnswerType answerType) {
        this(queryObjectForDistances, answerType, Integer.MAX_VALUE);
    }

    /**
     * Create a new instance of GetObjectsByLocatorsOperation with empty locators set.
     * 
     * @param queryObjectForDistances the query object to use for computing distances
     * @param maxAnswerCount the limit for the number of objects kept in this operation's answer
     */
    public GetObjectsByLocatorsOperation(LocalAbstractObject queryObjectForDistances, int maxAnswerCount) {
        this(null, queryObjectForDistances, AnswerType.REMOTE_OBJECTS, maxAnswerCount);
    }


    //****************** Attribute access ******************//

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
            return locators;
        case 1:
            return queryObjectForDistances;
        default:
            throw new IndexOutOfBoundsException("GetObjectsByLocatorsOperation has only one argument");
        }
    }

    /**
     * Returns number of arguments that were passed while constructing this instance.
     * @return number of arguments that were passed while constructing this instance
     */
    @Override
    public int getArgumentCount() {
        return queryObjectForDistances == null ? 1 : 2;
    }

    /**
     * Returns the object the distance to which is used for the answer rank.
     * @return the query object of this ranking query
     */
    public LocalAbstractObject getQueryObject() {
        return queryObjectForDistances;
    }

    /**
     * Returns the object locators this query searches for.
     * @return the object locators this query searches for
     */
    public Set<String> getLocators() {
        return Collections.unmodifiableSet(locators);
    }

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


    //****************** Implementation of query evaluation ******************//
    
    /**
     * Evaluate this query on a given set of objects.
     * The objects found by this evaluation are added to answer of this query via {@link #addToAnswer}.
     *
     * @param objects the collection of objects on which to evaluate this query
     * @return number of objects satisfying the query
     * @throws IllegalArgumentException if the object cannot be added to the answer, e.g. because it cannot be clonned
     */
    @Override
    public int evaluate(AbstractObjectIterator<? extends LocalAbstractObject> objects) {
        int count = 0;
        try {
            while (!locators.isEmpty()) {
                LocalAbstractObject object = objects.getObjectByAnyLocator(locators, true);
                if (queryObjectForDistances != null)
                    addToAnswer(queryObjectForDistances, object, LocalAbstractObject.MAX_DISTANCE);
                else
                    addToAnswer(object, LocalAbstractObject.UNKNOWN_DISTANCE, null);
                count++;
            }
        } catch (NoSuchElementException e) { // Search ended, there are no more objects in the bucket
        }

        return count;
    }


    //****************** Overrides for answer set ******************//

    /**
     * Returns the class of objects this operation stores in its answer.
     * @return the class of objects this operation stores in its answer
     */
    @Override
    public Class<? extends RankedAbstractObject> getAnswerClass() {
        return RankedAbstractObject.class;
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

        // Clear query object if there is one
        if (queryObjectForDistances != null)
            queryObjectForDistances.clearSurplusData();
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
