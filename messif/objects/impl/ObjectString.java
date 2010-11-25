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
import java.util.Random;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;


/**
 * This object uses {@link String} as its data content.
 * No implementation of distance function is provided - see {@link ObjectStringEditDist}.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class ObjectString extends LocalAbstractObject implements BinarySerializable {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Object data ******************//

    /** Data string */
    protected String text;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of ObjectString.
     * @param text the string content of the new object
     */
    public ObjectString(String text) {
        this.text = text;
    }

    /**
     * Creates a new instance of ObjectString.
     * @param text the string content of the new object
     * @param locatorURI the locator URI for the new object
     */
    public ObjectString(String text, String locatorURI) {
        super(locatorURI);
        this.text = text;
    }

    /**
     * Creates a new instance of ObjectString with randomly generated string content.
     */
    public ObjectString() {
        this(50, 200);
    }

    /**
     * Creates a new instance of ObjectString with randomly generated string content.
     * The string content is genereated with at least {@code minLength} characters
     * and at most {@code maxLength} characters.
     *
     * @param minLength minimal length of the randomly generated string content
     * @param maxLength maximal length of the randomly generated string content
     */
    public ObjectString(int minLength, int maxLength) {
        this.text = generateRandom(minLength, maxLength);
    }

    /**
     * Generate a random text.
     * The generated text has at least {@code minLength} characters
     * and at most {@code maxLength} characters.
     *
     * @param minLength minimal length of the randomly generated text
     * @param maxLength maximal length of the randomly generated text
     * @return a random text
     */
    public static String generateRandom(int minLength, int maxLength) {
        int len = minLength + (int)(Math.random() * (maxLength - minLength));
        char[] data = new char[len];

        for (int j = 0; j < len; j++) 
            data[j] = getRandomChar();
        return new String(data);
    }


    //****************** Text file store/retrieve methods ******************//

    /**
     * Creates a new instance of ObjectString from text stream.
     * @param stream the stream from which to read lines of text
     * @throws EOFException if the end-of-file of the given stream is reached
     * @throws IOException if there was an I/O error during reading from the stream
     */
    public ObjectString(BufferedReader stream) throws EOFException, IOException {
        this.text = readObjectComments(stream);
    }

    public void writeData(OutputStream stream) throws IOException {
        stream.write(text.getBytes());
        stream.write('\n');
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


    //****************** Attribute access methods ******************//

    /**
     * Returns the string that represents the contents of this object.
     * @return the string that represents the contents of this object
     */
    public String getStringData() {
        return this.text;           // Strings are immutable, so this is safe.
    }

    /**
     * Returns the length of the content string.
     * @return the length of the content string
     */
    public int getStringLength() {
        return text.length();
    }

    public int getSize() {
        return text.length() * Character.SIZE / 8;
    }


    //****************************** Cloning *****************************//

    /**
     * Creates and returns a randomly modified copy of this string.
     * Selects a string position in random and changes it - possible char values are in the passed argument.
     *
     * @param  args  expected size of the args array is 1: ObjectString containing all possible chars
     * @return a randomly modified clone of this instance.
     */
    @Override
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


    //************ String representation ************//

    /**
     * Converts this object to a string representation.
     */
    @Override
    public String toString() {
        return new StringBuffer(super.toString()).append(" [").append(text).append("]").toString();
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectString loaded from binary input buffer.
     *
     * @param input the buffer to read the ObjectString from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectString(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        text = serializator.readString(input);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator) +
               serializator.write(output, text);
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator) + serializator.getBinarySize(text);
    }
}
