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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.NoSuchElementException;
import messif.utility.Convert;

/**
 * A single {@link Executable} method.
 * Invokes a single method on given object when {@link #execute() executed}.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class SingleMethodExecutor implements Executable {

    //****************** Attributes ******************//

    /** Method to call before/after execution */
    protected final Method method;

    /** Object on which invoke the prepare/finalize method */
    protected final Object object;

    /** Arguments for prepare/finalize method */
    protected final Object[] arguments;

    /** Returned value from the executed method */
    protected Object returnedValue;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of SingleMethodExecutor.
     * @param method the method to execute
     * @param object the instance on which to execute the method
     * @param arguments the arguments for the method
     */
    public SingleMethodExecutor(Method method, Object object, Object[] arguments) {
        this.method = method;
        this.method.setAccessible(true);
        this.object = object;
        this.arguments = arguments;
    }

    /**
     * Creates a new instance of SingleMethodExecutor.
     * @param method the method to execute
     * @param possibleObjects the list of instances on which to execute the method
     *          (the correct one is selected using {@link #chooseExecutionObject(java.lang.reflect.Method, java.lang.Object[])})
     * @param arguments the arguments for the method
     * @throws NoSuchMethodException if there was no instance compatible with the given method
     */
    public SingleMethodExecutor(Method method, Object[] possibleObjects, Object[] arguments) throws NoSuchMethodException {
        this(method, chooseExecutionObject(method, possibleObjects), arguments);
    }

    /**
     * Creates a new instance of SingleMethodExecutor.
     * @param object the instance on which to execute the method
     * @param methodName the name of the method to execute
     * @param arguments the arguments for the method
     * @throws NoSuchMethodException if there was no method compatible with the given parameters
     */
    public SingleMethodExecutor(Object object, String methodName, Object... arguments) throws NoSuchMethodException {
        this(getDeclaredMethod(object.getClass(), methodName, Convert.getObjectTypes(arguments)), object, arguments);
    }


    //****************** Search methods ******************//

    /**
     * Searches a list of instances for an instance that is compatible with
     * the given method. That is, the first object that is instance of the class
     * that declared the method is returned.
     *
     * @param method the method for which to search a compatible instance
     * @param objects the list of instances to search
     * @return a compatible instance from the list
     * @throws NoSuchMethodException if there was no instance compatible with the given method
     */
    public static Object chooseExecutionObject(Method method, Object[] objects) throws NoSuchMethodException {
        Class<?> methodClass = method.getDeclaringClass();
        for (Object obj : objects)
            if (methodClass.isInstance(obj))
                return obj;

        throw new NoSuchMethodException("No object for method '" + method + "' found in " + Arrays.toString(objects));
    }

    /**
     * Returns a declared method (i.e. public, protected or private) that matches
     * the given name and parameter types.
     * @param classType the class in which to start the search
     * @param name the name of the method
     * @param parameterTypes the types of parameters of the method
     * @return the method that matches the parameters
     * @throws NoSuchMethodException if there was no method that matches the given parameters
     */
    public static Method getDeclaredMethod(Class<?> classType, String name, Class[] parameterTypes) throws NoSuchMethodException {
        try {
            return classType.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            try {
                return getDeclaredMethod(classType.getSuperclass(), name, parameterTypes);
            } catch (NoSuchMethodException ignore) {
            } catch (NullPointerException ignore) {
            }
            throw e;
        }
    }


    /****************** Data access ******************/

    /**
     * Returns the number of executed arguments.
     * @return the number of executed arguments
     */
    public int getArgumentCount() {
        return arguments.length;
    }
    
    /**
     * Returns the index-th argument of the executed method.
     * @param index the index (zero-based) of the argument to return
     * @return the argument of the executed method
     */
    public Object getArgument(int index) {
        return arguments[index];
    }
    
    /**
     * Returns the array of all arguments of the executed method.
     * @return the array of all arguments of the executed method
     */
    public Object[] getArguments() {
        return arguments.clone();
    }

    /**
     * Returns executed argument that has a specified class.
     * @param <E> the distinguishing class of the argument
     * @param argClass the distinguishing class of the argument
     * @return executed argument
     * @throws NoSuchElementException if a parameter with the argClass class was not found
     */
    public <E> E getArgument(Class<E> argClass) throws NoSuchElementException {
        for (Object argument:arguments)
            if (argClass.isInstance(argument))
                return argClass.cast(argument);
        
        throw new NoSuchElementException("Parameter with class " + argClass + " was not found");
    }
    
    /**
     * Returns value returned by the executed method.
     * @return value returned by the executed method
     */
    public Object getReturnedValue() {
        return returnedValue;
    }


    /****************** Execution method ******************/

    /** Invoke the method represented by this object */
    @Override
    public void execute() throws NoSuchMethodException, InvocationTargetException {
        returnedValue = MethodExecutor.execute(method, object, arguments);
    }

}