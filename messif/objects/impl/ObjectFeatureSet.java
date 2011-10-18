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
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;
import messif.utility.Convert;

/**
 * Represents a list of LocalAbstractObjects
 * <p>
 * All the encapsulated objects share the same locator URI.
 * </p>
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 * @author Tomas Homola, Masaryk University, Brno, Czech Republic, xhomola@fi.muni.cz 
 */

public abstract class ObjectFeatureSet extends LocalAbstractObject implements BinarySerializable, Iterable<LocalAbstractObject> {
    /** Class id for serialization. */
    private static final long serialVersionUID = 666L;


    //****************** Attributes ******************//

    /** List of encapsulated objects */
    protected final List<LocalAbstractObject> objects;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of ObjectFeatureSet with empty list of objects.
     */
    public ObjectFeatureSet () {
        objects = new ArrayList<LocalAbstractObject>();
    }

    /**
     * Creates a new instance of ObjectFeatureSet for the given locatorURI and encapsulated objects.
     * @param locatorURI the locator URI for the new object
     * @param objects the list of objects to encapsulate in this object
     */
    public ObjectFeatureSet(String locatorURI, Collection<? extends LocalAbstractObject> objects) {
        super(locatorURI);
        this.objects = new ArrayList<LocalAbstractObject>(objects);
    }

    /**
     * Creates a new instance of ObjectFeatureSet from a text stream.
     * @param stream the text stream to read an object from
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public ObjectFeatureSet(BufferedReader stream) throws IOException {
        this.objects = new ArrayList<LocalAbstractObject>();
        readObjects(stream);
    }

    /**
     * Creates a new instance of ObjectFeatureSet as a subset of an existing ObjectFeatureSet.
     * Subset is determined as a sub-window.
     * @param supSet original set of features (super-set)
     * @param minX minimal X-coordinate to be included in the resulting subset
     * @param maxX maximal X-coordinate to be included in the resulting subset
     * @param minY minimal Y-coordinate to be included in the resulting subset
     * @param maxY maximal Y-coordinate to be included in the resulting subset
     */
    public ObjectFeatureSet (ObjectFeatureSet supSet, float minX, float maxX, float minY, float maxY) {
        super (supSet.getLocatorURI());
        this.objects =  new ArrayList<LocalAbstractObject>();
        for (int i = 0; i < supSet.getObjectCount(); i++) {
            ObjectFeature of = (ObjectFeature) supSet.getObject(i);
            if (of.getX() >= minX && of.getX() <= maxX && of.getY() >= minY && of.getY() <= maxY) {
                objects.add(of);
            }
        }
    }

    @Override
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    //****************** Attribute access ******************//

    /**
     * Returns the encapsulated object for given index.
     *
     * @param index the index to the object array of the object to return
     * @return encapsulated object for given index or <tt>null</tt> if the index is out of boubds
     */
    public LocalAbstractObject getObject(int index) {
        // return objects.get(index).getFirst();
        return objects.get(index);
    }

    /**
     * Returns iterator over all features.
     * @return iterator over all features
     */
    public Iterator<LocalAbstractObject> getObjects() {
        return iterator();
    }

    /**
     * Returns iterator over all features.
     * @return iterator over all features
     */
    @Override
    public Iterator<LocalAbstractObject> iterator() {
        return objects.iterator();
    }

    /**
     * Returns the number of encapsulated objects.
     * @return the number of encapsulated objects
     */
    public int getObjectCount() {
        return objects.size();
    }

    /**
     * Returns the class of the encapsulated objects.
     * @return the class of the encapsulated objects
     */
    protected String getSaveObjectsClass () {
        return (getObjectCount() > 0) ? objects.get(0).getClass().toString() : "LocalAbstractObject";
    }


