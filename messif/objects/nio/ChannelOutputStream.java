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
import java.nio.channels.WritableByteChannel;

/**
 * Buffered output stream for operating over channels.
 * 
 * <p>
 * Note that it is <em>not safe</em> to use several {@link ChannelOutputStream ChannelOutputStreams} over the
 * same channel (even if synchronized). For file channels, the {@link FileChannelOutputStream}
 * can be used if you need this functionality. Use copy-pipes if you need it
 * on other channel types.
 * </p>
 * 
 * <p>
 * If multiple threads use the same instance of this class, the access to the
 * instance must be synchronized.
 * </p>
 * 
 * @see ChannelInputStream
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ChannelOutputStream extends BufferOutputStream implements BinaryOutput {

    //****************** Attributes ******************//

    /** The channel used to write data */
    private final WritableByteChannel writeChannel;


    //****************** Constructor ******************//

    /**
     * Creates a new instance of ChannelOutputStream.
     * @param bufferSize the size of the internal buffer used for flushing
     * @param bufferDirect allocate the internal buffer as {@link ByteBuffer#allocateDirect direct}
     * @param writeChannel the channel into which to write data
     * @throws IOException if there was an error using writeChannel
     */
    public ChannelOutputStream(int bufferSize, boolean bufferDirect, WritableByteChannel writeChannel) throws IOException {
        super(bufferSize, bufferDirect);
        this.writeChannel = writeChannel;
    }

    /** 
     * Writes the buffered data to the write channel.
     * 
     * @param buffer the buffer from which to write data
     * @throws IOException if there was an error writing the data
     */
    @Override
    protected void write(ByteBuffer buffer) throws IOException {
        writeChannel.write(buffer);
    }

}
