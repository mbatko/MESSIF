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
package messif.objects.keys;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

/**
 * This class encapsulates the standard key used by the AbstractObject - the URI locator.
 * It is also an ancestor of all key classes to be used in the Abstact object.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class AbstractObjectKey implements java.io.Serializable, Comparable<AbstractObjectKey>, BinarySerializable {
    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;

    //************ Attributes ************//

    /** The URI locator */
    private final String locatorURI;


    //************ Constructor ************//

    /** 
     * Creates a new instance of AbstractObjectKey given the locator URI.
     * @param locatorURI the URI locator
     */
    public AbstractObjectKey(String locatorURI) {
        this.locatorURI = locatorURI;
    }


    //************ Factory method ************//

    /**
     * Factory method for creating object key instances of arbitrary class.
     * The key class must contain a public constructor with single {@link String} argument.
     *
     * @param <T> the class of created the object key
     * @param keyClass the class of created the object key
     * @param keyData the data from which to create the key
     * @return a new instance of object key
     */
    public static <T extends AbstractObjectKey> T create(Class<? extends T> keyClass, String keyData) {
        try {
            return keyClass.getConstructor(String.class).newInstance(keyData);
        } catch (IllegalAccessException e) {
            throw new InternalError("This should never happen: " + e);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Cannot create instance of " + keyClass + ": it is an abstract class");
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Cannot create instance of " + keyClass + ": there is no constructor " + e.getMessage());
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("Cannot create instance of " + keyClass + ": " + e.getCause(), e.getCause());
        }
    }


    //************ Attribute access methods ************//

    /**
     * Returns the URI from this key as a string.
     * @return the URI from this key as a string
     */
    public String getLocatorURI() {
        return locatorURI;
    }


    //****************** Serialization ******************//

    /**
     * Writes this object key into the output text stream.
     * The key is written using the following format:
     * <pre>#objectKey keyClass key value</pre>
     *
     * @param stream the stream to write the key to
     * @throws IOException if any problem occures during comment writing
     */
    public final void write(OutputStream stream) throws IOException {
        stream.write("#objectKey ".getBytes());
        stream.write(getClass().getName().getBytes());
        stream.write(' ');
        writeData(stream);
        stream.write('\n');
    }

    /**
     * Store this key's data to a text stream.
     * This method should have the opposite deserialization in constructor.
     * Note that this method should <em>not</em> write a line separator (\n).
     *
     * @param stream the stream to store this object to
     * @throws IOException if there was an error while writing to stream
     */
    protected void writeData(OutputStream stream) throws IOException {
        if (locatorURI != null)
            stream.write(locatorURI.getBytes());
    }


    //************ Comparator and equality methods ************//

    /**
     * Compare the keys according to their locators.
     * @param o the key to compare this key with
     * @return a negative integer, zero, or a positive integer if this object
     *         is less than, equal to, or greater than the specified object
     */
    @Override
    public int compareTo(AbstractObjectKey o) {
        if (o == null)
            return 3;
        if (! (o.getClass().equals(AbstractObjectKey.class)))
            return 3;
        if (locatorURI == null) {
            if (o.locatorURI == null)
                return 0;
            return -2;
        }
        if (o.locatorURI == null)
            return 2;
        return locatorURI.compareTo(o.locatorURI);
    }

    /**
     * Return the hashCode of the locator URI or 0, if it is null.
     * @return the hashCode of the locator URI or 0, if it is null
     */
    @Override
    public int hashCode() {
        if (locatorURI == null)
            return 0;
        return locatorURI.hashCode();
    }

    /**
     * Returns whether this key is equal to the <code>obj</code>.
     * It is only and only if the <code>obj</code> is descendant of
     * {@link AbstractObjectKey} and has an equal locator URI.
     * 
     * @param obj the object to compare this object to
     * @return <tt>true</tt> if this object is the same as the <code>obj</code> argument; <tt>false</tt> otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (! (obj.getClass().equals(AbstractObjectKey.class)))
            return false;
        if (locatorURI == null)
            return ((AbstractObjectKey) obj).locatorURI == null;
        return locatorURI.equals(((AbstractObjectKey) obj).locatorURI);
    }


    //************ String representation ************//

    /**
     * Returns the URI string.
     * @return the URI string
     */
    @Override
    public String toString() {
        return locatorURI;
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of AbstractObjectKey loaded from binary input.
     * 
     * @param input the input to read the AbstractObjectKey from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the input
     */
    protected AbstractObjectKey(BinaryInput input, BinarySerializator serializator) throws IOException {
        this.locatorURI = serializator.readString(input);
    }

    /**
     * Binary-serialize this object into the <code>output</code>.
     * @param output the output that this object is binary-serialized into
     * @param serializator the serializator used to write objects
     * @return the number of bytes actually written
     * @throws IOException if there was an I/O error during serialization
     */
    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return serializator.write(output, locatorURI);
    }

    /**
     * Returns the exact size of the binary-serialized version of this object in bytes.
     * @param serializator the serializator used to write objects
     * @return size of the binary-serialized version of this object
     */
    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return serializator.getBinarySize(locatorURI);
    }

}
