/*
 * BucketErrorCode.java
 *
 * Created on 22. zari 2005, 10:40
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package messif.buckets;

import messif.utility.ErrorCode;

/**
 * Represents an error code that can be returned by bucket operations.
 * Standard error codes are instantiated as static constants.
 * Other error codes can be using constructor, but there is no guarantee
 * they will have the same code when used in distributed environment.
 *
 * @author xbatko
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
