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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Pattern;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;


/**
 * This class provides getter extension for the basic {@link Properties set of properties}.
 * The following features are added:
 * <ul>
 * <li>Reading properties using a {@link ClassLoader}.</li>
 * <li>Property getter that throws exception if value is not found.</li>
 * <li>Message-formated property getter.</li>
 * <li>Primitive types property getters.</li>
 * <li>Array (multi-values) property getters.</li>
 * <li>Class property getters.</li>
 * <li>Factory method and constructor property getters</li>
 * <li>Database connection property getter.</li>
 * </ul>
 * 
 * <p>
 * In addition, a caching facility is provided if the
 * {@link #getProperties(java.lang.String) factory method} is used, i.e.
 * the same {@code ExtendedProperties} instance is returned for the same resource.
 * </p>
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ExtendedProperties extends Properties {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Constants ******************//

    /** Pattern that match variables properties */
    private static final Pattern variablePattern = Pattern.compile("<([^>]+?)(?::([^>]+))?>", Pattern.MULTILINE);
    /** Group number in the <code>variablePattern</code> that represents the variable name */
    private static final int variablePatternNameGroup = 1;
    /** Group number in the <code>variablePattern</code> that represents the default value for the variable */
    private static final int variablePatternDefaultValueGroup = 2;


    //****************** Attributes ******************//

    /** Properties cache */
    protected static final Map<Object, ExtendedProperties> cache = new HashMap<Object, ExtendedProperties>();


    //****************** Constructors ******************//

    /**
     * Creates an empty property list with no default values.
     */
    public ExtendedProperties() {
        super();
    }

    /**
     * Creates an empty property list with the specified defaults.
     *
     * @param defaults the defaults
     */
    public ExtendedProperties(Properties defaults) {
        super(defaults);
    }


    //****************** Cloning ******************//

    /**
     * Creates a shallow copy of this properties with the given defaults.
     * The defaults can be set only if the copied properties have none.
     *
     * @param defaults the defaults
     * @return a clone of the properties
     * @throws IllegalStateException if the defaults cannot be set
     */
    public ExtendedProperties clone(Properties defaults) throws IllegalStateException {
        if (defaults != null && this.defaults != null)
            throw new IllegalStateException("This properties already have defaults");
        try {
            ExtendedProperties ret = (ExtendedProperties)clone();
            ret.defaults = defaults;
            return ret;
        } catch (ClassCastException e) {
            // This should never happen, since the cloned properties should be of this class
            throw new InternalError();
        }
    }


    //****************** Factory methods ******************//

    /**
     * Returns a cached instance of ExtendedProperties for the specified file.
     * If there is no instance in the cache yet, create a new one, populate it
     * from the property file and cache it.
     *
     * @param file the file to load the properties from
     * @return a cached instance of ExtendedProperties for the specified file
     * @throws ExtendedPropertiesException if there was a problem creating properties
     */
    public static ExtendedProperties getProperties(String file) throws ExtendedPropertiesException {
        // Get the properties from cache
        ExtendedProperties ret = cache.get(file);

        // If it is not in the cache yet, create a new instance and add it to the cache
        if (ret == null)
            try {
                ret = new ExtendedProperties();
                ret.load(new FileInputStream(file));
                cache.put(file, ret);
            } catch (Exception e) {
                throw new ExtendedPropertiesException("Cannot create properties for file '" + file + "': " + e, e);
            }

        return ret;
    }

    /**
     * Returns a cached instance of ExtendedProperties for the specified class.
     * If there is no instance in the cache yet, create a new one, populate it
     * from the class's properties and cache it.
     * 
     * @param clazz the class to load the properties for
     * @return a cached instance of ExtendedProperties for the specified class
     * @throws ExtendedPropertiesException if there was a problem creating properties
     */
    public static ExtendedProperties getProperties(Class<?> clazz) throws ExtendedPropertiesException {
        // Get the properties from cache
        ExtendedProperties ret = cache.get(clazz);

        // If it is not in the cache yet, create a new instance and add it to the cache
        if (ret == null)
            try {
                ret = new ExtendedProperties();
                ret.load(clazz);
                cache.put(clazz, ret);
            } catch (Exception e) {
                throw new ExtendedPropertiesException("Cannot create properties for " + clazz + ": " + e, e);
            }

        return ret;
    }

    /**
     * Return {@link ExtendedProperties} from the specified <code>properties</code> with variable expansion.
     * Only keys with the given prefix are copied and the prefix is removed from the keys.
     * A {@link Convert#substituteVariables variable substitution} is
     * performed on all values (no substitution is done on keys).
     *
     * @param properties the properties to copy from
     * @param prefix the starting prefix on the keys
     * @param defaultProperties the default properties to use (see {@link Properties#Properties(java.util.Properties)})
     * @param variables the variable names with their values
     * @return a new instance of {@link ExtendedProperties} populated from <code>properties</code>
     */
    public static ExtendedProperties restrictProperties(Properties properties, String prefix, Properties defaultProperties, Map<String, String> variables) {
        ExtendedProperties ret = new ExtendedProperties(defaultProperties);
        ret.load(properties, prefix, variables);
        return ret;
    }

    /**
     * Return {@link ExtendedProperties} from the specified <code>properties</code> with variable expansion.
     * Only keys with the given prefix are copied and the prefix is removed from the keys.
     * A {@link Convert#substituteVariables variable substitution} is
     * performed on all values (no substitution is done on keys).
     *
     * @param properties the properties to copy from
     * @param prefix the starting prefix on the keys
     * @param variables the variable names with their values
     * @return a new instance of {@link ExtendedProperties} populated from <code>properties</code>
     */
    public static ExtendedProperties restrictProperties(Properties properties, String prefix, Map<String, String> variables) {
        return restrictProperties(properties, prefix, null, variables);
    }

    /**
     * Return {@link ExtendedProperties} from the specified <code>properties</code>.
     * Only keys with the given prefix are copied and the prefix is removed from the
     * keys.
     *
     * @param properties the properties to copy from
     * @param prefix the starting prefix on the keys
     * @return a new instance of {@link ExtendedProperties} populated from <code>properties</code>
     */
    public static ExtendedProperties restrictProperties(Properties properties, String prefix) {
        return restrictProperties(properties, prefix, null);
    }

    /**
     * Return a new instance of {@link ExtendedProperties} loaded from the specified {@code map}.
     * @param map the map of key-value pairs to load into the properties
     * @return a new instance of {@link ExtendedProperties}
     */
    public static ExtendedProperties createPropertiesFromMap(Map<String, String> map) {
        ExtendedProperties ret = new ExtendedProperties();
        ret.load(map);
        return ret;
    }

    /**
     * Creates a new instance of {@link ExtendedProperties} from the given {@code properties}.
     * If a {@code clazz} is given and a property file for that class exists, it
     * is used as properties default for the new instance.
     * @param properties the properties to load into the new instance
     * @param clazz the class to load the properties for
     * @return a new instance of {@link ExtendedProperties}
     * @throws IllegalArgumentException if there was an error reading the class properties
     */
    public static ExtendedProperties createPropertiesWithClassDefault(Properties properties, Class<?> clazz) throws IllegalArgumentException {
        ExtendedProperties ret = new ExtendedProperties();
        if (clazz != null) {
            try {
                ret.load(clazz);
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot read class properties: " + e, e);
            }
        }
        if (properties != null) {
            if (!ret.isEmpty())
                ret = new ExtendedProperties(ret);
            ret.load(properties, null);
        }
        return ret;
    }


    //****************** Additional load methods ******************//

    /**
     * Populate this properties with the data stored in a <code>clazz</code>'s property file.
     * That is, the file with the same package, the same name as the <code>clazz</code>
     * and the "properties" extension.
     *
     * @param clazz the class to load the properties for
     * @return <tt>false</tt> if the property file was not found
     * @throws IOException if an error occurred when reading from the property file
     * @throws IllegalArgumentException if the property file contains a malformed Unicode escape sequence
     */
    public boolean load(Class<?> clazz) throws IOException, IllegalArgumentException {
        return load(clazz.getClassLoader(), clazz.getName().replace('.', '/') + ".properties");
    }

    /**
     * Populate this properties with the data stored in <code>resourceName</code>.
     * 
     * @param loader the class loader to use to access the resource
     * @param resourceName the name of a resource to load
     * @return <tt>false</tt> if the resource was not found
     * @throws IOException if an error occurred when reading from the property file
     * @throws IllegalArgumentException if the property file contains a malformed Unicode escape sequence
     */
    public boolean load(ClassLoader loader, String resourceName) throws IOException, IllegalArgumentException {
        InputStream in = loader.getResourceAsStream(resourceName);
        if (in == null) // Resource was not found
            return false;
        load(in);
        return true;
    }

    /**
     * Populate this properties with the data stored in another {@link Properties properties}.
     *
     * @param properties the propertries to load
     * @param prefix if <tt>null</tt>, all keys from the <code>properties</code>
     *          are copied; otherwise, only the keys that starts with the prefix
     *          are copied without the prefix
     * @throws NullPointerException if the <code>properties</code> is null
     */
    public void load(Properties properties, String prefix) throws NullPointerException {
        load(properties, prefix, null);
    }

    /**
     * Populate this properties with the data stored in another {@link Properties properties}.
     * Variable substitution is performed on property values (keys are not substituted).
     * Variables have <code>&lt;name:default_value&gt;</code> format, where the <code>:default_value</code>
     * part is optional (empty string will be placed if an unknown variable is encountered).
     * If a <code>prefix</code> is specified, only keys that begins with that prefix are
     * copied and the prefix is removed in the process.
     *
     * @param properties the propertries to load
     * @param prefix if <tt>null</tt>, all keys from the <code>properties</code>
     *          are copied; otherwise, only the keys that starts with the prefix
     *          are copied without the prefix
     * @param variables the variable names with their values
     * @throws NullPointerException if the <code>properties</code> is null
     */
    public void load(Properties properties, String prefix, Map<String,String> variables) throws NullPointerException {
        load(properties, prefix, variablePattern, variablePatternNameGroup, variablePatternDefaultValueGroup, variables);
    }

    /**
     * Populate this properties with the data stored in another {@link Properties properties}.
     * Variable substitution is performed on property values (keys are not substituted).
     * If a <code>prefix</code> is specified, only keys that begins with that prefix are
     * copied and the prefix is removed in the process.
     *
     * @param properties the properties to load
     * @param prefix if <tt>null</tt>, all keys from the <code>properties</code>
     *          are copied; otherwise, only the keys that starts with the prefix
     *          are copied without the prefix
     * @param variableRegex regular expression that matches variables
     * @param variableRegexGroup parenthesis group within regular expression
     *          that holds the variable name
     * @param defaultValueRegexGroup parenthesis group within regular expression
     *          that holds the default value for a variable that is not present
     *          in the <code>variables</code> map
     * @param variables the variable names with their values
     * @throws NullPointerException if the <code>properties</code> is null
     */
    public void load(Properties properties, String prefix, Pattern variableRegex, int variableRegexGroup, int defaultValueRegexGroup, Map<String,String> variables) throws NullPointerException {
        Enumeration<?> names = properties.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String)names.nextElement();
            if (prefix != null && !name.startsWith(prefix))
                continue;
            String value = properties.getProperty(name);
            if (variables != null)
                value = Convert.substituteVariables(value, variableRegex, variableRegexGroup, defaultValueRegexGroup, variables);
            setProperty((prefix == null)?name:name.substring(prefix.length()), value);
        }
    }

    /**
     * Populate this properties with the data from a {@link Map}.
     * Note that any existing keys are replaced.
     * @param map the map of key-value pairs to load
     */
    public void load(Map<String, String> map) {
        for (Entry<String, String> entry : map.entrySet()) {
            setProperty(entry.getKey(), entry.getValue());
        }
    }


    //****************** Required property getters ******************//

    /**
     * Searches for the property with the specified key in this property list.
     * If the value was not found in property <code>key</code> a
     * {@link ExtendedPropertiesException} is thrown instead of returning <tt>null</tt>.
     *
     * @param key the hashtable key
     * @return the value for the specified <code>key</code>
     * @throws ExtendedPropertiesException if the value was not found and default value is <tt>null</tt>
     */
    public String getRequiredProperty(String key) throws ExtendedPropertiesException {
        String ret = getProperty(key);
        if (ret != null)
            return ret;
        throw new ExtendedPropertiesException(getFormattedProperty("propertyNotFound", "Property \"{0}\" is required but it was not found", key));
    }


    //****************** Message format-getter methods ******************//

    /**
     * Formats a message using the property with the specified key in this property list.
     * If the value was not found in property <code>key</code> the <code>defaultValue</code> is used.
     * The message is then formatted according to {@link MessageFormat#format}.
     * @param key the hashtable key of the message
     * @param defaultValue a default value if the property <code>key</code> is <tt>null</tt>
     * @param parameters an array of objects to be formatted and substituted 
     * @return a formatted message
     * @throws IllegalArgumentException if a parameter in the <code>parameters</code> array is not of the type expected by the format element(s) that use it
     */
    public String getFormattedProperty(String key, String defaultValue, Object... parameters) throws IllegalArgumentException {
        String message = getProperty(key, defaultValue);
        if (message == null)
            return null;
        return MessageFormat.format(message, parameters);
    }


    //****************** Primitive types property getters ******************//

    /**
     * Returns an integer value from the given property. If there is no value
     * for the key, a {@link ExtendedPropertiesException} is thrown.
     *
     * @param key the hashtable key
     * @return an integer value from the given property
     * @throws ExtendedPropertiesException if the property was not found or its value is not a valid integer
     */
    public int getRequiredIntProperty(String key) throws ExtendedPropertiesException {
        String value = getRequiredProperty(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ExtendedPropertiesException(getFormattedProperty("invalidInteger", "Invalid integer for configuration key \"{0}\": {1}", key, value, e));
        }
    }

    /**
     * Returns an integer value from the given property.
     * If there is no value for the key, the supplied default value is returned.
     *
     * @param key the hashtable key
     * @param defaultValue a default value if the property <code>key</code> is <tt>null</tt>
     * @return an integer value from the given property
     * @throws ExtendedPropertiesException if the property's value is not a valid integer
     */
    public int getIntProperty(String key, int defaultValue) throws ExtendedPropertiesException {
        String value = getProperty(key);
        try {
            return (value == null)?defaultValue:Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ExtendedPropertiesException(getFormattedProperty("invalidInteger", "Invalid integer for configuration key \"{0}\": {1}", key, value, e));
        }
    }

    /**
     * Returns an integer value from the given property.
     * If there is no value for the key, the supplied default value is returned.
     * The returned value's range is checked. To impose a required check,
     * supply a default value outside the checked range.
     * @param key the hashtable key
     * @param defaultValue a default value if the property <code>key</code> is <tt>null</tt>
     * @param minValue
     * @param maxValue
     * @return an integer value from the given property
     * @throws ExtendedPropertiesException if the property's value is not a valid integer or it is out of range
     */
    public int getIntProperty(String key, int defaultValue, int minValue, int maxValue) throws ExtendedPropertiesException {
        int value = getIntProperty(key, defaultValue);
        if (value < minValue || value > maxValue)
            throw new ExtendedPropertiesException(getFormattedProperty("valueOutOfRange", "Value \"{1}\" for configuration key \"{0}\" is out of [{2}, {3}] range", key, value, minValue, maxValue));
        return value;
    }

    /**
     * Returns a boolean value from the given property. If there is no value
     * for the key, a {@link ExtendedPropertiesException} is thrown.
     *
     * @param key the hashtable key
     * @return an boolean value from the given property
     * @throws ExtendedPropertiesException if the property's value is not a valid integer
     */
    public boolean getRequiredBoolProperty(String key) throws ExtendedPropertiesException {
        String value = getRequiredProperty(key);
        try {
            return Boolean.parseBoolean(value);
        } catch (NumberFormatException e) {
            throw new ExtendedPropertiesException(getFormattedProperty("invalidBoolean", "Invalid boolean for configuration key \"{0}\": {1}", key, value, e));
        }
    }

    /**
     * Returns a boolean value from the given property.
     * If there is no value for the key, the supplied default value is returned.
     *
     * @param key the hashtable key
     * @param defaultValue a default value if the property <code>key</code> is <tt>null</tt>
     * @return an boolean value from the given property
     * @throws ExtendedPropertiesException if the property's value is not a valid integer
     */
    public boolean getBoolProperty(String key, boolean defaultValue) throws ExtendedPropertiesException {
        String value = getProperty(key);
        try {
            return (value == null)?defaultValue:Boolean.parseBoolean(value);
        } catch (NumberFormatException e) {
            throw new ExtendedPropertiesException(getFormattedProperty("invalidBoolean", "Invalid boolean for configuration key \"{0}\": {1}", key, value, e));
        }
    }


    //****************** Array property getters ******************//

    /**
     * Returns an array of property values.
     * The array will have exactly <code>count</code> items and each item
     * will be filled with value of the property <code>key#</code>, where #
     * is an index of the respective item starting from 1. If {@code count}
     * is negative, the properties <code>key#</code> are tried until the last
     * one is found.
     * @param key the hashtable key (index number will be appended)
     * @param count the number of items to retrieve
     * @return an array of property values
     */
    public String[] getMultiProperty(String key, int count) {
        if (count < 0) {
            List<String> ret = new ArrayList<String>();
            int i = 1;
            String lastProp = getProperty(key + i++);
            while (lastProp != null) {
                ret.add(lastProp);
                lastProp = getProperty(key + i++);
            }
            return ret.toArray(new String[ret.size()]);
        } else {
            String[] ret = new String[count];
            for (int i = 1; i <= count; i++)
                ret[i] = getProperty(key + i);
            return ret;
        }
    }

    /**
     * Returns an array of property values.
     * The array size will be retrieved from property name <code>key</code>.
     * Each item will be filled with value of the property <code>key#</code>, where #
     * is an index of the respective item starting from 1.
     * If the property <code>key</code> does not exist, properties <code>key#</code>
     * are tried until the last one is found.
     * @param key the hashtable key (index number will be appended)
     * @return an array of property values
     * @throws ExtendedPropertiesException if the property's value is not a non-negative integer
     */
    public String[] getMultiProperty(String key) throws ExtendedPropertiesException {
        return getMultiProperty(key, getIntProperty(key, -1, -1, Integer.MAX_VALUE));
    }

    /**
     * Returns an array of property values converted to appropriate types.
     * The conversion is done by {@link Convert#stringToType(java.lang.String, java.lang.Class) stringToType}
     * method.
     * The returned array will have exactly <code>parameterTypes.length</code> items
     * and each item will be filled with value of the property <code>key#</code>, where #
     * is a zero-based index of the respective item.
     *
     * @param key the hashtable key (index number will be appended)
     * @param parameterTypes the parameter types array
     * @return an array of property values
     * @throws ExtendedPropertiesException if the array of values for the property cannot be converted to the specified types
     */
    public Object[] getMultiProperty(String key, Class<?>... parameterTypes) throws ExtendedPropertiesException {
        try {
            return Convert.parseTypesFromString(getMultiProperty(key, parameterTypes.length), parameterTypes, true);
        } catch (InstantiationException e) {
            throw new ExtendedPropertiesException(getFormattedProperty("convertFromString", "Cannot convert value of configuration key \"{0}\" to object: {1}", key, e));
        }
    }

    /**
     * Fill the specified array with property values converted to appropriate types.
     * The conversion is done by {@link Convert#stringToType(java.lang.String, java.lang.Class) stringToType}
     * method. Each item of the array will be filled with value of the property <code>key#</code>, where #
     * is a zero-based index of the respective item. If the property does not exist, <tt>null</tt>
     * value will be placed in the parameters.
     *
     * @param key the hashtable key (index number will be appended)
     * @param parameters the parameter array to fill
     * @param parameterTypes the parameter types array
     * @return the index of the first null value in the array
     * @throws ExtendedPropertiesException if the array of values for the property cannot be converted to the specified types
     */
    public int fillByMultiProperty(String key, Object[] parameters, Class<?>[] parameterTypes) throws ExtendedPropertiesException {
        // Assert parameters
        if (parameters == null || parameterTypes == null || parameters.length != parameterTypes.length)
            throw new ExtendedPropertiesException(getFormattedProperty("illegalArgument", "Parameter count of configuration key \"{0}\" does not match the number of types", key));
        // Get multi-values
        String[] values = getMultiProperty(key, parameters.length);
        try {
            int nullIndex = -1;
            for (int i = parameters.length - 1; i >= 0; i--) {
                if (values[i] == null) {
                    parameters[i] = null;
                    nullIndex = i;
                } else {
                    parameters[i] = Convert.stringToType(values[i], parameterTypes[i]);
                }
            }
            return nullIndex;
        } catch (InstantiationException e) {
            throw new ExtendedPropertiesException(getFormattedProperty("convertFromString", "Cannot convert value of configuration key \"{0}\" to object: {1}", key, e));
        }
    }


    //****************** Class property getters ******************//

    /**
     * Returns a generics-safe class from the given property.
     * @param <E> the superclass of the returned class
     * @param key the hashtable key
     * @param required if <tt>true</tt> the property must exist (and <tt>null</tt> will be never returned)
     * @param checkClass the superclass of the returned class for the generics check
     * @return a generics-safe class from the given property
     * @throws ExtendedPropertiesException if the property was not found or the class with the property's value cannot be resolved
     */
    public <E> Class<E> getClassProperty(String key, boolean required, Class<E> checkClass) throws ExtendedPropertiesException {
        String className = required?getRequiredProperty(key):getProperty(key);
        try {
            return Convert.getClassForName(className, checkClass);
        } catch (ClassNotFoundException e) {
            throw new ExtendedPropertiesException(getFormattedProperty("classNotFound", "Configuration key \"{0}\" specifies invalid class \"{1}\": {2}", key, className, e.getMessage()));
        }
    }

    /**
     * Returns a class from the given property.
     * @param key the hashtable key
     * @param required if <tt>true</tt> the property must exist (and <tt>null</tt> will be never returned)
     * @return a class from the given property
     * @throws ExtendedPropertiesException if the property was not found or the class with the property's value cannot be resolved
     */
    public Class<?> getClassProperty(String key, boolean required) throws ExtendedPropertiesException {
        String className = required?getRequiredProperty(key):getProperty(key);
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new ExtendedPropertiesException(getFormattedProperty("classNotFound", "Configuration key \"{0}\" specifies invalid class \"{1}\": {2}", key, className, e.getMessage()));
        }
    }


    //****************** Factory method property getters ******************//

    /**
     * Returns a factory method for a class.
     * The class name is retrieved from this properties using key <code>classKeyName</code>.
     * The static factory method is then searched with the name from this properties using key
     * <code>methodKeyName</code> and the given prototype.
     * @param returnType the superclass (or class itself) of instances returned by the static factory method
     * @param classKeyName the key looked up in this properties to get the class name
     * @param methodKeyName the key looked up in this properties to get the factory method name
     * @param prototype the factory method's prototype
     * @return a factory method
     * @throws ExtendedPropertiesException if there was an error resolving the class or the factory method
     */
    public Method getFactoryMethod(Class<?> returnType, String classKeyName, String methodKeyName, Class<?>... prototype) throws ExtendedPropertiesException {
        return getFactoryMethod(getClassProperty(classKeyName, true), returnType, getRequiredProperty(methodKeyName), prototype);
    }

    /**
     * Returns a factory method for a class.
     * The class name is retrieved from this properties using key <code>classKeyName</code>.
     * The static factory method is then searched with the name from this properties using key
     * <code>methodKeyName</code> and the given prototype.
     * @param classKeyName the key looked up in this properties to get the class name
     * @param methodKeyName the key looked up in this properties to get the factory method name
     * @param prototype the factory method's prototype
     * @return a factory method
     * @throws ExtendedPropertiesException if there was an error resolving the class or the factory method
     */
    public Method getFactoryMethod(String classKeyName, String methodKeyName, Class<?>... prototype) throws ExtendedPropertiesException {
        return getFactoryMethod(getClassProperty(classKeyName, true), (Class<?>)null, getRequiredProperty(methodKeyName), prototype);
    }

    /**
     * Returns a <code>className</code>'s factory method with the specified name and prototype.
     * The factory method must be static and must return the specified <code>returnType</code>.
     * If the <code>returnType</code> is <tt>null</tt>, the factory method must return <code>factoryClass</code>.
     * @param factoryClass the class the get the factory method for
     * @param returnType the superclass (or class itself) of instances returned by the static factory method
     * @param methodName the factory method name
     * @param prototype the factory method's prototype
     * @return a factory method
     * @throws ExtendedPropertiesException if there was an error resolving the class or the factory method
     */
    public Method getFactoryMethod(Class<?> factoryClass, Class<?> returnType, String methodName, Class<?>... prototype) throws ExtendedPropertiesException {
        try {
            Method method = factoryClass.getMethod(methodName, prototype);
            // Check if method is static
            if (!Modifier.isStatic(method.getModifiers()))
                throw new ExtendedPropertiesException(getFormattedProperty("methodNotStatic", "Method \"{0}\" is not static", method));
            // Check return type
            if (returnType == null)
                returnType = factoryClass;
            if (!returnType.isAssignableFrom(method.getReturnType()))
                throw new ExtendedPropertiesException(getFormattedProperty("methodInvalidRetval", "Method \"{0}\" returns invalid return type {1}", method, method.getReturnType()));
            return method;
        } catch (NoSuchMethodException e) {
            throw new ExtendedPropertiesException(getFormattedProperty("methodNotFound", "Method \"{0}\" was not found in {1}", methodName, factoryClass));
        }
    }


    //****************** Constructor property getters ******************//

    /**
     * Returns a constructor for a class.
     * The class name is retrieved from this properties using key <code>classKeyName</code>.
     * The constructor is then searched with the given prototype.
     * @param <E> the type of the object for which the constructor is retrieved
     * @param classKeyName the key looked up in this properties to get the class name
     * @param checkClass the superclass (or class itself) of instances created by the constructor
     * @param prototype the factory method's prototype
     * @return a constructor
     * @throws ExtendedPropertiesException if there was an error resolving the class or the factory method
     */
    public <E> Constructor<E> getConstructor(Class<E> checkClass, String classKeyName, Class<?>... prototype) throws ExtendedPropertiesException {
        return getConstructor(getClassProperty(classKeyName, true, checkClass), prototype);
    }

    /**
     * Returns a constructor for a class.
     * The class name is retrieved from this properties using key <code>classKeyName</code>.
     * The constructor is then searched with the given prototype.
     * @param classKeyName the key looked up in this properties to get the class name
     * @param prototype the factory method's prototype
     * @return a constructor
     * @throws ExtendedPropertiesException if there was an error resolving the class or the factory method
     */
    public Constructor<?> getConstructor(String classKeyName, Class<?>... prototype) throws ExtendedPropertiesException {
        return getConstructor(getClassProperty(classKeyName, true), prototype);
    }

    /**
     * Returns a constructor for a class.
     * The class name is retrieved from this properties using key <code>classKeyName</code>.
     * The constructor is then searched with the given prototype.
     * @param <E> the type of the object for which the constructor is retrieved
     * @param objectClass the class the get the constructor for
     * @param prototype the factory method's prototype
     * @return a constructor
     * @throws ExtendedPropertiesException if there was an error resolving the class or the factory method
     */
    public <E> Constructor<E> getConstructor(Class<E> objectClass, Class<?>... prototype) throws ExtendedPropertiesException {
        try {
            return objectClass.getConstructor(prototype);
        } catch (NoSuchMethodException e) {
            throw new ExtendedPropertiesException(getFormattedProperty("constructorNotFound", "Constructor with specified parameters was not found in {0}", objectClass));
        }
    }


    //****************** Serialized object property getters ******************//

    /**
     * Reads a serialized object from the file specified by the given property.
     *
     * @param fileNameProperty the name of the property where the file path is stored
     * @param required if <tt>true</tt>, the property is required to exist (otherwise and exception is thrown)
     * @return an object that is read from the serialization
     * @throws ExtendedPropertiesException if the required property does not exist or there was a problem deserializing the object
     */
    public Object getSerializedObject(String fileNameProperty, boolean required) throws ExtendedPropertiesException {
        // Read property with the file name
        String fileName = getProperty(fileNameProperty);
        if (fileName == null) { // Property was not specified
            if (required)
                throw new ExtendedPropertiesException(getFormattedProperty("propertyNotFound", "Property \"{0}\" is required but it was not found", fileNameProperty));
            else
                return null;
        }

        // Open object stream
        try {
            ObjectInputStream inputStream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(fileName)));
            Object ret = inputStream.readObject();
            inputStream.close();
            return ret;
        } catch (IOException e) {
            throw new ExtendedPropertiesException("Cannot read serialized object from " + fileName, e);
        } catch (ClassNotFoundException e) {
            throw new ExtendedPropertiesException("Cannot read serialized object from " + fileName, e);
        }
    }


    //****************** Network property getters ******************//

    /**
     * Returns an inet address from the given property.
     * <p> 
     * The property value can either be a machine name or a textual
     * representation of its IP address. If a literal IP address is
     * supplied, only the validity of the address format is checked.
     * </p>
     * 
     * @param key the hashtable key
     * @param required if <tt>true</tt> the property must exist (and <tt>null</tt> will be never returned)
     * @return an inet address from the given property
     * @throws ExtendedPropertiesException if the property was not found or the class with the property's value cannot be resolved
     */
    public InetAddress getInetAddressProperty(String key, boolean required) throws ExtendedPropertiesException {
        String inetAddress = required?getRequiredProperty(key):getProperty(key);
        try {
            return InetAddress.getByName(inetAddress);
        } catch (UnknownHostException e) {
            throw new ExtendedPropertiesException(getFormattedProperty("unknownHost", "Configuration key \"{0}\" specifies unknown host \"{1}\": {2}", key, inetAddress, e.getMessage()));
        }
    }


    //****************** Database propery getters ******************//

    /**
     * Returns a data source using either JNDI or {@link DriverManager} using the
     * URL specified in the value of the property <code>key</code>. If the URL
     * starts with "java:", JNDI is looked up, otherwise the driver manager is used
     * (the driver for the url must be registered in advance).
     * @param key the hashtable key
     * @return an established database connection
     * @throws ExtendedPropertiesException if the specified key is not found
     *      in the configuration or there is no JNDI data source with the given URL
     * @throws SQLException if the database connection cannot be established
     */
    public Connection getDatabaseConnection(String key) throws ExtendedPropertiesException, SQLException {
        String url = getRequiredProperty(key);
        try {
            if (url.startsWith("java:"))
                return ((DataSource)new InitialContext().lookup(url)).getConnection();
            else
                return DriverManager.getConnection(url);
        } catch (NamingException e) {
            throw new ExtendedPropertiesException(getFormattedProperty("resolveDataSource", "Cannot find data source for configuration key \"{0}\": {1}", key, e.getExplanation()));
        } catch (ClassCastException e) {
            throw new ExtendedPropertiesException(getFormattedProperty("invalidDataSource", "There is a named object for configuration key \"{0}\", but it is not a data source", key, e));
        }
    }

    /**
     * Prepare an SQL statement from the configuration. It is prepared on the
     * database connection that is resolved using {@link #getDatabaseConnection}.
     * @param connectionKey the hashtable key for the connection URL
     * @param sqlStatementKey the hashtable key for the SQL statement
     * @return a prepared SQL statement
     * @throws ExtendedPropertiesException if the specified keys are not found
     *      or there is no valid database connection for the <code>connectionKey</code>
     * @throws SQLException if there was an error preparing the SQL statement
     */
    public PreparedStatement getSQLStatement(String connectionKey, String sqlStatementKey) throws ExtendedPropertiesException, SQLException {
        return getSQLStatement(getDatabaseConnection(connectionKey), sqlStatementKey);
    }

    /**
     * Prepare an SQL statement from the configuration. It is prepared on the
     * specified database connection.
     * @param connection the database connection for which to prepare SQL statement
     * @param key the hashtable key for the SQL statement
     * @return a prepared SQL statement
     * @throws ExtendedPropertiesException if the specified key is not found
     * @throws SQLException if the database connection cannot be established
     */
    public PreparedStatement getSQLStatement(Connection connection, String key) throws SQLException {
        return connection.prepareStatement(getRequiredProperty(key));
    }

}
