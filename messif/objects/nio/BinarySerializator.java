/*
 * BinarySerializator
 *
 */
package messif.objects.nio;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import messif.utility.Logger;

/**
 * This class provides a framework for {@link BinarySerializable binary serialization} of objects.
 * It operates on any {@link BinaryInput}/{@link BinaryOutput}.
 * 
 * @author xbatko
 * @see ByteBufferFileInputStream
 * @see ByteBufferFileOutputStream
 */
public abstract class BinarySerializator {

    /** Logger for serializators */
    protected static final Logger log = Logger.getLoggerEx("messif.objects.nio.serializator");

    //************************ Serializing methods for primitive types ************************//

    /**
     * Writes a <code>boolean</code> value to the specified output.
     * @param output the output buffer to write the value into
     * @param value the <code>boolean</code> value to be written
     * @return the number of bytes written
     * @throws IOException if there was an I/O error
     */
    public int write(BinaryOutput output, boolean value) throws IOException {
        write(output, (byte)(value?1:0));
        return 1;
    }

    /**
     * Writes a <code>byte</code> value to the specified output.
     * @param output the output buffer to write the value into
     * @param value the <code>byte</code> value to be written
     * @return the number of bytes written
     * @throws IOException if there was an I/O error
     */
    public int write(BinaryOutput output, byte value) throws IOException {
        output.prepareOutput(1).put(value);
        return 1;
    }

    /**
     * Writes a <code>short</code> value to the specified output.
     * @param output the output buffer to write the value into
     * @param value the <code>short</code> value to be written
     * @return the number of bytes written
     * @throws IOException if there was an I/O error
     */
    public int write(BinaryOutput output, short value) throws IOException {
        output.prepareOutput(2).putShort(value);
        return 2;
    }

    /**
     * Writes a <code>char</code> value to the specified output.
     * @param output the output buffer to write the value into
     * @param value the <code>char</code> value to be written
     * @return the number of bytes written
     * @throws IOException if there was an I/O error
     */
    public int write(BinaryOutput output, char value) throws IOException {
        output.prepareOutput(2).putChar(value);
        return 2;
    }

    /**
     * Writes a <code>int</code> value to the specified output.
     * @param output the output buffer to write the value into
     * @param value the <code>int</code> value to be written
     * @return the number of bytes written
     * @throws IOException if there was an I/O error
     */
    public int write(BinaryOutput output, int value) throws IOException {
        output.prepareOutput(4).putInt(value);
        return 4;
    }

    /**
     * Writes a <code>long</code> value to the specified output.
     * @param output the output buffer to write the value into
     * @param value the <code>long</code> value to be written
     * @return the number of bytes written
     * @throws IOException if there was an I/O error
     */
    public int write(BinaryOutput output, long value) throws IOException {
        output.prepareOutput(8).putLong(value);
        return 8;
    }

    /**
     * Writes a <code>float</code> value to the specified output.
     * @param output the output buffer to write the value into
     * @param value the <code>float</code> value to be written
     * @return the number of bytes written
     * @throws IOException if there was an I/O error
     */
    public int write(BinaryOutput output, float value) throws IOException {
        output.prepareOutput(4).putFloat(value);
        return 4;
    }

    /**
     * Writes a <code>double</code> value to the specified output.
     * @param output the output buffer to write the value into
     * @param value the <code>double</code> value to be written
     * @return the number of bytes written
     * @throws IOException if there was an I/O error
     */
    public int write(BinaryOutput output, double value) throws IOException {
        output.prepareOutput(8).putDouble(value);
        return 8;
    }


    //************************ Serializing methods for primitive arrays ************************//

