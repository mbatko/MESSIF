/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.objects.util;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.FileChannelInputStream;

/**
 *
 * @author xnovak8
 */
public class UKSignaturesReader<E extends LocalAbstractObject> implements Iterator<E> {

    /** Default size of the reading buffer */
    protected static final int DEFAULT_BUFFER_SIZE = 16*1024;
    
    //****************** Attributes ******************//

    /** An input stream for reading objects of this iterator from */
    protected FileChannel channel;
    /** Remembered name of opened file to provide reset capability */
    protected String fileName;
    /** Instance of a next object. This is needed for implementing reading objects from a stream */
    protected E nextObject;
    /** Number of objects read from the stream */
    protected int objectsRead;

    /** Header and directory reader */
    protected FileChannelInputStream headerReader;
    
    
    public UKSignaturesReader(String fileName) {
        try {
            this.fileName = fileName;
            this.channel = FileChannel.open(new File(fileName).toPath(), StandardOpenOption.READ);
            headerReader = new FileChannelInputStream(DEFAULT_BUFFER_SIZE, false, channel, 0L, Long.MAX_VALUE);
            headerReader.order(ByteOrder.LITTLE_ENDIAN);
            
            readHeader(headerReader);
        } catch (IOException ex) {
            Logger.getLogger(UKSignaturesReader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //   ***************      The signatures setting      ************************* //
    
    private int mDim;
    
    
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
        if (buffer.getInt() != 0x1beddeed) {
            throw new IOException("The first 4 bytes (Little endian) are expected to be a magic number '0x1beddeed'");
        }
        mDim = buffer.getShort();
        System.out.println("dimensionality is " + mDim);
    }
    
    
    public boolean hasNext() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public E next() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void remove() {
        throw new UnsupportedOperationException("The " + UKSignaturesReader.class.getName() + " is read-only"); 
    }
    

}
