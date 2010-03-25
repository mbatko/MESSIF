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
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;

/**
 * The object key that contains a long value and a locator URI.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
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
     * Auxiliary method for parsing the string 'longKey locatorURI'.
     * @param keyString the string with the key and the locator URI
     * @return the locatorURI string
     * @throws IllegalArgumentException if the string is not in the 'longKey locatorURI' format
     */
    private static String getLocatorURI(String keyString) throws IllegalArgumentException {
        try {
            return keyString.substring(keyString.indexOf(" ") + 1);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("string must be of format 'longKey locatorUri': "+keyString);
        }
    }
    
    @Override
    protected void writeData(OutputStream stream) throws IOException {
        stream.write(Long.toString(key).getBytes());
        stream.write(' ');
        super.writeData(stream);
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
        return (new StringBuffer()).append(key).append(": ").append(getLocatorURI()).toString();
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of LongKey loaded from binary input.
     * 
     * @param input the input to read the LongKey from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the input
     */
    protected LongKey(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        key = serializator.readLong(input);
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
        return super.getBinarySize(serializator) + 8;
    }
    
}
