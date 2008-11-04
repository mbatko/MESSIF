/*
 * LocalObject.java
 *
 * Created on 19. kveten 2004, 20:22
 */

package messif.objects;

import messif.objects.keys.AbstractObjectKey;
import messif.netbucket.RemoteAbstractObject;
import messif.statistics.StatisticCounter;
import messif.statistics.Statistics;
import messif.utility.Convert;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import messif.objects.nio.BinaryInputStream;
import messif.objects.nio.BinaryOutputStream;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

/**
 * This class is ancestor of all objects that hold some data the MESSI Framework can work with.
 * Since MESSIF works with metric-based data, every descendant of <tt>LocalAbstractObject<tt> must
 * implement a metric function {@link #getDistanceImpl} for its own data.
 *
 * To be able to read/write data from text streams, a constructor with one {@link java.io.BufferedReader} argument
 * should be implemented to parse object data from a line of text. A dual operation should be implemented as the
 * {@link #write} method.
 *
 * Each object can hold an additional data in its {@link #suppData} attribute. However, no management is guaranteed 
 * inside MESSIF, thus, if several algorithms that use supplemental data are combined, unpredictable results might
 * appear. This attribute is never modified inside MESSIF itself apart from the {@link #clearSurplusData} method that
 * sets it to <tt>null</tt>.
 *
 * @see AbstractObject
 * @see messif.netbucket.RemoteAbstractObject
 *
 * @author  xbatko
 */
public abstract class LocalAbstractObject extends AbstractObject {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 4L;

    //****************** Attributes ******************//

    /** Supplemental data object */
    public Object suppData = null;

    /** Object for storing and using precomputed distances */
    private PrecomputedDistancesFilter distanceFilter = null;


    //****************** Statistics ******************//

    /** Global counter for distance computations (any purpose) */
    protected static final StatisticCounter counterDistanceComputations = StatisticCounter.getStatistics("DistanceComputations");

    /** Global counter for lower-bound distance computations (any purpose) */
    protected static final StatisticCounter counterLowerBoundDistanceComputations = StatisticCounter.getStatistics("DistanceComputations.LowerBound");

    /** Global counter for upper-bound distance computations (any purpose) */
    protected static final StatisticCounter counterUpperBoundDistanceComputations = StatisticCounter.getStatistics("DistanceComputations.UpperBound");


    //****************** Constructors ******************//

    /**
     * Creates a new instance of LocalAbstractObject.
     * A new unique object ID is generated and the
     * object's key is set to <tt>null</tt>.
     */
    protected LocalAbstractObject() {
        super();
    }

    /**
     * Creates a new instance of LocalAbstractObject.
     * A new unique object ID is generated and the 
     * object's key is set to the specified key.
     * @param objectKey the key to be associated with this object
     */
    protected LocalAbstractObject(AbstractObjectKey objectKey) {
        super(objectKey);
    }

    /**
     * Creates a new instance of LocalAbstractObject.
     * A new unique object ID is generated and a
     * new {@link AbstractObjectKey} is generated for
     * the specified <code>locatorURI</code>.
     * @param locatorURI the locator URI for the new object
     */
    protected LocalAbstractObject(String locatorURI) {
        super(locatorURI);
    }


    //****************** Local object converter ******************//

    /**
     * Returns this abstract object as local object.
     * Thus, this method returns this object itself.
     * @return this abstract object as local object
     */
    public LocalAbstractObject getLocalAbstractObject() {
        return this;
    }


    //****************** Remote object converter ******************//

    /**
     * Returns the RemoteAbstractObject that contains only the URI locator of this object.
     * For LocalAbstractObject create new object.
     * @return new RemoteAbstractObject containing URI locator of this object.
     */
    public RemoteAbstractObject getRemoteAbstractObject() {
        return new RemoteAbstractObject(this);
    }


    //****************** Size function ******************//

    /**
     * Returns the size of this object in bytes.
     * @return the size of this object in bytes
     */
    public abstract int getSize();


    //****************** Unused/undefined, min, max distances ******************//

