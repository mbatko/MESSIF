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
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class GenericMatchingObjectList<E extends AbstractObject> extends TreeMap<Integer,AbstractObjectList<E>> implements Serializable, ObjectProvider {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    //****************** Constructors ******************//

    /** Creates a new instance of MatchingObjectList */
    public GenericMatchingObjectList() {
    }

    /** Creates a new instance of MatchingObjectList */
    public GenericMatchingObjectList(AbstractObjectIterator<E> iterator) {
        this(iterator, 1);
    }

    /** Creates a new instance of MatchingObjectList */
    public GenericMatchingObjectList(AbstractObjectIterator<E> iterator, int partId) {
        getPart(partId, true).addAll(iterator);
    }

    
    //****************** Parts ******************//
    
    public Set<Integer> getPartIDs() {
        return keySet();
    }
    
    protected AbstractObjectList<E> getPart(int partId) { 
        return getPart(partId, false); 
    }
    
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
    
    /** Returns number of object in the given part.
     * @param partId partition id
     * @return number of objects if partition exists, otherwise zero
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

    /** Returns number of object in all parts
     * @return number of objects
     */
    public int getObjectCount() {
        int rtv = 0;
        for (AbstractObjectList<E> list : values())
           rtv += list.size();

        return rtv;
    }
    
    /** Get object with index position from part partId
     *  @throws NoSuchElementException if part is not found
     *  @throws ArrayIndexOutOfBoundsException if object index is out of bounds
     */
    public E getObject(int index, int partId) {
        return getPart(partId).get(index);
    }
    
    /** Get ID of object with index position from part partId
     *  @throws NoSuchElementException if part is not found
     *  @throws ArrayIndexOutOfBoundsException if object index is out of bounds
     */
    public UniqueID getObjectID(int index, int partId) { 
        return getObject(index, partId).getObjectID();
    }

    /** Add object to a specified part */
    public void add(E object, int partId) {
        getPart(partId, true).add(object);
    }

    /** Insert object on a specified index position to a specified part */
    public void add(int index, E object, int partId) {
        getPart(partId, true).add(index, object);
    }
    
    /** Remove specified object from part partId
     *  throws NoSuchElementException if part is not found
     */
    public E remove(int index, int partId) {
        AbstractObjectList<E> part = getPart(partId);
        E obj = part.remove(index);
        
        if (part.size() == 0)
            removeAll(partId);

        return obj;
    }
    
    /** Remove object by index position from part partId
     *  throws NoSuchElementException if part is not found
     */
    public boolean remove(E object, int partId) { 
        AbstractObjectList<E> part = getPart(partId);
        
        if (!part.remove(object)) 
            return false;
        
        if (part.size() == 0)
            removeAll(partId);

        return true;
    }
    
    public void removeAll(int partId) {
        remove(partId);
    }


    /****************** Iterators ******************/
    
    /**
     *  Returns iterator through objects from the specified part of this MatchingObjectList
     *  throws NoSuchElementException if specified part cannot be found
     */
    public AbstractObjectIterator<E> iterator(int partId) {
        try {
            return getPart(partId).iterator();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    /**
     * Returns list of all objects from given part of this MatchingObjectList.
     * @throws NoSuchElementException if the part does not exist
     */
    public AbstractObjectList<E> objects(int partId) throws NoSuchElementException {
        return getPart(partId, false);
    }

    /**
     *  Returns iterator through all objects from all parts of this MatchingObjectList
     */
    public AbstractObjectIterator<E> iterator() {
        return objects().iterator();
    }
    
    /**
     *  Returns list of all objects from all parts of this MatchingObjectList
     */
    public AbstractObjectList<E> objects() {
        AbstractObjectList<E> rtv = new AbstractObjectList<E>();
        
        // Iterate through all parts
        for (AbstractObjectList<E> list : values())
            rtv.addAll(list);
            
        return rtv;
    }

    /**
     * The iterator for provided objects for ObjectProvider interface.
     *
     * @return iterator for provided objects
     */
    public AbstractObjectIterator<E> provideObjects() {
        return iterator();
    }


    /****************** String representation ******************/
    public String toString() {
        return "MatchingObjectList: " + super.toString();
    }

}
