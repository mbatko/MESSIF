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

import java.util.Collection;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.util.AggregationFunction;
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;
import messif.operations.RankingQueryOperation;

/**
 * K-nearest neighbors query operation with multiple query objects.
 * Retrieves <code>k</code> objects that are nearest to the specified query objects
 * (according to the distance measure).
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class KNNMultiQueryOperation extends RankingQueryOperation {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Query object */
    private final LocalAbstractObject[] queryObjects;

    /** Number of nearest objects to retrieve */
    private final int k;

    /** Type of aggregation of distances between a data object and all query objects into a single distance. */
    private final DistanceAggregation distanceAggregation;

    /**
     * If not null, this function is used to evaluate distance between two meta objects
     *  (instead of {@link LocalAbstractObject#getDistance(messif.objects.LocalAbstractObject)}).
     */
    private final AggregationFunction metaObjectAggregation;

    /**
     * Enumeration to distinguish between several variants of aggregating distances between a data object
     *  and all query objects into a single distance.
     */
    public static enum DistanceAggregation {

        SUM, MAX, AVG, MIN;

        protected float evaluate(float[] distances) {
            float retVal = 0f;
            switch (this) {
                case SUM:
                    for (float f : distances) {
                        retVal += f;
                    }
                    break;
                case MAX:
                    for (float f : distances) {
                        retVal = Math.max(retVal, f);
                    }
                    break;
                case MIN:
                    retVal = Float.MAX_VALUE;
                    for (float f : distances) {
                        retVal = Math.min(retVal, f);
                    }
                    break;
                case AVG:
                    for (float f : distances) {
                        retVal += f;
                    }
                    retVal /= (float) distances.length;
                    break;
            }
            return retVal;
        }
    }
    
    //****************** Constructors ******************//

    /**
     * Creates a new instance of kNNQueryOperation for given query objects and maximal number of objects to return.
     * Objects added to answer are updated to {@link AnswerType#NODATA_OBJECTS no-data objects}.
     * Default aggregation function is "sum".
     * @param queryObjects the objects to which the nearest neighbors are searched
     * @param k the number of nearest neighbors to retrieve
     */
    @AbstractOperation.OperationConstructor({"Query objects", "Number of nearest objects"})
    public KNNMultiQueryOperation(Collection<LocalAbstractObject> queryObjects, int k) {
        this(queryObjects, k, AnswerType.NODATA_OBJECTS, DistanceAggregation.SUM, null);
    }

    /**
     * Creates a new instance of kNNQueryOperation for given query objects and maximal number of objects to return.
     * Objects added to answer are updated to {@link AnswerType#NODATA_OBJECTS no-data objects}.
     * @param queryObjects the objects to which the nearest neighbors are searched
     * @param k the number of nearest neighbors to retrieve
     * @param aggregationFunction function to aggregate distances between set of query objects and data objects
     */
    @AbstractOperation.OperationConstructor({"Query objects", "Number of nearest objects", "Aggregation function"})
    public KNNMultiQueryOperation(Collection<LocalAbstractObject> queryObjects, int k, DistanceAggregation aggregationFunction, AggregationFunction metaObjectAggregation) {
        this(queryObjects, k, AnswerType.NODATA_OBJECTS, aggregationFunction, metaObjectAggregation);
    }

    /**
     * Creates a new instance of kNNQueryOperation for given query objects and maximal number of objects to return.
     * @param queryObjects the objects to which the nearest neighbors are searched
     * @param k the number of nearest neighbors to retrieve
     * @param answerType the type of objects this operation stores in its answer
     * @param aggregationFunction function to aggregate distances between set of query objects and data objects
     */
    @AbstractOperation.OperationConstructor({"Query objects", "Number of nearest objects", "Answer type", "Aggregation function"})
    public KNNMultiQueryOperation(Collection<LocalAbstractObject> queryObjects, int k, AnswerType answerType, DistanceAggregation aggregationFunction, AggregationFunction metaObjectAggregation) {
        this(queryObjects, k, false, answerType, aggregationFunction, metaObjectAggregation);
    }

    /**
     * Creates a new instance of kNNQueryOperation for given query objects and maximal number of objects to return.
     * @param queryObjects the objects to which the nearest neighbors are searched
     * @param k the number of nearest neighbors to retrieve
     * @param storedIndividualDistances if <tt>true</tt>, all distances between the data object and all query objects are
     *          stored in {@link messif.objects.util.RankedAbstractMetaObject sub-distances}
     * @param answerType the type of objects this operation stores in its answer
     * @param aggregationFunction function to aggregate distances between set of query objects and data objects
     */
    @AbstractOperation.OperationConstructor({"Query objects", "Number of nearest objects", "Store individual obj-queries distances?", "Answer type", "Aggregation function"})
    public KNNMultiQueryOperation(Collection<LocalAbstractObject> queryObjects, int k, boolean storedIndividualDistances, AnswerType answerType, DistanceAggregation distanceAggregation, AggregationFunction metaObjectAggregation) {
        super(answerType, k, storedIndividualDistances);
        this.queryObjects = queryObjects.toArray(new LocalAbstractObject[queryObjects.size()]);
        this.k = k;
        if (distanceAggregation == null) {
            throw new NullPointerException("distance aggregation cannot be null");
        }
        this.distanceAggregation = distanceAggregation;
        this.metaObjectAggregation = metaObjectAggregation;
    }


    //****************** Attribute access ******************//

    /**
     * Returns the query objects of this operation.
     * @return the query objects of this operation
     */
    public LocalAbstractObject[] getQueryObjects() {
        return queryObjects.clone();
    }

    /**
     * Returns the number of nearest objects to retrieve.
     * @return the number of nearest objects to retrieve
     */
    public int getK() {
        return k;
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
            return queryObjects;
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

        float [] distances = new float [queryObjects.length];

        // Iterate through all supplied objects
        while (objects.hasNext()) {
            // Get current object
            LocalAbstractObject object = objects.next();

            for (int i = 0; i < queryObjects.length; i++) {
                if (metaObjectAggregation != null) {
                    distances[i] = metaObjectAggregation.getDistance((MetaObject) queryObjects[i], (MetaObject) object);
                } else {
                    distances[i] = queryObjects[i].getDistance(object);
                }
            }

            addToAnswer(object, distanceAggregation.evaluate(distances), isStoringMetaDistances() ? distances.clone() : null);
        }

        return getAnswerCount() - beforeCount;
    }
    

    //****************** Overrides ******************//
    
    /**
     * Clear non-messif data stored in operation.
     * This method is intended to be called whenever the operation is
     * sent back to client in order to minimize problems with unknown
     * classes after deserialization.
     */
    @Override
    public void clearSurplusData() {
        super.clearSurplusData();
        for (int i = 0; i < queryObjects.length; i++)
            queryObjects[i].clearSurplusData();
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
        // The argument obj is always kNNQueryOperation or its descendant, because it has only abstract ancestors
        KNNMultiQueryOperation castObj = (KNNMultiQueryOperation)obj;

        if (k != castObj.k)
            return false;

        if (queryObjects.length != castObj.queryObjects.length)
            return false;

        for (int i = 0; i < queryObjects.length; i++)
            if (!queryObjects[i].dataEquals(castObj.queryObjects[i]))
                return false;

        if (distanceAggregation != castObj.distanceAggregation)
            return false;

        if ((metaObjectAggregation != null) && (castObj.metaObjectAggregation == null) ||
                (metaObjectAggregation == null) && (castObj.metaObjectAggregation != null))
            return false;
        
        if ((metaObjectAggregation != null) && (castObj.metaObjectAggregation != null) && (! metaObjectAggregation.equals(castObj.metaObjectAggregation)))
            return false;

        return true;
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        int hc = k;
        for (int i = 0; i < queryObjects.length; i++)
            hc += queryObjects[i].dataHashCode() << i;
        hc += distanceAggregation.hashCode();
        if (metaObjectAggregation != null) {
            hc += metaObjectAggregation.hashCode();
        }
        return hc;
    }

}
