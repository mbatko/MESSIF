/*
 * MetaObjectMap
 *
 */

package messif.objects.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import messif.objects.keys.AbstractObjectKey;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

/**
 * Implementation of {@link MetaObject} that stores encapsulated objects
 * in a hash table.
 * The metric distance function for this object is the absolute value of the
 * differences of locatorURI hashcodes.
 * 
 * @author xbatko
 */
public class MetaObjectMap extends MetaObject implements BinarySerializable {

    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;

    /****************** Attributes ******************/

    /** List of encapsulated objects */
    protected Map<String, LocalAbstractObject> objects;


    /****************** Constructors ******************/
    
    /**
     * Creates a new instance of MetaObjectMap from a collection of named objects.
     * The locatorURI of every object from the collection is set to the provided one.
     *
     * @param locatorURI the locator URI for this object and all the provided objects will be set as well
     * @param objects collection of objects with their symbolic names
     * @param cloneObjects if <tt>true</tt> the provided <code>objects</code> will be cloned, otherwise the
     *        the locators of the provided <code>objects</code> will be replaced by the specified one
     * @throws CloneNotSupportedException if the clonning of the <code>objects</code> was unsuccessful
     */
    public MetaObjectMap(String locatorURI, Map<String, LocalAbstractObject> objects, boolean cloneObjects) throws CloneNotSupportedException {
        super(locatorURI);
        if (cloneObjects) {
            this.objects = new TreeMap<String, LocalAbstractObject>();
            for (Entry<String, LocalAbstractObject> entry : objects.entrySet())
                this.objects.put(entry.getKey(), (LocalAbstractObject)entry.getValue().clone(getObjectKey()));
        } else {
            this.objects = new TreeMap<String, LocalAbstractObject>(objects);
        }
    }

    /**
     * Creates a new instance of MetaObjectMap from a collection of named objects.
     * The locatorURI of every object from the collection is set to the provided one.
     *
     * @param locatorURI the locator URI for this object and all the provided objects will be set as well
     * @param objects collection of objects with their symbolic names
     */
    public MetaObjectMap(String locatorURI, Map<String, LocalAbstractObject> objects) {
        super(locatorURI);
        this.objects = new TreeMap<String, LocalAbstractObject>(objects);
    }

    /**
     * Creates a new instance of MetaObjectMap from a text stream.
     * Only objects for names specified in <code>restrictNames</code> are added.
     * @param stream the text stream to read an object from
     * @param restrictNames if not <tt>null</tt> only the names specified in this collection are added to the objects table
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public MetaObjectMap(BufferedReader stream, Set<String> restrictNames) throws IOException {
        this.objects = new TreeMap<String, LocalAbstractObject>();
        readObjects(stream, restrictNames);
    }    

    /**
     * Creates a new instance of MetaObjectMap from a text stream.
     * Only objects for names specified in <code>restrictNames</code> are added.
     * @param stream the text stream to read an object from
     * @param restrictNames if not <tt>null</tt> only the names specified in this collection are added to the objects table
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public MetaObjectMap(BufferedReader stream, String[] restrictNames) throws IOException {
        this(stream, (restrictNames == null)?null:new HashSet<String>(Arrays.asList(restrictNames)));
    }

    /**
     * Creates a new instance of MetaObjectMap from a text stream.
     * @param stream the text stream to read an object from
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public MetaObjectMap(BufferedReader stream) throws IOException {
        this(stream, (Set<String>)null);
    }    


    //****************** Factory method ******************//

    /**
     * Creates a meta object from the specified file.
     * The constructor from {@link BufferedReader} is used.
     * @param file the file to create the object from
     * @return a new instance of MetaObjectMap
     * @throws IOException if there was an error reading from the specified file
     */
    public static MetaObjectMap create(File file) throws IOException {
        return new MetaObjectMap(new BufferedReader(new InputStreamReader(new FileInputStream(file))));
    }


    //****************** Clonning ******************//

