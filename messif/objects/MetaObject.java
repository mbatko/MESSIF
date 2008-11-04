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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import messif.objects.nio.BinaryInputStream;
import messif.objects.nio.BinarySerializator;
import messif.statistics.Statistics;
import messif.utility.Convert;

/**
 * Represents a collection of LocalAbstractObjects encapsulated as one object.
 * <p>
 * All the encapsulated objects share the same locator URI.
 * The metric distance function for this object is the absolute value of the
 * differences of locatorURI hashcodes.
 * </p>
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
            public Map<String, LocalAbstractObject> getObjectMap() {
                return Collections.emptyMap();
            }

            @Override
            public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
               return clone(false);
            }

            @Override
            protected void writeData(OutputStream stream) throws IOException {
                throw new UnsupportedOperationException("This object cannot be stored into text file");
            }
        };
    }


    //****************** Attribute access ******************//

    /**
     * Returns a collection of all the encapsulated objects associated with their symbolic names.
     * Note that the collection can contain <tt>null</tt> values.
     * @return a map with symbolic names as keyas and the respective encapsulated objects as values
     */
    public abstract Map<String, LocalAbstractObject> getObjectMap();

    /**
     * Returns the encapsulated object for given symbolic name.
     *
     * @param name the symbolic name of the object to return
     * @return encapsulated object for given name or <tt>null</tt> if the key is unknown
     */
    public LocalAbstractObject getObject(String name) {
        return getObjectMap().get(name);
    }

    /**
     * Returns a set of symbolic names of the encapsulated objects.
     * @return a set of symbolic names of the encapsulated objects
     */
    public Collection<String> getObjectNames() {
        return getObjectMap().keySet();
    }

    /**
     * Returns a collection of all the encapsulated objects.
     * @return a collection of all the encapsulated objects
     */
    public Collection<LocalAbstractObject> getObjects() {
        return getObjectMap().values();
    }

    /**
     * Returns <tt>true</tt> if there is an encapsulated object for given symbolic name.
     * @param name the symbolic name of the object to return
     * @return <tt>true</tt> if there is an encapsulated object for given symbolic name
     */
    public boolean containsObject(String name) {
        return getObject(name) != null;
    }

    /**
     * Returns the number of encapsulated objects.
     * @return the number of encapsulated objects
     */
    public int getObjectCount() {
        return getObjectMap().size();
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
        Map<String, LocalAbstractObject> otherObjects = ((MetaObject)obj).getObjectMap();

        // Compare the data of the respective objects (name-compatible)
        for (Entry<String, LocalAbstractObject> entry : getObjectMap().entrySet()) {
            LocalAbstractObject o1 = entry.getValue();
            LocalAbstractObject o2 = otherObjects.get(entry.getKey());
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
    @Override
    protected final float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        return getDistanceImpl(obj, null, distThreshold);
    }

    /**
     * Metric distance function.
     * Measures the distance between this object and <code>obj</code>.
     * The array <code>metaDistances</code> is filled with the distances
     * of the respective encapsulated objects.
     * 
     * <p>
     * Note that this method does not use the fast access to the 
     * {@link messif.objects.PrecomputedDistancesFilter#getPrecomputedDistance precomputed distances}
     * even if there is a filter that supports it.
     * </p>
     *
     * @param obj the object to compute distance to
     * @param metaDistances the array that is filled with the distances of the respective encapsulated objects, if it is not <tt>null</tt>
     * @param distThreshold the threshold value on the distance
     * @return the actual distance between obj and this if the distance is lower than distThreshold.
     *         Otherwise the returned value is not guaranteed to be exact, but in this respect the returned value
     *         must be greater than the threshold distance.
     */
    public final float getDistance(LocalAbstractObject obj, float[] metaDistances, float distThreshold) {
        // This check is to enhance performance when statistics are disabled
        if (Statistics.isEnabledGlobally())
            counterDistanceComputations.add();

        return getDistanceImpl(obj, metaDistances, distThreshold);
    }

    /**
     * The actual implementation of the metric function.
     * The distance is computed as the difference of this and <code>obj</code>'s locator hash-codes.
     * The array <code>metaDistances</code> is ignored.
     *
     * @param obj the object to compute distance to
     * @param metaDistances the array that is filled with the distances of the respective encapsulated objects, if it is not <tt>null</tt>
     * @param distThreshold the threshold value on the distance
     * @return the actual distance between obj and this if the distance is lower than distThreshold
     * @see LocalAbstractObject#getDistance
     */
    protected float getDistanceImpl(LocalAbstractObject obj, float[] metaDistances, float distThreshold) {
        return Math.abs(getLocatorURI().hashCode() - obj.getLocatorURI().hashCode());
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
     * Creates a new instance of MetaObject loaded from binary input stream.
     * 
     * @param input the stream to read the MetaObject from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the stream
     */
    protected MetaObject(BinaryInputStream input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }

}
