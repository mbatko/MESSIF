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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import messif.objects.LocalAbstractObject;

/**
 * This exception indicates that inserting objects into a bucket has failed.
 * The exception returns a list of objects that were NOT stored.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class StorageInsertFailureException extends BucketStorageException {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    /** List of objects that could not be inserted */
    private final List<LocalAbstractObject> failedObjects = new ArrayList<LocalAbstractObject>();

    /**
     * Creates a new instance of StorageInsertFailureException
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     */
    public StorageInsertFailureException(Throwable cause) {
        super(BucketErrorCode.STORAGE_FAILURE, cause);
    }

    /**
     * Creates a new instance of StorageInsertFailureException with the specified detail message.
     *
     * @param msg the detail message
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     */
    public StorageInsertFailureException(String msg, Throwable cause) {
        super(BucketErrorCode.STORAGE_FAILURE, msg, cause);
    }

    /**
     * Creates a new instance of StorageInsertFailureException by copying the error code,
     * message, and cause from another {@link BucketStorageException}.
     * @param e the exception to copy from
     */
    public StorageInsertFailureException(BucketStorageException e) {
        super(e.getErrorCode(), e.getMessage(), e.getCause());
    }

    /**
     * Adds a failed object to this exception.
     * @param failedObject the object that cannot be inserted
     */
    public void addFailedObject(LocalAbstractObject failedObject) {
        failedObjects.add(failedObject);
    }

    /**
     * Returns the list of objects that could not be inserted.
     * @return the list of objects that could not be inserted
     */
    public List<LocalAbstractObject> getFailedObjects() {
        return Collections.unmodifiableList(failedObjects);
    }

    /**
     * Returns a short description of this throwable.
     * If there was a "cause" throwable, it is appended to the string.
     * @return a string representation of this throwable.
     */
    @Override
    public String toString() {
        Throwable cause = getCause();
        StringBuilder ret = new StringBuilder(super.toString());
        if (cause != null)
            ret.append(" (").append(cause).append(')');
        if (!failedObjects.isEmpty()) {
            ret.append(" failed objects:");
            for (LocalAbstractObject failedObject : failedObjects) {
                ret.append(' ').append(failedObject.getLocatorURI());
            }
        }

        return ret.toString();
    }

}
