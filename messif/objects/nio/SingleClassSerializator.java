/*
 *  This file is part of MESSIF library.
 *
 *  MESSIF library is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MESSIF library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MESSIF library.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.objects.nio;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * This is the simple serializator implementation for {@link BinarySerializable} objects.
 * It can store and restore only one specified class or the standard Java-serialized objects.
 * 
 * @param <T> the class of objects created by this serializator during deserialization
 * @see MultiClassSerializator
 * @see CachingSerializator
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class SingleClassSerializator<T> extends BinarySerializator implements Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /** The class of objects created by this serializator during deserialization */
    protected final Class<? extends T> deserializationClass;

    /** The constructor used to create instances of objects during deserialization */
    protected final transient Constructor<? extends T> constructor;

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
    public SingleClassSerializator(Class<? extends T> baseClass) {
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
    public Class<? extends T> getDefaultClass() {
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
     * Reads an instance from the <code>input</code> using this serializator.
     * The {@link #getDefaultClass default} class that is expected to be in the buffer.
     *
     * @param input the buffer to read the instance from
     * @return an instance of the deserialized object
     * @throws IOException if there was an I/O error
     * @throws IllegalArgumentException if the constructor or the factory method has a wrong prototype
     */
    public T readObject(BinaryInput input) throws IOException, IllegalArgumentException {
        return readObject(input, getDefaultClass());
    }

    @Override
    protected int write(BinaryOutput output, BinarySerializable object) throws IOException {
        if (object instanceof JavaToBinarySerializable || deserializationClass.isInstance(object)) {
            return object.binarySerialize(output, this);
        } else {
            throw new IOException("Serializator can't store '" + object.getClass().getName() + "' because it is restricted to '" + deserializationClass.getName() + "'");
        }
    }

    @Override
    protected <E> E readObjectImpl(BinaryInput input, Class<E> expectedClass) throws IOException, IllegalArgumentException {
        return expectedClass.cast(readObject(
                input,
                this,
                constructor,
                factoryMethod
            ));        
    }

    /**
     * Returns the size of the binary-serialized <code>object</code> in bytes.
     * 
     * @param object the object to get the size for
     * @return the size of the binary-serialized <code>object</code>
     * @throws IllegalArgumentException if there was an error using Java standard serialization on the object
     */
    @Override
    protected int getBinarySize(BinarySerializable object) throws IllegalArgumentException {
        return object.getBinarySize(this);
    }


    //****************** Serialization ******************//

    /**
     * Read this serializator from an object stream.
     * @param in the object stream to read the serializator from
     * @throws IOException if there was an I/O error reading the serializator from the stream
     * @throws ClassNotFoundException if there was an error resolving object class
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {        
        try {
            // Proceed with standard deserialization first
            in.defaultReadObject();

            // Get constructor for the base class
            if (BinarySerializable.class.isAssignableFrom(deserializationClass)) {
                // Restore the constructor (set it through reflection to overcome the "final" flag)
                Field field = SingleClassSerializator.class.getDeclaredField("constructor");
                field.setAccessible(true);
                field.set(this, getNativeSerializableConstructor(deserializationClass));

                // Restore the factory method
                field = SingleClassSerializator.class.getDeclaredField("factoryMethod");
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
