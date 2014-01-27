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
import messif.objects.keys.AbstractObjectKey;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;

/**
 * Extension of the {@link MetaObjectParametricArray} that implements the distance
 * as a weighted sum of the encapsulated objects.
 * 
 * <p>
 * Note that the weights are set to 1 by default. Normally, this method is overloaded
 * in a subclass to provide more appropriate weights.
 * </p>
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class MetaObjectParametricArrayWeightedSum extends MetaObjectParametricArray {
    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** Constructors ******************//

    /**
     * Creates a new instance of MetaObjectParametricArrayWeightedSum.
     * A new unique object ID is generated and the
     * object's key is set to <tt>null</tt>.
     * @param additionalParameters additional parameters for this meta object
     * @param objects the encapsulated list of objects
     */
    public MetaObjectParametricArrayWeightedSum(Map<String, ? extends Serializable> additionalParameters, LocalAbstractObject... objects) {
        super(additionalParameters, objects);
    }

    /**
     * Creates a new instance of MetaObjectParametricArrayWeightedSum.
     * A new unique object ID is generated and the 
     * object's key is set to the specified key.
     * @param objectKey the key to be associated with this object
     * @param additionalParameters additional parameters for this meta object
     * @param objects the encapsulated list of objects
     */
    public MetaObjectParametricArrayWeightedSum(AbstractObjectKey objectKey, Map<String, ? extends Serializable> additionalParameters, LocalAbstractObject... objects) {
        super(objectKey, additionalParameters, objects);
    }

    /**
     * Creates a new instance of MetaObjectParametricArrayWeightedSum.
     * A new unique object ID is generated and a
     * new {@link AbstractObjectKey} is generated for
     * the specified <code>locatorURI</code>.
     * @param locatorURI the locator URI for the new object
     * @param additionalParameters additional parameters for this meta object
     * @param objects the encapsulated list of objects
     */
    public MetaObjectParametricArrayWeightedSum(String locatorURI, Map<String, ? extends Serializable> additionalParameters, LocalAbstractObject... objects) {
        super(locatorURI, additionalParameters, objects);
    }

    /**
     * Creates a new instance of MetaObjectParametricArrayWeightedSum that takes the objects from the given map.
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
    public MetaObjectParametricArrayWeightedSum(String locatorURI, Map<String, ? extends Serializable> additionalParameters, Map<String, ? extends LocalAbstractObject> objects, String... objectNames) {
        super(locatorURI, additionalParameters, objects, objectNames);
    }

    /**
     * Creates a new instance of MetaObjectParametricArrayWeightedSum from the given text stream with header.
     * Note that a header must contain also the object names even though they are not
     * stored and used by the array.
     * @param stream the text stream to read the objects from
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     * @see #readObjectsHeader(java.io.BufferedReader)
     */
    public MetaObjectParametricArrayWeightedSum(BufferedReader stream) throws IOException {
        super(stream);
    }

    /**
     * Creates a new instance of MetaObjectParametricArrayWeightedSum from the given text stream.
     * @param stream the text stream to read the objects from
     * @param classes the classes of the objects to read from the stream
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public MetaObjectParametricArrayWeightedSum(BufferedReader stream, Class<? extends LocalAbstractObject>[] classes) throws IOException {
        super(stream, classes);
    }

    /**
     * Creates a new instance of MetaObjectParametricArrayWeightedSum from the given text stream.
     * @param stream the text stream to read the objects from
     * @param additionalParameters additional parameters for this meta object
     * @param classes the classes of the objects to read from the stream
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public MetaObjectParametricArrayWeightedSum(BufferedReader stream, Map<String, ? extends Serializable> additionalParameters, Class<? extends LocalAbstractObject>[] classes) throws IOException {
        super(stream, additionalParameters, classes);
    }

    /**
     * Creates a new instance of MetaObjectParametricArrayWeightedSum from the given text stream.
     * @param stream the text stream to read the objects from
     * @param additionalParameters additional parameters for this meta object
     * @param objectCount number of objects to read
     * @param objectClass the class of objects to read from the stream
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public MetaObjectParametricArrayWeightedSum(BufferedReader stream, Map<String, ? extends Serializable> additionalParameters, int objectCount, Class<? extends LocalAbstractObject> objectClass) throws IOException {
        super(stream, additionalParameters, objectCount, objectClass);
    }

    /**
     * Creates a new instance of MetaObjectParametricArrayWeightedSum loaded from binary input.
     * 
     * @param input the input to read the MetaObject from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected MetaObjectParametricArrayWeightedSum(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }


    //****************** Distance function ******************//

    /**
     * Returns the weight used for the {@code index}th encapsulated object distance in the overall distance sum.
     * @param index the fixed index of the object the weight of which to get
     * @return the weight used for the {@code index}th encapsulated object
     */
    protected float getWeight(int index) {
        return 1f;
    }

    @Override
    public float getMaxDistance() {
        float weightSum = 1; // This is a safeguard to overcome possible normalization excesses
        for (int i = 0; i < objects.length; i++)
            weightSum += getWeight(i);
        return weightSum;
    }

    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float[] metaDistances, float distThreshold) {
        float rtv = 0;
        MetaObjectParametricArray castObj = (MetaObjectParametricArray)obj;

        for (int i = 0; i < objects.length; i++) {
            float weight = getWeight(i);
            if (weight <= 0)
                continue;
            float distance = implementationGetDistance(objects[i], castObj.objects[i], distThreshold / weight);
            if (metaDistances != null)
                metaDistances[i] = distance;
            if (distance != UNKNOWN_DISTANCE)
                rtv += distance * weight;
            if (rtv > distThreshold) // The threshold reached, we are done
                return rtv;
        }

        return rtv;
    }

}
