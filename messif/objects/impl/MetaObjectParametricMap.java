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
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.MetaObjectParametric;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

/**
 * Extension of the standard {@link MetaObjectParametric} that stores encapsulated
 * {@link LocalAbstractObject}s in a map under their symbolic names (strings).
 *
 * The metric distance function for this object is defined as a binary function returning
 * <code>0</code> for identical objects (as of {@link #equals(java.lang.Object) equals} on object's locator) and 
 * <code>1</code> for different objects.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class MetaObjectParametricMap extends MetaObjectParametric implements BinarySerializable {

    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** List of encapsulated objects */
    protected Map<String, LocalAbstractObject> objects;


    //****************** Constructors ******************//
    
    /**
     * Creates a new instance of MetaObjectParametricMap from a collection of named objects.
     * A new unique object ID is generated and the
     * object's key (locatorURI) is set to <tt>null</tt>.
     *
     * @param additionalParameters additional parameters for this meta object
     * @param objects collection of objects with their symbolic names
     */
    public MetaObjectParametricMap(Map<String, ? extends Serializable> additionalParameters, Map<String, LocalAbstractObject> objects) {
        super(additionalParameters);
        this.objects = new TreeMap<String, LocalAbstractObject>(objects);
    }

    /**
     * Creates a new instance of MetaObjectParametricMap from a collection of named objects.
     * The locatorURI of every object from the collection is set to the provided
     * one only if {@code cloneObjects} is requested.
     *
     * @param locatorURI the locator URI for this object and all the provided objects will be set as well
     * @param additionalParameters additional parameters for this meta object
     * @param objects collection of objects with their symbolic names
     * @param cloneObjects if <tt>true</tt> the provided <code>objects</code> will be cloned and the
     *        the locators of the provided <code>objects</code> will be replaced by the specified one
     * @throws CloneNotSupportedException if the cloning of the <code>objects</code> was unsuccessful
     */
    public MetaObjectParametricMap(String locatorURI, Map<String, ? extends Serializable> additionalParameters, Map<String, LocalAbstractObject> objects, boolean cloneObjects) throws CloneNotSupportedException {
        super(locatorURI, additionalParameters);
        if (cloneObjects) {
            this.objects = new TreeMap<String, LocalAbstractObject>();
            for (Entry<String, LocalAbstractObject> entry : objects.entrySet())
                this.objects.put(entry.getKey(), (LocalAbstractObject)entry.getValue().clone(getObjectKey()));
        } else {
            this.objects = new TreeMap<String, LocalAbstractObject>(objects);
        }
    }

    /**
     * Creates a new instance of MetaObjectParametricMap from a collection of named objects.
     *
     * @param locatorURI the locator URI for this object and all the provided objects will be set as well
     * @param additionalParameters additional parameters for this meta object
     * @param objects collection of objects with their symbolic names
     */
    public MetaObjectParametricMap(String locatorURI, Map<String, ? extends Serializable> additionalParameters, Map<String, LocalAbstractObject> objects) {
        super(locatorURI, additionalParameters);
        this.objects = new TreeMap<String, LocalAbstractObject>(objects);
    }

    /**
     * Creates a new instance of MetaObjectParametricMap from a text stream.
     * Only objects for names specified in <code>restrictNames</code> are added.
     * Parameters are set to the given map.
     * @param stream the text stream to read an object from
     * @param restrictNames if not <tt>null</tt> only the names specified in this collection are added to the objects table
     * @param additionalParameters additional parameters for this meta object
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public MetaObjectParametricMap(BufferedReader stream, Set<String> restrictNames, Map<String, ? extends Serializable> additionalParameters) throws IOException {
        super(additionalParameters);
        this.objects = readObjects(stream, restrictNames, readObjectsHeader(stream), new TreeMap<String, LocalAbstractObject>());
    }

    /**
     * Creates a new instance of MetaObjectParametricMap from a text stream.
     * Only objects for names specified in <code>restrictNames</code> are added.
     * Note that no additional parameters are set.
     * @param stream the text stream to read an object from
     * @param restrictNames if not <tt>null</tt> only the names specified in this collection are added to the objects table
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public MetaObjectParametricMap(BufferedReader stream, Set<String> restrictNames) throws IOException {
        this(stream, restrictNames, new HashMap<String, Serializable>());
    }    

    /**
     * Creates a new instance of MetaObjectParametricMap from a text stream.
     * Only objects for names specified in <code>restrictNames</code> are added.
     * Note that no additional parameters are set.
     * @param stream the text stream to read an object from
     * @param restrictNames if not <tt>null</tt> only the names specified in this collection are added to the objects table
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public MetaObjectParametricMap(BufferedReader stream, String[] restrictNames) throws IOException {
        this(stream, (restrictNames == null)?null:new HashSet<String>(Arrays.asList(restrictNames)));
    }

    /**
     * Creates a new instance of MetaObjectParametricMap from a text stream.
     * Note that no additional parameters are set.
     * @param stream the text stream to read an object from
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public MetaObjectParametricMap(BufferedReader stream) throws IOException {
        this(stream, (Set<String>)null);
    }


    //****************** Cloning ******************//

    /**
     * Creates and returns a copy of this object. The precise meaning 
     * of "copy" may depend on the class of the object.
     * @param cloneFilterChain  the flag whether the filter chain must be cloned as well.
     * @return a clone of this instance.
     * @throws CloneNotSupportedException if the object's class does not support cloning or there was an error
     */
    @Override
    public LocalAbstractObject clone(boolean cloneFilterChain) throws CloneNotSupportedException {
        MetaObjectParametricMap rtv = (MetaObjectParametricMap)super.clone(cloneFilterChain);
        
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
     *        the minimal and the maximal possible values
     * @return a randomly modified clone of this instance
     * @throws CloneNotSupportedException if the object's class does not support cloning or there was an error
     */
    @Override
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        MetaObjectParametricMap objectClone = (MetaObjectParametricMap)clone(true);
        // Replace all sub-objects with random-modified clones
        for (String name : getObjectNames())
            objectClone.objects.put(name, getObject(name).cloneRandomlyModify(args));
        return objectClone;
    }


    //****************** Text stream I/O ******************//

    @Override
    protected void writeDataImpl(OutputStream stream) throws IOException {
        writeObjects(stream, writeObjectsHeader(stream, objects));
    }


    //****************** MetaObject implementation ******************//

    /**
     * Returns a collection of all the encapsulated objects associated with their symbolic names.
     * Note that the collection can contain <tt>null</tt> values.
     * @return a map with symbolic names as keyas and the respective encapsulated objects as values
     */
    @Override
    public Map<String, LocalAbstractObject> getObjectMap() {
        return Collections.unmodifiableMap(objects);
    }

    @Override
    public LocalAbstractObject getObject(String name) {
        return objects.get(name);
    }

    @Override
    public Collection<String> getObjectNames() {
        return Collections.unmodifiableCollection(objects.keySet());
    }

    @Override
    public Collection<LocalAbstractObject> getObjects() {
        return Collections.unmodifiableCollection(objects.values());
    }

    @Override
    public int getObjectCount() {
        return objects.size();
    }

    /**
     * The actual implementation of the metric function.
     * The distance is a trivial metric on locator URIs of this object and {@code obj} and
     * returns <code>0</code> when locators are equal and <code>1</code> otherwise.
     * The array <code>metaDistances</code> is ignored.
     *
     * @param obj the object to compute distance to
     * @param metaDistances the array that is filled with the distances of the respective encapsulated objects, if it is not <tt>null</tt>
     * @param distThreshold the threshold value on the distance
     * @return the actual distance between obj and this if the distance is lower than distThreshold
     * @see LocalAbstractObject#getDistance
     */
    @Override
    protected float getDistanceImpl(MetaObject obj, float[] metaDistances, float distThreshold) {
        String thisLocator = getLocatorURI();
        String otherLocator = obj.getLocatorURI();
        return (thisLocator == null ? otherLocator == null : thisLocator.equals(otherLocator)) ? 0 : 1;
    }


    //************ Protected methods of BinarySerializable interface ************//

    /**
     * Creates a new instance of MetaObject loaded from binary input buffer.
     * 
     * @param input the buffer to read the MetaObject from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected MetaObjectParametricMap(BinaryInput input, BinarySerializator serializator) throws IOException {
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
