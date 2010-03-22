/*
 *  Removable
 * 
 */

package messif.buckets;

/**
 * Interface for classes that supports addition of an object.
 * 
 * @param <T> the type of added objects
 * @author xbatko
 */
public interface Addible<T> {

    /**
     * Adds the specified object to this instance.
     * @param object the object to be added
     * @return <tt>true</tt> if the addition was successful
     * @throws BucketStorageException if there was an error adding the object
     */
    public boolean add(T object) throws BucketStorageException;
}
