/*
 * RankedAbstractObject
 * 
 */
package messif.objects.util;

import messif.objects.AbstractObject;
import messif.utility.Clearable;

/**
 * Encapsulation of an object-distance pair.
 * This class holds an {@link AbstractObject} and its distance.
 * It is used as a return value for all the {@link messif.operations.QueryOperation query operations}.
 * 
 * @author xbatko
 */
public class RankedAbstractObject extends DistanceRankedObject<AbstractObject> implements Clearable {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Constructor ******************//

    /**
     * Creates a new instance of RankedAbstractObject for the object its measured distance.
     * @param object the measured object
     * @param distance the measured distance
     */
    public RankedAbstractObject(AbstractObject object, float distance) {
        super(object, distance);
    }


    //****************** Clearable interface ******************//

    public void clearSurplusData() {
        object.clearSurplusData();
    }

}
