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
import java.nio.channels.ReadableByteChannel;

/**
 * Buffered binary input stream.
 * 
 * <p>
 * Note that it is <em>not safe</em> to use several {@link ChannelInputStream ChannelInputStreams} over the
 * same channel (even if synchronized). For file channels, the {@link FileChannelInputStream}
 * can be used if you need this functionality. Use copy-pipes if you need it
 * on other channel types.
 * </p>
 * 
 * <p>
 * If multiple threads use the same instance of this class, the access to the
 * instance must be synchronized.
 * </p>
 * 
 * @see ChannelOutputStream
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ChannelInputStream extends BufferInputStream implements BinaryInput {

    //****************** Constants ******************//

    /** Time to wait for additional data (in miliseconds) */
    private static final long WAIT_DATA_TIME = 100;


    //****************** Attributes ******************//

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
        super(bufferSize, bufferDirect);
        this.readChannel = readChannel;
    }


    //****************** Overriden methods ******************//

    @Override
    protected void read(ByteBuffer buffer) throws EOFException, IOException {
        int readBytes = readChannel.read(buffer);

        // If there are no data, wait a little while and retry
        while (readBytes == 0) {
            try {
                Thread.sleep(WAIT_DATA_TIME);
                readBytes = readChannel.read(buffer);
            } catch (InterruptedException e) {
                throw new InterruptedIOException(e.getMessage());
            }
        }

        if (readBytes == -1)
            throw new EOFException("Cannot read more bytes - end of file encountered");
    }

}
