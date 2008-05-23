/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package messif.buckets.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import messif.utility.Convert;

/**
 *
 * @author xbatko
 */
public class BinarySerializingInputStream extends InputStream implements NativeDataInput {

    /** Minimal buffer size in bytes */
    private final int MINIMAL_BUFFER_SIZE = 32;

    /** The buffer where data is stored */
    protected final ByteBuffer byteBuffer;

    /** The file from which to read data */
    protected final ReadableByteChannel readChannel;

    /** The current position in the read channel */
    protected long readChannelPosition;

    /** The maximal position that can be read from the read channel */
    protected final long readChannelMaximalPosition;

    /**
     * Creates a new instance of BinarySerializingOutputStream.
     * @param bufferSize the size of the internal buffer used for flushing
     * @param readChannel the channel from which to read data
     * @param maxLength the maximal length of data
     * @throws IOException if there was an error using readChannel
     */
    public BinarySerializingInputStream(int bufferSize, ReadableByteChannel readChannel, long maxLength) throws IOException {
        if (bufferSize < MINIMAL_BUFFER_SIZE)
            throw new IllegalArgumentException("Buffer must be at least " + MINIMAL_BUFFER_SIZE + " bytes long");
        this.byteBuffer = ByteBuffer.allocateDirect(bufferSize);
        this.readChannel = readChannel;
        this.readChannelMaximalPosition = maxLength;

        // Read first chunk of data
        readChannelPosition = readChannel.read(byteBuffer);
        byteBuffer.flip();
    }

    /**
     * Reads the next byte of data from the input stream. The value byte is
     * returned as an <code>int</code> in the range <code>0</code> to
     * <code>255</code>. If no byte is available because the end of the stream
     * has been reached, the value <code>-1</code> is returned. This method
     * blocks until input data is available, the end of the stream is detected,
     * or an exception is thrown.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the stream is reached
     * @throws IOException if there was an error using readChannel
     */
    @Override
    public int read() throws IOException {
        checkBufferSize(1, true);
        return byteBuffer.get();
    }

    /**
     * Reads up to <code>len</code> bytes of data from the input stream into
     * an array of bytes.  An attempt is made to read as many as
     * <code>len</code> bytes, but a smaller number may be read.
     * The number of bytes actually read is returned as an integer.
     *
     * @param buf the buffer into which the data is read
     * @param off the start offset in array <code>buf</code> at which the data is written
     * @param len the maximum number of bytes to read
     * @return the total number of bytes read into the buffer, or
     *             <code>-1</code> if there is no more data because the end of
     *             the stream has been reached
     * @throws IOException if there was an error reading from the input stream
     * @throws IndexOutOfBoundsException if the <code>buf.length - off &lt len</code>
     */
    @Override
    public int read(byte buf[], int off, int len) throws IOException, IndexOutOfBoundsException {
        int totalRead = 0;
        while (len > 0) {
            int bufLen = checkBufferSize(len, false);
            byteBuffer.get(buf, off, bufLen);
            len -= bufLen;
            off += bufLen;
            totalRead += bufLen;
        }
        return totalRead;
    }

    /**
     * Read an instance of <code>objectClass</code> from this input stream.
     *
     * @param objectClass the object class to read
     * @return an instance of <code>objectClass</code>
     * @throws IOException if there was an error reading from the input stream
     * @throws IllegalArgumentException if the constructor or factory method exists for the <code>objectClass</code>, but has a wrong prototype
     */
    public <E> E read(Class<E> objectClass) throws IOException, IllegalArgumentException {
        Object constructorOrMethod;
        try {
            // Try to get the native-serialization constructor for the objectClass
            constructorOrMethod = JavaToBinarySerializable.getNativeSerializableConstructor(objectClass);
        } catch (NoSuchMethodException ignore) {
            try {
                // Try to get the native-serialization factory for the objectClass
                constructorOrMethod = JavaToBinarySerializable.getNativeSerializableFactoryMethod(objectClass);
            } catch (NoSuchMethodException ignoretoo) {
                // Both the native-serialization tries failed, fall back to Java serialization
                constructorOrMethod = null;
            }
        }

        // Read the object using the specified constructor, factory method or Java serialization
        return Convert.safeGenericCast(read(constructorOrMethod), objectClass);
    }

    /**
     * Read an instance created by <code>constructorOrFactory</code> from this input stream.
     * If the constructorOrFactory is <tt>null</tt>, standard Java {@link Serializable deserialization}
     * is used.
     *
     * @param constructorOrFactory the {@link java.lang.reflect.Constructor constructor} or
     *          {@link java.lang.reflect.Method factory method} of the object class to read
     * @return an instance of an serialized object
     * @throws IOException if there was an error reading from the input stream
     * @throws IllegalArgumentException if there is a constructor or a factory method for the <code>objectClass</code>, but it has a wrong prototype
     */
    protected Object read(Object constructorOrFactory) throws IOException, IllegalArgumentException {
        // Skip the deleted objects (the size stored in the stream is negative)
        int objectSize;
        for (;;) {
            objectSize = readInt();
            if (objectSize >= 0)
                break;
            skip(-objectSize);
        }

        // If the object size is zero, the object was null
        if (objectSize == 0)
            return null;

        if (constructorOrFactory != null)
            // Use the constructor or factory method to create the instance
            return JavaToBinarySerializable.constructNativeSerializable(constructorOrFactory, this, objectSize);
        else
            // Use Java serialization
            return JavaToBinarySerializable.binaryDeserialize(this, objectSize);
    }

