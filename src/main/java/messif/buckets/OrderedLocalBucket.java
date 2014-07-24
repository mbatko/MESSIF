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
package messif.buckets;

import messif.buckets.index.ModifiableOrderedIndex;
import messif.buckets.index.OrderedIndex;
import messif.objects.LocalAbstractObject;

/**
 * An extension of {@link LocalBucket} that maintains the stored objects in
 * a certain order.
 * 
 * @param <C> type of the keys that this bucket's objects are ordered by
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class OrderedLocalBucket<C> extends LocalBucket {
    /** class serial id for serialization */
    private static final long serialVersionUID = 934001L;

    /**
     * Constructs a new LocalBucket instance and setups all bucket limits
     *
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     * @param occupation the actual bucket occupation in either bytes or object count (see occupationAsBytes flag)
     */
    protected OrderedLocalBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes, long occupation) {
        super(capacity, softCapacity, lowOccupation, occupationAsBytes, occupation);
    }

    @Override
    public OrderedIndex<C, LocalAbstractObject> getIndex() {
        // Update statistics
        counterBucketRead.add(this);
        
        return getModifiableIndex();
    }

    @Override
    protected abstract ModifiableOrderedIndex<C, LocalAbstractObject> getModifiableIndex();

}
