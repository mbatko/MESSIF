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
package messif.buckets.storage;

import java.io.Serializable;
import messif.buckets.BucketStorageException;

/**
 * Interface of a generic storage.
 * The {@link #store} method stores the provided object into the storage
 * and returns its address. This address can be used to retrieve or remove the
 * object back at any time later.
 * 
 * @param <T> the class of objects stored in this storage
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface Storage<T> extends Serializable {

    /**
     * Stores an object in this storage.
     * The address returned by this call can be used to retrieve or remove the object.
     * 
     * @param object the object to store
     * @return the address where the object has been stored
     * @throws BucketStorageException if there was an error writing the data
     */
    public Address<T> store(T object) throws BucketStorageException;

    /**
     * Finalize this storage. All transient resources associated with this
     * storage are released.
     * After this method is called, the store and retrieval methods' behavior is unpredictable.
     *
     * @throws Throwable if there was an error while cleaning
     */
    public void finalize() throws Throwable;

    /**
     * Destroy this storage. This method releases all resources (transient and persistent)
     * associated with this storage.
     * After this method is called, the store and retrieval methods' behavior is unpredictable.
     *
     * @throws Throwable if there was an error while cleaning
     */
    public void destroy() throws Throwable;

}
