/*
 * MeasuredAbstractObjectList.java
 *
 * Created on 19. kveten 2004, 19:03
 */

package messif.objects.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import messif.objects.AbstractObject;
import messif.objects.MeasuredAbstractObject;


/**
 *
 * @author  xbatko
 */
public class MeasuredAbstractObjectList<T extends AbstractObject> extends TreeSet<MeasuredAbstractObject<? extends T>> implements Serializable {
    
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    //****************** InternalIteration class ******************//
    
    private static class InternalIterator<T extends AbstractObject> extends AbstractObjectIterator<T> {
        private T currentObject = null;
        private final Iterator<MeasuredAbstractObject<? extends T>> pairEnum;
        public InternalIterator(Iterator<MeasuredAbstractObject<? extends T>> pairEnum) {
            this.pairEnum = pairEnum;
        }
        public boolean hasNext() { return pairEnum.hasNext(); }
        public T next() { return currentObject = pairEnum.next().getObject(); }
        public void remove() { throw new UnsupportedOperationException(); }

        /**
         * Returns an instance of object returned by the last call to next().
         * @return an instance of object returned by the last call to next()
         * @throws NoSuchElementException if next() has not been called yet.
         */
        public T getCurrentObject() throws NoSuchElementException {
            if (currentObject == null)
                throw new NoSuchElementException("Can't call getCurrentObject before next was called");
            return currentObject;
        }
    }
    
    //****************** Objects and distances list ******************//
    protected final int maxPairCount;
    
    
    /****************** Constructors ******************/
    
    /** Creates a new instance of MeasuredAbstractObjectList */
    public MeasuredAbstractObjectList() {
        this(Integer.MAX_VALUE);
    }

    /** Creates a new instance of MeasuredAbstractObjectList */
    public MeasuredAbstractObjectList(int maxPairCount) {
        this.maxPairCount = maxPairCount;
    }

    /** Creates a new instance of MeasuredAbstractObjectList */
    public MeasuredAbstractObjectList(Iterator<MeasuredAbstractObject<? extends T>> iterator) {
        this(Integer.MAX_VALUE, iterator);
    }

    /** Creates a new instance of MeasuredAbstractObjectList */
    public MeasuredAbstractObjectList(int maxPairCount, Iterator<MeasuredAbstractObject<? extends T>> iterator) {
        this(maxPairCount);
        while (iterator.hasNext()) {
            MeasuredAbstractObject<? extends T> measuredObj = iterator.next();
            add(measuredObj.getObject(), measuredObj.getDistance());
        }
    }

    /****************** Operations ******************/
    
    /** Add new measured object
     * @param object    object to add
     * @param distance  distance from the query object to the object being added
     * @return <code>true</code> if the object has been added. Otherwise <code>false</code>.
     */
    public boolean add(T object, float distance) {
        // Optimalization - do not add object, if array is full and the distance is greater than the last object
        if ((size() >= maxPairCount) && (distance >= getLastDistance()))
            return false;
        
        add(new MeasuredAbstractObject<T>(object, distance));
        return true;
    }

    public boolean add(MeasuredAbstractObject<? extends T> pair) {
        if (!super.add(pair))
            return false;

        // Remove overflown object
        if (size() > maxPairCount)
            remove(last());
        
        return true;
    }

    public boolean addAll(Collection<? extends MeasuredAbstractObject<? extends T>> source) {
        if (!super.addAll(source))
            return false;

        // Remove overflown objects
        while (size() > maxPairCount)
            remove(last());

        return true;
    }

    /** Add all objects from the other measured list
     * @param source  list of objects to be added to this
     * @return <code>true</code> if at least one object has been added. Otherwise <code>false</code>.
     */
    public boolean add(MeasuredAbstractObjectList<? extends T> source) {
        boolean retVal = false;
        for (MeasuredAbstractObject<? extends T> item : source) {
            if (add(item.getObject(), item.getDistance()))
                retVal = true;
        }
        return retVal;
    }
    
    /** Enumerate all stored measured object */
    public AbstractObjectIterator<T> objects() {
        return new InternalIterator<T>(iterator());
    }
    
    /** Get last stored measured object
     *  Throws NoSuchElementException if list is empty
     */
    public AbstractObject getLastObject() {
        return last().getObject();
    }
    
    /** Get last stored measured object distance 
     *  Throws NoSuchElementException if list is empty
     */
    public float getLastDistance() {
        return last().getDistance();
    }
    
}
