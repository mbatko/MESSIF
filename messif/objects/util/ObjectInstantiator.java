/*
 * ObjectInstantiator
 * 
 */

package messif.objects.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * This class allows to create instances of a given class.
 * A constructor with the given prototype is encapsulated and used in subsequent calls.
 *
 * <p>
 * This class provides a convenient way of repeatable creation of instances
 * of a without the need of repetable constructor retrieval and processing of all the
 * checked exceptions.
 * </p>
 *
 * @param <T> the class the instances of which will be created by this ObjectInstantiator
 * @author xbatko
 */
public class ObjectInstantiator<T> {
    //****************** Attributes ******************//

    /** Constructor objects of type T needed for instantiating objects */
    private final Constructor<? extends T> constructor;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of ObjectInstantiator for creating instances of
     * {@code objectClass} that accepts parameters of the given prototype.
     *
     * @param objectClass the class the instances of which will be created
     * @param prototype the types of constructor arguments
     * @throws IllegalArgumentException if the provided class does not have a proper constructor
     */
    public ObjectInstantiator(Class<? extends T> objectClass, Class<?>... prototype) throws IllegalArgumentException {
        try {
            this.constructor = objectClass.getConstructor(prototype);
            if (Modifier.isAbstract(objectClass.getModifiers()))
                throw new IllegalArgumentException("Cannot create abstract " + objectClass);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("There is no constructor " + e.getMessage(), e);
        }
    }


    //****************** Instantiation support ******************//

    /**
     * Creates a new instance using the encapsulated constructor.
     * The arguments must be compatible with the prototype that was given while
     * {@link #ObjectInstantiator(java.lang.Class, java.lang.Class[]) creating} this
     * {@link ObjectInstantiator} class.
     * @param arguments the arguments for the encapsulated constructor
     * @return the new instance
     * @throws IllegalArgumentException if the arguments are not compatible with the constructor prototype
     * @throws InvocationTargetException if there was an exception thrown when the constructor was invoked
     */
    public T newInstance(Object... arguments) throws IllegalArgumentException, InvocationTargetException {
        try {
            return constructor.newInstance(arguments);
        } catch (InstantiationException e) {
            throw new InternalError("Cannot call " + constructor + ": " + e); // This should never happen - the class is not abstract
        } catch (IllegalAccessException e) {
            throw new InternalError("Cannot call " + constructor + ": " + e); // This should never happen - the constructor is public
        }
    }

    @Override
    public String toString() {
        return constructor.toString();
    }

}
