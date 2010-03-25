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

import messif.buckets.BucketStorageException;

/**
 * Interface for storage that uses long addresses.
 * The {@link #store} method stores the provided object into the storage
 * and returns its address. This address can be used to {@link #read read}
 * or {@link #remove remove} the object at any time later.
 * 
 * @param <T> the class of objects stored in this storage
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface LongStorage<T> extends Storage<T> {

    @Override
    public LongAddress<T> store(T object) throws BucketStorageException;

    /**
     * Reads the object stored at the specified address in this storage.
     * @param address the address of the object to read
     * @return the object retrieved
     * @throws BucketStorageException if there was an error reading the data
     */
    public abstract T read(long address) throws BucketStorageException;

    /**
     * Removes the object stored at the specified address in this storage.
     * This operation is optional and need not be implemented.
     * 
     * @param address the address of the object to remove
     * @throws BucketStorageException if there was an error deleting an object
     * @throws UnsupportedOperationException if this storage does not support removal of objects
     */
    public abstract void remove(long address) throws BucketStorageException, UnsupportedOperationException;

}
