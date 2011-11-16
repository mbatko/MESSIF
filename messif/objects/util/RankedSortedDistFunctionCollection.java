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

import messif.objects.AbstractObject;
import messif.objects.DistanceFunction;
import messif.objects.LocalAbstractObject;
import messif.operations.AnswerType;

/**
 * Specialization of {@link RankedSortedCollection} that uses a different distance
 * function to rank the objects.
 * 
 * <p>
 * Note that the distance function is computed only if the
 * {@link #add(messif.operations.AnswerType, messif.objects.AbstractObject, float, float[])}
 * is used. The already ranked data added via {@link #add} or {@link #addAll}
 * are not touched, so that the lists can be merged without recomputing the data.
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
     * @param rankingDistanceFunction the distance function used for the ranking
     * @param rankingObject the object used for ranking
     * @param originalDistanceWeight the weight of the original distance (if zero, the original distance is ignored)
     * @param rankInAdd flag whether the {@link #add(java.lang.Object) add} method computes the rank (<tt>true</tt>) or adds the ranked objects as-is (<tt>false</tt>)
     * @param initialCapacity the initial capacity of the collection
     * @param maximalCapacity the maximal capacity of the collection
     * @throws IllegalArgumentException if the specified initial or maximal capacity is invalid
     * @throws NullPointerException if the ranking distance function is <tt>null</tt>
     */
    public RankedSortedDistFunctionCollection(DistanceFunction<? super T> rankingDistanceFunction, T rankingObject, float originalDistanceWeight, boolean rankInAdd, int initialCapacity, int maximalCapacity) throws IllegalArgumentException, NullPointerException {
        super(initialCapacity, maximalCapacity);
        if (rankingDistanceFunction == null)
            throw new NullPointerException();
        this.rankingDistanceFunction = rankingDistanceFunction;
        this.originalDistanceWeight = originalDistanceWeight;
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
        return new RankedSortedDistFunctionCollection<LocalAbstractObject>(rankingObject, rankingObject, originalDistanceWeight, rankInAdd, initialCapacity, maximalCapacity);
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
        return super.add(answerType, object, distance * originalDistanceWeight + rankingDistanceFunction.getDistance(rankingObject, (T)object), objectDistances, true);
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
