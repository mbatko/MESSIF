/*
 * ObjectList.java
 *
 * Created on 3. kveten 2005, 11:22
 */

package messif.objects.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import messif.objects.AbstractObject;
import messif.objects.ObjectProvider;
import messif.objects.UniqueID;


/**
 * Resizable-array for storing AbstractObjects or their descendants.
 * All list operations are implemented and additional support for
 * building randomly selected AbstracObject lists is provided.
 *
 * Additionally, the list returns <code>GenericObjectIterator</code>
 * through <code>iterator</code> method.
 * 
 * @param <E> the type of abstract objects stored in this list
 */
public class AbstractObjectList<E extends AbstractObject> extends ArrayList<E> implements Serializable, ObjectProvider<E> {
    
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    /****************** Constructors ******************/
    
    /**
     * Constructs an empty AbstractObject list with the specified initial capacity.
     *
     * @param capacity the initial capacity of the list
     * @exception IllegalArgumentException if the specified initial capacity is negative
     */
    public AbstractObjectList(int capacity) throws IllegalArgumentException {
        super(capacity);
    }
    
    /**
     * Constructs an empty AbstractObject list with an initial capacity of ten.
     */
    public AbstractObjectList() {
    }
    
    /**
     * Constructs an AbstractObject list containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     *
     * @param source the collection whose elements are to be placed into this list
     * @throws NullPointerException if the specified collection is null
     */
    public AbstractObjectList(Collection<? extends E> source) throws NullPointerException {
        super(source);
    }
    
    /**
     * Constructs an AbstractObject list containing maximally <code>count</code>
     * elements returned by the specified iterator (in that order).
     *
     * @param iterator the iterator returing elements that are to be placed into this list
     * @param count maximal number of objects that are placed from iterator
     *              (negative number means unlimited)
     * @throws NullPointerException if the specified iterator is null
     */
    public AbstractObjectList(Iterator<? extends E> iterator, int count) throws NullPointerException {
        super((count > 0)?count:10);
        while (iterator.hasNext() && (count-- != 0))
            add(iterator.next());
    }

    /**
     * Constructs an AbstractObject list containing all
     * elements returned by the specified iterator (in that order).
     *
     * @param iterator the iterator returing elements that are to be placed into this list
     * @throws NullPointerException if the specified iterator is null
     */
    public AbstractObjectList(Iterator<? extends E> iterator) throws NullPointerException {
        this(iterator, -1);
    }

    
    /****************** Additional access methods ******************/

    /**
     * Returns the ID of object at specified position.
     *
     * @param index position of object in the list
     * @return the ID of object at position <code>index</code>
     */
    public UniqueID getObjectID(int index) { 
        return get(index).getObjectID();
    }

    /**
     * Returns an iterator over the elements in this list in proper sequence.
     *
     * @return an iterator over the elements in this list in proper sequence
     */
    @Override
    public AbstractObjectIterator<E> iterator() {
        final Iterator<E> iterator = super.iterator();
        return new AbstractObjectIterator<E>() {
            protected E currentObject = null;
            @Override
            public E getCurrentObject() throws NoSuchElementException {
                if (currentObject == null)
                    throw new NoSuchElementException("Can't call getCurrentObject before next was called");
                return currentObject;
            }
            public boolean hasNext() {
                return iterator.hasNext();
            }
            public E next() {
                return currentObject = iterator.next();
            }
            public void remove() {
                iterator.next();
            }
        };
    }

    /**
     * The iterator for provided objects for ObjectProvider interface.
     *
     * @return iterator for provided objects
     */
    public AbstractObjectIterator<E> provideObjects() {
        return iterator();
    }

    /**
     * Appends all of the elements that can be retrieved from the specified
     * iterator to the end of this list.
     *
     * @param iterator iterator over elements to be added to this list
     */
    public void addAll(Iterator<E> iterator) {
        if (iterator != null)
            while (iterator.hasNext())
                add(iterator.next());
    }