    /**
     * Returns an estimate of the number of bytes that can be read (or 
     * skipped over) from this input stream without blocking by the next
     * invocation of a method for this input stream. The next invocation
     * might be the same thread or another thread.  A single read or skip of this
     * many bytes will not block, but may read or skip fewer bytes.
     *
     * @return an estimate of the number of bytes that can be read (or skipped
     *         over) from this input stream without blocking or {@code 0} when
     *         it reaches the end of the input stream
     * @throws IOException if there was an error using readChannel
     */
    @Override
    public int available() throws IOException {
        return byteBuffer.remaining();
    }

    /**
     * Returns current position from the beginning of the stream.
     * @return current position from the beginning of the stream
     */
    public long position() {
        return readChannelPosition - byteBuffer.remaining();
    }

    /**
     * Returns the remaining number of bytes in the stream.
     * If the channel is at the end-of-file (i.e., the maximal length is reached), -1 is returned.
     * @return the remaining number of bytes in the stream
     */
    public long remaining() {
        long remaining = readChannelMaximalPosition - position();
        if (remaining > 0)
            return remaining;
        return -1;
    }

    /**
     * Checks the buffer size for remaining buffered data size. If the <code>requiredSize</code>
     * is bigger than the actual remaining size of the buffer, the buffer is compacted
     * and aditional chunk of data is read from the readChannel.
     * If the <code>requiredSize</code> is bigger than the capacity
     * of the buffer, only the capacity is read.
     * 
     * @param requiredSize the required remaining size of the buffered data
     * @param enforce if set to <tt>true</tt> the requiredSize is enfoced, i.e.
     *        if there is not enough data, an {@link IOException} will be thrown
     * @return the size of the actually available data
     * @throws IOException if there was an error using readChannel
     */
    protected synchronized int checkBufferSize(int requiredSize, boolean enforce) throws IOException {
        // If there is enough data in the buffer, we are done
        if (byteBuffer.remaining() >= requiredSize)
            return requiredSize;

        // If there is some space in the buffer
        if (byteBuffer.remaining() < byteBuffer.capacity())
            try {
                // Switch buffer to reading from stream
                byteBuffer.compact();

                // Check for the maximal position
                if (readChannelMaximalPosition - readChannelPosition < byteBuffer.remaining())
                    byteBuffer.limit(byteBuffer.position() + (int)(readChannelMaximalPosition - readChannelPosition));

                // Read next chunk of data
                int readBytes = readChannel.read(byteBuffer);
                if (readBytes == -1)
                    throw new EOFException("Cannot read more bytes");
                readChannelPosition += readBytes;
            } finally {
                // Switch buffer back
                byteBuffer.flip();
            }

        // Check for enforcement
        if (enforce && byteBuffer.remaining() < requiredSize)
            throw new IOException("Cannot read required number of bytes");

        // Return either the required size, if it is smaller than the buffer, or the buffer size
        return Math.min(byteBuffer.remaining(), requiredSize);
    }


    //**************** DataOutput methods ****************//

    /**
     * Returns a <code>boolean</code> value read from this input.
     *
     * @return a <code>boolean</code> value read from this input
     * @throws IOException if there was an I/O error
     */
    public boolean readBoolean() throws IOException {
        checkBufferSize(1, true);
        return (byteBuffer.get() == 0)?false:true;
    }

    /**
     * Returns a <code>byte</code> value read from this input.
     *
     * @return a <code>byte</code> value read from this input
     * @throws IOException if there was an I/O error
     */
    public byte readByte() throws IOException {
        checkBufferSize(1, true);
        return byteBuffer.get();
    }

    /**
     * Returns a <code>short</code> value read from this input.
     *
     * @return a <code>short</code> value read from this input
     * @throws IOException if there was an I/O error
     */
    public short readShort() throws IOException {
        checkBufferSize(2, true);
        return byteBuffer.getShort();
    }

    /**
     * Returns a <code>char</code> value read from this input.
     *
     * @return a <code>char</code> value read from this input
     * @throws IOException if there was an I/O error
     */
    public char readChar() throws IOException {
        checkBufferSize(2, true);
        return byteBuffer.getChar();
    }

    /**
     * Returns a <code>int</code> value read from this input.
     *
     * @return a <code>int</code> value read from this input
     * @throws IOException if there was an I/O error
     */
    public int readInt() throws IOException {
        checkBufferSize(4, true);
        return byteBuffer.getInt();
    }

    /**
     * Returns a <code>long</code> value read from this input.
     *
     * @return a <code>long</code> value read from this input
     * @throws IOException if there was an I/O error
     */
    public long readLong() throws IOException {
        checkBufferSize(8, true);
        return byteBuffer.getLong();
    }

    /**
     * Returns a <code>float</code> value read from this input.
     *
     * @return a <code>float</code> value read from this input
     * @throws IOException if there was an I/O error
     */
    public float readFloat() throws IOException {
        checkBufferSize(4, true);
        return byteBuffer.getFloat();
    }

    /**
     * Returns a <code>double</code> value read from this input.
     *
     * @return a <code>double</code> value read from this input
     * @throws IOException if there was an I/O error
     */
    public double readDouble() throws IOException {
        checkBufferSize(8, true);
        return byteBuffer.getDouble();
    }

    /**
     * Returns a string read from this input.
     *
     * @return a string read from this input
     * @throws IOException if there was an I/O error
     */
    public String readString() throws IOException {
        int size = readInt();
        byte[] buffer = new byte[size];
        read(buffer);
        return new String(buffer);
    }

}
