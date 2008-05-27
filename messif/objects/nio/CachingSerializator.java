/*
 * CachingSerializator
 *
 */

package messif.objects.nio;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import messif.utility.Convert;

/**
 * This class extends the deserializing support of {@link MultiClassSerializator} with
 * caching. The constructors/factory methods of all the <code>cachedClasses</code>
 * are cached and thus this serializator is quite fast when deserializing from binary stream.
 * If the class is not predefined, but supports {@link BinarySerializable} interface,
 * there will be an additional cost of the class name and constructor/factory method lookup.
 * 
 * This serializator checks the serialUIDs of the cached objects for changes.
 * 
 * @author xbatko
 */
public class CachingSerializator extends MultiClassSerializator {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /** The hash table of cached classes with references to the constructor/factory method lists */
    protected final Map<Class<? extends BinarySerializable>, Integer> cachedClasses;

    /** The list of constructors for the cached classes with the exactly the same order as specified by cachedClasses */
    protected transient List<Constructor<?>> cachedConstructors;

    /** The list of factory methods for the cached classes with the exactly the same order as specified by cachedClasses */
    protected transient List<Method> cachedFactoryMethods;

    /**
     * Creates a new instance of CachingSerializator.
     * The constructors/factory methods of the <code>cachedClasses</code>
     * are cached.
     * 
     * <p>
     * If any of the items of this the list of predefined classes is replaced or deleted,
     * the deserialization of the older binary stream <b>will fail</b>. Appending the
     * list is safe.
     * </p>
     * 
     * @param defaultClass the default class that is used for deserialization when a class is not specified
     * @param cachedClasses the classes that are used frequently and should be cached
     * @throws IllegalArgumentException if there is an invalid value in <code>cachedClasses</code>
     */
    public CachingSerializator(Class<?> defaultClass, Class... cachedClasses) throws IllegalArgumentException {
        super(defaultClass);

        if (cachedClasses == null || cachedClasses.length == 0)
            throw new IllegalArgumentException("At least one class must be specified for BinarySerializator");

        this.cachedClasses = new HashMap<Class<? extends BinarySerializable>, Integer>(cachedClasses.length);
        this.cachedConstructors = new ArrayList<Constructor<?>>(cachedClasses.length);
        this.cachedFactoryMethods = new ArrayList<Method>(cachedClasses.length);

        // Fill the predefined data
        for (Class selClass : cachedClasses) {
            try {
                Class<? extends BinarySerializable> castClass = Convert.genericCastToClass(selClass, BinarySerializable.class);
                this.cachedClasses.put(castClass, addToCache(castClass));
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Class '" + selClass.getName() + "' does not implement BinarySerializable");
            }
        }
    }

    /**
     * Add the specified class to cache.
     * @param classToAdd the class to add
     * @return the position in the cache
     * @throws IllegalArgumentException if the class has invalid constructor/factory method or there is already another class on this position
     */
    protected int addToCache(Class<? extends BinarySerializable> classToAdd) throws IllegalArgumentException {
        // Get the constructor/factory method for the predefined class
        Constructor<?> selConstructor = getNativeSerializableConstructor(classToAdd);
        Method selFactoryMethod = getNativeSerializableFactoryMethod(classToAdd);
        if (selConstructor == null && selFactoryMethod == null)
            throw new IllegalArgumentException("Class '" + classToAdd.getName() + "' lacks proper constructor/factory method for BinarySerializable");

        // Add to the cache
        cachedConstructors.add(selConstructor);
        cachedFactoryMethods.add(selFactoryMethod);
        return cachedConstructors.size() - 1;
    }

