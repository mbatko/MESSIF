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
package messif.executor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import messif.utility.reflection.Instantiators;



/**
 *  This class allows to execute a methods on a specified object.
 *  First, methods must be registered. This is done through constructor, where 
 *  also an instance of the target object must be provided.
 *  The second parameter specify the required argument types
 *  that the method must have. One argument is specified as a super class of
 *  the actual parameter (this argument class is then used to distinguish
 *  between the executed methods), the other arguments must be either of the same class
 *  as the method's one or its subclasses. This item is marked by the third
 *  constructor argument. 
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
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class MethodClassExecutor extends MethodExecutor {
    
    /** The table of found operation methods */
    protected transient Map<Class<?>, Method> registeredMethods = Collections.synchronizedMap(new HashMap<Class<?>, Method>());
  
    /** Index of the argument in the method prototype, which is used to distinguish between the methods */
    protected final int differentiateByArgNo;

    /****************** Constructors ******************/
    
    /**
     * Create new instance of MethodClassExecutor and search for operation methods 
     * 
     * @param executionObject an instance of the object to execute the operations on
     * @param differentiateByArgNo the index of an argument from methodPrototype, which will destinguish the methods
     * @param methodNames the name which all the methods must match
     * @param modifiers specify or'ed values of {@link java.lang.reflect.Modifier} that the method must have set
     * @param inheritedMethods if <tt>null</tt> only methods declared in the <code>executionObject</code> are loaded.
     *        Otherwise, methods from all superclasses up to class <code>inheritedMethods</code> exclusive are loaded.
     * @param methodPrototype list of argument types for the registered methods
     * @throws IllegalArgumentException if either the method prototype or differentiating argument index is invalid or the executionObject is <tt>null</tt>
     */
    public MethodClassExecutor(Object executionObject, int differentiateByArgNo, String methodNames, int modifiers, Class<?> inheritedMethods, Class<?>... methodPrototype) throws IllegalArgumentException {
        super(executionObject);
        
        // The method prototype must have at least one argument
        if (methodPrototype == null)
            throw new IllegalArgumentException("Method prototype must be specified.");
        
        if (differentiateByArgNo < 0 || differentiateByArgNo >= methodPrototype.length)
            throw new IllegalArgumentException("Specified differentiator argument index is invalid.");
        
        // Store local parameters
        this.differentiateByArgNo = differentiateByArgNo;

        // Get the class of the execution object
        Class<?> executionClass;
        if (executionObject instanceof Class) {
            // Execution object is class, so only static methods are called
            inheritedMethods = null;
            modifiers = (modifiers & accessModifierMask) | Modifier.STATIC;
            executionClass = (Class)executionObject;
        } else executionClass = executionObject.getClass();

        // Search all methods of the execution object and register the matching ones
        for (Method method : getClassMethods(executionClass, modifiers, inheritedMethods)) {
            // Skip methods with different name if methodNames parameter was specified
            if (methodNames != null && !methodNames.equals(method.getName()))
                continue;
            
            // Check prototype and add method to the registry
            Class<?>[] methodArgTypes = method.getParameterTypes();
            if (Instantiators.isPrototypeMatching(methodArgTypes, methodPrototype, differentiateByArgNo)) {
                // Prototypes are matching in all except the differentiate index, which must be subclass of the class specified in the prototype
                if (methodPrototype[differentiateByArgNo].isAssignableFrom(methodArgTypes[differentiateByArgNo]) && !registeredMethods.containsKey(methodArgTypes[differentiateByArgNo]))
                    registeredMethods.put(methodArgTypes[differentiateByArgNo], method);
            }
        }
    }

    /**
     * Create new instance of MethodClassExecutor and search for operation methods 
     * Public methods from the whole object's hierarchy are executed.
     * 
     * @param executionObject an instance of the object to execute the operations on
     * @param differentiateByArgNo the index of an argument from methodPrototype, which will destinguish the methods
     * @param methodNames the name which all the methods must match
     * @param methodPrototype list of argument types for the registered methods
     * @throws IllegalArgumentException if the differentiating argument index is invalid or the executionObject is <tt>null</tt>
     */
    public MethodClassExecutor(Object executionObject, int differentiateByArgNo, String methodNames, Class<?>... methodPrototype) throws IllegalArgumentException {
        this(executionObject, differentiateByArgNo, methodNames, Modifier.PUBLIC, Object.class, methodPrototype);
    }

    /**
     * Create new instance of MethodExecutor and search for operation methods.
     * The first argument is selected as the differentiator.
     * Public methods from the whole object's hierarchy are executed.
     * 
     * @param executionObject an instance of the object to execute the operations on
     * @param inheritedMethods if <tt>null</tt> only methods declared in the <code>executionObject</code> are loaded.
     *        Otherwise, methods from all superclasses up to class <code>inheritedMethods</code> exclusive are loaded.
     * @param methodPrototype list of argument types for the registered methods
     * @throws IllegalArgumentException if the method prototype has no arguments or the executionObject is <tt>null</tt>
     */
    public MethodClassExecutor(Object executionObject, Class<?> inheritedMethods, Class<?>... methodPrototype) throws IllegalArgumentException {
        this(executionObject, 0, null, Modifier.PUBLIC, inheritedMethods, methodPrototype);
    }

    /**
     * Create new instance of MethodExecutor and search for operation methods.
     * The first argument is selected as the differentiator.
     * Public methods from the whole object's hierarchy are executed.
     * 
     * @param executionObject an instance of the object to execute the operations on
     * @param methodPrototype list of argument types for the registered methods
     * @throws IllegalArgumentException if the method prototype has no arguments or the executionObject is <tt>null</tt>
     */
    public MethodClassExecutor(Object executionObject, Class<?>... methodPrototype) throws IllegalArgumentException {
        this(executionObject, 0, null, methodPrototype);
    }    

    /**
     * Create new instance of MethodExecutor and search for operation methods.
     * The first argument is selected as the differentiator.
     * Public methods from the whole object's hierarchy are executed.
     * 
     * @param executionObject an instance of the object to execute the operations on
     * @param differentiateByArgNo the index of an argument from methodPrototype, which will destinguish the methods
     * @param methodPrototype list of argument types for the registered methods
     * @throws IllegalArgumentException if the differentiating argument index is invalid or the executionObject is <tt>null</tt>
     */
    public MethodClassExecutor(Object executionObject, int differentiateByArgNo, Class<?>... methodPrototype) throws IllegalArgumentException {
        this(executionObject, differentiateByArgNo, null, methodPrototype);
    }
    
    /**
     * Create new instance of MethodExecutor and search for operation methods.
     * The first argument is selected as the differentiator.
     * Public methods from the whole object's hierarchy are executed.
     * 
     * @param executionObject an instance of the object to execute the operations on
     * @param methodNames the name which all the methods must match
     * @param methodPrototype list of argument types for the registered methods
     * @throws IllegalArgumentException if the method prototype has no arguments or the executionObject is <tt>null</tt>
     */
    public MethodClassExecutor(Object executionObject, String methodNames, Class<?>... methodPrototype) throws IllegalArgumentException {
        this(executionObject, 0, methodNames, methodPrototype);
    }
    
    
    /****************** Data access ******************/

    /** The all access modifiers mask - or-ed values of all of them */
    private static final int accessModifierMask = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE;

    /**
     * Returns all methods from the specified class that have the modifiers.
     * @param readClass the class for which the methods are read
     * @param modifiers or'ed together (see {@link java.lang.reflect.Modifier} for their list). All set modifiers must be present
     *        for a specific method except for the access modifiers. For example, value
     *        <code>
     *          {@link java.lang.reflect.Modifier#PROTECTED} | {@link java.lang.reflect.Modifier#PUBLIC} |
     *          {@link java.lang.reflect.Modifier#TRANSIENT} | {@link java.lang.reflect.Modifier#FINAL}
     *        </code>
     *        matches all <b>final transient</b> methods that are either <b>public</b> or <b>protected</b>.
     * @param readSuperclass methods from all subclasses (up to <code>readSuperclass</code>) are also returned, otherwise, only
     *        methods declared in the <code>readClass</code> are checked
     * @return all methods from the specified class that have the modifiers
     */
    protected static Collection<Method> getClassMethods(Class<?> readClass, int modifiers, Class<?> readSuperclass) {
        Collection<Method> methodList = new ArrayList<Method>();
        int accessModifiers = modifiers & accessModifierMask;
        modifiers &= ~accessModifierMask;

        do {
            for (Method method : readClass.getDeclaredMethods())
                if ((method.getModifiers() & modifiers) == modifiers && ((method.getModifiers() & accessModifiers) != 0)) {
                    method.setAccessible(true);
                    methodList.add(method);
                }
            readClass = readClass.getSuperclass();
        } while (readSuperclass != null && readClass != null && !readSuperclass.equals(readClass));

        return methodList;
    }

    /**
     * Returns the stored method according to the the specified class.
     * @param key the differentiating class from method prototype
     * @param trySuperClasses check all superclasses of the <tt>key<tt> if a direct match was not found
     * @return the stored method according to the the specified class
     */
    protected Method getMethod(Class<?> key, boolean trySuperClasses) {
        Method rtv = registeredMethods.get(key);
        if (trySuperClasses)
            while (rtv == null && key != null) {
                key = key.getSuperclass();
                rtv = registeredMethods.get(key);
            }
            
        return rtv;
    }
    
    /**
     * Returns a proper execution method for provided arguments.
     * The key for the search is the class of the <tt>differentiateByArgNo</tt>-th object from the <tt>arguments</tt> parameter.
     *
     * @param arguments the arguments of the execution method we are looking for
     * @return proper execution method for provided arguments
     * @throws NoSuchMethodException if there was no method for the specified arguments
     */
    @Override
    protected Method getMethod(Object[] arguments) throws NoSuchMethodException {
        try {
            Class diffClass = arguments[differentiateByArgNo].getClass();
            Method rtv = getMethod(diffClass, true);
            if (rtv == null)
                throw new NoSuchMethodException("Method for '" + diffClass.getName() + "' was not found");
            return rtv;
        } catch (IndexOutOfBoundsException e) {
            throw new NoSuchMethodException("Invalid argument list");
        } catch (NullPointerException e) {
            throw new NoSuchMethodException("Invalid argument list");
        }
    }

    /**
     * Returns the list of classes that this executor recognizes and can execute their associated method.
     * This method is typically used to return the list of supported arguments.
     * Only the specified subclasses are returned.
     *
     * @param <E> the super-class of the returned classes
     * @param subclassesToSearch a filter the list to contain only the subclasses of this parameter
     * @param modifiers or'ed together (see {@link java.lang.reflect.Modifier} for their list). All set modifiers must be present
     *        for a specific method except for the access modifiers. For example, value
     *        <code>
     *          {@link java.lang.reflect.Modifier#PROTECTED} | {@link java.lang.reflect.Modifier#PUBLIC} |
     *          {@link java.lang.reflect.Modifier#TRANSIENT} | {@link java.lang.reflect.Modifier#FINAL}
     *        </code>
     *        matches all <b>final transient</b> methods that are either <b>public</b> or <b>protected</b>.
     * @return the list of classes that this executor recognizes
     */
    public <E> List<Class<? extends E>> getDifferentiatingClasses(Class<? extends E> subclassesToSearch, int modifiers) {
        int accessModifiers = modifiers & accessModifierMask;
        modifiers &= ~accessModifierMask;
        List<Class<? extends E>> rtv = new ArrayList<Class<? extends E>>();
        for (Map.Entry<Class<?>, Method> entry : registeredMethods.entrySet())
            if ((entry.getValue().getModifiers() & modifiers) == modifiers && ((entry.getValue().getModifiers() & accessModifiers) != 0)) {
                if (subclassesToSearch.isAssignableFrom(entry.getKey())) {
                    @SuppressWarnings("unchecked")
                    Class<? extends E> val = (Class<? extends E>)entry.getKey(); // This cast IS checked on the previous line
                    rtv.add(val);
                }
            }
        return rtv;
    }

    /**
     * Returns the list of classes that this executor recognizes and can execute their associated method.
     * This method is typically used to return the list of supported arguments.
     * Only the specified subclasses that have public method are returned.
     *
     * @param <E> the super-class of the returned classes
     * @param subclassesToSearch a filter the list to contain only the subclasses of this parameter
     * @return the list of classes that this executor recognizes
     */
    public <E> List<Class<? extends E>> getDifferentiatingClasses(Class<? extends E> subclassesToSearch) {
        return getDifferentiatingClasses(subclassesToSearch, Modifier.PUBLIC);
    }

    /**
     * Returns the list of classes that this executor recognizes and can execute their associated method.
     * This method is typically used to return the list of supported arguments.
     * @return the list of classes that this executor recognizes
     */
    @Override
    protected Collection<Method> getRegisteredMethods() {
        return Collections.unmodifiableCollection(registeredMethods.values());
    }    

}