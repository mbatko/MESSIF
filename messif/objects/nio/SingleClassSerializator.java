/*
 * SingleClassSerializator
 *
 */

package messif.objects.nio;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import messif.utility.Convert;

/**
 * This is the simple serializator implementation for {@link BinarySerializable} objects.
 * It can store and restore only one specified class or the standard Java-serialized objects.
 * 
 * @param E the class of objects created by this serializator during deserialization
 * @see MultiClassSerializator
 * @see CachingSerializator
 * @author xbatko
 */
public class SingleClassSerializator<E> extends BinarySerializator implements Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /** The class of objects created by this serializator during deserialization */
    protected final Class<? extends E> deserializationClass;

    /** The constructor used to create instances of objects during deserialization */
    protected final transient Constructor<? extends E> constructor;

    /** The factory method used to create instances of objects during deserialization */
    protected final transient Method factoryMethod;


    //************************ Constructor ************************//

    /**
     * Create a new instance of BinarySerializator.
     * If the <code>baseClass</code> implements the {@link BinarySerializable} interface,
     * the constructor/factory method is extracted for deserializing.
     * Otherwise a standard Java serialization will be used.
     * 
     * @param baseClass the class of objects created by this serializator during deserialization
     */
    public SingleClassSerializator(Class<? extends E> baseClass) {
        this.deserializationClass = baseClass;

        // Get constructor for the base class
        if (BinarySerializable.class.isAssignableFrom(baseClass)) {
            constructor = getNativeSerializableConstructor(baseClass);
            factoryMethod = getNativeSerializableFactoryMethod(baseClass);
        } else {
            constructor = null;
            factoryMethod = null;
        }
    }


    //************************ Overriden methods ************************//

    /**
     * Returns a default class that is used for deserialization when a class is not specified.
     * @return a default class that is used for deserialization
     */
    @Override
    public Class<?> getDefaultClass() {
        return deserializationClass;
    }

    /**
     * Returns a hash code value for this serializator. 
     * It is based on the {@link #deserializationClass} name.
     * @return a hash code value for this serializator
     */
    @Override
    public int hashCode() {
        return getSerialVersionUIDHash(deserializationClass);
    }

    /**
     * Indicates whether some other object is "equal to" this serializator.
     * It is based on the {@link #deserializationClass} name.
     * @param obj the reference object with which to compare
     * @return <code>true</code> if this serializator is the same as the obj
     *          argument; <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SingleClassSerializator))
            return false;
        return getSerialVersionUIDHash(deserializationClass) == getSerialVersionUIDHash(((SingleClassSerializator)obj).deserializationClass);
    }


    //************************ Serializator methods ************************//

    /**
     * Writes <code>object</code> to this output stream using binary serialization.
     * 
     * @param stream the stream to write the object to
     * @param object the object to write
     * @return the number of bytes actually written
     * @throws IOException if there was an error using flushChannel
     */
    protected int write(BinaryOutputStream stream, BinarySerializable object) throws IOException {
        if (object instanceof JavaToBinarySerializable || deserializationClass.isInstance(object)) {
            return object.binarySerialize(stream, this);
        } else {
            throw new IOException("Serializator can't store '" + object.getClass().getName() + "' because it is restricted to '" + deserializationClass.getName() + "'");
        }
    }

    /**
     * Read an instance using the default constructor/factory method of this serializator.
     *
     * @param stream the stream to read the instance from
     * @param objectSize the size of the instance in the stream
     * @return an instance of the deserialized object
     * @throws IOException if there was an error reading from the stream
     * @throws IllegalArgumentException if the constructor or the factory method has a wrong prototype
     */
    protected <E> E readObject(BinaryInputStream stream, int objectSize, Class<E> expectedClass) throws IOException, IllegalArgumentException {
        return Convert.safeGenericCast(readObject(
                stream,
                this,
                objectSize,
                constructor,
                factoryMethod
            ), expectedClass);        
    }

    /**
     * Returns the size of the binary-serialized <code>object</code> in bytes.
     * 
     * @param object the object to get the size for
     * @return the size of the binary-serialized <code>object</code>
     * @throws IllegalArgumentException if there was an error using Java standard serialization on the object
     */
    protected int getBinarySize(BinarySerializable object) throws IllegalArgumentException {
        return object.getBinarySize(this);
    }


    //****************** Serialization ******************//

    /** Read this serializator from the object stream */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {        
        try {
            // Proceed with standard deserialization first
            in.defaultReadObject();

            // Get constructor for the base class
            if (BinarySerializable.class.isAssignableFrom(deserializationClass)) {
                // Restore the constructor (set it through reflection to overcome the "final" flag)
                Field field = getClass().getDeclaredField("constructor");
                field.setAccessible(true);
                field.set(this, getNativeSerializableConstructor(deserializationClass));

                // Restore the factory method
                field = getClass().getDeclaredField("factoryMethod");
                field.setAccessible(true);
                field.set(this, getNativeSerializableFactoryMethod(deserializationClass));
            }
        } catch (NoSuchFieldException e) {
            throw new ClassNotFoundException("This should never happen!", e);
        } catch (IllegalAccessException e) {
            throw new ClassNotFoundException("This should never happen!", e);
        }
    }

}
