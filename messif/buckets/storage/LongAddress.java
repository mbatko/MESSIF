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
 * Implementation of {@link Address} for a storage that uses long addresses.
 * 
 * @param <T> the class of objects that this address points to
 * @see Storage
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public final class LongAddress<T> implements Address<T> {

    /** Class serial id for serialization. */
    private static final long serialVersionUID = 23101L;

    /** Storage associated with this address */
    private final LongStorage<T> storage;
    /** Actual address in the storage this object points to */
    private final long address;

    /**
     * Creates a new instance of IntAddress on the specified int storage.
     * @param storage the storage on which this address is valid
     * @param address the int address in the storage this IntAddress points to
     */
    public LongAddress(LongStorage<T> storage, long address) {
        this.storage = storage;
        this.address = address;
    }

    /**
     * Returns the associated long address into the storage.
     * @return the associated long address into the storage
     */
    public long getAddress() {
        return address;
    }

    public T read() throws BucketStorageException {
        return storage.read(address);
    }

    public void remove() throws BucketStorageException, UnsupportedOperationException {
        storage.remove(address);
    }
}
