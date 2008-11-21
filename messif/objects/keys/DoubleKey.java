
package messif.objects.keys;

import java.io.IOException;
import messif.objects.nio.BinaryInputStream;
import messif.objects.nio.BinaryOutputStream;
import messif.objects.nio.BinarySerializator;

/**
 * The object key that contains an Double and an locator URI.
 * 
 * @author David Novak, FI Masaryk University, Brno, Czech Republic; <a href="mailto:xnovak8@fi.muni.cz">xnovak8@fi.muni.cz</a>
 */
public class DoubleKey extends AbstractObjectKey {

    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;
    
    /** The double key */
    public final double key;
    
    /** Creates a new instance of DoubleKey 
     * @param locatorURI the URI locator
     * @param key the double key of the object - it musn't be null
     */
    public DoubleKey(String locatorURI, double key) {
        super(locatorURI);
        this.key = key;
    }
    
    /**
     * Creates a new instance of AbstractObjectKey given a buffered reader with the first line of the
     * following format: "doubleKey locatorUri"
     * 
     * @param keyString the text stream to read an object from
     * @throws IllegalArgumentException if the string is not of format "doubleKey locatorUri"
     */
    public DoubleKey(String keyString) throws IllegalArgumentException {
        super(getLocatorURI(keyString));
        try {
            this.key = Double.valueOf(keyString.substring(0, keyString.indexOf(" ")));
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("string must be of format 'doubleKey locatorUri': "+keyString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("string must be of format 'doubleKey locatorUri': "+keyString);
        }
    }
    
    /**
     * Auxiliary method for parsing the string 'doubleKey locatorURI'
     * @return the locatorURI string
     */
    private static String getLocatorURI(String keyString) throws IllegalArgumentException {
        try {
            return keyString.substring(keyString.indexOf(" ") + 1);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("string must be of format 'doubleKey locatorUri': "+keyString);
        }
    }
    
    /**
     * Returns the string representation of this key (the key and the locator).
     * @return the string representation of this key 
     */
    @Override
    public String getText() {
        StringBuffer buf = new StringBuffer(Double.toString(key));
        buf.append(' ').append(locatorURI);
        return buf.toString();
    }
        
    /**
     * Compare the keys according to the double key
     * @param o the key to compare this key with
     */
    @Override
    public int compareTo(AbstractObjectKey o) {
        if (o == null)
            return 3;
        if (! (o instanceof DoubleKey))
            return 3;
        if (key < ((DoubleKey) o).key)
            return -1;
        if (key > ((DoubleKey) o).key)
            return 1;
        return 0;
    }

    /**
     * Return the double key converted to int.
     * @return the double key converted to int
     */
    @Override
    public int hashCode() {
        return ((Double) key).hashCode();
    }

    /**
     * Equals according to the double key. If the parameter is not of the DoubleKey class or subclass then 
     * return false.
     * @param obj object to compare this object to
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (! (obj instanceof DoubleKey))
            return false;
        return (key == ((DoubleKey) obj).key);
    }
    
    /** Return the URI string. */
    @Override
    public String toString() {
        return (new StringBuffer()).append(key).append(": ").append(locatorURI).toString();
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of DoubleKey loaded from binary input stream.
     * 
     * @param input the stream to read the DoubleKey from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the stream
     */
    protected DoubleKey(BinaryInputStream input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        key = serializator.readDouble(input);
    }

    /**
     * Binary-serialize this object into the <code>output</code>.
     * @param output the output stream this object is binary-serialized into
     * @param serializator the serializator used to write objects
     * @return the number of bytes actually written
     * @throws IOException if there was an I/O error during serialization
     */
    @Override
    public int binarySerialize(BinaryOutputStream output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator) + serializator.write(output, key);
    }

    /**
     * Returns the exact size of the binary-serialized version of this object in bytes.
     * @param serializator the serializator used to write objects
     * @return size of the binary-serialized version of this object
     */
    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator) + 8;
    }
    
}
