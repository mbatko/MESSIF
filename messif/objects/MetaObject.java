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
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import messif.objects.nio.BinaryInputStream;
import messif.objects.nio.BinaryOutputStream;
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
public class MetaObject extends LocalAbstractObject {
    
    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;
    
    /****************** Attributes ******************/
    
    /** List of internal descriptors */
    protected SortedMap<String, LocalAbstractObject> objects;
    
    
    /****************** Constructors ******************/
    
    /**
     * Creates a new instance of MetaObject with empty descriptor list.
     * This is inteded to be used for querying (using the MetaObject's distance function, which is locatorURI based).
     * @param locatorURI the locator URI for this object and all the provided objects will be set as well
     */
    public MetaObject(String locatorURI) {
        if (locatorURI != null)
            this.objectKey = new AbstractObjectKey(locatorURI);
        this.objects = new TreeMap<String, LocalAbstractObject>();
    }
    
    /**
     * Creates a new instance of MetaObject from a collection of named objects.
     * The locatorURI of every object from the collection is set to the provided one.
     *
     * @param locatorURI the locator URI for this object and all the provided objects will be set as well
     * @param objects collection of objects with their symbolic names
     * @param cloneObjects if <tt>true</tt> the provided <code>objects</code> will be cloned, otherwise the
     *        the locators of the provided <code>objects</code> will be replaced by the specified one
     * @throws CloneNotSupportedException if the clonning of the <code>objects</code> was unsuccessful
     */
    public MetaObject(String locatorURI, Map<String, LocalAbstractObject> objects, boolean cloneObjects) throws CloneNotSupportedException {
        this(locatorURI);
        for (Map.Entry<String, LocalAbstractObject> entry : objects.entrySet()) {
            LocalAbstractObject object = entry.getValue();
            // Set locator to the same value as this metaobject will have
            if (cloneObjects)
                object = object.clone(objectKey);
            else object.objectKey = objectKey;
            this.objects.put(entry.getKey(), object);
        }
    }

    /**
     * Creates a new instance of MetaObject from a collection of named objects.
     * The locatorURI of every object from the collection is set to the provided one.
     * Objects are not clonned.
     *
     * @param locatorURI the locator URI for this object and all the provided objects will be set as well
     * @param objects collection of objects with their symbolic names
     */
    public MetaObject(String locatorURI, Map<String, LocalAbstractObject> objects) {
        this(locatorURI);
        for (Map.Entry<String, LocalAbstractObject> entry : objects.entrySet()) {
            LocalAbstractObject object = entry.getValue();
            object.objectKey = objectKey;
            this.objects.put(entry.getKey(), object);
        }
    }

    /**
     * Creates a new instance of MetaObject from a text stream.
     * @param stream the text stream to read an object from
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public MetaObject(BufferedReader stream) throws IOException {
        this(stream, null);
    }

    /**
     * Creates a new instance of MetaObject from a text stream with a restriction on object names.
     * @param stream the text stream to read an object from
     * @param restrictedNames only encapsulated objects with names in this set are loaded (no restriction is used if <tt>null</tt>)
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public MetaObject(BufferedReader stream, Set<String> restrictedNames) throws IOException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line;
        do {
            line = stream.readLine();
            if (line == null)
                throw new EOFException("EoF reached while initializing MetaObject.");
        } while (processObjectComment(line));
        
        try {
            // The line should have format "URI;name1;class1;name2;class2;..." and URI can be empty
            String[] uriNamesClasses = line.split(";");
            int startIndex = 0;
            
            // if the URI locator is used
            if (uriNamesClasses.length % 2 == 1) {
                // Set locator, if it has not been set already and if it is not empty (otherwise it stays null)
                if ((this.objectKey == null) && (uriNamesClasses[0].length() > 0))
                    this.objectKey = new AbstractObjectKey(uriNamesClasses[0]);
                startIndex = 1;
            }
            
            // Initialize the object array
            this.objects = new TreeMap<String, LocalAbstractObject>();
            
            // For every class (as string), call the streaming constructor to create a LocalAbstractObject
            for (int i = startIndex; i < uriNamesClasses.length; i += 2) {
                // Read the object (must do that even if restricting names)
                LocalAbstractObject obj = Convert.getClassForName(uriNamesClasses[i + 1], LocalAbstractObject.class).getConstructor(BufferedReader.class).newInstance(stream);

                if (restrictedNames == null || restrictedNames.contains(uriNamesClasses[i])) {
                    // Set locator to the same value as this meta object
                    obj.objectKey = this.objectKey;
                    // Add the created object to our list
                    objects.put(uriNamesClasses[i], obj);
                }
            }
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
    
    
    /****************** Attribute access ******************/
    
