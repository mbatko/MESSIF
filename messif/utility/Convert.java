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
package messif.utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class that provides methods for type conversions and instantiation.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class Convert {
    
    /** Number of bytes that the {@link #readStreamData(java.io.InputStream, int) readStreamData} method allocates */
    private static final int readStreamDataAllocation = 4096;

    /**
     * Converts a string into object of the specified type.
     * <p>
     * Currently supported types are:
     * <ul>
     *   <li>all primitive types (int, long, boolean, double, byte, float, short, char)</li>
     *   <li>all object wrappers for primitive types</li>
     *   <li>{@link String}</li>
     *   <li>{@link Class}</li>
     *   <li>{@link Iterator} - if the parameter represents the named object that implements {@link Iterator}, then the next value from the iterator is tried</li>
     *   <li>static array of any "convertible" element type - parameter should be comma-separated values that will be converted using {@link #stringToType} into the array's items</li>
     *   <li>{@link Map} with {@link String} key and value - parameter should be comma-separated key=value pairs (possibly quoted)</li>
     *   <li>any class with a public constructor that has a single {@link String} parameter</li>
     *   <li>any class with a <code>valueOf(String <i>parameter</i>)</code> factory method, e.g., {@link java.lang.Integer#valueOf(java.lang.String)}</li>
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
    public static <E> E stringToType(String string, Class<E> type, Map<String, ? extends Object> namedInstances) throws InstantiationException {
        if (string == null || string.equals("null"))
            return null;

        // Wrap primitive types, so that a named-instance object or the 'valueOf' method can be used
        if (type.isPrimitive())
            type = wrapPrimitiveType(type);

        // Named instances of objects (this needs to be before the other type conversions are tried so that the named instances work)
        if (namedInstances != null) {
            Object instance = namedInstances.get(string);
            if (instance != null) {
                // Return named object as-is
                if (type.isInstance(instance))
                    return type.cast(instance);
                instance = expandReferencedInstances(instance);
                // Return named object as-is after the expansion
                if (type.isInstance(instance))
                    return type.cast(instance);
                else if (instance instanceof Iterator) // Try iterator
                    return type.cast(((Iterator<?>)instance).next());
                else if (!type.isArray()) // If type is array, the result is a single-value static array and will be called from below
                    throw new InstantiationException("Named instance '" + string + "' exists, but cannot be converted to '" + type.getName() + "'");
            }
        }

        // Converting class types
        if (type == Class.class) try {
            return type.cast(Class.forName(string));
        } catch (ClassNotFoundException e) {
            throw new InstantiationException(e.toString());
        }

        // Converting string types
        if (type == String.class)
            return type.cast(string);

        // Converting map types
        if (type == Map.class) {
            Map<String, Object> rtv = new HashMap<String, Object>();
            putStringIntoMap(string, rtv, String.class);
            // Add current named instances to a Map that contain a 'namedInstances' key but it is null
            if (rtv.containsKey("namedInstances") && rtv.get("namedInstances") == null)
                rtv.put("namedInstances", namedInstances);
            return type.cast(rtv);
        }

        // Converting static arrays
        if (type.isArray()) {
            return stringToArray(string, Pattern.compile("\\s*[|,]\\s*"), -1, type, namedInstances);
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
                return type.cast(method.invoke(null, string)); // This cast IS checked
        } catch (InvocationTargetException e) {
            // This string is unconvertible, because some exception arised
            throw new InstantiationException(e.getCause().toString());
        } catch (IllegalAccessException e) {
            // Can't access valueOf, but never mind, other conversions might be possible...
        } catch (NoSuchMethodException e) {
            // Method not found, but never mind, other conversions might be possible...
        }

        // Try name of a static field
        try {
            int dotPos = string.lastIndexOf('.');
            if (dotPos != -1) {
                Field field = Class.forName(string.substring(0, dotPos)).getField(string.substring(dotPos + 1));
                if (Modifier.isStatic(field.getModifiers()) && type.isAssignableFrom(field.getType()))
                    return type.cast(field.get(null));
            }
        } catch (ClassNotFoundException ignore) {
        } catch (NoSuchFieldException ignore) {
        } catch (IllegalAccessException ignore) {
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
     * Creates a new instance of a class specified in the {@code classAndData}.
     * The class name is separated from the data using {@code separator}.
     * The data are fed into public constructor with one {@link String} argument.
     *
     * @param <T> the class, super class, or the interface of the created instance
     * @param classAndData the string containing the class name followed by the data
     * @param separator separates the class name from the data
     * @param checkClass the class, super class, or the interface of the created instance
     * @return a new instance of the specified class
     * @throws IllegalArgumentException if the instance cannot be created (see the message to get the reason)
     */
    public static <T> T stringAndClassToType(String classAndData, char separator, Class<? extends T> checkClass) throws IllegalArgumentException {
        if (classAndData == null)
            return null;

        // Get position of a separator that separates class from data
        int separatorPos = classAndData.indexOf(separator);
        if (separatorPos == -1)
            throw new IllegalArgumentException("Cannot find separator '" + separator + "' in '" + classAndData + "'");

        // Get class part and convert it to real class
        Class<? extends T> clazz;
        try {
            clazz = getClassForName(classAndData.substring(0, separatorPos), checkClass);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        // Create instance using a constructor with String parameter
        try {
            return clazz.getConstructor(String.class).newInstance(classAndData.substring(separatorPos + 1));
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("There is no constructor that accepts String or BufferedReader argument in " + clazz);
        } catch (IllegalAccessException e) {
            throw new InternalError("This should never happen: " + e); // Constructor is public
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Cannot create instance of abstract " + clazz);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("Cannot create instance of " + clazz + " for data '" + classAndData.substring(separatorPos + 1) + "': " + e.getCause(), e.getCause());
        }
    }

    /**
     * Expand {@link ThreadLocal} and {@link Reference} instances to their referees.
     * If the given instance is <tt>null</tt> or a non-expandable data type, it is returned as-is.
     * @param instance the instance to expand
     * @return the expanded instance
     */
    public static Object expandReferencedInstances(Object instance) {
        // Expand thread-local variable
        if (instance instanceof ThreadLocal)
            instance = ((ThreadLocal<?>)instance).get();
        // Expand variable references 
        while (instance instanceof Reference)
            instance = ((Reference<?>)instance).get();
        return instance;
    }

    /**
     * Returns a string serialization of the given {@code array}.
     * @param array the array object to convert
     * @param deep if <tt>true</tt>, the {@link #typeToString(java.lang.Object)}
     *       method is applied to array items, otherwise a simple {@link Object#toString()} is used
     * @return a string representation of the array
     * @throws IllegalArgumentException if the argument is not an array
     */
    public static String arrayToString(Object array, boolean deep) throws IllegalArgumentException {
        int size = Array.getLength(array);
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0)
                str.append(',');
            if (deep)
                str.append(typeToString(Array.get(array, i)));
            else
                str.append(Array.get(array, i));
        }
        return str.toString();
    }

    /**
     * Creates a static array from the given string.
     * The string is split using the given regular expression split pattern
     * and the respective elements converted using the {@link #stringToType(java.lang.String, java.lang.Class, java.util.Map) stringToType}
     * method.
     * @param <T> the class of the array to create
     * @param string the string to convert to array
     * @param splitPattern the pattern used to split the string into array elements
     * @param splitLimit the number of times the splitPattern is applied and
     *          therefore affects the length of the resulting array (see {@link Pattern#split(java.lang.CharSequence, int) split} method)
     * @param type the class of the array to create (note that this must be the class of the static array)
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @return a new static array
     * @throws InstantiationException if there was a problem converting the array elements
     */
    public static <T> T stringToArray(CharSequence string, Pattern splitPattern, int splitLimit, Class<? extends T> type, Map<String, ? extends Object> namedInstances) throws InstantiationException {
        if (string == null)
            return null;
        Class<?> componentType = type.getComponentType();
        if (componentType == null)
            throw new InstantiationException("Class " + type.getName() + " is not static array class");
        if (string.length() == 0)
            return type.cast(Array.newInstance(componentType, 0));
        String[] items = splitPattern.split(string, splitLimit);
        Object array = Array.newInstance(componentType, items.length);
        for (int i = 0; i < items.length; i++)
            Array.set(array, i, stringToType(items[i], componentType, namedInstances));
        return type.cast(array);        
    }

    /**
     * Returns a string representation of the given IP address and port.
     * @param host the IP address to convert
     * @param port the port to convert (zero or negative port is ignored)
     * @return a string representation of the given IP address and port
     */
    public static String inetAddressToString(InetAddress host, int port) {
        if (port <= 0)
            return host.getHostName();
        return new StringBuilder(host.getHostName()).append(":").append(port).toString();
    }

    /**
     * Returns a string serialization of the given {@code object}.
     * Note that the {@link #stringToType} methods are able to convert the object back.
     * @param object the object to convert
     * @return a string serialization of the object
     */
    public static String typeToString(Object object) {
        if (object == null)
            return "null";
        if (object instanceof Number || object instanceof Character || object instanceof Boolean || object instanceof String)
            return object.toString();
        if (object instanceof TextSerializable)
            return ((TextSerializable)object).toString();
        if (object.getClass().isArray())
            return arrayToString(object, true);
        throw new IllegalArgumentException("Class " + object.getClass().getName() + " cannot be serialized to string");
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
        if (type == Void.TYPE)
            return (Class<T>)Void.class;
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
        Matcher m = Pattern.compile("\\p{Space}*(\"([^\"]*)\"|[^=]*?)\\p{Space}*=\\p{Space}*(\"([^\"]*)\"|[^=]*?)\\p{Space}*([;,]|$)").matcher(string);
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
     * Returns whether the given class is a primitive-writable.
     * That is, the type is either primitive, a wrapper-class for a primitive type,
     * {@link String}, or a static array with primitive-writable components.
     * Note that the static arrays are only checked if {@code checkArrays} is <tt>true</tt>.
     * @param type the class to check
     * @param checkArrays flag whether the static arrays should be considered writable
     * @return <tt>true</tt> if the given class is a primitive-writable
     */
    public static boolean isPrimitiveWritableClass(Class<?> type, boolean checkArrays) {
        if (type == Integer.TYPE || type.equals(Integer.class))
            return true;
        else if (type == Float.TYPE || type.equals(Float.class))
            return true;
        else if (type == Long.TYPE || type.equals(Long.class))
            return true;
        else if (type == Short.TYPE || type.equals(Short.class))
            return true;
        else if (type == Byte.TYPE || type.equals(Byte.class))
            return true;
        else if (type == Double.TYPE || type.equals(Double.class))
            return true;
        else if (type == Character.TYPE || type.equals(Character.class))
            return true;
        else if (type == Boolean.TYPE || type.equals(Boolean.class))
            return true;
        else if (type.equals(String.class))
            return true;
        else if (checkArrays && type.isArray())
            return isPrimitiveWritableClass(type.getComponentType(), true);
        else
            return false;
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
     * @param checkClass the superclass of the returned class for the generic check
     * @return the <code>Class</code> object associated with the class or
     *         interface with the given string name
     * @throws ClassNotFoundException if the class cannot be located
     */
    @SuppressWarnings("unchecked")
    public static <E> Class<E> getClassForName(String name, Class<E> checkClass) throws ClassNotFoundException {
        Class<?> rtv = Class.forName(name);
        if (checkClass.isAssignableFrom(rtv))
            return (Class<E>)rtv; // This cast IS checked on the previous line
        throw new ClassNotFoundException("Class '" + name + "' is not subclass of " + checkClass.getName());
    }

    /**
     * Cast the provided object to Class with generic typing.
     * If the generic type check fails, the <code>ClassCastException</code> is thrown
     * even if the provided <code>classObject</code> is a valid <code>Class</code>.
     * 
     * @param <E> the type of the returned object
     * @param classObject the class object to be cast
     * @param checkClass the generic typed class that is returned
     * @return the generic-typed <code>Class</code> object
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
     * Convert the provided object to Class with generic typing. The specified
     * {@code classObject} should be either instance of {@link String} or {@link Class}.
     * If the generic type check fails, the <code>ClassCastException</code> is thrown
     * even if the provided <code>classObject</code> is a valid <code>Class</code>.
     * 
     * @param <E> the type of the returned object
     * @param classObject the class object to be cast
     * @param checkClass the generic typed class that is returned
     * @return the generic-typed <code>Class</code> object
     * @throws ClassCastException if passed <code>classObject</code> is not subclass of <code>checkClass</code>
     */
    public static <E> Class<E> toGenericClass(Object classObject, Class<E> checkClass) throws ClassCastException {
        if (classObject == null)
            return null;

        try {
            if (classObject instanceof String)
                return getClassForName((String)classObject, checkClass);
            else
                return genericCastToClass(classObject, checkClass);
        } catch (ClassNotFoundException e) {
            throw new ClassCastException("Class '" + classObject + "' was not found or is not a subclass of " + checkClass.getName());
        }
    }

    /**
     * Parses array of strings into array of objects according to the types provided in the second argument.
     * Only parameters from <code>argStartIndex</code> to <code>argEndIndex</code> of <code>strings</code>
     * array will be used.
     *
     * @param strings array of strings that hold the values
     * @param types array of classes that the strings should be converted to
     * @param handleVarArgs flag whether to handle the variable number of arguments (<tt>true</tt>) or not (<tt>false</tt>)
     * @param argStartIndex index in the strings array which denotes the first changeable argument
     * @param argEndIndex index in the strings array which denotes the last changeable argument
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @return the array of converted values
     * @throws InstantiationException if there was a type that cannot be created from the provided string value
     */
    public static Object[] parseTypesFromString(String[] strings, Class<?>[] types, boolean handleVarArgs, int argStartIndex, int argEndIndex, Map<String, ? extends Object> namedInstances) throws InstantiationException {
        // Create return array
        Object[] rtv = new Object[types.length];

        // VarArg handling (last type is array)
        if (handleVarArgs && types.length > 0 && types[types.length - 1].isArray()) {
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
     * Parses array of strings into array of objects according to the types provided in the second argument.
     * Only parameters from <code>argStartIndex</code> to <code>argEndIndex</code> of <code>strings</code>
     * array will be used.
     *
     * <p>
     * Note that {@link messif.objects.LocalAbstractObject} parameters will not be converted. For this functionality,
     * use the full {@link #parseTypesFromString parseTypesFromString} method instead.
     * </p>
     *
     * @param strings array of strings that hold the values
     * @param types array of classes that the strings should be converted to
     * @param handleVarArgs flag whether to handle the variable number of arguments (<tt>true</tt>) or not (<tt>false</tt>)
     * @param argStartIndex index in the strings array which denotes the first changeable argument
     * @param argEndIndex index in the strings array which denotes the last changeable argument
     * @return the array of converted values
     * @throws InstantiationException if there was a type that cannot be created from the provided string value
     */
    public static Object[] parseTypesFromString(String[] strings, Class<?>[] types, boolean handleVarArgs, int argStartIndex, int argEndIndex) throws InstantiationException {
        return parseTypesFromString(strings, types, handleVarArgs, argStartIndex, argEndIndex, null);
    }

    /**
     * Parses array of strings into array of objects according to the types provided in the second argument.
     * All the parameters from <code>argStartIndex</code> till the end of <code>strings</code> array will
     * be used.
     *
     * <p>
     * Note that {@link messif.objects.LocalAbstractObject} parameters will not be converted. For this functionality,
     * use the full {@link #parseTypesFromString parseTypesFromString} method instead.
     * </p>
     *
     * @param strings array of strings that hold the values
     * @param types array of classes that the strings should be converted to
     * @param handleVarArgs flag whether to handle the variable number of arguments (<tt>true</tt>) or not (<tt>false</tt>)
     * @param argStartIndex index in the strings array which denotes the first changeable argument
     * @return the array of converted values
     * @throws InstantiationException if there was a type that cannot be created from the provided string value
     */
    public static Object[] parseTypesFromString(String[] strings, Class<?>[] types, boolean handleVarArgs, int argStartIndex) throws InstantiationException {
        return parseTypesFromString(strings, types, handleVarArgs, argStartIndex, strings.length - 1);
    }

    /**
     * Parses array of strings into array of objects according to the types provided in the second argument.
     * All the parameters from <code>argStartIndex</code> till the end of <code>strings</code> array will
     * be used.
     *
     * @param strings array of strings that hold the values
     * @param types array of classes that the strings should be converted to
     * @param handleVarArgs flag whether to handle the variable number of arguments (<tt>true</tt>) or not (<tt>false</tt>)
     * @param argStartIndex index in the strings array which denotes the first changeable argument
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @return the array of converted values
     * @throws InstantiationException if there was a type that cannot be created from the provided string value
     */
    public static Object[] parseTypesFromString(String[] strings, Class<?>[] types, boolean handleVarArgs, int argStartIndex, Map<String, ? extends Object> namedInstances) throws InstantiationException {
        return parseTypesFromString(strings, types, handleVarArgs, argStartIndex, strings.length - 1, namedInstances);
    }

    /**
     * Parses array of strings into array of objects according to the types provided in the second argument.
     * All the parameters from <code>strings</code> array will be used.
     *
     * <p>
     * Note that {@link messif.objects.LocalAbstractObject} parameters will not be converted. For this functionality,
     * use the full {@link #parseTypesFromString parseTypesFromString} method instead.
     * </p>
     *
     * @param strings array of strings that hold the values
     * @param types array of classes that the strings should be converted to
     * @param handleVarArgs flag whether to handle the variable number of arguments (<tt>true</tt>) or not (<tt>false</tt>)
     * @return the array of converted values
     * @throws InstantiationException if there was a type that cannot be created from the provided string value
     */
    public static Object[] parseTypesFromString(String[] strings, Class<?>[] types, boolean handleVarArgs) throws InstantiationException {
        return parseTypesFromString(strings, types, handleVarArgs, 0);
    }

    /**
     * Parses array of strings into array of objects according to the types provided in the second argument.
     * All the parameters from <code>strings</code> array will be used.
     *
     * @param strings array of strings that hold the values
     * @param types array of classes that the strings should be converted to
     * @param handleVarArgs flag whether to handle the variable number of arguments (<tt>true</tt>) or not (<tt>false</tt>)
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @return the array of converted values
     * @throws InstantiationException if there was a type that cannot be created from the provided string value
     */
    public static Object[] parseTypesFromString(String[] strings, Class<?>[] types, boolean handleVarArgs, Map<String, ? extends Object> namedInstances) throws InstantiationException {
        return parseTypesFromString(strings, types, handleVarArgs, 0, namedInstances);
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
     * Returns type-safe constructors of the given class.
     * @param <E> the class for which to get the constructors
     * @param objectClass the class for which to get the constructors
     * @param publicOnlyConstructors flag whether to return all declared constructors (<tt>false</tt>) or only the public ones (<tt>true</tt>)
     * @return a list of constructors of the given class
     */
    @SuppressWarnings("unchecked")
    public static <E> Constructor<E>[] getConstructors(Class<? extends E> objectClass, boolean publicOnlyConstructors) {
        return (Constructor<E>[])(publicOnlyConstructors ? objectClass.getConstructors() : objectClass.getDeclaredConstructors());  // This IS A STUPID unchecked !!!
    }

    /**
     * Returns type-safe public constructors of the given class.
     * @param <E> the class for which to get the constructors
     * @param objectClass the class for which to get the constructors
     * @return all public constructors of the given class
     */
    public static <E> Constructor<E>[] getConstructors(Class<? extends E> objectClass) {
        return getConstructors(objectClass, true);
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
     * so the copy has the specified length. If the original array is not <tt>null</tt>,
     * the data are copied starting from the index {@code offset} up the the
     * length of the original or the new array (whichever is smaller). Thus
     * the zeroth index of the new array will have the value of {@code offset} index
     * of the original array, and so on. All elements of the new array that does
     * not have the corresponding value in the original array (i.e. the elements
     * of the new array with indices {@code i >= original.length - offset}) will
     * be filled with <tt>null</tt>.
     * The resulting array is of the class {@code componentType}.
     *
     * @param <T> the type of objects in the new array
     * @param <U> the type of objects in the old array (must be assignable to T)
     * @param original the array to be copied
     * @param offset the index of the element from the original array from which to start copying the data
     * @param length the length of the new copied array to be returned
     * @param componentType the class of array components in the new array
     * @return a copy of the original array, truncated or padded with <tt>null</tt>
     * @throws NegativeArraySizeException if <tt>newLength</tt> is negative
     */
    public static <T, U extends T> T[] copyGenericArray(U[] original, int offset, int length, Class<T> componentType) throws NegativeArraySizeException {
        T[] copy = createGenericArray(componentType, length);
        if (original != null)
            System.arraycopy(original, offset, copy, 0, length);
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
        T[] ret = copyGenericArray(original, 0, (original == null)?1:(original.length + 1), componentType);
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

    /** Internal patter for {@link #trimAndUnquote} and {@link #splitBySpaceWithQuotes} methods */
    private static final Pattern quoteMatchingPattern = Pattern.compile("\\G\\s*(?:'((?:[^'\\\\]|\\\\.)*)'|\"((?:[^\"\\\\]|\\\\.)*)\"|(\\S+))(\\s*)");

    /**
     * Removes quotes from the given string (double or single).
     * Unquoted leading and trailing space will be removed as well.
     * Note that if quotes are not closed, the original text is returned.
     * @param text the text to unquote
     * @return the unquoted (and trimmed) text
     */
    public static String trimAndUnquote(String text) {
        if (text == null || text.isEmpty())
            return text;
        Matcher matcher = quoteMatchingPattern.matcher(text);
        if (!matcher.matches())
            return text;
        String str = matcher.group(1);
        if (str == null)
            str = matcher.group(2);
        if (str == null)
            str = matcher.group(3);
        return str;
    }

    /**
     * Splits the given string by white space, but preserve the whitespace
     * enclosed in (single or double) quotes. The quotes are removed in
     * the process.
     * @param text the string to split
     * @return the string split into subsequences
     * @throws IllegalArgumentException if there are unterminated quotes in the string
     */
    public static String[] splitBySpaceWithQuotes(String text) throws IllegalArgumentException {
        if (text == null || text.length() == 0)
            return new String[0];

        // Parse string by regular expression
        Matcher matcher = quoteMatchingPattern.matcher(text);
        List<String> args = new ArrayList<String>();
        StringBuilder lastStr = new StringBuilder();
        int lastMatchPos = 0;
        while (matcher.find()) {
            lastMatchPos = matcher.end();
            // Get the right or-ed group that has matched
            String str = matcher.group(1);
            if (str == null)
                str = matcher.group(2);
            if (str == null)
                str = matcher.group(3);
            // Add the string to the buffer
            lastStr.append(str);
            // If there is a space match, add the string to splitted array and start over
            if (matcher.group(4).length() > 0) {
                args.add(lastStr.toString());
                lastStr.setLength(0);
            }
        }
        if (lastMatchPos != text.length())
            throw new IllegalArgumentException("Missing quotes: " + text);
        args.add(lastStr.toString());
        return args.toArray(new String[args.size()]);
    }

    /**
     * Converts the values of the given iterator to string by separating
     * all the values by the given separator.
     * @param iterator the iterator with the values
     * @param separator the separator string inserted in between the values
     * @return the string with all values returned by the {@code iterator} separated by the {@code separator}
     */
    public static String iterableToString(Iterator<?> iterator, String separator) {
        if (iterator == null)
            return null;
        if (!iterator.hasNext())
            return "";
        StringBuilder str = new StringBuilder();
        while (true) {
            str.append(iterator.next());
            if (!iterator.hasNext())
                return str.toString();
            str.append(separator);
        }
    }

    /**
     * Converts the values of the given iterable instance to string by separating
     * all the values by the given separator.
     * This method can process either static arrays (of any type including primitive ones)
     * or any {@link Iterable}.
     * @param iterable the iterable with the values
     * @param separator the separator string inserted in between the values
     * @return the string with all values returned by the {@code iterable} separated by the {@code separator}
     */
    public static String iterableToString(Object iterable, String separator) {
        if (iterable == null) {
            return null;
        } else if (iterable instanceof Iterable) {
            return iterableToString(((Iterable<?>)iterable).iterator(), separator);
        } else if (iterable instanceof Iterator) {
            return iterableToString((Iterator<?>)iterable, separator);
        } else if (iterable.getClass().getComponentType() != null) {
            int endIndex = Array.getLength(iterable) - 1;
            if (endIndex == -1)
                return "";
            StringBuilder str = new StringBuilder();
            for (int i = 0; ; i++) {
                str.append(Array.get(iterable, i));
                if (i == endIndex)
                    return str.toString();
                str.append(separator);
            }
        } else {
            throw new IllegalArgumentException("IterableToString can only process iterable types or static arrays");
        }
    }

    /**
     * Return generic-safe {@link java.util.Map} type.
     * Note that only non-typed arguments can be passed as key/value class to be
     * generic-safe.
     *
     * @param <K> the type of keys in the returned map
     * @param <V> the type of values in the returned map
     * @param object the object that should be cast to other type
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
     * Copies values from the {@code sourceMap} to {@code destinationMap}.
     * Note that only non-<tt>null</tt> values that are instances of
     * {@code valuesClass} are copied.
     *
     * @param <K> the type of keys in the returned map
     * @param <V> the type of values in the returned map
     * @param sourceMap the source map that provides the values
     * @param keysToRetrieve the keys for which the values are copied from the source map
     * @param destinationMap the target map into which the values are put
     *          (if <tt>null</tt>, a new instance of {@link HashMap} is used)
     * @param valuesClass the class to cast the map values to
     * @return the modified map with the copied values
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> copyMapValues(Map<?, ?> sourceMap, Iterable<? extends K> keysToRetrieve, Map<K, V> destinationMap, Class<? extends V> valuesClass) {
        if (destinationMap == null)
            destinationMap = new HashMap<K, V>();
        for (K key : keysToRetrieve) {
            Object value = sourceMap.get(key);
            if (value != null && valuesClass.isInstance(value))
                destinationMap.put(key, (V)value); // This cast IS checked on the previous line
        }
        return destinationMap;
    }

    /**
     * Copies all values from the {@code sourceMap} to {@code destinationMap}.
     * Note that only non-<tt>null</tt> values that are instances of
     * {@code valuesClass} are copied.
     *
     * @param <K> the type of keys in the returned map
     * @param <V> the type of values in the returned map
     * @param sourceMap the source map that provides the values
     * @param destinationMap the target map into which the values are put
     *          (if <tt>null</tt>, a new instance of {@link HashMap} is used)
     * @param valuesClass the class to cast the map values to
     * @return the modified map with the copied values
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> copyAllMapValues(Map<? extends K, ?> sourceMap, Map<K, V> destinationMap, Class<? extends V> valuesClass) {
        if (destinationMap == null)
            destinationMap = new HashMap<K, V>();
        for (Map.Entry<? extends K, ?> entry : sourceMap.entrySet()) {
            Object value = entry.getValue();
            if (value != null && valuesClass.isInstance(value))
                destinationMap.put(entry.getKey(), (V)value); // This cast IS checked on the previous line
        }
        return destinationMap;
    }

    /**
     * Returns an object from parameter table.
     * If the parameter table is <tt>null</tt> or if it does not contain
     * the parameter with the specified name, the default value is returned.
     * Otherwise, the class of the parameter is checked and if it is not the
     * requested class (but it is a {@link String}), the {@link #stringToType}
     * conversion is tried.
     *
     * @param <T> the type of the returned parameter value
     * @param parameters the parameter table
     * @param paramName the name of parameter to get from the table; if there is no parameter of that 
     * @param paramClass the class to cast the value to
     * @param defaultValue the default value to return if there is no parameter
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
            return stringToType((String)value, paramClass, (Map<String, Object>)parameters.get("namedInstances"));
        } catch (InstantiationException e) {
            throw new ClassCastException(e.getMessage());
        }
    }

    /**
     * Replace all occurrences of the variable pattern with the value from the hash table.
     * The specified pattern group is used the the variable name.
     *
     * @param string the string to be modified
     * @param variableRegex regular expression that matches variables
     * @param variableRegexGroup parenthesis group within regular expression
     *          that holds the variable name
     * @param flagsRegexpGroup parenthesis group within regular expression
     *          that holds flags:
     *          ":defaultValue" the default value for a variable that is not present in the <code>variables</code> map,
     *          "!" the value is required
     * @param variables the variable names with their values
     * @return the original string with all variables replaced
     * @throws IllegalArgumentException if there was a required variable that has no value 
     */
    public static String substituteVariables(String string, Pattern variableRegex, int variableRegexGroup, int flagsRegexpGroup, Map<String, ?> variables) throws IllegalArgumentException {
        // Check null strings
        if (string == null)
            return null;

        // Prepare for matching
        Matcher matcher = variableRegex.matcher(string);
        StringBuffer sb = new StringBuffer();
        
        // Find every occurance of pattern in string
        while (matcher.find()) {
            // Backslashed match found
            if (string.charAt(matcher.start()) == '\\') {
                matcher.appendReplacement(sb, string.substring(matcher.start() + 1, matcher.end()));
                continue;
            }

            // Get variable with the name from the matched pattern group
            Object value = variables.get(matcher.group(variableRegexGroup));

            // Set the default value if specified
            if (value == null && flagsRegexpGroup > 0) {
                String flag = matcher.group(flagsRegexpGroup);
                if (flag != null) {
                    if (flag.startsWith(":"))
                        value = flag.substring(1);
                    else if (flag.startsWith("!"))
                        throw new IllegalArgumentException("Variable " + matcher.group(variableRegexGroup) + " is required but no value has been provided");
                }
            }

            // Do the replacement, if variable is not found, the variable placeholder is removed
            matcher.appendReplacement(sb, value != null ? Matcher.quoteReplacement(value.toString()) : "");
        }

        // Finish replacing
        matcher.appendTail(sb);

        // Return the string with replaced variables
        return sb.toString();
    }

    /**
     * Returns the today's specified time in milliseconds.
     * @param time the time in {@code hh:mm:ss.iii} format (seconds and milliseconds are optional)
     * @return the today's specified time in milliseconds
     * @throws NumberFormatException if the specified time has invalid format
     */
    public static long timeToMilliseconds(String time) throws NumberFormatException {
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
            case 1:
                return Long.parseLong(time);
            default:
                throw new NumberFormatException("At least hours and minutes must be specified");
        }
        try {
            calendar.setLenient(false);
            return calendar.getTimeInMillis();
        } catch (IllegalArgumentException e) {
            // The value for a field (message of the exception) was invalid
            StringBuilder str = new StringBuilder("Value of ");
            str.append(e.getMessage().toLowerCase());
            str.append(" is invalid");
            throw new NumberFormatException(str.toString());
        }
    }

    /**
     * Converts a string specifying hours/minutes/seconds time period.
     * The string must contain only numbers with a one-char specifier of
     * <b>h</b>ours, <b>m</b>inutes, or <b>s</b>econds. If no specifier is
     * given, the number represents milliseconds.
     * 
     * <p>Example: 1h 10 m 23s 999<br/>
     *      will return 4223999 milliseconds
     * </p>
     * 
     * @param hmsStr the string containing the hours/minutes/seconds time period
     * @return the number of milliseconds the string represents
     * @throws NumberFormatException if the format of the string is not recognized or the numbers cannot be converted
     */
    public static long hmsToMilliseconds(String hmsStr) throws NumberFormatException {
        Matcher matcher = Pattern.compile("\\G\\s*(\\d+)\\s*(\\D)?\\s*").matcher(hmsStr);
        long time = 0;
        int lastMatchEnd = 0;
        while (matcher.find()) {
            long mult;
            lastMatchEnd = matcher.end();
            String type = matcher.group(2);
            if (type == null || type.isEmpty()) {
                mult = 1;
            } else {
                switch (type.charAt(0)) {
                    case 'H':
                    case 'h':
                        mult = 60 * 60 * 1000;
                        break;
                    case 'M':
                    case 'm':
                        mult = 60 * 1000;
                        break;
                    case 'S':
                    case 's':
                        mult = 1000;
                        break;
                    default:
                        throw new NumberFormatException("Unknown time specification: " + type);
                }
            }
            time += mult*Long.parseLong(matcher.group(1));
        }
        if (lastMatchEnd != hmsStr.length())
            throw new NumberFormatException("Cannot understand hours/minutes/seconds value '" + hmsStr + "'");
        return time;
    }

    /**
     * Fills the given collection with integer indexes that correspond to the
     * coma-separated numbers or number ranges specified by the given string.
     * @param rangeSelectors the coma-separated numbers or number ranges
     * @param collection the collection to which to add the indexes
     * @throws NumberFormatException if the format of the string is not recognized or the numbers cannot be converted
     */
    public static void rangeSelectorsToIndexes(String rangeSelectors, Collection<? super Integer> collection) throws NumberFormatException {
        Matcher matcher = Pattern.compile("\\G(?:^|,)\\s*(\\d+)(?:-(\\d+))?").matcher(rangeSelectors);
        int lastMatchEnd = 0;
        while (matcher.find()) {
            lastMatchEnd = matcher.end();
            int number = Integer.parseInt(matcher.group(1));
            if (matcher.group(2) != null) {
                int range = Integer.parseInt(matcher.group(2));
                for (; number <= range; number++)
                    collection.add(number);
            } else {
                collection.add(number);
            }
        }
        if (lastMatchEnd != rangeSelectors.length())
            throw new NumberFormatException("Cannot understand range selector value '" + rangeSelectors + "'");
    }

    /**
     * Returns a collection of integer indexes that correspond to the coma-separated
     * numbers or number ranges specified by the given string.
     * @param rangeSelectors the coma-separated numbers or number ranges
     * @param removeDuplicates flag if each index is selected only once (<tt>true</tt>)
     *          or if the returned collection can contain the same indexes multiple times
     * @return a collection of indexes
     * @throws NumberFormatException if the format of the string is not recognized or the numbers cannot be converted
     */
    public static Collection<Integer> rangeSelectorsToIndexes(String rangeSelectors, boolean removeDuplicates) throws NumberFormatException {
        Collection<Integer> ret = removeDuplicates ? new LinkedHashSet<Integer>() : new ArrayList<Integer>();
        rangeSelectorsToIndexes(rangeSelectors, ret);
        return ret;
    }

    /**
     * Returns a table of all values from the specified enum keyed by the enum value name.
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

    /**
     * Read data from input stream into a byte buffer.
     * If the {@code maxBytes} parameter is greater than zero, then no more than
     * {@code maxBytes} will be read from the input stream. Otherwise, the buffer
     * will contain all the data from the input stream until the end-of-stream.
     * <p>
     * Note that the stream is not closed.
     * </p>
     *
     * @param inputStream the stream from which to read the data
     * @param maxBytes maximal number of bytes to read from the stream (unlimited if less or equal to zero)
     * @return a buffer containing the data
     * @throws IOException if there was a problem reading from the input stream
     */
    public static byte[] readStreamData(InputStream inputStream, int maxBytes) throws IOException {
        // Create buffer (has always at least bufferSize bytes available)
        byte[] buffer = new byte[maxBytes > 0 ? maxBytes : readStreamDataAllocation];
        int offset = 0;
        int bytes;
        while ((bytes = inputStream.read(buffer, offset, buffer.length - offset)) > 0) {
            offset += bytes;
            // Check if the buffer is not full
            if (offset == buffer.length && maxBytes <= 0) {
                // Add some space
                byte[] copy = new byte[offset + readStreamDataAllocation];
                System.arraycopy(buffer, 0, copy, 0, offset);
                buffer = copy;
            }
        }

        // Shrink the array
        if (offset != buffer.length) {
            byte[] copy = new byte[offset];
            System.arraycopy(buffer, 0, copy, 0, offset);
            buffer = copy;
        }

        return buffer;
    }

    /**
     * Read data from the string reader into a string builder.
     * Note that the reader {@code data} is {@link Reader#close() closed} after
     * the data are read.
     * @param data the reader to retrieve the data from
     * @param str the buffer to store the data to
     * @return the string buffer with data (i.e. the {@code str} or,
     *          if {@code str} was <tt>null</tt>, a new instance of {@link StringBuilder})
     * @throws IOException if there was a problem reading the data
     */
    public static StringBuilder readStringData(Reader data, StringBuilder str) throws IOException {
        if (str == null)
            str = new StringBuilder();
        try {
            char[] buf = new char[1024];
            int len;
            while ((len = data.read(buf)) != -1)
                str.append(buf, 0, len);
        } finally {
            data.close();
        }
        return str;
    }

    /**
     * Read data from the input stream into a string builder.
     * Note that the input stream {@code data} is {@link InputStream#close() closed} after
     * the data are read.
     * @param data the stream to retrieve the data from
     * @param str the buffer to store the data to
     * @return the string buffer with data (i.e. the {@code str} or,
     *          if {@code str} was <tt>null</tt>, a new instance of {@link StringBuilder})
     * @throws IOException if there was a problem reading the data
     */
    public static StringBuilder readStringData(InputStream data, StringBuilder str) throws IOException {
        return readStringData(new InputStreamReader(data), str);
    }

    /**
     * Copies a resource data to a given local file.
     * @param resourcePath the absolute path of the resource
     * @param localFile the local file where the resource will be copied
     * @param overwrite flag whether to silently overwrite the local file if it already exists
     * @throws FileNotFoundException if the resource was not found, the destination local file cannot be created,
     *          or (when overwrite flag is true) the local file already exists
     * @throws IOException if there was a problem writing the local file
     * @see Class#getResource(java.lang.String)
     */
    public static void resourceToLocalFile(String resourcePath, File localFile, boolean overwrite) throws FileNotFoundException, IOException {
        InputStream resource = Convert.class.getResourceAsStream(resourcePath);
        if (resource == null)
            throw new FileNotFoundException("Resource '" + resourcePath + "' not found");
        try {
            if (!overwrite && localFile.exists())
                throw new FileNotFoundException("Cannot create '" + localFile + "', it already exists");
            FileOutputStream out = new FileOutputStream(localFile);
            try {
                byte[] buf = new byte[4069];
                int len;
                while ((len = resource.read(buf)) != -1)
                    out.write(buf, 0, len);
            } finally {
                out.close();
            }
        } finally {
            resource.close();
        }
    }

    /**
     * Copies a resource data to a generated temporary file.
     * @param resourcePath the absolute path of the resource
     * @return the generated temporary file where the file has been stored
     * @throws FileNotFoundException if the resource was not found, the destination local file cannot be created,
     *          or (when overwrite flag is true) the local file already exists
     * @throws IOException if there was a problem writing the local file
     * @see Class#getResource(java.lang.String)
     */
    public static File resourceToTemporaryFile(String resourcePath) throws IOException {
        int extPos = resourcePath.lastIndexOf('.');
        File ret = File.createTempFile("restotemp", extPos == -1 ? null : resourcePath.substring(extPos)); // Preserve extension
        ret.deleteOnExit();
        resourceToLocalFile(resourcePath, ret, true);
        return ret;
    }
}