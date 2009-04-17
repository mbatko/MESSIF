/*
 * DiskBlockBucket.java
 *
 * Created on 12. kveten 2008, 16:49
 */

package messif.buckets.impl;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import messif.objects.LocalAbstractObject;
import messif.buckets.BucketDispatcher;
import messif.buckets.LocalBucket;
import messif.buckets.index.ModifiableIndex;
import messif.buckets.storage.impl.DiskStorage;
import messif.objects.nio.BinarySerializator;
import messif.objects.nio.MultiClassSerializator;

/**
 * A disk-oriented implementation of {@link LocalBucket}.
 * It stores all objects in a specified blocks of a file.
 *
 * The storage is persistent, even if the process using this bucket
 * quits, the bucket can be opened afterwards.
 * Note that this bucket only saves the name of the file when serialized,
 * thus the file must exist when the bucket is deserialized.
 *
 * @author  xbatko
 * @see BucketDispatcher
 * @see LocalBucket
 * @see DiskBlockObjectKeyBucket
 */
public class DiskBlockBucket extends LocalBucket implements Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /** The prefix for auto-generated filenames */
    protected static final String FILENAME_PREFIX = "disk_block_bucket_";

    /** The suffix for auto-generated filenames */
    protected static final String FILENAME_SUFFIX = ".dbb";


    //****************** Data storage ******************//

    /** Object storage */
    protected final ModifiableIndex<LocalAbstractObject> objects;


    /****************** Constructors ******************/

    /**
     * Constructs a new DiskBlockBucket instance with 16k direct-buffered input.
     * A generic {@link MultiClassSerializator} is used to serialize objects and
     * the occupation is counted in bytes.
     * 
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param file the file where the bucket will be stored
     * @throws IOException if there was a problem opening or creating the bucket file
     */
    public DiskBlockBucket(long capacity, long softCapacity, long lowOccupation, File file) throws IOException {
        this(capacity, softCapacity, lowOccupation, file, 16*1024, true, false, new MultiClassSerializator<LocalAbstractObject>(LocalAbstractObject.class));
    }

    /**
     * Constructs a new DiskBlockBucket instance.
     * Occupation is counted in bytes.
     * 
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param file the file where the bucket will be stored
     * @param bufferSize the size of the buffer used for I/O operations
     * @param directBuffers flag whether to use the {@link java.nio.ByteBuffer#allocateDirect(int) direct buffers}
     * @param memoryMap flag whether to use memory-mapped I/O
     * @param serializator the {@link BinarySerializator binary serializator} used to store objects
     * @throws IOException if there was a problem opening or creating the bucket file
     */
    public DiskBlockBucket(long capacity, long softCapacity, long lowOccupation, File file, int bufferSize, boolean directBuffers, boolean memoryMap, BinarySerializator serializator) throws IOException {
        this(capacity, softCapacity, lowOccupation, true,  new DiskStorage<LocalAbstractObject>(
                LocalAbstractObject.class, file, false, bufferSize, directBuffers, memoryMap, 0, capacity,
                serializator
        ));
    }

    /**
     * Constructs a new DiskBlockBucket instance.
     * This constructor is intended to be used from the factory method.
     * 
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     * @param diskStorage the object storage for this bucket
     */
    private DiskBlockBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes, DiskStorage<LocalAbstractObject> diskStorage) {
        super(capacity, softCapacity, lowOccupation, occupationAsBytes);
        objects = diskStorage;
    }

    @Override
    public void finalize() throws Throwable {
        objects.destroy();
        super.finalize();
    }


    //****************** Factory method ******************//
    
    /**
     * Creates a bucket. For the description of additional parameters that
     * can be specified in the parameters map see {@link DiskStorage#create}.
     * 
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     * @param parameters list of named parameters (see above)
     * @return a new SimpleDiskBucket instance
     * @throws IOException if something goes wrong when working with the filesystem
     * @throws InstantiationException if the parameters specified are invalid (non existent directory, null values, etc.)
     * @throws ClassNotFoundException if the parameter <em>class</em> could not be resolved or is not a descendant of LocalAbstractObject
     */
    public static DiskBlockBucket getBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes, Map<String, Object> parameters) throws IOException, InstantiationException, ClassNotFoundException {
        return new DiskBlockBucket(capacity, softCapacity, lowOccupation, occupationAsBytes, DiskStorage.create(LocalAbstractObject.class, parameters));
    }


    //****************** Overrides ******************//

    @Override
    protected ModifiableIndex<LocalAbstractObject> getModifiableIndex() {
        return objects;
    }
}