    /**
     * Adds the object to the internal list of objects (to the end of the list)
     * @param obj Object to be added
     */
    public void addObject(LocalAbstractObject obj) {
        objects.add(obj);
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
     * @throws IOException if there was an error resolving the specified class or its constructor or a problem
     *         occurred while reading from the stream
     */
    protected LocalAbstractObject readObject(BufferedReader stream, String className) throws IOException {
        try {
            return readObject(stream, Convert.getClassForName(className, LocalAbstractObject.class));
        } catch (ClassNotFoundException e) {
            throw new IOException("Can't create object from stream while creating FeatureSet " + this.getObjectKey() + ": " + e.toString());
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
     * @throws IOException if there was an error resolving the specified class or its constructor or a problem
     *         occurred while reading from the stream
     */
    protected <E> E readObject(BufferedReader stream, Class<E> objectClass) throws IOException {
        try {
            // Read the object
            return objectClass.getConstructor(BufferedReader.class).newInstance(stream);
        } catch (InstantiationException e) {
            throw new IOException("Can't create object from stream while creating FeatureSet " + this.getObjectKey() + ": " + e.toString());
        } catch (IllegalAccessException e) {
            throw new IOException("Can't create object from stream while creating FeatureSet " + this.getObjectKey() + ": " + e.toString());
        } catch (InvocationTargetException e) {
            throw new IOException("Can't create object from stream while creating FeatureSet " + this.getObjectKey() + ": " + e.getCause().toString());
        } catch (NoSuchMethodException e) {
            throw new IOException("Can't create object from stream while creating FeatureSet " + this.getObjectKey() + ": " + e.toString());
        }
    }


    //****************** Text stream I/O ******************//

    /**
     * Fills this instance of ObjectFeatureSet from a text stream.
     * @param stream the text stream to read the objects from
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    protected void readObjects(BufferedReader stream) throws IOException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line = readObjectComments(stream);

        // The line should have format "URI;name1;class1;name2;class2;..." and URI can be skipped (including the semicolon)
        String[] objTypeAndLength = line.split("[: ]+");
        if (objTypeAndLength.length < 2) {
            throw new EOFException ("No object type or vector length defined while initializing ObjectFeatureSet");
        }

        int vectorCount = Integer.parseInt(objTypeAndLength[1]);

        // Read objects and add them to the collection
        objects.clear();
        for (int i = 0; i < vectorCount; i++) {
            objects.add(readObject(stream, objTypeAndLength[0]));
        }
    }

    /**
     * Store this object to a text stream.
     * This method should have the opposite deserialization in constructor of a given object class.
     *
     * @param stream the stream to store this object to
     * @throws IOException if there was an error while writing to stream
     */
    @Override
    protected void writeData(OutputStream stream) throws IOException {
        // Write a line for every object from the list (skip the comments)
        if (objects != null && !objects.isEmpty()) {
            stream.write((objects.get(0).getClass().toString() + " : " + String.valueOf(objects.size()) + "\n").getBytes());
            for (LocalAbstractObject object : objects) {
                object.write(stream, false);
            }
        }
    }


    //****************** Data equality ******************//

    /**
     * Indicates whether some other object has the same data as this one.
     * All the stored local objects for equality with the other ObjectFeatureSet's list.
     *
     * @param   obj   the reference object with which to compare (if it is not ObjectFeatureSet, this method will return <code>false</code>)
     * @return  <code>true</code> if this object is the same as the obj
     *          argument; <code>false</code> otherwise.
     */
    @Override
    public boolean dataEquals(Object obj) {
        if (!(obj instanceof ObjectFeatureSet))
            return false;

        ObjectFeatureSet objSet = (ObjectFeatureSet) obj;
        int size = objects.size();

        if (size != objSet.getObjectCount()) {
            return false;
        }

        for (int i = 0; i < size; i++) {
            if (! objects.get(i).dataEquals(objSet.getObject(i))) {
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
        for (LocalAbstractObject object : (LocalAbstractObject[]) objects.toArray())
            rtv += object.dataHashCode();
        return rtv;
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
        for (LocalAbstractObject object : objects) {
            if (object != null) {
                size += object.getSize();
            }
        }
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
        for (LocalAbstractObject object : objects) {
            if (object != null)
                object.clearSurplusData();
        }
    }

    /**
     * Returns a string representation of this ObjectFeatureSet.
     * @return a string representation of this ObjectFeatureSet
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(super.toString());
        if (!objects.isEmpty()) {
            for (LocalAbstractObject object : objects) {
                sb.append("\n" + object.toString());
            }
        }
        return sb.toString();
    }


    //************ Protected methods of BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectFeatureSet loaded from binary input.
     *
     * @param input the input to read the ObjectFeatureSet from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectFeatureSet(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        int objcount = serializator.readInt(input);
        this.objects = new ArrayList<LocalAbstractObject>(objcount);
        for (int i = 0; i < objcount; i++) {
            objects.add(serializator.readObject(input, LocalAbstractObject.class));
        }
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        int size = super.binarySerialize(output, serializator) ;
        int objcount = getObjectCount();
        size += serializator.write (output, objcount);
        for (LocalAbstractObject obj : objects) {
            size += serializator.write(output, obj);
        }
        return size;
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        int size = super.getBinarySize(serializator);        
        size += 4; // print integer "objectCount"
        for (LocalAbstractObject obj : objects) {
            size += serializator.getBinarySize(obj);
        }
        return size;
    }
}
