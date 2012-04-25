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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Extending class for {@link ChannelOutputStream} operating over a
 * file. The position is restored before every write operation, so it is safe
 * to use multiple instances of this class over the same file channel. However,
 * if multiple threads use the same instance of this class, the access to the
 * instance must be synchronized.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class FileChannelOutputStream extends ChannelOutputStream {

    /** The file to which to write data */
    protected final FileChannel fileChannel;

    /** The current position in the file */
    private long position;

    /** Starting position of the file */
    private final long startPosition;

    /** The maximal position that can be accessed in the file */
    private final long endPosition;

    /**
     * Creates a new instance of FileChannelOutputStream.
     * @param bufferSize the size of the internal buffer used for flushing
     * @param bufferDirect allocate the internal buffer as {@link java.nio.ByteBuffer#allocateDirect direct}
     * @param fileChannel the file channel into which to write data
     * @param position the starting position of the file
     * @param maxLength the maximal length of data
     * @throws IOException if there was an error using the file channel
     */
    public FileChannelOutputStream(int bufferSize, boolean bufferDirect, FileChannel fileChannel, long position, long maxLength) throws IOException {
        super(bufferSize, bufferDirect, fileChannel);
        this.fileChannel = fileChannel;
        this.startPosition = position;
        this.position = position;
        this.endPosition = position + maxLength;
        setBufferedSizeLimit(maxLength);
    }

    /**
     * Returns the current position in the file.
     * @return the current position in the file
     */
    public long getPosition() {
        return position + bufferedSize();
    }

    /**
     * Set the position at which the data will be written.
     * @param position the new position
     * @throws IOException if the specified position is outside the boundaries
     */
    public void setPosition(long position) throws IOException {
        // Check position validity
        if (position < startPosition || position > endPosition)
            throw new IOException("Position " + position + " is outside the allowed range");

        // Flush the data and set new position
        flush();
        this.position = position;
        setBufferedSizeLimit(endPosition - this.position);
    }

    /**
     * Writes the buffered data to the file channel.
     * The writing is done at the correct position regardless of the underlying file's actual position.
     * 
     * @param buffer the buffer from which to write data
     * @throws IOException if there was an error writing the data
     */
    @Override
    protected void write(ByteBuffer buffer) throws IOException {
        try {
            buffer.flip();
            if (position + buffer.remaining() > endPosition)
                throw new InternalError("Buffered data exceeds the end position");
            position += fileChannel.write(buffer, position);
        } finally {
            buffer.compact();
        }
        setBufferedSizeLimit(endPosition - position);
    }

}
