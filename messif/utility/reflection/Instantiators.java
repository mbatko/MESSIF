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
package messif.utility.reflection;

import java.lang.reflect.Method;
import java.util.Map;
import messif.utility.Convert;

/**
 * Collection of utility methods for {@link Instantiator}s.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class Instantiators {

    /**
     * Retrieves a getter method for a given property on a given bean class.
     *
     * @param clazz the bean class to use
     * @param propertyName the name of the property
     * @return a getter method
     * @throws IllegalArgumentException if there was no public getter method for the given property name
     */
    public static Method getPropertyGetterMethod(Class<?> clazz, String propertyName) throws IllegalArgumentException {
        Method method;
        if (propertyName != null && !propertyName.isEmpty()) {
            String baseName = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
            try {
                method = clazz.getMethod("is" + baseName);
            } catch (NoSuchMethodException e) {
                try {
                    method = clazz.getMethod("get" + baseName);
                } catch (NoSuchMethodException ex) {
                    method = null;
                }
            }
        } else {
            method = null;
        }

        if (method == null || method.getReturnType() == Void.TYPE)
            throw new IllegalArgumentException("Cannot get a getter method for property '" + propertyName + "' on " + clazz);
        return method;
    }

    /**
     * Retrieves a setter method for a given property on a given bean class.
     *
     * @param clazz the bean class to use
     * @param propertyName the name of the property
     * @param propertyClass the class of the property
     * @return a setter method
     * @throws IllegalArgumentException if there was no public getter method for the given property name
     */
    public static Method getPropertySetterMethod(Class<?> clazz, String propertyName, Class<?> propertyClass) throws IllegalArgumentException {
        if (propertyName == null || propertyName.isEmpty())
            throw new IllegalArgumentException("Cannot get a setter method for property '" + propertyName + "' on " + clazz);
        try {
            return clazz.getMethod("set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1), propertyClass);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Cannot get a getter method for property '" + propertyName + "' on " + clazz);
        }
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
     * @param convertStringArguments if <tt>true</tt> the string values from the arguments are converted using {@link Convert#stringToType}
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @return <tt>true</tt> if the arguments are compatible with the prototype
     */
    public static boolean isPrototypeMatching(Class<?>[] prototype, Object[] arguments, boolean convertStringArguments, Map<String, Object> namedInstances) {
        // Not enough arguments
        if (prototype.length != arguments.length)
            return false;

        // Array for the converted values (since they must not be modified if the method return false)
        Object[] convertedArguments = null;

        // Test arguments
        for (int i = 0; i < prototype.length; i++) {
            // Null value is accepted by any class
            if (arguments[i] == null && !prototype[i].isPrimitive())
                continue;
            // If string conversion is enabled and the argument is string
            if (convertStringArguments && (arguments[i] instanceof String)) {
                // Try to convert string argument
                try {
                    // Clone argument array if not clonned yet
                    if (convertedArguments == null)
                        convertedArguments = arguments.clone();
                    convertedArguments[i] = Convert.stringToType((String)arguments[i], prototype[i], namedInstances);
                } catch (InstantiationException ignore) {
                    return false;
                }
            } else if (!Convert.wrapPrimitiveType(prototype[i]).isInstance(arguments[i])) {
                // The argument of the method must be the same as or a superclass of the provided prototype class
                return false;
            }
        }

        // Move converted arguments
        if (convertedArguments != null)
            System.arraycopy(convertedArguments, 0, arguments, 0, arguments.length);

        return true;
    }
}
