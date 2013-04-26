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
package messif.objects.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import messif.objects.AbstractObject;
import messif.objects.DistanceFunctionMultiObject;
import messif.operations.AnswerType;

/**
 * Specialization of {@link RankedSortedMultiCollection} that uses a different distance
 * function to rank the objects. The additional collections store the objects ordered
 * by the respective sub-distance.
 * 
 * <p>
 * Note that this collection can be used either during the query evaluation or
 * as after the computation is done (i.e. as a post-ranking of the collection).
 * </p>
 * <p>
 * For the first mode of operation (query evaluation - set by {@code rankInAdd = false}),
 * the new distance is computed only when a query-object distance is computed via
 * {@link #add(messif.operations.AnswerType, messif.objects.AbstractObject, float, float[])}.
 * The already computed data added via {@link #add(messif.objects.util.RankedAbstractObject) add}
 * or {@link #addAll(java.util.Collection) addAll} methods are not touched, so that the lists
 * can be merged without recomputing the data.
 * </p>
 * <p>
 * For the second mode of operation (post-ranking - set by {@code rankInAdd = true}), the
 * objects have their ranking recomputed when added to this collection by any {@code add}
 * method. To create a separate re-ranked list, create a new instance of this collection
 * with {@code rankInAdd = true} and {@link #addAll(java.util.Collection) add all}
 * objects from the original collection.
 * </p>
 * 
 * @param <T> the type of objects that the ranking function works with
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class RankedSortedDistFunctionMultiCollection<T extends AbstractObject> extends RankedSortedMultiCollection {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Distance function for the ranking */
    private final DistanceFunctionMultiObject<? super T> rankingDistanceFunction;
    /** Weight of the original distance */
    private final float originalDistanceWeight;
    /** Ranking objects used as the first argument for the {@link #rankingDistanceFunction} */
    private final Collection<? extends T> rankingObjects;
    /** Flag whether the {@link #add(java.lang.Object) add} method computes the rank (<tt>true</tt>) or adds the ranked objects as-is (<tt>false</tt>)*/
    private final boolean rankInAdd;


    //****************** Constructors ******************//

    /**
     * Constructs an empty collection with the specified initial and maximal capacity.
     * If this collection is used during a query processing, the {@code rankInAdd}
     * should be set to <tt>false</tt>. If the collection is used as post-ranking,
     * the {@code rankInAdd} probably needs to be set to <tt>true</tt>, however
     * the correct type of objects must be present in the operation (i.e. the {@link AnswerType}
     * should be set to something above {@link AnswerType#CLEARED_OBJECTS}.
     * 
     * @param rankingDistanceFunction the distance function used for the ranking
     * @param rankingObjects the objects used for ranking
     * @param originalDistanceWeight the weight of the original distance (if zero, the original distance is ignored)
     * @param rankInAdd flag whether the {@link #add(java.lang.Object) add} method computes the rank (<tt>true</tt>) or adds the ranked objects as-is (<tt>false</tt>)
     * @param initialCapacity the initial capacity of the collection
     * @param maximalCapacity the maximal capacity of the collection
     * @throws IllegalArgumentException if the specified initial or maximal capacity is invalid
     * @throws NullPointerException if the ranking distance function is <tt>null</tt>
     */
    public RankedSortedDistFunctionMultiCollection(DistanceFunctionMultiObject<? super T> rankingDistanceFunction, T[] rankingObjects, float originalDistanceWeight, boolean rankInAdd, int initialCapacity, int maximalCapacity) throws IllegalArgumentException, NullPointerException {
        super(initialCapacity, maximalCapacity, rankingObjects.length);
        if (rankingDistanceFunction == null)
            throw new NullPointerException();
        this.rankingDistanceFunction = rankingDistanceFunction;
        this.originalDistanceWeight = originalDistanceWeight;
        this.rankingObjects = new ArrayList<T>(Arrays.asList(rankingObjects));
        this.rankInAdd = rankInAdd;
    }

    /**
     * Constructs an empty collection with the specified initial and maximal capacity.
     * If this collection is used during a query processing, the {@code rankInAdd}
     * should be set to <tt>false</tt>. If the collection is used as post-ranking,
     * the {@code rankInAdd} probably needs to be set to <tt>true</tt>, however
     * the correct type of objects must be present in the operation (i.e. the {@link AnswerType}
     * should be set to something above {@link AnswerType#CLEARED_OBJECTS}.
     * 
     * @param rankingDistanceFunction the distance function used for the ranking
     * @param rankingObjects an iterator over the objects used for ranking
     * @param originalDistanceWeight the weight of the original distance (if zero, the original distance is ignored)
     * @param rankInAdd flag whether the {@link #add(java.lang.Object) add} method computes the rank (<tt>true</tt>) or adds the ranked objects as-is (<tt>false</tt>)
     * @param initialCapacity the initial capacity of the collection
     * @param maximalCapacity the maximal capacity of the collection
     * @throws IllegalArgumentException if the specified initial or maximal capacity is invalid
     * @throws NullPointerException if the ranking distance function is <tt>null</tt>
     */
    public RankedSortedDistFunctionMultiCollection(DistanceFunctionMultiObject<? super T> rankingDistanceFunction, AbstractObjectIterator<T> rankingObjects, float originalDistanceWeight, boolean rankInAdd, int initialCapacity, int maximalCapacity, boolean dummyParam) throws IllegalArgumentException, NullPointerException {
        this(rankingDistanceFunction, (T[]) new AbstractObjectList<T>(rankingObjects).toArray(new AbstractObject [0]), originalDistanceWeight, rankInAdd, initialCapacity, maximalCapacity);
    }
    
    /**
     * Constructs an empty collection.
     * The initial capacity of the collection is set to 16 and maximal capacity
     * is not limited.
     * 
     * @param rankingDistanceFunction the distance function used for the ranking
     * @param rankingObjects the object used for ranking
     * @param originalDistanceWeight the weight of the original distance (if zero, the original distance is ignored)
     * @param rankInAdd flag whether the {@link #add(java.lang.Object) add} method computes the rank (<tt>true</tt>) or adds the ranked objects as-is (<tt>false</tt>)
     */
    public RankedSortedDistFunctionMultiCollection(DistanceFunctionMultiObject<? super T> rankingDistanceFunction, T[] rankingObjects, float originalDistanceWeight, boolean rankInAdd) {
        this(rankingDistanceFunction, rankingObjects, originalDistanceWeight, rankInAdd, DEFAULT_INITIAL_CAPACITY, UNLIMITED_CAPACITY);
    }

    /**
     * Constructs an empty collection.
     * The initial capacity of the collection is set to 16 and maximal capacity
     * is not limited.
     * 
     * @param rankingDistanceFunction the distance function used for the ranking
     * @param rankingObjects the object used for ranking
     * @param originalDistanceWeight the weight of the original distance (if zero, the original distance is ignored)
     */
    public RankedSortedDistFunctionMultiCollection(DistanceFunctionMultiObject<? super T> rankingDistanceFunction, T[] rankingObjects, float originalDistanceWeight) {
        this(rankingDistanceFunction, rankingObjects, originalDistanceWeight, false);
    }


    //****************** Overrides ******************//

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc} or the distance function requires a different class
     */
    @SuppressWarnings("unchecked")
    @Override
    public RankedAbstractObject add(AnswerType answerType, AbstractObject object, float distance, float[] objectDistances) throws IllegalArgumentException {
        if (!rankingDistanceFunction.getDistanceObjectClass().isInstance(object))
            throw new IllegalArgumentException("Distance function requires " + rankingDistanceFunction.getDistanceObjectClass() + " but " + object.getClass() + " was given (using AnswerType.NODATA_OBJECTS?)");
        // Compute multidistance
        float[] multiObjectDistances = new float[rankingObjects.size()];
        float newDistance = rankingDistanceFunction.getDistanceMultiObject(rankingObjects, (T)object, multiObjectDistances);
        // This cast is sufficiently checked on the first line - we only require object compatible with the distance function
        RankedAbstractObject rankedObject = rankObject(answerType, object, distance * originalDistanceWeight + newDistance, multiObjectDistances); // FIXME: object sub-distances are forgotten
        return super.add(rankedObject) ? rankedObject : null;
    }

    @Override
    public boolean add(RankedAbstractObject obj) {
        if (!rankInAdd)
            return super.add(obj);
        return add(
                AnswerType.ORIGINAL_OBJECTS, obj.getObject(), obj.getDistance(),
                obj instanceof RankedAbstractMetaObject ? ((RankedAbstractMetaObject)obj).getSubDistances() : null
            ) != null;
    }

}
