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
import java.util.Arrays;
import java.util.Map;
import messif.utility.Convert;

/**
 * This class allows to create instances of a given class.
 * A constructor with the given prototype is encapsulated and used in subsequent calls.
 * The instantiator can store a predefined values for the constructor arguments
 * that are used automatically whenever a <tt>null</tt> is passed in any argument to the
 * {@link #instantiate(java.lang.Object[]) instantiate} method.
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
public class ConstructorInstantiatorWithArguments<T> extends ConstructorInstantiator<T> {
    //****************** Attributes ******************//

    /** Arguments for the constructor */
    private final Object[] arguments;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of ConstructorInstantiatorWithArguments for creating instances of
     * T using the specified constructor and with the given stored arguments.
     * @param constructor the constructor using which the instances will be created
     * @param arguments the stored arguments for the constructor
     * @throws NoSuchInstantiatorException if the provided class does not have a proper constructor
     */
    public ConstructorInstantiatorWithArguments(Constructor<? extends T> constructor, Object... arguments) throws NoSuchInstantiatorException {
        super(constructor);
        if (!Instantiators.isPrototypeMatching(constructor.getParameterTypes(), arguments, false, null))
            throw new NoSuchInstantiatorException("Given arguments " + Arrays.toString(arguments) + " are not compatible with " + constructor);
        this.arguments = arguments.clone();
    }

    /**
     * Creates a new instance of ConstructorInstantiatorWithArguments for creating instances of
     * {@code objectClass} that accepts the given arguments.
     *
     * @param objectClass the class the instances of which will be created
     * @param arguments the arguments for the constructor
     * @throws NoSuchInstantiatorException if the provided class does not have a proper constructor
     */
    public ConstructorInstantiatorWithArguments(Class<? extends T> objectClass, Object... arguments) throws NoSuchInstantiatorException {
        super(objectClass, arguments);
        this.arguments = arguments.clone();
    }

    /**
     * Creates a new instance of ConstructorInstantiatorWithArguments for creating instances of
     * {@code objectClass} that accepts the given arguments.
     *
     * @param objectClass the class the instances of which will be created
     * @param convertStringArguments if <tt>true</tt> the string values from the arguments are converted using {@link messif.utility.Convert#stringToType}
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @param arguments the arguments for the constructor
     * @throws NoSuchInstantiatorException if the provided class does not have a proper constructor
     */
    private ConstructorInstantiatorWithArguments(Class<? extends T> objectClass, boolean convertStringArguments, Map<String, Object> namedInstances, Object[] arguments) throws NoSuchInstantiatorException {
        super(objectClass, convertStringArguments, namedInstances, arguments);
        this.arguments = arguments;
    }

    /**
     * Creates a new instance of ConstructorInstantiatorWithArguments for creating instances of
     * {@code objectClass} that accepts the given string arguments.
     * Note that string arguments will be converted to the proper types using {@link Convert#stringToType}.
     *
     * @param objectClass the class the instances of which will be created
     * @param arguments the string arguments for the constructor (will be converted to proper types)
     * @param offset the index of the first argument to use from the {@code arguments} array
     * @param length the number of arguments that will be used from the {@code arguments} array (starting from offset)
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @throws NoSuchInstantiatorException if the provided class does not have a proper constructor
     */
    public ConstructorInstantiatorWithArguments(Class<? extends T> objectClass, String[] arguments, int offset, int length, Map<String, Object> namedInstances) throws NoSuchInstantiatorException {
        this(objectClass, Convert.copyGenericArray(arguments, offset, length, Object.class), namedInstances);
    }

    /**
     * {@inheritDoc}
     * For every given argument that is <tt>null</tt>, the {@link #arguments stored value} is used.
     */
    @Override
    public T instantiate(Object... arguments) throws IllegalArgumentException, InvocationTargetException {
        if (this.arguments == null)
            return super.instantiate(arguments);
        if (arguments == null)
            return super.instantiate(this.arguments);
        
        Object[] argumentsMerged = this.arguments.clone();
        for (int i = 0; i <= argumentsMerged.length; i++)
            if (i < arguments.length && arguments[i] != null)
                argumentsMerged[i] = arguments[i];
        return super.instantiate(argumentsMerged);
    }

    /**
     * Creates a new instance using the encapsulated constructor with the stored arguments.
     * @return the new instance
     * @throws InvocationTargetException if there was an exception thrown when the constructor was invoked
     */
    public T instantiate() throws InvocationTargetException {
        return super.instantiate(this.arguments);
    }
}
