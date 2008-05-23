/*
 * NativeDataInput
 * 
 */

package messif.buckets.io;

import java.io.IOException;

/**
 * The <code>NativeDataInput</code> interface provides methods
 * for reading bytes from a binary stream and reconstructing from
 * them data in any of the Java primitive types.
 * 
 * @see NativeDataOutput
 * @see BinarySerializable
 * @author xbatko
 */
public interface NativeDataInput {

    /**
     * Returns a <code>boolean</code> value read from this input.
     *
     * @return a <code>boolean</code> value read from this input
     * @throws IOException if there was an I/O error
     */
    public boolean readBoolean() throws IOException;

    /**
     * Returns a <code>byte</code> value read from this input.
     *
     * @return a <code>byte</code> value read from this input
     * @throws IOException if there was an I/O error
     */
    public byte readByte() throws IOException;

    /**
     * Returns a <code>short</code> value read from this input.
     *
     * @return a <code>short</code> value read from this input
     * @throws IOException if there was an I/O error
     */
    public short readShort() throws IOException;

    /**
     * Returns a <code>char</code> value read from this input.
     *
     * @return a <code>char</code> value read from this input
     * @throws IOException if there was an I/O error
     */
    public char readChar() throws IOException;

    /**
     * Returns a <code>int</code> value read from this input.
     *
     * @return a <code>int</code> value read from this input
     * @throws IOException if there was an I/O error
     */
    public int readInt() throws IOException;

    /**
     * Returns a <code>long</code> value read from this input.
     *
     * @return a <code>long</code> value read from this input
     * @throws IOException if there was an I/O error
     */
    public long readLong() throws IOException;

    /**
     * Returns a <code>float</code> value read from this input.
     *
     * @return a <code>float</code> value read from this input
     * @throws IOException if there was an I/O error
     */
    public float readFloat() throws IOException;

    /**
     * Returns a <code>double</code> value read from this input.
     *
     * @return a <code>double</code> value read from this input
     * @throws IOException if there was an I/O error
     */
    public double readDouble() throws IOException;

    /**
     * Returns a string read from this input.
     *
     * @return a string read from this input
     * @throws IOException if there was an I/O error
     */
    public String readString() throws IOException;

    /**
     * Reads up to <code>length</code> bytes of data from this input into
     * <code>buffer</code> starting from position <code>offset</code>.
     * An attempt is made to read as many as <code>length</code> bytes,
     * but a smaller number may be read.
     *
     * @param buffer the buffer into which the data is read
     * @param offset the start offset in array <code>buffer</code> at which the data is written
     * @param length the maximum number of bytes to read
     * @return the total number of bytes actually read (and stored into the buffer)
     * @throws IOException if there was an I/O error
     * @throws IndexOutOfBoundsException if the buffer runs out of space (e.g., if the <code>buffer.length - offset &lt length</code>)
     */
    public int read(byte[] buffer, int offset, int length) throws IOException, IndexOutOfBoundsException;

}
