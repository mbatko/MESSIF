/*
 * ObjectProvider.java
 *
 * Created on 7. unor 2007, 12:25
 *
 */

package messif.objects;

/**
 * Interaface for providing objects through iterator.
 * 
 * Common object providers: {@link GenericAbstractObjectList}, {@link GenericMatchingObjectList}, {@link messif.buckets.LocalBucket}
 * @author xbatko
 */
public interface ObjectProvider<E extends AbstractObject> {
    
    
    /**
     * The iterator returning provided objects must be returned.
     *
     * @return iterator for provided objects
     */
    public GenericObjectIterator<E> provideObjects();

}
