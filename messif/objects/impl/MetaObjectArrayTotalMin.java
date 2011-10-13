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
public class MetaObjectArrayTotalMin extends MetaObjectArray {
    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** Constructors ******************//

    /**
     * Creates a new instance of MetaObjectArrayTotalMin.
     * A new unique object ID is generated and the
     * object's key is set to <tt>null</tt>.
     * @param objects the encapsulated list of objects
     */
    public MetaObjectArrayTotalMin(LocalAbstractObject... objects) {
        super(objects);
    }

    /**
     * Creates a new instance of MetaObjectArrayTotalMin.
     * A new unique object ID is generated and the 
     * object's key is set to the specified key.
     * @param objectKey the key to be associated with this object
     * @param objects the encapsulated list of objects
     */
    public MetaObjectArrayTotalMin(AbstractObjectKey objectKey, LocalAbstractObject... objects) {
        super(objectKey, objects);
    }

    /**
     * Creates a new instance of MetaObjectArrayTotalMin.
     * A new unique object ID is generated and a
     * new {@link AbstractObjectKey} is generated for
     * the specified <code>locatorURI</code>.
     * @param locatorURI the locator URI for the new object
     * @param objects the encapsulated list of objects
     */
    public MetaObjectArrayTotalMin(String locatorURI, LocalAbstractObject... objects) {
        super(locatorURI, objects);
    }

    /**
     * Creates a new instance of MetaObjectArrayTotalMin that takes the objects from the given map.
     * The array is initialized with objects from the map in the order they
     * appear in the {@code objectNames} array. Note that if the object of a given
     * name is not in the map, <tt>null</tt> is inserted into the array.
     * A new unique object ID is generated and a new {@link AbstractObjectKey} is
     * generated for the specified <code>locatorURI</code>.
     * @param locatorURI the locator URI for the new object
     * @param objects the map with named objects to encapsulate
     * @param objectNames the names of the objects to take from the given {@code objects} map
     */
    public MetaObjectArrayTotalMin(String locatorURI, Map<String, ? extends LocalAbstractObject> objects, String... objectNames) {
        super(locatorURI, objects, objectNames);
    }

    /**
     * Creates a new instance of MetaObjectArrayTotalMin from the given text stream.
     * @param stream the text stream to read the objects from
     * @param classes the classes of the objects to read from the stream
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public MetaObjectArrayTotalMin(BufferedReader stream, Class<? extends LocalAbstractObject>... classes) throws IOException {
        super(stream, classes);
    }

    /**
     * Creates a new instance of MetaObjectArrayTotalMin from the given text stream.
     * @param stream the text stream to read the objects from
     * @param objectCount number of objects to read
     * @param objectClass the class of objects to read from the stream
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public MetaObjectArrayTotalMin(BufferedReader stream, int objectCount, Class<? extends LocalAbstractObject> objectClass) throws IOException {
        super(stream, objectCount, objectClass);
    }

    /**
     * Creates a new instance of MetaObjectArrayTotalMin loaded from binary input.
     * 
     * @param input the input to read the MetaObject from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected MetaObjectArrayTotalMin(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }


    //****************** Distance function ******************//

    @Override
    public float getMaxDistance() {
        return 1;
    }

    @Override
    protected float getDistanceImpl(MetaObject obj, float[] metaDistances, float distThreshold) {
        float rtv = getMaxDistance();
        MetaObjectArray castObj = (MetaObjectArray)obj;

        for (int i = 0; i < objects.length; i++) {
            for (int j = 0; j < castObj.objects.length; j++) {
                if (objects[i].getClass().equals(castObj.objects[j].getClass())) {
                    float dist = objects[i].getNormDistance(castObj.objects[j], distThreshold);
                    if (dist < rtv)
                        rtv = dist;
                }
            }
        }

        return rtv;
    }

}
