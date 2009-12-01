/*
 * Convert.java
 *
 * Created on 2. prosinec 2005, 21:05
 */

package messif.utility;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import messif.objects.util.AbstractObjectList;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractStreamObjectIterator;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class that provides methods for type conversions and instantiation.
 * 
 * @author xbatko
 */
public abstract class Convert {
    
    /**
     * Converts a string into object of the specified type.
     * <p>
     * Currently supported types are:
     * <ul>
     *   <li>all primitive types (int, long, boolean, double, byte, float, short, char)</li>
     *   <li>all object wrappers for primitive types</li>
     *   <li>{@link String}</li>
     *   <li>{@link Class}</li>
     *   <li>{@link AbstractStreamObjectIterator} - parameter represents the name of an opened stream from <code>objectStreams</code></li>
     *   <li>{@link AbstractObjectList} - parameter represents the name of an opened stream from <code>objectStreams</code>, the number of objects to read can be specified after a colon</li>
     *   <li>{@link LocalAbstractObject} - parameter represents the name of an opened stream from <code>objectStreams</code>, the next object is acquired</li>
     *   <li>static array of any "convertible" element type - parameter should be comma-separated values that will be converted using {@link #stringToType} into the array's items</li>
     *   <li>{@link Map} with {@link String} key and value - parameter should be comma-separated key=value pairs (possibly quoted)</li>
     *   <li>any class with a public constructor that has a single {@link String} parameter</li>
     *   <li>any class with a <code>valueOf(String <i>parameter</i>)</code> factory method, e.g., {@link messif.network.NetworkNode#valueOf}</li>
     * </ul>
     * </p>
     * 
     * @param <E> the type of the value to return
     * @param string the string value to be converted
     * @param type the class of the value
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @return the converted value
     * @throws InstantiationException if the type cannot be created from the string value
     */
    @SuppressWarnings("unchecked")
    public static <E> E stringToType(String string, Class<E> type, Map<String, Object> namedInstances) throws InstantiationException {
        if (string.equals("null"))
            return null;
        
        // Converting string types
        if (type == String.class)
            return (E)string; // This cast IS checked

        // Converting class types
        if (type == Class.class) try {
            return (E)Class.forName(string); // This cast IS checked
        } catch (ClassNotFoundException e) {
            throw new InstantiationException(e.toString());
        }

        // Converting map types
        if (type == Map.class) {
            Map<String, Object> rtv = new HashMap<String, Object>();
            putStringIntoMap(string, rtv, String.class);
            // Add streams parameter to a Map that contain a 'namedInstances' key but it is null
            if (rtv.containsKey("namedInstances") && rtv.get("namedInstances") == null)
                rtv.put("namedInstances", namedInstances);
            return (E)rtv; // This cast IS checked
        }

        // Converting static arrays
        if (type.isArray()) {
            String[] items = string.split("\\p{Space}*,\\p{Space}*");
            Class<?> componentType = type.getComponentType();
            Object array = Array.newInstance(componentType, items.length);
            for (int i = 0; i < items.length; i++)
                Array.set(array, i, stringToType(items[i], componentType, namedInstances));
            return (E)array; // This cast IS checked
        }

        // Wrap primitive types, so that their 'valueOf' method can be used
        if (type.isPrimitive())
            type = wrapPrimitiveType(type);

        // Named instances of objects
        if (namedInstances != null) {
            Object instance = namedInstances.get(string);
            if (instance != null) {
                // Return named object as-is
                if (type.isInstance(instance))
                    return (E)instance; // This cast IS checked

                // Try the LocalAbstractObject iterators
                if (type.isAssignableFrom(LocalAbstractObject.class))
                    return (E)((AbstractStreamObjectIterator<?>)instance).next();
            }
        }

        // Try string public constructor
        if (!Modifier.isAbstract(type.getModifiers())) {
            try {
                return type.getConstructor(String.class).newInstance(string);
            } catch (InvocationTargetException e) {
                // This string is unconvertible, because some exception arised
                throw new InstantiationException(e.getCause().toString());
            } catch (IllegalAccessException e) {
                // Can't access constructor, this should never happen since the constructor is public
            } catch (NoSuchMethodException e) {
                // Method not found, but never mind, other conversions might be possible...
            }
        }

        // Try the static valueOf method of a primitive type
        try {
            Method method = type.getMethod("valueOf", String.class);
            if (Modifier.isStatic(method.getModifiers()))
                return (E)method.invoke(null, string); // This cast IS checked
        } catch (InvocationTargetException e) {
            // This string is unconvertible, because some exception arised
            throw new InstantiationException(e.getCause().toString());
        } catch (IllegalAccessException e) {
            // Can't access valueOf, but never mind, other conversions might be possible...
        } catch (NoSuchMethodException e) {
            // Method not found, but never mind, other conversions might be possible...
        }
        
        throw new InstantiationException("String '" + string + "' cannot be converted to '" + type.getName() + "'");
    }

    /**
     * Converts a string into object of the specified type.
     * @param <E> the type of the value to return
     * @param string the string value to be converted
     * @param type the class of the value
     * @return the converted value
     * @throws InstantiationException if the type cannot be created from the string value
     */
    public static <E> E stringToType(String string, Class<E> type) throws InstantiationException {
        return stringToType(string, type, null);
    }

    /**
     * Returns a wrapper class for primitive type.
     * If the type is not primitive, it is returned as is.
     * @param <T> a primitive type class
     * @param type a primitive type class
     * @return a wrapper class for primitive type
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> wrapPrimitiveType(Class<T> type) {
        if (!type.isPrimitive())
            return type;
        if (type == Integer.TYPE)
            return (Class<T>)Integer.class; // This cast IS checked
        if (type == Long.TYPE)
            return (Class<T>)Long.class; // This cast IS checked
        if (type == Boolean.TYPE)
            return (Class<T>)Boolean.class; // This cast IS checked
        if (type == Double.TYPE)
            return (Class<T>)Double.class; // This cast IS checked
        if (type == Byte.TYPE)
            return (Class<T>)Byte.class; // This cast IS checked
        if (type == Float.TYPE)
            return (Class<T>)Float.class; // This cast IS checked
        if (type == Short.TYPE)
            return (Class<T>)Short.class; // This cast IS checked
        if (type == Character.TYPE)
            return (Class<T>)Character.class; // This cast IS checked
        throw new InternalError("Unknown primitive type");
    }

    /**
     * Parses string key-value pairs from the specified string and adds them to the map.
     * String contains key=value pairs (key, value or both can be quoted) that are separated by commas.
     * For example:
     * <pre>one = 1, "two"=2,"three"="3", four=null</pre>
     * <p>
     * The values are converted using the {@link #stringToType(java.lang.String, java.lang.Class) stringToType}
     * method to the specified <code>valueType</code>.
     * </p>
     *
     * @param <E> the class of values in the map
     * @param string the string value to be converted
     * @param map a table to which the string key-value pairs are added
     * @param valueType the class of values in the map
     * @throws InstantiationException if the conversion of a value has failed
     */
    public static <E> void putStringIntoMap(String string, Map<? super String, ? super E> map, Class<E> valueType) throws InstantiationException {
        Matcher m = Pattern.compile("\\p{Space}*(\"([^\"]*)\"|[^=]*?)\\p{Space}*=\\p{Space}*(\"([^\"]*)\"|[^=]*?)\\p{Space}*(,|$)").matcher(string);
        while (m.find())
            map.put(
                (m.group(2) == null)?m.group(1):m.group(2), // Key
                stringToType((m.group(4) == null)?m.group(3):m.group(4), valueType) // Converted value
            );
    }

