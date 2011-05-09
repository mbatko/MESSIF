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

import java.util.Comparator;
import java.util.Iterator;
import messif.objects.AbstractObject;
import messif.objects.DistanceFunction;
import messif.objects.LocalAbstractObject;
import messif.objects.ObjectProvider;
import messif.utility.SortedCollection;

/**
 * Specialization of {@link SortedCollection} that is specific for distance-ranked objects.
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class RankedSortedCollection extends DistanceRankedSortedCollection<RankedAbstractObject>  {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Constructor ******************//

    /**
     * Constructs an empty collection with the specified initial and maximal capacity.
     * @param initialCapacity the initial capacity of the collection
     * @param maximalCapacity the maximal capatity of the collection
     * @param comparator the comparator that defines ordering
     * @throws IllegalArgumentException if the specified initial or maximal capacity is invalid
     */
    public RankedSortedCollection(int initialCapacity, int maximalCapacity, Comparator<? super RankedAbstractObject> comparator) throws IllegalArgumentException {
        super(initialCapacity, maximalCapacity, comparator);
    }

    /**
     * Constructs an empty collection with the specified initial and maximal capacity.
     * The order is defined using the natural order of items.
     * @param initialCapacity the initial capacity of the collection
     * @param maximalCapacity the maximal capatity of the collection
     * @throws IllegalArgumentException if the specified initial or maximal capacity is invalid
     */
    public RankedSortedCollection(int initialCapacity, int maximalCapacity) throws IllegalArgumentException {
        super(initialCapacity, maximalCapacity, null);
    }

    /**
     * Constructs an empty collection.
     * The order is defined using the natural order of items.
     * The initial capacity of the collection is set to 16 and maximal capacity
     * is not limited.
     * @throws IllegalArgumentException if the specified initial or maximal capacity is invalid
     */
    public RankedSortedCollection() throws IllegalArgumentException {
        super();
    }

    /**
     * Creates a new collection filled with objects provided by the {@code iterator}.
     * Objects are ranked by the distance measured by the {@code distanceFunction}
     * from the given {@code referenceObject}.
     * @param <T> the type of object used to measure the distance
     * @param distanceFunction the distance function used for the measuring
     * @param referenceObject the reference object from which the distance is measured
     * @param iterator the iterator on objects to add to the collection
     */
    public <T extends AbstractObject> RankedSortedCollection(DistanceFunction<? super T> distanceFunction, T referenceObject, Iterator<? extends T> iterator) {
        while (iterator.hasNext())
            add(new RankedAbstractObject(iterator.next(), distanceFunction, referenceObject));
    }

    /**
     * Creates a new collection filled with objects provided by the {@code objectProvider}.
     * Objects are ranked by the distance measured by the {@code distanceFunction}
     * from the given {@code referenceObject}.
     * @param <T> the type of object used to measure the distance
     * @param distanceFunction the distance function used for the measuring
     * @param referenceObject the reference object from which the distance is measured
     * @param objectProvider the provider of objects to add to the collection
     */
    public <T extends AbstractObject> RankedSortedCollection(DistanceFunction<? super T> distanceFunction, T referenceObject, ObjectProvider<? extends T> objectProvider) {
        this(distanceFunction, referenceObject, (Iterator<? extends T>)objectProvider.provideObjects());
    }

    /**
     * Creates a new collection filled with objects provided by the {@code iterator}.
     * Objects are ranked by the distance measured from the given {@code referenceObject}.
     * @param referenceObject the reference object from which the distance is measured
     * @param iterator the iterator on objects to add to the collection
     */
    public RankedSortedCollection(LocalAbstractObject referenceObject, Iterator<? extends LocalAbstractObject> iterator) {
        this(referenceObject, referenceObject, iterator);
    }

    /**
     * Creates a new collection filled with objects provided by the {@code objectProvider}.
     * Objects are ranked by the distance measured from the given {@code referenceObject}.
     * @param referenceObject the reference object from which the distance is measured
     * @param objectProvider the provider of objects to add to the collection
     */
    public RankedSortedCollection(LocalAbstractObject referenceObject, ObjectProvider<? extends LocalAbstractObject> objectProvider) {
        this(referenceObject, referenceObject, objectProvider);
    }
}
