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
import messif.operations.AnswerType;
import messif.operations.RankingQueryOperation;
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

    //****************** Attributes******************//

    /** Flag whether the add method checks if the object that {@link #isEqual} exists already in the collection */
    private boolean ignoringDuplicates;


    //****************** Constructors ******************//

    /**
     * Constructs an empty collection with the specified initial and maximal capacity.
     * @param initialCapacity the initial capacity of the collection
     * @param maximalCapacity the maximal capacity of the collection
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
     * @param maximalCapacity the maximal capacity of the collection
     * @throws IllegalArgumentException if the specified initial or maximal capacity is invalid
     */
    public RankedSortedCollection(int initialCapacity, int maximalCapacity) throws IllegalArgumentException {
        super(initialCapacity, maximalCapacity, null);
    }

    /**
     * Constructs an empty collection.
     * The order is defined using the natural order of items.
     * The initial capacity of the collection is set to {@link #DEFAULT_INITIAL_CAPACITY}
     * and maximal capacity is not limited.
     */
    public RankedSortedCollection() {
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

    /**
     * Constructor from an existing operation - all parameters are copied from the operation answer.
     * @param operation operation with collection to copy all parameters from
     */
    public RankedSortedCollection(RankingQueryOperation operation) {
        super(operation.getAnswerCount(), operation.getAnswerMaximalCapacity(), operation.getAnswerComparator());
    }


    //****************** Attribute access methods ******************//

    @Override
    public void setMaximalCapacity(int capacity) {
        super.setMaximalCapacity(capacity);
    }

    /**
     * Set the flag whether the add method checks if the object that {@link #isEqual} exists already in the collection.
     * @param ignoringDuplicates the flag whether the add method checks for duplicate objects
     */
    public void setIgnoringDuplicates(boolean ignoringDuplicates) {
        this.ignoringDuplicates = ignoringDuplicates;
    }

    /**
     * Returns the flag whether the add method checks if the object that {@link #isEqual} exists already in the collection.
     * @return the flag whether the add method checks for duplicate objects
     */
    public boolean isIgnoringDuplicates() {
        return ignoringDuplicates;
    }


    //****************** Overrides ******************//

    @Override
    protected boolean add(RankedAbstractObject e, int index) {
        if (ignoringDuplicates && !isEmpty()) {
            for (int i = index; i >= 0; i--) {
                RankedAbstractObject objI = get(i);
                if (objI.getDistance() != e.getDistance())
                    break;
                if (isEqual(e.getObject(), objI.getObject()))
                    return false;
            }
            for (int i = index + 1; i < size(); i++) {
                RankedAbstractObject objI = get(i);
                if (objI.getDistance() != e.getDistance())
                    break;
                if (isEqual(e.getObject(), objI.getObject()))
                    return false;
            }
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
    protected boolean isEqual(AbstractObject e1, AbstractObject e2) {
        String e1Locator = e1.getLocatorURI();
        String e2Locator = e2.getLocatorURI();
        if (e1Locator != null && e2Locator != null) {
            return e1Locator.equals(e2Locator);
        }
        if (e1Locator == null && e2Locator == null && e1 instanceof LocalAbstractObject && e2 instanceof LocalAbstractObject) {
            return ((LocalAbstractObject) e1).dataEquals((LocalAbstractObject) e2);
        }
        return false;
    }


    //****************** Distance ranked add ******************//

    /**
     * Add a distance-ranked object to this collection.
     * The information about distances of the respective sub-objects is preserved
     * using {@link RankedAbstractMetaObject} if the given {@code objectDistances}
     * array is not <tt>null</tt>.
     * @param answerType the type of the objects added to this collection
     * @param object the object to add
     * @param distance the distance of object
     * @param objectDistances the array of distances to the respective sub-objects (can be <tt>null</tt>)
     * @return the distance-ranked object that was added to this collection or <tt>null</tt> if the object was not added
     * @throws IllegalArgumentException if the answer type of this operation requires cloning but the passed object cannot be cloned
     */
    public RankedAbstractObject add(AnswerType answerType, AbstractObject object, float distance, float[] objectDistances) {
        RankedAbstractObject rankedObject = rankObject(answerType, object, distance, objectDistances);
        return add(rankedObject) ? rankedObject : null;
    }

    /**
     * Internal method that creates a distance-ranked object for adding to this collection.
     * The information about distances of the respective sub-objects is preserved
     * using {@link RankedAbstractMetaObject} if the given {@code objectDistances}
     * array is not <tt>null</tt>.
     * 
     * @param answerType the type of the objects added to this collection
     * @param object the object to add
     * @param distance the distance of object
     * @param objectDistances the array of distances to the respective sub-objects (can be <tt>null</tt>)
     * @return the distance-ranked object that can be added to this collection
     * @throws IllegalArgumentException if the answer type of this operation requires cloning but the passed object cannot be cloned
     */
    protected RankedAbstractObject rankObject(AnswerType answerType, AbstractObject object, float distance, float[] objectDistances) {
        if (object == null)
            return null;

        RankedAbstractObject rankedObject;
        try {
            // Create the ranked object encapsulation
            if (objectDistances == null)
                rankedObject = new RankedAbstractObject(answerType.update(object), distance);
            else
                rankedObject = new RankedAbstractMetaObject(answerType.update(object), distance, objectDistances);
        } catch (CloneNotSupportedException e) {
            throw new IllegalArgumentException(e);
        }

        return rankedObject;
    }
}