    /**
     * Returns a map of string key-value pairs parsed from the specified string.
     * String contains key=value pairs (key, value or both can be quoted) that are separated by commas.
     * For example:
     * <pre>one = 1, "two"=2,"three"="3", four=null</pre>
     * <p>
     * The values are converted using the {@link #stringToType(java.lang.String, java.lang.Class) stringToType}
     * method to the specified <code>valueType</code>.
     * </p>
     *
     * @param <E> the class of values in the map
     * @param string the string value to be converted
     * @param valueType the class of values in the map
     * @throws InstantiationException if the conversion of a value has failed
     * @return a map of string key-value pairs
     */
    public static <E> Map<String, E> stringToMap(String string, Class<E> valueType) throws InstantiationException {
        Map<String, E> rtv = new HashMap<String, E>();
        putStringIntoMap(string, rtv, valueType);
        return rtv;
    }

    /**
     * Returns a map of string key-value pairs parsed from the specified string.
     * String contains key=value pairs (key, value or both can be quoted) that are separated by commas.
     * For example:
     * <pre>one = 1, "two"=2,"three"="3", four=null</pre>
     * @param string the string value to be converted
     * @return a map of string key-value pairs
     */
    public static Map<String, String> stringToMap(String string) {
        try {
            return stringToMap(string, String.class);
        } catch (InstantiationException thisShouldNeverHappen) {
            throw new InternalError();
        }
    }

    /**
     * Returns a random value from the interval between minVal and maxVal.
     * The minVal and maxVal must be primitive types (or their wrapper classes).
     *
     * @param minVal the minimal value (interval's lower bound)
     * @param maxVal the maximal value (interval's upper bound)
     * @return a random value from the interval between minVal and maxVal
     */
    public static Object getRandomValue(Object minVal, Object maxVal) {
        // Get the primitive type to process (and check min/max values to be the same)
        Class<?> type = minVal.getClass();
        if (!type.equals(maxVal.getClass()))
            throw new IllegalArgumentException("Minimal and maximal values have different types");

        // Prepare random value
        double randomValue = Math.random();
        
        if (type == Integer.TYPE || type.equals(Integer.class)) {
            int minValue = (Integer)minVal;
            return minValue + (int)(randomValue*((Integer)maxVal - minValue));
        }
        if (type == Long.TYPE || type.equals(Long.class)) {
            long minValue = (Long)minVal;
            return minValue + (long)(randomValue*((Long)maxVal - minValue));
        }
        if (type == Short.TYPE || type.equals(Short.class)) {
            short minValue = (Short)minVal;
            return minValue + (short)(randomValue*((Short)maxVal - minValue));
        }
        if (type == Byte.TYPE || type.equals(Byte.class)) {
            byte minValue = (Byte)minVal;
            return minValue + (byte)(randomValue*((Byte)maxVal - minValue));
        }
        if (type == Double.TYPE || type.equals(Double.class)) {
            double minValue = (Double)minVal;
            return minValue + randomValue*((Double)maxVal - minValue);
        }
        if (type == Float.TYPE || type.equals(Float.class)) {
            float minValue = (Float)minVal;
            return minValue + (float)(randomValue*((Float)maxVal - minValue));
        }
        
        throw new IllegalArgumentException("Unsupported type for random value generating method");
    }

    /**
     * Returns the number of bits used to represent the specified primitive class.
     * @param type the primitive type for which to get the size
     * @return the number of bits used to represent the specified primitive class
     * @throws IllegalArgumentException if the specified class is not a primitive type
     */
    public static int getPrimitiveTypeSize(Class<?> type) throws IllegalArgumentException {
        if (type == Integer.TYPE || type.equals(Integer.class))
            return Integer.SIZE;
        else if (type == Float.TYPE || type.equals(Float.class))
            return Float.SIZE;
        else if (type == Long.TYPE || type.equals(Long.class))
            return Short.SIZE;
        else if (type == Short.TYPE || type.equals(Short.class))
            return Short.SIZE;
        else if (type == Byte.TYPE || type.equals(Byte.class))
            return Byte.SIZE;
        else if (type == Double.TYPE || type.equals(Double.class))
            return Double.SIZE;
        else if (type == Character.TYPE || type.equals(Character.class))
            return Character.SIZE;
        else if (type == Boolean.TYPE || type.equals(Boolean.class))
            return 1;
            
        throw new IllegalArgumentException("Class " + type.getName() + " doesn't represent a primitive type");
    }

    /**
     * Write a value of a primitive type (or string) to the specified output stream.
     * @param stream the stream to write the data to
     * @param value the value to write to the stream
     * @param type the type of the data to be written
     * @throws IOException if there was an error writing to the stream
     */
    public static void writePrimitiveTypeToDataStream(ObjectOutputStream stream, Object value, Class<?> type) throws IOException {
        if (type == Integer.TYPE || type.equals(Integer.class))
            stream.writeInt((Integer)value);
        else if (type == Float.TYPE || type.equals(Float.class))
            stream.writeFloat((Float)value);
        else if (type == Long.TYPE || type.equals(Long.class))
            stream.writeLong((Long)value);
        else if (type == Short.TYPE || type.equals(Short.class))
            stream.writeShort((Short)value);
        else if (type == Byte.TYPE || type.equals(Byte.class))
            stream.writeByte((Byte)value);
        else if (type == Double.TYPE || type.equals(Double.class))
            stream.writeDouble((Double)value);
        else if (type == Character.TYPE || type.equals(Character.class))
            stream.writeChar((Character)value);
        else if (type == Boolean.TYPE || type.equals(Boolean.class))
            stream.writeBoolean((Boolean)value);
        else if (type.equals(String.class))
            stream.writeUTF((String)value);
        else
            throw new IllegalArgumentException("Class " + type.getName() + " doesn't represent a primitive type");
    }

