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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.keys.AbstractObjectKey;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;

/**
 * Extension of the {@link MetaObjectArray} that implements the distance
 * as minimum of the distances between all pairs (from this object and the other
 * {@link MetaObjectArray} object). Note that only objects that have the
 * same class are measured.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class MetaObjectParametricArrayTotalMin extends MetaObjectParametricArray {
    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** Constructors ******************//

    /**
     * Creates a new instance of MetaObjectParametricArrayTotalMin.
     * A new unique object ID is generated and the
     * object's key is set to <tt>null</tt>.
     * @param additionalParameters additional parameters for this meta object
     * @param objects the encapsulated list of objects
     */
    public MetaObjectParametricArrayTotalMin(Map<String, ? extends Serializable> additionalParameters, LocalAbstractObject... objects) {
        super(additionalParameters, objects);
    }

    /**
     * Creates a new instance of MetaObjectParametricArrayTotalMin.
     * A new unique object ID is generated and the 
     * object's key is set to the specified key.
     * @param objectKey the key to be associated with this object
     * @param additionalParameters additional parameters for this meta object
     * @param objects the encapsulated list of objects
     */
    public MetaObjectParametricArrayTotalMin(AbstractObjectKey objectKey, Map<String, ? extends Serializable> additionalParameters, LocalAbstractObject... objects) {
        super(objectKey, additionalParameters, objects);
    }

    /**
     * Creates a new instance of MetaObjectParametricArrayTotalMin.
     * A new unique object ID is generated and a
     * new {@link AbstractObjectKey} is generated for
     * the specified <code>locatorURI</code>.
     * @param locatorURI the locator URI for the new object
     * @param additionalParameters additional parameters for this meta object
     * @param objects the encapsulated list of objects
     */
    public MetaObjectParametricArrayTotalMin(String locatorURI, Map<String, ? extends Serializable> additionalParameters, LocalAbstractObject... objects) {
        super(locatorURI, additionalParameters, objects);
    }

    /**
     * Creates a new instance of MetaObjectParametricArrayTotalMin that takes the objects from the given map.
     * The array is initialized with objects from the map in the order they
     * appear in the {@code objectNames} array. Note that if the object of a given
     * name is not in the map, <tt>null</tt> is inserted into the array.
     * A new unique object ID is generated and a new {@link AbstractObjectKey} is
     * generated for the specified <code>locatorURI</code>.
     * @param locatorURI the locator URI for the new object
     * @param additionalParameters additional parameters for this meta object
     * @param objects the map with named objects to encapsulate
     * @param objectNames the names of the objects to take from the given {@code objects} map
     */
    public MetaObjectParametricArrayTotalMin(String locatorURI, Map<String, ? extends Serializable> additionalParameters, Map<String, ? extends LocalAbstractObject> objects, String... objectNames) {
        super(locatorURI, additionalParameters, objects, objectNames);
    }

    /**
     * Creates a new instance of MetaObjectParametricArrayTotalMin from the given text stream with header.
     * Note that a header must contain also the object names even though they are not
     * stored and used by the array.
     * @param stream the text stream to read the objects from
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     * @see #readObjectsHeader(java.io.BufferedReader)
     */
    public MetaObjectParametricArrayTotalMin(BufferedReader stream) throws IOException {
        super(stream);
    }

    /**
     * Creates a new instance of MetaObjectParametricArrayTotalMin from the given text stream.
     * @param stream the text stream to read the objects from
     * @param classes the classes of the objects to read from the stream
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public MetaObjectParametricArrayTotalMin(BufferedReader stream, Class<? extends LocalAbstractObject>... classes) throws IOException {
        super(stream, classes);
    }

    /**
     * Creates a new instance of MetaObjectParametricArrayTotalMin from the given text stream.
     * @param stream the text stream to read the objects from
     * @param additionalParameters additional parameters for this meta object
     * @param classes the classes of the objects to read from the stream
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public MetaObjectParametricArrayTotalMin(BufferedReader stream, Map<String, ? extends Serializable> additionalParameters, Class<? extends LocalAbstractObject>... classes) throws IOException {
        super(stream, additionalParameters, classes);
    }

    /**
     * Creates a new instance of MetaObjectParametricArrayTotalMin from the given text stream.
     * @param stream the text stream to read the objects from
     * @param additionalParameters additional parameters for this meta object
     * @param objectCount number of objects to read
     * @param objectClass the class of objects to read from the stream
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public MetaObjectParametricArrayTotalMin(BufferedReader stream, Map<String, ? extends Serializable> additionalParameters, int objectCount, Class<? extends LocalAbstractObject> objectClass) throws IOException {
        super(stream, additionalParameters, objectCount, objectClass);
    }

    /**
     * Creates a new instance of MetaObjectParametricArrayTotalMin loaded from binary input.
     * 
     * @param input the input to read the MetaObject from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected MetaObjectParametricArrayTotalMin(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }


    //****************** Distance function ******************//

    @Override
    public float getMaxDistance() {
        return 1;
    }

    /**
     * The actual implementation of the metric function.
     * Minimal distance between all objects with a matching class from this array
     * and the given {@code obj} is returned as the resulting distance. If the
     * {@code obj} is also {@link MetaObjectParametricArray}, the distance
     * is computed to all compatible encapsulated objects.
     * 
     * @param obj the object to compute distance to
     * @param metaDistances the array that is filled with the distances of the respective encapsulated objects, if it is not <tt>null</tt>
     * @param distThreshold the threshold value on the distance
     * @return the actual distance between this and {@code obj} if the distance is lower than {@code distThreshold}
     * @see LocalAbstractObject#getDistance(messif.objects.LocalAbstractObject, float) LocalAbstractObject.getDistance
     */
    @Override
    protected float getDistanceImpl(MetaObject obj, float[] metaDistances, float distThreshold) {
        for (int i = 0; i < objects.length; i++) {
            float dist = getMinNormDistanceToArray(this.objects[i], obj, distThreshold);
            if (metaDistances != null)
                metaDistances[i] = dist;
            if (dist < distThreshold)
                distThreshold = dist; // Note that this is intentional, sice we do not report distances higher than distThreshold
        }

        return distThreshold;
    }

    /**
     * Returns the normalized distance between object {@code o1} and {@code o2}.
     * If object {@code o2} is instance of {@link MetaObjectParametricArray},
     * the minimal distance between o1 and all non-null,
     * {@link LocalAbstractObject#isDistanceCompatible(messif.objects.LocalAbstractObject) distance compatible}
     * objects in {@code o2} array is returned.
     * 
     * @param o1 the object from which to compute the distance
     * @param o2 the object to which to compute the distance (special if o2 is {@link MetaObjectParametricArray})
     * @param distThreshold the threshold value on the distance (should be normalized, i.e. {@code 0 <= distThreshold <= 1})
     * @return the minimal normalized distance between {@code o1} and (all) {@code o2};
     *          if all encapsulated objects in {@code o2} are <tt>null<tt> or not
     *          compatible with the {@code o1}, the {@link #MAX_DISTANCE} is returned
     */
    protected static float getMinNormDistanceToArray(LocalAbstractObject o1, LocalAbstractObject o2, float distThreshold) {
        if (!(o2 instanceof MetaObjectParametricArray))
            return o1.getNormDistance(o2, distThreshold);
        MetaObjectParametricArray castO2 = (MetaObjectParametricArray)o2;

        for (int j = 0; j < castO2.objects.length; j++) {
            if (o1.isDistanceCompatible(castO2.objects[j])) {
                float dist = o1.getNormDistance(castO2.objects[j], distThreshold);
                if (dist < distThreshold)
                    distThreshold = dist; // Note that this is intentional, sice we do not report distances higher than distThreshold
            }
        }

        return distThreshold;
    }
}
