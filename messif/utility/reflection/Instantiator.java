/*
 * Instantiator
 * 
 */

package messif.utility.reflection;

import java.lang.reflect.InvocationTargetException;

/**
 * Interface for creating instances of a given class.
 *
 * <p>
 * This class provides a convenient way of repeatable creation of instances
 * without the need of repetable inspection of the target class.
 * </p>
 *
 * @param <T> the class the instances of which will be created by this Instantiator
 * @author xbatko
 */
public interface Instantiator<T> {
    /**
     * Creates an instance for the given arguments.
     * @param arguments the arguments for the intstance
     * @return a new instance
     * @throws IllegalArgumentException if the arguments are not compatible
     * @throws InvocationTargetException if there was an exception thrown when the instance was created
     */
    public T instantiate(Object... arguments) throws IllegalArgumentException, InvocationTargetException;

    /**
     * Returns the class instantiated by this Instantiator.
     * @return the instantiated class
     */
    public Class<? extends T> getInstantiatorClass();

    /**
     * Returns the classes of arguments for the {@link #instantiate(java.lang.Object[])} method.
     * @return the prototype of instantiatior arguments
     */
    public Class<?>[] getInstantiatorPrototype();
}
