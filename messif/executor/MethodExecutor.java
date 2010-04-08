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

import java.io.PrintStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;





/**
 *  This class is a generic framework for executing methods on a specified object.
 *  Methods must match the specified prototype.
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
public abstract class MethodExecutor {

    //****************** Attributes ******************//

    /** The object that the operations are invoked on */
    protected final Object executionObject;


    //****************** Constructors ******************//

    /**
     * Create new instance of MethodExecutor
     * 
     * @param executionObject an instance of the object to execute the operations on
     * @throws IllegalArgumentException if the execution object is <tt>null</tt>
     */
    protected MethodExecutor(Object executionObject) throws IllegalArgumentException {
        // The instance of execution object can't be null
        if (executionObject == null)
            throw new IllegalArgumentException("Execution class or object instance must be specified.");
        
        // Store local parameters
        this.executionObject = executionObject;
    }


    //****************** Internal methods ******************//

    /**
     * Returns all methods that are registered within this executor.
     * @return all methods that are registered within this executor
     */
    protected abstract Collection<Method> getRegisteredMethods();


    //****************** Executable method annotations ******************//

    /**
     * Annotation for methods that provide usage description.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ExecutableMethod {
        /**
         * Returns the description of the annotated method.
         * @return the description of the annotated method
         */
        String description();
        /**
         * Returns the description of the annotated method's arguments.
         * The number of items should be equal to the number of method's arguments
         * @return the description of the annotated method's arguments
         */
        String[] arguments();
    }

    /**
     * Prints the method usage built from the {@link ExecutableMethod} annotation.
     * If the annotation is not present, nothing is printed out.
     * Otherwise the concatenation of the method name, its argument descriptions
     * (in sharp parenthesis) and the method's description is printed out.
     *
     * @param out the print stream where the usage is print
     * @param printArguments flag whether to print the method's
     *          {@link ExecutableMethod#arguments() argument descriptions}
     * @param printDescription flag whether to print the method's
     *          {@link ExecutableMethod#description() description}
     * @param method the method for which to get the usage
     */
    public static void printUsage(PrintStream out, boolean printArguments, boolean printDescription, Method method) {
        ExecutableMethod annotation = method.getAnnotation(ExecutableMethod.class);
        if (annotation == null)
            return;
        
        out.print(method.getName());
        if (printArguments) {
            for (String argdesc : annotation.arguments()) {
                out.print(" <");
                out.print(argdesc);
                out.print(">");
            }
        }
        out.println();
        if (printDescription) {
            out.print("\t");
            out.println(annotation.description());
        }
    }

    /**
     * Prints usage of all methods managed by this executor.
     *
     * @param out the print stream where the usage is print
     * @param printArguments flag whether to print the method's
     *          {@link ExecutableMethod#arguments() argument descriptions}
     * @param printDescription flag whether to print the method's
     *          {@link ExecutableMethod#description() description}
     */
    public void printUsage(PrintStream out, boolean printArguments, boolean printDescription) {
        // Sort array first
        List<Method> methods = new ArrayList<Method>(getRegisteredMethods());
        Collections.sort(methods, new Comparator<Method>() {
            public int compare(Method o1, Method o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        // Print usage for every method
        for (Method method : methods)
            printUsage(out, printArguments, printDescription, method);
    }

    /**
     * Prints usage of method that is to be called for the given arguments.
     * @param out the print stream where the usage is print
     * @param printArguments flag whether to print the method's
     *          {@link ExecutableMethod#arguments() argument descriptions}
     * @param printDescription flag whether to print the method's
     *          {@link ExecutableMethod#description() description}
     * @param arguments the arguments for the method
     * @throws NoSuchMethodException if there is no method for the specified parameters in this executor
     */
    public void printUsage(PrintStream out, boolean printArguments, boolean printDescription, Object[] arguments) throws NoSuchMethodException {
        printUsage(out, printArguments, printDescription, getMethod(arguments));
    }


    //****************** Execution ******************//

    /**
     * Returns the method that is appropriate for the provided arguments.
     * @param arguments the arguments for the method
     * @return the method that is appropriate for the provided arguments
     * @throws NoSuchMethodException if there is no method that can process the provided arguments in this executor
     */
    protected abstract Method getMethod(Object[] arguments) throws NoSuchMethodException;

    /**
     * Execute specified method on exectutionObject with specified arguments and handle exceptions properly.
     * @param method the method to execute
     * @param executionObject the instance on which to invoke the method
     * @param arguments the method arguments
     * @return the method's return value
     * @throws NoSuchMethodException if the arguments are not compatible with the method
     * @throws InvocationTargetException if an exeception was thrown when the method was executed
     */
    protected static Object execute(Method method, Object executionObject, Object[] arguments) throws NoSuchMethodException, InvocationTargetException {
        try {
            // Execute method
            return method.invoke(executionObject, arguments);
        } catch (IllegalAccessException e) {
            throw new NoSuchMethodException("Can't access method: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new NoSuchMethodException("Specified arguments are invalid or an incorrect method was found: " + e.getMessage());
        }
    }
    
    /**
     * Execute a registered method using the specified arguments.
     * @param arguments the array of arguments for the execution method
     *          (must be consistent with the prototype in constructor)
     * @return the method's return value (if method returns void, <tt>null</tt> is returned instead)
     * @throws NoSuchMethodException if there is no method that can process the provided arguments in this executor
     * @throws InvocationTargetException if an exeception was thrown when the method was executed
     */
    public Object execute(Object... arguments) throws NoSuchMethodException, InvocationTargetException {
        return execute(getMethod(arguments), executionObject, arguments);
    }
    
    /**
     * Execute a registered method by arguments on background.
     * Another methods - executeBefore and executeAfter - can be called before
     * and after the execution in the same thread.
     * @param arguments the array of arguments for the execution method
     *          (must be consistent with the prototype in constructor)
     * @param executeBefore method to call before registered method
     * @param executeAfter method to call after registered method
     * @return a method execution thread object - method {@link MethodThread#waitExecutionEnd} can be used to retrieve the results
     * @throws NoSuchMethodException if there is no method that can process the provided arguments in this executor
     */
    public MethodThread backgroundExecute(Object[] arguments, Executable executeBefore, Executable executeAfter) throws NoSuchMethodException {
        return new MethodThread(getMethod(arguments), executionObject, arguments, executeBefore, executeAfter);
    }
    
    /**
     * Execute a registered method by arguments on background.
     * Another methods - executeBefore and executeAfter - can be called before
     * and after the execution in the same thread.
     * @param arguments The array of arguments for the execution method
     *          (must be consistent with the prototype in constructor)
     * @param executeBefore list of methods to call before registered method
     * @param executeAfter list of methods to call after registered method
     * @return a method execution thread object - method {@link MethodThread#waitExecutionEnd} can be used to retrieve the results
     * @throws NoSuchMethodException if there is no method that can process the provided arguments in this executor
     */
    public MethodThread backgroundExecute(Object[] arguments, List<Executable> executeBefore, List<Executable> executeAfter) throws NoSuchMethodException {
        return new MethodThread(getMethod(arguments), executionObject, arguments, executeBefore, executeAfter);
    }
    
    /**
     * Execute a registered method by arguments on background.
     * @param arguments the array of arguments for the execution method
     *          (must be consistent with the prototype in constructor)
     * @return a method execution thread object - method {@link MethodThread#waitExecutionEnd} can be used to retrieve the results
     * @throws NoSuchMethodException if there is no method that can process the provided arguments in this executor
     */
    public MethodThread backgroundExecute(Object... arguments) throws NoSuchMethodException {
        return new MethodThread(getMethod(arguments), executionObject, arguments);
    }
    
}