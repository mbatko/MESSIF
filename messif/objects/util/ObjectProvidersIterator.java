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
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import messif.objects.AbstractObject;
import messif.objects.ObjectProvider;

/**
 * This class allows iterating over multiple ObjectProviders.
 * Providers are accessed in the same order as they were passed in the collection.
 * If actualy iterated provider has no next object, next provider (that has object)
 * is selected until whole list of providers is scanned.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ObjectProvidersIterator<E extends AbstractObject> extends AbstractObjectIterator<E> {
    /** Queue of providers that are waiting to be scanned */
    protected final Queue<ObjectProvider<? extends E>> providers;
    
    /** Iterator from current provider */
    protected AbstractObjectIterator<? extends E> currentIterator;
    
    /** Stored instance of object returned by the last call to next() */
    protected E currentObject = null;
    
    /**
     * Creates a new instance of ObjectProvidersIterator
     * 
     * 
     * @param objectProviders The source ObjectProviders that will provide objects
     */
    public ObjectProvidersIterator(Collection<ObjectProvider<? extends E>> objectProviders) {
        synchronized (objectProviders) {
            providers =  new LinkedList<ObjectProvider<? extends E>>(objectProviders);
        }
        currentIterator = providers.isEmpty()?null:providers.poll().provideObjects();
    }
    
    /**
     * Returns an instance of object returned by the last call to next().
     *
     * @return Returns an instance of object returned by the last call to next()
     * @throws NoSuchElementException Exception NoSuchElementException is thrown if next() has not been called yet.
     */
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
    public boolean hasNext() {
        // Until the end of iterators is reached
        while (currentIterator != null) {
            // If current iterator has next object, we can continue 
            if (currentIterator.hasNext())
                return true;
        
            // We have reached end of current iterator, change to next one and restart hasNext checking procedure
            currentIterator = providers.isEmpty()?null:providers.poll().provideObjects();
        }

        return false;
    }
    
    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration.
     * @exception NoSuchElementException iteration has no more elements.
     */
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
    public void remove() throws UnsupportedOperationException, IllegalStateException {
        if (currentIterator == null)
            throw new IllegalStateException("There is no object available for removal");
        currentIterator.remove();
    }
    
}
