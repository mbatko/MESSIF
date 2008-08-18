/*
 * MethodExecutor.java
 *
 * Created on 20. unor 2005, 10:12
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
 * @author  xbatko
 */
public abstract class MethodExecutor {
    
    /****************** Internal data ******************/

    /** The object that the operations are invoked on */
    protected final Object executionObject;


    /****************** Constructors ******************/
    
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


    /****************** Internal methods ******************/

    /** Return all methods that are registered within this executor */
    protected abstract Collection<Method> getRegisteredMethods();


    /****************** Executable method annotations ******************/

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ExecutableMethod {
        String description();
        String[] arguments();
    }

    protected static String getMethodDescription(Method method) {
        ExecutableMethod annotation = method.getAnnotation(ExecutableMethod.class);
        return (annotation == null)?null:annotation.description();
    }

    protected static String[] getMethodArgumentDescription(Method method) {
        ExecutableMethod annotation = method.getAnnotation(ExecutableMethod.class);
        return (annotation == null)?null:annotation.arguments();
    }
    
    protected static String getMethodUsage(Method method) {
        ExecutableMethod annotation = method.getAnnotation(ExecutableMethod.class);
        if (annotation == null)
            return "";
        
        StringBuffer rtv = new StringBuffer(method.getName());
        for (String argdesc : annotation.arguments())
            rtv.append(" <").append(argdesc).append(">");
        rtv.append("\r\n\t").append(annotation.description());
        
        return rtv.toString();
    }
    
    public void printUsage(PrintStream out) {
        // Sort array first
        List<Method> methods = new ArrayList<Method>(getRegisteredMethods());
        Collections.sort(methods, new Comparator<Method>() {
            public int compare(Method o1, Method o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        
        // Print usage for every method
        for (Method method : methods)
            out.println(getMethodUsage(method));
    }

    /****************** Execution ******************/

    /** Get appropriate method for provided arguments */
    protected abstract Method getMethod(Object[] arguments) throws NoSuchMethodException;

    /** Execute specified method on exectutionObject with specified arguments and handle exceptions properly. */
    protected static Object execute(Method method, Object executionObject, Object[] arguments) throws NoSuchMethodException, InvocationTargetException {
        try {
            // Execute method
            return method.invoke(executionObject, arguments);
        } catch (IllegalAccessException e) {
            throw new NoSuchMethodException("Can't access method: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new NoSuchMethodException("Specified arguments are invalid or an incorrect method was found: " + e.getMessage());
        } catch (InvocationTargetException e) {
            throw new InvocationTargetException(e.getCause(), getMethodUsage(method));
        }
    }
    
    /** Execute registered method by arguments
     *  @param arguments The array of arguments for the execution method (must be consistent with the prototype in constructor)
     */
    public Object execute(Object... arguments) throws NoSuchMethodException, InvocationTargetException {
        return execute(getMethod(arguments), executionObject, arguments);
    }
    
    /** Execute registered method by arguments on background. 
     *  Another methods - executeBefore and executeAfter - can be called before and after the execution in the same thread.
     *  @param arguments The array of arguments for the execution method (must be consistent with the prototype in constructor)
     *  @param executeBefore method to call before registered method
     *  @param executeAfter method to call after registered method
     *  @return method execution thread object. Method waitExecutionEnd of this object can be used to retrieve the results
     */
    public MethodThread backgroundExecute(Object[] arguments, Executable executeBefore, Executable executeAfter) throws NoSuchMethodException {
        return new MethodThread(getMethod(arguments), executionObject, arguments, executeBefore, executeAfter);
    }
    
    /** Execute registered method by arguments on background. 
     *  Another methods - executeBefore and executeAfter - can be called before and after the execution in the same thread.
     *  @param arguments The array of arguments for the execution method (must be consistent with the prototype in constructor)
     *  @param executeBefore list of methods to call before registered method
     *  @param executeAfter list of methods to call after registered method
     *  @return method execution thread object. Method waitExecutionEnd of this object can be used to retrieve the results
     */
    public MethodThread backgroundExecute(Object[] arguments, List<Executable> executeBefore, List<Executable> executeAfter) throws NoSuchMethodException {
        return new MethodThread(getMethod(arguments), executionObject, arguments, executeBefore, executeAfter);
    }
    
    /** Execute registered method by arguments on background. 
     *  @param arguments The array of arguments for the execution method (must be consistent with the prototype in constructor)
     *  @return method execution thread object. Method waitExecutionEnd of this object can be used to retrieve the results
     */
    public MethodThread backgroundExecute(Object... arguments) throws NoSuchMethodException {
        return new MethodThread(getMethod(arguments), executionObject, arguments);
    }
    
}