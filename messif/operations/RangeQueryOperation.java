/*
 * RangeQueryOperation.java
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
 * Range query operation.
 * Retrieves all objects that have their distances to the specified query object
 * less than or equal to the specified radius.
 * 
 * @author  Vlastislav Dohnal, xdohnal@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
@AbstractOperation.OperationName("Range query")
public class RangeQueryOperation extends QueryOperation {
    
    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;
    
    /****************** Query request attributes ******************/
    
    /** Range query object (accessible directly) */
    public final LocalAbstractObject queryObject;
    /** Range query radius (accessible directly) */
    public final float radius;
    
    
    /****************** Query answer attributes ******************/

    /** The list of answer objects */
    protected MeasuredAbstractObjectList<AbstractObject> answer;
     
    
    /****************** Constructors ******************/

    /**
     * Creates a new instance of RangeQueryOperation given the query object and radius.
     * @param queryObject the query object
     * @param radius the query radius
     */
    @AbstractOperation.OperationConstructor({"Query object", "Query radius"})
    public RangeQueryOperation(LocalAbstractObject queryObject, float radius) {
        this(queryObject, radius, Integer.MAX_VALUE);
    }

    /**
     * Creates a new instance of RangeQueryOperation given the query object, radius and maximal number of objects to return.
     * @param queryObject the query object
     * @param radius the query radius
     * @param k max number of objects to carry (default is Integer.MAX_VALUE)
     */
    public RangeQueryOperation(LocalAbstractObject queryObject, float radius, int k) {
        this.queryObject = queryObject;
        this.radius = radius;
        if (k == Integer.MAX_VALUE)
            this.answer = new MeasuredAbstractObjectList<AbstractObject>();
        else this.answer = new MeasuredAbstractObjectList<AbstractObject>(k);
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
            return radius;
        default:
            throw new IndexOutOfBoundsException("RangeQueryOperation has only two arguments");
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
    
    /**
     * Evaluate this query on a given set of objects.
     * The objects found by this evaluation are added to answer of this query via {@link #addToAnswer}.
     *
     * @param objects the collection of objects on which to evaluate this query
     * @return number of objects satisfying the query
     */
    @Override
    public int evaluate(GenericObjectIterator<LocalAbstractObject> objects) {
        int count = 0;

        // Iterate through all supplied objects
        while (objects.hasNext()) {
            // Get current object
            LocalAbstractObject object = objects.next();
            
            if (object.excludeUsingPrecompDist(queryObject, getRadius())) 
                continue;
                
            // Get distance to query object (the second parameter defines a stop condition in getDistance() 
            // which stops further computations if the distance will be greater than this value).
            float distance = queryObject.getDistance(object, getRadius());
            
            if (distance <= radius) {
                // Object satisfies the query (i.e. distance is smaller than radius)
                addToAnswer(object, distance);
                count++;
            }
        }
        
        return count;
    }

    
    /****************** Answer methods ******************/
    
    /**
     * Returns the radius of this range query.
     * The radius is accessible directly using the attribute {@link #radius radius}.
     * @return the radius of this range query
     */
    public float getRadius() {
        return this.radius;
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
     * Returns a string representation of this operation.
     * @return a string representation of this operation.
     */
    @Override
    public String toString() {
        return new StringBuffer("Range query <").append(queryObject).append(',').append(radius).append("> returned ").append(getAnswerCount()).append(" objects").toString();
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
        // The argument obj is always RangeQueryOperation or its descendant, because it has only abstract ancestors
        RangeQueryOperation castObj = (RangeQueryOperation)obj;

        if (!queryObject.dataEquals(castObj.queryObject))
            return false;

        return radius == castObj.radius;
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return queryObject.dataHashCode() ^ Float.floatToIntBits(radius);
    }

}
