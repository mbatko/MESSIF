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

import messif.buckets.StorageFailureException;

/**
 * Exception that indicates that an invalid address has been used while
 * accessing a storage.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class InvalidAddressException extends StorageFailureException {
    /** class serial id for serialization */
    static final long serialVersionUID = 8785487981624795432L;

    /**
     * Creates an {@code InvalidAddressException} with no detail message.
     */
    public InvalidAddressException() {
	super(null);
    }

    /**
     * Creates an {@code InvalidAddressException} with the specified detail message.
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public InvalidAddressException(String message) {
	super(message, null);
    }

}
