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

import messif.objects.DistanceFunctionMultiObject;
import messif.objects.DistanceFunctionMultiObjectAggregation;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;
import messif.operations.QueryOperation;
import messif.operations.RankingMultiQueryOperation;

/**
 * Range query operation with multiple query objects.
 * Retrieves all objects that have their distances to the specified query objects
 * less than or equal to the specified radius.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("Range multi-object query")
public class RangeMultiQueryOperation extends RankingMultiQueryOperation {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Range query radius */
    protected final float radius;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of RangeMultiQueryOperation for given query objects and radius.
     * Objects added to answer are updated to {@link AnswerType#NODATA_OBJECTS no-data objects}.
     * The distance function is {@link DistanceFunctionMultiObjectAggregation#SUM sum aggregation}.
     * @param queryObjects the objects to which the nearest neighbors are searched
     * @param radius the query radius
     */
    @AbstractOperation.OperationConstructor({"Query objects", "Radius"})
    public RangeMultiQueryOperation(LocalAbstractObject[] queryObjects, float radius) {
        this(queryObjects, radius, DistanceFunctionMultiObjectAggregation.SUM);
    }

    /**
     * Creates a new instance of RangeMultiQueryOperation for given query objects and radius.
     * Objects added to answer are updated to {@link AnswerType#NODATA_OBJECTS no-data objects}.
     * @param queryObjects the objects to which the nearest neighbors are searched
     * @param radius the query radius
     * @param distanceFunction the distance function for computing the distances between a data object and all query objects
     */
    @AbstractOperation.OperationConstructor({"Query objects", "Radius", "Distance aggregation function"})
    public RangeMultiQueryOperation(LocalAbstractObject[] queryObjects, float radius, DistanceFunctionMultiObject<? super LocalAbstractObject> distanceFunction) {
        this(queryObjects, radius, AnswerType.NODATA_OBJECTS, distanceFunction);
    }

    /**
     * Creates a new instance of RangeMultiQueryOperation for given query objects and radius.
     * @param queryObjects the objects to which the nearest neighbors are searched
     * @param radius the query radius
     * @param answerType the type of objects this operation stores in its answer
     * @param distanceFunction the distance function for computing the distances between a data object and all query objects
     */
    @AbstractOperation.OperationConstructor({"Query objects", "Radius", "Answer type", "Distance aggregation function"})
    public RangeMultiQueryOperation(LocalAbstractObject[] queryObjects, float radius, AnswerType answerType, DistanceFunctionMultiObject<? super LocalAbstractObject> distanceFunction) {
        this(queryObjects, radius, false, answerType, distanceFunction);
    }

    /**
     * Creates a new instance of RangeMultiQueryOperation for given query objects and radius.
     * @param queryObjects the objects to which the nearest neighbors are searched
     * @param radius the query radius
     * @param storedIndividualDistances if <tt>true</tt>, all distances between the data object and all query objects are
     *          stored in {@link messif.objects.util.RankedAbstractMetaObject sub-distances}
     * @param answerType the type of objects this operation stores in its answer
     * @param distanceFunction the distance function for computing the distances between a data object and all query objects
     */
    @AbstractOperation.OperationConstructor({"Query objects", "Radius", "Store individual obj-queries distances?", "Answer type", "Distance aggregation function"})
    public RangeMultiQueryOperation(LocalAbstractObject[] queryObjects, float radius, boolean storedIndividualDistances, AnswerType answerType, DistanceFunctionMultiObject<? super LocalAbstractObject> distanceFunction) {
        super(queryObjects, distanceFunction, storedIndividualDistances, answerType);
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

    @Override
    public Object getArgument(int index) throws IndexOutOfBoundsException {
        switch (index) {
        case 0:
            return getQueryObjects();
        case 1:
            return radius;
        case 2:
            return getDistanceFunction();
        default:
            throw new IndexOutOfBoundsException(getClass().getSimpleName() + " has only three arguments");
        }
    }

    @Override
    public int getArgumentCount() {
        return 3;
    }


    //****************** Implementation of query evaluation ******************//

    @Override
    public int evaluate(AbstractObjectIterator<? extends LocalAbstractObject> objects) {
        int beforeCount = getAnswerCount();

        // Iterate through all supplied objects
        while (objects.hasNext())
            addToAnswerInternal(objects.next(), null, getAnswerThreshold());

        return getAnswerCount() - beforeCount;
    }


    //****************** Equality driven by operation data ******************//

    @Override
    protected boolean dataEqualsImpl(QueryOperation obj) {
        // The argument obj is always kNNQueryOperation or its descendant, because it has only abstract ancestors
        RangeMultiQueryOperation castObj = (RangeMultiQueryOperation)obj;

        if (radius != castObj.radius)
            return false;

        return super.dataEquals(obj);
    }

    @Override
    public int dataHashCode() {
        return super.dataHashCode() + Float.floatToIntBits(radius);
    }

}
