/*
 * kNNQueryOperation.java
 *
 * Created on 6. kveten 2004, 17:31
 */

package messif.operations;

import java.util.Iterator;
import messif.netbucket.RemoteAbstractObject;
import messif.objects.AbstractObject;
import messif.objects.GenericObjectIterator;
import messif.objects.LocalAbstractObject;
import messif.objects.MeasuredAbstractObjectList;

/**
 * K-nearest neighbors query operation.
 * Retrieves <code>k</code> objects that are nearest to the specified query object
 * (according to the distance measure).
 * 
 * @author  Vlastislav Dohnal, xdohnal@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
@AbstractOperation.OperationName("k-nearest neighbors query")
public class kNNQueryOperation extends QueryOperation {
    
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    /****************** Query request attributes ******************/
    
    /** kNN query object (accessible directly) */
    public final LocalAbstractObject queryObject;
    /** kNN query number of nearest objects to retrieve (accessible directly) */
    public final int k;
    
    
    /****************** Query answer attributes ******************/
 
    /** The list of answer objects */
    protected final MeasuredAbstractObjectList<AbstractObject> answer;
     
    
    /****************** Constructors ******************/

    /**
     * Creates a new instance of kNNQueryOperation.
     * @param queryObject the object to which the nearest neighbors are searched
     * @param k the number of nearest neighbors to retrieve
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects"})
    public kNNQueryOperation(LocalAbstractObject queryObject, int k) {
        this.queryObject = queryObject;
        this.k = k;
        this.answer = new MeasuredAbstractObjectList<AbstractObject>(k);
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
        switch (index) {
        case 0:
            return queryObject;
        case 1:
            return k;
        default:
            throw new IndexOutOfBoundsException("kNNQueryOperation has only two arguments");
        }
    }

    /**
     * Returns number of arguments that were passed while constructing this instance.
     * @return number of arguments that were passed while constructing this instance
     */
    @Override
    public int getArgumentCount() {
        return 2;
    }


    /****************** Default implementation of query evaluation ******************/
    
    /** @return all objects tested, whiche were not filtered out */
    public int evaluate(GenericObjectIterator<LocalAbstractObject> objects) {
        int beforeCount = getAnswerCount();
        
        // Iterate through all supplied objects
        while (objects.hasNext()) {
            // Get current object
            LocalAbstractObject object = objects.next();
            
            if (object.excludeUsingPrecompDist(queryObject, getRadius()))
                continue;
            
            // Get distance to query object (the second parameter defines a stop condition in getDistance() 
            // which stops further computations if the distance will be greater than this value).
            float distance = queryObject.getDistance(object, getRadius());

            addToAnswer(object, distance);
        }

        return getAnswerCount() - beforeCount;
    }
    

    /****************** Answer methods ******************/
    
    /** Get radius of this kNN query.
     *
     * @return Returns the distance to the k-th nearest object in the answer list. 
     *         If there are fewer objects, LocalAbstractObject.MAX_DISTANCE is returned.
     */
    public float getRadius() {
        if (answer.size() < k) return LocalAbstractObject.MAX_DISTANCE;
        return answer.getLastDistance();
    }

    /** Returns the number of answered objects */
    public int getAnswerCount() {
        return answer.size();
    }

    /** Returns an iterator over all objects in the answer to this query. */
    public Iterator<AbstractObject> getAnswer() { 
        return answer.objects();
    }
    
    /** Returns an iterator over pairs of objects and their distances from the query object of this query. 
     *  The object of a pair is accessible through getObject().
     *  The associated distance of a pair is accessible through getDistance().
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
        return answer.add(object.getRemoteAbstractObject(), distance);
    }
    
    /**
     * Add all objects with distances from the passed iterator to the answer of this operation.
     *
     * @param iterator iterator over object-distance pairs that should be added to this operation's answer
     * @return <code>true</code> if at least one object has been added to the answer. Otherwise <code>false</code>.
     */
    @Override
    public boolean addToAnswer(Iterator<MeasuredAbstractObjectList.Pair<AbstractObject>> iterator) { 
        boolean retVal = false;
        while (iterator.hasNext()) {
            MeasuredAbstractObjectList.Pair<AbstractObject> pair = iterator.next();
            if (RemoteAbstractObject.class.isInstance(pair.getObject())) {
                if (answer.add(pair))
                    retVal = true;
            } else {
                if (answer.add(pair.getObject().getRemoteAbstractObject(), pair.getDistance()))
                    retVal = true;
            }
        }
        return retVal;
    }

    /**
     * Reset the current query answer.
     */
    @Override
    public void resetAnswer() {
        answer.clear();
    }

    /**
     * Returns the information about this operation.
     * @return the information about this operation
     */
    @Override
    public String toString() {
        return new StringBuffer("kNN query <").append(queryObject).append(',').append(k).append("> returned "
                ).append(getAnswerCount()).append(" objects (max distance is ").append(getRadius()).append(")").toString();
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
        queryObject.clearSurplusData();
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
        // The argument obj is always kNNQueryOperation or its descendant, because it has only abstract ancestors
        kNNQueryOperation castObj = (kNNQueryOperation)obj;

        if (!queryObject.dataEquals(castObj.queryObject))
            return false;

        return k == castObj.k;
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return (queryObject.dataHashCode() << 8) + k;
    }

}
