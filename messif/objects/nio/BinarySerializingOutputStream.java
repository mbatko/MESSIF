/*
 * BinarySerializingOutputStream
 * 
 */

package messif.objects.nio;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Buffered output stream for operating over channels.
 * The stream offers {@link NativeDataOutput write methods} for Java primitives as well as
 * write support for {@link BinarySerializable} and {@link java.io.Serializable} objects.
 * 
 * <p>
 * Note that it is <em>not safe</em> to use several BinarySerializingOutputStreams over the
 * same channel (even if synchronized). For file channels, the {@link BinarySerializingFileOutputStream}
 * can be used if you need this functionality. Use copy-pipes if you need it
 * on other channel types.
 * </p>
 * 
 * <p>
 * If multiple threads use the same instance of this class, the access to the
 * instance must be synchronized.
 * </p>
 * 
 * @see BinarySerializingInputStream
 * @author xbatko
 */
public class BinarySerializingOutputStream extends OutputStream implements NativeDataOutput {

    /** Minimal buffer size in bytes */
    private final int MINIMAL_BUFFER_SIZE = 32;

    /** The buffer where data is stored */
    protected final ByteBuffer byteBuffer;

    /** The file to which flush filled buffer */
    protected final WritableByteChannel flushChannel;

    /** The current position in the flush channel */
    protected long flushChannelPosition = 0;

    /** The maximal position written to the flush channel */
    protected final long flushChannelMaximalPosition;

    /**
     * Creates a new instance of BinarySerializingOutputStream.
     * @param bufferSize the size of the internal buffer used for flushing
     * @param bufferDirect allocate the internal buffer as {@link ByteBuffer#allocateDirect direct}
     * @param flushChannel the channel into which to write data
     * @param maxLength the maximal length of data 
     * @throws IOException if there was an error using flushChannel
     */
    public BinarySerializingOutputStream(int bufferSize, boolean bufferDirect, WritableByteChannel flushChannel, long maxLength) throws IOException {
        if (bufferSize < MINIMAL_BUFFER_SIZE)
            throw new IllegalArgumentException("Buffer must be at least " + MINIMAL_BUFFER_SIZE + " bytes long");
        if (bufferDirect)
            this.byteBuffer = ByteBuffer.allocateDirect(bufferSize);
        else
            this.byteBuffer = ByteBuffer.allocate(bufferSize);
        this.flushChannel = flushChannel;
        this.flushChannelMaximalPosition = maxLength;
    }