    /** Unknown distance constant */
    public static final float UNKNOWN_DISTANCE = Float.NEGATIVE_INFINITY;
    /** Minimal possible distance constant */
    public static final float MIN_DISTANCE = 0.0f;
    /** Maximal possible distance constant */
    public static final float MAX_DISTANCE = Float.MAX_VALUE;


    //****************** Metric functions ******************//

    /** 
     * Metric distance function.
     * Returns the distance between this object and the object that is supplied as argument.
     *
     * @param obj the object for which to measure the distance
     * @return the distance between this object and the provided object <code>obj</code>
     */
    public final float getDistance(LocalAbstractObject obj) {
        return getDistance(obj, MAX_DISTANCE);
    }

    /**
     * Metric distance function.
     *    This method is intended to be used in situations such as:
     *    We are executing a range query, so all objects distant from the query object up to the query radius
     *    must be returned. In other words, all objects farther from the query object than the query radius are
     *    uninteresting. From the distance point of view, during the evaluation process of the distance between 
     *    a pair of objects we can find out that the distance cannot be lower than a certain value. If this value 
     *    is greater than the query radius, we can safely abort the distance evaluation since we are dealing with one
     *    of those uninteresting objects.
     *
     * @param obj the object to compute distance to
     * @param distThreshold the threshold value on the distance (the query radius from the example above)
     * @return the actual distance between obj and this if the distance is lower than distThreshold.
     *         Otherwise the returned value is not guaranteed to be exact, but in this respect the returned value
     *         must be greater than the threshold distance.
     */
    public final float getDistance(LocalAbstractObject obj, float distThreshold) {
        if (distanceFilter != null && distanceFilter.isGetterSupported()) {
            float distance = distanceFilter.getPrecomputedDistance(obj);
            if (distance != UNKNOWN_DISTANCE)
                return distance;
        }

        // This check is to enhance performance when statistics are disabled
        if (Statistics.isEnabledGlobally())
            counterDistanceComputations.add();

        return getDistanceImpl(obj, distThreshold);
    }

    /**
     * The actual implementation of the metric function (see {@link #getDistance} for full explanation).
     * The implementation should not increment distanceComputations statistics.
     *
     * @param obj the object to compute distance to
     * @param distThreshold the threshold value on the distance
     * @return the actual distance between obj and this if the distance is lower than distThreshold
     */
    protected abstract float getDistanceImpl(LocalAbstractObject obj, float distThreshold);

    /**
     * Normalized metric distance function, i.e. the result of {@link #getDistance}
     * divided by {@link #getMaxDistance}. Note that unless an object overrides
     * the {@link #getMaxDistance} the resulting distance will be too small.
     * 
     * @param obj the object to compute distance to
     * @param distThreshold the threshold value on the distance (see {@link #getDistance} for explanation)
     * @return the actual normalized distance between obj and this if the distance is lower than distThreshold
     */
    public final float getNormDistance(LocalAbstractObject obj, float distThreshold) {
        return getDistance(obj, distThreshold) / getMaxDistance();
    }

    /**
     * Returns a maximal possible distance for this class.
     * This method <i>must</i> return the same value for all instances of this class.
     * Default implementation returns {@link #MAX_DISTANCE}.
     * @return a maximal possible distance for this class
     */
    public float getMaxDistance() {
        return MAX_DISTANCE;
    }

    /**
     * Lower bound of a metric distance.
     *    Returns the lower bound of the distance between this object and the object that is supplied 
     *    as argument. The function allows several levels of precision (parameter accuracy).
     *
     *  When redefining this method do not forget to add <code>
     *      counterLowerBoundDistanceComputations.add();
     *  </code> for statistics to be maintained.
     *
     * @param obj the object to compute lower-bound distance to
     * @param accuracy the level of precision to use for lower-bound
     * @return the lower bound of the distance between this object and <code>obj</code>
     */
    public float getDistanceLowerBound(LocalAbstractObject obj, int accuracy) {
        counterLowerBoundDistanceComputations.add();
        return MIN_DISTANCE;
    }