    /**
     * Writes a <code>boolean</code> array to the specified output.
     * @param output the output buffer to write the array into
     * @param array the <i>boolean</i> array to write
     * @param index the start index in the <i>boolean</i> array
     * @param length the number of array items to write
     * @return the number of bytes written to the output
     * @throws IOException if there was an I/O error
     * @throws NullPointerException if the specified array is <tt>null</tt>
     * @throws IndexOutOfBoundsException if the <code>index</code> or <code>length</code> are invalid for the specified array
     */
    public int write(BinaryOutput output, boolean[] array, int index, int length) throws IOException, NullPointerException, IndexOutOfBoundsException {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++)
            bytes[i] = array[index + i]?(byte)1:(byte)0;
        return write(output, bytes);
    }

    /**
     * Writes a <code>boolean</code> array to the specified output.
     * @param output the output buffer to write the array into
     * @param array the <i>boolean</i> array to write
     * @return the number of bytes written to the output
     * @throws IOException if there was an I/O error
     */
    public int write(BinaryOutput output, boolean[] array) throws IOException {
        if (array != null)
            return write(output, array, 0, array.length);
        write(output, -1);
        return 4;
    }

    /**
     * Writes a <code>byte</code> array to the specified output.
     * @param output the output buffer to write the array into
     * @param array the <i>byte</i> array to write
     * @param index the start index in the <i>byte</i> array
     * @param length the number of array items to write
     * @return the number of bytes written to the output
     * @throws IOException if there was an I/O error
     * @throws NullPointerException if the specified array is <tt>null</tt>
     * @throws IndexOutOfBoundsException if the <code>index</code> or <code>length</code> are invalid for the specified array
     */
    public int write(BinaryOutput output, byte[] array, int index, int length) throws IOException, NullPointerException, IndexOutOfBoundsException {
        write(output, length);
        while (length > 0) {
            ByteBuffer buffer = output.prepareOutput(1);
            int lenToWrite = Math.min(length, buffer.remaining());
            buffer.put(array, index, lenToWrite);
            index += lenToWrite;
            length -= lenToWrite;
        }
        return 4 + length;
    }

    /**
     * Writes a <code>byte</code> array to the specified output.
     * @param output the output buffer to write the array into
     * @param array the <i>byte</i> array to write
     * @return the number of bytes written to the output
     * @throws IOException if there was an I/O error
     */
    public int write(BinaryOutput output, byte[] array) throws IOException {
        if (array != null)
            return write(output, array, 0, array.length);
        write(output, -1);
        return 4;
    }

    /**
     * Writes a <code>short</code> array to the specified output.
     * @param output the output buffer to write the array into
     * @param array the <i>short</i> array to write
     * @param index the start index in the <i>short</i> array
     * @param length the number of array items to write
     * @return the number of bytes written to the output
     * @throws IOException if there was an I/O error
     * @throws NullPointerException if the specified array is <tt>null</tt>
     * @throws IndexOutOfBoundsException if the <code>index</code> or <code>length</code> are invalid for the specified array
     */
    public int write(BinaryOutput output, short[] array, int index, int length) throws IOException, NullPointerException, IndexOutOfBoundsException {
        int bytes = 4 + (length << 1);
        write(output, length);

        while (length > 0) {
            ByteBuffer byteBuffer = output.prepareOutput(2);
            ShortBuffer buffer = byteBuffer.asShortBuffer();
            int lenToWrite = Math.min(length, buffer.remaining());
            buffer.put(array, index, lenToWrite);
            index += lenToWrite;
            length -= lenToWrite;
            byteBuffer.position(byteBuffer.position() + (lenToWrite << 1));
        }

        return bytes;
    }

    /**
     * Writes a <code>short</code> array to the specified output.
     * @param output the output buffer to write the array into
     * @param array the <i>short</i> array to write
     * @return the number of bytes written to the output
     * @throws IOException if there was an I/O error
     */
    public int write(BinaryOutput output, short[] array) throws IOException {
        if (array != null)
            return write(output, array, 0, array.length);
        write(output, -1);
        return 4;
    }

    /**
     * Writes a <code>char</code> array to the specified output.
     * @param output the output buffer to write the array into
     * @param array the <i>char</i> array to write
     * @param index the start index in the <i>char</i> array
     * @param length the number of array items to write
     * @return the number of bytes written to the output
     * @throws IOException if there was an I/O error
     * @throws NullPointerException if the specified array is <tt>null</tt>
     * @throws IndexOutOfBoundsException if the <code>index</code> or <code>length</code> are invalid for the specified array
     */
    public int write(BinaryOutput output, char[] array, int index, int length) throws IOException, NullPointerException, IndexOutOfBoundsException {
        int bytes = 4 + (length << 1);
        write(output, length);

        while (length > 0) {
            ByteBuffer byteBuffer = output.prepareOutput(2);
            CharBuffer buffer = byteBuffer.asCharBuffer();
            int lenToWrite = Math.min(length, buffer.remaining());
            buffer.put(array, index, lenToWrite);
            index += lenToWrite;
            length -= lenToWrite;
            byteBuffer.position(byteBuffer.position() + (lenToWrite << 1));
        }

        return bytes;
    }

    /**
     * Writes a <code>char</code> array to the specified output.
     * @param output the output buffer to write the array into
     * @param array the <i>char</i> array to write
     * @return the number of bytes written to the output
     * @throws IOException if there was an I/O error
     */
    public int write(BinaryOutput output, char[] array) throws IOException {
        if (array != null)
            return write(output, array, 0, array.length);
        write(output, -1);
        return 4;
    }

    /**
     * Writes a <code>int</code> array to the specified output.
     * @param output the output buffer to write the array into
     * @param array the <i>int</i> array to write
     * @param index the start index in the <i>int</i> array
     * @param length the number of array items to write
     * @return the number of bytes written to the output
     * @throws IOException if there was an I/O error
     * @throws NullPointerException if the specified array is <tt>null</tt>
     * @throws IndexOutOfBoundsException if the <code>index</code> or <code>length</code> are invalid for the specified array
     */
    public int write(BinaryOutput output, int[] array, int index, int length) throws IOException, NullPointerException, IndexOutOfBoundsException {
        int bytes = 4 + (length << 2);
        write(output, length);

        while (length > 0) {
            ByteBuffer byteBuffer = output.prepareOutput(4);
            IntBuffer buffer = byteBuffer.asIntBuffer();
            int lenToWrite = Math.min(length, buffer.remaining());
            buffer.put(array, index, lenToWrite);
            index += lenToWrite;
            length -= lenToWrite;
            byteBuffer.position(byteBuffer.position() + (lenToWrite << 2));
        }

        return bytes;
    }

    /**
     * Writes a <code>int</code> array to the specified output.
     * @param output the output buffer to write the array into
     * @param array the <i>int</i> array to write
     * @return the number of bytes written to the output
     * @throws IOException if there was an I/O error
     */
    public int write(BinaryOutput output, int[] array) throws IOException {
        if (array != null)
            return write(output, array, 0, array.length);
        write(output, -1);
        return 4;
    }

    /**
     * Writes a <code>long</code> array to the specified output.
     * @param output the output buffer to write the array into
     * @param array the <i>long</i> array to write
     * @param index the start index in the <i>long</i> array
     * @param length the number of array items to write
     * @return the number of bytes written to the output
     * @throws IOException if there was an I/O error
     * @throws NullPointerException if the specified array is <tt>null</tt>
     * @throws IndexOutOfBoundsException if the <code>index</code> or <code>length</code> are invalid for the specified array
     */
    public int write(BinaryOutput output, long[] array, int index, int length) throws IOException, NullPointerException, IndexOutOfBoundsException {
        int bytes = 4 + (length << 3);
        write(output, length);

        while (length > 0) {
            ByteBuffer byteBuffer = output.prepareOutput(8);
            LongBuffer buffer = byteBuffer.asLongBuffer();
            int lenToWrite = Math.min(length, buffer.remaining());
            buffer.put(array, index, lenToWrite);
            index += lenToWrite;
            length -= lenToWrite;
            byteBuffer.position(byteBuffer.position() + (lenToWrite << 3));
        }

        return bytes;
    }

    /**
     * Writes a <code>long</code> array to the specified output.
     * @param output the output buffer to write the array into
     * @param array the <i>long</i> array to write
     * @return the number of bytes written to the output
     * @throws IOException if there was an I/O error
     */
    public int write(BinaryOutput output, long[] array) throws IOException {
        if (array != null)
            return write(output, array, 0, array.length);
        write(output, -1);
        return 4;
    }

    /**
     * Writes a <code>float</code> array to the specified output.
     * @param output the output buffer to write the array into
     * @param array the <i>float</i> array to write
     * @param index the start index in the <i>float</i> array
     * @param length the number of array items to write
     * @return the number of bytes written to the output
     * @throws IOException if there was an I/O error
     * @throws NullPointerException if the specified array is <tt>null</tt>
     * @throws IndexOutOfBoundsException if the <code>index</code> or <code>length</code> are invalid for the specified array
     */
    public int write(BinaryOutput output, float[] array, int index, int length) throws IOException, NullPointerException, IndexOutOfBoundsException {
        int bytes = 4 + (length << 2);
        write(output, length);

        while (length > 0) {
            ByteBuffer byteBuffer = output.prepareOutput(4);
            FloatBuffer buffer = byteBuffer.asFloatBuffer();
            int lenToWrite = Math.min(length, buffer.remaining());
            buffer.put(array, index, lenToWrite);
            index += lenToWrite;
            length -= lenToWrite;
            byteBuffer.position(byteBuffer.position() + (lenToWrite << 2));
        }

        return bytes;
    }

    /**
     * Writes a <code>float</code> array to the specified output.
     * @param output the output buffer to write the array into
     * @param array the <i>float</i> array to write
     * @return the number of bytes written to the output
     * @throws IOException if there was an I/O error
     */
    public int write(BinaryOutput output, float[] array) throws IOException {
        if (array != null)
            return write(output, array, 0, array.length);
        write(output, -1);
        return 4;
    }

    /**
     * Writes a <code>double</code> array to the specified output.
     * @param output the output buffer to write the array into
     * @param array the <i>double</i> array to write
     * @param index the start index in the <i>double</i> array
     * @param length the number of array items to write
     * @return the number of bytes written to the output
     * @throws IOException if there was an I/O error
     * @throws NullPointerException if the specified array is <tt>null</tt>
     * @throws IndexOutOfBoundsException if the <code>index</code> or <code>length</code> are invalid for the specified array
     */
    public int write(BinaryOutput output, double[] array, int index, int length) throws IOException, NullPointerException, IndexOutOfBoundsException {
        int bytes = 4 + (length << 3);
        write(output, length);

        while (length > 0) {
            ByteBuffer byteBuffer = output.prepareOutput(8);
            DoubleBuffer buffer = byteBuffer.asDoubleBuffer();
            int lenToWrite = Math.min(length, buffer.remaining());
            buffer.put(array, index, lenToWrite);
            index += lenToWrite;
            length -= lenToWrite;
            byteBuffer.position(byteBuffer.position() + (lenToWrite << 3));
        }

        return bytes;
    }

    /**
     * Writes a <code>double</code> array to the specified output.
     * @param output the output buffer to write the array into
     * @param array the <i>double</i> array to write
     * @return the number of bytes written to the output
     * @throws IOException if there was an I/O error
     */
    public int write(BinaryOutput output, double[] array) throws IOException {
        if (array != null)
            return write(output, array, 0, array.length);
        write(output, -1);
        return 4;
    }


    //************************ Serializing methods for generic objects ************************//

    /**
     * Writes a {@link String} to the specified output.
     * @param output the buffer to write the string into
     * @param string the {@link String} to be written
     * @return the number of bytes written
     * @throws IOException if there was an I/O error
     */
    public int write(BinaryOutput output, String string) throws IOException {
        return write(output, string.toCharArray());
    }

    /**
     * Writes <code>object</code> to the provided output buffer.
     * If the object implements {@link BinarySerializable} interface, it
     * is binary-serialized. Otherwise, a standard Java {@link java.io.Serializable serialization} is used.
     *
     * @param output the buffer to write the object to
     * @param object the object to write
     * @return the number of bytes actually written
     * @throws IOException if there was an error using flushChannel
     */
    public final int write(BinaryOutput output, Object object) throws IOException {
        // Write null as zero-sized object
        if (object == null) {
            write(output, 0);
            return 4;
        }

        /* Prepare BinarySerializable object:
         *   Either a constructor or factory method exists, thus this object must implement BinarySerializable
         *   or a standard java serialization object wrapper is used
         */
        BinarySerializable binarySerializableObject;
        if (object instanceof BinarySerializable) {
            binarySerializableObject = (BinarySerializable) object;
        } else {
            binarySerializableObject = new JavaToBinarySerializable(object);
        }

        // Write object size (this method is final to ensure that the object size is written first)
        int objectSize = getBinarySize(binarySerializableObject);
        write(output, objectSize);

        // Write object data
        if (write(output, binarySerializableObject) != objectSize)
            throw new IllegalStateException("Write operation expected different number of bytes while writing " + binarySerializableObject.getClass().getName());

        return objectSize + 4;
    }

    /**
     * Writes <code>object</code> to this output buffer using binary serialization.
     * The following rules must hold:
     * <ul>
     *   <li>this method must write to the buffer exactly the number of bytes returned by {@link #getBinarySize(BinarySerializable)}</li>
     *   <li>the {@link #readObject(messif.objects.nio.BinaryInput, java.lang.Class) readObject} method must read the serialized data exactly as written by this method</li>
     * </ul> 
     * 
     * @param output the buffer to write the object to
     * @param object the object to write
     * @return the number of bytes actually written
     * @throws IOException if there was an error using flushChannel
     */
    protected abstract int write(BinaryOutput output, BinarySerializable object) throws IOException;


    //************************ Deserializing methods for primitive types ************************//

    /**
     * Returns a <code>boolean</code> value read from the specified input.
     * @param input the input buffer to read the value from
     * @return a <code>boolean</code> value read from the input
     * @throws IOException if there was an I/O error
     */
    public boolean readBoolean(BinaryInput input) throws IOException {
        return (readByte(input) == 0)?false:true;
    }

    /**
     * Returns a <code>byte</code> value read from the specified input.
     * @param input the input buffer to read the value from
     * @return a <code>byte</code> value read from the input
     * @throws IOException if there was an I/O error
     */
    public byte readByte(BinaryInput input) throws IOException {
        return input.readInput(1).get();
    }

    /**
     * Returns a <code>short</code> value read from the specified input.
     * @param input the input buffer to read the value from
     * @return a <code>short</code> value read from the input
     * @throws IOException if there was an I/O error
     */
    public short readShort(BinaryInput input) throws IOException {
        return input.readInput(2).getShort();
    }

    /**
     * Returns a <code>char</code> value read from the specified input.
     * @param input the input buffer to read the value from
     * @return a <code>char</code> value read from the input
     * @throws IOException if there was an I/O error
     */
    public char readChar(BinaryInput input) throws IOException {
        return input.readInput(2).getChar();
    }

    /**
     * Returns a <code>int</code> value read from the specified input.
     * @param input the input buffer to read the value from
     * @return a <code>int</code> value read from the input
     * @throws IOException if there was an I/O error
     */
    public int readInt(BinaryInput input) throws IOException {
        return input.readInput(4).getInt();
    }

    /**
     * Returns a <code>long</code> value read from the specified input.
     * @param input the input buffer to read the value from
     * @return a <code>long</code> value read from the input
     * @throws IOException if there was an I/O error
     */
    public long readLong(BinaryInput input) throws IOException {
        return input.readInput(8).getLong();
    }

    /**
     * Returns a <code>float</code> value read from the specified input.
     * @param input the input buffer to read the value from
     * @return a <code>float</code> value read from the input
     * @throws IOException if there was an I/O error
     */
    public float readFloat(BinaryInput input) throws IOException {
        return input.readInput(4).getFloat();
    }

    /**
     * Returns a <code>double</code> value read from the specified input.
     * @param input the input buffer to read the value from
     * @return a <code>double</code> value read from the input
     * @throws IOException if there was an I/O error
     */
    public double readDouble(BinaryInput input) throws IOException {
        return input.readInput(8).getDouble();
    }


    //************************ Deserializing methods for primitive arrays ************************//

    /**
     * Returns a <code>boolean</code> array read from the specified input.
     * @param input the buffer to read the array from
     * @return a <code>boolean</code> array read from the input
     * @throws IOException if there was an I/O error
     */
    public boolean[] readBooleanArray(BinaryInput input) throws IOException {
        byte[] byteArray = readByteArray(input);
        if (byteArray == null)
            return null;
        boolean[] array = new boolean[byteArray.length];
        for (int i = 0; i < byteArray.length; i++)
            array[i] = (byteArray[i] == 0)?false:true;
        return array;
    }

    /**
     * Returns a <code>byte</code> array read from the specified input.
     * @param input the buffer to read the array from
     * @return a <code>byte</code> array read from the input
     * @throws IOException if there was an I/O error
     */
    public byte[] readByteArray(BinaryInput input) throws IOException {
        int len = readInt(input);
        if (len == -1)
            return null;
        byte[] array = new byte[len];
        int off = 0;
        while (len > 0) {
            ByteBuffer buffer = input.readInput(1);
            int countToRead = Math.min(len, buffer.remaining());
            buffer.get(array, off, countToRead);
            off += countToRead;
            len -= countToRead;
        }
        return array;
    }

    /**
     * Returns a <code>short</code> array read from the specified input.
     * @param input the buffer to read the array from
     * @return a <code>short</code> array read from the input
     * @throws IOException if there was an I/O error
     */
    public short[] readShortArray(BinaryInput input) throws IOException {
        int len = readInt(input);
        if (len == -1)
            return null;
        short[] array = new short[len];
        int off = 0;
        while (len > 0) {
            ByteBuffer byteBuffer = input.readInput(2);
            ShortBuffer buffer = byteBuffer.asShortBuffer();
            int countToRead = Math.min(len, buffer.remaining());
            buffer.get(array, off, countToRead);
            off += countToRead;
            len -= countToRead;
            byteBuffer.position(byteBuffer.position() + (countToRead << 1));
        }
        return array;
    }

    /**
     * Returns a <code>char</code> array read from the specified input.
     * @param input the buffer to read the array from
     * @return a <code>char</code> array read from the input
     * @throws IOException if there was an I/O error
     */
    public char[] readCharArray(BinaryInput input) throws IOException {
        int len = readInt(input);
        if (len == -1)
            return null;
        char[] array = new char[len];
        int off = 0;
        while (len > 0) {
            ByteBuffer byteBuffer = input.readInput(2);
            CharBuffer buffer = byteBuffer.asCharBuffer();
            int countToRead = Math.min(len, buffer.remaining());
            buffer.get(array, off, countToRead);
            off += countToRead;
            len -= countToRead;
            byteBuffer.position(byteBuffer.position() + (countToRead << 1));
        }
        return array;
    }

    /**
     * Returns a <code>int</code> array read from the specified input.
     * @param input the buffer to read the array from
     * @return a <code>int</code> array read from the input
     * @throws IOException if there was an I/O error
     */
    public int[] readIntArray(BinaryInput input) throws IOException {
        int len = readInt(input);
        if (len == -1)
            return null;
        int[] array = new int[len];
        int off = 0;
        while (len > 0) {
            ByteBuffer byteBuffer = input.readInput(4);
            IntBuffer buffer = byteBuffer.asIntBuffer();
            int countToRead = Math.min(len, buffer.remaining());
            buffer.get(array, off, countToRead);
            off += countToRead;
            len -= countToRead;
            byteBuffer.position(byteBuffer.position() + (countToRead << 2));
        }
        return array;
    }

    /**
     * Returns a <code>long</code> array read from the specified input.
     * @param input the buffer to read the array from
     * @return a <code>long</code> array read from the input
     * @throws IOException if there was an I/O error
     */
    public long[] readLongArray(BinaryInput input) throws IOException {
        int len = readInt(input);
        if (len == -1)
            return null;
        long[] array = new long[len];
        int off = 0;
        while (len > 0) {
            ByteBuffer byteBuffer = input.readInput(8);
            LongBuffer buffer = byteBuffer.asLongBuffer();
            int countToRead = Math.min(len, buffer.remaining());
            buffer.get(array, off, countToRead);
            off += countToRead;
            len -= countToRead;
            byteBuffer.position(byteBuffer.position() + (countToRead << 3));
        }
        return array;
    }

    /**
     * Returns a <code>float</code> array read from the specified input.
     * @param input the buffer to read the array from
     * @return a <code>float</code> array read from the input
     * @throws IOException if there was an I/O error
     */
    public float[] readFloatArray(BinaryInput input) throws IOException {
        int len = readInt(input);
        if (len == -1)
            return null;
        float[] array = new float[len];
        int off = 0;
        while (len > 0) {
            ByteBuffer byteBuffer = input.readInput(4);
            FloatBuffer buffer = byteBuffer.asFloatBuffer();
            int countToRead = Math.min(len, buffer.remaining());
            buffer.get(array, off, countToRead);
            off += countToRead;
            len -= countToRead;
            byteBuffer.position(byteBuffer.position() + (countToRead << 2));
        }
        return array;
    }

    /**
     * Returns a <code>double</code> array read from the specified input.
     * @param input the buffer to read the array from
     * @return a <code>double</code> array read from the input
     * @throws IOException if there was an I/O error
     */
    public double[] readDoubleArray(BinaryInput input) throws IOException {
        int len = readInt(input);
        if (len == -1)
            return null;
        double[] array = new double[len];
        int off = 0;
        while (len > 0) {
            ByteBuffer byteBuffer = input.readInput(8);
            DoubleBuffer buffer = byteBuffer.asDoubleBuffer();
            int countToRead = Math.min(len, buffer.remaining());
            buffer.get(array, off, countToRead);
            off += countToRead;
            len -= countToRead;
            byteBuffer.position(byteBuffer.position() + (countToRead << 3));
        }
        return array;
    }


    //************************ Deserializing methods for generic objects ************************//

    /**
     * Returns a {@link String} read from the specified input.
     * @param input the buffer to read the string from
     * @return a {@link String} read from the input
     * @throws IOException if there was an I/O error
     */
    public String readString(BinaryInput input) throws IOException {
        char[] stringBytes = readCharArray(input);
        if (stringBytes == null)
            return null;
        return new String(stringBytes);
    }

    /**
     * Reads an instance from the <code>input</code> using this serializator.
     *
     * @param <E> the class that is expected to be in the input
     * @param input the buffer to read the instance from
     * @param expectedClass the class that is expected to be in the input
     * @return an instance of the deserialized object
     * @throws IOException if there was an I/O error
     * @throws IllegalArgumentException if the constructor or the factory method has a wrong prototype
     */
    public final <E> E readObject(BinaryInput input, Class<E> expectedClass) throws IOException, IllegalArgumentException {
        // Read object size (this method is final to ensure that the object size is read first)
        int objectSize = readObjectSize(input);

        // If the object size is zero, the object is null
        if (objectSize == 0)
            return null;

        return readObjectImpl(input, expectedClass);
    }

    /**
     * Reads an instance using the proper constructor/factory method as specified by this serializator.
     * The following rules must hold:
     * <ul>
     *   <li>this method must read exactly <code>objectSize</code> bytes from the <code>input</code> or throw an exception</li>
     *   <li>the bytes are provided by the buffer exactly as the {@link #write(BinaryOutput, BinarySerializable) write method} has written them</li>
     * </ul>
     *
     * @param <E> the class that is expected to be in the input
     * @param input the buffer to read the instance from
     * @param expectedClass the class that is expected to be in the input
     * @return an instance of the deserialized object
     * @throws IOException if there was an I/O error
     * @throws IllegalArgumentException if the constructor or the factory method has a wrong prototype
     */
    protected abstract <E> E readObjectImpl(BinaryInput input, Class<E> expectedClass) throws IOException, IllegalArgumentException;

    /**
     * Reads an instance created by <code>constructor</code> or <code>factoryMethod</code>
     * from the input buffer.
     * If both the constructor and the factory method are <tt>null</tt>,
     * standard Java {@link java.io.Serializable deserialization} is used.
     *
     * @param input the buffer to read the instance from
     * @param serializator the serializator used to write objects
     * @param constructor the {@link Constructor constructor} of the object class to read
     * @param factoryMethod the {@link Method factory method} of the object class to read
     * @return an instance of the deserialized object
     * @throws IOException if there was an I/O error
     * @throws IllegalArgumentException if the constructor or the factory method has a wrong prototype
     */
    protected static Object readObject(BinaryInput input, BinarySerializator serializator, Constructor constructor, Method factoryMethod) throws IOException, IllegalArgumentException {
        try {
            if (factoryMethod != null) {// If factory method provided
                return factoryMethod.invoke(null, input, serializator);
            } else if (constructor != null) { // If constructor provided
                return constructor.newInstance(input, serializator);
            } else { // Fallback to native java serialization
                return JavaToBinarySerializable.binaryDeserialize(input, serializator);
            }
        } catch (InstantiationException e) {
            // The provided class is abstract
            throw new IllegalArgumentException(e.getMessage());
        } catch (IllegalAccessException e) {
            // The constructor/factory should be "accessible"
            throw new IllegalArgumentException(e.toString());
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException)
                throw (IOException) cause;
            if (cause instanceof RuntimeException)
                throw (RuntimeException) cause;
            throw new IllegalArgumentException("Illegal exception thrown during binary deserialization", cause);
        }
    }


    //************************ Binary size methods ************************//

    /**
     * Returns the size of the (binary) serialized <code>boolean</code>array in bytes.
     * The exact size including all overhead is returned.
     * 
     * @param array the array to get the size for
     * @return the size of the binary-serialized array
     */
    public int getBinarySize(boolean[] array) {
        if (array == null)
            return 4;
        return 4 + (array.length << 1);
    }

    /**
     * Returns the size of the (binary) serialized <code>byte</code>array in bytes.
     * The exact size including all overhead is returned.
     * 
     * @param array the array to get the size for
     * @return the size of the binary-serialized array
     */
    public int getBinarySize(byte[] array) {
        if (array == null)
            return 4;
        return 4 + array.length;
    }

    /**
     * Returns the size of the (binary) serialized <code>short</code>array in bytes.
     * The exact size including all overhead is returned.
     * 
     * @param array the array to get the size for
     * @return the size of the binary-serialized array
     */
    public int getBinarySize(short[] array) {
        if (array == null)
            return 4;
        return 4 + (array.length << 1);
    }

    /**
     * Returns the size of the (binary) serialized <code>char</code>array in bytes.
     * The exact size including all overhead is returned.
     * 
     * @param array the array to get the size for
     * @return the size of the binary-serialized array
     */
    public int getBinarySize(char[] array) {
        if (array == null)
            return 4;
        return 4 + (array.length << 1);
    }

    /**
     * Returns the size of the (binary) serialized <code>int</code>array in bytes.
     * The exact size including all overhead is returned.
     * 
     * @param array the array to get the size for
     * @return the size of the binary-serialized array
     */
    public int getBinarySize(int[] array) {
        if (array == null)
            return 4;
        return 4 + (array.length << 2);
    }

    /**
     * Returns the size of the (binary) serialized <code>long</code>array in bytes.
     * The exact size including all overhead is returned.
     * 
     * @param array the array to get the size for
     * @return the size of the binary-serialized array
     */
    public int getBinarySize(long[] array) {
        if (array == null)
            return 4;
        return 4 + (array.length << 3);
    }

    /**
     * Returns the size of the (binary) serialized <code>float</code>array in bytes.
     * The exact size including all overhead is returned.
     * 
     * @param array the array to get the size for
     * @return the size of the binary-serialized array
     */
    public int getBinarySize(float[] array) {
        if (array == null)
            return 4;
        return 4 + (array.length << 2);
    }

    /**
     * Returns the size of the (binary) serialized <code>double</code>array in bytes.
     * The exact size including all overhead is returned.
     * 
     * @param array the array to get the size for
     * @return the size of the binary-serialized array
     */
    public int getBinarySize(double[] array) {
        if (array == null)
            return 4;
        return 4 + (array.length << 3);
    }

    /**
     * Returns the size of the (binary) serialized {@link String} in bytes.
     * The exact size including all overhead is returned.
     * 
     * @param string the string to get the size for
     * @return the size of the binary-serialized {@link String}
     */
    public int getBinarySize(String string) {
        if (string == null)
            return 4;
        return 4 + 2 * string.length();
    }

    /**
     * Read the size of the object at the current position of the buffer.
     * This method will skip the deleted objects, i.e. the
     * size stored in the stream is negative.
     * Position is advanced to the beginning of the object's data.
     * 
     * @param input the buffer from which to read the object size
     * @return the size of the object
     * @throws IOException if there was an error reading from the input buffer
     */
    protected int readObjectSize(BinaryInput input) throws IOException {
        int objectSize = readInt(input);
        while (objectSize < 0) {
            input.skip(-objectSize);
            objectSize = readInt(input);
        }
        return objectSize;
    }

    /**
     * Skip the object at the current position of the stream.
     * @param stream the stream in which to skip an object
     * @param skipDeleted if <tt>true</tt> the deleted object are silently skipped (their sizes are not reported)
     * @return the skipped object's size in bytes - it is negative if the object was deleted
     * @throws IOException if there was an error reading from the input stream
     */
    public int skipObject(BinaryInput stream, boolean skipDeleted) throws IOException {
        // Compute object size
        int objectSize;
        if (skipDeleted) {
            objectSize = readObjectSize(stream);
        } else {
            objectSize = readInt(stream);
        }

        // Seek over object
        if (objectSize < 0) {
            stream.skip(-objectSize);
        } else {
            stream.skip(objectSize);
        }

        return objectSize;
    }

    /**
     * Returns the size of the (binary) serialized <code>object</code> in bytes.
     * The exact size including all overhead is returned.
     * This method can be very slow if the standard Java {@link java.io.Serializable serialization}
     * is used on object, i.e. when the object does not implement the {@link BinarySerializable} interface.
     * 
     * @param object the object to get the size for
     * @return the size of the binary-serialized <code>object</code>
     * @throws IllegalArgumentException if there was an error using Java standard serialization on the object
     */
    public final int getBinarySize(Object object) throws IllegalArgumentException {
        // Object is null, the size will be zero
        if (object == null)
            return 4;

        // Object will be serialized as the binary serializable object
        if (object instanceof BinarySerializable)
            return getBinarySize((BinarySerializable) object) + 4;

        // Object will be serialized using standard Java serialization
        try {
            return getBinarySize(new JavaToBinarySerializable(object)) + 4;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns the size of the binary-serialized <code>object</code> in bytes.
     * 
     * @param object the object to get the size for
     * @return the size of the binary-serialized <code>object</code>
     * @throws IllegalArgumentException if there was an error using Java standard serialization on the object
     */
    protected abstract int getBinarySize(BinarySerializable object) throws IllegalArgumentException;


    //************************ Search for constructor/factory ************************//

    /**
     * Returns a native-serializable constructor for <code>objectClass</code>.
     * The constructor should have the following prototype:
     * <pre>
     *      <i>ClassConstructor</i>({@link BinaryInput} input, {@link BinarySerializator} serializator) throws {@link IOException}
     * </pre>
     * 
     * @param <T> the object class to construct
     * @param objectClass the object class to construct
     * @return a constructor for <code>objectClass</code> or <tt>null</tt> if there is no native-serializable constructor
     */
    protected static <T> Constructor<T> getNativeSerializableConstructor(Class<T> objectClass) {
        try {
            Constructor<T> constructor = objectClass.getDeclaredConstructor(BinaryInput.class, BinarySerializator.class);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException ignore) {
            return null;
        }
    }

    /**
     * Returns a native-serializable factory method for <code>objectClass</code>.
     * The factory method should have the following prototype:
     * <pre>
     *      <i>ObjectClass</i> binaryDeserialize({@link BinaryInput} input, {@link BinarySerializator} serializator) throws {@link IOException}
     * </pre>
     * 
     * @param objectClass the object class to construct
     * @return a factory method for <code>objectClass</code> or <tt>null</tt> if there is no native-serializable factory method
     */
    protected static Method getNativeSerializableFactoryMethod(Class<?> objectClass) {
        try {
            Method method = objectClass.getDeclaredMethod("binaryDeserialize", BinaryInput.class, BinarySerializator.class);
            if (!Modifier.isStatic(method.getModifiers())) {
                return null;
            }
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException ignore) {
            return null;
        }
    }


    //************************ Helper methods ************************//

    /**
     * Returns the value of the <code>serialVersionUID</code> field for the
     * specified class.
     * @param classToCheck the class for which to look-up the serial version
     * @return the serial version of the specified class
     * @throws NoSuchFieldException if the class does not have serial version
     */
    protected static long getSerialVersionUID(Class<?> classToCheck) throws NoSuchFieldException {
        try {
            Field field = classToCheck.getDeclaredField("serialVersionUID");
            field.setAccessible(true);
            return field.getLong(null);
        } catch (IllegalAccessException ex) {
            // This should never happen
            return 0;
        }
    }

    /**
     * Returns the hash code for value of the <code>serialVersionUID</code>
     * field of the specified class.
     * @param classToCheck the class for which to look-up the serial version
     * @return the serial version of the specified class
     */
    protected static int getSerialVersionUIDHash(Class<?> classToCheck) {
        try {
            long serialVersion = getSerialVersionUID(classToCheck);
            return (int)(serialVersion >> 32) | (int)serialVersion;
        } catch (NoSuchFieldException ignore) {
            return -1;
        }
    }

}
