/*
 * DiskBucket.java
 *
 * Created on 18. kveten 2006, 18:57
 */

package messif.buckets.impl;

import messif.objects.LocalAbstractObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import messif.buckets.BucketDispatcher;
import messif.buckets.BucketErrorCode;
import messif.buckets.CapacityFullException;
import messif.buckets.LocalBucket;
import messif.buckets.LocalFilteredBucket;
import messif.buckets.OccupationLowException;
import messif.objects.UniqueID;
import messif.utility.Convert;
import messif.utility.Logger;


/**
 * This is the disk-oriented bucket. The data is stored in a file and all operations are performed on the file data.
 *
 * The bucket requires a file given in constructor. If the file does not exist, it creates it. If the file exists,
 * the bucket is restored from the file - if it is a valid DiskBucket file. Some information about the file (namely
 * list of free and occupied blocks) are read to main memory.
 *
 *
 * @author David Novak, xnovak8@fi
 */
public class DiskBucket extends LocalFilteredBucket implements Closeable, Serializable {
    /** class id for serialization */
    private static final long serialVersionUID = 1001L;
    
    /** Logger for this bucket */
    private static Logger log = Logger.getLoggerEx("messif.buckets.impl.DiskBucket");

    /** a block size (in bytes) - a step to enlarge the file by */
    private static final int RESIZE_BLOCK_SIZE = (int) Math.pow(2,14);
    
    /** this value encapsulates pointer to an unexisting position in the file*/
    private static final long NULL_POSITION = -1;
    
    /** size of the block header - flag occupied/free  and size of the block */
    private static final int BLOCK_HEADER_SIZE = 1 + (Long.SIZE/8);
    
    /**
     * The path to the physical file in which the bucket is stored
     */
    protected File file = null;

    /**
     * Returns the path to the physical file in which the bucket is stored
     * @return the path to the physical file in which the bucket is stored
     */
    public File getFile() {
        return this.file;
    }
    
    /** The FileChannel of the file */
    private RandomAccessFile raFile = null;
    
    /** The list of records in this bucket. It is sorted according to the position in the file */
    private SortedMap<Long,FileBlock> records = new TreeMap<Long, FileBlock>();
    
    /** The list of free blocks in the file. It is sorted according to the position in the file */
    private SortedMap<Long,FileBlock> freeList = new TreeMap<Long, FileBlock>();

    private Map<UniqueID, FileBlock> objectIDMap = new HashMap<UniqueID, FileBlock>();

    private Map<String, FileBlock> objectLocatorMap = new HashMap<String, FileBlock>();


    /******************  Constructors ******************/

