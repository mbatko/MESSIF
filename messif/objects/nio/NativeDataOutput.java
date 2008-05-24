/*
 * NativeDataOutput
 * 
 */

package messif.objects.nio;

import java.io.IOException;

/**
 * The <code>NativeDataOutput</code> interface provides methods for converting
 * data from any of the Java primitive types to a series of bytes.
 * <p>
 * For all the methods in this interface that
 * write bytes, it is generally true that if
 * a byte cannot be written for any reason,
 * an {@link IOException} is thrown.
 * </p>
 *
 * @see NativeDataInput
 * @see BinarySerializable
 * @author xbatko
 */
public interface NativeDataOutput {

    /**
     * Writes a <code>boolean</code> value to this output.
     *
     * @param value the <code>boolean</code> value to be written
     * @throws IOException if there was an I/O error
     */
    public void writeBoolean(boolean value) throws IOException;

    /**
     * Writes a <code>byte</code> value to this output.
     *
     * @param value the <code>byte</code> value to be written
     * @throws IOException if there was an I/O error
     */
    public void writeByte(byte value) throws IOException;

    /**
     * Writes a <code>short</code> value to this output.
     *
     * @param value the <code>short</code> value to be written
     * @throws IOException if there was an I/O error
     */
    public void writeShort(short value) throws IOException;

    /**
     * Writes a <code>char</code> value to this output.
     *
     * @param value the <code>char</code> value to be written
     * @throws IOException if there was an I/O error
     */
    public void writeChar(char value) throws IOException;

    /**
     * Writes an <code>int</code> value to this output.
     *
     * @param value the <code>int</code> value to be written
     * @throws IOException if there was an I/O error
     */
    public void writeInt(int value) throws IOException;

    /**
     * Writes a <code>long</code> value to this output.
     *
     * @param value the <code>long</code> value to be written
     * @throws IOException if there was an I/O error
     */
    public void writeLong(long value) throws IOException;

    /**
     * Writes a <code>float</code> value to this output.
     *
     * @param value the <code>float</code> value to be written
     * @throws IOException if there was an I/O error
     */
    public void writeFloat(float value) throws IOException;

    /**
     * Writes a <code>double</code> value to this output.
     *
     * @param value the <code>double</code> value to be written
     * @throws IOException if there was an I/O error
     */
    public void writeDouble(double value) throws IOException;

    /**
     * Writes a string to this output.
     *
     * @param string the string to be written
     * @return the number of bytes written to the output
     * @throws IOException if there was an I/O error
     */
    public int writeString(String string) throws IOException;

    /**
     * Writes <code>len</code> bytes from the specified byte array 
     * starting at offset <code>off</code> to this output.
     *
     * @param bytes the data to write
     * @param off the start index in the <code>bytes</code> array
     * @param len the number of bytes to write
     * @throws IOException if there was an I/O error
     */
    public void write(byte[] bytes, int off, int len) throws IOException;
}
