/*
 *  BinaryInput
 * 
 */

package messif.objects.nio;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * Buffered binary input stream.
 * 
 * <p>
 * Note that it is <em>not safe</em> to use several {@link ByteBufferInputStream ByteBufferInputStreams} over the
 * same channel (even if synchronized). For file channels, the {@link ByteBufferFileInputStream}
 * can be used if you need this functionality. Use copy-pipes if you need it
 * on other channel types.
 * </p>
 * 
 * <p>
 * If multiple threads use the same instance of this class, the access to the
 * instance must be synchronized.
 * </p>
 * 
 * @see ByteBufferOutputStream
 * @author xbatko
 */
public class ChannelInputStream extends InputStream implements BinaryInput {

    //****************** Constants ******************//

    /** Minimal buffer size in bytes */
    private final int MINIMAL_BUFFER_SIZE = 32;

    /** Time to wait for additional data (in miliseconds) */
    private final long WAIT_DATA_TIME = 100;


    //****************** Attributes ******************//

    /** The buffer where data is stored */
    final ByteBuffer byteBuffer;

    /** The channel used to read data */
    private final ReadableByteChannel readChannel;


    //****************** Constructor ******************//

    /**
     * Creates a new instance of BinaryInput.
     * @param readChannel the channel used to read data
     * @param bufferSize the size of the internal buffer used for flushing
     * @param bufferDirect allocate the internal buffer as {@link java.nio.ByteBuffer#allocateDirect direct}
     */
    public ChannelInputStream(ReadableByteChannel readChannel, int bufferSize, boolean bufferDirect) {
        this.readChannel = readChannel;

        if (bufferSize < MINIMAL_BUFFER_SIZE)
            throw new IllegalArgumentException("Buffer must be at least " + MINIMAL_BUFFER_SIZE + " bytes long");
        if (bufferDirect)
            this.byteBuffer = ByteBuffer.allocateDirect(bufferSize);
        else
            this.byteBuffer = ByteBuffer.allocate(bufferSize);

        // The buffer is empty first, first attempt to read will fill it
        byteBuffer.limit(0);
    }


    //****************** Implementation of InputStream ******************//

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
        try {
            return readInput(1).get();
        } catch (EOFException ignore) {
            return -1;
        }
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
        try {
            while (len > 0) {
                ByteBuffer buffer = readInput(1);
                int bufLen = Math.min(len, buffer.remaining());
                buffer.get(buf, off, bufLen);
                len -= bufLen;
                off += bufLen;
                totalRead += bufLen;
            }
        } catch (EOFException ignore) {
            if (totalRead == 0)
                return -1;
            else
                return totalRead;
        }

        return totalRead;
    }

    @Override
    public int available() throws IOException {
        return byteBuffer.remaining();
    }


    //****************** Implementation of BinaryInput ******************//

    /**
     * Returns a buffer that contains at least <code>minimalSize</code> bytes.
     * If the current buffered data is not big enough, the buffer is compacted
     * and aditional data is read from the readChannel. Note that the returned
     * buffer can contain more that <code>minBytes</code> data.
     * 
     * @param minBytes the minimal number of bytes that must be available for reading in the buffer
     * @return the buffer with prepared data
     * @throws IOException if there was an error reading additional data or the requested <code>minBytes</code> is too big
     */
    public ByteBuffer readInput(int minBytes) throws IOException {
        // There is enough data remaining in the buffer
        if (minBytes <= byteBuffer.remaining())
            return byteBuffer;

        // Requested minimal size is too big
        if (byteBuffer.remaining() + minBytes > byteBuffer.capacity())
            throw new IOException("Buffer is too small to accomodate " + minBytes + " additional bytes");

        try {
            // Switch buffer to reading from stream
            byteBuffer.compact();

            do {
                // Read next chunk of data
                minBytes -= readChannelData(byteBuffer);
            } while (minBytes > 0); // Until enough data is read (this is usually only one run)
        } finally {
            // Switch buffer to providing data
            byteBuffer.flip();
        }
            
        return byteBuffer;
    }

    /** 
     * Reads next chunk of data into {@link #byteBuffer internal buffer}.
     * This method blocks until at least one byte is read or,
     * if there are no more data, {@link EOFException} is thrown.
     * 
     * @param buffer the buffer into which to read additional data
     * @return the number of bytes read (always bigger than zero)
     * @throws EOFException if there are no more data available
     * @throws IOException if there was an error reading data
     */
    protected int readChannelData(ByteBuffer buffer) throws EOFException, IOException {
        int readBytes = readChannel.read(buffer);

        // If there are no data, wait a little while and retry
        while (readBytes == 0) {
            try {
                Thread.sleep(WAIT_DATA_TIME);
                readBytes = readChannelData(buffer);
            } catch (InterruptedException e) {
                throw new InterruptedIOException(e.getMessage());
            }
        }

        if (readBytes == -1)
            throw new EOFException("Cannot read more bytes - end of file encountered");

        return readBytes;
    }

}
