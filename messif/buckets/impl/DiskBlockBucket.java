/*
 * DiskBlockBucket.java
 *
 * Created on 12. kveten 2008, 16:49
 */

package messif.buckets.impl;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import messif.objects.LocalAbstractObject;
import messif.buckets.BucketDispatcher;
import messif.buckets.LocalBucket;
import messif.buckets.index.ModifiableIndex;
import messif.buckets.storage.impl.IndexedDiskStorage;
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
 * @see DiskBucket
 * @see SimpleDiskBucket
 */
public class DiskBlockBucket extends LocalBucket implements Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Data storage ******************//

    /** Object storage */
    protected final IndexedDiskStorage<LocalAbstractObject> objects;


    /****************** Constructors ******************/

    /**
     * Constructs a new DiskBlockBucket instance.
     * Occupation is always counted in bytes for DiskBlockBucket.
     * 
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     */
    public DiskBlockBucket(long capacity, long softCapacity, long lowOccupation, File file, int bufferSize, boolean directBuffers, BinarySerializator serializator) throws IOException {
        super(capacity, softCapacity, lowOccupation, true);
        objects = new IndexedDiskStorage<LocalAbstractObject>(
                LocalAbstractObject.class, file, bufferSize, directBuffers, 0, capacity,
                serializator
        );
    }

    public DiskBlockBucket(long capacity, long softCapacity, long lowOccupation, File file) throws IOException {
        this(capacity, softCapacity, lowOccupation, file, 16*1024, true, new MultiClassSerializator<LocalAbstractObject>(LocalAbstractObject.class));
    }

    @Override
    public void finalize() throws Throwable {
        objects.finalize();
        super.finalize();
    }


    //****************** Overrides ******************//

    @Override
    protected ModifiableIndex<LocalAbstractObject> getModifiableIndex() {
        return objects;
    }
}
