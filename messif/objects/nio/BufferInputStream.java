/*
 *  BinaryInput
 * 
 */

package messif.objects.nio;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

/**
 * Input stream that implements the {@link BinaryInput} using
 * an internal {@link ByteBuffer buffer}.
 * 
 * <p>
 * If multiple threads use the same instance of this class, the access to the
 * instance must be synchronized.
 * </p>
 * 
 * @see BufferOutputStream
 * @author xbatko
 */
public class BufferInputStream extends InputStream implements BinaryInput {

    //****************** Constants ******************//

    /** Minimal buffer size in bytes */
    private final int MINIMAL_BUFFER_SIZE = 32;


    //****************** Attributes ******************//

    /** The buffer where data is stored */
    final ByteBuffer byteBuffer;


    //****************** Constructor ******************//

    /**
     * Creates a new instance of BufferInputStream.
     * The input operates on a newly created buffer with the specified size.
     * @param bufferSize the size of the internal buffer
     * @param bufferDirect allocate the internal buffer as {@link ByteBuffer#allocateDirect direct}
     * @throws IllegalArgumentException if there specified buffer size is not valid
     */
    public BufferInputStream(int bufferSize, boolean bufferDirect) throws IllegalArgumentException {
        if (bufferSize < MINIMAL_BUFFER_SIZE)
            throw new IllegalArgumentException("Buffer must be at least " + MINIMAL_BUFFER_SIZE + " bytes long");
        if (bufferDirect)
            this.byteBuffer = ByteBuffer.allocateDirect(bufferSize);
        else
            this.byteBuffer = ByteBuffer.allocate(bufferSize);

        // The buffer is empty first, first attempt to read will fill it
        byteBuffer.limit(0);
    }

    /**
     * Creates a new instance of BufferInputStream.
     * The input operates on the given buffer.
     * @param buffer the internal buffer for this stream
     */
    public BufferInputStream(ByteBuffer buffer) {
        this.byteBuffer = buffer;
    }

    /**
     * Creates a new instance of BufferInputStream.
     * The input operates on a buffer that {@link ByteBuffer#wrap(byte[]) wraps}
     * the given data array.
     * @param array the data array for this stream
     */
    public BufferInputStream(byte[] array) {
        this(ByteBuffer.wrap(array));
    }


    //****************** Buffer access methods ******************//

    /**
     * Returns the current position in this input stream.
     * @return the current position in this input stream
     */
    public long getPosition() {
        return byteBuffer.position();
    }

    /**
     * Sets the current position in this input stream.
     * @param position the new position in this input stream
     * @throws IOException if the position is invalid or there was another I/O error
     */
    public void setPosition(long position) throws IOException {
        if (position < 0 || position > byteBuffer.limit())
            throw new IOException("Position " + position + " is outside the allowed range");
        byteBuffer.position((int)position);
    }

    /**
     * Returns the number of bytes currently in the buffer.
     * @return the number of bytes currently in the buffer
     */
    public int bufferedSize() {
        return byteBuffer.limit();
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
    public long skip(long n) throws IOException {
        setPosition(getPosition() + n);
        return n;
    }

    @Override
    public int available() {
        return byteBuffer.remaining();
    }


    //****************** Implementation of BinaryInput ******************//

    public ByteBuffer readInput(int minBytes) throws IOException {
        // There is enough data remaining in the buffer
        if (minBytes <= byteBuffer.remaining())
            return byteBuffer;

        // Requested minimal size is too big
        if (byteBuffer.remaining() + minBytes > byteBuffer.capacity())
            throw new IOException("Buffer is too small to provide " + minBytes + " additional bytes");

        // Switch buffer to reading from stream
        byteBuffer.compact();
        try {
            do {
                // Read next chunk of data
                read(byteBuffer);
            } while (byteBuffer.position() < minBytes); // Until enough data is read (this is usually only one run)
        } finally {
            // Switch buffer to providing data
            byteBuffer.flip();
        }

        return byteBuffer;
    }

    /** 
     * Reads some data into the <code>buffer</code>.
     * This method blocks until at least one byte is read or,
     * if there are no more data, {@link EOFException} is thrown.
     * 
     * @param buffer the buffer into which to read additional data
     * @throws EOFException if there are no more data available
     * @throws IOException if there was an error reading data
     */
    protected void read(ByteBuffer buffer) throws EOFException, IOException {
        throw new EOFException("Cannot read more bytes - end of buffer reached");
    }

    /**
     * Reads data from the specified channel.
     * The {@link ReadableByteChannel#read(java.nio.ByteBuffer) read}
     * method is called on the channel.
     * 
     * @param channel the readable channel from which the data is read
     * @return number of bytes actually read
     * @throws IOException if there was an I/O error reading from the channel
     */
    public int read(ReadableByteChannel channel) throws IOException {
        byteBuffer.compact();
        try {
            return channel.read(byteBuffer);
        } finally {
            byteBuffer.flip();
        }
    }

    /**
     * Reads data from the specified file channel.
     * The {@link FileChannel#read(java.nio.ByteBuffer, long) read}
     * method is called on the channel.
     * 
     * @param channel the file channel from which the data is read
     * @param position the position in the file where to read the data
     * @return number of bytes actually read
     * @throws IOException if there was an I/O error reading from the channel
     */
    public int read(FileChannel channel, long position) throws IOException {
        byteBuffer.compact();
        try {
            return channel.read(byteBuffer, position);
        } finally {
            byteBuffer.flip();
        }
    }

}
