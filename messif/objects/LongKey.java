
package messif.objects;

import java.io.IOException;
import messif.objects.nio.BinaryInputStream;
import messif.objects.nio.BinaryOutputStream;
import messif.objects.nio.BinarySerializator;

/**
 * The object key that contains an Long and an locator URI.
 * 
 * @author David Novak, FI Masaryk University, Brno, Czech Republic; <a href="mailto:xnovak8@fi.muni.cz">xnovak8@fi.muni.cz</a>
 */
public class LongKey extends AbstractObjectKey {

    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;
    
    /** The long key */
    public final long key;
    
    /** Creates a new instance of LongKey 
     * @param locatorURI the URI locator
     * @param key the long key of the object - it musn't be null
     */
    public LongKey(String locatorURI, long key) {
        super(locatorURI);
        this.key = key;
    }
    
    /** Creates a new instance of LongKey given only the locatorURI - 
     *    implicitly create the key as the <code>(locatorURI.hashCode() + Integer.MAX_VALUE) modulo maxKey</code>.
     * @param locatorURI the URI locator
     * @param hashURI if true then the key is created as hashCode of the locator; it set to 0, otherwise
     * @param maxValue the maximal value the key can have (incremented by 1)
     * @throws java.lang.IllegalArgumentException if the locatorURI is null
     */
    public LongKey(String locatorURI, boolean hashURI, long maxValue) throws IllegalArgumentException {
        super(locatorURI);
        if (hashURI && locatorURI != null)
            this.key = ((long) locatorURI.hashCode() + (long) Integer.MAX_VALUE) % maxValue;
        else key = 0;
    }
    
    /**
     * Creates a new instance of AbstractObjectKey given a buffered reader with the first line of the
     * following format: "longKey locatorUri"
     * 
     * @param keyString the text stream to read an object from
     * @throws IllegalArgumentException if the string is not of format "longKey locatorUri"
     */
    public LongKey(String keyString) throws IllegalArgumentException {
        super(getLocatorURI(keyString));
        try {
            this.key = Long.valueOf(keyString.substring(0, keyString.indexOf(" ")));
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("string must be of format 'longKey locatorUri': "+keyString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("string must be of format 'longKey locatorUri': "+keyString);
        }
    }
    
    /**
     * Auxiliary method for parsing the string 'longKey locatorURI'
     * @return the locatorURI string
     */
    private static String getLocatorURI(String keyString) throws IllegalArgumentException {
        try {
            return keyString.substring(keyString.indexOf(" ") + 1);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("string must be of format 'longKey locatorUri': "+keyString);
        }
    }
    
    /**
     * Returns the string representation of this key (the key and the locator).
     * @return the string representation of this key 
     */
    @Override
    public String getText() {
        StringBuffer buf = new StringBuffer(Long.toString(key));
        buf.append(' ').append(locatorURI);
        return buf.toString();
    }
        
    /**
     * Compare the keys according to the long key
     * @param o the key to compare this key with
     */
    @Override
    public int compareTo(AbstractObjectKey o) {
        if (o == null)
            return 3;
        if (! (o.getClass().equals(LongKey.class)))
            return 3;
        if (key < ((LongKey) o).key)
            return -1;
        if (key > ((LongKey) o).key)
            return 1;
        return 0;
    }

    /**
     * Return the long key converted to int.
     * @return the long key converted to int
     */
    @Override
    public int hashCode() {
        return (int) key;
    }

    /**
     * Equals according to the long key. If the parameter is not of the LongKey class then 
     * return false.
     * @param obj object to compare this object to
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (! (obj.getClass().equals(LongKey.class)))
            return false;
        return (key == ((LongKey) obj).key);
    }
    
    /** Return the URI string. */
    @Override
    public String toString() {
        return (new StringBuffer()).append(key).append(": ").append(locatorURI).toString();
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of LongKey loaded from binary input stream.
     * 
     * @param input the stream to read the LongKey from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the stream
     */
    protected LongKey(BinaryInputStream input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        key = serializator.readLong(input);
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