    /**
     * Upper bound of a metric distance.
     *    Returns the upper bound of the distance between this object and the object that is supplied 
     *    as argument. The function allows several levels of precision (parameter accuracy).
     *
     *  When redefining this method do not forget to add <code>
     *      counterUpperBoundDistanceComputations.add();
     *  </code> for statistics to be maintained.
     *
     * @param obj the object to compute upper-bound distance to
     * @param accuracy the level of precision to use for upper-bound
     * @return the upper bound of the distance between this object and <code>obj</code>
     */
    public float getDistanceUpperBound(LocalAbstractObject obj, int accuracy) {
        counterUpperBoundDistanceComputations.add();
        return MAX_DISTANCE;
    }

    /**
     * Returns <tt>true</tt> if the <code>obj</code> has been excluded (filtered out) using stored precomputed distance.
     * Otherwise returns <tt>false</tt>, i.e. when <code>obj</code> must be checked using original distance (see {@link #getDistance}).
     *
     * In other words, method returns <tt>true</tt> if <code>this</code> object and <code>obj</code> are more distant than <code>radius</code>. By
     * analogy, returns <tt>false</tt> if <code>this</code> object and <code>obj</code> are within distance <code>radius</code>. However, both this cases
     * use only precomputed distances. Thus, the real distance between <code>this</code> object and <code>obj</code> can be greater
     * than <code>radius</code> although the method returned <tt>false</tt>!
     * @param obj the object to check the distance for
     * @param radius the radius between <code>this</code> object and <code>obj</code> to check
     * @return <tt>true</tt> if the <code>obj</code> has been excluded (filtered out) using stored precomputed distance
     */
    public final boolean excludeUsingPrecompDist(LocalAbstractObject obj, float radius) {
        if (distanceFilter != null && obj.distanceFilter != null)
            return distanceFilter.excludeUsingPrecompDist(obj.distanceFilter, radius);

        return false;
    }

    /**
     * Returns <tt>true</tt> if the <code>obj</code> has been included using stored precomputed distance.
     * Otherwise returns <tt>false</tt>, i.e. when <code>obj</code> must be checked using original distance (see {@link #getDistance}).
     *
     * In other words, method returns <tt>true</tt> if the distance of <code>this</code> object and <code>obj</code> is below the <code>radius</code>.
     * By analogy, returns <tt>false</tt> if <code>this</code> object and <code>obj</code> are more distant than <code>radius</code>.
     * However, both this cases use only precomputed distances. Thus, the real distance between <code>this</code> object and
     * <code>obj</code> can be lower than <code>radius</code> although the method returned <tt>false</tt>!
     * @param obj the object to check the distance for
     * @param radius the radius between <code>this</code> object and <code>obj</code> to check
     * @return <tt>true</tt> if the obj has been included using stored precomputed distance
     */
    public final boolean includeUsingPrecompDist(LocalAbstractObject obj, float radius) {
        if (distanceFilter != null && obj.distanceFilter != null)
            return distanceFilter.includeUsingPrecompDist(obj.distanceFilter, radius);

        return false;
    }


    //****************** Distance filter manipulation ******************//

    /**
     * Returns a filter of specified class from this object's filter chain.
     * If there is no filter with requested class, return <tt>null</tt>.
     * If there are more filters with the same class, the sameClassPosition parameter
     * is considered to pick the correct one.
     *
     * @param <T> the class of the filter to retrieve from the chain
     * @param filterClass the class of the filter to retrieve from the chain
     * @return a filter of specified class from this object's filter chain
     * @throws NullPointerException if the filterClass is <tt>null</tt>
     */
    public <T extends PrecomputedDistancesFilter> T getDistanceFilter(Class<T> filterClass) throws NullPointerException {
        for (PrecomputedDistancesFilter currentFilter = distanceFilter; currentFilter != null; currentFilter = currentFilter.getNextFilter())
            if (filterClass.equals(currentFilter.getClass()))
                return (T)currentFilter; // This cast IS checked on the previous line            

        return null;
    }

