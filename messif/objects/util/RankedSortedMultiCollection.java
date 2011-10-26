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
import java.util.Comparator;

/**
 * Implementation of a sorted collection that stores additional collections.
 * The additional collections store the {@link RankedAbstractMetaObject} by
 * the respective sub-distance.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class RankedSortedMultiCollection extends RankedSortedCollection implements CollectionProvider<RankedAbstractObject> {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /** Internal list of additional collections */
    private final RankedSortedCollection[] sublists;

    /**
     * Constructs an empty collection with the specified initial and maximal capacity.
     * The order is defined using the natural order of items.
     * @param initialCapacity the initial capacity of the collection
     * @param maximalCapacity the maximal capacity of the collection
     * @param sublistCount number of additional collections
     * @throws IllegalArgumentException if the specified initial or maximal capacity is invalid
     */
    public RankedSortedMultiCollection(int initialCapacity, int maximalCapacity, int sublistCount) throws IllegalArgumentException {
        super (initialCapacity, maximalCapacity);
        sublists = new RankedSortedCollection[sublistCount];
        for (int i = 0; i < sublistCount; i++) {
            final int index = i;
            sublists[i] = new RankedSortedCollection(initialCapacity, maximalCapacity, new RankedSortedMultiCollectionComparator(index));
        }
    }

    /**
     * Returns the comparator used by the particular subcollection.
     * @param sublistIndex index of the particular sublist
     * @return the comparator used by the specific subcollection
     */
    public Comparator<? super RankedAbstractObject> getSublistComparator(int sublistIndex) {
        return sublists[sublistIndex].getComparator();
    }
    
    /**
     * Comparator based on the subdistances assigned to {@link RankedAbstractMetaObject}.
     * It used one of the subdistances to compare the objects.
     */
    public static class RankedSortedMultiCollectionComparator implements Comparator<RankedAbstractObject>,java.io.Serializable {
        /** Index to the subdistances array */
        private final int distanceIndex;
        
        /**
         * New comparator
         * @param index index of the subdistance to use for comparisons
         */
        public RankedSortedMultiCollectionComparator(int index) {
            distanceIndex = index;
        }
        
        @Override
        public int compare(RankedAbstractObject o1, RankedAbstractObject o2) {
                return Float.compare(((RankedAbstractMetaObject) o1).getSubDistances()[distanceIndex], ((RankedAbstractMetaObject) o2).getSubDistances()[distanceIndex]);
        }
    }
    
//    public RankedSortedMultiCollection(int initialCapacity, int maximalCapacity, RankedSortedCollection[] sublists) throws IllegalArgumentException {
//        super (initialCapacity, maximalCapacity);
//        this.sublists = sublists;
//        // Check the comparators of the collections passed in sublists
//        for (int i = 0; i < sublists.length; i++) {
//            if (sublists[i].getComparator() instanceof RankedSortedMultiCollectionComparator)
//                throw new IllegalArgumentException("Collection at index " + i + " does not have a proper comparator assigned. See documentation.");
//        }
//    }

    @Override
    public boolean add(RankedAbstractObject e) throws IllegalArgumentException {
        if (!(e instanceof RankedAbstractMetaObject) || ((RankedAbstractMetaObject)e).getSubDistances().length != sublists.length)
            throw new IllegalArgumentException("Multi collection can only process RankedAbstractMetaObject with " + sublists.length + " meta distances");
        boolean rtv = super.add(e);
        for (int j = 0; j < sublists.length; j++)
            sublists[j].add(e);
        return rtv;
    }

    @Override
    protected boolean remove(int index) {
        if (index >= 0 && index < size()) {
            RankedAbstractObject o = get(index);
            for (int j = 0; j < sublists.length; j++)
                sublists[j].remove(o);
        }
        
        return super.remove(index);
    }

    @Override
    public void clear() {
        super.clear();
        for (int i = 0; i < sublists.length; i++)
            sublists[i].clear();
    }

    @Override
    public int getCollectionCount() {
        return sublists.length;
    }

    @Override
    public Collection<RankedAbstractObject> getCollection(int index) throws IndexOutOfBoundsException {
        return Collections.unmodifiableCollection(sublists[index]);
    }
    
    @Override
    public Class<? extends RankedAbstractObject> getCollectionValueClass() {
        return RankedAbstractObject.class;
    }

    /**
     * Returns the number of additional collections.
     * @return the number of additional collections
     */
    public int getSublistCount() {
        return sublists.length;
    }
}
