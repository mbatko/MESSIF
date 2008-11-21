/*
 * SimpleDiskBucket.java
 *
 * Created on 19. prosinec 2007, 12:17
 */

package messif.buckets.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.WeakHashMap;
import messif.objects.LocalAbstractObject;
import java.io.Serializable;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import messif.buckets.BucketDispatcher;
import messif.buckets.BucketErrorCode;
import messif.buckets.LocalBucket;
import messif.buckets.LocalFilteredBucket;
import messif.buckets.OccupationLowException;
import messif.utility.Logger;

/**
 * A disk-oriented implementation of {@link LocalBucket}.
 * It stores all objects in a sequential file.
 *
 * Note that this class only uses disk to save memory, all data is lost
 * when {@link SimpleDiskBucket} is finalized. If a persistent bucket is needed,
 * use {@link DiskBucket} or {@link DiskBlockBucket} instead.
 *
 * @author  xbatko
 * @see BucketDispatcher
 * @see LocalBucket
 * @see DiskBucket
 * @see DiskBlockBucket
 */
public class SimpleDiskBucket extends LocalFilteredBucket implements Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /** Logger for this bucket */
    private static Logger log = Logger.getLoggerEx("messif.buckets.impl.SimpleDiskBucket");

    /** The number of objects currently stored in the bucket file */
    protected transient int storedObjectCount = 0;

    /** The file with bucket data */
    protected File currentFileName;

    /** The file for compacted data */
    protected File compactedFileName;
    
    /** The output stream for adding new objects */
    protected transient ObjectOutputStream bucketOutput;

    /** The list of deleted objects positions */
    protected transient Set<Integer> deletedObjectPositions;

    /** List of currently running iterators */
    protected transient Map<SimpleDiskBucketIterator<? extends SimpleDiskBucket>, Boolean> runningIterators;

    /**
     * The compacting threshold value.
     * If the ratio of deleted objects to actual objects held exceeds the threshold, the bucket is compacted.
     */
    protected static final float DELETED_RATIO_THRESHOLD = 0.5f;

    /**
     * The number of objects written to a stream before it is reset.
     * Increasing this setting increases performance but memory requirements will be higher for each query.
     * If set to one, no additional memory will be allocated, but the performance will be one order of magnitude worse.
     */
    protected static final int STREAM_RESET_THRESHOLD = 50;

    /** The prefix for auto-generated filenames */
    protected static final String FILENAME_PREFIX = "simple_disk_bucket_";

    /** The suffix for auto-generated filenames */
    protected static final String FILENAME_SUFFIX = ".sdb";

    /** The suffix for the second bucket file */
    protected static final String FILENAME_SECOND_SUFFIX = ".2";

    /****************** Constructors ******************/

    /**
     * Creates a new SimpleDiskBucket instance.
     * Two files will be created - one as the specified file and second with appended ".2" suffix.
     * Note that these files <i>will</i> be erased and their contents <i>will be lost</i>.
     *
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     * @param file the file in which to create the bucket
     * @throws IOException if there was an error opening the bucket file
     */
    public SimpleDiskBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes, File file) throws IOException {
        super(capacity, softCapacity, lowOccupation, occupationAsBytes);

        // Create file names
        currentFileName = file;
        compactedFileName = new File(file.getPath() + FILENAME_SECOND_SUFFIX);
        
        // Create output stream (and rewrite the file)
        bucketOutput = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(currentFileName)));

        // Empty list of deleted objects
        deletedObjectPositions = new HashSet<Integer>();
        runningIterators = new WeakHashMap<SimpleDiskBucketIterator<? extends SimpleDiskBucket>, Boolean>();
    }

    /**
     * Creates a new SimpleDiskBucket instance.
     * Two files will be created - one as the specified file and second with appended ".2" suffix.
     * Note that these files <i>will</i> be erased and their contents <i>will be lost</i>.
     *
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     * @param fileName the file in which to create the bucket
     * @throws IOException if there was an error opening the bucket file
     */
    public SimpleDiskBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes, String fileName) throws IOException {
        this(capacity, softCapacity, lowOccupation, occupationAsBytes, new File(fileName));
    }

    /**
     * Clean opened file descriptors and remove the bucket file.
     * @throws Throwable the <code>Exception</code> raised by this method
     */
    @Override
    public void finalize() throws Throwable {
        bucketOutput.close();
        currentFileName.delete();
        compactedFileName.delete();
    }


    /******************  Factory method ******************/
    
    /**
     * Creates a bucket. The additional parameters are specified in the parameters map.
     * Recognized parameters:
     *   file (either as File or as String) - the path to the particular bucket
     *   dir (either as File or as String) - the path to a directory where a temporary file name is created in the format of "simple_disk_bucket_XXXX.dbt"
     * 
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     * @param parameters list of named parameters - this bucket supports "file" and "path" (see above)
     * @throws IOException if something goes wrong when working with the filesystem
     * @throws InstantiationException if the parameters specified are invalid (non existent directory, null values, etc.)
     * @return a new SimpleDiskBucket instance
     */
    public static SimpleDiskBucket getBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes, Map<String, Object> parameters) throws IOException, InstantiationException {
        String fileName;
        try {
            fileName = (String)parameters.get("file");
        } catch (ClassCastException ignore) {
            fileName = ((File)parameters.get("file")).getPath();
        }
        // if a file was not specified - create a new file in given directory
        if (fileName == null) {
            File dir;
            try {
                dir = (File)parameters.get("dir");
            } catch (ClassCastException ignore) {
                dir = new File((String)parameters.get("dir"));
            }
            if (dir == null)
                return new SimpleDiskBucket(capacity, softCapacity, lowOccupation, occupationAsBytes, File.createTempFile(FILENAME_PREFIX, FILENAME_SUFFIX));
            else return new SimpleDiskBucket(capacity, softCapacity, lowOccupation, occupationAsBytes, File.createTempFile(FILENAME_PREFIX, FILENAME_SUFFIX, dir));
        } else return new SimpleDiskBucket(capacity, softCapacity, lowOccupation, occupationAsBytes, fileName);
    }
    

    /****************** Serialization ******************/

    /**
     * Serialize this object into the output stream <code>out</code>.
     * @param out the stream to serialize this object into
     * @throws IOException if there was an I/O error during serialization
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        synchronized (this) {
            out.defaultWriteObject();
            // Write actual object count (deleted objects will be skipped automatically by the iterator)
            out.writeInt(getObjectCount());

            // Read all objects from this bucket using iterator and write them to the output stream
            Iterator<LocalAbstractObject> objects = iterator();
            while (objects.hasNext())
                out.writeObject(objects.next());
        }
    }

    /**
     * Deserialize this object from the input stream <code>in</code>.
     * @param in the stream to deserialize this object from
     * @throws IOException if there was an I/O error during serialization
     * @throws ClassNotFoundException if there was an unknown class serialized
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {        
        in.defaultReadObject();

        // Read actual object count
        storedObjectCount = in.readInt();

        // Create output stream (and rewrite the file)
        bucketOutput = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(currentFileName)));

        // Initialize empty lists of deleted objects and iterators
        deletedObjectPositions = new HashSet<Integer>();
        runningIterators = new WeakHashMap<SimpleDiskBucketIterator<? extends SimpleDiskBucket>, Boolean>();

        // Write objects to the bucket file
        for (int i = 0; i < storedObjectCount; i++) {
            bucketOutput.writeUnshared(in.readObject());
            if ((i+1) % STREAM_RESET_THRESHOLD == 0)
                bucketOutput.reset();
        }
    }


    /****************** Overrides ******************/
    
    /**
     * Returns current number of objects stored in bucket.
     * @return current number of objects stored in bucket
     */
    public int getObjectCount() {
        return storedObjectCount - deletedObjectPositions.size();
    }

    /**
     * Stores the specified object in a the hash table.
     *
     * @param object the new object to be inserted
     * @return OBJECT_INSERTED if the object was successfuly inserted,
     *         otherwise an exception is thrown (usually OutOfMemoryError)
     */
    protected synchronized BucketErrorCode storeObject(LocalAbstractObject object) {
        try {
            bucketOutput.writeUnshared(object);
            if ((storedObjectCount + 1) % STREAM_RESET_THRESHOLD == 0)
                bucketOutput.reset();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        storedObjectCount++;
        return BucketErrorCode.OBJECT_INSERTED;
    }

    /**
     * Returns if the ratio of deleted objects to actualy stored objects exceeds the threshold.
     * @param threshold the threshold to check
     * @return if the ratio of deleted objects to actualy stored objects exceeds the threshold
     */
    protected boolean isCompactingRequired(float threshold) {
        return deletedObjectPositions.size() > storedObjectCount * threshold;
    }

    /**
     * Compact the free space of deleted objects.
     * File is compacted only if there was a delete operation executed and a threshold on number of
     * deleted objects is reached.
     *
     * @param callingIterator the iterator from which the compating was called
     * @return <tt>true</tt> if the file was compacted or <tt>false</tt> if there was no need for compacting
     */
    protected synchronized boolean compactFile(SimpleDiskBucketIterator<? extends SimpleDiskBucket> callingIterator) {
        // Check if the file needs compacting
        if (!isCompactingRequired(DELETED_RATIO_THRESHOLD))
            return false;

        try {
            // Invalidate all iterators currently executing on this bucket (since they can't continue anyway and will throw ConcurrentModificationException)
            for (SimpleDiskBucketIterator iterator : runningIterators.keySet()) {
                try {
                    if (iterator != callingIterator && iterator != null)
                        iterator.stream.close();
                } catch (NullPointerException e) {
                    // Ignored, since weak hash map can have unpredictable nulls in keys (but this should not happen very often)
                }
            }
                
            // Open temporary file for writing
            ObjectOutputStream compactedFile = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(compactedFileName)));
        
            // Read all objects from this file using iterator and write them to a new file
            Iterator<LocalAbstractObject> objects = iterator();
            for (int i = 0; objects.hasNext(); i++) {
                compactedFile.writeUnshared(objects.next());
                if ((i+1) % STREAM_RESET_THRESHOLD == 0)
                    bucketOutput.reset();
            }

            // Switch compacted and current bucket files
            bucketOutput.close();
            bucketOutput = compactedFile;
            File tmpVar = currentFileName;
            currentFileName = compactedFileName;
            compactedFileName = tmpVar;

            // Clear deleted positions info and update stored count
            storedObjectCount -= deletedObjectPositions.size();
            deletedObjectPositions.clear();
            
            // Update deletion of the file (if current iterator is holding it, postpone the deletion)
            if (callingIterator != null)
                callingIterator.delete(compactedFileName);
            else compactedFileName.delete();

            return true;
        } catch (IOException e) {
            compactedFileName.delete();
            return false;
        }
    }

    /**
     * Returns iterator through all the objects in this bucket.
     * @return iterator through all the objects in this bucket
     */
    protected synchronized LocalBucketIterator<? extends SimpleDiskBucket> iterator() {
        try {
            SimpleDiskBucketIterator<SimpleDiskBucket> iterator = new SimpleDiskBucketIterator<SimpleDiskBucket>(this);
            runningIterators.put(iterator, null);
            return iterator;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Delete all objects from this bucket.
     * @return the number of deleted objects
     * @throws OccupationLowException if the low occupation limit is reached when deleting objects
     */
    @Override
    public synchronized int deleteAllObjects() throws OccupationLowException {
        // If the bucket has some required lowest occupation, this method cannot be used
        if (lowOccupation > 0)
            throw new OccupationLowException();

        // Invalidate all iterators currently executing on this bucket (since they can't continue anyway and will throw ConcurrentModificationException)
        for (SimpleDiskBucketIterator iterator : runningIterators.keySet()) {
            try {
                iterator.stream.close();
            } catch (NullPointerException e) {
                // Ignored, since weak hash map can have unpredictable nulls in keys (but this should not happen very often)
            } catch (IOException e) {
                // Ignored, this is just a closing exception
            }
        }

        int deleted = 0;
        try {
            // Open temporary file for writing
            ObjectOutputStream compactedFile = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(compactedFileName)));
        
            // Switch compacted and current bucket files
            bucketOutput.close();
            bucketOutput = compactedFile;
            File tmpVar = currentFileName;
            currentFileName = compactedFileName;
            compactedFileName = tmpVar;

            // Clear deleted positions info and update stored count
            storedObjectCount = 0;
            deletedObjectPositions.clear();
            occupation = 0;

            // Update statistics
            counterBucketDelObject.add(this, deleted);
        } catch (IOException e) {
            log.warning("Cannot delete all objects from simple disk bucket: " + e);
        }

        return deleted;
    }


    /****************** Iterator object ******************/

    /**
     * Internal class for iterator implementation.
     * @param <T> the type of the bucket this iterator operates on
     */
    protected static class SimpleDiskBucketIterator<T extends SimpleDiskBucket> extends LocalBucket.LocalBucketIterator<T> {
        /** Currently executed stream of objects */
        protected final ObjectInputStream stream;
        /** Last returned object */
        protected LocalAbstractObject currentObject = null;
        /** Instance of a next object. This is needed for implementing reading objects from a stream */
        protected LocalAbstractObject nextObject;
        /** Position in the file (in object count) */
        protected int position;
        /** The temporary file that needs deletion if there was compacting involved */
        protected File deleteFile = null;

        /**
         * Creates a new instance of SimpleDiskBucketIterator with the SimpleDiskBucket.
         * This constructor is intended to be called only from SimpleDiskBucket class.
         *
         * @param bucket actual instance of AlgorithmStorageBucket on which this iterator should work
         * @throws IOException if there was an error reading the bucket's file
         */
        protected SimpleDiskBucketIterator(T bucket) throws IOException {
            super(bucket);
            
            bucket.bucketOutput.flush(); // Write all buffered data to disk
            this.stream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(bucket.currentFileName)));
            position = 0;
            this.nextObject = nextStreamObject();
        }
        
        /**
         * Clean up of stream iterator
         * @throws Throwable if there was an error closing the stream
         */
        @Override
        protected void finalize() throws Throwable {
            stream.close();
        }

        /**
         * Physically remove the current object.
         */
        protected void removeInternal() {
            if (position <= 1)
                throw new IllegalStateException("The next method has not yet been called");
            synchronized (bucket) {
                bucket.deletedObjectPositions.add(position - 1); // since we are one object in advance (nextObject) with respect to current object
                bucket.compactFile(this);
            }
        }

        /**
         * Mark the temporary file for the deletion after this iterator is closed.
         * @param temporaryFile the file to delete
         */
        protected void delete(File temporaryFile) {
            deleteFile = temporaryFile;
        }

        /**
         * Returns the next element in the iteration.
         *
         * @return the next element in the iteration.
         * @throws NoSuchElementException iteration has no more elements.
         */
        public LocalAbstractObject next() throws NoSuchElementException {
            // No next object available
            if (nextObject == null)
                throw new NoSuchElementException("No more objects in bucket");

            // Reading object from a stream on the fly
            try {
                currentObject = nextObject;
                nextObject = nextStreamObject();
            } catch (IOException e) {
                throw new ConcurrentModificationException(e.toString());
            }
            return currentObject;
        }

        /**
         * Returns <tt>true</tt> if the iteration has more elements. (In other
         * words, returns <tt>true</tt> if <tt>next</tt> would return an element
         * rather than throwing an exception.)
         *
         * @return <tt>true</tt> if the iterator has more elements.
         */
        public boolean hasNext() {
            return nextObject != null;
        }

        /**
         * Returns the object returned by the last call to next().
         * @return the object returned by the last call to next()
         * @throws NoSuchElementException if next() has not been called yet
         */
        public LocalAbstractObject getCurrentObject() throws NoSuchElementException {
            if (currentObject == null)
                throw new NoSuchElementException("Can't call getCurrentObject before next was called");
            
            return currentObject;
        }

        /**
         * Returns an instance of object which would be returned by next call to next().
         * 
         * @return an instance of object which would be returned by next call to next().
         *         If there is no additional object, null is returned.
         * @throws IOException if the file was modified or there was an IO error
         */
        protected LocalAbstractObject nextStreamObject() throws IOException {
            synchronized (bucket) {
                try {
                    // Advance position (next read will)
                    position++;
                    
                    // Skip deleted objects
                    while (bucket.deletedObjectPositions.contains(position)) {
                        stream.readObject();
                        position++;
                    }

                    return (LocalAbstractObject)stream.readObject();
                } catch (EOFException e) {
                    stream.close();
                    bucket.runningIterators.remove(this);
                    if (deleteFile != null)
                        deleteFile.delete();
                    return null;
                } catch (ClassNotFoundException e) {
                    throw new IOException(e.toString());
                }
            }
        }

    }    
    

}
