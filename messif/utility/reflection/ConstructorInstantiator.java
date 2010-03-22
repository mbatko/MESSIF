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

/**
 * This class allows to create instances of a given class.
 * A constructor with the given prototype is encapsulated and used in subsequent calls.
 *
 * <p>
 * This class provides a convenient way of repeatable creation of instances
 * of a given class without the need of repetable constructor retrieval and checking all
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
     * @throws IllegalArgumentException if the provided class does not have a proper constructor
     */
    public ConstructorInstantiator(Constructor<? extends T> constructor) throws IllegalArgumentException {
        if (Modifier.isAbstract(constructor.getDeclaringClass().getModifiers()))
            throw new IllegalArgumentException("Cannot create abstract " + constructor.getDeclaringClass());
        this.constructor = constructor;
    }

    /**
     * Creates a new instance of ConstructorInstantiator for creating instances of
     * {@code objectClass} that accepts parameters of the given prototype.
     *
     * @param objectClass the class the instances of which will be created
     * @param prototype the types of constructor arguments
     * @throws IllegalArgumentException if the provided class does not have a proper constructor
     */
    public ConstructorInstantiator(Class<? extends T> objectClass, Class<?>... prototype) throws IllegalArgumentException {
        this(getConstructor(objectClass, prototype));
    }

    /**
     * Creates a new instance of ConstructorInstantiator for creating instances of
     * {@code objectClass} that accepts the given number of arguments.
     * Note that if there are several constructors with the same number of arguments,
     * one is selected.
     *
     * @param objectClass the class the instances of which will be created
     * @param argumentCount the number of arguments that the constructor should have
     * @throws IllegalArgumentException if the provided class does not have a proper constructor
     */
    public ConstructorInstantiator(Class<? extends T> objectClass, int argumentCount) throws IllegalArgumentException {
        this(getConstructor(objectClass, argumentCount));
    }

    /**
     * Creates a new instance of ConstructorInstantiator for creating instances of
     * {@code objectClass} that accepts the given arguments.
     *
     * @param objectClass the class the instances of which will be created
     * @param arguments the arguments for the constructor
     * @throws IllegalArgumentException if the provided class does not have a proper constructor
     */
    public ConstructorInstantiator(Class<? extends T> objectClass, Object... arguments) throws IllegalArgumentException {
        this(getConstructor(objectClass, arguments));
    }

    /**
     * Retrieves a public constructor with the given prototype from the given class.
     * @param <T> the class in which to search for the constructor
     * @param constructorClass the class in which to search for the constructor
     * @param prototype the constructor prototype
     * @return the constructor found
     * @throws IllegalArgumentException if the there is no constructor for the given prototype
     */
    private static <T> Constructor<T> getConstructor(Class<T> constructorClass, Class<?>... prototype) throws IllegalArgumentException {
        try {
            return constructorClass.getConstructor(prototype);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("There is no constructor " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a public constructor with the given number of arguments from the given class.
     * @param <T> the class in which to search for the constructor
     * @param constructorClass the class in which to search for the constructor
     * @param argumentCount the number of arguments that the method should have
     * @return the constructor found
     * @throws IllegalArgumentException if the there is no constructor for the given prototype
     */
    private static <T> Constructor<T> getConstructor(Class<T> constructorClass, int argumentCount) throws IllegalArgumentException {
        @SuppressWarnings("unchecked")
        Constructor<T>[] constructors = (Constructor<T>[])constructorClass.getConstructors();
        for (int i = 0; i < constructors.length; i++)
            if (constructors[i].getParameterTypes().length == argumentCount)
                return constructors[i];
        throw new IllegalArgumentException("There is no constructor " + constructorClass.getName() + "(...) with " + argumentCount + " arguments");
    }

    /**
     * Retrieves a public constructor for the specified class that accepts the specified arguments.
     * The <code>constructorClass</code>'s public constructors are searched for the one that
     * accepts the arguments.
     *
     * @param <T> the class the constructor will create
     * @param constructorClass the class for which to get the constructor
     * @param arguments the arguments for the constructor
     * @return a constructor for the specified class
     * @throws IllegalArgumentException if there was no constructor for the specified list of arguments
     */
    private static <T> Constructor<T> getConstructor(Class<T> constructorClass, Object[] arguments) throws IllegalArgumentException {
        @SuppressWarnings("unchecked")
        Constructor<T>[] constructors = (Constructor<T>[])constructorClass.getConstructors();
        for (int i = 0; i < constructors.length; i++) {
            if (Instantiators.isPrototypeMatching(constructors[i].getParameterTypes(), arguments, false, null))
                return constructors[i];
        }
        throw new IllegalArgumentException("There is no constructor " + constructorClass.getName() + "(...) with " + arguments.length + " arguments");
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
    public T instantiate(Object... arguments) throws IllegalArgumentException, InvocationTargetException {
        try {
            return constructor.newInstance(arguments);
        } catch (InstantiationException e) {
            throw new InternalError("Cannot call " + constructor + ": " + e); // This should never happen - the class is not abstract
        } catch (IllegalAccessException e) {
            throw new InternalError("Cannot call " + constructor + ": " + e); // This should never happen - the constructor is public
        }
    }

    public Class<?>[] getInstantiatorPrototype() {
        return constructor.getParameterTypes();
    }

    public Class<? extends T> getInstantiatorClass() {
        return constructor.getDeclaringClass();
    }

    @Override
    public String toString() {
        return constructor.toString();
    }

}