    /**
     * Creates and returns a copy of this object. The precise meaning 
     * of "copy" may depend on the class of the object.
     * @param cloneFilterChain  the flag wheter the filter chain must be cloned as well.
     * @return a clone of this instance.
     * @throws CloneNotSupportedException if the object's class does not support clonning or there was an error
     */
    @Override
    public LocalAbstractObject clone(boolean cloneFilterChain) throws CloneNotSupportedException {
        MetaObjectMap rtv = (MetaObjectMap)super.clone(cloneFilterChain);
        
        rtv.objects = new TreeMap<String, LocalAbstractObject>();
        
        for (Map.Entry<String, LocalAbstractObject> entry : objects.entrySet())
            rtv.objects.put(entry.getKey(), entry.getValue().clone(cloneFilterChain));

        return rtv;
    }

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
        MetaObjectMap objectClone = (MetaObjectMap)clone(true);
        // Replace all sub-objects with random-modified clones
        for (String name : getObjectNames())
            objectClone.objects.put(name, getObject(name).cloneRandomlyModify(args));
        return objectClone;
    }


    //****************** Text stream I/O ******************//

    /**
     * Fills this instance of MetaObject from a text stream.
     * @param stream the text stream to read the objects from
     * @param restrictNames if not <tt>null</tt> only the names specified in this collection are added to the objects table
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    protected void readObjects(BufferedReader stream, Collection<String> restrictNames) throws IOException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line = readObjectComments(stream);

        // The line should have format "URI;name1;class1;name2;class2;..." and URI can be skipped (including the semicolon)
        String[] uriNamesClasses = line.split(";");

        // Skip the first name if the number of elements is odd
        int i = uriNamesClasses.length % 2;

        // If the URI locator is used (and it is not set from the previous - this is the old format)
        if (i == 1) {
            if ((this.getObjectKey() == null) && (uriNamesClasses[0].length() > 0))
                setObjectKey(new AbstractObjectKey(uriNamesClasses[0]));
        }

        // Read objects and add them to the collection
        for (i++; i < uriNamesClasses.length; i += 2) {
            try {
                LocalAbstractObject object = readObject(stream, uriNamesClasses[i]);
                if (restrictNames == null || restrictNames.contains(uriNamesClasses[i - 1])) {
                    object.setObjectKey(this.getObjectKey());
                    objects.put(uriNamesClasses[i - 1], object);
                }
            } catch (IOException e) {
                // Ignore the exception if the object is not loaded anyway
                if (restrictNames == null || restrictNames.contains(uriNamesClasses[i - 1]))
                    Logger.getLogger(getClass().getName()).warning(new StringBuffer("Error creating object ").append(uriNamesClasses[i - 1]).append(" for MetaObject with ID ").append(this.getLocatorURI()).append(":\n ").append(e).toString());
            }
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
        // Create first line with semicolon-separated names of classes
        Iterator<Entry<String, LocalAbstractObject>> iterator = objects.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, LocalAbstractObject> entry = iterator.next();
            if (entry.getValue() != null) {
                // Write object name and class
                stream.write(entry.getKey().getBytes());
                stream.write(';');
                stream.write(entry.getValue().getClass().getName().getBytes());
                // Do not append semicolon after the last object in the header
                if (iterator.hasNext())
                    stream.write(';');           }
        }
        stream.write('\n');
        
        // Write a line for every object from the list (skip the comments)
        for (LocalAbstractObject object : objects.values())
            object.write(stream, false);
    }


    //****************** MetaObject implementation ******************//

    /**
     * Returns a collection of all the encapsulated objects associated with their symbolic names.
     * Note that the collection can contain <tt>null</tt> values.
     * @return a map all the encapsulated objects
     */
    @Override
    public Map<String, LocalAbstractObject> getObjectMap() {
        return objects;
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
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float[] metaDistances, float distThreshold) {
        return Math.abs(getLocatorURI().hashCode() - obj.getLocatorURI().hashCode());
    }


    //************ Protected methods of BinarySerializable interface ************//

    /**
     * Creates a new instance of MetaObject loaded from binary input buffer.
     * 
     * @param input the buffer to read the MetaObject from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected MetaObjectMap(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        this.objects = new TreeMap<String, LocalAbstractObject>();
        int items = serializator.readInt(input);
        for (int i = 0; i < items; i++)
            objects.put(serializator.readString(input), serializator.readObject(input, LocalAbstractObject.class));
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        int size = super.binarySerialize(output, serializator);
        size += serializator.write(output, objects.size());
        for (Entry<String, LocalAbstractObject> entry : objects.entrySet()) {
            size += serializator.write(output, entry.getKey());
            size += serializator.write(output, entry.getValue());
        }
        return size;
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        int size = super.getBinarySize(serializator) + 4;
        for (Entry<String, LocalAbstractObject> entry : objects.entrySet())
            size += serializator.getBinarySize(entry.getKey()) + serializator.getBinarySize(entry.getValue());
        return size;
    }

}
