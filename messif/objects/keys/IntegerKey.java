
package messif.objects.keys;

import java.io.IOException;
import java.io.OutputStream;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;

/**
 * The object key that contains an integer value and a locator URI.
 * 
 * @author David Novak, FI Masaryk University, Brno, Czech Republic; <a href="mailto:xnovak8@fi.muni.cz">xnovak8@fi.muni.cz</a>
 */
public class IntegerKey extends AbstractObjectKey {

    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;
    
    /** The integer key */
    public final int key;
    
    /** Creates a new instance of IntegerKey 
     * @param locatorURI the URI locator
     * @param key the integer key of the object - it musn't be null
     */
    public IntegerKey(String locatorURI, int key) {
        super(locatorURI);
        this.key = key;
    }
    
    /**
     * Creates a new instance of AbstractObjectKey given a buffered reader with the first line of the
     * following format: "integerKey locatorUri"
     * 
     * @param keyString the text stream to read an object from
     * @throws IllegalArgumentException if the string is not of format "integerKey locatorUri"
     */
    public IntegerKey(String keyString) throws IllegalArgumentException {
        super(getLocatorURI(keyString));
        try {
            this.key = Integer.valueOf(keyString.substring(0, keyString.indexOf(" ")));
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("string must be of format 'integerKey locatorUri': "+keyString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("string must be of format 'integerKey locatorUri': "+keyString);
        }
    }
    
    /**
     * Auxiliary method for parsing the string 'integerKey locatorURI'.
     * @param keyString the string with the key and the locator URI
     * @return the locatorURI string
     * @throws IllegalArgumentException if the string is not in the 'integerKey locatorURI' format
     */
    private static String getLocatorURI(String keyString) throws IllegalArgumentException {
        try {
            return keyString.substring(keyString.indexOf(" ") + 1);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("string must be of format 'integerKey locatorUri': "+keyString);
        }
    }
    
    @Override
    protected void writeData(OutputStream stream) throws IOException {
        stream.write(Integer.toString(key).getBytes());
        stream.write(' ');
        super.writeData(stream);
    }

    /**
     * Compare the keys according to the integer key
     * @param o the key to compare this key with
     */
    @Override
    public int compareTo(AbstractObjectKey o) {
        if (o == null)
            return 3;
        if (! (o.getClass().equals(IntegerKey.class)))
            return 3;
        if (key < ((IntegerKey) o).key)
            return -1;
        if (key > ((IntegerKey) o).key)
            return 1;
        return 0;
    }

    /**
     * Return the integer key itself.
     * @return the integer key itself
     */
    @Override
    public int hashCode() {
        return key;
    }

    /**
     * Equals according to the integer key. If the parameter is not of the IntegerKey class then 
     * return false.
     * @param obj object to compare this object to
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (! (obj.getClass().equals(IntegerKey.class)))
            return false;
        return (key == ((IntegerKey) obj).key);
    }
    
    @Override
    public String toString() {
        return (new StringBuffer()).append(key).append(": ").append(getLocatorURI()).toString();
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of IntegerKey loaded from binary input.
     * 
     * @param input the input to read the IntegerKey from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the input
     */
    protected IntegerKey(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        key = serializator.readInt(input);
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
        return super.binarySerialize(output, serializator) + serializator.write(output, key);
    }

    /**
     * Returns the exact size of the binary-serialized version of this object in bytes.
     * @param serializator the serializator used to write objects
     * @return size of the binary-serialized version of this object
     */
    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator) + 4;
    }

}
