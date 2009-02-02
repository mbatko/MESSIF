/*
 * ChannelOutputStream
 * 
 */

package messif.objects.nio;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Buffered output stream for operating over channels.
 * 
 * <p>
 * Note that it is <em>not safe</em> to use several {@link ChannelOutputStream ChannelOutputStreams} over the
 * same channel (even if synchronized). For file channels, the {@link ByteBufferFileOutputStream}
 * can be used if you need this functionality. Use copy-pipes if you need it
 * on other channel types.
 * </p>
 * 
 * <p>
 * If multiple threads use the same instance of this class, the access to the
 * instance must be synchronized.
 * </p>
 * 
 * @see ByteBufferInputStream
 * @author xbatko
 */
public class ChannelOutputStream extends OutputStream implements BinaryOutput {

    //****************** Constants ******************//

    /** Minimal buffer size in bytes */
    private final int MINIMAL_BUFFER_SIZE = 32;


    //****************** Attributes ******************//

    /** The buffer where data is stored */
    protected final ByteBuffer byteBuffer;

    /** The channel used to write data */
    protected final WritableByteChannel writeChannel;


    //****************** Constructor ******************//

    /**
     * Creates a new instance of ChannelOutputStream.
     * @param bufferSize the size of the internal buffer used for flushing
     * @param bufferDirect allocate the internal buffer as {@link ByteBuffer#allocateDirect direct}
     * @param writeChannel the channel into which to write data
     * @throws IOException if there was an error using writeChannel
     */
    public ChannelOutputStream(int bufferSize, boolean bufferDirect, WritableByteChannel writeChannel) throws IOException {
        this.writeChannel = writeChannel;
        if (bufferSize < MINIMAL_BUFFER_SIZE)
            throw new IllegalArgumentException("Buffer must be at least " + MINIMAL_BUFFER_SIZE + " bytes long");
        if (bufferDirect)
            this.byteBuffer = ByteBuffer.allocateDirect(bufferSize);
        else
            this.byteBuffer = ByteBuffer.allocate(bufferSize);
    }

    /**
     * Writes the specified byte to this output stream. 
     * @param b the byte to be written
     * @throws IOException if there was an error using writeChannel
     */
    public void write(int b) throws IOException {
        prepareOutput(1).put((byte)b);
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array 
     * starting at offset <code>off</code> to this output stream.
     *
     * @param bytes the data
     * @param off the start offset in the data
     * @param len the number of bytes to write
     * @throws IOException if there was an error using writeChannel
     */
    @Override
    public void write(byte bytes[], int off, int len) throws IOException {
        while (len > 0) {
            ByteBuffer buffer = prepareOutput(1);
            int lenToWrite = Math.min(len, buffer.remaining());
            buffer.put(bytes, off, lenToWrite);
            off += lenToWrite;
            len -= lenToWrite;
        }
    }

    /**
     * Flushes this output stream and forces any buffered output bytes 
     * to be written out to flush channel.
     * 
     * @throws IOException if there was an error using writeChannel
     */
    @Override
    public void flush() throws IOException {
        // Must empty the whole buffer, so require the capacity of the buffer
        prepareOutput(byteBuffer.capacity());
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
     * @throws IOException if there was an error using writeChannel
     */
    @Override
    public void close() throws IOException {
        flush();
    }

    /**
     * Write current chunk of data toe the writeChannel. It is guaranteed
     * that at least <code>minBytes</code> is written or an exception is thrown.
     * 
     * @param minBytes the minimal number of bytes that must be freed from the buffer
     * @throws IOException if there was an error using writeChannel
     */
    public ByteBuffer prepareOutput(int minBytes) throws IOException {
        // Compute the number of bytes to free
        minBytes -= byteBuffer.remaining();
        if (minBytes <= 0) // There is enough space in the buffer, do nothing
            return byteBuffer;

        // Requested minimal size is too big
        if (minBytes > byteBuffer.position())
            throw new IOException("Buffer is too small to free additional " + minBytes + " bytes");


        // There is not enough space, flush data to free some more
        try {
            byteBuffer.flip();
            do {
                // Write data in the buffer to output
                minBytes -= writeChannelData(byteBuffer);
            } while (minBytes > 0); // Until enough data is written (this is usually only one run)
        } finally {
            byteBuffer.compact();
        }

        return byteBuffer;
    }

    /** 
     * Write buffered data to the channel.
     * @param buffer the buffer from which to write data
     * @return the number of bytes written
     * @throws IOException if there was an error using writeChannel
     */
    protected int writeChannelData(ByteBuffer buffer) throws IOException {
        return writeChannel.write(buffer);
    }

}
