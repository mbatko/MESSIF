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
import java.util.HashMap;
import java.util.Map;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.keys.AbstractObjectKey;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;

/**
 * Abstract implementation of the {@link MetaObject} that stores a fixed list of
 * encapsulated objects.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class MetaObjectFixed extends MetaObject {
    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** Constructors ******************//

    /**
     * Creates a new instance of MetaObjectFixed.
     * A new unique object ID is generated and the
     * object's key is set to <tt>null</tt>.
     */
    protected MetaObjectFixed() {
    }

    /**
     * Creates a new instance of MetaObjectFixed.
     * A new unique object ID is generated and the 
     * object's key is set to the specified key.
     * @param objectKey the key to be associated with this object
     */
    protected MetaObjectFixed(AbstractObjectKey objectKey) {
        super(objectKey);
    }

    /**
     * Creates a new instance of MetaObjectFixed.
     * A new unique object ID is generated and a
     * new {@link AbstractObjectKey} is generated for
     * the specified <code>locatorURI</code>.
     * @param locatorURI the locator URI for the new object
     */
    protected MetaObjectFixed(String locatorURI) {
        super(locatorURI);
    }

    /**
     * Creates a new instance of MetaObjectFixed loaded from binary input.
     * 
     * @param input the input to read the MetaObject from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     * @see #readObjectsBinary
     */
    protected MetaObjectFixed(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }


    //****************** Data interface ******************//

    /**
     * Returns the fixed number of the potential encapsulated objects.
     * @return number of encapsulated object names
     */
    protected abstract int getObjectNamesCount();

    /**
     * Returns the name of the fixed object with the given {@code index}.
     * @param index the fixed index of the object the name of which to get
     * @return the name of the {@code index}th object
     */
    protected abstract String getObjectName(int index);

    /**
     * Returns the fixed object with the given {@code index}.
     * @param index the fixed index of the object to get
     * @return the {@code index}th object
     */
    protected abstract LocalAbstractObject getObject(int index);


    //****************** MetaObject method implementations ******************//

    @Override
    public int getObjectCount() {
        int count = 0;
        for (int i = 0; i < getObjectNamesCount(); i++)
            if (getObject(i) != null)
                count++;
        return count;
    }

    @Override
    public LocalAbstractObject getObject(String name) {
        for (int i = 0; i < getObjectNamesCount(); i++)
            if (getObjectName(i).equals(name))
                return getObject(i);
        return null;
    }

    @Override
    public Collection<String> getObjectNames() {
        int count = getObjectNamesCount();
        Collection<String> names = new ArrayList<String>(count);
        for (int i = 0; i < count; i++)
            if (getObject(i) != null)
                names.add(getObjectName(i));
        return names;
    }

    @Override
    public Collection<LocalAbstractObject> getObjects() {
        int count = getObjectNamesCount();
        Collection<LocalAbstractObject> objects = new ArrayList<LocalAbstractObject>(count);
        for (int i = 0; i < count; i++) {
            LocalAbstractObject object = getObject(i);
            if (object != null)
                objects.add(object);
        }
        return objects;
    }

    @Override
    public Map<String, LocalAbstractObject> getObjectMap() {
        int count = getObjectNamesCount();
        Map<String, LocalAbstractObject> ret = new HashMap<String, LocalAbstractObject>(count);
        for (int i = 0; i < count; i++) {
            LocalAbstractObject object = getObject(i);
            if (object != null)
                ret.put(getObjectName(i), object);
        }
        return ret;
    }


    //****************** Text stream I/O helper method ******************//

    /**
     * Utility method for reading objects from a text stream.
     * This method is intended to be used in subclasses that have a fixed
     * list of objects to implement the {@link BufferedReader} constructor.
     *
     * @param stream the text stream to read the objects from
     * @param classes the classes of the objects to read from the stream
     * @return the array of objects read from the stream
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    protected static LocalAbstractObject[] readObjects(BufferedReader stream, Class<? extends LocalAbstractObject>[] classes) throws IOException {
        if (classes == null || classes.length == 0)
            throw new IllegalArgumentException("At least one object class must be specified for reading");
        LocalAbstractObject[] ret = new LocalAbstractObject[classes.length];
        for (int i = 0; i < classes.length; i++) {
            if (peekNextChar(stream) == '\n') {
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
        for (int i = 0; i < getObjectNamesCount(); i++) {
            LocalAbstractObject object = getObject(i);
            if (object == null)
                stream.write('\n');
            else
                object.write(stream, false);            
        }
    }


    //************ BinarySerializable support ************//

    /**
     * Reads encapsulated objects from the binary input buffer.
     *
     * @param input the buffer to read the encapsulated objects from
     * @param serializator the serializator used to write objects
     * @param classes the classes of the objects to read from the binary input
     * @return the array of objects read from the binary input
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected final LocalAbstractObject[] readObjectsBinary(BinaryInput input, BinarySerializator serializator, Class<? extends LocalAbstractObject>[] classes) throws IOException {
        LocalAbstractObject[] objects = new LocalAbstractObject[classes.length];
        for (int i = 0; i < classes.length; i++)
            objects[i] = serializator.readObject(input, classes[i]);
        return objects;
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        int size = super.binarySerialize(output, serializator);
        for (int i = 0; i < getObjectNamesCount(); i++)
            size += serializator.write(output, getObject(i));
        return size;
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        int size = super.getBinarySize(serializator);
        for (int i = 0; i < getObjectNamesCount(); i++)
            size += serializator.getBinarySize(getObject(i));
        return size;
    }
}
