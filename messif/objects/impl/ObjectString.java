/*
 * ObjectString.java
 *
 * Created on 6. kveten 2004, 15:08
 */

package messif.objects.impl;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
import messif.objects.AbstractObjectKey;
import messif.objects.LocalAbstractObject;
import messif.utility.Convert;


/**
 *
 * @author Vlastislav Dohnal, xdohnal@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
public abstract class ObjectString extends LocalAbstractObject {

    /** class id for serialization */
    private static final long serialVersionUID = 1L;
        
    //****************** Object data ******************
    
    protected String text;
    
    
    /** Returns the contents of this object as a new string.
     *  A copy is returned, so any modifications to the returned string do not affect the original object.
     */
    public String getStringData() {
        return this.text;           // Strings are immutable, so this is safe.
    }
    
    
    //****************** Constructors ******************

    /** Creates a new instance of Object */
    public ObjectString(String text) {
        this.text = text;
    }
    
    /** Creates a new instance of Object random generated */
    public ObjectString() {
        this.text = generateRandom();
    }
    
    /** Creates a new instance of Object random generated 
     * with minimal length equal to minLength and maximal 
     * length equal to maxLength */
    public ObjectString(int minLength, int maxLength) {
        this.text = generateRandom(minLength, maxLength);
    }
    
    /** Generate a random text object and return it as a string.
     * The length of the string is limited by minimum minLength and maximum maxLength.
     */
    public static String generateRandom(int minLength, int maxLength) {
        int len = minLength + (int)(Math.random() * (maxLength - minLength));
        char[] data = new char[len];
        
        for (int j = 0; j < len; j++) 
            data[j] = getRandomChar();
        return new String(data);
    }
    
    /** Generate a random text object and return it as a string */
    public static String generateRandom() {
        return generateRandom(50, 200);
    }
    
    
    //****************** Text file store/retrieve methods ******************
    
    /** Creates a new instance of Object from stream.
     * Throws IOException when an error appears during reading from given stream.
     * Throws EOFException when eof of the given stream is reached.
     */
    public ObjectString(BufferedReader stream) throws IOException {
        this(stream, null, null);
    }
    
    /** Creates a new instance of Object from stream.
     * Throws IOException when an error appears during reading from given stream.
     * Throws EOFException when eof of the given stream is reached.
     */
    public ObjectString(BufferedReader stream, String keySeparator, Class<? extends AbstractObjectKey> keyClass) throws IOException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line;
        do {
            line = stream.readLine();
            if (line == null)
                throw new EOFException("EoF reached while initializing ObjectString.");
        } while (processObjectComment(line));
        
        if (keySeparator != null) {
            if (keyClass == null)
                keyClass = AbstractObjectKey.class;
            String[] keyAndData = line.split(keySeparator, 2);
            if (keyAndData.length == 2) {
                this.text = keyAndData[1];
                try {
                    setObjectKey(Convert.stringToType(keyAndData[0], keyClass));
                } catch (InstantiationException e) {
                    throw new IOException(e.getMessage());
                }
            } else this.text = keyAndData[0];
        } else this.text = line;
    }

    /** Write object to stream */
    public void writeData(OutputStream stream) throws IOException {
        stream.write(text.getBytes());
        stream.write('\n');
    }
    
    
    /** toString
     * Converts the object to a string representation
     */
    public String toString() {
        return new StringBuffer(super.toString()).append(" [").append(text).append("]").toString();
    }

    
    //****************** Equality comparing function ******************
    
    public boolean dataEquals(Object obj) {
        if (!(obj instanceof ObjectString))
            return false;
        if (text == null)
            return (((ObjectString)obj).text == null);
        return ((ObjectString)obj).text.equals(text);
    }

    public int dataHashCode() {
        return text.hashCode();
    }

    
    //****************** Size function ******************

    /** Returns the length of the current string.
     */
    public int getStringLength() {
        return this.text.length();
    }
    
    /** Returns the size of object in bytes
     */
    public int getSize() {
        return text.length() * Character.SIZE / 8;
    }

    /****************************** Cloning *****************************/

    /**
     * Creates and returns a randomly modified copy of this string.
     * Selects a string position in random and changes it - possible char values are in the passed argument.
     *
     * @param  args  expected size of the args array is 1: ObjectString containing all possible chars
     * @return a randomly modified clone of this instance.
     */
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        ObjectString rtv = (ObjectString) this.clone();
        
        try {
            ObjectString availChars = (ObjectString) args[0];
            Random random = new Random(System.currentTimeMillis());
            
            // pick a character in random from the available characters
            char randomChar = availChars.text.charAt(random.nextInt(availChars.text.length()));
            
            // substitute it for any char
            int position = random.nextInt(text.length());
            StringBuffer buffer = new StringBuffer(text.length());
            buffer.append(text.substring(0,position)).append(randomChar).append(text.substring(position + 1));
            rtv.text = buffer.toString();
        } catch (ArrayIndexOutOfBoundsException ignore) {
        } catch (ClassCastException ignore) { }
        
        return rtv;
    }
    
    
}
