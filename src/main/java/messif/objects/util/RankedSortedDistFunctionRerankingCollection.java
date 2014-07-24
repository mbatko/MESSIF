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
import java.util.Collections;
import messif.objects.AbstractObject;
import messif.objects.DistanceFunction;
import messif.operations.AnswerType;

/**
 * Specialization of {@link RankedSortedDistFunctionCollection} that remembers
 * the original ordering of the objects and allows to re-rank the results
 * using another distance function.
 * 
 * <p>
 * This collection cannot be used during the operation evaluation, it is intended to be
 * used ONLY when multiple re-ranking collections are computed on the same data.
 * </p>
 * 
 * <p>
 * Note that the original ordering can be used only by other instances of
 * {@link RankedSortedDistFunctionRerankingCollection} and only when {@link #addAll(java.util.Collection)}
 * method is called.
 * </p>
 * 
 * @param <T> the type of objects that the ranking function works with
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class RankedSortedDistFunctionRerankingCollection<T extends AbstractObject> extends RankedSortedDistFunctionCollection<T> {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Internal memory that stores the objects in the original ordering */
    private final RankedSortedCollection memory;


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
     * @param initialCapacity the initial capacity of the collection
     * @param maximalCapacity the maximal capacity of the collection
     * @throws IllegalArgumentException if the specified initial or maximal capacity is invalid
     * @throws NullPointerException if the ranking distance function is <tt>null</tt>
     */
    public RankedSortedDistFunctionRerankingCollection(DistanceFunction<? super T> rankingDistanceFunction, T rankingObject, float originalDistanceWeight, int initialCapacity, int maximalCapacity) throws IllegalArgumentException, NullPointerException {
        super(rankingDistanceFunction, rankingObject, originalDistanceWeight, true, initialCapacity, maximalCapacity);
        this.memory = new RankedSortedCollection(initialCapacity, maximalCapacity);
    }


    //****************** Overrides ******************//

    @Override
    public void setMaximalCapacity(int capacity) {
        super.setMaximalCapacity(capacity);
        memory.setMaximalCapacity(capacity);
    }

    @Override
    public final boolean addAll(Collection<? extends RankedAbstractObject> c) {
        if (c instanceof RankedSortedDistFunctionRerankingCollection) {
            this.memory.addAll(((RankedSortedDistFunctionRerankingCollection<?>)c).memory);
        } else {
            this.memory.addAll(c);
        }
        return addAllWithRanking(memory);
    }

    /**
     * Returns the memorized collection with the original ranking.
     * @return the original ranking collection
     */
    protected final Collection<? extends RankedAbstractObject> getOriginalRankingCollection() {
        return Collections.unmodifiableCollection(memory);
    }

    /**
     * Compute the new ranking of the given data.
     * It is the responsibility of this method to {@link #add(java.lang.Object) add}
     * the data according to the new ranking.
     * @param c the collection with the data to rank
     * @return <tt>true</tt> if this collection changed as a result of the call
     */
    protected boolean addAllWithRanking(Collection<? extends RankedAbstractObject> c) {
        return super.addAll(c);
    }
}
