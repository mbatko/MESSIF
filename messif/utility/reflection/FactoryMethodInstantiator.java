/*
 * FactoryMethodInstantiator
 *
 */

package messif.utility.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

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
 * @param <T> the class the instances of which will be created by this FactoryMethodInstantiator
 */
public class FactoryMethodInstantiator<T> implements Instantiator<T> {
    //****************** Attributes ******************//

    /** Method that returns T needed for instantiating objects */
    private final Method method;
    /** Class created by this instantiator */
    private final Class<? extends T> objectClass;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of FactoryMethodInstantiator for creating instances of
     * {@code objectClass} via the given method.
     *
     * @param objectClass the class the instances of which will be created
     * @param method the factory method used to create instances
     * @throws IllegalArgumentException if the provided method is not static or does not return the given objectClass
     */
    public FactoryMethodInstantiator(Class<? extends T> objectClass, Method method) throws IllegalArgumentException {
        this.objectClass = objectClass;
        this.method = method;
        if (!Modifier.isStatic(method.getModifiers()))
            throw new IllegalArgumentException("Factory method " + method + " must be static");
        if (!objectClass.isAssignableFrom(method.getReturnType()))
            throw new IllegalArgumentException("Factory method " + method + " does not return requested " + objectClass);
    }

    /**
     * Creates a new instance of FactoryMethodInstantiator for creating instances of
     * {@code objectClass} that accepts parameters of the given prototype.
     *
     * @param checkClass the class the instances of which will be created
     * @param methodClass the class from which the method is taken
     * @param methodName the name of the factory method within the {@code objectClass}
     * @param prototype the types of constructor arguments
     * @throws IllegalArgumentException if the there is no method for the given name and prototype or
     *          if such method is not static or does not return the given objectClass
     */
    public FactoryMethodInstantiator(Class<? extends T> checkClass, Class<?> methodClass, String methodName, Class<?>... prototype) throws IllegalArgumentException {
        this(checkClass, getMethod(methodClass, methodName, prototype));
    }

    /**
     * Creates a new instance of FactoryMethodInstantiator for creating instances of
     * {@code objectClass} that accepts the given number of parameters.
     * Note that if there are several methods of the same name and number of arguments,
     * one is selected.
     *
     * @param checkClass the class the instances of which will be created
     * @param methodClass the class from which the method is taken
     * @param methodName the name of the factory method within the {@code objectClass}
     * @param argumentCount the number of arguments that the method should have
     * @throws IllegalArgumentException if the there is no method for the given name and number of arguments or
     *          if such method is not static or does not return the given objectClass
     */
    public FactoryMethodInstantiator(Class<? extends T> checkClass, Class<?> methodClass, String methodName, int argumentCount) throws IllegalArgumentException {
        this(checkClass, getMethod(methodClass, methodName, argumentCount));
    }

    /**
     * Retrieves a public method with the given name and prototype from the given method class.
     * @param methodClass the class in which to search for the method
     * @param name the name of the method
     * @param prototype the method prototype
     * @return the method found
     * @throws IllegalArgumentException if the there is no method for the given name and prototype
     */
    private static Method getMethod(Class<?> methodClass, String name, Class<?>... prototype) throws IllegalArgumentException {
        try {
            return methodClass.getMethod(name, prototype);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("There is no method " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a public method with the given name and argument count from the given method class.
     * @param methodClass the class in which to search for the method
     * @param name the name of the method
     * @param argumentCount the number of arguments that the method should have
     * @return the method found
     * @throws IllegalArgumentException if the there is no method for the given name and number of arguments
     */
    private static Method getMethod(Class<?> methodClass, String name, int argumentCount) throws IllegalArgumentException {
        Method[] methods = methodClass.getMethods();
        for (int i = 0; i < methods.length; i++)
            if (methods[i].getParameterTypes().length == argumentCount)
                return methods[i];
        throw new IllegalArgumentException("There is no method " + methodClass.getName() + "." + name + "(...) with " + argumentCount + " arguments");
    }


    //****************** Instantiation support ******************//

    /**
     * Creates a new instance using the encapsulated factory method.
     * The arguments must be compatible with the prototype that was given while
     * {@link #FactoryMethodInstantiator(java.lang.Class, java.lang.Class, java.lang.String, java.lang.Class[]) creating} this
     * {@link FactoryMethodInstantiator} class.
     * @param arguments the arguments for the encapsulated factory method
     * @return the new instance
     * @throws IllegalArgumentException if the arguments are not compatible with the method prototype
     * @throws InvocationTargetException if there was an exception thrown when the method was invoked
     */
    public T instantiate(Object... arguments) throws IllegalArgumentException, InvocationTargetException {
        try {
            return objectClass.cast(method.invoke(null, arguments));
        } catch (IllegalAccessException e) {
            throw new InternalError("Cannot call " + method + ": " + e); // This should never happen - the constructor is public
        }
    }

    public Class<?>[] getInstantiatorPrototype() {
        return method.getParameterTypes();
    }

    public Class<? extends T> getInstantiatorClass() {
        return objectClass;
    }

    @Override
    public String toString() {
        return method.toString();
    }

}