    /**
     * Read a value of a primitive type (or string) from the specified input stream.
     * @param stream the stream to read the data from
     * @param type the type of data to be read
     * @return the value read from the stream
     * @throws IOException if there was an error writing to the stream
     */
    public static Object readPrimitiveTypeFromDataStream(ObjectInputStream stream, Class<?> type) throws IOException {
        if (type == Integer.TYPE || type.equals(Integer.class))
            return stream.readInt();
        else if (type == Float.TYPE || type.equals(Float.class))
            return stream.readFloat();
        else if (type == Long.TYPE || type.equals(Long.class))
            return stream.readLong();
        else if (type == Short.TYPE || type.equals(Short.class))
            return stream.readShort();
        else if (type == Byte.TYPE || type.equals(Byte.class))
            return stream.readByte();
        else if (type == Double.TYPE || type.equals(Double.class))
            return stream.readDouble();
        else if (type == Character.TYPE || type.equals(Character.class))
            return stream.readChar();
        else if (type == Boolean.TYPE || type.equals(Boolean.class))
            return stream.readBoolean();
        else if (type.equals(String.class))
            return stream.readUTF();
        else
            throw new IllegalArgumentException("Class " + type.getName() + " doesn't represent a primitive type");
    }

    /**
     * Class loader with type check.
     * @param <E> the type of the returned class
     * @param name the fully qualified name of the class
     * @param checkClass the superclass of the returned class for the generics check
     * @return the <code>Class</code> object associated with the class or
     *         interface with the given string name
     * @throws ClassNotFoundException if the class cannot be located
     */
    @SuppressWarnings("unchecked")
    public static <E> Class<E> getClassForName(String name, Class<E> checkClass) throws ClassNotFoundException {
        Class rtv = Class.forName(name);
        if (checkClass.isAssignableFrom(rtv))
            return (Class<E>)rtv; // This cast IS checked on the previous line
        throw new ClassNotFoundException("Class '" + name + "' is not subclass of " + checkClass.getName());
    }

    /**
     * Cast the provided object to Class with generics typing.
     * If the generics type check fails, the <code>ClassCastException</code> is thrown
     * even if the provided <code>classObject</code> is a valid <code>Class</code>.
     * 
     * @param <E> the type of the returned object
     * @param classObject the class object to be cast
     * @param checkClass the generics typed class that is returned
     * @return the generics-typed <code>Class</code> object
     * @throws ClassCastException if passed <code>classObject</code> is not subclass of <code>checkClass</code>
     */
    @SuppressWarnings("unchecked")
    public static <E> Class<E> genericCastToClass(Object classObject, Class<E> checkClass) throws ClassCastException {
        if (classObject == null)
            return null;

        Class<E> rtv = (Class<E>)classObject; // This cast IS checked on the next line
        if (checkClass.isAssignableFrom(rtv))
            return rtv;
        throw new ClassCastException("Class '" + classObject + "' is not subclass of " + checkClass.getName());
    }
    
    /**
     * Parses array of strings into array of objects accoring to the types provided in the second argument.
     * Only parameters from <code>argStartIndex</code> to <code>argEndIndex</code> of <code>strings</code>
     * array will be used.
     * 
     * @param strings array of strings that hold the values
     * @param types array of classes that the strings should be converted to
     * @param argStartIndex index in the strings array which denotes the first changable argument
     * @param argEndIndex index in the strings array which denotes the last changable argument
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @return the array of converted values
     * @throws InstantiationException if there was a type that cannot be created from the provided string value
     */
    public static Object[] parseTypesFromString(String[] strings, Class<?>[] types, int argStartIndex, int argEndIndex, Map<String, Object> namedInstances) throws InstantiationException {
        // Create return array
        Object[] rtv = new Object[types.length];
        
        // VarArg handling (last type is array)
        if (types.length > 0 && types[types.length - 1].isArray()) {
            // Get class of the varargs (i.e. the class of the array's component
            Class<?> varargClass = types[types.length - 1].getComponentType();
            // Create new instance of the array
            rtv[types.length - 1] = Array.newInstance(varargClass, argEndIndex - argStartIndex + 1 - (types.length - 1));
            // Fill array items with conversion
            for (int i = argEndIndex - argStartIndex - (types.length - 1); i >= 0; i--, argEndIndex--)
                Array.set(rtv[types.length - 1], i, stringToType(strings[argEndIndex], varargClass, namedInstances));
        }

        // Convert every string to a proper class
        for (int i = 0; argStartIndex <= argEndIndex; argStartIndex++, i++)
            rtv[i] = stringToType(strings[argStartIndex], types[i], namedInstances);
        
        return rtv;
    }
    
    /**
     * Parses array of strings into array of objects accoring to the types provided in the second argument.
     * Only parameters from <code>argStartIndex</code> to <code>argEndIndex</code> of <code>strings</code>
     * array will be used.
     * 
     * <p>
     * Note that {@link messif.objects.LocalAbstractObject} parameters will not be converted. For this fuctionality,
     * use the full {@link #parseTypesFromString parseTypesFromString} method instead.
     * </p>
     * 
     * @param strings array of strings that hold the values
     * @param types array of classes that the strings should be converted to
     * @param argStartIndex index in the strings array which denotes the first changable argument
     * @param argEndIndex index in the strings array which denotes the last changable argument
     * @return the array of converted values
     * @throws InstantiationException if there was a type that cannot be created from the provided string value
     */
    public static Object[] parseTypesFromString(String[] strings, Class<?>[] types, int argStartIndex, int argEndIndex) throws InstantiationException {
        return parseTypesFromString(strings, types, argStartIndex, argEndIndex, null);
    }

    /**
     * Parses array of strings into array of objects accoring to the types provided in the second argument.
     * All the parameters from <code>argStartIndex</code> till the end of <code>strings</code> array will 
     * be used.
     * 
     * <p>
     * Note that {@link messif.objects.LocalAbstractObject} parameters will not be converted. For this fuctionality,
     * use the full {@link #parseTypesFromString parseTypesFromString} method instead.
     * </p>
     * 
     * @param strings array of strings that hold the values
     * @param types array of classes that the strings should be converted to
     * @param argStartIndex index in the strings array which denotes the first changable argument
     * @return the array of converted values
     * @throws InstantiationException if there was a type that cannot be created from the provided string value
     */
    public static Object[] parseTypesFromString(String[] strings, Class<?>[] types, int argStartIndex) throws InstantiationException {
        return parseTypesFromString(strings, types, argStartIndex, strings.length - 1);
    }

    /**
     * Parses array of strings into array of objects accoring to the types provided in the second argument.
     * All the parameters from <code>argStartIndex</code> till the end of <code>strings</code> array will 
     * be used.
     * 
     * @param strings array of strings that hold the values
     * @param types array of classes that the strings should be converted to
     * @param argStartIndex index in the strings array which denotes the first changable argument
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @return the array of converted values
     * @throws InstantiationException if there was a type that cannot be created from the provided string value
     */
    public static Object[] parseTypesFromString(String[] strings, Class<?>[] types, int argStartIndex, Map<String, Object> namedInstances) throws InstantiationException {
        return parseTypesFromString(strings, types, argStartIndex, strings.length - 1, namedInstances);
    }
    
