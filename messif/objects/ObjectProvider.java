/*
 * ObjectProvider.java
 *
 * Created on 7. unor 2007, 12:25
 *
 */

package messif.objects;

import messif.objects.util.AbstractObjectIterator;

/**
 * Interface for providing objects through iterator.
 * 
 * @param <E> the class of the iterated objects
 * @author xbatko
 */
public interface ObjectProvider<E extends AbstractObject> {
    
    
    /**
     * Returns an iterator over the {@link ObjectProvider provided} objects.
     * @return an iterator over the {@link ObjectProvider provided} objects
     */
    public AbstractObjectIterator<E> provideObjects();

}