    /****************** Equality driven by object data ******************/

    /** 
     * Indicates whether some other object has the same data as this one.
     * @param   obj   the reference object with which to compare.
     * @return  <code>true</code> if this object has the same data as the obj
     *          argument; <code>false</code> otherwise.
     */
    public boolean dataEquals(Object obj) {
        // We must compare with other collection
        if (!(obj instanceof Collection))
            return false;
        Collection<?> castObj = (Collection<?>)obj;

        // Check size
        if (size() != castObj.size())
            return false;

        // Iterate through objects in this list and the target collection
        Iterator<?> iterator = castObj.iterator();
        for (E localObject : this) {
            // Check their data equality
            if (!localObject.getLocalAbstractObject().dataEquals(iterator.next()))
                return false;
        }

        return true;
    }

    /**
     * Returns a hash code value for the data of this list.
     * @return a hash code value for the data of this list
     */
    public int dataHashCode() {
	int hashCode = 1;
	for (E obj : this) {
	    hashCode = 31*hashCode + (obj==null ? 0 : obj.getLocalAbstractObject().dataHashCode());
	}
	return hashCode;
    }


    /****************** String representation ******************/    
    
    /**
     * Returns a string representation of this collection of objects.
     *
     * @return a string representation of this collection of objects
     */
    @Override
    public String toString() {
        return "GenericAbstractObjectList " + super.toString();
    }

    
    /******************* Random lists *************************/
    
    /** Returns a list containing randomly choosen objects from this list.
     * If the uniqueness of objects in the retrieved list is not required, the number of objects
     * in the response is equal to 'count'. If unique list is requested the number of objects
     * can vary from 0 to 'count' and depends on the number of objects in this list. When this list
     * consists of fewer objects than 'count' the whole list is returned at any case.
     * The returned instance is exactly the same as passed in the parameter list. Chosen objects are 
     * only added to this list.
     * If the passed list contains some objects they are left untouched.
     *
     * @param <T> the list class that receives random objects
     * @param count   Number of object to return.
     * @param unique  Flag if returned list contains each object only once.
     * @param list    An instance of a class extending ObjectList<E> used to carry selected objects.
     *
     * @return the instance passed in list which contains randomly selected objects as requested
     */
    public <T extends List<E>> T randomList(int count, boolean unique, T list) {
        if (list == null)
            return null;
        
        // Ignore all previous elements in the list, just use it as it is empty.
        List<E> resList = list.subList(list.size(), list.size());
        
        // unique list is requested
        if (unique) {
            if (count >= size()) {
                resList.addAll(this);
                return list;
            }
            
            // now, count is less than the length of this list
            
            // we must pick fewer objects than the remaining objects
            if (count <= size() - count) {
                while (count > 0) {
                    E obj = get( (int)(Math.random() * (double)size()) );
                    
                    if (!resList.contains(obj)) {          // selected object is not in the response, add it
                        resList.add(obj);
                        --count;
                    }
                }
            }
            // we do not pick fewer objects than the number of objects we must pick (it is better to delete objects than to insert them).
            else {
                int[] toAdd = new int[size()];
                Arrays.fill(toAdd, 1);

                count = size() - count;          // how many objects to delete
                while (count > 0) {
                    int idx = (int)(Math.random() * (double)size());
                    
                    if (toAdd[idx] != 0) {
                        toAdd[idx] = 0;
                        --count;
                    }
                }
                
                for (int i = 0; i < toAdd.length; i++) {
                    if (toAdd[i] != 0)
                        resList.add( get(i) );
                }
            }
        } 
        // duplicate scan appear
        else {
            while (count > 0) {
                resList.add( get( (int)(Math.random() * (double)size()) ) );
                --count;
            }
        }
        
        // return the list of selected objects
        return list;
    }
    

