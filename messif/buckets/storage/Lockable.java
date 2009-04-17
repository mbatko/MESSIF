/*
 *  Lockable
 * 
 */

package messif.buckets.storage;

/**
 * Interface for objects that supports object-locking.
 * The {@link #lock(boolean) lock} method can be used to acquire a lock that holds
 * until the {@link Lock#unlock() unlock} method is called on that returned instance.
 * 
 * @author xbatko
 */
public interface Lockable {
    /**
     * Acquires a lock on this object.
     * If the locking was successful, a {@link Lock} object is obtained and
     * the lock holds until the {@link Lock#unlock()} method is called.
     * Otherwise, this method blocks until the lock can be acquired or
     * <tt>null</tt> is returned if a non-blocking call was required.
     * @param blocking if <tt>true</tt>, this method will block until the lock is obtained
     * @return a lock on this object or <tt>null</tt>
     * @throws IllegalStateException if the lock cannot be obtained
     */
    public Lock lock(boolean blocking) throws IllegalStateException;
}
