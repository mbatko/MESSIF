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

/**
 * Ranked object encapsulating a pair of objects as a result of join operation.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class RankedJoinObject extends RankedAbstractObject {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    
    //****************** Attributes ******************//

    /** The other object in the pair */
    private final AbstractObject rightObject;

    //****************** Constructors ******************//

    /**
     * Creates a new instance of RankedJoinObject for a pair of objects and their measured distance.
     * @param leftObject the left object of the pair
     * @param rightObject the right object of the pair
     * @param distance the measured distance for the pair
     */
    public RankedJoinObject(AbstractObject leftObject, AbstractObject rightObject, float distance) {
        super(leftObject, distance);
        this.rightObject = rightObject;
    }

    /**
     * Creates a new instance of RankedJoinObject by measuring the objects' distance online.
     * @param leftObject the left object of the pair
     * @param rightObject the right object of the pair
     */
    public RankedJoinObject(LocalAbstractObject leftObject, LocalAbstractObject rightObject) {
        this(leftObject, rightObject, leftObject.getDistance(rightObject));
    }

    /**
     * Creates a new instance of RankedJoinObject by measuring the objects' distance online
     * using a given distance function.
     * @param <T> the type of object used to measure the distance
     * @param leftObject the left object of the pair
     * @param rightObject the right object of the pair
     * @param distanceFunction the distance function used for the measuring
     * @throws NullPointerException if the distance function is <tt>null</tt>
     */
    public <T extends AbstractObject> RankedJoinObject(T leftObject, T rightObject, DistanceFunction<? super T> distanceFunction) throws NullPointerException {
        this(leftObject, rightObject, distanceFunction.getDistance(leftObject, rightObject));
    }


    //****************** Attribute access ******************//

    /**
     * Returns the left object of the pair.
     * @return the encapsulated object
     */
    public AbstractObject getLeftObject() {
        return getObject();
    }

    /**
     * Returns the right object of the pair.
     * @return the encapsulated object
     */
    public AbstractObject getRightObject() {
        return rightObject;
    }


    //****************** Clearable interface ******************//

    @Override
    public void clearSurplusData() {
        super.clearSurplusData();
        getRightObject().clearSurplusData();
    }

    //****************** Overrides ******************//

    /**
     * Returns <tt>true</tt> if this encapsulated object is equal to the specified 
     * <code>DistanceRankedObject</code> encapsulated object. <tt>Null</tt> values
     * are handled correctly.
     * <p>
     * Note that the equality defined by this method is <b>inconsistent</b> with {@link #compareTo}.
     * </p>
     * @param obj the reference object with which to compare
     * @return <code>true</code> if this object is the same as the obj argument; <code>false</code> otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RankedJoinObject) || !super.equals(obj))
            return false;
        return rightObject.equals(((RankedJoinObject)obj).rightObject);
    }

    /**
     * Returns a hash code value for the pair of object.
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return super.hashCode() + ((rightObject != null) ? 31*rightObject.hashCode() : 0);
    }

    //****************** Textual representation ******************//

    @Override
    public String toString() {
        return "<" + getDistance() + ": " + getLeftObject() + ", " + getRightObject() + ">";
    }

}
