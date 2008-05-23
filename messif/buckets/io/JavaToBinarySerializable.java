/*
 * JavaToBinarySerializable
 * 
 */

package messif.buckets.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import messif.utility.Convert;

/**
 * This is a helper class to provide the {@link BinarySerializable} wrapping
 * of the native {@link java.io.Serializable serialization} of Java.
 * 
 * <p>
 * This class also offers helper factory methods for working with {@link BinarySerializable}
 * classes.
 * </p>
 * 
 * @author xbatko
 */
public class JavaToBinarySerializable extends ByteArrayOutputStream implements BinarySerializable {

    /**
     * Creates an instance of a {@link java.io.Serializable serialized} version of the <code>object</code>.
     * 
     * @param object the object from which to create a serialized version
     * @throws IOException if there was an I/O error during serialization
     */
    public JavaToBinarySerializable(Serializable object) throws IOException {
        ObjectOutputStream objectStream = new ObjectOutputStream(this);
        objectStream.writeObject(object);
        objectStream.close();
    }

    public void binarySerialize(NativeDataOutput output) throws IOException {
        output.write(buf, 0, count);
    }

    public int getSize() {
        return size();
    }


    //************************ Factory method for deserializing ************************//

    /**
     * Deserialize a previously {@link java.io.Serializable stored} object from binary data input.
     * @param input the input from which to read the object
     * @param dataSize the size of the serialized data
     * @return the previously {@link java.io.Serializable stored} object
     * @throws IOException if there was an I/O error during deserialization
     */
    public static Serializable binaryDeserialize(NativeDataInput input, int dataSize) throws IOException {
        try {
            byte[] buffer = new byte[dataSize];
            input.read(buffer, 0, dataSize);
            return (Serializable)new ObjectInputStream(new ByteArrayInputStream(buffer)).readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e.toString());
        }
    }


    //************************ Helper functions for binary deserializing ************************//

    /**
     * Returns a native-serializable constructor for <code>objectClass</code>.
     * The constructor should have the following prototype:
     * <pre>
     *      <i>ClassConstructor</i>({@link NativeDataInput} input, int dataSize) throws {@link IOException}
     * </pre>
     * 
     * @param objectClass the object class to construct
     * @return a constructor for <code>objectClass</code>
     * @throws NoSuchMethodException if there is no native-serializable constructor
     */
    public static <E> Constructor<E> getNativeSerializableConstructor(Class<E> objectClass) throws NoSuchMethodException {
        Constructor<E> constructor = objectClass.getDeclaredConstructor(NativeDataInput.class, int.class);
        constructor.setAccessible(true);
        return constructor;
    }

    /**
     * Returns a native-serializable factory method for <code>objectClass</code>.
     * The factory method should have the following prototype:
     * <pre>
     *      <i>ObjectClass</i> binaryDeserialize({@link NativeDataInput} input, int dataSize) throws {@link IOException}
     * </pre>
     * 
     * @param objectClass the object class to construct
     * @return a factory method for <code>objectClass</code>
     * @throws NoSuchMethodException if there is no native-serializable factory method
     */
    public static Method getNativeSerializableFactoryMethod(Class<?> objectClass) throws NoSuchMethodException {
        Method method = objectClass.getDeclaredMethod("binaryDeserialize", NativeDataInput.class, int.class);
        if (!Modifier.isStatic(method.getModifiers()))
            throw new NoSuchMethodException("The binaryDeserialize method must be static");
        method.setAccessible(true);
        return method;
    }

    /**
     * Creates a new instance of the <code>objectClass</code> using a native-serializable
     * constructor or factory method.
     * 
     * @param objectClass the object class to construct
     * @param input a native input from which to get the data for the new instance
     * @param dataSize the size of the data for the new instance
     * @return an instance of <code>objectClass</code>
     * @throws NoSuchMethodException if there is no valid factory method
     * @throws IllegalArgumentException if the factory method throws an incorrect exception or has an incorrect return type
     * @throws IOException if there was an I/O error while creating the object instance
     */
    public static <E> E newNativeSerializable(Class<E> objectClass, NativeDataInput input, int dataSize) throws IllegalArgumentException, NoSuchMethodException, IOException {
        // Get constructor or factory method for the objectClass
        Object constructorOrMethod;
        try {
            constructorOrMethod = getNativeSerializableConstructor(objectClass);
        } catch (NoSuchMethodException ignore) {
            constructorOrMethod = getNativeSerializableFactoryMethod(objectClass);
        }

        // Create the instance
        try {
            return Convert.safeGenericCast(constructNativeSerializable(constructorOrMethod, input, dataSize), objectClass);
        } catch (ClassCastException e) {
            // The factory method has a wrong return value
            throw new IllegalArgumentException("The factory method for '" + objectClass.getName() + "' returns invalid class: " + e.getMessage());
        }
    }

    /**
     * Creates a new instance from the <code>dataSize</code> bytes of the <code>input</code> using
     * the <code>constructorOrFactory</code>. The constructor should have the following prototype:
     * <pre>
     *      <i>ClassConstructor</i>({@link NativeDataInput} input, int dataSize) throws {@link IOException}
     * </pre>
     * 
     * @param constructorOrFactory the constructor or factory method for the object to create from the data input
     * @param input a native input from which to get the data for the new instance
     * @param dataSize the size of the data for the new instance
     * @return an instance of <code>objectClass</code>
     * @throws IllegalArgumentException if the <code>objectClass</code>'s constructor is abstract,
     *              there was an unknown exception thrown during the construction, or
     *              the provided constructor has a wrong prototype
     * @throws IOException if there was an I/O error while creating the object instance
     * @throws ClassCastException if the <code>constructorOrFactory<code> is neither {@link Constructor} nor {@link Method}
     */
    protected static Object constructNativeSerializable(Object constructorOrFactory, NativeDataInput input, int dataSize) throws IllegalArgumentException, ClassCastException, IOException {
        try {
            if (constructorOrFactory instanceof Constructor)
                return ((Constructor)constructorOrFactory).newInstance(input, dataSize);
            else
                return ((Method)constructorOrFactory).invoke(null, input, dataSize);
        } catch (InstantiationException e) {
            // The provided class is abstract
            throw new IllegalArgumentException(e.getMessage());
        } catch (IllegalAccessException e) {
            // The constructor/factory should be "accessible"
            throw new IllegalArgumentException(e.toString());
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException)
                throw (IOException)cause;
            if (cause instanceof RuntimeException)
                throw (RuntimeException)cause;
            throw new IllegalArgumentException("Illegal exception thrown during binary deserialization", cause);
        }
    }

}
