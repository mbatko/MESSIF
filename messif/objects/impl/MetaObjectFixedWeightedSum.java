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
package messif.objects.impl;

import java.io.IOException;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.keys.AbstractObjectKey;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;

/**
 * Abstract extension of the {@link MetaObjectFixed} that implements the distance
 * as a weighted sum of the encapsulated objects.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class MetaObjectFixedWeightedSum extends MetaObjectFixed {
    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** Constructors ******************//

    /**
     * Creates a new instance of MetaObjectFixedWeightedSum.
     * A new unique object ID is generated and the
     * object's key is set to <tt>null</tt>.
     */
    protected MetaObjectFixedWeightedSum() {
    }

    /**
     * Creates a new instance of MetaObjectFixedWeightedSum.
     * A new unique object ID is generated and the 
     * object's key is set to the specified key.
     * @param objectKey the key to be associated with this object
     */
    protected MetaObjectFixedWeightedSum(AbstractObjectKey objectKey) {
        super(objectKey);
    }

    /**
     * Creates a new instance of MetaObjectFixedWeightedSum.
     * A new unique object ID is generated and a
     * new {@link AbstractObjectKey} is generated for
     * the specified <code>locatorURI</code>.
     * @param locatorURI the locator URI for the new object
     */
    protected MetaObjectFixedWeightedSum(String locatorURI) {
        super(locatorURI);
    }

    /**
     * Creates a new instance of MetaObjectFixedWeightedSum loaded from binary input.
     * 
     * @param input the input to read the MetaObject from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     * @see #readObjectsBinary
     */
    protected MetaObjectFixedWeightedSum(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }


    //****************** Distance function ******************//

    /**
     * Returns the weight used for the {@code index}th encapsulated object distance in the overall distance sum.
     * @param index the fixed index of the object the weight of which to get
     * @return the weight used for the {@code index}th encapsulated object
     */
    protected abstract float getWeight(int index);

    /**
     * Computes the distance of the {@code index}th fixed encapsulated object of this metaobject
     * to the {@code index}th fixed encapsulated object of the given {@code obj}.
     * If the distance cannot be measured, the {@link #UNKNOWN_DISTANCE} is returned.
     * 
     * @param index the index of the fixed encapsulated object for which to get the distance
     * @param obj the other {@link MetaObjectFixed} the fixed encapsulated object of which to measure
     * @param distThreshold the threshold value on the distance (the query radius from the example above)
     * @return the actual distance between obj and this if the distance is lower than distThreshold.
     *         Otherwise the returned value is not guaranteed to be exact, but in this respect the returned value
     *         must be greater than the threshold distance.
     */
    protected float getDistanceObjectImpl(int index, MetaObjectFixed obj, float distThreshold) {
        LocalAbstractObject o1 = getObject(index);
        LocalAbstractObject o2 = obj.getObject(index);
        if (o1 == null || o2 == null)
            return UNKNOWN_DISTANCE;
        return o1.getDistance(o2, distThreshold);
    }

    @Override
    public float getMaxDistance() {
        float weightSum = 1; // This is a safeguard to overcome possible normalization excesses
        for (int i = 0; i < getObjectNamesCount(); i++)
            weightSum += getWeight(i);
        return weightSum;
    }

    @Override
    protected float getDistanceImpl(MetaObject obj, float[] metaDistances, float distThreshold) {
        float rtv = 0;
        MetaObjectFixed castObj = (MetaObjectFixed)obj;

        for (int i = 0; i < getObjectNamesCount(); i++) {
            if (getWeight(i) <= 0)
                continue;
            float distance = getDistanceObjectImpl(i, castObj, distThreshold);
            if (metaDistances != null)
                metaDistances[i] = distance;
            if (distance != UNKNOWN_DISTANCE)
                rtv += distance * getWeight(i);
        }

        return rtv;
    }

}
