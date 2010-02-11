/*
 *  RankedAbstractMetaObject
 * 
 */

package messif.objects.util;

import java.util.Arrays;
import messif.objects.AbstractObject;

/**
 * Encapsulation of an object-distance pair with the distances to respective
 * sub-objects of a {@link messif.objects.MetaObject}.
 * This class holds an {@link messif.objects.AbstractObject} and its distance.
 * It is used as a return value for all the {@link messif.operations.QueryOperation query operations}.
 * 
 * @author xbatko
 */
public class RankedAbstractMetaObject extends RankedAbstractObject {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Array of distances to respective sub-objects */
    private final float[] subDistances;


    //****************** Constructor ******************//

    /**
     * Creates a new instance of RankedAbstractObject for the object its measured distance.
     * @param object the measured object
     * @param distance the measured distance
     * @param subDistances the distances to respective sub-objects of the <code>object</code>
     */
    public RankedAbstractMetaObject(AbstractObject object, float distance, float[] subDistances) {
        super(object, distance);
        this.subDistances = subDistances;
    }


    //****************** Attribute access ******************//

    /**
     * Returns the array of distances to respective sub-objects of the encapsulated object.
     * @return the array of distances to respective sub-objects
     */
    public float[] getSubDistances() {
        return subDistances.clone();
    }


    //****************** Textual representation ******************//

    @Override
    public String toString() {
        return "<" + getDistance() + Arrays.toString(subDistances) + ": " + getObject() + ">";
    }

}
