/*
 *  Removable
 * 
 */

package messif.buckets;

import java.util.NoSuchElementException;

/**
 * Interface for classes that supports removal of a current object.
 * 
 * @param <T> the type of removable objects
 * @author xbatko
 */
public interface Removable<T> {

    /**
     * Returns the current object (that can be removed).
     * @return the current object (that can be removed)
     * @throws NoSuchElementException if there is no current object
     */
    T getCurrentObject() throws NoSuchElementException;

    /**
     * Removes the current object.
     *
     * @throws IllegalStateException there is no current object to be removed
     *          or the current object has been removed (e.g. by a previous
     *          call to {@link #remove()})
     * @throws BucketStorageException if there was an error removing the object
     */
    void remove() throws IllegalStateException, BucketStorageException;
}
