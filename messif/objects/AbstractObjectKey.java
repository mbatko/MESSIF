/*
 * AbstractObjectKey
 * 
 */

package messif.objects;

import java.io.IOException;
import messif.objects.nio.BinaryInputStream;
import messif.objects.nio.BinaryOutputStream;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

/**
 * This class encapsulates the standard key used by the AbstractObject - the URI locator.
 *  It is also an ancestor of all key classes to be used in the Abstact object.
 *
 * @author David Novak, FI Masaryk University, Brno, Czech Republic; <a href="mailto:xnovak8@fi.muni.cz">xnovak8@fi.muni.cz</a>
 */
public class AbstractObjectKey implements java.io.Serializable, Comparable<AbstractObjectKey>, BinarySerializable {

    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;
    
    /** The URI locator */
    protected final String locatorURI;

    /**
     * Returns the URI from this key as a string.
     * @return the URI from this key as a string
     */
    public String getLocatorURI() {
        return locatorURI;
    }
    
    /** 
     * Creates a new instance of AbstractObjectKey given the locator URI.
     * @param locatorURI the URI locator
     */
    public AbstractObjectKey(String locatorURI) {
        this.locatorURI = locatorURI;
    }

    /**
     * Returns the string representation of this key (the locator).
     * @return the string representation of this key (the locator)
     */
    public String getText() {
        return (locatorURI == null)?"":locatorURI;
    }
    
    /**
     * Compare the keys according to their locators.
     * @param o the key to compare this key with
     * @return a negative integer, zero, or a positive integer if this object
     *         is less than, equal to, or greater than the specified object
     */
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
     * Creates a new instance of AbstractObjectKey loaded from binary input stream.
     * 
     * @param input the stream to read the AbstractObjectKey from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the stream
     */
    protected AbstractObjectKey(BinaryInputStream input, BinarySerializator serializator) throws IOException {
        this.locatorURI = serializator.readString(input);
    }

    /**
     * Binary-serialize this object into the <code>output</code>.
     * @param output the output stream this object is binary-serialized into
     * @param serializator the serializator used to write objects
     * @return the number of bytes actually written
     * @throws IOException if there was an I/O error during serialization
     */
    public int binarySerialize(BinaryOutputStream output, BinarySerializator serializator) throws IOException {
        return serializator.write(output, locatorURI);
    }

    /**
     * Returns the exact size of the binary-serialized version of this object in bytes.
     * @param serializator the serializator used to write objects
     * @return size of the binary-serialized version of this object
     */
    public int getBinarySize(BinarySerializator serializator) {
        return serializator.getBinarySize(locatorURI);
    }

}
