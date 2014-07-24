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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import messif.utility.Convert;

/**
 * This class allows to create instances of a given class.
 * A field of the given type is encapsulated and used in subsequent calls.
 * Note that FieldInstantiator gives always the same instance.
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
    /** Instance from which the field is taken or <tt>null</tt> if the field is static */
    private final Object fieldInstance;
    /** Class created by this instantiator */
    private final Class<? extends T> objectClass;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of FieldInstantiator for creating instances of
     * {@code objectClass} via the given field.
     *
     * @param objectClass the class the instances of which will be created
     * @param field the field used to create instances
     * @param fieldInstance the instance from which the field is taken or <tt>null</tt> if the field is static
     * @throws NoSuchInstantiatorException if the provided field has not the given objectClass type
     */
    public FieldInstantiator(Class<? extends T> objectClass, Field field, Object fieldInstance) throws NoSuchInstantiatorException {
        if (!Convert.wrapPrimitiveType(objectClass).isAssignableFrom(Convert.wrapPrimitiveType(field.getType())))
            throw new NoSuchInstantiatorException("Field " + field + " has not the requested " + objectClass);
        if (fieldInstance == null) {
            if (!Modifier.isStatic(field.getModifiers()))
                throw new NoSuchInstantiatorException("Field " + field + " must be static, since no field instance was provided");

        } else {
            if (!field.getDeclaringClass().isInstance(fieldInstance))
                throw new NoSuchInstantiatorException("Field " + field + " cannot be taken from an instance of " + fieldInstance.getClass());
        }
        this.objectClass = objectClass;
        this.field = field;
        this.fieldInstance = fieldInstance;
    }

    /**
     * Creates a new instance of FieldInstantiator for creating instances of {@code objectClass}.
     *
     * @param checkClass the class the instances of which will be created
     * @param fieldClass the class in which the field is looked up
     * @param fieldName the name of the field within the {@code objectClass}
     * @throws NoSuchInstantiatorException if the there is no field for the given name or
     *          if such field has not the given objectClass type
     */
    public FieldInstantiator(Class<? extends T> checkClass, Class<?> fieldClass, String fieldName) throws NoSuchInstantiatorException {
        this(checkClass, getField(fieldClass, true, fieldName), (Object)null);
    }

    /**
     * Creates a new instance of FieldInstantiator for creating instances of {@code objectClass}.
     *
     * @param checkClass the class the instances of which will be created
     * @param fieldInstance the instance from which the field is taken or <tt>null</tt> if the field is static
     * @param fieldName the name of the field within the {@code objectClass}
     * @throws NoSuchInstantiatorException if the there is no field for the given name or
     *          if such field has not the given objectClass type
     */
    public FieldInstantiator(Class<? extends T> checkClass, Object fieldInstance, String fieldName) throws NoSuchInstantiatorException {
        this(checkClass, getField(fieldInstance.getClass(), true, fieldName), fieldInstance);
    }

    /**
     * Retrieves a field with the given name from the given class.
     * @param fieldClass the class in which to search for the field
     * @param publicOnlyField flag wheter to search for all declared fields (<tt>false</tt>) or only for the public ones (<tt>true</tt>)
     * @param name the name of the field
     * @return the field found
     * @throws NoSuchInstantiatorException if the there is no field for the given name
     */
    public static Field getField(Class<?> fieldClass, boolean publicOnlyField, String name) throws NoSuchInstantiatorException {
        Class<?> currentClass = fieldClass;
        do {
            try {
                return publicOnlyField ? currentClass.getField(name) : currentClass.getDeclaredField(name);
            } catch (NoSuchFieldException ignore) {
                currentClass = currentClass.getSuperclass();
            }
        } while (!publicOnlyField && currentClass != null);

        throw new NoSuchInstantiatorException(fieldClass, name);
    }


    //****************** Instantiation support ******************//

    /**
     * Returns the instance in the encapsulated field.
     * 
     * @param arguments arguments are ignored
     * @return the encapsulated field instance
     */
    @Override
    public T instantiate(Object... arguments) throws IllegalArgumentException, InvocationTargetException {
        try {
            return objectClass.cast(field.get(fieldInstance));
        } catch (IllegalAccessException e) {
            throw new InternalError("Cannot get " + field + ": " + e); // This should never happen - the constructor is public
        }
    }

    @Override
    public Class<?>[] getInstantiatorPrototype() {
        if (Modifier.isStatic(field.getModifiers()))
            return new Class<?>[0];
        else
            return new Class<?>[] { objectClass };
    }

    @Override
    public Class<? extends T> getInstantiatorClass() {
        return objectClass;
    }

    @Override
    public String toString() {
        return field.toString();
    }

}
