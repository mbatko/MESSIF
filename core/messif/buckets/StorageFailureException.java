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
package messif.buckets;

/**
 * This exception indicates that storing or reading object from bucket is not possible
 * due to lower layer storage exception.
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class StorageFailureException extends BucketStorageException {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;    

    /**
     * Creates a new instance of OccupationLowException
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     */
    public StorageFailureException(Throwable cause) {
        super(BucketErrorCode.STORAGE_FAILURE, cause);
    }

    /**
     * Creates a new instance of OccupationLowException with the specified detail message.
     *
     * @param msg the detail message
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     */
    public StorageFailureException(String msg, Throwable cause) {
        super(BucketErrorCode.STORAGE_FAILURE, msg, cause);
    }

    /**
     * Returns a short description of this throwable.
     * If there was a "cause" throwable, it is appended to the string.
     * @return a string representation of this throwable.
     */
    @Override
    public String toString() {
        Throwable cause = getCause();
        String ret = super.toString();
        if (cause != null)
            ret = ret + " (" + cause.toString() + ")";
        return ret;
    }

}
