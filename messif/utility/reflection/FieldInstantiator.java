/*
 * FieldInstantiator
 *
 */

package messif.utility.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * This class allows to create instances of a given class.
 * A field of the given type is encapsulated and used in subsequent calls.
 * Note that FieldInstantiator instantiates by getting a value of a given filed
 * on the instance provided in arguments (can be omitted if the field is static).
 *
 * <p>
 * This class provides a convenient way of repeatable creation of instances
 * of a given class without the need of repetable field retrieval and checking all
 * the exceptions.
 * </p>
 *
 * @param <T> the class the instances of which will be created by this FieldInstantiator
 */
public class FieldInstantiator<T> implements Instantiator<T> {
    //****************** Attributes ******************//

    /** Field that returns T needed for instantiating objects */
    private final Field field;
    /** Class created by this instantiator */
    private final Class<? extends T> objectClass;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of FieldInstantiator for creating instances of
     * {@code objectClass} via the given field.
     *
     * @param objectClass the class the instances of which will be created
     * @param field the field used to create instances
     * @throws IllegalArgumentException if the provided field has not the given objectClass type
     */
    public FieldInstantiator(Class<? extends T> objectClass, Field field) throws IllegalArgumentException {
        this.objectClass = objectClass;
        this.field = field;
        if (!objectClass.isAssignableFrom(field.getType()))
            throw new IllegalArgumentException("Field " + field + " has not the requested " + objectClass);
    }

    /**
     * Creates a new instance of FieldInstantiator for creating instances of {@code objectClass}.
     *
     * @param checkClass the class the instances of which will be created
     * @param fieldClass the class in which the field is looked up
     * @param fieldName the name of the field within the {@code objectClass}
     * @throws IllegalArgumentException if the there is no field for the given name or
     *          if such field has not the given objectClass type
     */
    public FieldInstantiator(Class<? extends T> checkClass, Class<?> fieldClass, String fieldName) throws IllegalArgumentException {
        this(checkClass, getField(fieldClass, fieldName));
    }

    /**
     * Retrieves a public field with the given name from the given class.
     * @param fieldClass the class in which to search for the field
     * @param name the name of the field
     * @return the field found
     * @throws IllegalArgumentException if the there is no field for the given name
     */
    private static Field getField(Class<?> fieldClass, String name) throws IllegalArgumentException {
        try {
            return fieldClass.getField(name);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("There is no field " + e.getMessage(), e);
        }
    }


    //****************** Instantiation support ******************//

    /**
     * Creates a new instance using the encapsulated field.
     * There must be a single argument with the instance from which to get the field.
     * 
     * @param arguments the instance from which to get the field (or <tt>null</tt> if the field is static)
     * @return the new instance
     */
    public T instantiate(Object... arguments) throws IllegalArgumentException, InvocationTargetException {
        try {
            return objectClass.cast(field.get(arguments == null || arguments.length == 0 ? null : arguments[0]));
        } catch (IllegalAccessException e) {
            throw new InternalError("Cannot get " + field + ": " + e); // This should never happen - the constructor is public
        }
    }

    public Class<?>[] getInstantiatorPrototype() {
        if (Modifier.isStatic(field.getModifiers()))
            return new Class<?>[0];
        else
            return new Class<?>[] { objectClass };
    }

    public Class<? extends T> getInstantiatorClass() {
        return objectClass;
    }

    @Override
    public String toString() {
        return field.toString();
    }

}
