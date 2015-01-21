/*
 *  This file is part of MESSIF library.
 *
 *  MESSIF library is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MESSIF library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MESSIF library.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.objects.nio;

import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NonReadableChannelException;
import java.util.concurrent.ExecutionException;

/**
 * Extending class for a {@link ChannelInputStream} that operates on a
 * file. The position is restored before every read operation, so it is safe
 * to use multiple instances of this class over the same file channel. However,
 * if multiple threads use the same instance of this class, the access to the
 * instance must be synchronized.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class AsynchronousFileChannelInputStream extends BufferInputStream {

    /** The file from which to read data */
    private final AsynchronousFileChannel fileChannel;

    /** The current position in the file */
    private long position;

    /** Starting position of the file */
    private final long startPosition;

    /** The maximal position that can be accessed in the file */
    private final long endPosition;

    /**
     * Creates a new instance of FileChannelInputStream.
     * @param bufferSize the size of the internal buffer used for flushing
     * @param bufferDirect allocate the internal buffer as {@link java.nio.ByteBuffer#allocateDirect direct}
     * @param fileChannel the file channel from which to read data
     * @param position the starting position of the file
     * @param maxLength the maximal length of data
     * @throws IOException if there was an error using readChannel
     */
    public AsynchronousFileChannelInputStream(int bufferSize, boolean bufferDirect, AsynchronousFileChannel fileChannel, long position, long maxLength) throws IOException {
        super(bufferSize, bufferDirect);
        this.fileChannel = fileChannel;
        this.startPosition = position;
        this.endPosition = maxLength == Long.MAX_VALUE ? Long.MAX_VALUE : position + maxLength; // Compensate for long overflow
        if (this.endPosition < this.startPosition)
            throw new IllegalArgumentException("End position (" + this.endPosition + ") cannot be smaller than start position (" + this.startPosition + ") - wrong maximal length or there was a long-int overflow");
        this.position = position;
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
        long newPosition = getPosition() + n;
        if (newPosition > endPosition) {
            n -= newPosition - endPosition;
            setPosition(endPosition);
        } else {
            setPosition(newPosition);
        }

        return n;
    }

    /**
     * Repositions this stream to the starting position.
     */
    @Override
    public void reset() throws IOException {
        setPosition(startPosition);
    }

    /**
     * Returns the current position in the file.
     * @return the current position in the file
     */
    @Override
    public long getPosition() {
        return position - available();
    }

    /**
     * Set the position from which the data will be read.
     * @param position the new position
     * @throws IOException if the specified position is outside the boundaries
     */
    @Override
    public void setPosition(long position) throws IOException {
        // Check relative position and current buffer position
        if (position < this.position - bufferedSize() || position > this.position) { // Outside buffer
            // Check position validity
            if (position < startPosition || position > endPosition)
                throw new IOException("Position " + position + " is outside the allowed range");

            // Set buffer's position to end, so that next readInput will need to read data
            super.setPosition(bufferedSize());

            this.position = position;
        } else { // Inside buffer
            super.setPosition(bufferedSize() - (int)(this.position - position));
        }
    }

    /** 
     * {@inheritDoc}
     * <p>
     * Data are accessed correctly regardless of the actual position in the fileChannel.
     * </p>
     * 
     * @param buffer the buffer into which to read additional data
     * @throws EOFException if there are no more data available
     * @throws IOException if there was an error reading data
     */
    @Override
    protected void read(ByteBuffer buffer) throws EOFException, IOException {
        if (position == endPosition)
            throw new EOFException("Cannot read more bytes - end of file encountered");
        // Check for the maximal position
        if (position + buffer.remaining() > endPosition)
            buffer.limit(buffer.position() + (int)(endPosition - position));

        try {
            int bytesRead = fileChannel.read(buffer, position).get();
            if (bytesRead == -1)
                throw new EOFException("Cannot read more bytes - end of file encountered");
            position += bytesRead;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IOException)
                throw (IOException)e.getCause();
            throw new IOException(e);
        } catch (InterruptedException e) {
            throw new InterruptedIOException(e.getMessage());
        }
    }

    /**
     * Interface for the handler method that processes asynchronous read completion.
     */
    public static interface AsynchronousReadCallback {
        /**
         * Invoked when the asynchronous read is completed.
         * @param input the input buffer that has completed the reading
         */
        public void completed(AsynchronousFileChannelInputStream input);
        /**
         * Invoked when the asynchronous read has failed.
         * @param input the input buffer that has failed the reading
         * @param exc the exception to indicate why the read has failed
         */
        public void failed(AsynchronousFileChannelInputStream input, Throwable exc);
    }

    /** Internal completion handler used for the asynchronous read */
    private final CompletionHandler<Integer, AsynchronousReadCallback> readAsyncCompletionHandler = new CompletionHandler<Integer, AsynchronousReadCallback>() {
        @Override
        public void completed(Integer bytesRead, AsynchronousReadCallback handler) {
            byteBuffer.flip();
            try {
                if (bytesRead == -1)
                    throw new EOFException("Cannot read more bytes - end of file encountered");
                position += bytesRead;
            } catch (IOException | IllegalArgumentException e) {
                handler.failed(AsynchronousFileChannelInputStream.this, e);
            }
            handler.completed(AsynchronousFileChannelInputStream.this);
        }
        @Override
        public void failed(Throwable exc, AsynchronousReadCallback handler) {
            byteBuffer.flip();
            handler.failed(AsynchronousFileChannelInputStream.this, exc);
        }
    };

    /**
     * Reads a sequence of bytes starting at the current {@link #getPosition() position}.
     * @param handler the callback handler that is called when the asynchronous read is completed
     * @throws IllegalArgumentException if the position is negative or the buffer is read-only
     * @throws NonReadableChannelException if this channel was not opened for reading
     */
    public void readAsynchronously(AsynchronousReadCallback handler) throws IllegalArgumentException, NonReadableChannelException {
        byteBuffer.compact(); // Note that the buffer flipping is done in the completion handler
        fileChannel.read(byteBuffer, position, handler, readAsyncCompletionHandler);
    }

}
