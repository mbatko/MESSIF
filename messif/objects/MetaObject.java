/*
 * MetaObject.java
 *
 * Created on 28. brezen 2007, 17:42
 *
 */

package messif.objects;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import messif.objects.nio.BinaryInputStream;
import messif.objects.nio.BinarySerializator;
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
            protected LocalAbstractObject addObject(String name, LocalAbstractObject object) throws IllegalArgumentException {
                throw new UnsupportedOperationException("Not supported");
            }

            @Override
            protected LocalAbstractObject removeObject(String name) {
                throw new UnsupportedOperationException("Not supported");
            }

            @Override
            protected void removeObjects() {
                throw new UnsupportedOperationException("Not supported");
            }

            @Override
            public LocalAbstractObject getObject(String name) {
                return null;
            }

            @Override
            public Collection<String> getObjectNames() {
                return Collections.emptySet();
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


    //****************** Clonning ******************//

    /** 
     * Creates and returns a randomly modified copy of this object. 
     * The modification depends on particular subclass implementation.
     *
     * @param args any parameters required by the subclass implementation - usually two objects with 
     *        the miminal and the maximal possible values
     * @return a randomly modified clone of this instance
     * @throws CloneNotSupportedException if the object's class does not support clonning or there was an error
     */
    @Override
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        MetaObject objectClone = (MetaObject)clone(true);
        // Replace all sub-objects with random-modified clones
        for (String name : getObjectNames())
            objectClone.addObject(name, getObject(name).cloneRandomlyModify(args));
        return objectClone;
    }


    /****************** Attribute access ******************/

    /**
     * Adds an object to the encapsulated collection.
     * If the collection already contains that name, the object will be replaced.
     * 
     * @param name the symbolic name of the encapsulated object
     * @param object the object to encapsulate
     * @return the previous encapsulated object with the specified symbolic name
     *         or <tt>null</tt> if the collection has been enlarged
     * @throws IllegalArgumentException if the name or object is invalid
     */
    protected abstract LocalAbstractObject addObject(String name, LocalAbstractObject object) throws IllegalArgumentException;

    /**
     * Add several objects to the encapsulated collection.
     * The keys of the map provide the name of the encapsulated object.
     * @param objects pairs of the symbolic name and the encapsulated object to add
     * @throws IllegalArgumentException if any of the names or objects is invalid, note that some objects may have been already added
     */
    protected void addObjects(Map<String, ? extends LocalAbstractObject> objects) throws IllegalArgumentException {
        for (Map.Entry<String, ? extends LocalAbstractObject> entry : objects.entrySet())
                addObject(entry.getKey(), entry.getValue());
    }

    /**
     * Adds an object clone to the encapsulated collection.
     * If the collection already contains that name, the object will be replaced.
     * 
     * @param name the symbolic name of the encapsulated object
     * @param object the object whose clone to encapsulate
     * @return the previous encapsulated object with the specified symbolic name
     *         or <tt>null</tt> if the collection has been enlarged
     * @throws IllegalArgumentException if the name or object is invalid
     * @throws CloneNotSupportedException if the object's class does not support clonning or there was an error
     */
    protected LocalAbstractObject addObjectClone(String name, LocalAbstractObject object) throws IllegalArgumentException, CloneNotSupportedException {
        return addObject(name, object.clone(objectKey));
    }

    /**
     * Add several clones of specified objects to the encapsulated collection.
     * The keys of the map provide the name of the encapsulated object.
     * @param objects pairs of the symbolic name and the encapsulated object to add
     * @throws IllegalArgumentException if any of the names or objects is invalid, note that some objects may have been already added
     * @throws CloneNotSupportedException if the object's class does not support clonning or there was an error
     */
    protected void addObjectClones(Map<String, ? extends LocalAbstractObject> objects) throws IllegalArgumentException, CloneNotSupportedException {
        for (Map.Entry<String, ? extends LocalAbstractObject> entry : objects.entrySet())
                addObjectClone(entry.getKey(), entry.getValue());
    }

    /**
     * Removes an object from the encapsulated collection.
     * If the collection does not contains that name, <tt>null</tt> is
     * returned and collection is left untouched.
     * 
     * @param name the symbolic name of the encapsulated object to remove
     * @return the removed encapsulated object or <tt>null</tt> if the synbolic name was not found
     */
    protected abstract LocalAbstractObject removeObject(String name);

    /**
     * Removes all objects from the encapsulated collection.
     */
    protected abstract void removeObjects();

    /**
     * Returns the encapsulated object for given symbolic name.
     *
     * @param name the symbolic name of the object to return
     * @return encapsulated object for given name or <tt>null</tt> if the key is unknown
     */
    public abstract LocalAbstractObject getObject(String name);

    /**
     * Returns the set of symbolic names of the encapsulated objects.
     * @return the set of symbolic names of the encapsulated objects
     */
    public abstract Collection<String> getObjectNames();

    /**
     * Returns a collection of all the encapsulated objects.
     * Note that the collection can contain <tt>null</tt> values.
     * @return a collection all the encapsulated objects
     */
    public abstract Collection<LocalAbstractObject> getObjects();

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
    public abstract int getObjectCount();


    /****************** Text stream I/O ******************/

    /**
     * Fills this instance of MetaObject from a text stream.
     * @param stream the text stream to read the objects from
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    protected void readObjects(BufferedReader stream) throws IOException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line;
        do {
            line = stream.readLine();
            if (line == null)
                throw new EOFException("EoF reached while initializing MetaObject.");
        } while (processObjectComment(line));

        // The line should have format "URI;name1;class1;name2;class2;..." and URI can be skipped (including the semicolon)
        String[] uriNamesClasses = line.split(";");

        // Skip the first name if the number of elements is odd
        int i = uriNamesClasses.length % 2;

        // If the URI locator is used (and it is not set from the previous - this is the old format)
        if (i == 1) {
            if ((this.objectKey == null) && (uriNamesClasses[0].length() > 0))
                    this.objectKey = new AbstractObjectKey(uriNamesClasses[0]);
        }

        // Read objects and add them to the collection
        for (i++; i < uriNamesClasses.length; i += 2) {
            LocalAbstractObject object = readObject(stream, uriNamesClasses[i]);
            if (object != null) {
                addObject(uriNamesClasses[i - 1], object);
                object.objectKey = this.objectKey;
            }
        }
    }

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
            return Convert.getClassForName(className, LocalAbstractObject.class).getConstructor(BufferedReader.class).newInstance(stream);
        } catch (ClassNotFoundException e) {
            throw new IOException("Can't create object from stream: " + e);
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
     * Store this object to a text stream.
     * This method should have the opposite deserialization in constructor of a given object class.
     *
     * @param stream the stream to store this object to
     * @throws IOException if there was an error while writing to stream
     */
    protected void writeData(OutputStream stream) throws IOException {
        Collection<LocalAbstractObject> objects = new ArrayList<LocalAbstractObject>(getObjectCount());
        
        // Create first line with semicolon-separated names of classes
        for (String objectName : getObjectNames()) {
            LocalAbstractObject object = getObject(objectName);
            if (object != null) {
                // Do not prepend semicolon for the first object in the header
                if (!objects.isEmpty())
                    stream.write(';');
                // Write object name and class
                stream.write(objectName.getBytes());
                stream.write(';');
                stream.write(object.getClass().getName().getBytes());
                // Add object to collection that will be written later
                objects.add(object);
            }
        }
        stream.write('\n');
        
        // Write a line for every object from the list (skip the comments)
        for (LocalAbstractObject object : objects)
            object.write(stream, false);
    }


    /****************** Data equality ******************/

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
        MetaObject castObj = (MetaObject)obj;
        
        // Get the names of this object and compare them to the other one
        Collection<String> objectNames = getObjectNames();
        if (!objectNames.equals(castObj.getObjectNames()))
            return false;

        // Compare the data of the respective objects (name-compatible)
        for (String objectName : objectNames) {
            LocalAbstractObject o1 = getObject(objectName);
            LocalAbstractObject o2 = castObj.getObject(objectName);
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


    /****************** Distance function ******************/

    /**
     * The actual implementation of the metric function.
     * The distance is computed as the difference of this and <code>obj</code>'s locator hash-codes.
     *
     * @param obj the object to compute distance to
     * @param distThreshold the threshold value on the distance
     * @return the actual distance between obj and this if the distance is lower than distThreshold
     * @see LocalAbstractObject#getDistance
     */
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        return Math.abs(getLocatorURI().hashCode() - obj.getLocatorURI().hashCode());
    }


    /****************** Additional overrides ******************/

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
