/*
 * Instantiators
 *
 */

package messif.utility.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import messif.utility.Convert;

/**
 * Collection of utility methods for {@link Instantiator}s.
 *
 * @author xbatko
 */
public abstract class Instantiators {

    /**
     * Creates an {@link Instantiator} for the given signature.
     * It can be a constructor, method, or field signature.
     *
     * @param <T> the class of instances that the instantiator will create
     * @param signature the signature of a constructor, method, or field
     * @param checkClass the class of instances that the instantiator will create
     * @return a new instantiator
     * @throws IllegalArgumentException if the instantiator cannot be created
     */
    public static <T> Instantiator<T> createInstantiator(String signature, Class<? extends T> checkClass) throws IllegalArgumentException {
        // Parse arguments (enclosed in braces)
        String[] args;
        String callname;
        int openParenthesisPos = signature.indexOf('(');
        int closeParenthesisPos = signature.lastIndexOf(')');
        if (openParenthesisPos == -1 || openParenthesisPos == closeParenthesisPos - 1) { // There are no parenthesis or they are empty
            args = new String[0];
            callname = signature;
        } else if (closeParenthesisPos == -1) {
            throw new IllegalArgumentException("Missing closing parenthesis: " + signature);
        } else {
            args = signature.substring(openParenthesisPos + 1, closeParenthesisPos).split("\\s*,\\s*");
            callname = signature.substring(0, openParenthesisPos);
        }

        // Create instance
        try {
            // Try call name as a constructor
            Class<? extends T> clazz = Convert.getClassForName(callname, checkClass);
            return new ConstructorInstantiator<T>(clazz, args.length);
        } catch (ClassNotFoundException e) {
            // Class not found, try dot earlier
            int dotPos = callname.lastIndexOf('.');
            if (dotPos == -1)
                throw new IllegalArgumentException("Cannot create instantiator for " + signature + ": " + e);

            // Class without last item, which is supposed to be a factory method name or a field name
            Class<?> clazz;
            try {
                clazz = Class.forName(callname.substring(0, dotPos));
            } catch (ClassNotFoundException ex) {
                throw new IllegalArgumentException("Cannot create instantiator for " + signature + ": " + ex);
            }
            callname = callname.substring(dotPos + 1);

            return (openParenthesisPos == -1) ?
                new FieldInstantiator<T>(checkClass, clazz, callname) :
                new FactoryMethodInstantiator<T>(checkClass, clazz, callname, args.length);
        }
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
     * @param <E> the type of the instantiated object
     * @param constructors the list of constructors of the desired class to try
     * @param arguments the string arguments for the constructor that will be converted to correct types
     * @param argStartIndex index in the string arguments array from which to expect arguments (all the previous items are ignored)
     * @param argEndIndex index in the string arguments array to which to expect arguments (all the following items are ignored)
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @return a new instance of the class the constructors were specified for
     * @throws InvocationTargetException
     *              if the constructor can't be found for the specified arguments,
     *              the argument string-to-type convertion has failed or
     *              there was an error during instantiation
     */
    public static <E> E createInstanceWithStringArgs(List<Constructor<E>> constructors, String[] arguments, int argStartIndex, int argEndIndex, Map<String, Object> namedInstances) throws InvocationTargetException {
        InstantiationException lastException = null;
        Constructor<E> lastConstructor = null;

        // Search for proper constructor
        for (Constructor<E> constructor : constructors) {
            try {
                // Get constructor parameter types
                Class<?>[] argTypes = constructor.getParameterTypes();

                // Skip constructors with different number of parameters
                if (argTypes.length != argEndIndex - argStartIndex + 1)
                    // Test variable number of arguments parameter (last argument is an array)
                    if (argTypes.length == 0 || !argTypes[argTypes.length - 1].isArray() || (argTypes.length - 1) > argEndIndex - argStartIndex + 1)
                        continue;

                // Try to convert the string arguments
                return constructor.newInstance(Convert.parseTypesFromString(arguments, argTypes, argStartIndex, argEndIndex, namedInstances));
            } catch (InstantiationException e) {
                lastException = e;
                lastConstructor = constructor;
            } catch (IllegalAccessException e) {
                throw new InvocationTargetException(e);
            }
        }

        if (lastConstructor == null)
            throw new InvocationTargetException(new NoSuchMethodException("Constructor for specified arguments cannot be found"));
        throw new InvocationTargetException(lastException, lastConstructor.toString());
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
     * <p>
     * Note also that {@link messif.objects.LocalAbstractObject} parameters will not be converted. For this fuctionality,
     * use the full {@link #createInstanceWithStringArgs createInstanceWithStringArgs} method instead.
     * </p>
     *
     * @param <E> the type of the instantiated object
     * @param constructors the list of constructors of the desired class to try
     * @param arguments the string arguments for the constructor that will be converted to correct types
     * @param argStartIndex index in the string arguments array from which to expect arguments (all the previous items are ignored)
     * @param argEndIndex index in the string arguments array to which to expect arguments (all the following items are ignored)
     * @return a new instance of the class the constructors were specified for
     * @throws InvocationTargetException
     *              if the constructor can't be found for the specified arguments,
     *              the argument string-to-type convertion has failed or
     *              there was an error during instantiation
     */
    public static <E> E createInstanceWithStringArgs(List<Constructor<E>> constructors, String[] arguments, int argStartIndex, int argEndIndex) throws InvocationTargetException {
        return createInstanceWithStringArgs(constructors, arguments, argStartIndex, argEndIndex, null);
    }

    /**
     * Creates a new instance of a class using string arguments for its constructor.
     * The list of constructors of the desired class must be provided as <code>constructors</code> argument.
     * The constructors are tried one by one from this list and if the <code>arguments</code>
     * are convertible to the arguments of that constructor, a new instance is created.
     * <p>
     * Note that only constructors with the number of arguments equal to <code>arguments.length - argStartIndex</code>
     * are tried. If there are several constructors with the same number of arguments, the first (in the order
     * of the list) constructor that succeeds in converting string arguments will be used.
     * </p>
     * <p>
     * Note also that only types convertible by {@link Convert#stringToType} method can be used in constructors.
     * </p>
     * <p>
     * Note also that {@link messif.objects.LocalAbstractObject} parameters will not be converted. For this fuctionality,
     * use the full {@link #createInstanceWithStringArgs createInstanceWithStringArgs} method instead.
     * </p>
     *
     * @param <E> the type of the instantiated object
     * @param constructors the list of constructors of the desired class to try
     * @param arguments the string arguments for the constructor that will be converted to correct types
     * @param argStartIndex index in the string arguments array from which to expect arguments (all the previous items are ignored)
     * @return a new instance of the class the constructors were specified for
     * @throws InvocationTargetException
     *              if the constructor can't be found for the specified arguments,
     *              the argument string-to-type convertion has failed or
     *              there was an error during instantiation
     */
    public static <E> E createInstanceWithStringArgs(List<Constructor<E>> constructors, String[] arguments, int argStartIndex) throws InvocationTargetException {
        return createInstanceWithStringArgs(constructors, arguments, argStartIndex, arguments.length - 1);
    }

    /**
     * Creates a new instance of a class using string arguments for its constructor.
     * The list of constructors of the desired class must be provided as <code>constructors</code> argument.
     * The constructors are tried one by one from this list and if the <code>arguments</code>
     * are convertible to the arguments of that constructor, a new instance is created.
     * <p>
     * Note that only constructors with the number of arguments equal to <code>arguments.length - argStartIndex</code>
     * are tried. If there are several constructors with the same number of arguments, the first (in the order
     * of the list) constructor that succeeds in converting string arguments will be used.
     * </p>
     * <p>
     * Note also that only types convertible by {@link Convert#stringToType} method can be used in constructors.
     * </p>
     *
     * @param <E> the type of the instantiated object
     * @param constructors the list of constructors of the desired class to try
     * @param arguments the string arguments for the constructor that will be converted to correct types
     * @param argStartIndex index in the string arguments array from which to expect arguments (all the previous items are ignored)
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @return a new instance of the class the constructors were specified for
     * @throws InvocationTargetException
     *              if the constructor can't be found for the specified arguments,
     *              the argument string-to-type convertion has failed or
     *              there was an error during instantiation
     */
    public static <E> E createInstanceWithStringArgs(List<Constructor<E>> constructors, String[] arguments, int argStartIndex, Map<String, Object> namedInstances) throws InvocationTargetException {
        return createInstanceWithStringArgs(constructors, arguments, argStartIndex, arguments.length - 1, namedInstances);
    }

    /**
     * Creates a new instance of a class using string arguments for its constructor.
     * The list of constructors of the desired class must be provided as <code>constructors</code> argument.
     * The constructors are tried one by one from this list and if the <code>arguments</code>
     * are convertible to the arguments of that constructor, a new instance is created.
     * <p>
     * Note that only constructors with the same number of arguments as the length of <code>arguments</code>
     * are tried. If there are several constructors with the same number of arguments, the first (in the order
     * of the list) constructor that succeeds in converting string arguments will be used.
     * </p>
     * <p>
     * Note also that only types convertible by {@link Convert#stringToType} method can be used in constructors.
     * </p>
     * <p>
     * Note also that {@link messif.objects.LocalAbstractObject} parameters will not be converted. For this fuctionality,
     * use the full {@link #createInstanceWithStringArgs createInstanceWithStringArgs} method instead.
     * </p>
     *
     * @param <E> the type of the instantiated object
     * @param constructors the list of constructors of the desired class to try
     * @param arguments the string arguments for the constructor that will be converted to correct types
     * @return a new instance of the class the constructors were specified for
     * @throws InvocationTargetException
     *              if the constructor can't be found for the specified arguments,
     *              the argument string-to-type convertion has failed or
     *              there was an error during instantiation
     */
    public static <E> E createInstanceWithStringArgs(List<Constructor<E>> constructors, String... arguments) throws InvocationTargetException {
        return createInstanceWithStringArgs(constructors, arguments, 0);
    }

    /**
     * Creates a new instance of a class using string arguments for its constructor.
     * The list of constructors of the desired class must be provided as <code>constructors</code> argument.
     * The constructors are tried one by one from this list and if the <code>arguments</code>
     * are convertible to the arguments of that constructor, a new instance is created.
     * <p>
     * Note that only constructors with the same number of arguments as the length of <code>arguments</code>
     * are tried. If there are several constructors with the same number of arguments, the first (in the order
     * of the list) constructor that succeeds in converting string arguments will be used.
     * </p>
     * <p>
     * Note also that only types convertible by {@link Convert#stringToType} method can be used in constructors.
     * </p>
     *
     * @param <E> the type of the instantiated object
     * @param constructors the list of constructors of the desired class to try
     * @param arguments the string arguments for the constructor that will be converted to correct types
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @return a new instance of the class the constructors were specified for
     * @throws InvocationTargetException
     *              if the constructor can't be found for the specified arguments,
     *              the argument string-to-type convertion has failed or
     *              there was an error during instantiation
     */
    public static <E> E createInstanceWithStringArgs(List<Constructor<E>> constructors, String[] arguments, Map<String, Object> namedInstances) throws InvocationTargetException {
        return createInstanceWithStringArgs(constructors, arguments, 0, namedInstances);
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
     * @throws ClassNotFoundException if the class in the constructor signature was not found or is not a descendant of checkClass
     */
    public static <E> E createInstanceWithStringArgs(String signature, Class<E> checkClass, Map<String, Object> namedInstances) throws InvocationTargetException, ClassNotFoundException {
        // Parse arguments (enclosed in braces)
        String[] args;
        String callname;
        int openParenthesisPos = signature.indexOf('(');
        int closeParenthesisPos = signature.lastIndexOf(')');
        if (openParenthesisPos == -1 || openParenthesisPos == closeParenthesisPos - 1) { // There are no parenthesis or they are empty
            args = new String[0];
            callname = signature;
        } else if (closeParenthesisPos == -1) {
            throw new IllegalArgumentException("Missing closing parenthesis: " + signature);
        } else {
            args = signature.substring(openParenthesisPos + 1, closeParenthesisPos).split("\\s*,\\s*");
            callname = signature.substring(0, openParenthesisPos);
        }

        // Create instance
        try {
            // Try call name as a constructor
            Class<E> clazz = Convert.getClassForName(callname, checkClass);
            return createInstanceWithStringArgs(Arrays.asList(Convert.getConstructors(clazz)), args, namedInstances);
        } catch (ClassNotFoundException e) {
            // Class not found, try dot earlier
            int dotPos = callname.lastIndexOf('.');
            if (dotPos == -1)
                throw e;

            // Class without last item, which is supposed to be a factory method name or a field name
            Class<?> clazz = Class.forName(callname.substring(0, dotPos));
            callname = callname.substring(dotPos + 1);

            try {
                // We have correct class, now check if it is method or attribute
                return (openParenthesisPos == -1)?
                    checkClass.cast(createInstanceStaticField(clazz, callname)):
                    createInstanceUsingFactoryMethod(clazz, callname, checkClass, args);
            } catch (NoSuchMethodException ex) {
                throw new InvocationTargetException(ex);
            }
        }
    }

    /**
     * Creates a new instance of a class.
     * First, a constructor for the specified arguments is searched in the provided class.
     * Then, an instance is created and returned.
     *
     * @param <E> the type of the instantiated object
     * @param instanceClass the class for which to create an instance
     * @param arguments the arguments for the constructor
     * @return a new instance of the class
     * @throws NoSuchMethodException if there was no constructor for the specified list of arguments
     * @throws InvocationTargetException if there was an exception during instantiation
     */
    public static <E> E createInstanceWithInheritableArgs(Class<E> instanceClass, Object... arguments) throws NoSuchMethodException, InvocationTargetException {
        try {
            return getConstructor(instanceClass, false, arguments).newInstance(arguments);
        } catch (IllegalAccessException e) {
            throw new NoSuchMethodException(e.getMessage());
        } catch (InstantiationException e) {
            throw new NoSuchMethodException(e.getMessage());
        }
    }

    /**
     * Creates a new instance of a class.
     * First, a factory method for the specified arguments is searched in the provided class and its ancestors.
     * Then, an instance is created and returned.
     *
     * @param <E> the type of the instantiated object
     * @param instanceClass the class for which to create an instance
     * @param methodName the name of the factory method
     * @param arguments the arguments for the factory method
     * @return a new instance of the class
     * @throws NoSuchMethodException if there was no factory method for the specified list of arguments
     * @throws InvocationTargetException if there was an exception during instantiation
     */
    @SuppressWarnings("unchecked")
    public static <E> E createInstanceUsingFactoryMethod(Class<E> instanceClass, String methodName, Object... arguments) throws NoSuchMethodException, InvocationTargetException {
        try {
            Method factoryMethod = getMethod(instanceClass, methodName, false, arguments);
            if (!Modifier.isStatic(factoryMethod.getModifiers()))
                throw new IllegalArgumentException("Factory method " + factoryMethod + " is required to be static");
            if (!instanceClass.isAssignableFrom(factoryMethod.getReturnType()))
                throw new IllegalArgumentException("Factory method " + factoryMethod + " is required to return " + instanceClass);
            return (E)factoryMethod.invoke(null, arguments); // This cast IS checked on the previous line
        } catch (IllegalAccessException e) {
            throw new NoSuchMethodException(e.getMessage());
        }
    }

    /**
     * Creates a new instance of a class.
     * First, a factory method for the specified arguments is searched in the provided class and its ancestors.
     * Then, string arguments are converted to proper types for the method that was found.
     * Finally, an instance is created and returned.
     *
     * @param <E> the type of the instantiated object
     * @param declaringClass the class where the factory method is declared
     * @param methodName the name of the factory method
     * @param instanceClass the class for which to create an instance
     * @param arguments the arguments for the factory method
     * @return a new instance of the class
     * @throws NoSuchMethodException if there was no factory method for the specified list of arguments
     * @throws InvocationTargetException if there was an exception during instantiation
     */
    @SuppressWarnings("unchecked")
    public static <E> E createInstanceUsingFactoryMethod(Class<?> declaringClass, String methodName, Class<E> instanceClass, String... arguments) throws NoSuchMethodException, InvocationTargetException {
        try {
            // Convert string arguments to object array
            Object[] args = new Object[arguments.length];
            System.arraycopy(arguments, 0, args, 0, arguments.length);
            Method factoryMethod = getMethod(declaringClass, methodName, true, args);
            if (!Modifier.isStatic(factoryMethod.getModifiers()))
                throw new IllegalArgumentException("Factory method " + factoryMethod + " is required to be static");
            if (!instanceClass.isAssignableFrom(factoryMethod.getReturnType()))
                throw new IllegalArgumentException("Factory method " + factoryMethod + " is required to return " + instanceClass);
            return (E)factoryMethod.invoke(null, args); // This cast IS checked on the previous line
        } catch (IllegalAccessException e) {
            throw new NoSuchMethodException(e.getMessage());
        }
    }

    /**
     * Get instance from a static field of a class.
     *
     * @param clazz the class the field of which to access
     * @param name the name of the <code>clazz</code>'s static field
     * @return a new instance of the class
     * @throws IllegalArgumentException if there was no public static field for the specified <code>clazz</code> and <code>name</code>
     */
    @SuppressWarnings("unchecked")
    public static Object createInstanceStaticField(Class<?> clazz, String name) throws IllegalArgumentException {
        try {
            Field field = clazz.getField(name);
            if (!Modifier.isStatic(field.getModifiers()))
                throw new IllegalArgumentException("Field '" + name + "' in " + clazz + " is not static");
            return field.get(null);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Field '" + name + "' was not found in " + clazz);
        } catch (IllegalAccessException e) { // This should never happen
            throw new IllegalArgumentException("Field '" + name + "' in " + clazz + " is not accessible");
        }
    }

    /**
     * Returns a constructor for the specified class that accepts the specified arguments.
     * The <code>clazz</code>'s declared constructors are searched for the one that
     * accepts the arguments.
     * If the <code>convertStringArguments</code> is specified, the
     * <code>arguments</code> elements are replaced with the converted types
     * if and only if a proper constructor is found. Their types then will be
     * compatible with the constructor.
     *
     * @param <E> the class the constructor will create
     * @param clazz the class for which to get the constructor
     * @param convertStringArguments if <tt>true</tt> the string values from the arguments are converted using {@link Convert#stringToType}
     * @param arguments the arguments for the constructor
     * @return a constructor for the specified class
     * @throws NoSuchMethodException if there was no constructor for the specified list of arguments
     */
    @SuppressWarnings("unchecked")
    public static <E> Constructor<E> getConstructor(Class<E> clazz, boolean convertStringArguments, Object[] arguments) throws NoSuchMethodException {
        for (Constructor<E> constructor : (Constructor<E>[])clazz.getDeclaredConstructors()) {
            if (isPrototypeMatching(constructor.getParameterTypes(), arguments, convertStringArguments))
                return constructor;
        }

        // Constructor not found, prepare error
        StringBuilder str = new StringBuilder("There is no constructor ");
        str.append(clazz.getName()).append('(');
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0)
                str.append(", ");
            if (arguments[i] == null)
                str.append("null");
            else
                str.append(arguments[i].getClass().getName());
        }
        str.append(')');
        throw new NoSuchMethodException(str.toString());
    }

    /**
     * Returns a method for the specified class that accepts the specified arguments.
     * The <code>clazz</code>'s declared methods are searched for the one that
     * accepts the arguments.
     * If the <code>convertStringArguments</code> is specified, the
     * <code>arguments</code> elements are replaced with the converted types
     * if and only if a proper method is found. Their types then will be
     * compatible with the method.
     *
     * @param clazz the class for which to get the method
     * @param methodName the name of the method to get
     * @param convertStringArguments if <tt>true</tt> the string values from the arguments are converted using {@link Convert#stringToType}
     * @param arguments the arguments for the method
     * @return a method of the specified class
     * @throws NoSuchMethodException if there was no method with the specified name and arguments
     */
    @SuppressWarnings("unchecked")
    public static Method getMethod(Class<?> clazz, String methodName, boolean convertStringArguments, Object[] arguments) throws NoSuchMethodException {
        if (clazz == null || methodName == null)
            throw new NoSuchMethodException("There is not method '" + methodName + "' that accepts " + Arrays.toString(arguments));

        // Search all methods of the execution object and register the matching ones
        for (Method method : clazz.getDeclaredMethods()) {
            // Skip methods with different name if methodNames parameter was specified
            if (!methodName.equals(method.getName()))
                continue;

            // Check prototype
            if (isPrototypeMatching(method.getParameterTypes(), arguments, convertStringArguments))
                return method;
        }

        // Recurse to superclass
        return getMethod(clazz.getSuperclass(), methodName, convertStringArguments, arguments);
    }

    /**
     * Test argument array, if it is compatible with the provided prototype.
     * That is, the number of arguments must be equal and each argument
     * (an item from the <code>methodTypes</code>) must be assignable
     * from the respective <code>methodPrototype</code> item.
     *
     * @param methodTypes the tested arguments array
     * @param methodPrototype the prototype arguments array
     * @param skipIndex the index of an argument that is not checked for the compatibility condition
     * @return <tt>true</tt> if the method types are compatible with the prototype
     */
    public static boolean isPrototypeMatching(Class<?>[] methodTypes, Class<?>[] methodPrototype, int skipIndex) {
        // Not enough arguments
        if (methodTypes.length != methodPrototype.length) return false;

        // Test arguments
        for (int i = 0; i < methodTypes.length; i++) {
            if (i == skipIndex) continue;

            // If the method type is primitive type, check names
            if (methodTypes[i].isPrimitive() && methodPrototype[i].getName().toLowerCase().startsWith("java.lang." + methodTypes[i].getSimpleName()))
                continue;

            // The argument of the method must be the same as or a superclass of the provided prototype class
            if (!methodTypes[i].isAssignableFrom(methodPrototype[i]))
                return false;
        }

        return true;
    }

    /**
     * Test argument array, if it is compatible with the provided prototype.
     * That is, each item from the <code>methodTypes</code> must be assignable
     * from the respective <code>methodPrototype</code> item.
     *
     * @param methodTypes the tested arguments array
     * @param methodPrototype the prototype arguments array
     * @return <tt>true</tt> if the method types are compatible with the prototype
     */
    public static boolean isPrototypeMatching(Class<?>[] methodTypes, Class<?>[] methodPrototype) {
        return isPrototypeMatching(methodTypes, methodPrototype, -1);
    }

    /**
     * Test argument array, if it is compatible with the provided prototype.
     * That is, the number of arguments must be equal and each argument
     * must be assignable to the respective <code>prototype</code> item.
     * If the <code>convertStringArguments</code> is specified, the
     * <code>arguments</code> elements are replaced with the converted types
     * if and only if the method returns <tt>true</tt>.
     *
     * @param prototype the tested prototype
     * @param arguments the tested arguments
     * @param convertStringArguments if <tt>true</tt> the string values from the arguments are converted using {@link Convert#stringToType}
     * @return <tt>true</tt> if the arguments are compatible with the prototype
     */
    public static boolean isPrototypeMatching(Class<?>[] prototype, Object[] arguments, boolean convertStringArguments) {
        // Not enough arguments
        if (prototype.length != arguments.length)
            return false;

        // Array for the converted values (since they must not be modified if the method return false)
        Object[] convertedArguments = null;

        // Test arguments
        for (int i = 0; i < prototype.length; i++) {
            // Null value is accepted by any class
            if (arguments[i] == null && !prototype[i].isPrimitive())
                continue;
            // The argument of the method must be the same as or a superclass of the provided prototype class
            if (!Convert.wrapPrimitiveType(prototype[i]).isInstance(arguments[i])) {
                if (!convertStringArguments || !(arguments[i] instanceof String))
                    return false;
                // Try to convert string argument
                try {
                    // Clone argument array if not clonned yet
                    if (convertedArguments == null)
                        convertedArguments = arguments.clone();
                    convertedArguments[i] = Convert.stringToType((String)arguments[i], prototype[i]);
                } catch (InstantiationException ignore) {
                    return false;
                }
            }
        }

        // Move converted arguments
        if (convertedArguments != null)
            System.arraycopy(convertedArguments, 0, arguments, 0, arguments.length);

        return true;
    }
}
