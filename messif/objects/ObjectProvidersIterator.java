/*
 * ObjectProvidersIterator.java
 *
 * Created on 7. unor 2007, 12:37
 *
 */

package messif.objects;

import java.util.Collection;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * This class allows iterating over multiple ObjectProviders.
 * Providers are accessed in the same order as they were passed in the collection.
 * If actualy iterated provider has no next object, next provider (that has object)
 * is selected until whole list of providers is scanned.
 *
 * @author xbatko
 */
public class ObjectProvidersIterator<E extends AbstractObject> extends GenericObjectIterator<E> {
    /** Queue of providers that are waiting to be scanned */
    protected final Queue<ObjectProvider<? extends E>> providers;
    
    /** Iterator from current provider */
    protected GenericObjectIterator<? extends E> currentIterator;
    
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
