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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import messif.utility.Convert;

/**
 * This class allows to create instances of a given class.
 * A factory method with the given prototype is encapsulated and used in subsequent calls.
 * Note that a factory method must be static and must return the object of the given class.
 *
 * <p>
 * This class provides a convenient way of repeatable creation of instances
 * of a given class without the need of repetable factory method retrieval and checking all
 * the exceptions.
 * </p>
 *
 * @param <T> the class the instances of which will be created by this MethodInstantiator
 */
public class MethodInstantiator<T> implements Instantiator<T> {
    //****************** Attributes ******************//

    /** Method that returns T needed for instantiating objects */
    private final Method method;
    /** Instance on which the method is called or <tt>null</tt> if the method is static */
    private final Object callInstance;
    /** Class created by this instantiator */
    private final Class<? extends T> objectClass;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of MethodInstantiator for creating instances of
     * {@code objectClass} via the given method.
     *
     * @param objectClass the class the instances of which will be created
     * @param method the factory method used to create instances
     * @param callInstance the instance on which the method is called or <tt>null</tt> if the method is static
     * @throws NoSuchInstantiatorException if the provided method is not static or does not return the given objectClass
     */
    public MethodInstantiator(Class<? extends T> objectClass, Method method, Object callInstance) throws NoSuchInstantiatorException {
        if (!objectClass.isAssignableFrom(method.getReturnType()))
            throw new NoSuchInstantiatorException("Method " + method + " does not return requested " + objectClass);
        if (callInstance == null) {
            if (!Modifier.isStatic(method.getModifiers()))
                throw new NoSuchInstantiatorException("Method " + method + " must be static, since no call instance was provided");
        } else {
            if (!method.getDeclaringClass().isInstance(callInstance))
                throw new NoSuchInstantiatorException("Method " + method + " cannot be called on an instance of " + callInstance.getClass());
        }
        this.objectClass = objectClass;
        this.method = method;
        this.callInstance = callInstance;
    }

    /**
     * Creates a new instance of MethodInstantiator for creating instances of
     * {@code objectClass} by calling a factory method with the specified name and prototype.
     *
     * @param checkClass the class the instances of which will be created
     * @param methodClass the class from which the method is taken
     * @param methodName the name of the factory method within the {@code objectClass}
     * @param prototype the types of constructor arguments
     * @throws NoSuchInstantiatorException if the there is no method for the given name and prototype or
     *          if such method is not static or does not return the given objectClass
     */
    public MethodInstantiator(Class<? extends T> checkClass, Class<?> methodClass, String methodName, Class<?>... prototype) throws NoSuchInstantiatorException {
        this(checkClass, getMethod(methodClass, methodName, true, prototype), null);
    }

    /**
     * Creates a new instance of MethodInstantiator for creating instances of
     * {@code objectClass} by calling a method with the specified name and prototype
     * on the given {@code callInstance}.
     *
     * @param checkClass the class the instances of which will be created
     * @param callInstance the instance on which the method is called
     * @param methodName the name of the factory method within the {@code objectClass}
     * @param prototype the types of constructor arguments
     * @throws NoSuchInstantiatorException if the there is no method for the given name and prototype or
     *          if such method is not static or does not return the given objectClass
     */
    public MethodInstantiator(Class<? extends T> checkClass, Object callInstance, String methodName, Class<?>... prototype) throws NoSuchInstantiatorException {
        this(checkClass, getMethod(callInstance.getClass(), methodName, true, prototype), callInstance);
    }

    /**
     * Creates a new instance of MethodInstantiator for creating instances of
     * {@code objectClass} by calling a method with the specified name
     * and the number of parameters on the given {@code callInstance}.
     * Note that if there are several methods with the same name and number of
     * arguments, the first one inspected is selected.
     *
     * @param checkClass the class the instances of which will be created
     * @param methodClass the class from which the method is taken
     * @param methodName the name of the factory method within the {@code objectClass}
     * @param convertStringArguments if <tt>true</tt> the string values from the arguments are converted using {@link messif.utility.Convert#stringToType}
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @param arguments the arguments for the method
     * @throws NoSuchInstantiatorException if the there is no method for the given name and number of arguments or
     *          if such method is not static or does not return the given objectClass
     */
    public MethodInstantiator(Class<? extends T> checkClass, Class<?> methodClass, String methodName, boolean convertStringArguments, Map<String, Object> namedInstances, Object[] arguments) throws NoSuchInstantiatorException {
        this(checkClass, getMethod(methodClass, methodName, convertStringArguments, true, namedInstances, arguments), null);
    }