    /**
     * Returns a filter at specified position in this object's filter chain.
     * @param position a zero based position in the chain (zero returns this filter, negative value returns the last filter)
     * @return a filter at specified position in this filter's chain
     * @throws IndexOutOfBoundsException if the specified position is too big
     */
    public PrecomputedDistancesFilter getDistanceFilter(int position) throws IndexOutOfBoundsException {
        // Fill iteration variable
        PrecomputedDistancesFilter currentFilter = distanceFilter;
        while (currentFilter != null) {
            if (position == 0)
                return currentFilter;

            // Get next iteration value
            PrecomputedDistancesFilter nextFilter = currentFilter.getNextFilter();

            if (position < 0 && nextFilter == null)
                return currentFilter;

            currentFilter = nextFilter;
            position--;    
        }

        throw new IndexOutOfBoundsException("There is no filter at position " + position);
    }

    /**
     * Adds the specified filter to the end of this object's filter chain.
     * 
     * @param filter the filter to add to this object's filter chain
     * @param replaceIfExists if <tt>true</tt> and there is another filter with the same class as the inserted filter, it is replaced
     * @return either the replaced or the existing filter that has the same class as the newly inserted one; <tt>null</tt> is
     *         returned if the filter was appended to the end of the chain
     * @throws IllegalArgumentException if the provided chain has set nextFilter attribute
     */
    public final PrecomputedDistancesFilter chainFilter(PrecomputedDistancesFilter filter, boolean replaceIfExists) throws IllegalArgumentException {
        if (filter.nextFilter != null)
            throw new IllegalArgumentException("This filter is a part of another chain");

        // Add this filter to the object's distance filter chain
        if (distanceFilter == null) {
            // We are at the end of the chain
            distanceFilter = filter;
            return null;
        } else if (distanceFilter.getClass().equals(filter.getClass())) {
            if (!replaceIfExists)
                return distanceFilter;
            // Preserve the chain link
            filter.nextFilter = distanceFilter.nextFilter;
            // Replace filter
            PrecomputedDistancesFilter storedFilter = distanceFilter;
            distanceFilter = filter;
            return storedFilter;
        } else return distanceFilter.chainFilter(filter, replaceIfExists);
    }

    /**
     * Deletes the specified filter from this object's filter chain.
     * A concerete instance of filter is deleted (the same reference must be present in the chain).
     * 
     * @param filter the concrete instance of filter to delete from this object's filter chain
     * @return <tt>true</tt> if the filter was unchained (deleted). If the given filter was not found, <tt>false</tt> is returned.
     */
    public boolean unchainFilter(PrecomputedDistancesFilter filter) {
        if (distanceFilter == null)
            return false;
        
        if (distanceFilter == filter) {
            distanceFilter = distanceFilter.nextFilter;
            return true;
        } else {
            PrecomputedDistancesFilter prev = distanceFilter;
            PrecomputedDistancesFilter curr = distanceFilter.nextFilter;
            while (curr != null) {
                if (curr == filter) {
                    prev.nextFilter = curr.nextFilter;
                    return true;
                }
                prev = curr;
                curr = curr.nextFilter;
            }
            return false;
        }
    }

    /**
     * Destroys whole filter chain of this object.
     * The first (head of the chain) filter is returned.
     * @return the first filter in the chain; the rest of the chain can be
     *         obtained by calling {@link PrecomputedDistancesFilter#getNextFilter getNextFilter}
     */
    public final PrecomputedDistancesFilter chainDestroy() {
        PrecomputedDistancesFilter rtv = distanceFilter;
        distanceFilter = null;
        return rtv;
    }

    /**
     * Clear non-messif data stored in this object.
     * In addition to changing object key, this method removes
     * the {@link #suppData supplemental data} and
     * all {@link #distanceFilter distance filters}.
     */
    @Override
    public void clearSurplusData() {
        super.clearSurplusData();
        suppData = null;
        distanceFilter = null;
    }


