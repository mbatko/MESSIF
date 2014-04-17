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

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import messif.utility.Convert;

/**
 * Parse a given string signature and provide methods for creating
 * {@link Instantiator}s that match the signature or instances directly.
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class InstantiatorSignature {
    /** List of arguments parsed from the signature, i.e. coma separated values in parenthesis */
    private final String[] args;
    /** Parsed class of the constructor/method/field */
    private final Class<?> objectClass;
    /** Parsed named instance of the method/field */
    private final Object instance;
    /** Parsed name of the constructor/method/field */
    private final String name;

    /**
     * Creates a new instance of ParsedSignature.
     * The internal information about arguments and names are initialized.
     * @param signature a fully specified constructor/method/field signature
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     */
    public InstantiatorSignature(String signature, Map<String, Object> namedInstances) {
        // Find left parenthesis
        int leftParenthesis = signature.indexOf('(');
        Class<?> constructorClass = null;

        // Parse arguments and constructor class is appropriate
        if (leftParenthesis == -1) {
            this.args = null;
        } else {
            // Check right parenthesis (must be last character)
            if (signature.charAt(signature.length() - 1) != ')')
                throw new IllegalArgumentException("Missing or invalid closing parenthesis: " + signature);
            // Parse arguments
            if (leftParenthesis == signature.length() - 2)
                this.args = new String[0];
            else
                this.args = signature.substring(leftParenthesis + 1, signature.length() - 1).split("\\s*,\\s*", -1);
            // Remove arguments from the signature (already parsed)
            signature = signature.substring(0, leftParenthesis);
            try {
                // Parse signature as constructor
                constructorClass = Class.forName(signature);
            } catch (ClassNotFoundException ignore) {
            }
        }

        // Parse class and name
        if (constructorClass == null) {
            // Try method or field (i.e. last dot position denotes method/field name
            int dotPos = signature.lastIndexOf('.');
            if (dotPos == -1) {
                this.instance = Convert.expandReferencedInstances(namedInstances.get(signature));
                if (this.instance == null)
                    throw new IllegalArgumentException("Class not found: " + signature);
                this.objectClass = null;
                this.name = null;
            } else {
                this.instance = namedInstances == null ? null : Convert.expandReferencedInstances(namedInstances.get(signature.substring(0, dotPos)));
                if (this.instance == null) {
                    try {
                        this.objectClass = Class.forName(signature.substring(0, dotPos));
                    } catch (ClassNotFoundException ignore) {
                        throw new IllegalArgumentException("Class not found: " + signature.substring(0, dotPos));
                    }
                } else {
                    this.objectClass = null;
                }
                this.name = signature.substring(dotPos + 1);
            }
        } else {
            // We have parsed constructor class
            this.objectClass = constructorClass;
            this.instance = null;
            this.name = null;
        }
    }

    /**
     * Return arguments parsed by this signature.
     * If a field signature was parsed, <tt>null</tt> is returned.
     * @return arguments parsed by this signature
     */
    public String[] getParsedArgs() {
        return args;
    }

    /**
     * Returns the parsed declaring class of the constructor/method/field.
     * @return the parsed declaring class
     */
    public Class<?> getParsedClass() {
        return objectClass;
    }

    /**
     * Returns the parsed declaring class of the constructor/method/field.
     * Generic-safe typecast is performed using the given {@code checkClass}.
     *
     * @param <T> the super class of the declaring class agains which to check
     * @param checkClass the super class of the declaring class agains which to check
     * @return the parsed declaring class
     */
    @SuppressWarnings("unchecked")
    public <T> Class<T> getParsedClass(Class<? extends T> checkClass) {
        if (checkClass.isAssignableFrom(objectClass))
            return (Class<T>)objectClass; // This cast IS checked on the previous line
        else
            throw new IllegalArgumentException("Cannot cast " + objectClass + " to " + checkClass);
    }

    /**
     * Returns the parsed name of the method/field.
     * If a constructor signature was parsed, <tt>null</tt> is returned.
     * @return the parsed name
     */
    public String getParsedName() {
        return name;
    }

    /**
     * Returns <tt>true</tt> if a constructor signature was parsed.
     * @return <tt>true</tt> if a constructor signature was parsed
     */
    public boolean isConstructorSignature() {
        return args != null && name == null;
    }

    /**
     * Returns <tt>true</tt> if a method signature was parsed.
     * @return <tt>true</tt> if a method signature was parsed
     */
    public boolean isMethodSignature() {
        return args != null && name != null;
    }

    /**
     * Returns <tt>true</tt> if a field signature was parsed.
     * @return <tt>true</tt> if a field signature was parsed
     */
    public boolean isFieldSignature() {
        return args == null;
    }

    /**
     * Creates an instance for the parsed signature.
     * @param <T> the class of the instance that will be created
     * @param checkClass the class of the instance that will be created
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @return a new instance for the parsed signature
     * @throws NoSuchInstantiatorException if the instantiator cannot be created
     * @throws InvocationTargetException if there was an exception during instantiation
     */
    public <T> T create(Class<? extends T> checkClass, Map<String, Object> namedInstances) throws NoSuchInstantiatorException, InvocationTargetException {
        if (isFieldSignature()) {
            if (instance == null)
                return new FieldInstantiator<T>(checkClass, objectClass, name).instantiate();
            else if (name == null)
                return checkClass.cast(instance);
            else
                return new FieldInstantiator<T>(checkClass, instance, name).instantiate();
        } else {
            Object[] objArgs = new Object[args.length];
            System.arraycopy(args, 0, objArgs, 0, args.length);
            if (isMethodSignature()) {
                if (instance == null)
                    return new MethodInstantiator<T>(checkClass, objectClass, name, true, namedInstances, objArgs).instantiate(objArgs);
                else
                    return new MethodInstantiator<T>(checkClass, instance, name, true, namedInstances, objArgs).instantiate(objArgs);
            } else {
                return new ConstructorInstantiator<T>(getParsedClass(checkClass), true, namedInstances, objArgs).instantiate(objArgs);
            }
        }
    }

    /**
     * Creates instantiator for the parsed signature.
     * @param <T> the class of instances that will be created by the returned instantiator
     * @param checkClass the class of instances that will be created by the returned instantiator
     * @return a new instantiator for the parsed signature
     * @throws NoSuchInstantiatorException if the instantiator cannot be created
     */
    public <T> Instantiator<T> createInstantiator(Class<? extends T> checkClass) throws NoSuchInstantiatorException {
        if (isFieldSignature()) {
            if (instance == null)
                return new FieldInstantiator<T>(checkClass, objectClass, name);
            else
                return new FieldInstantiator<T>(checkClass, instance, name);
        } else if (isMethodSignature()) {
            if (instance == null)
                return new MethodInstantiator<T>(checkClass, objectClass, name, args.length);
            else
                return new MethodInstantiator<T>(checkClass, instance, name, args.length);
        } else {
            return new ConstructorInstantiator<T>(getParsedClass(checkClass), args.length);
        }
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
     * Note that only types convertible by {@link Convert#stringToType} method can be used in arguments.
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
     * @throws NoSuchInstantiatorException if the class in the constructor signature was not found or is not a descendant of checkClass
     */
    public static <E> E createInstanceWithStringArgs(String signature, Class<E> checkClass, Map<String, Object> namedInstances) throws InvocationTargetException, NoSuchInstantiatorException {
        return new InstantiatorSignature(signature, namedInstances).create(checkClass, namedInstances);
    }
}
