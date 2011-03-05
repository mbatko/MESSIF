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

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import messif.objects.AbstractObject;
import messif.objects.ObjectProvider;
import messif.objects.UniqueID;


/**
 * List of objects returned by using {@link ObjectMatcher}.
 *
 * @param <E> the class of the objects in this list
 * @see AbstractObjectIterator#getMatchingObjects(messif.objects.util.ObjectMatcher, boolean, int[])
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class GenericMatchingObjectList<E extends AbstractObject> extends TreeMap<Integer,AbstractObjectList<E>> implements Serializable, ObjectProvider {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Constructors ******************//

    /**
     * Creates a new instance of MatchingObjectList that is empty.
     */
    public GenericMatchingObjectList() {
    }

    /**
     * Creates a new instance of MatchingObjectList filled with objects from the iterator.
     * The objects are added to partition identified by 1.
     * @param iterator the iterator that provides the objects to fill
     */
    public GenericMatchingObjectList(AbstractObjectIterator<E> iterator) {
        this(iterator, 1);
    }

    /**
     * Creates a new instance of MatchingObjectList filled with objects from the iterator.
     * @param iterator the iterator that provides the objects to fill
     * @param partId the identification of the partition of this list to fill
     */
    public GenericMatchingObjectList(AbstractObjectIterator<E> iterator, int partId) {
        getPart(partId, true).addAll(iterator);
    }


    //****************** Parts ******************//

    /**
     * Returns all partition identifiers that are set in this list.
     * @return all partition identifiers that are set in this list
     */
    public Set<Integer> getPartIDs() {
        return keySet();
    }

    /**
     * Returns a list of objects in the partition identified by the given {@code partId}.
     * Note that the returned list can be used to directly modify the encapsulated data.
     * 
     * @param partId the identification of the partition of this list to retrieve
     * @return a list of objects in the given partition or <tt>null</tt> if
     *      there is no list for the given {@code partId}
     */
    protected AbstractObjectList<E> getPart(int partId) { 
        return getPart(partId, false); 
    }

    /**
     * Returns a list of objects in the partition identified by the given {@code partId}.
     * Note that the returned list can be used to directly modify the encapsulated data.
     *
     * @param partId the identification of the partition of this list to retrieve
     * @param allocateNewIfMissing if <tt>true</tt>, a new list is allocated if there
     *      there was none for the given {@code partId}
     * @return a list of objects in the given partition or <tt>null</tt> if
     *      there is no list for the given {@code partId} and {@code allocateNewIfMissing} is <tt>false</tt>
     */
    protected AbstractObjectList<E> getPart(int partId, boolean allocateNewIfMissing) {
        // Try to get the part partId
        AbstractObjectList<E> part = get(partId);
        
        // If part not found
        if (part == null) {
            // If allocation requested
            if (allocateNewIfMissing) {
                part = new AbstractObjectList<E>();
                put(partId, part);
            } else
                throw new NoSuchElementException("Part '" + partId + "' not found");
        }
        
        return part;
    }

    /**
     * Returns the number of objects in the given partition.
     * @param partId the identification of the partition of this list to retrieve
     * @return the number of objects if partition exists or zero otherwise
     */
    public int getObjectCount(int partId) {
        // Try to get the part partId
        try {
            return getPart(partId, false).size();
        } catch (NoSuchElementException e) {
            return 0;
        }
    }


    //****************** List access ******************//

    /**
     * Returns the number of objects in all partitions.
     * @return the number of objects in all partitions
     */
    public int getObjectCount() {
        int rtv = 0;
        for (AbstractObjectList<E> list : values())
           rtv += list.size();

        return rtv;
    }

    /**
     * Returns the object at position {@code index} of the partition {@code partId}.
     * @param index the index of object within the given partition
     * @param partId the identification of the partition from which to retrieve the object
     * @return the object at position {@code index} of the part {@code partId}
     * @throws NoSuchElementException if the partition {@code partId} is not found
     * @throws IndexOutOfBoundsException if object {@code index} is invalid
     */
    public E getObject(int index, int partId) throws NoSuchElementException, IndexOutOfBoundsException {
        return getPart(partId).get(index);
    }

    /**
     * Returns the ID of the object at position {@code index} of the partition {@code partId}.
     * @param index the index of object within the given partition
     * @param partId the identification of the partition from which to retrieve the object
     * @return the ID of the object at position {@code index} of the part {@code partId}
     * @throws NoSuchElementException if the partition {@code partId} is not found
     * @throws IndexOutOfBoundsException if object {@code index} is invalid
     */
    public UniqueID getObjectID(int index, int partId) throws NoSuchElementException, IndexOutOfBoundsException { 
        return getObject(index, partId).getObjectID();
    }

    /**
     * Adds an object to the specified partition.
     * @param object the object to add
     * @param partId the identification of the partition from which to retrieve the object
     */
    public void add(E object, int partId) {
        getPart(partId, true).add(object);
    }

    /**
     * Inserts an object at the specified position of the specified partition.
     * @param index the index in the specified partition where to insert the object
     * @param object the object to insert
     * @param partId the identification of the partition from which to retrieve the object
     * @throws IndexOutOfBoundsException if object {@code index} is invalid
     */
    public void add(int index, E object, int partId) throws IndexOutOfBoundsException {
        getPart(partId, true).add(index, object);
    }

    /**
     * Removes the object at specified {@code index} of partition {@code partId}.
     * @param index the index of object within the given partition
     * @param partId the identification of the partition from which to remove the object
     * @return the removed object
     * @throws NoSuchElementException if the partition {@code partId} is not found
     * @throws IndexOutOfBoundsException if object {@code index} is invalid
     */
    public E remove(int index, int partId) throws NoSuchElementException, IndexOutOfBoundsException {
        AbstractObjectList<E> part = getPart(partId);
        E obj = part.remove(index);
        
        if (part.size() == 0)
            removeAll(partId);

        return obj;
    }

    /**
     * Removes the first occurrence of the specified object from partition {@code partId}.
     * Note that the {@link Object#equals(java.lang.Object) equals} method is used
     * to locate the object to remove.
     * @param object the object to remove
     * @param partId the identification of the partition from which to remove the object
     * @return <tt>true</tt> if the object was removed or <tt>false</tt> if the
     *      object was not found (partition is then not modified)
     * @throws NoSuchElementException if the partition {@code partId} is not found
     * @throws IndexOutOfBoundsException if object {@code index} is invalid
     */
    public boolean remove(E object, int partId) { 
        AbstractObjectList<E> part = getPart(partId);
        
        if (!part.remove(object)) 
            return false;
        
        if (part.size() == 0)
            removeAll(partId);

        return true;
    }

    /**
     * Removes all objects from the given partition {@code partId}.
     * @param partId the identification of the partition from which to remove all objects
     */
    public void removeAll(int partId) {
        remove(partId);
    }


    //****************** Iterators ******************//

    /**
     * Returns an iterator for objects of the specified partition of this list.
     * @param partId the identification of the partition the iterator of which to retrieve
     * @return an iterator for objects of the specified partition
     * @throws NoSuchElementException if the partition {@code partId} is not found
     */
    public AbstractObjectIterator<E> iterator(int partId) throws NoSuchElementException {
        try {
            return getPart(partId).iterator();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    /**
     * Returns an iterator for all objects of all partitions of this list.
     * @return an iterator for all objects
     */
    public AbstractObjectIterator<E> iterator() {
        return objects().iterator();
    }

    /**
     * Returns a list of all objects from all partitions.
     * The modifications in the returned list are <em>not</em> reflected in this list.
     * @return a list of all objects from all partitions
     */
    public AbstractObjectList<E> objects() {
        AbstractObjectList<E> rtv = new AbstractObjectList<E>();
        
        // Iterate through all parts
        for (AbstractObjectList<E> list : values())
            rtv.addAll(list);
            
        return rtv;
    }

    @Override
    public AbstractObjectIterator<E> provideObjects() {
        return iterator();
    }


    //****************** String representation ******************//

    @Override
    public String toString() {
        return "MatchingObjectList: " + super.toString();
    }

}