    //****************** Random generators ******************//

    /**
     * Returns a pseudorandom number.
     * The generator has normal distribution not the default standardized.
     * @return a pseudorandom <code>double</code> greater than or equal 
     * to <code>0.0</code> and less than <code>1.0</code>
     */
    protected static double getRandomNormal() {
        double rand = 0;
        for (int i = 0; i < 12; i++) rand += Math.random();
        return rand/12.0;
    }

    /**
     * Returns a pseudorandom character.
     * @return a pseudorandom <code>char</code> greater than or equal 
     * to <i>a</i> and less than <i>z</i>
     */
    protected static char getRandomChar() {
        return (char)('a' + (int)(Math.random()*('z' - 'a')));
    }


    //****************** Equality driven by object data ******************//

    /** 
     * Indicates whether some other object has the same data as this one.
     * @param   obj   the reference object with which to compare.
     * @return  <code>true</code> if this object is the same as the obj
     *          argument; <code>false</code> otherwise.
     */
    public abstract boolean dataEquals(Object obj);

    /**
     * Returns a hash code value for the data of this object.
     * @return a hash code value for the data of this object
     */
    public abstract int dataHashCode();

    /**
     * A wrapper class that allows to hash/equal abstract objects
     * using their data and not ID. Especially, standard hashing
     * structures (HashMap, etc.) can be used on wrapped object.
     */
    public static class DataEqualObject {
        /** Encapsulated object */
        protected final LocalAbstractObject object;

        /**
         * Creates a new instance of DataEqualObject wrapper over the specified LocalAbstractObject.
         * @param object the encapsulated object
         */
        public DataEqualObject(LocalAbstractObject object) {
            this.object = object;
        }

        /**
         * Returns the encapsulated object.
         * @return the encapsulated object
         */
        public LocalAbstractObject get() {
            return object;
        }

        /**
         * Returns a hash code value for the object data.
         * @return a hash code value for the data of this object
         */
        @Override
        public int hashCode() {
            return object.dataHashCode();
        }

        /** 
         * Indicates whether some other object has the same data as this one.
         * @param   obj   the reference object with which to compare.
         * @return  <code>true</code> if this object is the same as the obj
         *          argument; <code>false</code> otherwise.
         */
        @Override
        public boolean equals(Object obj) {
            return object.dataEquals(obj);
        }
    }


    //****************** Factory method ******************//

    /**
     * Creates a new LocalAbstractObject of the specified type from string.
     * The format is "objectClass:object data", where "object data" is anything
     * after the first colon (including colons of course). The object data is
     * feeded through string buffer stream to the stream constructor of
     * the object.
     *
     * @param objectClassAndData the string to parse the class and data from
     * @return a new LocalAbstractObject of the specified type
     * @throws IllegalArgumentException if there is no class (colon separated), the class is invalid, the class is not LocalAbstractObject descendant or there is no stream constructor
     * @throws InvocationTargetException if there was an error during creating a new object instance
     */
    public static LocalAbstractObject valueOf(String objectClassAndData) throws IllegalArgumentException, InvocationTargetException {
        try {
            // Get position of a colon that separates object class from data
            int colonPos = objectClassAndData.indexOf(':');
            
            return valueOf(
                    // Get everything before the colon as the class name
                    Convert.getClassForName(objectClassAndData.substring(0, colonPos), LocalAbstractObject.class),
                    // Get everything after the colon as the object data
                    objectClassAndData.substring(colonPos + 1)
            );
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Cannot found object class. Use format objectClass:objectData: "+objectClassAndData);
        }
    }