    /**
     * Returns all encapsulated objects with their symbolic names as key.
     *
     * @return all encapsulated objects with their symbolic names as key
     */
    public SortedMap<String, LocalAbstractObject> getObjects() {
        return Collections.unmodifiableSortedMap(objects);
    }

    /**
     * Returns the encapsulated object for given symbolic name.
     *
     * @param name the symbolic name of the object to return
     * @return encapsulated object for given name or <tt>null</tt> if the key is unknown
     */
    public LocalAbstractObject getObject(String name) {
        return objects.get(name);
    }
    
    /**
     * Returns the set of symbolic names of the encapsulated objects.
     * @return the set of symbolic names of the encapsulated objects
     */
    public Set<String> getObjectNames() {
        return objects.keySet();
    }

    /**
     * Returns <tt>true</tt> if there is an encapsulated object for given symbolic name.
     * @param name the symbolic name of the object to return
     * @return <tt>true</tt> if there is an encapsulated object for given symbolic name
     */
    public boolean containsObject(String name) {
        return objects.containsKey(name);
    }
    
    /**
     * Returns the number of encapsulated objects.
     * @return the number of encapsulated objects
     */
    public int getObjectCount() {
        return objects.size();
    }
    
    /****************** Overriden methods ******************/
    
    /**
     * Returns the size of this object in bytes.
     * @return the size of this object in bytes
     */
    public int getSize() {
        int rtv = 0;
        for (LocalAbstractObject object : objects.values())
            rtv += object.getSize();
        return rtv;
    }
    
    /**
     * Store this object to a text stream.
     * This method should have the opposite deserialization in constructor of a given object class.
     *
     * @param stream the stream to store this object to
     * @throws IOException if there was an error while writing to stream
     */
    public void writeData(OutputStream stream) throws IOException {
        // Create first line with semicolon-separated names of classes
        for (Iterator<Map.Entry<String, LocalAbstractObject>> it = objects.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, LocalAbstractObject> entry = it.next();
            stream.write(entry.getKey().getBytes());
            stream.write(';');
            stream.write(entry.getValue().getClass().getName().getBytes());
            if (it.hasNext())
                stream.write(';');
        }
        stream.write('\n');
        
        // Write a line for every object from the list (skip the comments)
        for (LocalAbstractObject object : objects.values())
            object.write(stream, false);
    }

