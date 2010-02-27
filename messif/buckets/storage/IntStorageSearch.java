/*
 *  Search
 * 
 */

package messif.buckets.storage;

import messif.buckets.index.ModifiableSearch;

/**
 * This interface represents a {@link ModifiableSearch} that supports getting
 * {@link IntStorage}'s address of the found object.
 * 
 * @param <T> the type of objects that are looked up
 * @author xbatko
 */
public interface IntStorageSearch<T> extends StorageSearch<T> {

    /**
     * Returns the address of the object found by the last search. That is, if method {@link #next}
     * or {@link #previous} has returned <tt>true</tt>, this method returns the address of the matching
     * object. If <tt>false</tt> has been returned, this method throws an {@link IllegalStateException}.
     *
     * <p>
     * Note that even though the address can be used to retrieve the actual object,
     * the {@link #getCurrentObject()} should be used instead, since it is usually faster.
     * </p>
     * @return the address of the object found by the last search
     * @throws IllegalStateException if there is no current object (next/previous method was not called or returned <tt>false</tt>)
     */
    public int getCurrentObjectIntAddress() throws IllegalStateException;

    @Override
    public IntAddress<T> getCurrentObjectAddress() throws IllegalStateException;
}
