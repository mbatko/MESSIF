/*
 * GenericAbstractObjectIterator.java
 *
 * Created on 3. kveten 2005, 11:37
 */

package messif.objects;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 * @author Vlastislav Dohnal, xdohnal@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
public class GenericAbstractObjectIterator<E extends AbstractObject> extends GenericObjectIterator<E> {
    
    protected final Iterator<? extends E> iterator;
    protected E currentObject;
    protected boolean hasNext;
    
    /****************** Constructors ******************/

    /** Creates a new instance of GenericAbstractObjectIterator<E> */
    public GenericAbstractObjectIterator(Iterator<? extends E> iterator) {
        this.iterator = iterator;
        this.currentObject = null;
        this.hasNext = false;
    }
    
    /** Creates a new instance of GenericAbstractObjectIterator<E>.
     *  The iterator contains only one object.
     */
    public GenericAbstractObjectIterator(E object) {
        this.iterator = null;
        this.currentObject = object;
        this.hasNext = object != null;
    }    

    /****************** Iterator methods ******************/

    public boolean hasNext() {
        return (iterator == null) ? hasNext : iterator.hasNext();
    }

    public E next() {
        if (iterator == null) {
            hasNext = false;
            return currentObject;
        } else 
            return currentObject = iterator.next(); 
    }
    
    public void remove() {
        if (iterator == null)
            throw new UnsupportedOperationException("This iterator doesn't support remove method");
        
        iterator.remove();
    }

    /****************** Just for convenience *************/
    
    /** Returns an instance of object returned by the last call to next().
     * @throws NoSuchElementException if next() has not been called yet.
     */
    public E getCurrentObject() throws NoSuchElementException {
        if (currentObject == null)
            throw new NoSuchElementException("Can't call getCurrentObject before next was called");
        return currentObject;
    }
    
}
