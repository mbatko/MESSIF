/*
 * MetaObject.java
 *
 * Created on 28. brezen 2007, 17:42
 *
 */

package messif.objects;

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
 * @author xbatko
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
            protected float getDistanceImpl(LocalAbstractObject obj, float[] metaDistances, float distThreshold) {
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
    protected LocalAbstractObject readObject(BufferedReader stream, String className) throws IOException {
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
    protected <E> E readObject(BufferedReader stream, Class<E> objectClass) throws IOException {
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


    //****************** Data equality ******************//

    /**
     * Indicates whether some other object has the same data as this one.
     * All the stored local objects for equality with the other metaobject's list.
     *
     * @param   obj   the reference object with which to compare (if it is not MetaObject, this method will return <code>false</code>)
     * @return  <code>true</code> if this object is the same as the obj
     *          argument; <code>false</code> otherwise.
     */
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
    protected final float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        return getDistanceImpl(obj, null, distThreshold);
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
    @Override
    protected abstract float getDistanceImpl(LocalAbstractObject obj, float[] metaDistances, float distThreshold);

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
