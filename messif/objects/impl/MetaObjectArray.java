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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.keys.AbstractObjectKey;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

/**
 * Implementation of the {@link MetaObject} that stores a fixed array of encapsulated objects.
 * The metric distance function for this object is the absolute value of the
 * differences of locatorURI hash codes. For a more sophisticated distance function
 * use {@link MetaObjectArrayWeightedSum}.
 * 
 * <p>
 * Note that the encapsulated object names are automatically generated by
 * {@link #getObjectName(int)} method. Normally, this method is overloaded
 * in a subclass to provide more appropriate names.
 * </p>
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class MetaObjectArray extends MetaObject implements BinarySerializable {
    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** List of objects */
    protected final LocalAbstractObject[] objects;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of MetaObjectArray.
     * A new unique object ID is generated and the
     * object's key is set to <tt>null</tt>.
     * @param objects the encapsulated list of objects
     */
    public MetaObjectArray(LocalAbstractObject... objects) {
        this.objects = objects.clone();
    }

    /**
     * Creates a new instance of MetaObjectArray.
     * A new unique object ID is generated and the 
     * object's key is set to the specified key.
     * @param objectKey the key to be associated with this object
     * @param objects the encapsulated list of objects
     */
    public MetaObjectArray(AbstractObjectKey objectKey, LocalAbstractObject... objects) {
        super(objectKey);
        this.objects = objects.clone();
    }

    /**
     * Creates a new instance of MetaObjectArray.
     * A new unique object ID is generated and a
     * new {@link AbstractObjectKey} is generated for
     * the specified <code>locatorURI</code>.
     * @param locatorURI the locator URI for the new object
     * @param objects the encapsulated list of objects
     */
    public MetaObjectArray(String locatorURI, LocalAbstractObject... objects) {
        super(locatorURI);
        this.objects = objects.clone();
    }

    /**
     * Creates a new instance of MetaObjectArray that takes the objects from the given collection.
     * A new unique object ID is generated and a new {@link AbstractObjectKey} is
     * generated for the specified <code>locatorURI</code>.
     * @param locatorURI the locator URI for the new object
     * @param objects the collection with objects to encapsulate
     */
    public MetaObjectArray(String locatorURI, Collection<? extends LocalAbstractObject> objects) {
        super(locatorURI);
        this.objects = objects.toArray(new LocalAbstractObject[objects.size()]);
    }

    /**
     * Creates a new instance of MetaObjectArray that takes the objects from the given map.
     * The array is initialized with objects from the map in the order they
     * appear in the {@code objectNames} array. Note that if the object of a given
     * name is not in the map, <tt>null</tt> is inserted into the array.
     * A new unique object ID is generated and a new {@link AbstractObjectKey} is
     * generated for the specified <code>locatorURI</code>.
     * @param locatorURI the locator URI for the new object
     * @param objects the map with named objects to encapsulate
     * @param objectNames the names of the objects to take from the given {@code objects} map
     */
    public MetaObjectArray(String locatorURI, Map<String, ? extends LocalAbstractObject> objects, String... objectNames) {
        this.objects = new LocalAbstractObject[objectNames.length];
        for (int i = 0; i < objectNames.length; i++)
            this.objects[i] = objects.get(objectNames[i]);
    }

    /**
     * Creates a new instance of MetaObjectArray from the given text stream.
     * @param stream the text stream to read the objects from
     * @param classes the classes of the objects to read from the stream
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public MetaObjectArray(BufferedReader stream, Class<? extends LocalAbstractObject>... classes) throws IOException {
        readObjectCommentsWithoutData(stream);
        this.objects = readObjects(stream, classes);
    }

    /**
     * Creates a new instance of MetaObjectArray from the given text stream.
     * @param stream the text stream to read the objects from
     * @param objectCount number of objects to read
     * @param objectClass the class of objects to read from the stream
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public MetaObjectArray(BufferedReader stream, int objectCount, Class<? extends LocalAbstractObject> objectClass) throws IOException {
        this(stream, createClassArray(objectCount, objectClass));
    }

    /**
     * Creates a new instance of MetaObjectArray from the given text stream with header.
     * Note that a header must contain also the object names even though they are not
     * stored and used by the array.
     * @param stream the text stream to read the objects from
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     * @see #readObjectsHeader(java.io.BufferedReader)
     */
    public MetaObjectArray(BufferedReader stream) throws IOException {
        this.objects = readObjects(stream, null, readObjectsHeader(stream), new LinkedHashMap<String, LocalAbstractObject>()).values().toArray(new LocalAbstractObject[0]);
    }


    //****************** MetaObject method implementations ******************//

    /**
     * Returns the name of the fixed object with the given {@code index}.
     * By default, this implementation returns "ObjectX" where X is the given
     * {@code index}. However, this method should be overridden to give
     * the real name of the object if appropriate (e.g. from a static array of names).
     * 
     * @param index the fixed index of the object the name of which to get
     * @return the name of the {@code index}th object
     */
    protected String getObjectName(int index) {
        return "Object" + index;
    }


    @Override
    public int getObjectCount() {
        int count = 0;
        for (int i = 0; i < objects.length; i++)
            if (objects[i] != null)
                count++;
        return count;
    }

    @Override
    public LocalAbstractObject getObject(String name) {
        for (int i = 0; i < objects.length; i++)
            if (getObjectName(i).equals(name))
                return objects[i];
        return null;
    }

    /**
     * Returns the encapsulated object for given index.
     *
     * @param index the index of the object to return
     * @return encapsulated object for given name or <tt>null</tt> if the key is unknown
     * @throws IndexOutOfBoundsException if the given index is not within [0;{@link #getObjectCount() count}) interval
     */
    public LocalAbstractObject getObject(int index) throws IndexOutOfBoundsException {
        return objects[index];
    }

    @Override
    public Collection<String> getObjectNames() {
        Collection<String> names = new ArrayList<String>(objects.length);
        for (int i = 0; i < objects.length; i++)
            if (objects[i] != null)
                names.add(getObjectName(i));
        return names;
    }

    @Override
    public Collection<LocalAbstractObject> getObjects() {
        Collection<LocalAbstractObject> ret = new ArrayList<LocalAbstractObject>(objects.length);
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] != null)
                ret.add(objects[i]);
        }
        return ret;
    }

    @Override
    public Map<String, LocalAbstractObject> getObjectMap() {
        Map<String, LocalAbstractObject> ret = new LinkedHashMap<String, LocalAbstractObject>(objects.length);
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] != null)
                ret.put(getObjectName(i), objects[i]);
        }
        return ret;
    }


    //****************** Text stream I/O helper method ******************//

    /**
     * Creates an array of {@code count} {@code clazz} elements.
     * @param count the number of array elements to create
     * @param clazz the class to fill the array with
     * @return an array filled with {@code count} {@code clazz} elements
     */
    protected static Class<? extends LocalAbstractObject>[] createClassArray(int count, Class<? extends LocalAbstractObject> clazz) {
        @SuppressWarnings("unchecked")
        Class<? extends LocalAbstractObject>[] classes = new Class[count];
        for (int i = 0; i < classes.length; i++)
            classes[i] = clazz;
        return classes;
    }

    /**
     * Utility method for reading objects from a text stream.
     * This method is intended to be used in subclasses that have a fixed
     * list of objects to implement the {@link BufferedReader} constructor.
     * 
     * <p>
     * Note that for each <tt>null</tt> item of the {@code classes} array
     * a <tt>null</tt> is stored in the returned array without reading anything
     * from the {@code stream}.
     * </p>
     *
     * @param stream the text stream to read the objects from
     * @param classes the classes of the objects to read from the stream
     * @return the array of objects read from the stream
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    protected static LocalAbstractObject[] readObjects(BufferedReader stream, Class<? extends LocalAbstractObject>... classes) throws IOException {
        if (classes == null || classes.length == 0)
            throw new IllegalArgumentException("At least one object class must be specified for reading");
        LocalAbstractObject[] ret = new LocalAbstractObject[classes.length];
        for (int i = 0; i < classes.length; i++) {
            if (classes[i] == null) {
                ret[i] = null;
            } else if (peekNextChar(stream) == '\n') {
                if (!stream.readLine().isEmpty()) // Read the empty line and assertion check
                    throw new InternalError("This should never happen - something is wrong with peekNextChar");
                ret[i] = null;
            } else {
                ret[i] = readObject(stream, classes[i]);
            }
        }
        return ret;
    }

    @Override
    protected void writeData(OutputStream stream) throws IOException {
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] == null)
                stream.write('\n');
            else
                objects[i].write(stream, false);
        }
    }


    //****************** Distance function ******************//

    /**
     * The actual implementation of the metric function.
     * The distance is a trivial metric on locator URIs of this object and {@code obj}.
     * The array <code>metaDistances</code> is ignored.
     * @see LocalAbstractObject#getDistance(messif.objects.LocalAbstractObject, float) LocalAbstractObject.getDistance
     */
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float[] metaDistances, float distThreshold) {
        String thisLocator = getLocatorURI();
        String otherLocator = obj.getLocatorURI();
        return (thisLocator == null ? otherLocator == null : thisLocator.equals(otherLocator)) ? 0 : 1;
    }


    //************ BinarySerializable support ************//

    /**
     * Creates a new instance of MetaObjectFixed loaded from binary input.
     * 
     * @param input the input to read the MetaObject from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected MetaObjectArray(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        this.objects = new LocalAbstractObject[serializator.readInt(input)];
        for (int i = 0; i < this.objects.length; i++)
            this.objects[i] = serializator.readObject(input, LocalAbstractObject.class);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        int size = super.binarySerialize(output, serializator);
        size += serializator.write(output, objects.length);
        for (int i = 0; i < objects.length; i++)
            size += serializator.write(output, objects[i]);
        return size;
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        int size = super.getBinarySize(serializator);
        size += serializator.getBinarySize(objects.length);
        for (int i = 0; i < objects.length; i++)
            size += serializator.getBinarySize(objects[i]);
        return size;
    }

}
