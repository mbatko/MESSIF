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
 * Interface of a generic storage address.
 * An address can be retrieved by storing an object into a {@link Storage} via
 * the {@link Storage#store} method.
 * 
 * @param <T> the class of objects that this address points to
 * @see Storage
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface Address<T> extends Serializable {

    /**
     * Reads the object stored at this address from the associated storage.
     * @return the object retrieved
     * @throws BucketStorageException if there was an error reading the data
     */
    public T read() throws BucketStorageException;

    /**
     * Removes the object stored at this address from the associated storage.
     * This operation is optional and need not be implemented.
     * 
     * @throws BucketStorageException if there was an error deleting an object
     * @throws UnsupportedOperationException if this storage does not support removal of objects
     */
    public void remove() throws BucketStorageException, UnsupportedOperationException;

}
