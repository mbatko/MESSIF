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

import messif.utility.ErrorCode;

/**
 * Represents an error code that can be returned by bucket operations.
 * Standard error codes are instantiated as static constants.
 * Other error codes can be using constructor, but there is no guarantee
 * they will have the same code when used in distributed environment.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class BucketErrorCode extends ErrorCode {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    //****************** Insertion codes ******************//

    /** Object has been successfully inserted causing no capacity overflow. */
    public static BucketErrorCode OBJECT_INSERTED = new BucketErrorCode("object inserted");
    /** Object cannot be inserted due to some limits of structure. */
    public static BucketErrorCode OBJECT_REFUSED = new BucketErrorCode("object refused");
    /** Object was not inserted because its copy is already present. */
    public static BucketErrorCode OBJECT_DUPLICATE = new BucketErrorCode("object duplicate"); 
    /** Object has been inserted but the soft-capacity has been reached. Overflow of the hard-capacity is reported as a CapacityFullException exception. */
    public static BucketErrorCode SOFTCAPACITY_EXCEEDED = new BucketErrorCode("soft capacity exceeded");
    /** Object was not inserted because the hard-capacity has been exceeded. This is usually reported as a CapacityFullException exception, but it can caught, so this error code allows it to be reported. */
    public static BucketErrorCode HARDCAPACITY_EXCEEDED = new BucketErrorCode("hard capacity exceeded");


    //****************** Deletion codes ******************//

    /** Object has been deleted successfully. */
    public static BucketErrorCode OBJECT_DELETED = new BucketErrorCode("object deleted");
    /** Object cannot be deleted because it is not present. */
    public static BucketErrorCode OBJECT_NOT_FOUND = new BucketErrorCode("object not found");
    /** Object has been deleted but the current capacity is less than the minimal required one (low-occupation has been reached). */
    public static BucketErrorCode LOWOCCUPATION_EXCEEDED = new BucketErrorCode("low occupation exceeded");

    //****************** Storage codes ******************//

    /** Object has not been stored, removed or read due to lower layer storage exception. */
    public static BucketErrorCode STORAGE_FAILURE = new BucketErrorCode("storage failed to process object");


    //****************** Constructor ******************//

    /**
     * Creates a new instance of BucketErrorCode, i.e. a new error code.
     * @param text error message for this error code
     */
    public BucketErrorCode(String text) {
        super(text);
    }

}
