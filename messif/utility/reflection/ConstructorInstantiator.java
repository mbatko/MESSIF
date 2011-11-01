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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Map;
import messif.utility.Convert;

/**
 * This class allows to create instances of a given class.
 * A constructor with the given prototype is encapsulated and used in subsequent calls.
 *
 * <p>
 * This class provides a convenient way of repeatable creation of instances
 * of a given class without the need of repeatable constructor retrieval and checking all
 * the exceptions.
 * </p>
 *
 * @param <T> the class the instances of which will be created by this ConstructorInstantiator
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ConstructorInstantiator<T> implements Instantiator<T> {
    //****************** Attributes ******************//

    /** Constructor objects of type T needed for instantiating objects */
    private final Constructor<? extends T> constructor;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of ConstructorInstantiator for creating instances of
     * {@code objectClass} via the specified constructor.
     *
     * @param constructor the constructor using which the instances will be created
     * @throws NoSuchInstantiatorException if the provided class does not have a proper constructor
     */
    public ConstructorInstantiator(Constructor<? extends T> constructor) throws NoSuchInstantiatorException {
        if (Modifier.isAbstract(constructor.getDeclaringClass().getModifiers()))
            throw new NoSuchInstantiatorException("Cannot create abstract " + constructor.getDeclaringClass());
        this.constructor = constructor;
    }

    /**
     * Creates a new instance of ConstructorInstantiator for creating instances of
     * {@code objectClass} that accepts parameters of the given prototype.
     *
     * @param objectClass the class the instances of which will be created
     * @param prototype the types of constructor arguments
     * @throws NoSuchInstantiatorException if the provided class does not have a proper constructor
     */
    public ConstructorInstantiator(Class<? extends T> objectClass, Class<?>... prototype) throws NoSuchInstantiatorException {
        this(getConstructor(objectClass, true, prototype));
    }

    /**
     * Creates a new instance of ConstructorInstantiator for creating instances of
     * {@code objectClass} that accepts the given number of arguments.
     * Note that if there are several constructors with the same number of arguments,
     * one is selected.
     *
     * @param objectClass the class the instances of which will be created
     * @param argumentCount the number of arguments that the constructor should have
     * @throws NoSuchInstantiatorException if the provided class does not have a proper constructor
     */
    public ConstructorInstantiator(Class<? extends T> objectClass, int argumentCount) throws NoSuchInstantiatorException {
        this(getConstructor(objectClass, true, argumentCount));
    }

    /**
     * Creates a new instance of ConstructorInstantiator for creating instances of
     * {@code objectClass} that accepts the given arguments.
     *
     * @param objectClass the class the instances of which will be created
     * @param convertStringArguments if <tt>true</tt> the string values from the arguments are converted using {@link messif.utility.Convert#stringToType}
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @param arguments the arguments for the constructor
     * @throws NoSuchInstantiatorException if the provided class does not have a proper constructor
     */
    public ConstructorInstantiator(Class<? extends T> objectClass, boolean convertStringArguments, Map<String, Object> namedInstances, Object[] arguments) throws NoSuchInstantiatorException {
        this(getConstructor(objectClass, convertStringArguments, true, namedInstances, arguments));
    }

    /**
     * Creates a new instance of ConstructorInstantiator for creating instances of
     * {@code objectClass} that accepts the given arguments.
     *
     * @param objectClass the class the instances of which will be created
     * @param arguments the arguments for the constructor
     * @throws NoSuchInstantiatorException if the provided class does not have a proper constructor
     */
    public ConstructorInstantiator(Class<? extends T> objectClass, Object... arguments) throws NoSuchInstantiatorException {
        this(objectClass, false, null, arguments);
    }

    /**
     * Retrieves a public constructor with the given prototype from the given class.
     * @param <T> the class in which to search for the constructor
     * @param constructorClass the class in which to search for the constructor
     * @param publicOnlyConstructors flag whether to search in all declared constructors (<tt>false</tt>) or only in public constructors (<tt>true</tt>)
     * @param prototype the constructor prototype
     * @return the constructor found
     * @throws NoSuchInstantiatorException if the there is no constructor for the given prototype
     */
    public static <T> Constructor<T> getConstructor(Class<T> constructorClass, boolean publicOnlyConstructors, Class<?>... prototype) throws NoSuchInstantiatorException {
        try {
            return publicOnlyConstructors ? constructorClass.getConstructor(prototype) : constructorClass.getDeclaredConstructor(prototype);
        } catch (NoSuchMethodException ignore) {
            throw new NoSuchInstantiatorException(constructorClass, null, prototype);
        }
    }

    /**
     * Retrieves a public constructor with the given number of arguments from the given class.
     * @param <T> the class in which to search for the constructor
     * @param constructors the list of constructors to search
     * @param argumentCount the number of arguments that the method should have
     * @return the constructor found
     * @throws NoSuchInstantiatorException if the there is no constructor for the given prototype
     */
    public static <T> Constructor<T> getConstructor(Constructor<T>[] constructors, int argumentCount) throws NoSuchInstantiatorException {
        if (constructors.length == 0)
            throw new NoSuchInstantiatorException("There are no constructors available");
        for (Constructor<T> constructor : constructors)
            if (constructor.getParameterTypes().length == argumentCount)
                return constructor;
        throw new NoSuchInstantiatorException(constructors[0].getDeclaringClass(), null, argumentCount);
    }

    /**
     * Retrieves a public constructor with the given number of arguments from the given class.
     * @param <T> the class in which to search for the constructor
     * @param constructorClass the class in which to search for the constructor
     * @param publicOnlyConstructors flag whether to search in all declared constructors (<tt>false</tt>) or only in public constructors (<tt>true</tt>)
     * @param argumentCount the number of arguments that the method should have
     * @return the constructor found
     * @throws NoSuchInstantiatorException if the there is no constructor for the given prototype
     */
    public static <T> Constructor<T> getConstructor(Class<T> constructorClass, boolean publicOnlyConstructors, int argumentCount) throws NoSuchInstantiatorException {
        return getConstructor(Convert.getConstructors(constructorClass, publicOnlyConstructors), argumentCount);
    }

    /**
     * Returns a constructor for the specified class that accepts the specified arguments.
     * The <code>constructorClass</code>'s declared constructors are searched for the one that
     * accepts the arguments.
     * If the <code>convertStringArguments</code> is specified, the
     * <code>arguments</code> elements are replaced with the converted types
     * if and only if a proper constructor is found. Their types then will be
     * compatible with the constructor.
     *
     * @param <T> the class in which to search for the constructor
     * @param constructors the list of constructors to search
     * @param convertStringArguments if <tt>true</tt> the string values from the arguments are converted using {@link Convert#stringToType}
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @param arguments the arguments for the constructor
     * @return a constructor for the specified class
     * @throws NoSuchInstantiatorException if there was no constructor for the specified list of arguments
     */
    public static <T> Constructor<T> getConstructor(Constructor<T>[] constructors, boolean convertStringArguments, Map<String, Object> namedInstances, Object[] arguments) throws NoSuchInstantiatorException {
        if (constructors.length == 0)
            throw new NoSuchInstantiatorException("There are no constructors available");
        for (Constructor<T> constructor : constructors) {
            if (Instantiators.isPrototypeMatching(constructor.getParameterTypes(), arguments, convertStringArguments, namedInstances))
                return constructor;
        }

        throw new NoSuchInstantiatorException(constructors[0].getDeclaringClass(), null, convertStringArguments, arguments);
    }

    /**
     * Returns a constructor for the specified class that accepts the specified arguments.
     * The <code>constructorClass</code>'s declared constructors are searched for the one that
     * accepts the arguments.
     * If the <code>convertStringArguments</code> is specified, the
     * <code>arguments</code> elements are replaced with the converted types
     * if and only if a proper constructor is found. Their types then will be
     * compatible with the constructor.
     *
     * @param <T> the class in which to search for the constructor
     * @param constructorClass the class for which to get the constructor
     * @param convertStringArguments if <tt>true</tt> the string values from the arguments are converted using {@link Convert#stringToType}
     * @param publicOnlyConstructors flag whether to search in all declared constructors (<tt>false</tt>) or only in public constructors (<tt>true</tt>)
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @param arguments the arguments for the constructor
     * @return a constructor for the specified class
     * @throws NoSuchInstantiatorException if there was no constructor for the specified list of arguments
     */
    public static <T> Constructor<T> getConstructor(Class<T> constructorClass, boolean convertStringArguments, boolean publicOnlyConstructors, Map<String, Object> namedInstances, Object[] arguments) throws NoSuchInstantiatorException {
        return getConstructor(Convert.getConstructors(constructorClass, publicOnlyConstructors), convertStringArguments, namedInstances, arguments);
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
     * Note also that only types convertible by {@link Convert#stringToType} method can be used in constructors.
     * </p>
     *
     * @param <T> the type of the instantiated object
     * @param constructors the list of constructors of the desired class to try
     * @param arguments the string arguments for the constructor that will be converted to correct types
     * @param argStartIndex index in the string arguments array from which to expect arguments (all the previous items are ignored)
     * @param argEndIndex index in the string arguments array to which to expect arguments (all the following items are ignored)
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @return a new instance of the class the constructors were specified for
     * @throws NoSuchInstantiatorException if the constructor can't be found for the specified arguments or the argument string-to-type convertion has failed
     * @throws InvocationTargetException if there was an exception when calling the constructor
     */
    public static <T> T createInstanceWithStringArgs(Constructor<T>[] constructors, Object[] arguments, int argStartIndex, int argEndIndex, Map<String, Object> namedInstances) throws NoSuchInstantiatorException, InvocationTargetException {
        Object[] args = new Object[argEndIndex - argStartIndex + 1];
        System.arraycopy(arguments, argStartIndex, args, 0, args.length);
        Constructor<? extends T> constructor = getConstructor(constructors, true, namedInstances, args);
        try {
            return constructor.newInstance(args);
        } catch (InstantiationException e) {
            throw new NoSuchInstantiatorException("Cannot create abstract class using " + constructor);
        } catch (IllegalAccessException e) {
            throw new NoSuchInstantiatorException("Cannot access " + constructor);
        } catch (IllegalArgumentException e) {
            throw new InternalError("String arguments should be converted but they are not");
        }
    }

    /**
     * Creates a new instance of a class using string arguments for its constructor.
     * All the public constructors of the given {@code clazz} are tried one by one and
     * if the {@code arguments} are convertible to the arguments of that constructor,
     * a new instance is created.
     * <p>
     * Note that only constructors with the number of arguments equal to <code>argEndIndex - argStartIndex + 1</code>
     * are tried. If there are several constructors with the same number of arguments,
     * the first constructor that succeeds in converting string arguments will be used.
     * </p>
     * <p>
     * Note also that only types convertible by {@link Convert#stringToType} method can be used in constructors.
     * </p>
     *
     * @param <T> the type of the instantiated object
     * @param clazz the class of the instantiated object
     * @param arguments the string arguments for the constructor that will be converted to correct types
     * @param argStartIndex index in the string arguments array from which to expect arguments (all the previous items are ignored)
     * @param argEndIndex index in the string arguments array to which to expect arguments (all the following items are ignored)
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @return a new instance of the class the constructors were specified for
     * @throws NoSuchInstantiatorException if the constructor can't be found for the specified arguments or the argument string-to-type convertion has failed
     * @throws InvocationTargetException if there was an exception when calling the constructor
     */
    public static <T> T createInstanceWithStringArgs(Class<? extends T> clazz, Object[] arguments, int argStartIndex, int argEndIndex, Map<String, Object> namedInstances) throws NoSuchInstantiatorException, InvocationTargetException {
        return createInstanceWithStringArgs(Convert.getConstructors(clazz, true), arguments, argStartIndex, argEndIndex, namedInstances);
    }


    //****************** Instantiation support ******************//

    /**
     * Creates a new instance using the encapsulated constructor.
     * The arguments must be compatible with the prototype that was given while
     * {@link #ConstructorInstantiator(java.lang.Class, java.lang.Class[]) creating} this
     * {@link ConstructorInstantiator} class.
     * @param arguments the arguments for the encapsulated constructor
     * @return the new instance
     * @throws IllegalArgumentException if the arguments are not compatible with the constructor prototype
     * @throws InvocationTargetException if there was an exception thrown when the constructor was invoked
     */
    @Override
    public T instantiate(Object... arguments) throws IllegalArgumentException, InvocationTargetException {
        try {
            return constructor.newInstance(arguments);
        } catch (InstantiationException e) {
            throw new InternalError("Cannot call " + constructor + ": " + e); // This should never happen - the class is not abstract
        } catch (IllegalAccessException e) {
            throw new InternalError("Cannot call " + constructor + ": " + e); // This should never happen - the constructor is public
        }
    }

    @Override
    public Class<?>[] getInstantiatorPrototype() {
        return constructor.getParameterTypes();
    }

    @Override
    public Class<? extends T> getInstantiatorClass() {
        return constructor.getDeclaringClass();
    }

    @Override
    public String toString() {
        return constructor.toString();
    }

}
