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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectList;
import messif.objects.util.AbstractStreamObjectIterator;

/**
 * Utility class that provides methods for type conversions and instantiation.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
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
    public static <E> E stringToType(String string, Class<E> type, Map<String, Object> namedInstances) throws InstantiationException {
        if (string.equals("null"))
            return null;

        // Converting string types
        if (type == String.class)
            return type.cast(string);

        // Converting class types
        if (type == Class.class) try {
            return type.cast(Class.forName(string));
        } catch (ClassNotFoundException e) {
            throw new InstantiationException(e.toString());
        }

        // Named instances of objects
        if (namedInstances != null) {
            Object instance = namedInstances.get(string);
            if (instance != null) {
                // Return named object as-is
                if (type.isInstance(instance))
                    return type.cast(instance);

                // Try iterator
                if (instance instanceof Iterator)
                    return type.cast(((Iterator<?>)instance).next());
            }
        }

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
            String[] items = string.split("\\p{Space}*[|,]\\p{Space}*");
            Class<?> componentType = type.getComponentType();
            Object array = Array.newInstance(componentType, items.length);
            for (int i = 0; i < items.length; i++)
                Array.set(array, i, stringToType(items[i], componentType, namedInstances));
            return type.cast(array);
        }

        // Wrap primitive types, so that their 'valueOf' method can be used
        if (type.isPrimitive())
            type = wrapPrimitiveType(type);

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
            return stringToType((String)value, paramClass, (Map<String, Object>)parameters.get("namedInstances"));
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