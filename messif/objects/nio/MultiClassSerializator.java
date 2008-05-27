/*
 * MultiClassSerializator
 *
 */

package messif.objects.nio;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import messif.utility.Convert;

/**
 * This implements a full-featured {@link BinarySerializator} which works on
 * all classes. There is a cost of the class name and constructor/factory
 * method lookup when the objects are deserialized for other than the default
 * class.
 * 
 * This serializator checks the serialUIDs of the default class for changes.
 * 
 * @author xbatko
 */
public class MultiClassSerializator extends BinarySerializator implements Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /** Constant indicating that the standard Java {@link java.io.Serializable serialization} is used */
    protected static final byte JAVA_SERIALIZATION = -1;
    /** Constant indicating that the default class is written in the stream */
    protected static final byte DEFAULTCLASS_SERIALIZATION = -2;
    /** Constant indicating that the name of the class is written in the stream */
    protected static final byte CLASSNAME_SERIALIZATION = -3;

    /** The first class in the predefinedClasses is used as default */
    protected final Class<?> defaultClass;

    /** The cached constructor for the default class */
    protected final Constructor<?> constructor;

    /** The cached factory method for the default class */
    protected final Method factoryMethod;


    //************************ Constructor ************************//

    /**
     * Creates a new instance of MultiClassSerializator.
     * If the <code>defaultClass</code> implements the {@link BinarySerializable} interface,
     * the constructor/factory method is extracted for deserializing.
     * Otherwise a standard Java serialization will be used.
     * 
     * The constructor/factory method of the <code>defaultClass</code>
     * is cached.
     * 
     * @param defaultClass the default class that is used for deserialization when a class is not specified
     * @throws IllegalArgumentException if there is an invalid value in <code>predefinedClasses</code>
     */
    public MultiClassSerializator(Class<?> defaultClass) throws IllegalArgumentException {
        this.defaultClass = defaultClass;
        if (BinarySerializable.class.isAssignableFrom(defaultClass)) {
            this.constructor = getNativeSerializableConstructor(defaultClass);
            this.factoryMethod = getNativeSerializableFactoryMethod(defaultClass);
            if (constructor == null && factoryMethod == null)
                throw new IllegalArgumentException("Class '" + defaultClass.getName() + "' lacks proper constructor/factory method for BinarySerializable");
        } else {
            this.constructor = null;
            this.factoryMethod = null;
        }
    }


    //************************ Serializator methods ************************//

    /**
     * Returns the size of the binary-serialized <code>object</code> in bytes.
     * 
     * @param object the object to get the size for
     * @return the size of the binary-serialized <code>object</code>
     * @throws IllegalArgumentException if there was an error using Java standard serialization on the object
     */
    @Override
    protected int getBinarySize(BinarySerializable object) throws IllegalArgumentException {
        int size = 1 + object.getBinarySize(this);
        if (getClassIndex(object) == CLASSNAME_SERIALIZATION)
            size += getBinarySize(object.getClass().getName());
        return size;
    }

    /**
     * Writes <code>object</code> to this output stream using binary serialization.
     * Additionally, the index of the cached class or the name of the class is stored.
     * 
     * @param stream the stream to write the object to
     * @param object the object to write
     * @return the number of bytes actually written
     * @throws IOException if there was an error using flushChannel
     */
    @Override
    protected int write(BinaryOutputStream stream, BinarySerializable object) throws IOException {
        byte position = (byte)getClassIndex(object);
        int size = write(stream, position);
        if (position == CLASSNAME_SERIALIZATION) {
            size += write(stream, object.getClass().getName());
            if (log.isLoggable(Level.FINE))
                log.fine("Class " + object.getClass().getName() + " is name-serialized, consider adding it to CachingSerializator");
        }
        size += object.binarySerialize(stream, this);
        return size;
    }

    /**
     * Reads an instance using the proper constructor/factory method.
     * First, a byte flag resolving the type of deserialization is read.
     * If the flag indicates that the class name is stored in the stream, it
     * is read and checked. Then, the constructor and/or factory method is retrieved via
     * {@link #getConstructor} and {@link #getFactoryMethod}.
     * Finally, the object is deserialized using the constructor, the factory method
     * or standard Java {@link java.io.Serializable serialization}.
     * 
     * @param stream the stream to read the instance from
     * @param objectSize the size of the instance in the stream
     * @param expectedClass the class that is expected to be in the stream
     * @return an instance of the deserialized object
     * @throws IOException if there was an error reading from the stream
     * @throws IllegalArgumentException if there was a problem getting a valid constructor/factory method
     */
    @Override
    protected <E> E readObject(BinaryInputStream stream, int objectSize, Class<E> expectedClass) throws IOException, IllegalArgumentException {
        Constructor<?> selectedConstructor;
        Method selectedFactoryMethod;

        // Get the type of deserialization (byte flag)
        byte flag = readByte(stream);
        objectSize--;

        // If flag indicates a stored class name, restore it
        switch (flag) {
            case JAVA_SERIALIZATION:
                selectedConstructor = null;
                selectedFactoryMethod = null;
                break;
            case DEFAULTCLASS_SERIALIZATION:
                selectedConstructor = constructor;
                selectedFactoryMethod = factoryMethod;
                break;
            case CLASSNAME_SERIALIZATION:
                String className = readString(stream);
                objectSize -= getBinarySize(className);
                try {
                    expectedClass = Convert.getClassForName(className, expectedClass);
                    selectedConstructor = getNativeSerializableConstructor(expectedClass);
                    selectedFactoryMethod = getNativeSerializableFactoryMethod(expectedClass);
                    if (selectedConstructor == null && selectedFactoryMethod == null)
                        throw new IllegalArgumentException("Class '" + expectedClass.getName() + "' lacks proper constructor/factory method for BinarySerializable");
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Illegal class read from stream: " + e.getMessage());
                }
                break;
            default:
                selectedConstructor = getConstructor(flag, expectedClass);
                selectedFactoryMethod = getFactoryMethod(flag, expectedClass);
        }

        // Deserialize object
        Object object = readObject(stream, this, objectSize, selectedConstructor, selectedFactoryMethod);

        // Convert to the specified type E
        return Convert.safeGenericCast(object, expectedClass);
    }

    /**
     * Returns a constructor for the specified flag and object class.
     * 
     * @param flag the type of deserialization (see constants)
     * @param objectClass the class that is expected to be in the stream
     * @return a constructor for the specified flag and object class
     * @throws IllegalArgumentException if there was a problem getting the constructor
     */
    protected Constructor<?> getConstructor(byte flag, Class<?> objectClass) throws IllegalArgumentException {
        throw new IllegalArgumentException("Unknown class flag read from stream: " + flag);
    }

    /**
     * Returns a factory method for the specified flag and object class.
     * 
     * @param flag the type of deserialization (see constants)
     * @param objectClass the class that is expected to be in the stream
     * @return a constructor for the specified flag and object class
     * @throws IllegalArgumentException if there was a problem getting the factory method
     */
    protected Method getFactoryMethod(byte flag, Class<?> objectClass) throws IllegalArgumentException {
        throw new IllegalArgumentException("Unknown class flag read from stream: " + flag);
    }

    /**
     * Returns the index of the predefined class for this object.
     * @param object the object whose class is looked up
     * @return the index of the predefined class for this object
     */
    protected int getClassIndex(BinarySerializable object) {
        // Object is a wrapped standard Java serialization
        if (object instanceof JavaToBinarySerializable)
            return JAVA_SERIALIZATION;
        // Object is the default class
        if (defaultClass.equals(object.getClass()))
            return DEFAULTCLASS_SERIALIZATION;

        // Other class
        return CLASSNAME_SERIALIZATION;
    }


    //************************ Overriden methods ************************//

    /**
     * Returns a default class that is used for deserialization when a class is not specified.
     * @return a default class that is used for deserialization
     */
    @Override
    public Class<?> getDefaultClass() {
        return defaultClass;
    }

    /**
     * Returns a hash code value for this serializator. 
     * It is based on the serialVersionUID of the {@link #defaultClass}.
     * @return a hash code value for this serializator
     */
    @Override
    public int hashCode() {
        return getSerialVersionUIDHash(defaultClass);
    }

    /**
     * Indicates whether some other object is "equal to" this serializator.
     * In particular, the other object must be MultiClassSerializator and have
     * the same default class.
     * @param obj the reference object with which to compare
     * @return <code>true</code> if this serializator is the same as the obj
     *          argument; <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MultiClassSerializator))
            return false;
        return defaultClass.equals(((MultiClassSerializator)obj).defaultClass);
    }


    //****************** Serialization ******************//

    /** Read this serializator from the object stream */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {        
        try {
            // Proceed with standard deserialization first
            in.defaultReadObject();

            // Get constructor for the base class
            if (BinarySerializable.class.isAssignableFrom(defaultClass)) {
                // Restore the constructor (set it through reflection to overcome the "final" flag)
                Field field = getClass().getDeclaredField("constructor");
                field.setAccessible(true);
                field.set(this, getNativeSerializableConstructor(defaultClass));

                // Restore the factory method
                field = getClass().getDeclaredField("factoryMethod");
                field.setAccessible(true);
                field.set(this, getNativeSerializableFactoryMethod(defaultClass));
            }
        } catch (NoSuchFieldException e) {
            throw new ClassNotFoundException("This should never happen!", e);
        } catch (IllegalAccessException e) {
            throw new ClassNotFoundException("This should never happen!", e);
        }
    }

}