    /**
     * Parses array of strings into array of objects accoring to the types provided in the second argument.
     * All the parameters from <code>strings</code> array will be used.
     * 
     * <p>
     * Note that {@link messif.objects.LocalAbstractObject} parameters will not be converted. For this fuctionality,
     * use the full {@link #parseTypesFromString parseTypesFromString} method instead.
     * </p>
     * 
     * @param strings array of strings that hold the values
     * @param types array of classes that the strings should be converted to
     * @return the array of converted values
     * @throws InstantiationException if there was a type that cannot be created from the provided string value
     */
    public static Object[] parseTypesFromString(String[] strings, Class<?>[] types) throws InstantiationException {
        return parseTypesFromString(strings, types, 0);
    }

    /**
     * Parses array of strings into array of objects accoring to the types provided in the second argument.
     * All the parameters from <code>strings</code> array will be used.
     * 
     * @param strings array of strings that hold the values
     * @param types array of classes that the strings should be converted to
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @return the array of converted values
     * @throws InstantiationException if there was a type that cannot be created from the provided string value
     */
    public static Object[] parseTypesFromString(String[] strings, Class<?>[] types, Map<String, Object> namedInstances) throws InstantiationException {
        return parseTypesFromString(strings, types, 0, namedInstances);
    }
    
    /**
     * Retrieves types of the objects in parameters 
     * @param objects list of objects to get the types for
     * @return list of types of the arguments
     */
    public static Class<?>[] getObjectTypes(Object... objects) {
        Class<?>[] rtv = new Class<?>[objects.length];
        for (int i = 0; i < objects.length; i++)
            rtv[i] = objects[i].getClass();
        
        return rtv;
    }
    
    /**
     * Creates a new instance of a class using string arguments for its constructor.
     * The list of constructors of the desired class must be provided as <code>constructors</code> argument.
     * The constructors are tried one by one from this list and if the <code>arguments</code>
     * are convertible to the arguments of that constructor, a new instance is created.
     * <p>
     * Note that only constructors with the number of arguments equal to <code>argEndIndex - argStartIndex + 1</code>
     * are tried. If there are several constructors with the same number of arguments, the first (in the order
     * of the list) constructor that succeeds in converting string arguments will be used.
     * </p>
     * <p>
     * Note also that only types convertible by {@link #stringToType} method can be used in constructors.
     * </p>
     *
     * @param <E> the type of the instantiated object
     * @param constructors the list of constructors of the desired class to try
     * @param arguments the string arguments for the constructor that will be converted to correct types
     * @param argStartIndex index in the string arguments array from which to expect arguments (all the previous items are ignored)
     * @param argEndIndex index in the string arguments array to which to expect arguments (all the following items are ignored)
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @return a new instance of the class the constructors were specified for
     * @throws InvocationTargetException
     *              if the constructor can't be found for the specified arguments,
     *              the argument string-to-type convertion has failed or
     *              there was an error during instantiation
     */
    public static <E> E createInstanceWithStringArgs(List<Constructor<E>> constructors, String[] arguments, int argStartIndex, int argEndIndex, Map<String, Object> namedInstances) throws InvocationTargetException {
        InstantiationException lastException = null;
        Constructor<E> lastConstructor = null;
        
        // Search for proper constructor
        for (Constructor<E> constructor : constructors) {
            try {
                // Get constructor parameter types
                Class<?>[] argTypes = constructor.getParameterTypes();
                
                // Skip constructors with different number of parameters
                if (argTypes.length != argEndIndex - argStartIndex + 1)
                    // Test variable number of arguments parameter (last argument is an array)
                    if (argTypes.length == 0 || !argTypes[argTypes.length - 1].isArray() || (argTypes.length - 1) > argEndIndex - argStartIndex + 1)
                        continue;
                
                // Try to convert the string arguments
                return constructor.newInstance(parseTypesFromString(arguments, argTypes, argStartIndex, argEndIndex, namedInstances));
            } catch (InstantiationException e) {
                lastException = e;
                lastConstructor = constructor;
            } catch (IllegalAccessException e) {
                throw new InvocationTargetException(e);
            }
        }
        
        if (lastConstructor == null)
            throw new InvocationTargetException(new NoSuchMethodException("Constructor for specified arguments cannot be found"));
        throw new InvocationTargetException(lastException, lastConstructor.toString());
    }
    
    /**
     * Creates a new instance of a class using string arguments for its constructor.
     * The list of constructors of the desired class must be provided as <code>constructors</code> argument.
     * The constructors are tried one by one from this list and if the <code>arguments</code>
     * are convertible to the arguments of that constructor, a new instance is created.
     * <p>
     * Note that only constructors with the number of arguments equal to <code>argEndIndex - argStartIndex + 1</code>
     * are tried. If there are several constructors with the same number of arguments, the first (in the order
     * of the list) constructor that succeeds in converting string arguments will be used.
     * </p>
     * <p>
     * Note also that only types convertible by {@link #stringToType} method can be used in constructors.
     * </p>
     * <p>
     * Note also that {@link messif.objects.LocalAbstractObject} parameters will not be converted. For this fuctionality,
     * use the full {@link #createInstanceWithStringArgs createInstanceWithStringArgs} method instead.
     * </p>
     *
     * @param <E> the type of the instantiated object
     * @param constructors the list of constructors of the desired class to try
     * @param arguments the string arguments for the constructor that will be converted to correct types
     * @param argStartIndex index in the string arguments array from which to expect arguments (all the previous items are ignored)
     * @param argEndIndex index in the string arguments array to which to expect arguments (all the following items are ignored)
     * @return a new instance of the class the constructors were specified for
     * @throws InvocationTargetException
     *              if the constructor can't be found for the specified arguments,
     *              the argument string-to-type convertion has failed or
     *              there was an error during instantiation
     */
    public static <E> E createInstanceWithStringArgs(List<Constructor<E>> constructors, String[] arguments, int argStartIndex, int argEndIndex) throws InvocationTargetException {
        return createInstanceWithStringArgs(constructors, arguments, argStartIndex, argEndIndex, null);
    }

    /**
     * Creates a new instance of a class using string arguments for its constructor.
     * The list of constructors of the desired class must be provided as <code>constructors</code> argument.
     * The constructors are tried one by one from this list and if the <code>arguments</code>
     * are convertible to the arguments of that constructor, a new instance is created.
     * <p>
     * Note that only constructors with the number of arguments equal to <code>arguments.length - argStartIndex</code>
     * are tried. If there are several constructors with the same number of arguments, the first (in the order
     * of the list) constructor that succeeds in converting string arguments will be used.
     * </p>
     * <p>
     * Note also that only types convertible by {@link #stringToType} method can be used in constructors.
     * </p>
     * <p>
     * Note also that {@link messif.objects.LocalAbstractObject} parameters will not be converted. For this fuctionality,
     * use the full {@link #createInstanceWithStringArgs createInstanceWithStringArgs} method instead.
     * </p>
     *
     * @param <E> the type of the instantiated object
     * @param constructors the list of constructors of the desired class to try
     * @param arguments the string arguments for the constructor that will be converted to correct types
     * @param argStartIndex index in the string arguments array from which to expect arguments (all the previous items are ignored)
     * @return a new instance of the class the constructors were specified for
     * @throws InvocationTargetException
     *              if the constructor can't be found for the specified arguments,
     *              the argument string-to-type convertion has failed or
     *              there was an error during instantiation
     */
    public static <E> E createInstanceWithStringArgs(List<Constructor<E>> constructors, String[] arguments, int argStartIndex) throws InvocationTargetException {
        return createInstanceWithStringArgs(constructors, arguments, argStartIndex, arguments.length - 1);
    }

