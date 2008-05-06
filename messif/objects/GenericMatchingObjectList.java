/*
 * GenericMatchingObjectList.java
 *
 * Created on 20. duben 2006, 15:01
 */

package messif.objects;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;


/**
 *
 * @author  xdohnal
 * @author  xbatko
 */
public class GenericMatchingObjectList<E extends AbstractObject> extends TreeMap<Integer,GenericAbstractObjectList<E>> implements Serializable, ObjectProvider {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    /****************** Constructors ******************/

    /** Creates a new instance of MatchingObjectList */
    public GenericMatchingObjectList() {
    }

    /** Creates a new instance of MatchingObjectList */
    public GenericMatchingObjectList(GenericAbstractObjectIterator<E> iterator) {
        this(iterator, 1);
    }

    /** Creates a new instance of MatchingObjectList */
    public GenericMatchingObjectList(GenericAbstractObjectIterator<E> iterator, int partId) {
        getPart(partId, true).addAll(iterator);
    }

    
    /****************** Parts ******************/
    
    public Set<Integer> getPartIDs() {
        return keySet();
    }
    
    protected GenericAbstractObjectList<E> getPart(int partId) { 
        return getPart(partId, false); 
    }
    
    protected GenericAbstractObjectList<E> getPart(int partId, boolean allocateNewIfMissing) {
        // Try to get the part partId
        GenericAbstractObjectList<E> part = get(partId);
        
        // If part not found
        if (part == null) {
            // If allocation requested
            if (allocateNewIfMissing) {
                part = new GenericAbstractObjectList<E>();
                put(partId, part);
            } else
                throw new NoSuchElementException("Part '" + partId + "' not found");
        }
        
        return part;
    }
    
    /** Returns number of object in the given part */
    public int objectCount(int partId) {
        // Try to get the part partId
        try {
            return getPart(partId, false).size();
        } catch (NoSuchElementException e) {
            return 0;
        }
    }
    

    /****************** List access ******************/

    /** Returns number of object in all parts */
    public int objectCount() {
        int rtv = 0;
        for (GenericAbstractObjectList<E> list : values())
           rtv += list.size();

        return rtv;
    }
    
    /** Get object with index position from part partId
     *  throws NoSuchElementException if part is not found
     *  throws ArrayIndexOutOfBoundsException if object index is out of bounds
     */
    public E getObject(int index, int partId) {
        return getPart(partId).get(index);
    }
    
    /** Get ID of object with index position from part partId
     *  throws NoSuchElementException if part is not found
     *  throws ArrayIndexOutOfBoundsException if object index is out of bounds
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
        GenericAbstractObjectList<E> part = getPart(partId);
        E obj = part.remove(index);
        
        if (part.size() == 0)
            removeAll(partId);

        return obj;
    }
    
    /** Remove object by index position from part partId
     *  throws NoSuchElementException if part is not found
     */
    public boolean remove(E object, int partId) { 
        GenericAbstractObjectList<E> part = getPart(partId);
        
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
    public GenericAbstractObjectIterator<E> iterator(int partId) {
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
    public GenericAbstractObjectList<E> objects(int partId) throws NoSuchElementException {
        return getPart(partId, false);
    }

    /**
     *  Returns iterator through all objects from all parts of this MatchingObjectList
     */
    public GenericAbstractObjectIterator<E> iterator() {
        return objects().iterator();
    }
    
    /**
     *  Returns list of all objects from all parts of this MatchingObjectList
     */
    public GenericAbstractObjectList<E> objects() {
        GenericAbstractObjectList<E> rtv = new GenericAbstractObjectList<E>();
        
        // Iterate through all parts
        for (GenericAbstractObjectList<E> list : values())
            rtv.addAll(list);
            
        return rtv;
    }

    /**
     * The iterator for provided objects for ObjectProvider interface.
     *
     * @return iterator for provided objects
     */
    public GenericObjectIterator<E> provideObjects() {
        return iterator();
    }


    /****************** String representation ******************/
    public String toString() {
        return "MatchingObjectList: " + super.toString();
    }

}