    /**
     * Returns a constructor for the specified flag and object class.
     * If the object was serialized as one of the predefined classes, the cached
     * constructor is used.
     * For the {@link #DEFAULTCLASS_SERIALIZATION DEFAULTCLASS} serialization,
     * current constructor is returned. If the stored object implements the
     * {@link BinarySerializable} interface, its constructor is looked up.
     * Otherwise, <tt>null</tt> is returned.
     * 
     * @param flag the type of deserialization (see constants)
     * @param objectClass the class that is expected to be in the stream
     * @return a constructor for the specified flag and object class
     * @throws IllegalArgumentException if there was a problem getting the constructor
     */
    @Override
    protected Constructor<?> getConstructor(byte flag, Class<?> objectClass) throws IllegalArgumentException {
        try {
            return cachedConstructors.get(flag);
        } catch (IndexOutOfBoundsException ignore) {
            throw new IllegalArgumentException("Unknown predefined class index: " + flag);
        }
    }

    /**
     * Returns a factory method for the specified flag and object class.
     * If the object was serialized as one of the predefined classes, the cached
     * factory method is used.
     * For the {@link #DEFAULTCLASS_SERIALIZATION DEFAULTCLASS} serialization,
     * current factory method is returned. If the stored object implements the
     * {@link BinarySerializable} interface, its factory method is looked up.
     * Otherwise, <tt>null</tt> is returned.
     * 
     * @param flag the type of deserialization (see constants)
     * @param objectClass the class that is expected to be in the stream
     * @return a constructor for the specified flag and object class
     * @throws IllegalArgumentException if there was a problem getting the factory method
     */
    @Override
    protected Method getFactoryMethod(byte flag, Class<?> objectClass) throws IllegalArgumentException {
        try {
            return cachedFactoryMethods.get(flag);
        } catch (IndexOutOfBoundsException ignore) {
            throw new IllegalArgumentException("Unknown predefined class index: " + flag);
        }
    }

    /**
     * Returns the index of the predefined class for this object.
     * @param object the object whose class is looked up
     * @return the index of the predefined class for this object
     */
    @Override
    protected int getClassIndex(BinarySerializable object) {
        // Try the super check first
        int index = super.getClassIndex(object);
        if (index != CLASSNAME_SERIALIZATION)
            return index;

        // Cached class
        Integer position = cachedClasses.get(object.getClass());
        if (position != null)
            return position;

        // Other class
        return CLASSNAME_SERIALIZATION;
    }


    //************************ Overriden methods ************************//

    /**
     * Returns a hash code value for this serializator. 
     * It is based on the serialVersionUIDs of the cached classes.
     * @return a hash code value for this serializator
     */
    @Override
    public int hashCode() {
        int value = super.hashCode();
        for (Class<?> cachedClass : cachedClasses.keySet())
            value ^= getSerialVersionUIDHash(cachedClass);
        return value;
    }

    /**
     * Indicates whether some other object is "equal to" this serializator.
     * In particular, the other object must be CachingSerializator and have
     * the same cached classes.
     * @param obj the reference object with which to compare
     * @return <code>true</code> if this serializator is the same as the obj
     *          argument; <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CachingSerializator))
            return false;
        if (!super.equals(obj))
            return false;
        CachingSerializator castObj = (CachingSerializator)obj;
        for (Class<?> cachedClass : cachedClasses.keySet())
            if (!castObj.cachedClasses.containsKey(cachedClass))
                return false;
        return true;
    }


    //****************** Serialization ******************//

    /** Read this serializator from the object stream */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {        
        // Proceed with standard deserialization first
        in.defaultReadObject();

        // Restore cached constructors and factory methods
        cachedConstructors = new ArrayList<Constructor<?>>(cachedClasses.size());
        cachedFactoryMethods = new ArrayList<Method>(cachedClasses.size());

        // Get the list of classes sorted by position
        List<Class<? extends BinarySerializable>> classes = new ArrayList<Class<? extends BinarySerializable>>(cachedClasses.keySet());
        Collections.sort(classes, new Comparator<Class<? extends BinarySerializable>>() {
            public int compare(Class<? extends BinarySerializable> o1, Class<? extends BinarySerializable> o2) {
                return cachedClasses.get(o1) - cachedClasses.get(o2);
            }
        });

        // Restore cache
        for (Class<? extends BinarySerializable> cls : classes)
            addToCache(cls);
    }
}