    /**
     * Creates a new instance of a class using string arguments for its constructor.
     * The list of constructors of the desired class must be provided as <code>constructors</code> argument.
     * The constructors are tried one by one from this list and if the <code>arguments</code>
     * are convertible to the arguments of that constructor, a new instance is created.
     * <p>
     * Note that only constructors with the number of arguments equal to <code>arguments.length - argStartIndex</code>
     * are tried. If there are several constructors with the same number of arguments, the first (in the order
     * of the list) constructor that succeeds in converting string arguments will be used.
     * </p>
     * <p>
     * Note also that only types convertible by {@link #stringToType} method can be used in constructors.
     * </p>
     *
     * @param <E> the type of the instantiated object
     * @param constructors the list of constructors of the desired class to try
     * @param arguments the string arguments for the constructor that will be converted to correct types
     * @param argStartIndex index in the string arguments array from which to expect arguments (all the previous items are ignored)
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @return a new instance of the class the constructors were specified for
     * @throws InvocationTargetException
     *              if the constructor can't be found for the specified arguments,
     *              the argument string-to-type convertion has failed or
     *              there was an error during instantiation
     */
    public static <E> E createInstanceWithStringArgs(List<Constructor<E>> constructors, String[] arguments, int argStartIndex, Map<String, Object> namedInstances) throws InvocationTargetException {
        return createInstanceWithStringArgs(constructors, arguments, argStartIndex, arguments.length - 1, namedInstances);
    }

    /**
     * Creates a new instance of a class using string arguments for its constructor.
     * The list of constructors of the desired class must be provided as <code>constructors</code> argument.
     * The constructors are tried one by one from this list and if the <code>arguments</code>
     * are convertible to the arguments of that constructor, a new instance is created.
     * <p>
     * Note that only constructors with the same number of arguments as the length of <code>arguments</code>
     * are tried. If there are several constructors with the same number of arguments, the first (in the order
     * of the list) constructor that succeeds in converting string arguments will be used.
     * </p>
     * <p>
     * Note also that only types convertible by {@link #stringToType} method can be used in constructors.
     * </p>
     * <p>
     * Note also that {@link messif.objects.LocalAbstractObject} parameters will not be converted. For this fuctionality,
     * use the full {@link #createInstanceWithStringArgs createInstanceWithStringArgs} method instead.
     * </p>
     *
     * @param <E> the type of the instantiated object
     * @param constructors the list of constructors of the desired class to try
     * @param arguments the string arguments for the constructor that will be converted to correct types
     * @return a new instance of the class the constructors were specified for
     * @throws InvocationTargetException
     *              if the constructor can't be found for the specified arguments,
     *              the argument string-to-type convertion has failed or
     *              there was an error during instantiation
     */
    public static <E> E createInstanceWithStringArgs(List<Constructor<E>> constructors, String... arguments) throws InvocationTargetException {
        return createInstanceWithStringArgs(constructors, arguments, 0);
    }

    /**
     * Creates a new instance of a class using string arguments for its constructor.
     * The list of constructors of the desired class must be provided as <code>constructors</code> argument.
     * The constructors are tried one by one from this list and if the <code>arguments</code>
     * are convertible to the arguments of that constructor, a new instance is created.
     * <p>
     * Note that only constructors with the same number of arguments as the length of <code>arguments</code>
     * are tried. If there are several constructors with the same number of arguments, the first (in the order
     * of the list) constructor that succeeds in converting string arguments will be used.
     * </p>
     * <p>
     * Note also that only types convertible by {@link #stringToType} method can be used in constructors.
     * </p>
     *
     * @param <E> the type of the instantiated object
     * @param constructors the list of constructors of the desired class to try
     * @param arguments the string arguments for the constructor that will be converted to correct types
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @return a new instance of the class the constructors were specified for
     * @throws InvocationTargetException
     *              if the constructor can't be found for the specified arguments,
     *              the argument string-to-type convertion has failed or
     *              there was an error during instantiation
     */
    public static <E> E createInstanceWithStringArgs(List<Constructor<E>> constructors, String[] arguments, Map<String, Object> namedInstances) throws InvocationTargetException {
        return createInstanceWithStringArgs(constructors, arguments, 0, namedInstances);
    }

    /**
     * Creates a new instance of a class with a string constructor/factory-method/static-field signature.
     * The string must contain a fully specified name of constructor optionally with arguments enclosed by parenthesis, e.g.:
     * <pre>
     *      messif.pivotselection.StreamSequencePivotChooser(messif.objects.impl.MetaObjectMap, file)
     * </pre>
     * Or the string can be a fully specified name of a factory method (package.class.methodName)
     * with arguments enclosed by parenthesis, e.g.:
     * <pre>
     *      messif.utility.ExtendedProperties.getProperties(file)
     * </pre>
     * Or the string can be a fully specified name of a public static field (package.class.fieldName), e.g.:
     * <pre>
     *     messif.buckets.index.LocalAbstractObjectOrder.locatorToLocalObjectComparator
     * </pre>
     * <p>
     * Note that only types convertible by {@link #stringToType} method can be used in arguments.
     * </p>
     *
     * @param <E> the type of the instantiated object
     * @param signature constructor or factory method call with string arguments or a static field
     * @param checkClass the superclass of (or the same class as) the instantiated object
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @return a new instance of the specified object
     * @throws InvocationTargetException
     *              if the constructor/factory-method/static-field can't be found for the specified arguments,
     *              the argument string-to-type convertion has failed or
     *              there was an error during instantiation
     * @throws ClassNotFoundException if the class in the constructor signature was not found or is not a descendant of checkClass
     */
    public static <E> E createInstanceWithStringArgs(String signature, Class<E> checkClass, Map<String, Object> namedInstances) throws InvocationTargetException, ClassNotFoundException {
        // Parse arguments (enclosed in braces)
        String[] args;
        String callname;
        int openParenthesisPos = signature.indexOf('(');
        int closeParenthesisPos = signature.lastIndexOf(')');
        if (openParenthesisPos == -1 || openParenthesisPos == closeParenthesisPos - 1) { // There are no parenthesis or they are empty
            args = new String[0];
            callname = signature;
        } else if (closeParenthesisPos == -1) {
            throw new IllegalArgumentException("Missing closing parenthesis: " + signature);
        } else {
            args = signature.substring(openParenthesisPos + 1, closeParenthesisPos).split("\\s*,\\s*");
            callname = signature.substring(0, openParenthesisPos);
        }

        // Create instance
        try {
            // Try call name as a constructor
            Class<E> clazz = getClassForName(callname, checkClass);
            @SuppressWarnings("unchecked")
            List<Constructor<E>> constructors = Arrays.asList((Constructor<E>[])clazz.getConstructors());
            return createInstanceWithStringArgs(constructors, args, namedInstances);
        } catch (ClassNotFoundException e) {
            // Class not found, try dot earlier
            int dotPos = callname.lastIndexOf('.');
            if (dotPos == -1)
                throw e;

            // Class without last item, which is supposed to be a factory method name or a field name
            Class<E> clazz = getClassForName(callname.substring(0, dotPos), checkClass);
            callname = callname.substring(dotPos + 1);

            try {
                // We have correct class, now check if it is method or attribute
                return (openParenthesisPos == -1)?
                    checkClass.cast(createInstanceStaticField(clazz, callname)):
                    createInstanceUsingFactoryMethod(clazz, callname, (Object[])args);
            } catch (NoSuchMethodException ex) {
                throw new InvocationTargetException(ex);
            }
        }
    }

