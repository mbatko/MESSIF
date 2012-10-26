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
package messif.objects.util;

import java.util.Arrays;
import messif.objects.AbstractObject;

/**
 * Encapsulation of an object-distance pair with the distances to respective
 * sub-objects of a {@link messif.objects.MetaObject}.
 * This class holds an {@link messif.objects.AbstractObject} and its distance.
 * It is used as a return value for all the {@link messif.operations.QueryOperation query operations}.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
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
        this.subDistances = subDistances.clone();
    }


    //****************** Attribute access ******************//

    /**
     * Returns the array of distances to respective sub-objects of the encapsulated object.
     * @return the array of distances to respective sub-objects
     */
    public float[] getSubDistances() {
        return subDistances.clone();
    }

    /**
     * Returns the number of sub-object distances stored in this object.
     * @return the number of sub-object distances
     */
    public int getSubDistancesCount() {
        return subDistances.length;
    }

    /**
     * Returns the distance to the sub-object {@code index} of the encapsulated object.
     * @param index the index of the sub-object the distance of which to get
     * @return the distance to the respective sub-object
     * @throws IndexOutOfBoundsException if the given index is not valid 
     */
    public float getSubDistance(int index) throws IndexOutOfBoundsException {
        return subDistances[index];
    }


    //****************** Textual representation ******************//

    @Override
    public String toString() {
        return "<" + getDistance() + Arrays.toString(subDistances) + ": " + getObject() + ">";
    }

}
