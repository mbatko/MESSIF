/*
 * ByteBufferOutputStream
 * 
 */

package messif.objects.nio;

import java.io.IOException;
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
public class ByteBufferOutputStream extends BinaryOutputStream {

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
     * Creates a new instance of ByteBufferOutputStream.
     * @param bufferSize the size of the internal buffer used for flushing
     * @param bufferDirect allocate the internal buffer as {@link ByteBuffer#allocateDirect direct}
     * @param flushChannel the channel into which to write data
     * @param maxLength the maximal length of data 
     * @throws IOException if there was an error using flushChannel
     */
    public ByteBufferOutputStream(int bufferSize, boolean bufferDirect, WritableByteChannel flushChannel, long maxLength) throws IOException {
        if (bufferSize < MINIMAL_BUFFER_SIZE)
            throw new IllegalArgumentException("Buffer must be at least " + MINIMAL_BUFFER_SIZE + " bytes long");
        if (bufferDirect)
            this.byteBuffer = ByteBuffer.allocateDirect(bufferSize);
        else
            this.byteBuffer = ByteBuffer.allocate(bufferSize);
        this.flushChannel = flushChannel;
        this.flushChannelMaximalPosition = maxLength;

        // Restrict buffer if the max length is smaller than its size
        if (maxLength < bufferSize)
            this.byteBuffer.limit((int)maxLength);
    }

    /**
     * Writes the specified byte to this output stream. 
     * @param b the byte to be written
     * @throws IOException if there was an error using flushChannel
     */
    public void write(int b) throws IOException {
        ensureBufferSize(1);
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
            int lenToWrite = checkBufferSize(len, 1);
            byteBuffer.put(bytes, off, lenToWrite);
            off += lenToWrite;
            len -= lenToWrite;
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
        ensureBufferSize(byteBuffer.capacity());
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
     * Checks if there is enough space in the buffer. If the <code>minimalSize</code>
     * is bigger than the actual remaining size of the buffer, the buffer is written
     * to the flushChannel.
     * Returns either the number of bytes actually available in the buffer or the <code>checkSize</code>
     * whichever is smaller.
     * 
     * @param checkSize the checked number of bytes that should be available in the buffer
     * @param minimalSize the minimal number of bytes that must be available in the buffer
     * @return either the number of bytes actually available or the <code>checkSize</code> whichever is smaller
     * @throws IOException if there was an error using flushChannel
     */
    @Override
    protected int checkBufferSize(int checkSize, int minimalSize) throws IOException {
        if (minimalSize > byteBuffer.remaining())
            writeChunk(minimalSize);

        // Return either the required size, if it is smaller than the buffer, or the buffer size
        return Math.min(byteBuffer.remaining(), checkSize);
    }

    /**
     * Checks if there is enough space in the buffer. If the <code>minimalSize</code>
     * is bigger than the actual remaining size of the buffer, the buffer is written
     * to the flushChannel.
     * 
     * @param minimalSize the minimal number of bytes that must be available in the buffer
     * @throws IOException if there was an error using flushChannel
     */
    @Override
    protected void ensureBufferSize(int minimalSize) throws IOException {
        if (minimalSize > byteBuffer.remaining())
            writeChunk(minimalSize);
    }

    /**
     * Write current chunk of data toe the writeChannel. It is guaranteed
     * that at least <code>minBytes</code> is written or an exception is thrown.
     * 
     * @param minBytes the minimal number of bytes that must be freed from the buffer
     * @throws IOException if there was an error using flushChannel
     */
    protected void writeChunk(int minBytes) throws IOException {
        try {
            byteBuffer.flip();
            flushChannelPosition += writeChannelData();
        } finally {
            byteBuffer.compact();
        }

        // Limit the buffer to the maximal size
        if (flushChannelMaximalPosition - flushChannelPosition < byteBuffer.remaining())
            byteBuffer.limit((int)(flushChannelMaximalPosition - flushChannelPosition - byteBuffer.position()));

        // Check the remaining minimal size 
        if (byteBuffer.remaining() < minBytes)
            throw new IOException("Cannot allocate " + minBytes + " bytes - buffer too small");
    }

    /** 
     * Write current buffered data to the flush channel.
     * @return the number of bytes written
     * @throws IOException if there was an error using flushChannel
     */
    protected int writeChannelData() throws IOException {
        return flushChannel.write(byteBuffer);
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