    /**
     * Metric distance function with threshold.
     * Returns the distance between the hashcode of the locator of this object
     * and the hashcode of the locator of the object that is supplied as argument.
     *
     * @param obj the object for which to measure the distance
     * @param distThreshold the threshold value on the distance (it is not used for the MetaObject class)
     * @return the distance between this object and the provided object <code>obj</code>
     */
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        return Math.abs(getLocatorURI().hashCode() - obj.getLocatorURI().hashCode());
    }
    
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
        
        // Get iterators of local objects for both MetaObjects (this and obj)
        Iterator<LocalAbstractObject> firstIterator = objects.values().iterator();
        Iterator<LocalAbstractObject> secondIterator = ((MetaObject)obj).objects.values().iterator();
        
        // Compare data of respective objects (the position is preserved) for equality
        while (firstIterator.hasNext() && secondIterator.hasNext())
            if (!firstIterator.next().dataEquals(secondIterator.next()))
                return false;
        
        // If both the metaobjects has the same number of object, we can return true
        return !firstIterator.hasNext() && !secondIterator.hasNext();
    }
    
    /**
     * Returns a hash code value for the object data.
     * @return a hash code value for the data of this object
     */
    public int dataHashCode() {
        int rtv = 0;
        for (LocalAbstractObject object : objects.values())
            rtv += object.dataHashCode();
        return rtv;
    }
    
    
    /**
     * Creates and returns a copy of this object. The precise meaning 
     * of "copy" may depend on the class of the object.
     * @param cloneFilterChain  the flag wheter the filter chain must be cloned as well.
     * @return a clone of this instance.
     * @throws CloneNotSupportedException if the object's class does not support clonning or there was an error
     */
    @Override
    public LocalAbstractObject clone(boolean cloneFilterChain) throws CloneNotSupportedException {
        MetaObject rtv = (MetaObject)super.clone(cloneFilterChain);
        
        rtv.objects = new TreeMap<String, LocalAbstractObject>();
        
        for (Map.Entry<String, LocalAbstractObject> entry : objects.entrySet()) {
            rtv.objects.put(entry.getKey(), entry.getValue().clone(cloneFilterChain));
        }
        return rtv;
    }

    /**
     * Creates and returns a randomly modified copy of this meta object.
     * It clones and randomly modifies all objects in this meta object.
     *
     * @param  args  expected size of the array is 2: <ul>
     *      <li><b>minMetaObject</b> a meta object containing the minimal values for all objects</li>
     *      <li><b>maxMetaObject</b> a meta object containing the maximal values for all objects</li></ul>
     * @return a randomly modified clone of this instance.
     * @throws CloneNotSupportedException if the object's class does not support clonning or there was an error
     */
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        // call the SUPER.clone here
        MetaObject rtv = (MetaObject) super.clone();
        rtv.objects = new TreeMap<String, LocalAbstractObject>();
        
        try {
            MetaObject minMetaObject = (MetaObject) args[0];
            MetaObject maxMetaObject = (MetaObject) args[1];
            Random random = new Random(System.currentTimeMillis());
            
            // clone and modify all objects in this meta object
            for (Map.Entry<String, LocalAbstractObject> entry : objects.entrySet()) {
                LocalAbstractObject originalObject = entry.getValue();
                LocalAbstractObject modifiedObject = originalObject.cloneRandomlyModify(
                        minMetaObject.getObject(entry.getKey()), maxMetaObject.getObject(entry.getKey())
                        );
                rtv.objects.put(new String(entry.getKey()), modifiedObject);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return (LocalAbstractObject) this.clone();
        } catch (ClassCastException e) { 
            return (LocalAbstractObject) this.clone();
        } catch (NullPointerException e) {
            return (LocalAbstractObject) this.clone();
        }
        
        return rtv;
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
        for (LocalAbstractObject object : objects.values())
            object.clearSurplusData();
    }

    /**
     * Returns a string representation of this metaobject.
     * @return a string representation of this metaobject
     */
    @Override
    public String toString() {
        return new StringBuffer(super.toString()).append(" ").append(objects.keySet()).toString();
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
        int items = serializator.readInt(input);
        this.objects = new TreeMap<String, LocalAbstractObject>();
        for (int i = 0; i < items; i++)
            objects.put(serializator.readString(input), serializator.readObject(input, LocalAbstractObject.class));
    }

    /**
     * Binary-serialize this object into the <code>output</code>.
     * @param output the data output this object is binary-serialized into
     * @param serializator the serializator used to write objects
     * @return the number of bytes actually written
     * @throws IOException if there was an I/O error during serialization
     */
    @Override
    protected int binarySerialize(BinaryOutputStream output, BinarySerializator serializator) throws IOException {
        int size = super.binarySerialize(output, serializator);
        size += serializator.write(output, objects.size());
        for (Entry<String, LocalAbstractObject> entry : objects.entrySet()) {
            size += serializator.write(output, entry.getKey());
            size += serializator.write(output, entry.getValue());
        }
        return size;
    }

    /**
     * Returns the exact size of the binary-serialized version of this object in bytes.
     * @param serializator the serializator used to write objects
     * @return size of the binary-serialized version of this object
     */
    @Override
    protected int getBinarySize(BinarySerializator serializator) {
        int size = super.getBinarySize(serializator) + 4;
        for (Entry<String, LocalAbstractObject> entry : objects.entrySet())
            size += serializator.getBinarySize(entry.getKey()) + serializator.getBinarySize(entry.getValue());
        return size;
    }

}
