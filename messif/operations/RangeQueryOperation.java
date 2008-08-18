/*
 * RangeQueryOperation.java
 *
 * Created on 6. kveten 2004, 17:31
 */

package messif.operations;

import messif.netbucket.RemoteAbstractObject;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.util.RankedAbstractObject;

/**
 * Range query operation.
 * Retrieves all objects that have their distances to the specified query object
 * less than or equal to the specified radius.
 * 
 * @author  Vlastislav Dohnal, xdohnal@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
@AbstractOperation.OperationName("Range query")
public class RangeQueryOperation extends RankingQueryOperation {

    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Range query object */
    protected final LocalAbstractObject queryObject;
    /** Range query radius */
    protected final float radius;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of RangeQueryOperation for a given query object and radius.
     * Reduced objects ({@link messif.netbucket.RemoteAbstractObject}) will be used.
     * @param queryObject the query object
     * @param radius the query radius
     */
    @AbstractOperation.OperationConstructor({"Query object", "Query radius"})
    public RangeQueryOperation(LocalAbstractObject queryObject, float radius) {
        this(queryObject, radius, AnswerType.REMOTE_OBJECTS, Integer.MAX_VALUE);
    }

    /**
     * Creates a new instance of RangeQueryOperation for a given query object and radius.
     * @param queryObject the query object
     * @param radius the query radius
     * @param answerType the type of objects this operation stores in its answer
     */
    public RangeQueryOperation(LocalAbstractObject queryObject, float radius, AnswerType answerType) {
        this(queryObject, radius, answerType, Integer.MAX_VALUE);
    }

    /**
     * Creates a new instance of RangeQueryOperation for a given query object, radius and maximal number of objects to return.
     * Reduced objects ({@link messif.netbucket.RemoteAbstractObject}) will be used.
     * @param queryObject the query object
     * @param radius the query radius
     * @param maxAnswerSize sets the maximal answer size
     */
    public RangeQueryOperation(LocalAbstractObject queryObject, float radius, int maxAnswerSize) {
        this(queryObject, radius, AnswerType.REMOTE_OBJECTS, maxAnswerSize);
    }

    /**
     * Creates a new instance of RangeQueryOperation for a given query object, radius and maximal number of objects to return.
     * @param queryObject the query object
     * @param radius the query radius
     * @param answerType the type of objects this operation stores in its answer
     * @param maxAnswerSize sets the maximal answer size
     */
    public RangeQueryOperation(LocalAbstractObject queryObject, float radius, AnswerType answerType, int maxAnswerSize) {
        super(answerType, maxAnswerSize);
        this.queryObject = queryObject;
        this.radius = radius;
    }


    //****************** Attribute access ******************//

    /**
     * Returns the query object of this range query.
     * @return the query object of this range query
     */
    public LocalAbstractObject getQueryObject() {
        return queryObject;
    }

    /**
     * Returns the radius of this range query.
     * @return the radius of this range query
     */
    public float getRadius() {
        return this.radius;
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


    //****************** Implementation of query evaluation ******************//

    /**
     * Evaluate this query on a given set of objects.
     * The objects found by this evaluation are added to answer of this query via {@link #addToAnswer}.
     *
     * @param objects the collection of objects on which to evaluate this query
     * @return number of objects satisfying the query
     */
    @Override
    public int evaluate(AbstractObjectIterator<? extends LocalAbstractObject> objects) {
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

            // Object satisfies the query (i.e. distance is smaller than radius)
            if (distance <= radius)
                addToAnswer(object, distance);
        }

        return getAnswerCount() - beforeCount;
    }


    //****************** Overrides ******************//

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
        queryObject.clearSurplusData();
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