    /**
     * Constructs a new DiskBucket instance
     * 
     * 
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     * @param file the path to the file to create the bucket from. If such file doesn't exist, create a new empty bucket with this filename
     * @throws InstantiationException if the file path is invalid
     */
    public DiskBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes, File file) throws InstantiationException {
        super(capacity, softCapacity, lowOccupation, occupationAsBytes);
        
        initBucket(file);
    }

    /**
     * Initialize bucket from the specified file
     * This method is used by the constructor and by the deserialization.
     * @param file the path to the file to create the bucket from. If such file doesn't exist, create a new empty bucket with this filename
     */
    private void initBucket(File file) throws InstantiationException {
        this.file = file;
        try {
            if (this.file == null)
                throw new InstantiationException("No file name specified");
            
            raFile = new RandomAccessFile(this.file, "rwd");
            
            if (raFile.length() == 0)
                initFile();
            else
                readFileStructure();
        } catch (FileNotFoundException e) {
            throw new InstantiationException(e.getMessage());
        } catch (IOException e) {
            throw new InstantiationException("File '"+file+"': "+e.getMessage());
        }        
    }

    /**
     * Close the bucket file in cleaning this object
     * @throws Throwable the <code>Exception</code> raised by the parent method
     */
    @Override
    public void finalize() throws Throwable {
        close();
        super.finalize();
    }


    /******************  Factory method ******************/
    
    /**
     * Creates a bucket. The additional parameters are specified in the parameters map.
     * Recognized parameters:
     *   file (either as File or as String) - the path to the particular bucket
     *   path (either as File or as String) - the path to a directory where a temporary file name is created in the format of "bucketXXXX.dbt"
     * 
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     * @param parameters list of named parameters - this bucket supports "file" and "path" (see above)
     * @throws IOException if something goes wrong when working with the filesystem
     * @throws InstantiationException if the parameters specified are invalid (non existent directory, null values, etc.)
     * @return a new DiskBucket instance
     */
    public static DiskBucket getBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes, Map<String, Object> parameters) throws IOException, InstantiationException {
        File file = Convert.getParameterValue(parameters, "file", File.class, null);

        // if a file was not specified - create a new file in given directory
        if (file == null) {
            File dir = Convert.getParameterValue(parameters, "path", File.class, null);
            if (dir == null)
                throw new InstantiationException("Neither file nor directory specified in the 'params' array");
            
            file = File.createTempFile("bucket", ".dbt", dir);
        }
        
        return new DiskBucket(capacity, softCapacity, lowOccupation, occupationAsBytes, file);
    }

    /**
     * Adds DiskBuckets from all files in a directory to the given bucketDispatcher.
     * Uses default settings of the bucket dispatcher (capacity etc.) and
     * the default directory path (default "path" parameter of the bucket dispatcher).
     * Only files with extension ".dbt" that are valid DiskBuckets are taken.
     * 
     * @param dispatcher bucket dispatcher to associate the buckets with
     * @return set of created buckets
     * @throws IOException if something goes wrong when working with the filesystem
     * @throws InstantiationException if the parameters specified are invalid (non existent directory, null values, etc.)
     * @throws CapacityFullException if the maximal number of buckets is already allocated
     */
    public static Set<DiskBucket> createBucketsFromDir(BucketDispatcher dispatcher) throws IOException, InstantiationException, CapacityFullException {
        return createBucketsFromDir(dispatcher, (String) dispatcher.getDefaultBucketClassParams().get("path"));
    }

    /**
     * Adds DiskBuckets from all files in a directory to the given bucketDispatcher.
     * Uses default settings of the bucket dispatcher (capacity etc.) and the specified directory path.
     * Only files with extension ".dbt" that are valid DiskBuckets are taken.
     * 
     * @param dispatcher bucket dispatcher to associate the buckets with
     * @param directory directory path to load disk buckets from
     * @return set of created buckets
     * @throws IOException if something goes wrong when working with the filesystem
     * @throws InstantiationException if the parameters specified are invalid (non existent directory, null values, etc.)
     * @throws CapacityFullException if the maximal number of buckets is already allocated
     */
    public static Set<DiskBucket> createBucketsFromDir(BucketDispatcher dispatcher, String directory) throws IOException, InstantiationException, CapacityFullException {
        return createBucketsFromDir(dispatcher, dispatcher.getBucketCapacity(), dispatcher.getBucketSoftCapacity(), dispatcher.getBucketLowOccupation(),
                dispatcher.getBucketOccupationAsBytes(), directory);
    }

    /**
     * Adds DiskBuckets from all files in a directory to the given bucketDispatcher.
     * All paramters and limits must be specified.
     * Only files with extension ".dbt" that are valid DiskBuckets are taken.
     * 
     * @param dispatcher bucket dispatcher to associate the buckets with
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     * @param directory directory path to load disk buckets from
     * @return set of created buckets
     * @throws IOException if something goes wrong when working with the filesystem
     * @throws InstantiationException if the parameters specified are invalid (non existent directory, null values, etc.)
     * @throws CapacityFullException if the maximal number of buckets is already allocated
     */
    public static Set<DiskBucket> createBucketsFromDir(BucketDispatcher dispatcher, long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes, String directory) throws IOException, InstantiationException, CapacityFullException {
        if (directory == null)
            throw new InstantiationException("The directory not specified");
        
        Set<DiskBucket> buckets = new HashSet<DiskBucket>();
        
        // Get all files from directory that end with .dbt
        File[] dbtFiles = new File(directory).listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".dbt");
            }
        });
        for (int i=0; i<dbtFiles.length; i++) {
            try {
                buckets.add(new DiskBucket(capacity,softCapacity, lowOccupation, occupationAsBytes, dbtFiles[i]));
            } catch (InstantiationException e) {
                System.err.println(e.getMessage());
            }
        }
        
        for (DiskBucket bucket: buckets)
            dispatcher.addBucket(bucket);
        
        return buckets;
    }


    /******************  Serrialization   *************/

    /** 
     * Serialize this object into the output stream <code>out</code>.
     * @param out the stream to serialize this object into
     * @throws IOException if there was an I/O error during serialization
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Only the bucket file is stored instead of attributes
        out.writeObject(file);
    }

    /**
     * Deserialize this object from the input stream <code>in</code>.
     * @param in the stream to deserialize this object from
     * @throws IOException if there was an I/O error during serialization
     * @throws ClassNotFoundException if there was an unknown class serialized
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // Restore the internal bucket structures from the file header
        try {
            initBucket((File) in.readObject());
        } catch (InstantiationException e) {
            throw new IOException(e.getMessage());
        }
    }


    /******************  Internal methods ************/

    /**
     * Initialize the bucket file.
     * One empty block is stored into the file.
     */
    private void initFile() throws IOException {
        raFile.setLength(RESIZE_BLOCK_SIZE);
        FileBlock block = new FileBlock(false, RESIZE_BLOCK_SIZE - BLOCK_HEADER_SIZE, 0);
        block.writeBlockInfo();
        
        records.clear();
        freeList.clear();
        freeList.put(block.position, block);
    }

    /**
     * Restored the internal structures from the given bucket file.
     * @throws IOException if the file does not contain a valid {@link DiskBucket}
     *         or there was another error reading the file
     */
    private void readFileStructure() throws IOException {
        try {
            records.clear();
            freeList.clear();
            
            raFile.seek(0);
            FileBlock block = null;
            while (raFile.getFilePointer() < raFile.length()) {
                // this constructor reads block info from the raFile from its position
                block = new FileBlock();
                if (block.occupied)
                    records.put(block.position, block);
                else
                    freeList.put(block.position, block);
            }
        } catch (IOException e) {
            throw new IOException("Not a valid DiskBucket: "+e.getMessage());
        }
    }

    /**
     * Adds a given block to the list of empty blocks.
     * The adjacent free blocks are joined.
     * @param block an empty block to add
     */
    private void addToFreeList(FileBlock block) throws IOException {
        block.occupied = false;
        // if the block to the left is free
        Long leftKey = (long) -1;
        for (Long key:freeList.keySet()) {
            if (key < block.position)
                leftKey = key;
            else
                break;
        }
        // if the left block is free, then only resize it and do not add the new block to the free list
        FileBlock leftBlock = freeList.get(leftKey);
        if ((leftBlock != null) && (leftKey + BLOCK_HEADER_SIZE +leftBlock.size == block.position)) {
            leftBlock.size += BLOCK_HEADER_SIZE + block.size;
            leftBlock.writeBlockInfo();
            block = leftBlock;
        }
        
        FileBlock rightBlock = freeList.remove(block.position + BLOCK_HEADER_SIZE + block.size );
        // then increase size of the inserted block and remove the next block
        if (rightBlock != null) {
            block.size += rightBlock.size + BLOCK_HEADER_SIZE;
        }
        block.writeBlockInfo();
        freeList.put(block.position, block);
    }

    /**
     * Shrinks the file and deallocate unnecessary blocks.
     */
    private void shrinkFile() throws IOException {
        // get last block
        FileBlock lastBlock = freeList.get(freeList.lastKey());
        if ((records.isEmpty() || lastBlock.position > records.lastKey()) &&  // if the last file block is empty
                lastBlock.size > (RESIZE_BLOCK_SIZE)) { // and its size is greater then the block factor
            // then shrink it
            long shrinkBy = RESIZE_BLOCK_SIZE * (lastBlock.size / RESIZE_BLOCK_SIZE - 1);
            lastBlock.size -= shrinkBy;
            lastBlock.writeBlockInfo();
            raFile.setLength(raFile.length() - shrinkBy);
        }
    }

    /**
     * Find the most appropriate empty block or enlarge the file.
     * @return an allocated block
     */
    private FileBlock allocateSpace(long size) throws IOException {
        // FirstFit algorithm to allocate free block of disk space
        FileBlock block = null;
        // iterate over the free blocks
        for (Iterator<FileBlock> iterator=freeList.values().iterator(); iterator.hasNext(); ) {
            block = iterator.next();
            // block that either can equals or can be split into 2 blocks (the second is free)
            if ((block.size == size) || (block.size >= size + BLOCK_HEADER_SIZE)) {
                iterator.remove();
                break;
            }
        }
        
        // if an appropriate free block has been found
        if ((block != null) && ((block.size == size) || (block.size >= size + BLOCK_HEADER_SIZE))) {
            if (block.size >= size + BLOCK_HEADER_SIZE) {
                FileBlock newBlock = new FileBlock(false, block.size - (size + BLOCK_HEADER_SIZE), block.position + size + BLOCK_HEADER_SIZE);
                addToFreeList(newBlock);
            }
            block.size = size;
            block.occupied = true;
            return block;
        } else { // else allocate new file block
            raFile.setLength(raFile.length() + RESIZE_BLOCK_SIZE);
            FileBlock newFreeBlock = new FileBlock(false, RESIZE_BLOCK_SIZE - BLOCK_HEADER_SIZE, raFile.length() - RESIZE_BLOCK_SIZE);
            addToFreeList(newFreeBlock);
            return allocateSpace(size);
        }
    }
    
    /**
     * This class encapsulates a block of the file.
     * It either contains a record or it's free (flag "occupied")
     * The "size" is without the header of the block (only size of data itself)
     * The position is the position of the block in the file.
     */
    private class FileBlock {
        /** flag occupied/free */
        private boolean occupied = true;
        
        /** size of the block - without the header!! (only bytes of record or free bytes) */
        private long size;
        
        /** position of the block in the file */
        private long position;
        
        /** constructor from given values */
        private FileBlock(boolean occupied, long size, long position) {
            this.occupied = occupied;
            this.size = size;
            this.position = position;
        }
        
        /** constructor from the standard file on its current position */
        private FileBlock() throws IOException {
            this(NULL_POSITION);
        }
        
        /** constructor from a given position in the file */
        private FileBlock(long filePosition) throws IOException {
            if (filePosition != NULL_POSITION)
                raFile.seek(filePosition);
            readBlockInfo();
            if (occupied) {
                LocalAbstractObject object = readObjectItself();
            } else
                raFile.skipBytes((int) size);
        }
        
        /** reads the block info from a given file (from its position) */
        private void readBlockInfo() throws IOException {
            this.position = raFile.getFilePointer();
            this.occupied = raFile.readBoolean();
            this.size = raFile.readLong();
        }
        
        /** write the block info to given file */
        private void writeBlockInfo() throws IOException {
            raFile.seek(position);
            raFile.writeBoolean(occupied);
            raFile.writeLong(size);
        }
        
        /** reads the object from the file and returns it */
        private void writeObject(byte[] buffer) throws IOException {
            if (buffer.length != size)
                throw new IOException("The buffer length does not correspond to the block size");
            occupied = true;
            writeBlockInfo();
            raFile.write(buffer);
        }
        
        /** reads the object from the file and returns it */
        private LocalAbstractObject readObject() throws IOException {
            if (! occupied)
                throw new IOException("This block of file does not contain a object");
            raFile.seek(position);
            if (occupied != raFile.readBoolean())
                throw new IOException("Memory index does not correspond to the physical file");
            if (size != raFile.readLong())
                throw new IOException("Memory index does not correspond to the physical file");
            
            return readObjectItself();
        }
        
        /** internal method that really reads the object itself - it expects being at the right file position */
        private LocalAbstractObject readObjectItself() throws IOException {
            byte[] buffer = new byte[(int) size];
            raFile.readFully(buffer);
            
            ObjectInputStream inStream = new ObjectInputStream((new ByteArrayInputStream(buffer)));
            try {
                LocalAbstractObject object = (LocalAbstractObject) inStream.readObject();
                return object;
            } catch (ClassNotFoundException e) {
                throw new IOException(e.getMessage());
            }
        }
    }


    /****************** Overrides ******************/

    /**
     * Stores an object into a disk block.
     *
     * @param object the new object to be inserted
     * @return OBJECT_REFUSED if there was an IOException during storage or
     *         OBJECT_INSERTED if the object was sucessfully inserted
     */
    protected BucketErrorCode storeObject(LocalAbstractObject object) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            new ObjectOutputStream(byteStream).writeObject(object);

            byte[] buffer = byteStream.toByteArray();
            byteStream.close();
            
            FileBlock block = allocateSpace(buffer.length);
            block.writeObject(buffer);
            
            records.put(block.position, block);
            objectIDMap.put(object.getObjectID(), block);
            if (object.getLocatorURI() != null)
                objectLocatorMap.put(object.getLocatorURI(), block);
        } catch (IOException e) {
            e.printStackTrace();
            return BucketErrorCode.OBJECT_REFUSED;
        }
        return BucketErrorCode.OBJECT_INSERTED;
    }

    /**
     * Returns current number of objects stored in bucket.
     * @return current number of objects stored in bucket
     */
    public int getObjectCount() {
        return records.size();
    }

    /**
     * Returns iterator through all the objects in this bucket.
     * @return iterator through all the objects in this bucket
     */
    protected LocalBucketIterator<? extends DiskBucket> iterator() {
        return new DiskBucketIterator<DiskBucket>(this);
    }

    /** Close the opened bucket file */
    public void close() {
        try {
            raFile.close();
        } catch (IOException ignore) { }
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

        int deleted = 0;
        try {
            deleted = records.size();
            initFile();
            objectIDMap.clear();
            objectLocatorMap.clear();
            occupation = 0;

            // Update statistics
            counterBucketDelObject.add(this, deleted);
        } catch (IOException e) {
            log.warning("Cannot delete all objects from disk bucket: " + e);
        }

        return deleted;
    }

    /**
     * Internal class for iterator implementation
     * @param <T> the type of the bucket this iterator operates on
     */
    protected static class DiskBucketIterator<T extends DiskBucket> extends LocalBucket.LocalBucketIterator<T> {
        /** Iterator over the records in the "records" array */
        private final Iterator<FileBlock> iterator;

        /** Iterator over the records in the "records" array */
        private FileBlock lastBlock = null;

        /** Current object */
        protected LocalAbstractObject currentObject = null;

        /**
         * Creates a new instance of DiskBucketIterator with the DiskBucket.
         * This constructor is intended to be called only from DiskBucket class.
         *
         * @param bucket actual instance of DiskBucket on which this iterator should work
         */
        protected DiskBucketIterator(T bucket) {
            super(bucket);
            this.iterator = bucket.records.values().iterator();
        }

        /**
         * Returns <tt>true</tt> if the iteration has more elements. (In other
         * words, returns <tt>true</tt> if <tt>next</tt> would return an element
         * rather than throwing an exception.)
         *
         * @return <tt>true</tt> if the iterator has more elements.
         */
        public boolean hasNext() {
            return iterator.hasNext();
        }

        /**
         * Returns the next element in the iteration.
         *
         * @return the next element in the iteration.
         * @throws NoSuchElementException iteration has no more elements.
         */
        public LocalAbstractObject next() throws NoSuchElementException {
            try {
                lastBlock = iterator.next();
                currentObject = lastBlock.readObject();
                return currentObject;
            } catch (IOException e) {
                throw new NoSuchElementException(e.getMessage());
            }
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
         * Physically removes the last object returned by this iterator.
         * 
         * @throws NoSuchElementException if next or getObjectByID was not called before
         * @throws IllegalStateException if there was an IOException during the removal
         */
        public void removeInternal() throws NoSuchElementException, IllegalStateException {
            try {
                iterator.remove();
                bucket.addToFreeList(lastBlock);
                bucket.shrinkFile();
                bucket.objectIDMap.remove(currentObject.getObjectID());
                if (currentObject.getLocatorURI() != null)
                    bucket.objectLocatorMap.remove(currentObject.getLocatorURI());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } catch (NullPointerException e) {
                throw new NoSuchElementException(e.getMessage());
            }
        }


        /************   MORE EFFICIENT IMPLEMENTATIONS - OVERRIDES FROM GenericObjectIterator *************************/

        /**
         * Returns an instance of object on the position of 'position' from the current object.
         * Naive solution: next() is called 'position' times and that object is returned.
         * 
         * @param position position of the object to get
         * @return instance of object on the position of 'position' from the current object
         * @throws NoSuchElementException if such an object cannot be found.
         */
        @Override
        public LocalAbstractObject getObjectByPosition(int position) throws NoSuchElementException {
            try {
                for (; position >= 0; position--)
                    lastBlock = iterator.next();
                
                return currentObject = lastBlock.readObject();
            } catch (IOException e) {
                throw new NoSuchElementException(e.getMessage());
            }
        }

        /**
         * Returns the instance of object, that has specified ID.
         *
         * @param objectID ID of the object that we are searching for
         * @return the instance of object, that has specified ID
         * @throws NoSuchElementException if such an object cannot be found.
         */
        @Override
        public LocalAbstractObject getObjectByID(UniqueID objectID) throws NoSuchElementException {
            try {
                lastBlock = bucket.objectIDMap.get(objectID);
                if (lastBlock == null) {
                    currentObject = null;
                    throw new NoSuchElementException("There is no object with ID " + objectID);
                }
                return currentObject = lastBlock.readObject();
            } catch (IOException e) {
                throw new NoSuchElementException(e.toString());
            }
        }

        /**
         * Returns the first instance of object, that has one of the specified locators.
         * The locators are checked one by one using hash table. The first locator that
         * has an object associated is returned. The locators without an object associated
         * are also removed from the set if <code>removeFound</code> is <tt>true</tt>.
         *
         * @param locatorURIs the set of locators that we are searching for
         * @param removeFound if <tt>true</tt> the locators which were found are removed from the <tt>locatorURIs</tt> set, otherwise, <tt>locatorURIs</tt> is not touched
         * @return the first instance of object, that has one of the specified locators
         * @throws NoSuchElementException if there is no object with any of the specified locators
         */
        @Override
        public LocalAbstractObject getObjectByAnyLocator(Set<String> locatorURIs, boolean removeFound) throws NoSuchElementException {
            try {
                Iterator<String> uriIterator = locatorURIs.iterator();
                while (uriIterator.hasNext()) {
                    lastBlock = bucket.objectLocatorMap.get(uriIterator.next());
                    if (removeFound)
                        uriIterator.remove();
                    if (lastBlock != null)
                        return currentObject = lastBlock.readObject();
                }

                currentObject = null;
                throw new NoSuchElementException("There is no object with the specified locator");
            } catch (IOException e) {
                throw new NoSuchElementException(e.toString());
            }
        }
    }

}
