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
import messif.operations.RankingQueryOperation;
import messif.utility.SortedCollection;

/**
 * Specialization of {@link SortedCollection} that is specific for distance-ranked objects.
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class RankedSortedCollectionNoDups extends RankedSortedCollection  {
    
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
    public RankedSortedCollectionNoDups(int initialCapacity, int maximalCapacity, Comparator<? super RankedAbstractObject> comparator) throws IllegalArgumentException {
        super(initialCapacity, maximalCapacity, comparator);
    }

    /**
     * Constructs an empty collection with the specified initial and maximal capacity.
     * The order is defined using the natural order of items.
     * @param initialCapacity the initial capacity of the collection
     * @param maximalCapacity the maximal capatity of the collection
     * @throws IllegalArgumentException if the specified initial or maximal capacity is invalid
     */
    public RankedSortedCollectionNoDups(int initialCapacity, int maximalCapacity) throws IllegalArgumentException {
        super(initialCapacity, maximalCapacity, null);
    }

    /**
     * Constructs an empty collection.
     * The order is defined using the natural order of items.
     * The initial capacity of the collection is set to 16 and maximal capacity
     * is not limited.
     * @throws IllegalArgumentException if the specified initial or maximal capacity is invalid
     */
    public RankedSortedCollectionNoDups() throws IllegalArgumentException {
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
    public <T extends AbstractObject> RankedSortedCollectionNoDups(DistanceFunction<? super T> distanceFunction, T referenceObject, Iterator<? extends T> iterator) {
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
    public <T extends AbstractObject> RankedSortedCollectionNoDups(DistanceFunction<? super T> distanceFunction, T referenceObject, ObjectProvider<? extends T> objectProvider) {
        this(distanceFunction, referenceObject, (Iterator<? extends T>)objectProvider.provideObjects());
    }

    /**
     * Creates a new collection filled with objects provided by the {@code iterator}.
     * Objects are ranked by the distance measured from the given {@code referenceObject}.
     * @param referenceObject the reference object from which the distance is measured
     * @param iterator the iterator on objects to add to the collection
     */
    public RankedSortedCollectionNoDups(LocalAbstractObject referenceObject, Iterator<? extends LocalAbstractObject> iterator) {
        this(referenceObject, referenceObject, iterator);
    }

    /**
     * Creates a new collection filled with objects provided by the {@code objectProvider}.
     * Objects are ranked by the distance measured from the given {@code referenceObject}.
     * @param referenceObject the reference object from which the distance is measured
     * @param objectProvider the provider of objects to add to the collection
     */
    public RankedSortedCollectionNoDups(LocalAbstractObject referenceObject, ObjectProvider<? extends LocalAbstractObject> objectProvider) {
        this(referenceObject, referenceObject, objectProvider);
    }
    
    /**
     * Constructor from an existing operation - all parameters are copied from the operation answer.
     * @param operation operation with collection to copy all parameters from
     */
    public RankedSortedCollectionNoDups(RankingQueryOperation operation) {
        super(operation);
    }
    
    @Override
    public boolean add(RankedAbstractObject e) {
        if (isEmpty()) {
            return super.add(e, 0);
        }        
        int index = binarySearch(e, 0, size() - 1, false);
        if (index < size() && areEqual(e.getObject(), get(index).getObject()) || (index > 0 && areEqual(e.getObject(), get(index - 1).getObject()))) {
            return false;
        }
        
        return super.add(e, index);
    }
    
    /**
     * Given two objects, this method compares them according to locator (if not null) and, if both locators null,
     *  then according to {@link LocalAbstractObject#dataEquals(java.lang.Object)}.
     * @param e1 first object
     * @param e2 second object
     * @return <b>true</b> only if the locators are not null and equal or both null and data are equal
     */
    protected boolean areEqual(AbstractObject e1, AbstractObject e2) {
        if (e1.getLocatorURI() != null && e2.getLocatorURI() != null) {
            return e1.getLocatorURI().equals(e2.getLocatorURI());
        }
        if (e1.getLocatorURI() == null &&  e2.getLocatorURI() == null && e1 instanceof LocalAbstractObject && e2 instanceof LocalAbstractObject) {
            return ((LocalAbstractObject) e1).dataEquals((LocalAbstractObject) e2);
        }
        return false;
    }
    

}