    /**
     * Creates a new instance of MethodInstantiator for creating instances of
     * {@code objectClass} by calling a method with the specified name
     * and the number of parameters on the given {@code callInstance}.
     * Note that if there are several methods with the same name and number of
     * arguments, the first one inspected is selected.
     *
     * @param checkClass the class the instances of which will be created
     * @param callInstance the instance on which the method is called
     * @param methodName the name of the factory method within the {@code objectClass}
     * @param convertStringArguments if <tt>true</tt> the string values from the arguments are converted using {@link messif.utility.Convert#stringToType}
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @param arguments the arguments for the method
     * @throws NoSuchInstantiatorException if the there is no method for the given name and number of arguments or
     *          if such method is not static or does not return the given objectClass
     */
    public MethodInstantiator(Class<? extends T> checkClass, Object callInstance, String methodName, boolean convertStringArguments, Map<String, Object> namedInstances, Object[] arguments) throws NoSuchInstantiatorException {
        this(checkClass, getMethod(callInstance.getClass(), methodName, convertStringArguments, true, namedInstances, arguments), callInstance);
    }

    /**
     * Creates a new instance of MethodInstantiator for creating instances of
     * {@code objectClass} by calling a factory method with the specified name
     * and the number of parameters. Note that if there are several methods with
     * the same name and number of arguments, the first one inspected is selected.
     *
     * @param checkClass the class the instances of which will be created
     * @param methodClass the class from which the method is taken
     * @param methodName the name of the factory method within the {@code objectClass}
     * @param argumentCount the number of arguments that the method should have
     * @throws NoSuchInstantiatorException if the there is no method for the given name and number of arguments or
     *          if such method is not static or does not return the given objectClass
     */
    public MethodInstantiator(Class<? extends T> checkClass, Class<?> methodClass, String methodName, int argumentCount) throws NoSuchInstantiatorException {
        this(checkClass, getMethod(methodClass, methodName, true, argumentCount), null);
    }

    /**
     * Creates a new instance of MethodInstantiator for creating instances of
     * {@code objectClass} by calling a method with the specified name
     * and the number of parameters on the given {@code callInstance}.
     * Note that if there are several methods with the same name and number of
     * arguments, the first one inspected is selected.
     *
     * @param checkClass the class the instances of which will be created
     * @param callInstance the instance on which the method is called
     * @param methodName the name of the factory method within the {@code objectClass}
     * @param argumentCount the number of arguments that the method should have
     * @throws NoSuchInstantiatorException if the there is no method for the given name and number of arguments or
     *          if such method is not static or does not return the given objectClass
     */
    public MethodInstantiator(Class<? extends T> checkClass, Object callInstance, String methodName, int argumentCount) throws NoSuchInstantiatorException {
        this(checkClass, getMethod(callInstance.getClass(), methodName, true, argumentCount), callInstance);
    }

    /**
     * Retrieves a method with the given name and prototype from the given method class.
     * @param methodClass the class in which to search for the method
     * @param name the name of the method
     * @param publicOnlyMethods flag wheter to search for all declared methods (<tt>false</tt>) or only for the public ones (<tt>true</tt>)
     * @param prototype the method prototype
     * @return the method found
     * @throws NoSuchInstantiatorException if the there is no method for the given name and prototype
     */
    public static Method getMethod(Class<?> methodClass, String name, boolean publicOnlyMethods, Class<?>... prototype) throws NoSuchInstantiatorException {
        Class<?> currentClass = methodClass;
        do {
            try {
                return publicOnlyMethods ? methodClass.getMethod(name, prototype) : methodClass.getDeclaredMethod(name, prototype);
            } catch (NoSuchMethodException ignore) {
                currentClass = currentClass.getSuperclass();
            }
        } while (!publicOnlyMethods && currentClass != null);
        throw new NoSuchInstantiatorException(methodClass, name, prototype);
    }

