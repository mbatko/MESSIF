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
import messif.objects.GenericAbstractObjectList;
import messif.objects.LocalAbstractObject;
import messif.objects.StreamGenericAbstractObjectIterator;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author xbatko
 */
public abstract class Convert {
    
    /**
     * Converts a string into object of the specified type.
     * @param string the string value to be converted
     * @param type the class of the value
     * @param objectStreams map of openned streams for getting {@link messif.objects.LocalAbstractObject objects}
     * @return the converted value
     * @throws InstantiationException if the type cannot be created from the string value
     */
    @SuppressWarnings("unchecked")
    public static <E> E stringToType(String string, Class<E> type, Map<String, StreamGenericAbstractObjectIterator<LocalAbstractObject>> objectStreams) throws InstantiationException {
        if (string.equals("null"))
            return null;
        
        // Converting the primitive types
        if (type.isPrimitive()) 
            try {
                if (type == Integer.TYPE)
                    return (E)Integer.valueOf(string); // This cast IS checked
                if (type == Long.TYPE)
                    return (E)Long.valueOf(string); // This cast IS checked
                if (type == Boolean.TYPE)
                    return (E)Boolean.valueOf(string); // This cast IS checked
                if (type == Double.TYPE)
                    return (E)Double.valueOf(string); // This cast IS checked
                if (type == Byte.TYPE)
                    return (E)Byte.valueOf(string); // This cast IS checked
                if (type == Float.TYPE)
                    return (E)Float.valueOf(string); // This cast IS checked
                if (type == Short.TYPE)
                    return (E)Short.valueOf(string); // This cast IS checked
                if (type == Character.TYPE)
                    return (E)Character.valueOf(string.charAt(0)); // This cast IS checked
                throw new InstantiationException("Can't create '" + type.getName() + "' from '" + string + "' because " + type + " is an unknown primitive type");
            } catch (NumberFormatException e) {
                throw new InstantiationException(e.toString());
            }

        if (type.equals(String.class))
            return (E)string; // This cast IS checked
        
        // Object stream type is returned if found
        if ((objectStreams != null) && type.isAssignableFrom(StreamGenericAbstractObjectIterator.class)) {
            E rtv = (E)objectStreams.get(string); // This cast IS checked
            if (rtv == null)
                throw new InstantiationException("Stream '" + string + "' is not opened");
            return rtv;
        }

        // Try to get LocalAbstractObject from string
        if ((objectStreams != null) && type.isAssignableFrom(GenericAbstractObjectList.class)) {
            int colonPos = string.lastIndexOf(':');
            if (colonPos != -1) {
                StreamGenericAbstractObjectIterator<LocalAbstractObject> objectIterator = objectStreams.get(string.substring(0, colonPos));
                if (objectIterator != null)
                    try {
                        return (E)new GenericAbstractObjectList(objectIterator, Integer.parseInt(string.substring(colonPos + 1)));
                    } catch (NumberFormatException e) {
                        // Ignored, might get converted later
                    }
            }
        }
        
        // Try to get LocalAbstractObject from string
        if (type.isAssignableFrom(LocalAbstractObject.class)) {
            if (objectStreams != null) {
                StreamGenericAbstractObjectIterator<LocalAbstractObject> objectIterator = objectStreams.get(string);
                if (objectIterator != null)
                    // Returns next object or throws NoSuchElement exception if there is no next objects
                    return (E)objectIterator.next(); // This cast IS checked
            }

            try {
                return (E)LocalAbstractObject.valueOf(string); // This cast IS checked
            } catch (InvocationTargetException e) {
                throw new InstantiationException("Can't create '" + type.getName() + "' from '" + string + "' - there is no stream with this name and " + e.getCause().toString());
            }
        }

        // Converting class types
        if (type.equals(Class.class)) try {
            return (E)Class.forName(string); // This cast IS checked
        } catch (ClassNotFoundException e) {
            throw new InstantiationException(e.toString());
        }

        // Converting string maps
        if (type.equals(Map.class)) {
            Matcher m = Pattern.compile("\\p{Space}*(\"([^\"]*)\"|[^=]*?)\\p{Space}*=\\p{Space}*(\"([^\"]*)\"|[^=]*?)\\p{Space}*(,|$)").matcher(string);
            Map<String, Object> rtv = new HashMap<String, Object>();
            while (m.find()) {
                String value = (m.group(4) == null)?m.group(3):m.group(4);
                if (value.equals("null"))
                    value = null;
                rtv.put((m.group(2) == null)?m.group(1):m.group(2), value);
            }
            // Add streams parameter to a Map that contain a 'objectStreams' key but it is null
            if (rtv.containsKey("objectStreams") && rtv.get("objectStreams") == null)
                rtv.put("objectStreams", objectStreams);
            return (E)rtv; // This cast IS checked
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
        
        throw new InstantiationException("String '" + string + "' cannot be converted into '" + type.toString() + "'");
    }

    /**
     * Converts a string into object of the specified type.
     * @param string the string value to be converted
     * @param type the class of the value
     * @return the converted value
     * @throws InstantiationException if the type cannot be created from the string value
     */
    public static <E> E stringToType(String string, Class<E> type) throws InstantiationException {
        return stringToType(string, type, null);
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
     * @param name the fully qualified name of the class
     * @param checkClass the class that is returned - must be equal or the super class for the created one
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
     * @param classObject the class object to be cast
     * @param checkClass the generics typed class that is returned
     * @return the generics-typed <code>Class</code> object
     * @throws ClassCastException if passed <code>classObject</code> is not subclass of <code>checkClass</code>
     */
    @SuppressWarnings("unchecked")
    public static <E> Class<E> genericCastToClass(Object classObject, Class<E> checkClass) throws ClassCastException {
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
     * @param objectStreams map of openned streams for getting LocalAbstractObjects
     * @return the array of converted values
     * @throws InstantiationException if there was a type that cannot be created from the provided string value
     */
    public static Object[] parseTypesFromString(String[] strings, Class<?>[] types, int argStartIndex, int argEndIndex, Map<String, StreamGenericAbstractObjectIterator<LocalAbstractObject>> objectStreams) throws InstantiationException {
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
                Array.set(rtv[types.length - 1], i, stringToType(strings[argEndIndex], varargClass, objectStreams));
        }

        // Convert every string to a proper class
        for (int i = 0; argStartIndex <= argEndIndex; argStartIndex++, i++)
            rtv[i] = stringToType(strings[argStartIndex], types[i], objectStreams);
        
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
     * @param objectStreams map of openned streams for getting LocalAbstractObjects
     * @return the array of converted values
     * @throws InstantiationException if there was a type that cannot be created from the provided string value
     */
    public static Object[] parseTypesFromString(String[] strings, Class<?>[] types, int argStartIndex, Map<String, StreamGenericAbstractObjectIterator<LocalAbstractObject>> objectStreams) throws InstantiationException {
        return parseTypesFromString(strings, types, argStartIndex, strings.length - 1, objectStreams);
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
     * @param objectStreams map of openned streams for getting LocalAbstractObjects
     * @return the array of converted values
     * @throws InstantiationException if there was a type that cannot be created from the provided string value
     */
    public static Object[] parseTypesFromString(String[] strings, Class<?>[] types, Map<String, StreamGenericAbstractObjectIterator<LocalAbstractObject>> objectStreams) throws InstantiationException {
        return parseTypesFromString(strings, types, 0, objectStreams);
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
     * @param constructors the list of constructors of the desired class to try
     * @param arguments the string arguments for the constructor that will be converted to correct types
     * @param argStartIndex index in the string arguments array from which to expect arguments (all the previous items are ignored)
     * @param argEndIndex index in the string arguments array to which to expect arguments (all the following items are ignored)
     * @param objectStreams map of openned streams for getting LocalAbstractObjects
     * @return a new instance of the class the constructors were specified for
     * @throws InvocationTargetException
     *              if the constructor can't be found for the specified arguments,
     *              the argument string-to-type convertion has failed or
     *              there was an error during instantiation
     */
    public static <E> E createInstanceWithStringArgs(List<Constructor<E>> constructors, String[] arguments, int argStartIndex, int argEndIndex, Map<String, StreamGenericAbstractObjectIterator<LocalAbstractObject>> objectStreams) throws InvocationTargetException {
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
                return constructor.newInstance(parseTypesFromString(arguments, argTypes, argStartIndex, argEndIndex, objectStreams));
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
     * @param constructors the list of constructors of the desired class to try
     * @param arguments the string arguments for the constructor that will be converted to correct types
     * @param argStartIndex index in the string arguments array from which to expect arguments (all the previous items are ignored)
     * @param objectStreams map of openned streams for getting LocalAbstractObjects
     * @return a new instance of the class the constructors were specified for
     * @throws InvocationTargetException
     *              if the constructor can't be found for the specified arguments,
     *              the argument string-to-type convertion has failed or
     *              there was an error during instantiation
     */
    public static <E> E createInstanceWithStringArgs(List<Constructor<E>> constructors, String[] arguments, int argStartIndex, Map<String, StreamGenericAbstractObjectIterator<LocalAbstractObject>> objectStreams) throws InvocationTargetException {
        return createInstanceWithStringArgs(constructors, arguments, argStartIndex, arguments.length - 1, objectStreams);
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
     * @param constructors the list of constructors of the desired class to try
     * @param arguments the string arguments for the constructor that will be converted to correct types
     * @param objectStreams map of openned streams for getting LocalAbstractObjects
     * @return a new instance of the class the constructors were specified for
     * @throws InvocationTargetException
     *              if the constructor can't be found for the specified arguments,
     *              the argument string-to-type convertion has failed or
     *              there was an error during instantiation
     */
    public static <E> E createInstanceWithStringArgs(List<Constructor<E>> constructors, String[] arguments, Map<String, StreamGenericAbstractObjectIterator<LocalAbstractObject>> objectStreams) throws InvocationTargetException {
        return createInstanceWithStringArgs(constructors, arguments, 0, objectStreams);
    }

    /**
     * Creates a new instance of a class.
     * First, a constructor for the specified arguments is searched in the provided class.
     * Then, an instance is created and returned.
     *
     * @param instanceClass the class for which to create an instance
     * @param arguments the arguments for the constructor
     * @return a new instance of the class
     * @throws NoSuchMethodException if there was no constructor for the specified list of arguments
     * @throws InvocationTargetException if there was an exception during instantiation
     */
    @SuppressWarnings("unchecked")
    public static <E> E createInstanceWithInheritableArgs(Class<E> instanceClass, Object... arguments) throws NoSuchMethodException, InvocationTargetException {
        Class<?>[] argTypes = getObjectTypes(arguments);
        for (Constructor<E> constructor : (Constructor<E>[])instanceClass.getConstructors()) // This cast IS A STUPID BUG!!!!
            if (isPrototypeMatching(constructor.getParameterTypes(), argTypes))
                try {
                    return constructor.newInstance(arguments);
                } catch (IllegalAccessException e) {
                    // Cant access constructor, try another one
                } catch (InstantiationException e) {
                    throw new NoSuchMethodException(e.getMessage());
                }
        throw new NoSuchMethodException("There is no constructor for '" + instanceClass.toString() + "' matching the supplied arguments");
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
     * Return generic type casted object with correct type cast check.
     *
     * @param object the object that sould be cast to other type
     * @param castToClass the class to cast the object to
     * @throws ClassCastException if the specified object is not an instance of the specified class
     * @return the object with different type
     */
    @SuppressWarnings("unchecked")
    public static <T> T safeGenericCast(Object object, Class<T> castToClass) throws ClassCastException {
        if (object == null)
            return null;
        if (castToClass.isInstance(object))
            return (T)object; // This cast IS checked
        else throw new ClassCastException("Can't cast object of class '" + object.getClass() + "' to '" + castToClass.getName() + "'");
    }

    /**
     * Return generic-safe {@link java.util.Map} type.
     * Note that only non-typed arguments can be passed as key/value class to be
     * generic-safe.
     *
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
     * Replace all occurances of the variable pattern with the value from the hash table.
     * The specified pattern group is used the the variable name.
     *
     * @param string the string to be modified
     * @param variableRegex regular expression that matches variables
     * @param variableRegexGroup parenthesis group within regular expression that holds the variable name
     * @param variables the variable names with their values
     * @return the original string with all variables replaced
     */
    public static String substituteVariables(String string, Pattern variableRegex, int variableRegexGroup, Map<String,String> variables) {
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

            // Do the replacement, if variable is not found, the variable placeholder is removed
            matcher.appendReplacement(sb, (value != null)?value:"");
        }

        // Finish replacing
        matcher.appendTail(sb);

        // Return the string with replaced variables
        return sb.toString();
    }

}