    /**
     * Creates a new instance of a class.
     * First, a constructor for the specified arguments is searched in the provided class.
     * Then, an instance is created and returned.
     *
     * @param <E> the type of the instantiated object
     * @param instanceClass the class for which to create an instance
     * @param arguments the arguments for the constructor
     * @return a new instance of the class
     * @throws NoSuchMethodException if there was no constructor for the specified list of arguments
     * @throws InvocationTargetException if there was an exception during instantiation
     */
    public static <E> E createInstanceWithInheritableArgs(Class<E> instanceClass, Object... arguments) throws NoSuchMethodException, InvocationTargetException {
        try {
            return getConstructor(instanceClass, false, arguments).newInstance(arguments);
        } catch (IllegalAccessException e) {
            throw new NoSuchMethodException(e.getMessage());
        } catch (InstantiationException e) {
            throw new NoSuchMethodException(e.getMessage());
        }
    }

    /**
     * Creates a new instance of a class.
     * First, a factory method for the specified arguments is searched in the provided class and its ancestors.
     * Then, an instance is created and returned.
     *
     * @param <E> the type of the instantiated object
     * @param instanceClass the class for which to create an instance
     * @param methodName the name of the factory method
     * @param arguments the arguments for the factory method
     * @return a new instance of the class
     * @throws NoSuchMethodException if there was no factory method for the specified list of arguments
     * @throws InvocationTargetException if there was an exception during instantiation
     */
    @SuppressWarnings("unchecked")
    public static <E> E createInstanceUsingFactoryMethod(Class<E> instanceClass, String methodName, Object... arguments) throws NoSuchMethodException, InvocationTargetException {
        try {
            Method factoryMethod = getMethod(instanceClass, methodName, false, arguments);
            if (!Modifier.isStatic(factoryMethod.getModifiers()))
                throw new IllegalArgumentException("Factory method " + factoryMethod + " is required to be static");
            if (!instanceClass.isAssignableFrom(factoryMethod.getReturnType()))
                throw new IllegalArgumentException("Factory method " + factoryMethod + " is required to return " + instanceClass);
            return (E)factoryMethod.invoke(null, arguments); // This cast IS checked on the previous line
        } catch (IllegalAccessException e) {
            throw new NoSuchMethodException(e.getMessage());
        }
    }

    /**
     * Get instance from a static field of a class.
     *
     * @param clazz the class the field of which to access
     * @param name the name of the <code>clazz</code>'s static field
     * @return a new instance of the class
     * @throws IllegalArgumentException if there was no public static field for the specified <code>clazz</code> and <code>name</code>
     */
    @SuppressWarnings("unchecked")
    public static Object createInstanceStaticField(Class<?> clazz, String name) throws IllegalArgumentException {
        try {
            Field field = clazz.getField(name);
            if (!Modifier.isStatic(field.getModifiers()))
                throw new IllegalArgumentException("Field '" + name + "' in " + clazz + " is not static");
            return field.get(null);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Field '" + name + "' was not found in " + clazz);
        } catch (IllegalAccessException e) { // This should never happen
            throw new IllegalArgumentException("Field '" + name + "' in " + clazz + " is not accessible");
        }
    }

