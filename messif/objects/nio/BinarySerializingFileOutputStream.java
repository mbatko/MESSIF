/*
 * BinarySerializingFileInputStream
 *
 */

package messif.objects.nio;

import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Extending class for {@link BinarySerializingOutputStream} operating over a
 * file. The position is restored before every write operation, so it is safe
 * to use multiple instances of this class over the same file channel. However,
 * if multiple threads use the same instance of this class, the access to the
 * instance must be synchronized.
 * 
 * @author xbatko
 */
public class BinarySerializingFileOutputStream extends BinarySerializingOutputStream {

    /** The file to which to write data */
    protected final FileChannel flushChannelFile;

    /** Starting position of the file */
    protected final long flushChannelStartPosition;

    /**
     * Creates a new instance of BinarySerializingFileOutputStream.
     * @param bufferSize the size of the internal buffer used for flushing
     * @param bufferDirect allocate the internal buffer as {@link ByteBuffer#allocateDirect direct}
     * @param flushChannel the channel into which to write data
     * @param position the starting position of the file
     * @param maxLength the maximal length of data
     * @throws IOException if there was an error using flushChannel
     */
    public BinarySerializingFileOutputStream(int bufferSize, boolean bufferDirect, FileChannel flushChannel, long position, long maxLength) throws IOException {
        super(bufferSize, bufferDirect, flushChannel, maxLength);
        this.flushChannelFile = flushChannel;
        this.flushChannelStartPosition = position;
    }

    /** 
     * Write current buffered data to the flush channel.
     * The writing is done at the correct position regardless of the underlying file's actual position.
     * @return the number of bytes written
     * @throws IOException if there was an error using flushChannel
     */
    @Override
    protected int writeChannelData() throws IOException {
        return flushChannelFile.write(byteBuffer, flushChannelStartPosition + flushChannelPosition);
    }

    /**
     * Sets this stream's position.
     *
     * @param  position the the number of bytes from the beginning of the stream
     * @throws IOException if there was an error using flushChannel
     */
    public void position(long position) throws IOException {
        if (position < 0 || position > flushChannelMaximalPosition)
            throw new EOFException("Cannot set position beyond the maximal length");

        // Flush current buffer
        flush();

        // Set new position
        flushChannelPosition = position;
    }

    /**
     * Writes the <code>value</code> at stream <code>position</code>.
     * The current position of the stream is unchanged.
     * 
     * @param position the number of bytes from the beginning of the stream
     * @param value the value to write
     * @throws IOException if there was an error using flushChannel
     */
    public void writeIntAt(long position, int value) throws IOException {
        long rememberedPos = position();
        position(position);
        try {
            writeInt(value);
        } finally {
            position(rememberedPos);
        }
    }
}
