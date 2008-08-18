/*
 * MethodNameExecutor.java
 *
 * Created on 20. unor 2005, 10:12
 */

package messif.executor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import messif.utility.Convert;


/**
 *  This class allows to execute a methods on a specified object.
 *  First, methods must be registered. This is done through constructor, where 
 *  also an instance of the target object must be provided.
 *  The second parameter specify the required argument types
 *  that the method must have.
 *
 *  Then the method execute method can be called. This method invokes the method
 *  of the instance (provided in constructor), which is appropriate for the provided
 *  arguments.
 *
 *  Method backgroundExecute can be called to invoke the method in
 *  a new thread. A returned thread can be used for wait calls
 *  to test, whether the execution has finished and also to retrieve the data.
 *
 *
 * @author  xbatko
 */
public class MethodNameExecutor extends MethodExecutor {
    
    /** The table of found operation methods */
    protected final Map<String, Method> registeredMethods = Collections.synchronizedMap(new HashMap<String, Method>());
    
    /** Index of an argument from methodPrototype, which will hold the method name */
    protected final int nameArgIndex;
    
    /****************** Constructors ******************/
    
    /**
     * Create new instance of MethodNameExecutor and search for operation methods 
     * 
     * @param executionObject an instance of the object to execute the operations on
     * @param methodPrototype list of argument types for the registered methods
     * @param nameArgIndex the index of an argument from methodPrototype, which will hold the method name
     * @throws IllegalArgumentException if either the method prototype or named argument index is invalid or the executionObject is <tt>null</tt>
     */
    public MethodNameExecutor(Object executionObject, int nameArgIndex, Class<?>... methodPrototype) throws IllegalArgumentException {
        super(executionObject);
        
        // The method prototype must have at least one argument
        if (methodPrototype == null || methodPrototype.length == 0)
            throw new IllegalArgumentException("Method prototype must be specified.");

        // Validate the nameArgIndex argument
        if ((nameArgIndex < 0) && (nameArgIndex >= methodPrototype.length && methodPrototype[methodPrototype.length - 1].isArray()))
            throw new IllegalArgumentException("Index of method name argument is out of bounds");
        this.nameArgIndex = nameArgIndex;
        
        // Search all methods of the execution object and register the matching ones
        boolean isExecutionClass = executionObject instanceof Class;
        for (Method method : (isExecutionClass?(Class)executionObject:executionObject.getClass()).getDeclaredMethods()) {
            // Skip non static members on execution classes
            if (isExecutionClass && !Modifier.isStatic(method.getModifiers()))
                continue;
            
            // Check prototype and add method to the registry
            Class<?>[] methodArgTypes = method.getParameterTypes();
            if (Convert.isPrototypeMatching(methodArgTypes, methodPrototype))
                registeredMethods.put(method.getName(), method);
        }
    }

    public MethodNameExecutor(Object executionObject, Class<?>... methodPrototype) throws IllegalArgumentException {
        this(executionObject, getFirstStringClass(methodPrototype), methodPrototype);
    }


    /****************** String argument searching ******************/

    /** Search array for first String class. String[] class is allowed as the last item. */
    public static int getFirstStringClass(Class<?>[] array) {
        for (int i = 0; i < array.length; i++)
            if (String.class.equals(array[i]))
                return i;
        if (array.length > 0 && String[].class.equals(array[array.length - 1]))
            return array.length - 1;
        
        return -1;
    }

    /** Get string from array at specified position.
     *  @throws ClassCastException if the array item at the specified position is not a string
     *  @throws IndexOutOfBoundsExeption if the specified position is invalid
     */
    public static String getStringObject(Object[] array, int index) {
        // Index is last or beyond end and the last argument is array
        if (index >= array.length - 1 && array[array.length - 1].getClass().isArray())
            return ((String[])array[array.length - 1])[index - array.length + 1];
        
        // Index is classical
        return (String)array[index];
    }


    /****************** Implementation of necessary methods ******************/

    protected Method getMethod(Object[] arguments) throws NoSuchMethodException {
        Method method = null;
        try {
            method = registeredMethods.get(getStringObject(arguments, nameArgIndex));
        } catch (ClassCastException ignore) {
        } catch (IndexOutOfBoundsException ignore) {
        }
        if (method == null)
            throw new NoSuchMethodException("Method for specified arguments not found");

        return method;
    }
    
    /** Returns the list of method names that this executor supports, that match a specified regular expression */
    public List<String> getDifferentiatingNames(String regexp) {
        List<String> rtv = new ArrayList<String>();
        for (String key : registeredMethods.keySet())
            if (key.matches(regexp))
                rtv.add(key);
        return rtv;
    }

    /** Return all methods that are registered within this executor */
    protected Collection<Method> getRegisteredMethods() {
        return Collections.unmodifiableCollection(registeredMethods.values());
    }

}