/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package messif.objects.nio;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * Buffered input stream for operating over channels.
 * The stream offers {@link NativeDataInput read methods} for Java primitives as well as
 * read support for {@link BinarySerializable} and {@link java.io.Serializable} objects.
 * 
 * <p>
 * Note that it is <em>not safe</em> to use several BinarySerializingInputStreams over the
 * same channel (even if synchronized). For file channels, the {@link BinarySerializingFileInputStream}
 * can be used if you need this functionality. Use copy-pipes if you need it
 * on other channel types.
 * </p>
 * 
 * <p>
 * If multiple threads use the same instance of this class, the access to the
 * instance must be synchronized.
 * </p>
 * 
 * @see BinarySerializingOutputStream
 * @author xbatko
 */
public class ByteBufferInputStream extends BinaryInputStream {

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
     * @param bufferDirect allocate the internal buffer as {@link ByteBuffer#allocateDirect direct}
     * @param readChannel the channel from which to read data
     * @param maxLength the maximal length of data
     * @throws IOException if there was an error using readChannel
     */
    public ByteBufferInputStream(int bufferSize, boolean bufferDirect, ReadableByteChannel readChannel, long maxLength) throws IOException {
        if (bufferSize < MINIMAL_BUFFER_SIZE)
            throw new IllegalArgumentException("Buffer must be at least " + MINIMAL_BUFFER_SIZE + " bytes long");
        if (bufferDirect)
            this.byteBuffer = ByteBuffer.allocateDirect(bufferSize);
        else
            this.byteBuffer = ByteBuffer.allocate(bufferSize);
        this.readChannel = readChannel;
        this.readChannelMaximalPosition = maxLength;

        // The buffer is empty first, first attempt to read will fill it
        byteBuffer.limit(0);
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
        checkBufferSize(1, 1);
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
            int bufLen = checkBufferSize(len, 1);
            byteBuffer.get(buf, off, bufLen);
            len -= bufLen;
            off += bufLen;
            totalRead += bufLen;
        }
        return totalRead;
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
     * Checks if there is enough data in the buffer. If the <code>minimalSize</code>
     * is bigger than the actual remaining size of the buffer, the buffer is compacted
     * and aditional chunk of data is read from the readChannel.
     * Returns either the number of bytes actually available in the buffer or the <code>checkSize</code>
     * whichever is smaller.
     * 
     * @param checkSize the checked number of bytes that should be available in the buffer
     * @param minimalSize the minimal number of bytes that must be available in the buffer
     * @return either the number of bytes actually available or the <code>checkSize</code> whichever is smaller
     * @throws IOException if there was an error using readChannel
     */
    @Override
    protected int checkBufferSize(int checkSize, int minimalSize) throws IOException {
        if (minimalSize > byteBuffer.remaining())
            readChunk(minimalSize);

        // Return either the required size, if it is smaller than the buffer, or the buffer size
        return Math.min(byteBuffer.remaining(), checkSize);
    }

    /**
     * Checks if there is enough data in the buffer. If the <code>minimalSize</code>
     * is bigger than the actual remaining size of the buffer, the buffer is compacted
     * and aditional chunk of data is read from the readChannel.
     * 
     * @param minimalSize the minimal number of bytes that must be available in the buffer
     * @throws IOException if there was an error using readChannel
     */
    @Override
    protected void ensureBufferSize(int minimalSize) throws IOException {
        if (minimalSize > byteBuffer.remaining())
            readChunk(minimalSize);
    }

    /**
     * Read aditional chunk of data from the readChannel. It is guaranteed
     * that at least <code>minBytes</code> is read or an exception is thrown.
     * 
     * @param minBytes the minimal number of bytes that must be added to the buffer
     * @return the number of bytes read from the readChannel
     * @throws IOException if there was an error using readChannel
     */
    protected int readChunk(int minBytes) throws IOException {
        if (byteBuffer.remaining() + minBytes > byteBuffer.capacity())
            throw new IOException("Buffer is too small to accomodate " + minBytes + " additional bytes");

        try {
            // Switch buffer to reading from stream
            byteBuffer.compact();

            // Check for the maximal position
            if (readChannelMaximalPosition - readChannelPosition < byteBuffer.remaining())
                byteBuffer.limit(byteBuffer.position() + (int)(readChannelMaximalPosition - readChannelPosition));

            // Read next chunk of data
            int readBytes = readChannelData();
            if (readBytes < minBytes)
                throw new EOFException("Cannot read more bytes - end of file encountered");
            readChannelPosition += readBytes;

            return readBytes;
        } finally {
            // Switch buffer back
            byteBuffer.flip();
        }
    }

    /** 
     * Reads next chunk of data into {@link #byteBuffer internal buffer}.
     * @return the number of bytes read
     * @throws IOException if there was an error using readChannel
     */
    protected int readChannelData() throws IOException {
        return readChannel.read(byteBuffer);
    }

    /**
     * Returns the buffer for binary operations.
     * @return the buffer for binary operations
     * @throws IOException if there was an error using the buffer
     */
    @Override
    protected ByteBuffer getBuffer() throws IOException {
        return byteBuffer;
    }

}
