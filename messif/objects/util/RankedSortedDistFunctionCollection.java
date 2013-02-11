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

import java.util.Collection;
import messif.objects.AbstractObject;
import messif.objects.DistanceFunction;
import messif.objects.LocalAbstractObject;
import messif.operations.AnswerType;

/**
 * Specialization of {@link RankedSortedCollection} that uses a different distance
 * function to rank the objects.
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
public class RankedSortedDistFunctionCollection<T extends AbstractObject> extends RankedSortedCollection {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Distance function for the ranking */
    private final DistanceFunction<? super T> rankingDistanceFunction;
    /** Weight of the original distance */
    private final float originalDistanceWeight;
    /** Ranking object used as the first argument for the {@link #rankingDistanceFunction} */
    private final T rankingObject;
    /** Flag whether the {@link #add(java.lang.Object) add} method computes the rank (<tt>true</tt>) or adds the ranked objects as-is (<tt>false</tt>)*/
    private final boolean rankInAdd;


    //****************** Constructors ******************//

    /**
     * Constructs an empty collection with the specified initial and maximal capacity.
     * If this collection is used during a query processing, the {@code rankInAddAll}
     * should be set to <tt>false</tt>. If the collection is used as post-ranking,
     * the {@code rankInAddAll} probably needs to be set to <tt>true</tt>, however
     * the correct type of objects must be present in the operation (i.e. the {@link AnswerType}
     * should be set to something above {@link AnswerType#CLEARED_OBJECTS}.
     * 
     * @param rankingDistanceFunction the distance function used for the ranking, 
     *          if <tt>null</tt>, the natural distance function of the ranking object will be used
     * @param rankingObject the object used for ranking
     * @param originalDistanceWeight the weight of the original distance (if zero, the original distance is ignored)
     * @param rankInAdd flag whether the {@link #add(java.lang.Object) add} method computes the rank (<tt>true</tt>) or adds the ranked objects as-is (<tt>false</tt>)
     * @param initialCapacity the initial capacity of the collection
     * @param maximalCapacity the maximal capacity of the collection
     * @throws IllegalArgumentException if the specified initial or maximal capacity is invalid or the ranking distance function is not compatible with the ranking object
     * @throws NullPointerException if both the ranking distance function and the ranking object are <tt>null</tt>
     */
    @SuppressWarnings("unchecked")
    public RankedSortedDistFunctionCollection(DistanceFunction<? super T> rankingDistanceFunction, T rankingObject, float originalDistanceWeight, boolean rankInAdd, int initialCapacity, int maximalCapacity) throws IllegalArgumentException, NullPointerException {
        super(initialCapacity, maximalCapacity);
        if (rankingDistanceFunction == null) {
            this.rankingDistanceFunction = (DistanceFunction)LocalAbstractObject.trivialDistanceFunction; // This cast IS checked on the next line
        } else {
            this.rankingDistanceFunction = rankingDistanceFunction;
        }
        this.originalDistanceWeight = originalDistanceWeight;
        if (rankingObject != null && !this.rankingDistanceFunction.getDistanceObjectClass().isInstance(rankingObject))
            throw new IllegalArgumentException("Ranking collection distance function is not compatible with the given ranking object");
        this.rankingObject = rankingObject;
        this.rankInAdd = rankInAdd;
    }

    /**
     * Constructs an empty collection.
     * The initial capacity of the collection is set to 16 and maximal capacity
     * is not limited.
     * 
     * @param rankingDistanceFunction the distance function used for the ranking
     * @param rankingObject the object used for ranking
     * @param originalDistanceWeight the weight of the original distance (if zero, the original distance is ignored)
     * @param rankInAdd flag whether the {@link #add(java.lang.Object) add} method computes the rank (<tt>true</tt>) or adds the ranked objects as-is (<tt>false</tt>)
     */
    public RankedSortedDistFunctionCollection(DistanceFunction<? super T> rankingDistanceFunction, T rankingObject, float originalDistanceWeight, boolean rankInAdd) {
        this(rankingDistanceFunction, rankingObject, originalDistanceWeight, rankInAdd, DEFAULT_INITIAL_CAPACITY, UNLIMITED_CAPACITY);
    }

    /**
     * Constructs an empty collection.
     * The initial capacity of the collection is set to 16 and maximal capacity
     * is not limited.
     * 
     * @param rankingDistanceFunction the distance function used for the ranking
     * @param rankingObject the object used for ranking
     * @param originalDistanceWeight the weight of the original distance (if zero, the original distance is ignored)
     */
    public RankedSortedDistFunctionCollection(DistanceFunction<? super T> rankingDistanceFunction, T rankingObject, float originalDistanceWeight) {
        this(rankingDistanceFunction, rankingObject, originalDistanceWeight, false);
    }

    /**
     * Creates an empty collection with the specified initial and maximal capacity.
     * The distance function of the given {@code rankingObject} is used to compute
     * the rank.
     * 
     * @param rankingObject the object used for ranking
     * @param originalDistanceWeight the weight of the original distance (if zero, the original distance is ignored)
     * @param rankInAdd flag whether the {@link #add(java.lang.Object) add} method computes the rank (<tt>true</tt>) or adds the ranked objects as-is (<tt>false</tt>)
     * @param initialCapacity the initial capacity of the collection
     * @param maximalCapacity the maximal capacity of the collection
     * @return an empty collection
     * @throws IllegalArgumentException if the specified initial or maximal capacity is invalid
     * @throws NullPointerException if the ranking object is <tt>null</tt> 
     */
    public static RankedSortedDistFunctionCollection<LocalAbstractObject> create(LocalAbstractObject rankingObject, float originalDistanceWeight, boolean rankInAdd, int initialCapacity, int maximalCapacity) throws IllegalArgumentException, NullPointerException {
        return new RankedSortedDistFunctionCollection<LocalAbstractObject>(LocalAbstractObject.trivialDistanceFunction, rankingObject, originalDistanceWeight, rankInAdd, initialCapacity, maximalCapacity);
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

        // This cast is sufficiently checked on the previous line - we only require object compatible with the distance function
        RankedAbstractObject rankedObject = rankObject(answerType, object, getNewDistance(distance, (T)object), objectDistances);
        return super.add(rankedObject) ? rankedObject : null;
    }

    @Override
    public final boolean add(RankedAbstractObject obj) {
        return add(obj, rankInAdd);
    }

    /**
     * Adds the specified element to this list.
     * The element is added according the to order defined by the comparator.
     * @param obj element to be appended to this list
     * @param rankInAdd flag whether the rank of the added object is recomputed (<tt>true</tt>) or
     *          the object is added as-is (<tt>false</tt>)
     * @return <tt>true</tt> if the object was added to the collection or
     *          <tt>false</tt> if not (e.g. because of the limited capacity of the collection)
     */
    protected boolean add(RankedAbstractObject obj, boolean rankInAdd) {
        if (!rankInAdd)
            return super.add(obj);
        return add(
                AnswerType.ORIGINAL_OBJECTS, obj.getObject(), obj.getDistance(),
                obj instanceof RankedAbstractMetaObject ? ((RankedAbstractMetaObject)obj).getSubDistances() : null
            ) != null;
    }

    /**
     * Returns the distance function used for the ranking.
     * @return the distance function used for the ranking
     */
    public DistanceFunction<? super T> getRankingDistanceFunction() {
        return rankingDistanceFunction;
    }

    /**
     * Returns the ranking object used as the first argument for the {@link #getRankingDistanceFunction()}.
     * @return the ranking object used as the first argument for the {@link #getRankingDistanceFunction()}
     */
    public T getRankingObject() {
        return rankingObject;
    }

    /**
     * Returns the weight of the original distance that is summed with the new distance to get the ranking.
     * If zero, the ranking ignores the original distances completely and computes
     * the ranking based only on the new distances.
     * @return the weight of the original distance
     */
    public float getOriginalDistanceWeight() {
        return originalDistanceWeight;
    }

    /**
     * Returns <tt>true</tt> if the {@link #add(java.lang.Object) add} method computes the rank or <tt>false</tt>,
     * if the ranked objects are added without additional computations.
     * @return the flag whether the {@link #add(java.lang.Object) add} method computes the rank or not
     */
    public boolean isRankingInAdd() {
        return rankInAdd;
    }
    
    /**
     * Given the original distance and the object, this method returns the
     * new distance according to which the object should be ranked.
     * @param origDistance original query-object distance
     * @param object data object corresponding to this distance
     * @return new distance to be used by this collection
     */
    protected final float getNewDistance(float origDistance, T object) {
        return origDistance * originalDistanceWeight + rankingDistanceFunction.getDistance(rankingObject, object);
    }    
        
}