    /**
     * Creates a new LocalAbstractObject of the specified type from string.
     * The object data is feeded through string buffer stream to the stream constructor of the object.
     *
     * @param <E> the class of the object to create
     * @param objectClass the class of the object to create
     * @param objectData the string that contains object's data
     * @return a new instance of the specified class
     * @throws IllegalArgumentException if the specified class lacks a public <tt>BufferedReader</tt> constructor
     * @throws InvocationTargetException if there was an error during creating a new object instance
     */
    public static <E extends LocalAbstractObject> E valueOf(Class<E> objectClass, String objectData) throws IllegalArgumentException, InvocationTargetException {
        try {
            // Create instance of the specified object
            return objectClass.getConstructor(BufferedReader.class).newInstance(
                // Create buffered reader from string
                new BufferedReader(new StringReader(objectData))
            );
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e);
        }
    }


    //****************** Clonning ******************//

    /**
     * Creates and returns a copy of this object. The precise meaning 
     * of "copy" may depend on the class of the object.
     *
     * @return a clone of this instance
     * @throws CloneNotSupportedException if the object's class does not support clonning or there was an error
     */
    @Override
    public final LocalAbstractObject clone() throws CloneNotSupportedException {
        return clone(true);
    }

    /**
     * Creates and returns a copy of this object. The precise meaning 
     * of "copy" may depend on the class of the object.
     *
     * @param cloneFilterChain the flag whether the filter chain should be clonned as well
     * @return a clone of this instance
     * @throws CloneNotSupportedException if the object's class does not support clonning or there was an error
     */
    public LocalAbstractObject clone(boolean cloneFilterChain) throws CloneNotSupportedException {
        LocalAbstractObject rtv = (LocalAbstractObject)super.clone();
        if (cloneFilterChain && rtv.distanceFilter != null)
            rtv.distanceFilter = (PrecomputedDistancesFilter)rtv.distanceFilter.clone();
        else
            rtv.distanceFilter = null;

        // Clone the supplemental data
        if ((suppData != null) && (suppData instanceof Cloneable))
            try {
                rtv.suppData = (BinarySerializable)rtv.suppData.getClass().getMethod("clone").invoke(rtv.suppData);
            } catch (IllegalAccessException e) {
                throw new CloneNotSupportedException(e.toString());
            } catch (NoSuchMethodException e) {
                throw new CloneNotSupportedException(e.toString());
            } catch (InvocationTargetException e) {
                throw new CloneNotSupportedException(e.getCause().toString());
            }
        return rtv;
    }

    /** 
     * Creates and returns a randomly modified copy of this object. 
     * The modification depends on particular subclass implementation.
     *
     * @param args any parameters required by the subclass implementation - usually two objects with 
     *        the miminal and the maximal possible values
     * @return a randomly modified clone of this instance
     * @throws CloneNotSupportedException if the object's class does not support clonning or there was an error
     */
    public abstract LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException;


    //****************** Serialization ******************//

    /**
     * Processes the comment line of text representation of the object.
     * The comment is of format "#typeOfComment comment value". 
     * Recognized types of comments are: <ul>
     *   <li>"#objectKey keyClass key value", where keyClass extends AbstractObjectKey</li>
     *   <li>"#filter filterClass filter value", where filterClass extends </li>
     * </ul>
     * @param line the string with the comment - should start with "#"
     * @return <b>false</b> if <code>line<code/> does not start with "#", <b>true</b> otherwise
     * @throws java.io.IOException if the comment type was recognized but its value is illegal
     */
    protected boolean processObjectComment(String line) throws IOException {
        if (! line.startsWith("#"))
            return false;

        try {
            String[] splitLine = line.split(" ", 3);
            
            if (splitLine[0].equals("#objectKey")) {
                if (splitLine.length < 3)
                    throw new IOException("comment must be of format '#objectKey keyClass key value': "+line);

                // Get key class constructor
                Constructor<AbstractObjectKey> keyConstructor = Convert.getClassForName(splitLine[1], AbstractObjectKey.class)
                        .getConstructor(String.class);
                // Create and set the key 
                setObjectKey(keyConstructor.newInstance(splitLine[2]));
            } else if (splitLine[0].equals("#filter")) {
                if (splitLine.length < 3)
                    throw new IOException("comment must be of format '#filterKey filterClass filter value': "+line);

                // Get key class constructor
                Constructor<PrecomputedDistancesFilter> keyConstructor = Convert.getClassForName(splitLine[1], PrecomputedDistancesFilter.class)
                        .getConstructor(String.class);
                // Create and set the key 
                chainFilter(keyConstructor.newInstance(splitLine[2]), false);
            }
            
        } catch (ClassNotFoundException e) {
            throw new IOException(e.getMessage());
        } catch (NoSuchMethodException e) {
            throw new IOException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IOException(e.getMessage());
        } catch (InstantiationException e) {
            throw new IOException(e.getMessage());
        } catch (InvocationTargetException e) {
            throw new IOException(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        }
        return true;
    }

    /**
     * Writes the object comments and data - key and filters - into an output text stream.
     * Writes the following comments: <ul>
     *   <li>"#objectKey keyClass key value", where keyClass extends AbstractObjectKey</li>
     *   <li>"#filter filterClass filter value", where filterClass extends </li>
     * </ul>
     * The data are stored by a overriden method <code>writeData</code>.
     * 
     * @param stream the stream to write the comments and data to
     * @throws java.io.IOException if any problem occures during comment writing
     */
    public final void write(OutputStream stream) throws IOException {
        write(stream, true);
    }

    /**
     * Writes the object comments and data - key and filters - into an output text stream.
     * Writes the following comments: <ul>
     *   <li>"#objectKey keyClass key value", where keyClass extends AbstractObjectKey</li>
     *   <li>"#filter filterClass filter value", where filterClass extends </li>
     * </ul>
     * The data are stored by a overriden method <code>writeData</code>.
     * 
     * @param stream the stream to write the comments and data to
     * @param writeComments if true then the comments are written
     * @throws java.io.IOException if any problem occures during comment writing
     */
    public final void write(OutputStream stream, boolean writeComments) throws IOException {
        if (writeComments) {
            // write the key as a comment
            if (objectKey != null) {
                stream.write("#objectKey ".getBytes());
                stream.write(objectKey.getClass().getName().getBytes());
                stream.write(' ');
                stream.write(objectKey.getText().getBytes());
                stream.write('\n');
            }

            // write the filters as comments
            PrecomputedDistancesFilter filter = this.distanceFilter;
            while (filter != null) {
                try {
                    String filterText = filter.getText();
                    stream.write("#filter ".getBytes());
                    stream.write(filter.getClass().getName().getBytes());
                    stream.write(' ');
                    stream.write(filterText.getBytes());
                    stream.write('\n');
                } catch (UnsupportedOperationException ignore) { }
                filter = filter.getNextFilter();
            }
        }
        writeData(stream);
    }

    /**
     * Store this object's data to a text stream.
     * This method should have the opposite deserialization in constructor of a given object class.
     *
     * @param stream the stream to store this object to
     * @throws IOException if there was an error while writing to stream
     */
    protected abstract void writeData(OutputStream stream) throws IOException;


    //************ Protected methods of BinarySerializable interface ************//

    /**
     * Creates a new instance of LocalAbstractObject loaded from binary input stream.
     * 
     * @param input the stream to read the LocalAbstractObject from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the stream
     */
    protected LocalAbstractObject(BinaryInputStream input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        suppData = serializator.readObject(input, Object.class);
        distanceFilter = serializator.readObject(input, PrecomputedDistancesFilter.class);
    }

    /**
     * Binary-serialize this object into the <code>output</code>.
     * @param output the output stream this object is binary-serialized into
     * @param serializator the serializator used to write objects
     * @return the number of bytes actually written
     * @throws IOException if there was an I/O error during serialization
     */
    @Override
    protected int binarySerialize(BinaryOutputStream output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator) +
               serializator.write(output, suppData) +
               serializator.write(output, distanceFilter);
    }

    /**
     * Returns the exact size of the binary-serialized version of this object in bytes.
     * @param serializator the serializator used to write objects
     * @return size of the binary-serialized version of this object
     */
    @Override
    protected int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator) + serializator.getBinarySize(suppData) +
                serializator.getBinarySize(distanceFilter);
    }

}
