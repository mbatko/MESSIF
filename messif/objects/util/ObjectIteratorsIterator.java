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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import messif.objects.AbstractObject;

/**
 * This class allows iterating over multiple Iterators.
 * The iterotors are accessed in the same order as they were passed in the collection.
 * If actualy iterated iterator has no next object, next iterator (that has object)
 * is selected until whole list of iterators is scanned.
 *
 * @param <E> iterators type
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ObjectIteratorsIterator<E extends AbstractObject> extends AbstractObjectIterator<E> {
    
    /** Queue of iterators that are waiting to be scanned */
    protected final Queue<Iterator<? extends E>> iterators;
    
    /** Iterator from current iterator */
    protected Iterator<? extends E> currentIterator;
    
    /** Stored instance of object returned by the last call to next() */
    protected E currentObject = null;
    
    /**
     * Creates a new instance of ObjectIteratorsIterator
     * 
     * 
     * @param objectIterators The source iterators that will provide objects
     */
    public ObjectIteratorsIterator(Collection<Iterator<E>> objectIterators) {
        synchronized (objectIterators) {
            iterators =  new LinkedList<Iterator<? extends E>>(objectIterators);
        }
        currentIterator = iterators.isEmpty()?null:iterators.poll();
    }
    
    /**
     * Returns an instance of object returned by the last call to next().
     *
     * @return Returns an instance of object returned by the last call to next()
     * @throws NoSuchElementException Exception NoSuchElementException is thrown if next() has not been called yet.
     */
    @Override
    public E getCurrentObject() throws NoSuchElementException {
        if (currentObject == null)
            throw new NoSuchElementException("Can't call getCurrentObject before next was called");
        return currentObject;
    }
    
    /**
     * Returns <tt>true</tt> if the iteration has more elements. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
     * rather than throwing an exception.)
     *
     * @return <tt>true</tt> if the iterator has more elements.
     */
    @Override
    public boolean hasNext() {
        // Until the end of iterators is reached
        while (currentIterator != null) {
            // If current iterator has next object, we can continue 
            if (currentIterator.hasNext())
                return true;
        
            // We have reached end of current iterator, change to next one and restart hasNext checking procedure
            currentIterator = iterators.isEmpty()?null:iterators.poll();
        }

        return false;
    }
    
    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration.
     * @exception NoSuchElementException iteration has no more elements.
     */
    @Override
    public E next() throws NoSuchElementException {
        if (hasNext())
            return currentObject = currentIterator.next();
        throw new NoSuchElementException("There are no additional objects to retrieve.");
    }
    

    /**
     * Removes from the underlying collection the last element returned by the
     * iterator (optional operation). This method can be called only once per
     * call to <tt>next</tt>.  The behavior of an iterator is unspecified if
     * the underlying collection is modified while the iteration is in
     * progress in any way other than by calling this method.
     *
     * @exception UnsupportedOperationException if the <tt>remove</tt>
     * 		  operation is not supported by this Iterator.
     * @exception IllegalStateException if the <tt>next</tt> method has not
     * 		  yet been called, or the <tt>remove</tt> method has already
     * 		  been called after the last call to the <tt>next</tt>
     * 		  method.
     */
    @Override
    public void remove() throws UnsupportedOperationException, IllegalStateException {
        if (currentIterator == null)
            throw new IllegalStateException("There is no object available for removal");
        currentIterator.remove();
    }
    
}