    /** Returns a list containing randomly choosen objects from the passed iterator.
     * If the uniqueness of objects in the retrieved list is not required, the number of objects
     * in the response is equal to 'count'. If a unique list is requested, the number of objects
     * can vary from zero to 'count' and depends on the number of objects in the passed iterator. 
     * When the iterator consists of fewer objects than 'count', all objects are returned at any case.
     *
     * The returned instance is exactly the same as passed in the parameter 'list'. Chosen objects are 
     * only added to this list. If the passed list contains some objects they are left untouched.
     *
     * @param <F> the class of objects that are stored in the list
     * @param <T> the list class that receives random objects
     * @param count      Number of object to return.
     * @param unique     Flag if returned list contains each object only once.
     * @param list       An instance of a class extending ObjectList<E> used to carry selected objects.
     * @param iterSource Iterator from which objects are randomly picked.
     *
     * @return the instance passed in list which contains the randomly selected objects as requested
     */
    public static <F extends AbstractObject, T extends List<F>> T randomList(int count, boolean unique, T list, Iterator<F> iterSource) {
        if (list == null || iterSource == null)
            return null;
        
        // Ignore all previous elements in the list, just use it as it is empty.
        List<F> resList = list.subList(list.size(), list.size());
        
        // Append objects upto count
        while (count > 0 && iterSource.hasNext()) {
            resList.add(iterSource.next());
            count--;
        }
        
        // the iterator didn't contain more elements than count
        if (count > 0)
            return list;
        // Reset count back (this value is exactly the same as the passed one!!!!)
        count = resList.size();
        
        
        // unique list is requested
        if (unique) {
            // Number of objects in the iterator
            int iterCount = count;
            
            // First, test the uniqueness of objects already in the list
            Iterator<F> it = resList.iterator();
            for ( ; it.hasNext(); iterCount++) {
                F o = it.next();
                if (resList.contains(o))
                    it.remove();
            }
            
            // Append objects from iterator until the quorum is achieved.
            for ( ; resList.size() < count && iterSource.hasNext(); iterCount++) {
                F o = iterSource.next();
                if (! resList.contains(o))
                    resList.add(o);
            }
            
            // If we have not enough object, exit prematurely.
            if (resList.size() != count)
                return list;
            
            // We added count objects. Continue replacing them while there are some objects in the iterator.
            for ( ; iterSource.hasNext(); iterCount++) {
                F o = iterSource.next();            // Get an object and move to next
                int idx = (int)(Math.random() * (double)(iterCount + 1));
                if (idx == iterCount) {
                    int replace = (int)(Math.random() * count);
                    if (! resList.contains(o))
                        resList.set( replace, o );
                }
            }
        } 
        // duplicate scan appear
        else {
            // Number of objects in the iterator
            int iterCount = count;
            
            for ( ; iterSource.hasNext(); iterCount++) {
                F o = iterSource.next();            // Get an object and move to next
                int idx = (int)(Math.random() * (double)(iterCount + 1));
                if (idx == iterCount) {
                    int replace = (int)(Math.random() * count);
                    resList.set( replace, o );
                }
            }
        }
        
        // return the list of selected objects
        return list;
    }

    /**
     * Returns a list containing randomly choosen objects from the passed iterator.
     * If the uniqueness of objects in the retrieved list is not required, the number of objects
     * in the response is equal to 'count'. If a unique list is requested, the number of objects
     * can vary from zero to 'count' and depends on the number of objects in the passed iterator. 
     * When the iterator consists of fewer objects than 'count', all objects are returned at any case.
     *
     * The returned instance is a new AbstractObjectList with the iterator's type of objects.
     *
     * @param <F> the class of objects that are stored in the list
     * @param count      Number of object to return.
     * @param unique     Flag if returned list contains each object only once.
     * @param iterSource Iterator from which objects are randomly picked.
     *
     * @return the instance passed in list which contains the randomly selected objects as requested
     */
    public static <F extends AbstractObject> AbstractObjectList<F> randomList(int count, boolean unique, Iterator<F> iterSource) {
        return randomList(count, unique, new AbstractObjectList<F>(count), iterSource);
    }
    
}