    /**
     * Retrieves a method with the given name and argument count from the given method class.
     * @param methodClass the class in which to search for the method
     * @param name the name of the method
     * @param publicOnlyMethods flag wheter to search for all declared methods (<tt>false</tt>) or only for the public ones (<tt>true</tt>)
     * @param argumentCount the number of arguments that the method should have
     * @return the method found
     * @throws NoSuchInstantiatorException if the there is no method for the given name and number of arguments
     */
    public static Method getMethod(Class<?> methodClass, String name, boolean publicOnlyMethods, int argumentCount) throws NoSuchInstantiatorException {
        Class<?> currentClass = methodClass;
        do {
            Method[] methods = publicOnlyMethods ? currentClass.getMethods() : currentClass.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++)
                if (name.equals(methods[i].getName()) && methods[i].getParameterTypes().length == argumentCount)
                    return methods[i];
            currentClass = currentClass.getSuperclass();
        } while (!publicOnlyMethods && currentClass != null);
        throw new NoSuchInstantiatorException(methodClass, name, argumentCount);
    }

    /**
     * Retrieves a method with the given name and prototype from the given method class.
     * @param methodClass the class in which to search for the method
     * @param name the name of the method
     * @param convertStringArguments if <tt>true</tt> the string values from the arguments are converted using {@link Convert#stringToType}
     * @param publicOnlyMethods flag wheter to search for all declared methods (<tt>false</tt>) or only for the public ones (<tt>true</tt>)
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @param arguments the arguments for the method
     * @return the method found
     * @throws NoSuchInstantiatorException if the there is no method for the given name and prototype
     */
    public static Method getMethod(Class<?> methodClass, String name, boolean convertStringArguments, boolean publicOnlyMethods, Map<String, Object> namedInstances, Object[] arguments) throws NoSuchInstantiatorException {
        Class<?> currentClass = methodClass;
        do {
            Method[] methods = publicOnlyMethods ? currentClass.getMethods() : currentClass.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++)
                if (name.equals(methods[i].getName()) && Instantiators.isPrototypeMatching(methods[i].getParameterTypes(), arguments, convertStringArguments, namedInstances))
                    return methods[i];
            currentClass = currentClass.getSuperclass();
        } while (!publicOnlyMethods && currentClass != null);
        throw new NoSuchInstantiatorException(methodClass, name, convertStringArguments, arguments);
    }

    /**
     * Calls a method with the given name and prototype from the given method class.
     * @param methodInstanceOrClass the instance or class in which to search for the method
     * @param name the name of the method
     * @param convertStringArguments if <tt>true</tt> the string values from the arguments are converted using {@link Convert#stringToType}
     * @param publicOnlyMethods flag wheter to search for all declared methods (<tt>false</tt>) or only for the public ones (<tt>true</tt>)
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @param arguments the arguments for the method
     * @return the method found
     * @throws NoSuchInstantiatorException if the there is no method for the given name and prototype
     * @throws InvocationTargetException if there was an exception when calling the constructor
     */
    public static Object callMethod(Object methodInstanceOrClass, String name, boolean convertStringArguments, boolean publicOnlyMethods, Map<String, Object> namedInstances, Object... arguments) throws NoSuchInstantiatorException, InvocationTargetException {
        // Convert method instance from string using named instances
        if (convertStringArguments && methodInstanceOrClass instanceof String && namedInstances != null)
            methodInstanceOrClass = namedInstances.get((String)methodInstanceOrClass);

        Class<?> methodClass;
        Object methodInstance;
        if (methodInstanceOrClass instanceof Class) {
            methodClass = (Class)methodInstanceOrClass;
            methodInstance = null;
        } else {
            methodClass = methodInstanceOrClass.getClass();
            methodInstance = methodInstanceOrClass;
        }

        Object[] args = new Object[arguments.length];
        System.arraycopy(arguments, 0, args, 0, args.length);
        Method method = getMethod(methodClass, name, convertStringArguments, publicOnlyMethods, namedInstances, args);
        if (methodInstance == null && !Modifier.isStatic(method.getModifiers()))
            throw new NoSuchInstantiatorException("Cannot call a non-static method " + method + " without instance");
        try {
            return method.invoke(methodInstance, args);
        } catch (IllegalAccessException e) {
            throw new NoSuchInstantiatorException("Cannot access " + method);
        } catch (IllegalArgumentException e) {
            throw new InternalError("String arguments should be converted but they are not");
        }
    }

    /**
     * Calls a method with the given name and prototype from the given method class.
     * @param <T> the class created by the factory method
     * @param factoryClass the class in which to search for the factory method
     * @param name the name of the method
     * @param convertStringArguments if <tt>true</tt> the string values from the arguments are converted using {@link Convert#stringToType}
     * @param publicOnlyMethods flag wheter to search for all declared methods (<tt>false</tt>) or only for the public ones (<tt>true</tt>)
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @param arguments the arguments for the method
     * @return the method found
     * @throws NoSuchInstantiatorException if the there is no method for the given name and prototype
     * @throws InvocationTargetException if there was an exception when calling the constructor
     */
    public static <T> T callFactoryMethod(Class<? extends T> factoryClass, String name, boolean convertStringArguments, boolean publicOnlyMethods, Map<String, Object> namedInstances, Object... arguments) throws NoSuchInstantiatorException, InvocationTargetException {
        return factoryClass.cast(callMethod(factoryClass, name, convertStringArguments, publicOnlyMethods, namedInstances, arguments));
    }


    //****************** Instantiation support ******************//

    /**
     * Creates a new instance using the encapsulated method.
     * The arguments must be compatible with the prototype that was given while
     * creating this {@link MethodInstantiator} class.
     * @param arguments the arguments for the encapsulated factory method
     * @return the new instance
     * @throws IllegalArgumentException if the arguments are not compatible with the method prototype
     * @throws InvocationTargetException if there was an exception thrown when the method was invoked
     */
    @Override
    public T instantiate(Object... arguments) throws IllegalArgumentException, InvocationTargetException {
        try {
            return objectClass.cast(method.invoke(callInstance, arguments));
        } catch (IllegalAccessException e) {
            throw new InternalError("Cannot call " + method + ": " + e); // This should never happen - the constructor is public
        }
    }

    @Override
    public Class<?>[] getInstantiatorPrototype() {
        return method.getParameterTypes();
    }

    @Override
    public Class<? extends T> getInstantiatorClass() {
        return objectClass;
    }

    @Override
    public String toString() {
        return method.toString();
    }

}
