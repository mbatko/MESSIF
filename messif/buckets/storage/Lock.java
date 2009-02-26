/*
 *  Lock
 * 
 */

package messif.buckets.storage;

/**
 * An acquired object-lock.
 * The lock is valid until the {@link #unlock()} is called.
 * 
 * @author xbatko
 */
public interface Lock {

    /**
     * Releases this lock.
     */
    public void unlock();
}
