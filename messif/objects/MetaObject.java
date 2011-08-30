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
package messif.objects;

import java.util.Iterator;
import java.util.Map.Entry;
import messif.objects.keys.AbstractObjectKey;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;
import messif.utility.Convert;

/**
 * Represents a collection of LocalAbstractObjects encapsulated as one object.
 * All the encapsulated objects share the same locator URI.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class MetaObject extends LocalAbstractObject {

    /** Class id for serialization. */
    private static final long serialVersionUID = 2L;

    //****************** Constructors ******************//

    /**
     * Creates a new instance of MetaObject.
     * A new unique object ID is generated and the
     * object's key is set to <tt>null</tt>.
     */
    protected MetaObject() {
        super();
    }

    /**
     * Creates a new instance of MetaObject.
     * A new unique object ID is generated and the 
     * object's key is set to the specified key.
     * @param objectKey the key to be associated with this object
     */
    protected MetaObject(AbstractObjectKey objectKey) {
        super(objectKey);
    }

    /**
     * Creates a new instance of MetaObject.
     * A new unique object ID is generated and a
     * new {@link AbstractObjectKey} is generated for
     * the specified <code>locatorURI</code>.
     * @param locatorURI the locator URI for the new object
     */
    protected MetaObject(String locatorURI) {
        super(locatorURI);
    }


    //****************** Factory method ******************//

    /**
     * Create a new instance of a simple MetaObject wihout any
     * encapsulated objects. This is meant primarilly for the
     * locatorURI distance-based searching.
     * @param locatorURI the locator URI for the new object
     * @return a new instance of MetaObject without encapsulated objects
     */
    public static MetaObject createSearchMetaObject(String locatorURI) {
        // No encapsulated objects are required, so no methods are necessary
        return new MetaObject(locatorURI) {
            /** Class id for serialization. */
            private static final long serialVersionUID = 1L;

            @Override
            protected void writeData(OutputStream stream) throws IOException {
                throw new UnsupportedOperationException("This object cannot be stored into text file");
            }

            @Override
            protected float getDistanceImpl(MetaObject obj, float[] metaDistances, float distThreshold) {
                return Math.abs(getLocatorURI().hashCode() - obj.getLocatorURI().hashCode());
            }

            @Override
            public LocalAbstractObject getObject(String name) {
                return null;
            }

            @Override
            public Collection<String> getObjectNames() {
                return Collections.emptyList();
            }

            @Override
            public Collection<LocalAbstractObject> getObjects() {
                return Collections.emptyList();
            }

            @Override
            public int getObjectCount() {
                return 0;
            }
        };
    }

    /**
     * Retrieves an {@link LocalAbstractObject object} from the given named map and
     * returns it or its {@link LocalAbstractObject#clone(messif.objects.keys.AbstractObjectKey) clone}.
     * 
     * @param <T> the type of the object to retrieve from the map
     * @param objects the map of named objects
     * @param name the name of the object to retrieve
     * @param objectClass the class of the object to retrieve
     * @param clone flag whether to clone the retrieved object (<tt>true</tt>) or not (<tt>false</tt>)
     * @param cloneKey the object key to set for the clonned data
     * @return the object retrieved from the map or <tt>null</tt> if there was no such object
     * @throws ClassCastException if there was an object with the given name in the map, but it has a different class
     * @throws CloneNotSupportedException if the clonning was not supported by the object
     */
    protected static <T> T getObjectFromMap(Map<String, ? extends LocalAbstractObject> objects, String name, Class<? extends T> objectClass, boolean clone, AbstractObjectKey cloneKey) throws ClassCastException, CloneNotSupportedException {
        LocalAbstractObject object = objects.get(name);
        if (object == null)
            return null;
        else
            return objectClass.cast(clone ? object.clone(cloneKey) : object);
    }


    //****************** Attribute access ******************//

    /**
     * Returns the number of encapsulated objects.
     * @return the number of encapsulated objects
     */
    public abstract int getObjectCount();

    /**
     * Returns a set of symbolic names of the encapsulated objects.
     * @return a set of symbolic names of the encapsulated objects
     */
    public abstract Collection<String> getObjectNames();

    /**
     * Returns the encapsulated object for given symbolic name.
     *
     * @param name the symbolic name of the object to return
     * @return encapsulated object for given name or <tt>null</tt> if the key is unknown
     */
    public abstract LocalAbstractObject getObject(String name);

    /**
     * Returns <tt>true</tt> if there is an encapsulated object for given symbolic name.
     * @param name the symbolic name of the object to return
     * @return <tt>true</tt> if there is an encapsulated object for given symbolic name
     */
    public boolean containsObject(String name) {
        return getObject(name) != null;
    }

    /**
     * Returns a collection of all the encapsulated objects.
     * @return a collection of all the encapsulated objects
     */
    public Collection<LocalAbstractObject> getObjects() {
        Collection<LocalAbstractObject> objects = new ArrayList<LocalAbstractObject>(getObjectCount());
        for (String string : getObjectNames()) {
            objects.add(getObject(string));
        }
        return objects;
    }

    /**
     * Returns a collection of all the encapsulated objects associated with their symbolic names.
     * @return a map with symbolic names as keyas and the respective encapsulated objects as values
     */
    public Map<String, LocalAbstractObject> getObjectMap() {
        Map<String, LocalAbstractObject> ret = new HashMap<String, LocalAbstractObject>(getObjectCount());
        for (String name : getObjectNames())
            ret.put(name, getObject(name));
        return ret;
    }


    //****************** Text stream I/O helper method ******************//

    /**
     * Reads one object with the specified class name from the stream.
     * The class name is looked up first - it must be a descendant of {@link LocalAbstractObject}.
     * Then, a constructor with {@link BufferedReader} argument is used to load the object up.
     * 
     * @param stream the text stream to read the object from
     * @param className the name of the class for the object
     * @return a new instance of the object
     * @throws IOException if there was an error resolving the specified class or its constuctor or a problem
     *         occurred while reading from the stream
     */
    protected static LocalAbstractObject readObject(BufferedReader stream, String className) throws IOException {
        try {
            // Read the object
            return readObject(stream, Convert.getClassForName(className, LocalAbstractObject.class));
        } catch (ClassNotFoundException e) {
            throw new IOException("Can't create object from stream: " + e);
        }
    }

    /**
     * Reads one object with the specified class name from the stream.
     * The class name is looked up first - it must be a descendant of {@link LocalAbstractObject}.
     * Then, a constructor with {@link BufferedReader} argument is used to load the object up.
     * 
     * @param <E> the class of the object that is read from the stream
     * @param stream the text stream to read the object from
     * @param objectClass the class of the object that is read from the stream
     * @return a new instance of the object
     * @throws IOException if there was an error resolving the specified class or its constuctor or a problem
     *         occurred while reading from the stream
     */
    protected static <E> E readObject(BufferedReader stream, Class<E> objectClass) throws IOException {
        try {
            // Read the object
            return objectClass.getConstructor(BufferedReader.class).newInstance(stream);
        } catch (InstantiationException e) {
            throw new IOException("Can't create object from stream: " + e);
        } catch (IllegalAccessException e) {
            throw new IOException("Can't create object from stream: " + e);
        } catch (InvocationTargetException e) {
            throw new IOException("Can't create object from stream: " + e.getCause());
        } catch (NoSuchMethodException e) {
            throw new IOException("Can't create object from stream: " + e);
        }
    }

    /**
     * Utility method for reading metaobject header from a text stream.
     * This method is intended to be used in subclasses to implement the {@link BufferedReader} constructor.
     * Note that this method also reads the {@link #readObjectComments(java.io.BufferedReader) object comments}.
     *
     * @param stream the text stream to read the header from
     * @return the header that contains pairs of object names and classes
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     * @see #writeObjectsHeader(java.io.OutputStream, java.util.Map)
     * @see #writeObjects(java.io.OutputStream, java.util.Collection)
     */
    protected final String[] readObjectsHeader(BufferedReader stream) throws IOException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line = readObjectComments(stream);

        // The line should have format "URI;name1;class1;name2;class2;..." and URI can be skipped (including the semicolon)
        String[] uriNamesClasses = line.split(";");

        // Skip the first name if the number of elements is odd
        if (uriNamesClasses.length % 2 == 1) {
            if ((this.getObjectKey() == null) && (uriNamesClasses[0].length() > 0))
                setObjectKey(new AbstractObjectKey(uriNamesClasses[0]));
            String[] shiftArray = uriNamesClasses;
            uriNamesClasses = new String[uriNamesClasses.length - 1];
            System.arraycopy(shiftArray, 1, uriNamesClasses, 0, uriNamesClasses.length);
        }

        return uriNamesClasses;
    }

    /**
     * Utility method for reading objects from a text stream.
     * This method is intended to be used in subclasses to implement the {@link BufferedReader} constructor.
     * The {@code namesAndClasses} parameter can be retrieved by {@link #readObjectsHeader} method.
     *
     * @param stream the text stream to read the objects from
     * @param restrictNames if not <tt>null</tt> only the names specified in this collection are added to the objects table
     * @param namesAndClasses the list of names and object classes pairs
     *          (the name of the first object is the first item, the class of the first object is the second item, and so on)
     * @param objects the map into which the objects are stored
     * @return the filled objects map
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     * @see #writeObjectsHeader(java.io.OutputStream, java.util.Map)
     * @see #writeObjects(java.io.OutputStream, java.util.Collection)
     */
    protected final Map<String, LocalAbstractObject> readObjects(BufferedReader stream, Collection<String> restrictNames, String[] namesAndClasses, Map<String, LocalAbstractObject> objects) throws IOException {
        for (int i = 1; i < namesAndClasses.length; i += 2) { // Note that it is safer to keep i pointing to the class
            boolean readObject = restrictNames == null || restrictNames.contains(namesAndClasses[i - 1]);
            try {
                LocalAbstractObject object = readObject(stream, namesAndClasses[i]);
                if (readObject) {
                    if (object.getObjectKey() == null)
                        object.setObjectKey(this.getObjectKey());
                    objects.put(namesAndClasses[i - 1], object);
                }
            } catch (IOException e) {
                if (readObject) // Silently ignore errors on objects that are skipped
                    throw e;
            }
        }
        return objects;
    }

    /**
     * Utility method for writing a metaobject header to a given text stream.
     * Note that only non-null objects are written.
     *
     * @param stream the stream to write the header and encapsulated objects to
     * @param objects the objects the header of which to write
     * @return returns a collection of objects the header of which was written to the stream
     * @throws IOException if there was an error while writing to stream
     * @see #readObjectsHeader(java.io.BufferedReader)
     * @see #readObjects(java.io.BufferedReader, java.util.Collection, java.lang.String[], java.util.Map)
     */
    protected final Collection<LocalAbstractObject> writeObjectsHeader(OutputStream stream, Map<String, LocalAbstractObject> objects) throws IOException {
        Collection<LocalAbstractObject> objectsToWrite = new ArrayList<LocalAbstractObject>(objects.size());

        // Create first line with semicolon-separated names of classes
        Iterator<Entry<String, LocalAbstractObject>> iterator = objects.entrySet().iterator();
        for (int i = 0; iterator.hasNext(); i++) {
            Entry<String, ? extends LocalAbstractObject> entry = iterator.next();
            if (entry.getValue() != null) {
                objectsToWrite.add(entry.getValue());
                // Write object name and class
                stream.write(entry.getKey().getBytes());
                stream.write(';');
                stream.write(entry.getValue().getClass().getName().getBytes());
                // Do not append semicolon after the last object in the header
                if (iterator.hasNext())
                    stream.write(';');
            }
        }
        stream.write('\n');
        return objectsToWrite;
    }

    /**
     * Utility method for writing a the given objects to a text stream.
     * The {@code objects} parameter can be prepared by {@link #writeObjectsHeader} method.
     *
     * @param stream the stream to write the header and encapsulated objects to
     * @param objects the objects to write
     * @throws IOException if there was an error while writing to stream
     * @see #readObjectsHeader(java.io.BufferedReader)
     * @see #readObjects(java.io.BufferedReader, java.util.Collection, java.lang.String[], java.util.Map)
     */
    protected final void writeObjects(OutputStream stream, Collection<LocalAbstractObject> objects) throws IOException {
        // Write a line for every object from the list (skip the comments)
        for (LocalAbstractObject object : objects)
            object.write(stream, false);
    }


    //****************** Data equality ******************//

    /**
     * Indicates whether some other object has the same data as this one.
     * All the stored local objects for equality with the other metaobject's list.
     *
     * @param   obj   the reference object with which to compare (if it is not MetaObject, this method will return <code>false</code>)
     * @return  <code>true</code> if this object is the same as the obj
     *          argument; <code>false</code> otherwise.
     */
    @Override
    public boolean dataEquals(Object obj) {
        if (!(obj instanceof MetaObject))
            return false;
        MetaObject otherObj = (MetaObject)obj;
        for (String name : getObjectNames()) {
            LocalAbstractObject o1 = getObject(name);
            LocalAbstractObject o2 = otherObj.getObject(name);
            if (o1 == null || o2 == null) {
                if (o1 != null || o2 != null)
                    return false;
            } else {
                if (!o1.dataEquals(o2))
                    return false;
            }
        }

        return true;
    }

    /**
     * Returns sum of hash code values for all the encapsulated objects' data.
     * @return a hash code value for the data of this object
     */
    @Override
    public int dataHashCode() {
        int rtv = 0;
        for (LocalAbstractObject object : getObjects())
            rtv += object.dataHashCode();
        return rtv;
    }


    //****************** Distance function ******************//

    /**
     * The actual implementation of the metric function.
     * Method {@link #getDistance(messif.objects.LocalAbstractObject, float[], float)}
     * is called with <tt>null</tt> meta distances array in order to compute the
     * actual distance.
     *
     * @param obj the object to compute distance to
     * @param distThreshold the threshold value on the distance
     * @return the actual distance between obj and this if the distance is lower than distThreshold
     * @see LocalAbstractObject#getDistance
     */
    @Override
    protected final float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        return getDistanceImpl((MetaObject)obj, null, distThreshold);
    }

    /**
     * The actual implementation of the metric function.
     * If {@code metaDistances} parameter is not <tt>null</tt>, it should be filled
     * with the distances to the respective encapsulated objects (method
     * {@link #fillMetaDistances(messif.objects.MetaObject, float, float[]) fillMetaDistances}
     * can be used).
     *
     * @param obj the object to compute distance to
     * @param metaDistances the array that is filled with the distances of the respective encapsulated objects, if it is not <tt>null</tt>
     * @param distThreshold the threshold value on the distance
     * @return the actual distance between obj and this if the distance is lower than distThreshold
     * @see LocalAbstractObject#getDistance
     */
    protected abstract float getDistanceImpl(MetaObject obj, float[] metaDistances, float distThreshold);

    /**
     * Returns the array that can hold distances to the respective encapsulated objects.
     * This method returns a valid array only for descendants of {@link MetaObject},
     * otherwise <tt>null</tt> is returned.
     * @return the array that can hold distances to meta distances
     */
    @Override
    public float[] createMetaDistancesHolder() {
        return new float[getObjectCount()];
    }

    /**
     * Convenience method that fills the given {@code metaDistances} array with distances.
     * Every item of the array is filled with the distance between
     * the encapsulated object stored in this metaobject under the name given in the
     * respective item of {@code objectNames} and the encapsulated object stored
     * in {@code obj} metaobject under the same name. If any of the two objects
     * are <tt>null</tt>, the value of {@code unknownDistance} parameter is filled.
     *
     * @param obj the object to compute distance to
     * @param distThreshold the threshold value on the distance
     * @param metaDistances the array that is filled with the distances of the respective encapsulated objects, if it is not <tt>null</tt>
     * @param objectNames the list of names of encapsulated objects to retrieve
     *          from this and {@code obj} (must have the same number of items as {@code metaDistances}
     * @param unknownDistance the distance to fill if either this or obj's encapsulated object is <tt>null</tt>
     * @return the number of computed distances (i.e. the number of distTreshold items minus
     *          the number of <tt>null</tt> objects)
     * @see LocalAbstractObject#getDistance
     */
    protected final int fillMetaDistances(MetaObject obj, float distThreshold, float[] metaDistances, String[] objectNames, float unknownDistance) {
        int count = 0;
        for (int i = 0; i < metaDistances.length; i++) {
            LocalAbstractObject obj1 = getObject(objectNames[i]);
            LocalAbstractObject obj2 = obj.getObject(objectNames[i]);
            if (obj1 == null || obj2 == null) {
                metaDistances[i] = unknownDistance;
            } else {
                count++;
                metaDistances[i] = obj1.getDistanceImpl(obj2, distThreshold);
            }
        }
        return count;
    }

    /**
     * Convenience method that fills the given {@code metaDistances} array with distances.
     * Every item of the array is filled with the distance between
     * all the encapsulated objects stored in this metaobject and the respective
     * (using the same name) encapsulated object in {@code obj}. If any of the two objects
     * are <tt>null</tt>, the value of {@link #UNKNOWN_DISTANCE} is filled.
     *
     * @param obj the object to compute distance to
     * @param distThreshold the threshold value on the distance
     * @param metaDistances the array that is filled with the distances of the respective encapsulated objects, if it is not <tt>null</tt>
     * @return the number of computed distances (i.e. the number of distTreshold items minus
     *          the number of <tt>null</tt> objects)
     * @see LocalAbstractObject#getDistance
     */
    protected final int fillMetaDistances(MetaObject obj, float distThreshold, float[] metaDistances) {
        return fillMetaDistances(obj, distThreshold, metaDistances, getObjectNames().toArray(new String[getObjectCount()]), UNKNOWN_DISTANCE);
    }

    /**
     * Convenience method that allows to call the metric function implementation
     * directly for encapsulated objects (so that the statistics and caching does not apply).
     * Note that if either {@code o1} or {@code o2} is <tt>null</tt>, the
     * {@link #UNKNOWN_DISTANCE} is returned.
     *
     * @param o1 the object to compute distance from
     * @param o2 the object to compute distance to
     * @param distThreshold the threshold value on the distance
     * @return the actual distance between o1 and o2 if the distance is lower than distThreshold or
     *          {@link #UNKNOWN_DISTANCE} if either {@code o1} or {@code o2} is <tt>null</tt>
     * @see LocalAbstractObject#getDistance(LocalAbstractObject, float) LocalAbstractObject.getDistance
     */
    protected static float implementationGetDistance(LocalAbstractObject o1, LocalAbstractObject o2, float distThreshold) {
        if (o1 == null || o2 == null)
            return UNKNOWN_DISTANCE;
        return o1.getDistanceImpl(o2, distThreshold);
    }


    //****************** Additional overrides ******************//

    /**
     * Returns the size of this object in bytes.
     * Calculates the sum of sizes of all encapsulated objects.
     * @return the size of this object in bytes
     */
    @Override
    public int getSize() {
        int size = 0;
        for (LocalAbstractObject object : getObjects())
            if (object != null)
                size += object.getSize();
        return size;
    }

    /**
     * Clear non-messif data stored in this object and all its subobjects.
     * This method is intended to be called whenever the object is
     * sent back to client in order to minimize problems with unknown
     * classes after deserialization.
     */
    @Override
    public void clearSurplusData() {
        super.clearSurplusData();
        for (LocalAbstractObject object : getObjects())
            if (object != null)
                object.clearSurplusData();
    }

    /**
     * Returns a string representation of this metaobject.
     * @return a string representation of this metaobject
     */
    @Override
    public String toString() {
        return new StringBuffer(super.toString()).append(" ").append(getObjectNames()).toString();
    }


    //************ Protected methods of BinarySerializable interface ************//

    /**
     * Creates a new instance of MetaObject loaded from binary input.
     * 
     * @param input the input to read the MetaObject from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected MetaObject(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }

}