    /**
     * Returns a constructor for the specified class that accepts the specified arguments.
     * The <code>clazz</code>'s declared constructors are searched for the one that
     * accepts the arguments.
     * If the <code>convertStringArguments</code> is specified, the 
     * <code>arguments</code> elements are replaced with the converted types
     * if and only if a proper constructor is found. Their types then will be
     * compatible with the constructor.
     * 
     * @param <E> the class the constructor will create
     * @param clazz the class for which to get the constructor
     * @param convertStringArguments if <tt>true</tt> the string values from the arguments are converted using {@link #stringToType}
     * @param arguments the arguments for the constructor
     * @return a constructor for the specified class
     * @throws NoSuchMethodException if there was no constructor for the specified list of arguments
     */
    @SuppressWarnings("unchecked")
    public static <E> Constructor<E> getConstructor(Class<E> clazz, boolean convertStringArguments, Object[] arguments) throws NoSuchMethodException {
        for (Constructor<E> constructor : (Constructor<E>[])clazz.getDeclaredConstructors()) { // This cast IS A STUPID BUG!!!!
            if (isPrototypeMatching(constructor.getParameterTypes(), arguments, convertStringArguments))
                return constructor;
        }

        // Constructor not found, prepare error
        StringBuilder str = new StringBuilder("There is no constructor ");
        str.append(clazz.getName()).append('(');
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0)
                str.append(", ");
            if (arguments[i] == null)
                str.append("null");
            else
                str.append(arguments[i].getClass().getName());
        }
        str.append(')');
        throw new NoSuchMethodException(str.toString());
    }

    /**
     * Returns type-safe public constructors of the given class.
     * @param <E> the class for which to get the constructors
     * @param objectClass the class for which to get the constructors
     * @return all public constructors of the given class
     */
    @SuppressWarnings("unchecked")
    public static <E> Constructor<E>[] getConstructors(Class<? extends E> objectClass) {
        return (Constructor<E>[])objectClass.getConstructors();  // This IS A STUPID unchecked !!!
    }

    /**
     * Returns a method for the specified class that accepts the specified arguments.
     * The <code>clazz</code>'s declared methods are searched for the one that
     * accepts the arguments.
     * If the <code>convertStringArguments</code> is specified, the 
     * <code>arguments</code> elements are replaced with the converted types
     * if and only if a proper method is found. Their types then will be
     * compatible with the method.
     * 
     * @param clazz the class for which to get the method
     * @param methodName the name of the method to get
     * @param convertStringArguments if <tt>true</tt> the string values from the arguments are converted using {@link #stringToType}
     * @param arguments the arguments for the method
     * @return a method of the specified class
     * @throws NoSuchMethodException if there was no method with the specified name and arguments
     */
    @SuppressWarnings("unchecked")
    public static Method getMethod(Class<?> clazz, String methodName, boolean convertStringArguments, Object[] arguments) throws NoSuchMethodException {
        if (clazz == null || methodName == null)
            throw new NoSuchMethodException("There is not method '" + methodName + "' that accepts " + Arrays.toString(arguments));

        // Search all methods of the execution object and register the matching ones
        for (Method method : clazz.getDeclaredMethods()) {
            // Skip methods with different name if methodNames parameter was specified
            if (!methodName.equals(method.getName()))
                continue;

            // Check prototype
            if (isPrototypeMatching(method.getParameterTypes(), arguments, convertStringArguments))
                return method;
        }

        // Recurse to superclass
        return getMethod(clazz.getSuperclass(), methodName, convertStringArguments, arguments);
    }

    /**
     * Test argument array, if it is compatible with the provided prototype.
     * That is, the number of arguments must be equal and each argument
     * (an item from the <code>methodTypes</code>) must be assignable
     * from the respective <code>methodPrototype</code> item.
     * 
     * @param methodTypes the tested arguments array
     * @param methodPrototype the prototype arguments array
     * @param skipIndex the index of an argument that is not checked for the compatibility condition
     * @return <tt>true</tt> if the method types are compatible with the prototype
     */
    public static boolean isPrototypeMatching(Class<?>[] methodTypes, Class<?>[] methodPrototype, int skipIndex) {
        // Not enough arguments
        if (methodTypes.length != methodPrototype.length) return false;
        
        // Test arguments
        for (int i = 0; i < methodTypes.length; i++) {
            if (i == skipIndex) continue;

            // If the method type is primitive type, check names
            if (methodTypes[i].isPrimitive() && methodPrototype[i].getName().toLowerCase().startsWith("java.lang." + methodTypes[i].getSimpleName()))
                continue;

            // The argument of the method must be the same as or a superclass of the provided prototype class
            if (!methodTypes[i].isAssignableFrom(methodPrototype[i]))
                return false;
        }
        
        return true;
    }
    
    /**
     * Test argument array, if it is compatible with the provided prototype.
     * That is, each item from the <code>methodTypes</code> must be assignable
     * from the respective <code>methodPrototype</code> item.
     * 
     * @param methodTypes the tested arguments array
     * @param methodPrototype the prototype arguments array
     * @return <tt>true</tt> if the method types are compatible with the prototype
     */
    public static boolean isPrototypeMatching(Class<?>[] methodTypes, Class<?>[] methodPrototype) {
        return isPrototypeMatching(methodTypes, methodPrototype, -1);
    }

    /**
     * Test argument array, if it is compatible with the provided prototype.
     * That is, the number of arguments must be equal and each argument
     * must be assignable to the respective <code>prototype</code> item.
     * If the <code>convertStringArguments</code> is specified, the 
     * <code>arguments</code> elements are replaced with the converted types
     * if and only if the method returns <tt>true</tt>.
     * 
     * @param prototype the tested prototype
     * @param arguments the tested arguments
     * @param convertStringArguments if <tt>true</tt> the string values from the arguments are converted using {@link #stringToType}
     * @return <tt>true</tt> if the arguments are compatible with the prototype
     */
    public static boolean isPrototypeMatching(Class<?>[] prototype, Object[] arguments, boolean convertStringArguments) {
        // Not enough arguments
        if (prototype.length != arguments.length)
            return false;

        // Array for the converted values (since they must not be modified if the method return false)
        Object[] convertedArguments = null;

        // Test arguments
        for (int i = 0; i < prototype.length; i++) {
            // Null value is accepted by any class
            if (arguments[i] == null)
                continue;
            // The argument of the method must be the same as or a superclass of the provided prototype class
            if (!wrapPrimitiveType(prototype[i]).isInstance(arguments[i])) {
                if (!convertStringArguments || !(arguments[i] instanceof String))
                    return false;
                // Try to convert string argument
                try {
                    // Clone argument array if not clonned yet
                    if (convertedArguments == null)
                        convertedArguments = arguments.clone();
                    convertedArguments[i] = stringToType((String)arguments[i], prototype[i]);
                } catch (InstantiationException ignore) {
                    return false;
                }
            }
        }

        // Move converted arguments
        if (convertedArguments != null)
            System.arraycopy(convertedArguments, 0, arguments, 0, arguments.length);

        return true;
    }

    /**
     * Returns a new instance of a static array.
     * @param <T> the type of components of the new array
     * @param componentType the class of components of the new array
     * @param size the number of elements of the new array
     * @return a new instance of a static array
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] createGenericArray(Class<T> componentType, int size) {
        return (T[])Array.newInstance(componentType, size);
    }

    /**
     * Returns a new instance of a static array.
     * @param <T> the type of components of the new array
     * @param array a generic array according to which to get the component type
     * @param size the number of elements of the new array
     * @return a new instance of a static array
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] createGenericArray(T[] array, int size) {
        return (T[])Array.newInstance(array.getClass().getComponentType(), size);
    }

    /**
     * Copies the specified array, truncating or padding with nulls (if necessary)
     * so the copy has the specified length.  For all indices that are
     * valid in both the original array and the copy, the two arrays will
     * contain identical values.  For any indices that are valid in the
     * copy but not the original, the copy will contain <tt>null</tt>.
     * Such indices will exist if and only if the specified length
     * is greater than that of the original array.
     * The resulting array is of the class <tt>newType</tt>.
     *
     * @param <T> the type of objects in the array
     * @param original the array to be copied
     * @param newLength the length of the copy to be returned
     * @param componentType the class of array components
     * @return a copy of the original array, truncated or padded with nulls
     *     to obtain the specified length
     * @throws NegativeArraySizeException if <tt>newLength</tt> is negative
     */
    public static <T> T[] resizeArray(T[] original, int newLength, Class<T> componentType) throws NegativeArraySizeException {
        T[] copy = createGenericArray(componentType, newLength);
        if (original != null)
            System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    /**
     * Adds an item to the end of a specified static array (enlarging its size by one).
     * @param <T> the type of objects in the array
     * @param original the array where the item is added
     * @param componentType the class of array components
     * @param item the item to add
     * @return a copy of the original array with added item
     */
    public static <T> T[] addToArray(T[] original, Class<T> componentType, T item) {
        T[] ret = resizeArray(original, (original == null)?1:(original.length + 1), componentType);
        ret[ret.length - 1] = item;
        return ret;
    }

    /**
     * Search the array for the specified item.
     * @param <T> the type of objects in the array
     * @param array the array to search
     * @param item the item to search for
     * @param backwards if set to <tt>true</tt>, the search is started from the last element
     * @return index of the array element, where the item was found, or -1 if it was not
     */
    public static <T> int searchArray(T[] array, T item, boolean backwards) {
        if (array == null)
            return -1;
        if (backwards) {
            for (int i = array.length - 1; i >= 0; i--)
                if (item.equals(array[i]))
                    return i;
        } else {
            for (int i = 0; i < array.length; i++)
                if (item.equals(array[i]))
                    return i;
        }
        return -1;
    }

    /**
     * Removes an item from the specified static array (shrinking its size by one).
     * The removed array element is the last one that is equal to the specified item.
     * If the element is not found, the same array (original) is returned.
     * If the removed element was the last one, <tt>null</tt> is returned.
     * @param <T> the type of objects in the array
     * @param original the array from which the item is removed
     * @param item the item to remove
     * @return a copy of the original array with added item
     */
    public static <T> T[] removeFromArray(T[] original, T item) {
        // Search for the array element to remove
        int i = searchArray(original, item, true);

        if (i == -1)
            return original;
        if (original.length == 1)
            return null;
        T[] ret = createGenericArray(original, original.length - 1);
        System.arraycopy(original, 0, ret, 0, i);
        System.arraycopy(original, i + 1, ret, i, original.length - i - 1);
        return ret;
    }

    /**
     * Return generic-safe {@link java.util.Map} type.
     * Note that only non-typed arguments can be passed as key/value class to be
     * generic-safe.
     *
     * @param <K> the type of keys in the returned map
     * @param <V> the type of values in the returned map
     * @param object the object that sould be cast to other type
     * @param keysClass the class to cast the map keys to
     * @param valuesClass the class to cast the map values to
     * @throws ClassCastException if there was an incompatible key or value or the object is not {@link java.util.Map}
     * @return a map with generic-safe key/values
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> safeGenericCastMap(Object object, Class<K> keysClass, Class<V> valuesClass) throws ClassCastException {
        if (object == null)
            return null;
        Map<Object, Object> rtv = (Map)object;
        for (Map.Entry<Object, Object> entry : rtv.entrySet()) {
            if (!keysClass.isInstance(entry.getKey()))
                throw new ClassCastException("Can't cast key '" + entry.getKey() + "' to '" + keysClass.getName() + "'");
            if (!valuesClass.isInstance(entry.getValue()))
                throw new ClassCastException("Can't cast value '" + entry.getValue() + "' to '" + valuesClass.getName() + "'");
        }

        return (Map<K, V>)rtv;
    }

    /**
     * Returns an object from parameter table.
     * If the parameter table is <tt>null</tt> or if it does not contain
     * the parameter with the specified name, the default value is returned.
     * Otherwise, the class of the parametr is checked and if it is not the
     * requested class (but it is a {@link String}), the {@link #stringToType}
     * conversion is tried.
     *
     * @param <T> the type of the returned parameter value
     * @param parameters the parameter table
     * @param paramName the name of parameter to get from the table; if there is no parameter of that 
     * @param paramClass the class to cast the value to
     * @param defaultValue the deault value to return if there is no parameter
     * @throws ClassCastException if there was an incompatible key or value or the object cannot be converted from string
     * @return a value from the parameter table
     */
    @SuppressWarnings("unchecked")
    public static <T> T getParameterValue(Map<String, ?> parameters, String paramName, Class<T> paramClass, T defaultValue) throws ClassCastException {
        if (parameters == null)
            return defaultValue;
        
        // Get the value from the parameters
        Object value = parameters.get(paramName);
        if (value == null)
            return defaultValue;

        if (paramClass.isInstance(value))
            return (T)value; // This cast IS checked on the previous line

        // Try to convert the value from string (there will be a class cast exception if the value is not string)
        try {
            return stringToType((String)value, paramClass);
        } catch (InstantiationException e) {
            throw new ClassCastException(e.getMessage());
        }
    }

    /**
     * Replace all occurances of the variable pattern with the value from the hash table.
     * The specified pattern group is used the the variable name.
     *
     * @param string the string to be modified
     * @param variableRegex regular expression that matches variables
     * @param variableRegexGroup parenthesis group within regular expression
     *          that holds the variable name
     * @param defaultValueRegexGroup parenthesis group within regular expression
     *          that holds the default value for a variable that is not present
     *          in the <code>variables</code> map
     * @param variables the variable names with their values
     * @return the original string with all variables replaced
     */
    public static String substituteVariables(String string, Pattern variableRegex, int variableRegexGroup, int defaultValueRegexGroup, Map<String,String> variables) {
        // Check null strings
        if (string == null)
            return null;

        // Prepare for matching
        Matcher matcher = variableRegex.matcher(string);
        StringBuffer sb = new StringBuffer();
        
        // Find every occurance of pattern in string
        while (matcher.find()) {
            // Get variable with the name from the matched pattern group
            String value = variables.get(matcher.group(variableRegexGroup));

            // Set the default value if specified
            if (value == null && defaultValueRegexGroup > 0)
                value = matcher.group(defaultValueRegexGroup);

            // Do the replacement, if variable is not found, the variable placeholder is removed
            matcher.appendReplacement(sb, (value != null)?value:"");
        }

        // Finish replacing
        matcher.appendTail(sb);

        // Return the string with replaced variables
        return sb.toString();
    }

    /**
     * Returns the today's specified time in miliseconds.
     * @param time the time in "hh:mm:ss.iii" format (seconds and miliseconds are optional)
     * @return the today's specified time in miliseconds
     * @throws NumberFormatException if the specified time has invalid format
     */
    public static long timeToMiliseconds(String time) throws NumberFormatException {
        Calendar calendar = Calendar.getInstance();
        String[] hms = time.split("\\p{Space}*[:.]\\p{Space}*", 4);
        switch (hms.length) {
            case 4:
                calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hms[0]));
                calendar.set(Calendar.MINUTE, Integer.parseInt(hms[1]));
                calendar.set(Calendar.SECOND, Integer.parseInt(hms[2]));
                calendar.set(Calendar.MILLISECOND, Integer.parseInt(hms[3]));
                break;
            case 3:
                calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hms[0]));
                calendar.set(Calendar.MINUTE, Integer.parseInt(hms[1]));
                calendar.set(Calendar.SECOND, Integer.parseInt(hms[2]));
                calendar.set(Calendar.MILLISECOND, 0);
                break;
            case 2:
                calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hms[0]));
                calendar.set(Calendar.MINUTE, Integer.parseInt(hms[1]));
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                break;
            default:
                throw new NumberFormatException("At least hours and minutes must be specified");
        }
        try {
            calendar.setLenient(false);
            return calendar.getTimeInMillis();
        } catch (IllegalArgumentException e) {
            // The value for a field (message of the exception) was invalid
            StringBuffer str = new StringBuffer("Value of ");
            str.append(e.getMessage().toLowerCase());
            str.append(" is invalid");
            throw new NumberFormatException(str.toString());
        }
    }

    /**
     * Returns a table of all values from the specified enum keyed by the enum value's name.
     * @param <E> the enum type
     * @param values the class of the enum for which to get the map
     * @return a table of all values from the specified enum
     */
    public static <E extends Enum<E>> Map<String, E> enumToMap(Class<E> values) {
        E[] constants = values.getEnumConstants();
        Map<String, E> ret = new HashMap<String, E>(constants.length);
        for (E constant : constants)
            ret.put(constant.name(), constant);
        return ret;
    }

}