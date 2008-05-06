/*
 * MeasuredAbstractObjectList.java
 *
 * Created on 19. kveten 2004, 19:03
 */

package messif.objects;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.TreeSet;


/**
 *
 * @author  xbatko
 */
public class MeasuredAbstractObjectList<T extends AbstractObject> extends TreeSet<MeasuredAbstractObjectList.Pair<T>> implements Serializable {
    
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    /****************** Pair class ******************/

    public static class Pair<T extends AbstractObject> implements Comparable<Pair>, Serializable {

        /** Class serial id for serialization */
        private static final long serialVersionUID = 1L;
    
        /****************** Pair attributes ******************/
        protected final T object;
        protected final float distance;
        /****************** Pair constructor ******************/
        public Pair(T object, float distance) {
            this.object = object;
            this.distance = distance;
        }
        
        public int compareTo(Pair o) {
            if (distance < o.distance) return -1;
            if (distance > o.distance) return 1;
            if (object == null)
                return (o.object == null)?0:-1;
            if (o.object == null)
                return 1;
            return object.compareTo(o.object);
        }
        
        public boolean equals(Object obj) {
            try {
                Pair pair = (Pair) obj;
                if (pair.distance != this.distance)
                    return false;
                if (object == null)
                    return pair.object == null;
                if (pair.object == null)
                    return false;
                return this.object.equals(pair.object);
            } catch (ClassCastException e) {
                return false;
            }
        }
        
        public String toString() {
            return "<" + distance + ": " + object + ">";
        }
        
        public T getObject() {
            return object;
        }
        
        public float getDistance() {
            return distance;
        }

    }
    
    /****************** InternalIteration class ******************/
    
    private static class InternalIterator<T extends AbstractObject> extends GenericObjectIterator<T> {
        private T currentObject = null;
        private final Iterator<Pair<T>> pairEnum;
        public InternalIterator(Iterator<Pair<T>> pairEnum) {
            this.pairEnum = pairEnum;
        }
        public boolean hasNext() { return pairEnum.hasNext(); }
        public T next() { return currentObject = pairEnum.next().object; }
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
    
    /****************** Objects and distances list ******************/
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
    public MeasuredAbstractObjectList(Iterator<Pair<T>> iterator) {
        this(Integer.MAX_VALUE, iterator);
    }

    /** Creates a new instance of MeasuredAbstractObjectList */
    public MeasuredAbstractObjectList(int maxPairCount, Iterator<Pair<T>> iterator) {
        this(maxPairCount);
        while (iterator.hasNext()) {
            Pair<T> pair = iterator.next();
            add(pair.getObject(), pair.getDistance());
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
        
        add(new Pair<T>(object, distance));
        return true;
    }

    public boolean add(MeasuredAbstractObjectList.Pair<T> pair) {
        if (!super.add(pair))
            return false;

        // Remove overflown object
        if (size() > maxPairCount)
            remove(last());
        
        return true;
    }

    public boolean addAll(Collection<? extends MeasuredAbstractObjectList.Pair<T>> source) {
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
    public boolean add(MeasuredAbstractObjectList<T> source) {
        boolean retVal = false;
        for (Pair<T> pair : source) {
            if (add(pair.object, pair.distance))
                retVal = true;
        }
        return retVal;
    }
    
    /** Enumerate all stored measured object */
    public GenericObjectIterator<T> objects() {
        return new InternalIterator<T>(iterator());
    }
    
    /** Get last stored measured object
     *  Throws NoSuchElementException if list is empty
     */
    public T getLastObject() {
        return last().object;
    }
    
    /** Get last stored measured object distance 
     *  Throws NoSuchElementException if list is empty
     */
    public float getLastDistance() {
        return last().distance;
    }
    
}
