/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.objects.util;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import messif.objects.impl.ObjectSignatureSQFD;
import messif.objects.nio.FileChannelInputStream;

/**
 *
 * @author xnovak8
 */
public class UKSignaturesReader implements Iterator<ObjectSignatureSQFD> {

    /** Default size of the reading buffer */
    protected static final int DEFAULT_BUFFER_SIZE = 64*1024;
    
    //****************** Attributes ******************//

    /** An input stream for reading objects of this iterator from */
    protected FileChannel channel;
    /** Remembered name of opened file to provide reset capability */
    protected String fileName;

    /** Header and directory reader */
    protected FileChannelInputStream headerReader;
    /** Data reader */
    protected FileChannelInputStream dataReader;
    
    
    public UKSignaturesReader(String fileName) {
        try {
            this.fileName = fileName;
            this.channel = FileChannel.open(new File(fileName).toPath(), StandardOpenOption.READ);
            headerReader = new FileChannelInputStream(DEFAULT_BUFFER_SIZE, false, channel, 0L, Long.MAX_VALUE);
            headerReader.order(ByteOrder.LITTLE_ENDIAN);
            
            readHeader(headerReader);
            
            long firstOffset = HEADER_SIZE_BYTES + mSignatures * 8;
            dataReader = new FileChannelInputStream(DEFAULT_BUFFER_SIZE, false, channel, firstOffset, Long.MAX_VALUE);
            dataReader.order(ByteOrder.LITTLE_ENDIAN);
        } catch (IOException ex) {
            Logger.getLogger(UKSignaturesReader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //   ***************      The signatures setting      ************************* //
    
    public static int HEADER_SIZE_BYTES = 16;
    public static int MAGIC_NUMBER = 0x1beddeed;
    private int mDim;
    private int mTypeLength;
    private int mSignatures;
    private int mMinSigLen;
    private int mMaxSigLen;
    
    /**
     * Structure of the head should be:
     *   std::uint32_t    mMagic;            ///< Magic value for verification (must be equal to 0x1beddeed).
     *   std::uint16_t    mDim;            ///< Centroid coordinates dimension.
     *   std::uint16_t    mTypeLength;    ///< Size of NUM_TYPE (in bytes).
     *   std::uint32_t    mSignatures;    ///< Number of signatures in database.
     *   std::uint16_t    mMinSigLen;        ///< Length of the smallest signature in db.
     *   std::uint16_t    mMaxSigLen;        ///< Length of the largest signature in db     * @param header 
     */
    protected final void readHeader(FileChannelInputStream header) throws IOException {
        ByteBuffer buffer = header.readInput(16);
        if (buffer.getInt() != MAGIC_NUMBER) {
            throw new IOException("The first 4 bytes (Little endian) are expected to be a magic number '" + MAGIC_NUMBER + "'");
        }
        mDim = buffer.getShort();
        mTypeLength = buffer.getShort();
        mSignatures = buffer.getInt();
        mMinSigLen = buffer.getShort();
        mMaxSigLen = buffer.getShort();
    }

    
    /** Instance of a next object. This is needed for implementing reading objects from a stream */
    protected ObjectSignatureSQFD nextObject;
    /** Number of objects read from the stream */
    protected int objectsRead;
    /** Offset of the previously read object */
    protected long previousOffset = 0L;
    
    protected ObjectSignatureSQFD readNextObject() throws IOException {
        long nextObjectOffset = headerReader.readInput(8).getLong();
        int sizeOfNextSignature = (int) (nextObjectOffset - previousOffset);
        previousOffset = nextObjectOffset;
        
        int dataSize = (mDim + 1) * sizeOfNextSignature;
        ByteBuffer dataBuffer = dataReader.readInput(dataSize * mTypeLength);

        float [] nextData = new float [dataSize];        
        for (int i = 0; i < dataSize; i++) {
            nextData [i] = dataBuffer.getFloat();
        }
        return new ObjectSignatureSQFD(sizeOfNextSignature, mDim, nextData);
    }
    
    public boolean hasNext() {
        if (nextObject != null) {
            return true;
        }
        if (objectsRead >= mSignatures) {
            return false;
        }
        try {
            nextObject = readNextObject();
            objectsRead ++;
            return true;
        } catch (IOException ex) {
            Logger.getLogger(UKSignaturesReader.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public ObjectSignatureSQFD next() {
        try {
            return nextObject;
        } finally {
            nextObject = null;
        }
    }

    public void remove() {
        throw new UnsupportedOperationException("The " + UKSignaturesReader.class.getName() + " is read-only"); 
    }
    

}
