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

import messif.utility.Convert;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class SingleMethodExecutor implements Executable {

    /** Method to call before/after execution */
    protected final Method method;

    /** Object on which invoke the prepare/finalize method */
    protected final Object object;

    /** Arguments for prepare/finalize method */
    protected final Object[] arguments;

    /** Returned value from the executed method */
    protected Object returnedValue;

    /** Creates a new instance of SingleMethodExecutor */
    public SingleMethodExecutor(Method method, Object object, Object[] arguments) {
        this.method = method;
        this.method.setAccessible(true);
        this.object = object;
        this.arguments = arguments;
    }

    /** Creates a new instance of SingleMethodExecutor */
    public SingleMethodExecutor(Method method, Object[] possibleObjects, Object[] arguments) throws NoSuchElementException {
        this(method, chooseExecutionObject(method, possibleObjects), arguments);
    }

    /** Creates a new instance of SingleMethodExecutor */
    public SingleMethodExecutor(Object object, String methodName, Object... arguments) throws NoSuchMethodException {
        this(getDeclaredMethod(object.getClass(), methodName, Convert.getObjectTypes(arguments)), object, arguments);
    }

    /****************** Search methods ******************/

    public static Object chooseExecutionObject(Method method, Object[] objects) throws NoSuchElementException {
        Class<?> methodClass = method.getDeclaringClass();
        for (Object obj : objects)
            if (methodClass.isAssignableFrom(obj.getClass()))
                return obj;

        throw new NoSuchElementException("No object for method '" + method + "' found in " + Arrays.toString(objects));
    }

    public static Method getDeclaredMethod(Class<?> classType, String name, Class[] parameterTypes) throws NoSuchMethodException {
        try {
            return classType.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            try {
                return getDeclaredMethod(classType.getSuperclass(), name, parameterTypes);
            } catch (NoSuchMethodException ignore) {
            } catch (NullPointerException ignore) {}
            throw e;
        }
    }


    /****************** Data access ******************/

    /** Get the number of executed arguments */
    public int getArgumentCount() {
        return arguments.length;
    }
    
    /** Get executed argument */
    public Object getArgument(int index) {
        return arguments[index];
    }
    
    /**
     * Returns executed argument that has a specified class.
     * @param argClass a distinguishing class of the argument
     * @return executed argument
     * @throws NoSuchElementException if a parameter with the argClass class was not found
     */
    public <E> E getArgument(Class<E> argClass) throws NoSuchElementException {
        for (Object argument:arguments)
            if (argClass.isAssignableFrom(argument.getClass()))
                return argClass.cast(argument);
        
        throw new NoSuchElementException("Parameter with class " + argClass + " was not found");
    }
    
    /** Get array of all executed arguments */
    public Object[] getArguments() {
        return arguments;
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
    public void execute() throws NoSuchMethodException, InvocationTargetException {
        returnedValue = MethodExecutor.execute(method, object, arguments);
    }

}