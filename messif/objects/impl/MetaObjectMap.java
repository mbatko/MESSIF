/*
 * MetaObjectMap
 *
 */

package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.nio.BinaryInputStream;
import messif.objects.nio.BinaryOutputStream;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

/**
 *
 * @author xbatko
 */
public class MetaObjectMap extends MetaObject implements BinarySerializable {

    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;

    /****************** Attributes ******************/

    /** List of encapsulated objects */
    protected Map<String, LocalAbstractObject> objects = new TreeMap<String, LocalAbstractObject>();


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
        if (cloneObjects)
            addObjectClones(objects);
        else
            addObjects(objects);
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
        addObjects(objects);
    }

    /**
     * Creates a new instance of MetaObjectMap from a text stream.
     * @param stream the text stream to read an object from
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public MetaObjectMap(BufferedReader stream) throws IOException {
        readObjects(stream);
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


    //****************** MetaObject implementation ******************//

    /**
     * Returns the number of encapsulated objects.
     * @return the number of encapsulated objects
     */
    @Override
    public int getObjectCount() {
        return objects.size();
    }

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
    @Override
    protected LocalAbstractObject addObject(String name, LocalAbstractObject object) throws IllegalArgumentException {
        return objects.put(name, object);
    }

    /**
     * Removes an object from the encapsulated collection.
     * If the collection does not contains that name, <tt>null</tt> is
     * returned and collection is left untouched.
     * 
     * @param name the symbolic name of the encapsulated object to remove
     * @return the removed encapsulated object or <tt>null</tt> if the synbolic name was not found
     */
    @Override
    protected LocalAbstractObject removeObject(String name) {
        return objects.remove(name);
    }

    /**
     * Removes all objects from the encapsulated collection.
     */
    @Override
    protected void removeObjects() {
        objects.clear();
    }

    /**
     * Returns the encapsulated object for given symbolic name.
     *
     * @param name the symbolic name of the object to return
     * @return encapsulated object for given name or <tt>null</tt> if the key is unknown
     */
    @Override
    public LocalAbstractObject getObject(String name) {
        return objects.get(name);
    }

    /**
     * Returns the set of symbolic names of the encapsulated objects.
     * @return the set of symbolic names of the encapsulated objects
     */
    @Override
    public Collection<String> getObjectNames() {
        return objects.keySet();
    }

    /**
     * Returns a collection of all the encapsulated objects.
     * Note that the collection can contain <tt>null</tt> values.
     * @return a collection all the encapsulated objects
     */
    public Collection<LocalAbstractObject> getObjects() {
        return objects.values();
    }


    //************ Protected methods of BinarySerializable interface ************//

    /**
     * Creates a new instance of MetaObject loaded from binary input stream.
     * 
     * @param input the stream to read the MetaObject from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the stream
     */
    protected MetaObjectMap(BinaryInputStream input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        int items = serializator.readInt(input);
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
    public int binarySerialize(BinaryOutputStream output, BinarySerializator serializator) throws IOException {
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
    public int getBinarySize(BinarySerializator serializator) {
        int size = super.getBinarySize(serializator) + 4;
        for (Entry<String, LocalAbstractObject> entry : objects.entrySet())
            size += serializator.getBinarySize(entry.getKey()) + serializator.getBinarySize(entry.getValue());
        return size;
    }

}
