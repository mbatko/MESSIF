/*
 *  This file is part of MESSIF library.
 *
 *  MESSIF library is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MESSIF library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MESSIF library.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.operations.query;

import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;
import messif.operations.RankingSingleQueryOperation;

/**
 * Range query operation.
 * Retrieves all objects that have their distances to the specified query object
 * less than or equal to the specified radius.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("Range query")
public class RangeQueryOperation extends RankingSingleQueryOperation {

    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Range query radius */
    protected final float radius;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of RangeQueryOperation for a given query object and radius.
     * Reduced objects ({@link messif.objects.NoDataObject}) will be used.
     * @param queryObject the query object
     * @param radius the query radius
     */
    @AbstractOperation.OperationConstructor({"Query object", "Query radius"})
    public RangeQueryOperation(LocalAbstractObject queryObject, float radius) {
        this(queryObject, radius, AnswerType.NODATA_OBJECTS, Integer.MAX_VALUE);
    }

    /**
     * Creates a new instance of RangeQueryOperation for a given query object and radius.
     * Reduced objects ({@link messif.objects.NoDataObject}) will be used.
     * @param queryObject the query object
     * @param radius the query radius
     * @param storeMetaDistances if <tt>true</tt>, all processed {@link messif.objects.MetaObject meta objects} will
     *          store their {@link messif.objects.util.RankedAbstractMetaObject sub-distances} in the answer
     */
    // This cannot have annotation, since it has also three parameters
    public RangeQueryOperation(LocalAbstractObject queryObject, float radius, boolean storeMetaDistances) {
        this(queryObject, radius, AnswerType.NODATA_OBJECTS, Integer.MAX_VALUE, storeMetaDistances);
    }

    /**
     * Creates a new instance of RangeQueryOperation for a given query object and radius.
     * @param queryObject the query object
     * @param radius the query radius
     * @param answerType the type of objects this operation stores in its answer
     */
    @AbstractOperation.OperationConstructor({"Query object", "Query radius", "Answer type"})
    public RangeQueryOperation(LocalAbstractObject queryObject, float radius, AnswerType answerType) {
        this(queryObject, radius, answerType, Integer.MAX_VALUE);
    }

    /**
     * Creates a new instance of RangeQueryOperation for a given query object and radius.
     * @param queryObject the query object
     * @param radius the query radius
     * @param answerType the type of objects this operation stores in its answer
     * @param storeMetaDistances if <tt>true</tt>, all processed {@link messif.objects.MetaObject meta objects} will
     *          store their {@link messif.objects.util.RankedAbstractMetaObject sub-distances} in the answer
     */
    public RangeQueryOperation(LocalAbstractObject queryObject, float radius, AnswerType answerType, boolean storeMetaDistances) {
        super(queryObject, answerType, storeMetaDistances);
        this.radius = radius;
    }

    /**
     * Creates a new instance of RangeQueryOperation for a given query object, radius and maximal number of objects to return.
     * Reduced objects ({@link messif.objects.NoDataObject}) will be used.
     * @param queryObject the query object
     * @param radius the query radius
     * @param maxAnswerSize sets the maximal answer size
     */
    // This cannot have annotation, since it has also three parameters
    public RangeQueryOperation(LocalAbstractObject queryObject, float radius, int maxAnswerSize) {
        super(queryObject, maxAnswerSize);
        this.radius = radius;
    }

    /**
     * Creates a new instance of RangeQueryOperation for a given query object, radius and maximal number of objects to return.
     * @param queryObject the query object
     * @param radius the query radius
     * @param answerType the type of objects this operation stores in its answer
     * @param maxAnswerSize sets the maximal answer size
     */
    @AbstractOperation.OperationConstructor({"Query object", "Query radius", "Answer type", "Maximal answer size"})
    public RangeQueryOperation(LocalAbstractObject queryObject, float radius, AnswerType answerType, int maxAnswerSize) {
        super(queryObject, answerType, maxAnswerSize);
        this.radius = radius;
    }

    /**
     * Creates a new instance of RangeQueryOperation for a given query object, radius and maximal number of objects to return.
     * @param queryObject the query object
     * @param radius the query radius
     * @param answerType the type of objects this operation stores in its answer
     * @param maxAnswerSize sets the maximal answer size
     * @param storeMetaDistances if <tt>true</tt>, all processed {@link messif.objects.MetaObject meta objects} will
     *          store their {@link messif.objects.util.RankedAbstractMetaObject sub-distances} in the answer
     */
    @AbstractOperation.OperationConstructor({"Query object", "Query radius", "Answer type", "Maximal answer size", "Store the meta-object subdistances?"})
    public RangeQueryOperation(LocalAbstractObject queryObject, float radius, AnswerType answerType, int maxAnswerSize, boolean storeMetaDistances) {
        super(queryObject, answerType, maxAnswerSize, storeMetaDistances);
        this.radius = radius;
    }


    //****************** Attribute access ******************//

    /**
     * Returns the radius of this range query.
     * @return the radius of this range query
     */
    public float getRadius() {
        return this.radius;
    }

    @Override
    public float getAnswerThreshold() {
        float dist = super.getAnswerThreshold();
        if (dist > radius)
            return radius;
        return dist;
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

            if (queryObject.excludeUsingPrecompDist(object, getAnswerThreshold()))
                continue;

            addToAnswer(object, getRadius());
        }

        return getAnswerCount() - beforeCount;
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
