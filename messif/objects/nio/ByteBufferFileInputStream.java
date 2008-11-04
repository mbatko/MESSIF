/*
 * ByteBufferFileInputStream
 *
 */

package messif.objects.nio;

import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Extending class for {@link ByteBufferInputStream} operating over a
 * file. The position is restored before every read operation, so it is safe
 * to use multiple instances of this class over the same file channel. However,
 * if multiple threads use the same instance of this class, the access to the
 * instance must be synchronized.
 * 
 * @author xbatko
 */
public class ByteBufferFileInputStream extends ByteBufferInputStream {

    /** The file from which to read data */
    protected final FileChannel readChannelFile;

    /** Starting position of the file */
    protected final long readChannelStartPosition;

    /**
     * Creates a new instance of ByteBufferFileInputStream.
     * @param bufferSize the size of the internal buffer used for flushing
     * @param bufferDirect allocate the internal buffer as {@link java.nio.ByteBuffer#allocateDirect direct}
     * @param readChannel the channel from which to read data
     * @param position the starting position of the file
     * @param maxLength the maximal length of data
     * @throws IOException if there was an error using readChannel
     */
    public ByteBufferFileInputStream(int bufferSize, boolean bufferDirect, FileChannel readChannel, long position, long maxLength) throws IOException {
        super(bufferSize, bufferDirect, readChannel, maxLength);
        this.readChannelFile = readChannel;
        this.readChannelStartPosition = position;
    }

    /**
     * Skips over and discards <code>n</code> bytes of data from this input
     * stream. The <code>skip</code> method may, for a variety of reasons, end
     * up skipping over some smaller number of bytes, possibly <code>0</code>.
     * This may result from any of a number of conditions; reaching end of file
     * before <code>n</code> bytes have been skipped is only one possibility.
     * The actual number of bytes skipped is returned. If <code>n</code> is
     * negative, no bytes are skipped.
     *
     * @param n the number of bytes to be skipped
     * @return the actual number of bytes skipped
     * @throws IOException if there was an error using readChannel
     */
    @Override
    public long skip(long n) throws IOException {
        // Skiping only in buffer
        if (byteBuffer.remaining() >= n) {
            byteBuffer.position(byteBuffer.position() + (int)n);
            return n;
        }

        // Skiping bigger than buffer, must skip in file
        long currentRelativePos = position();

        // Truncate n if beyond maximal position
        if (currentRelativePos + n > readChannelMaximalPosition)
            n = readChannelMaximalPosition - currentRelativePos;

        // No skiping necessary
        if (n <= 0)
            return 0;

        // Update relative position of the file, the actual seek will be done before next read operation
        readChannelPosition = currentRelativePos + n;

        // Empty buffer
        byteBuffer.position(0);
        byteBuffer.limit(0);

        return n;
    }

    /**
     * Repositions this stream to the starting position.
     */
    @Override
    public void reset() {
        // Update relative position of the file, the actual seek will be done before next read operation
        readChannelPosition = 0;

        // Empty buffer
        byteBuffer.position(0);
        byteBuffer.limit(0);
    }

    /** 
     * Reads next chunk of data into {@link #byteBuffer internal buffer}.
     * The reading is done at the correct position regardless of the underlying file's actual position.
     * @return the number of bytes read
     * @throws IOException if there was an error using readChannel
     */
    @Override
    protected int readChannelData() throws IOException {
        return readChannelFile.read(byteBuffer, readChannelStartPosition + readChannelPosition);
    }


}
