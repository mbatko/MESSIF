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

import messif.objects.AbstractObject;
import messif.objects.DistanceFunction;
import messif.objects.LocalAbstractObject;
import messif.utility.Clearable;

/**
 * Encapsulation of an object-distance pair.
 * This class holds an {@link AbstractObject} and its distance.
 * It is used as a return value for all the {@link messif.operations.QueryOperation query operations}.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class RankedAbstractObject extends DistanceRankedObject<AbstractObject> implements Clearable {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Constructor ******************//

    /**
     * Creates a new instance of RankedAbstractObject for an object and its measured distance.
     * @param object the ranked object
     * @param distance the measured distance of the object
     */
    public RankedAbstractObject(AbstractObject object, float distance) {
        super(object, distance);
    }

    /**
     * Creates a new instance of RankedAbstractObject by measuring an object's distance from the reference object.
     * @param referenceObject the reference object from which the distance is measured
     * @param object the ranked object
     */
    public RankedAbstractObject(LocalAbstractObject referenceObject, LocalAbstractObject object) {
        super(object, referenceObject.getDistance(object));
    }

    /**
     * Creates a new instance of RankedAbstractObject by measuring an object's distance from the reference object
     * using a given distance function.
     * @param <T> the type of object used to measure the distance
     * @param object the ranked object
     * @param distanceFunction the distance function used for the measuring
     * @param referenceObject the reference object from which the distance is measured
     * @throws NullPointerException if the distance function is <tt>null</tt>
     */
    public <T extends AbstractObject> RankedAbstractObject(T object, DistanceFunction<? super T> distanceFunction, T referenceObject) throws NullPointerException {
        super(object, distanceFunction, referenceObject);
    }


    //****************** Clearable interface ******************//

    public void clearSurplusData() {
        getObject().clearSurplusData();
    }

    @Override
    public RankedAbstractObject clone(float newDistance) {
        return (RankedAbstractObject)super.clone(newDistance); // This cast is valid because of the clonning
    }

}
