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
package messif.operations;

import java.util.ArrayList;
import java.util.Iterator;
import messif.objects.DistanceFunctionMultiObject;
import messif.objects.LocalAbstractObject;
import messif.objects.util.RankedAbstractMetaObject;
import messif.objects.util.RankedAbstractObject;
import messif.objects.util.RankedSortedCollection;
import messif.utility.Convert;


/**
 * The base class for query operations that return objects
 * ranked by a distance to multiple objects.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class RankingMultiQueryOperation extends RankingQueryOperation {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Array with the query objects */
    private LocalAbstractObject[] queryObjects;

    /** Distance function for computing the distances between a data object and all query objects */
    private final DistanceFunctionMultiObject<? super LocalAbstractObject> distanceFunction;

    /**
     * Flag whether the distances between the data object and all query objects are
     * stored in {@link messif.objects.util.RankedAbstractMetaObject sub-distances}
     */
    // TODO: This is a temporary hack - there should be additional object that handles both the object overall distances as well as meta object subdistances
    private final boolean storeIndividualDistances;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of RankingMultiQueryOperation.
     * @param queryObjects the objects with which this operation works
     * @param distanceFunction the distance function for computing the distances between a data object and all query objects
     * @param storeIndividualDistances if <tt>true</tt>, all distances between the data object and all query objects are
     *          stored in {@link messif.objects.util.RankedAbstractMetaObject sub-distances}
     * @param answerType the type of objects this operation stores in its answer
     * @param maxAnswerSize sets the maximal answer size
     * @throws NullPointerException if the {@code queryObjects} or {@code distanceAggregation} is <tt>null</tt> 
     */
    protected RankingMultiQueryOperation(LocalAbstractObject[] queryObjects, DistanceFunctionMultiObject<? super LocalAbstractObject> distanceFunction, boolean storeIndividualDistances, AnswerType answerType, int maxAnswerSize) throws NullPointerException {
        super(answerType, maxAnswerSize);
        this.queryObjects = queryObjects.clone();
        if (distanceFunction == null)
            throw new NullPointerException();
        this.distanceFunction = distanceFunction;
        this.storeIndividualDistances = storeIndividualDistances;
    }

    /**
     * Creates a new instance of RankingMultiQueryOperation.
     * Unlimited number of objects can be added to the answer.
     * @param queryObjects the objects with which this operation works
     * @param distanceFunction the distance function for computing the distances between a data object and all query objects
     * @param storeIndividualDistances if <tt>true</tt>, all distances between the data object and all query objects are
     *          stored in {@link messif.objects.util.RankedAbstractMetaObject sub-distances}
     * @param answerType the type of objects this operation stores in its answer
     * @throws NullPointerException if the {@code queryObjects} or {@code distanceAggregation} is <tt>null</tt> 
     */
    protected RankingMultiQueryOperation(LocalAbstractObject[] queryObjects, DistanceFunctionMultiObject<? super LocalAbstractObject> distanceFunction, boolean storeIndividualDistances, AnswerType answerType) throws NullPointerException {
        this(queryObjects, distanceFunction, storeIndividualDistances, answerType, Integer.MAX_VALUE);
    }

    /**
     * Creates a new instance of RankingMultiQueryOperation.
     * Objects added to answer are {@link AnswerType#NODATA_OBJECTS changed to no-data objects}.
     * Unlimited number of objects can be added to the answer.
     * @param queryObjects the objects with which this operation works
     * @param distanceFunction the distance function for computing the distances between a data object and all query objects
     * @param storeIndividualDistances if <tt>true</tt>, all distances between the data object and all query objects are
     *          stored in {@link messif.objects.util.RankedAbstractMetaObject sub-distances}
     * @throws NullPointerException if the {@code queryObjects} or {@code distanceAggregation} is <tt>null</tt> 
     */
    protected RankingMultiQueryOperation(LocalAbstractObject[] queryObjects, DistanceFunctionMultiObject<? super LocalAbstractObject> distanceFunction, boolean storeIndividualDistances) throws NullPointerException {
        this(queryObjects, distanceFunction, storeIndividualDistances, AnswerType.NODATA_OBJECTS);
    }

    /**
     * Creates a new instance of RankingMultiQueryOperation.
     * Objects added to answer are {@link AnswerType#NODATA_OBJECTS changed to no-data objects}.
     * Unlimited number of objects can be added to the answer.
     * The individual distances to the respective query objects are not stored.
     * @param queryObjects the objects with which this operation works
     * @param distanceFunction the distance function for computing the distances between a data object and all query objects
     * @throws NullPointerException if the {@code queryObjects} or {@code distanceAggregation} is <tt>null</tt> 
     */
    protected RankingMultiQueryOperation(LocalAbstractObject[] queryObjects, DistanceFunctionMultiObject<? super LocalAbstractObject> distanceFunction) throws NullPointerException {
        this(queryObjects, distanceFunction, false);
    }

    /**
     * Creates a new instance of RankingMultiQueryOperation.
     * @param queryObjects the objects with which this operation works
     * @param distanceFunction the distance function for computing the distances between a data object and all query objects
     * @param storeIndividualDistances if <tt>true</tt>, all distances between the data object and all query objects are
     *          stored in {@link messif.objects.util.RankedAbstractMetaObject sub-distances}
     * @param answerType the type of objects this operation stores in its answer
     * @param answerCollection collection to be used as answer (it must be empty, otherwise it will be cleared)
     * @throws NullPointerException if the {@code queryObjects} or {@code distanceAggregation} is <tt>null</tt> 
     */
    protected RankingMultiQueryOperation(LocalAbstractObject[] queryObjects, DistanceFunctionMultiObject<? super LocalAbstractObject> distanceFunction, boolean storeIndividualDistances, AnswerType answerType, RankedSortedCollection answerCollection) throws NullPointerException {
        super(answerType, answerCollection);
        this.queryObjects = queryObjects.clone();
        if (distanceFunction == null)
            throw new NullPointerException();
        this.distanceFunction = distanceFunction;
        this.storeIndividualDistances = storeIndividualDistances;
    }


    //****************** Utility method for creating array of objects ******************//

    /**
     * Creates an array of {@link LocalAbstractObject}s from the given iterator.
     * @param iterator the iterator that provides objects
     * @param count the number of objects to retrieve; negative number means unlimited
     * @return a new array of {@link LocalAbstractObject}s
     */
    public static LocalAbstractObject[] loadObjects(Iterator<? extends LocalAbstractObject> iterator, int count) {
        ArrayList<LocalAbstractObject> data = new ArrayList<LocalAbstractObject>(count > 0 ? count : 0);
        while (iterator.hasNext() && count-- != 0)
            data.add(iterator.next());
        return data.toArray(new LocalAbstractObject[data.size()]);
    }


    //****************** Data access methods ******************//

    /**
     * Returns the query objects of this operation.
     * @return the query objects of this operation
     */
    public LocalAbstractObject[] getQueryObjects() {
        return queryObjects.clone();
    }

    /**
     * Returns the number of query objects of this operation.
     * @return the number of query objects of this operation
     */
    public int getQueryObjectsCount() {
        return queryObjects.length;
    }

    /**
     * Returns the given query object of this operation.
     * @param index the zero-based index of the query object to retrieve
     * @return the given query object of this operation
     * @throws IndexOutOfBoundsException if the given index is not valid
     */
    public LocalAbstractObject getQueryObject(int index) throws IndexOutOfBoundsException {
        return queryObjects[index];
    }

    /**
     * Returns the distance function for computing the distances between a data object and all query objects.
     * This function is used to compute the resulting overall distance.
     * @return the distance function for computing the distances between a data object and all query objects
     */
    public DistanceFunctionMultiObject<? super LocalAbstractObject> getDistanceFunction() {
        return distanceFunction;
    }

    /**
     * Returns <tt>true</tt> if individual-distances for the respective query objects are stored in the answer.
     * @return <tt>true</tt> if individual-distances for the respective query objects are stored in the answer
     */
    public boolean isStoringIndividualDistances() {
        return storeIndividualDistances;
    }


    //****************** Cloning ******************//

    /**
     * Create a duplicate of this operation.
     * The answer of the query is not cloned.
     *
     * @return a clone of this operation
     * @throws CloneNotSupportedException if the operation instance cannot be cloned
     */
    @Override
    public RankingMultiQueryOperation clone() throws CloneNotSupportedException {
        RankingMultiQueryOperation operation = (RankingMultiQueryOperation)super.clone();
        LocalAbstractObject[] clonedQueryObjects = new LocalAbstractObject[operation.queryObjects.length];
        for (int i = 0; i < clonedQueryObjects.length; i++)
            clonedQueryObjects[i] = operation.queryObjects[i].clone();
        operation.queryObjects = clonedQueryObjects;
        return operation;
    }


    //****************** Overrides ******************//

    @Override
    public void clearSurplusData() {
        super.clearSurplusData();
        for (int i = 0; i < queryObjects.length; i++)
            queryObjects[i].clearSurplusData();
    }

    @Override
    protected boolean dataEqualsImpl(AbstractOperation obj) {
        // The argument obj is always RankingMultiQueryOperation or its descendant, because it has only abstract ancestors
        RankingMultiQueryOperation castObj = (RankingMultiQueryOperation)obj;

        if (queryObjects.length != castObj.queryObjects.length)
            return false;

        for (int i = 0; i < queryObjects.length; i++)
            if (!queryObjects[i].dataEquals(castObj.queryObjects[i]))
                return false;

        if (!distanceFunction.equals(castObj.distanceFunction))
            return false;

        return true;
    }

    @Override
    public int dataHashCode() {
        int hc = distanceFunction.hashCode();
        for (int i = 0; i < queryObjects.length; i++)
            hc += queryObjects[i].dataHashCode() << i;
        return hc;
    }

    @Override
    public String getArgumentString(int index) throws IndexOutOfBoundsException, UnsupportedOperationException {
        Object value = getArgument(index);
        if (value != null && value.getClass().isArray())
            return Convert.arrayToString(value, false);
        return String.valueOf(value);
    }


    //****************** Answer modification methods ******************//

    /**
     * Adds an object to the answer. The rank of the object is computed automatically
     * as a distance between all the {@link #getQueryObjects() query objects} and the specified object
     * using the specified {@link #getDistanceFunction() aggretate distance function}. The
     * passed array {@code individualDistances} will be filled with the distances
     * of the individual query objects.
     * 
     * @param object the object to add
     * @param individualDistances the array to fill with the distances to the respective query objects;
     *          if not <tt>null</tt>, it must have the same number of allocated elements as the number of query objects
     * @param distThreshold the threshold on distance;
     *      if the computed distance exceeds the threshold (sharply),
     *      the object is not added to the answer
     * @return the distance-ranked object object that was added to answer or <tt>null</tt> if the object was not added
     * @throws IndexOutOfBoundsException if the passed {@code individualDistances} array is not big enough
     */
    protected RankedAbstractObject addToAnswerInternal(LocalAbstractObject object, float[] individualDistances, float distThreshold) throws IndexOutOfBoundsException {
        if (object == null)
            return null;
        if (individualDistances == null)
            individualDistances = new float[queryObjects.length];
        float distance = distanceFunction.getDistanceMultiObject(queryObjects, object, individualDistances);

        if (distance > distThreshold)
            return null;

        return addToAnswer(object, distance, storeIndividualDistances ? individualDistances : null);
    }

    /**
     * Adds an object to the answer. The rank of the object is computed automatically
     * as a distance between all the {@link #getQueryObjects() query objects} and the specified object
     * using the specified {@link #getDistanceFunction() aggretate distance function}.
     * 
     * @param object the object to add
     * @param distThreshold the threshold on distance;
     *      if the computed distance exceeds the threshold (sharply),
     *      the object is not added to the answer
     * @return the distance-ranked object object that was added to answer or <tt>null</tt> if the object was not added
     */
    public RankedAbstractObject addToAnswer(LocalAbstractObject object, float distThreshold) {
        return addToAnswerInternal(object, null, distThreshold);
    }

    /**
     * Adds an object to the answer. The rank of the object is computed automatically
     * as a distance between the all the {@link #getQueryObjects() query objects} and the specified object
     * using the specified {@link #getDistanceFunction() aggretate distance function}.
     * 
     * @param object the object to add
     * @return the distance-ranked object object that was added to answer or <tt>null</tt> if the object was not added
     */
    public RankedAbstractObject addToAnswer(LocalAbstractObject object) {
        return addToAnswer(object, LocalAbstractObject.MAX_DISTANCE);
    }
    
}