    /**
     * Writes the specified byte to this output stream. 
     * @param b the byte to be written
     * @throws IOException if there was an error using flushChannel
     */
    public void write(int b) throws IOException {
        checkBufferSize(1, true);
	byteBuffer.put((byte)b);
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array 
     * starting at offset <code>off</code> to this output stream.
     *
     * @param bytes the data
     * @param off the start offset in the data
     * @param len the number of bytes to write
     * @throws IOException if there was an error using flushChannel
     */
    @Override
    public void write(byte bytes[], int off, int len) throws IOException {
        while (len > 0) {
            // Fill buffer with the data and adjust offset and length
            int lenToWrite = checkBufferSize(len, false);
            byteBuffer.put(bytes, off, lenToWrite);
            off += lenToWrite;
            len -= lenToWrite;
        }
    }

    /**
     * Writes <code>object</code> to this output stream.
     *
     * @param object the object to write
     * @return the number of bytes actually written
     * @throws IOException if there was an error using flushChannel
     */
    public int write(BinarySerializable object) throws IOException {
        // Write null as zero-sized object
        if (object == null) {
            writeInt(0);
            return 4;
        } else {
            int objectSize = object.getSize();
            writeInt(objectSize);
            object.binarySerialize(this);
            return objectSize + 4;
        }
    }

    /**
     * Flushes this output stream and forces any buffered output bytes 
     * to be written out to flush channel.
     * 
     * @throws IOException if there was an error using flushChannel
     */
    @Override
    public void flush() throws IOException {
        // Must empty the whole buffer, so require the capacity of the buffer
        checkBufferSize(byteBuffer.capacity(), true);
    }

    /**
     * Returns <tt>true</tt> if there are some bytes pending in the buffer.
     * @return <tt>true</tt> if there are some bytes pending in the buffer
     * @see #flush
     */
    public boolean isDirty() {
        return byteBuffer.position() > 0;
    }

    /**
     * Closes this output stream.
     * The flush channel is not closed, it is the responsibility of the
     * calling class.
     * 
     * @throws IOException if there was an error using flushChannel
     */
    @Override
    public void close() throws IOException {
        flush();
    }

    /**
     * Returns current position from the beginning of the stream.
     * @return current position from the beginning of the stream
     */
    public long position() {
        return flushChannelPosition + byteBuffer.position();
    }

    /**
     * Returns the remaining number of bytes in the stream.
     * If the channel is at the end-of-file (i.e., the maximal length is reached), -1 is returned.
     * @return the remaining number of bytes in the stream
     */
    public long remaining() {
        long remaining = flushChannelMaximalPosition - position();
        if (remaining > 0)
            return remaining;
        return -1;
    }

    /**
     * Checks the buffer size for remaining space. If the <code>requiredSize</code>
     * is bigger than the actual remaining size of the buffer, the buffer is flushed
     * out to the file. If the <code>requiredSize</code> is bigger than the capacity
     * of the buffer, only the capacity is freed.
     * 
     * @param requiredSize the required free space size of the buffer
     * @param enforce if set to <tt>true</tt> the requiredSize is enfoced, i.e.
     *        if there is not enough space, an {@link IOException} will be thrown
     * @return the size of the actually allocated free space
     * @throws IOException if there was an error using flushChannel
     */
    protected int checkBufferSize(int requiredSize, boolean enforce) throws IOException {
        // Check overflow in flushChannel
        long remaining = remaining();
        if (remaining - requiredSize < 0) {
            if (enforce || remaining < 0)
                throw new EOFException("Cannot allocate " + requiredSize + " bytes");
            else
                requiredSize = (int)remaining;
        }

        // If there is enough space in the buffer, we are done
        if (byteBuffer.remaining() >= requiredSize)
            return requiredSize;

        // The buffer has some data that can be flushed
        if (byteBuffer.position() > 0)
            try {
                byteBuffer.flip();
                flushChannelPosition += writeChannelData();
            } finally {
                byteBuffer.compact();
            }

        // Check for enforcement
        if (enforce && byteBuffer.remaining() < requiredSize)
            throw new IOException("Cannot allocate required space - the buffer is too small");

        // Return either the required size, if it is smaller than the buffer, or the buffer size
        return Math.min(byteBuffer.remaining(), requiredSize);
    }

    /** 
     * Write current buffered data to the flush channel.
     * @return the number of bytes written
     * @throws IOException if there was an error using flushChannel
     */
    protected int writeChannelData() throws IOException {
        return flushChannel.write(byteBuffer);
    }


    //**************** Special data output methods ****************//

    /**
     * Writes a {@link Class} to this output stream.
     *
     * @param classToWrite the {@link Class} to be written
     * @return the number of bytes written to the output
     * @throws IOException if there was an I/O error using flushChannel
     */
    public int writeClass(Class<?> classToWrite) throws IOException {
        return writeString((classToWrite == null)?null:classToWrite.getName());
    }

    /**
     * Writes a {@link Method} to this output stream.
     *
     * @param method the {@link Method} to be written
     * @return the number of bytes written to the output
     * @throws IOException if there was an I/O error using flushChannel
     */
    public int writeMethod(Method method) throws IOException {
        if (method == null)
            return writeClass(null);

        // Write method's declaring class and name
        int bytesWritten = writeClass(method.getDeclaringClass());
        bytesWritten += writeString(method.getName());

        // Write prototype
        Class<?>[] prototype = method.getParameterTypes();
        writeInt(prototype.length); bytesWritten += 4;
        for (int i = 0; i < prototype.length; i++)
            bytesWritten += writeClass(prototype[i]);

        return bytesWritten;
    }

    /**
     * Writes a {@link Constructor} to this output stream.
     *
     * @param constructor the {@link Constructor} to be written
     * @return the number of bytes written to the output
     * @throws IOException if there was an I/O error using flushChannel
     */
    public int writeConstructor(Constructor<?> constructor) throws IOException {
        if (constructor == null)
            return writeClass(null);

        // Write constructor's declaring class
        int bytesWritten = writeClass(constructor.getDeclaringClass());

        // Write prototype
        Class<?>[] prototype = constructor.getParameterTypes();
        writeInt(prototype.length); bytesWritten += 4;
        for (int i = 0; i < prototype.length; i++)
            bytesWritten += writeClass(prototype[i]);

        return bytesWritten;
    }


    //**************** DataOutput methods ****************//

    /**
     * Writes a <code>boolean</code> value to this output stream.
     *
     * @param value the <code>boolean</code> value to be written
     * @throws IOException if there was an I/O error using flushChannel
     */
    public void writeBoolean(boolean value) throws IOException {
        write(value?1:0);
    }

    /**
     * Writes a <code>byte</code> value to this output stream.
     *
     * @param value the <code>byte</code> value to be written
     * @throws IOException if there was an I/O error using flushChannel
     */
    public void writeByte(byte value) throws IOException {
        checkBufferSize(1, true);
        byteBuffer.put(value);
    }

    /**
     * Writes a <code>short</code> value to this output stream.
     *
     * @param value the <code>short</code> value to be written
     * @throws IOException if there was an I/O error using flushChannel
     */
    public void writeShort(short value) throws IOException {
        checkBufferSize(2, true);
        byteBuffer.putShort(value);
    }

    /**
     * Writes a <code>char</code> value to this output stream.
     *
     * @param value the <code>char</code> value to be written
     * @throws IOException if there was an I/O error using flushChannel
     */
    public void writeChar(char value) throws IOException {
        checkBufferSize(2, true);
        byteBuffer.putChar(value);
    }

    /**
     * Writes an <code>int</code> value to this output stream.
     *
     * @param value the <code>int</code> value to be written
     * @throws IOException if there was an I/O error using flushChannel
     */
    public void writeInt(int value) throws IOException {
        checkBufferSize(4, true);
        byteBuffer.putInt(value);
    }

    /**
     * Writes a <code>long</code> value to this output stream.
     *
     * @param value the <code>long</code> value to be written
     * @throws IOException if there was an I/O error using flushChannel
     */
    public void writeLong(long value) throws IOException {
        checkBufferSize(8, true);
        byteBuffer.putLong(value);
    }

    /**
     * Writes a <code>float</code> value to this output stream.
     *
     * @param value the <code>float</code> value to be written
     * @throws IOException if there was an I/O error using flushChannel
     */
    public void writeFloat(float value) throws IOException {
        checkBufferSize(4, true);
        byteBuffer.putFloat(value);
    }

    /**
     * Writes a <code>double</code> value to this output stream.
     *
     * @param value the <code>double</code> value to be written
     * @throws IOException if there was an I/O error using flushChannel
     */
    public void writeDouble(double value) throws IOException {
        checkBufferSize(8, true);
        byteBuffer.putDouble(value);
    }

    /**
     * Writes a string to this output stream.
     *
     * @param string the string to be written
     * @return the number of bytes written to the output
     * @throws IOException if there was an I/O error using flushChannel
     */
    public int writeString(String string) throws IOException {
        if (string == null) {
            writeInt(-1);
            return 4;
        } else {
            byte[] stringBytes = string.getBytes();
            writeInt(stringBytes.length); // Warning - this is NOT the string.length()!!!
            write(stringBytes);
            return 4 + stringBytes.length;
        }
    }

}
