/*
 * MethodThreadList.java
 *
 * Created on 1. brezen 2005, 17:01
 */

package messif.executor;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 *  This is a wrapper for the MethodExecutor object, which automatically remembers
 *  all the method threads that were executed on background. The list of the
 *  threads is kept per ThreadLocal variable, thus accessible only by one thread
 *  - the one that has started the background execute operation.
 *
 *  A wait function is available - it is a blocking method, which waits for the 
 *  end of execution for all stored threads.
 *
 *  Parameters from all the stored methods can be retrieved by get operations.
 *
 * @author xbatko
 */
public class MethodThreadList {
    
    /****************** Internal variables ******************/
    /** MethodExecutor object that is bound to this */
    protected final MethodExecutor methodExecutor;
    
    /** List of MethodThreads currently executed on background */
    protected final List<MethodThread> methodStartedList = Collections.synchronizedList(new ArrayList<MethodThread>());

    /** List of MethodThreads executed on background and now finished */
    protected final List<MethodThread> methodFinishedList = Collections.synchronizedList(new ArrayList<MethodThread>());
    
    /****************** Constructors ******************/

    /**
     * Create a new instance of MethodThreadList.
     */
    public MethodThreadList(MethodExecutor methodExecutor) {
        this.methodExecutor = methodExecutor;
    }

    /****************** Clear both lists - started and finished threads ***********************/
    public void clearThreadLists() {
        methodStartedList.clear();
        methodFinishedList.clear();
    }
    
    /****************** Execute wrapper ******************/

    /** Execute registered method by arguments
     *  @param arguments The array of arguments for the execution method (must be consistent with the prototype in constructor)
     */
    public void execute(Object... arguments) throws NoSuchMethodException, InvocationTargetException {
        methodExecutor.execute(arguments);
    }

    
    /****************** Background execute wrapper ******************/
    
    /**
     * Execute registered method by arguments on background. 
     * Another methods - executeBefore and executeAfter - can be called before and after the execution in the same thread.
     * 
     * @param arguments The array of arguments for the execution method (must be consistent with the prototype in constructor)
     * @param executeBefore method to call before registered method
     * @param executeAfter method to call after registered method
     * @return method execution thread object. Method waitExecutionEnd of this object can be used to retrieve the results
     * @throws NoSuchMethodException if there was no valid method for the specified arguments
     */
    public MethodThread backgroundExecute(Object[] arguments, Executable executeBefore, Executable executeAfter) throws NoSuchMethodException {
        // Begin execution in a new method thread
        MethodThread rtv = methodExecutor.backgroundExecute(arguments, executeBefore, executeAfter);
        
        // Store the thread in local list
        methodStartedList.add(rtv);
        
        return rtv;
    }
        
    /** Execute registered method by arguments on background. 
     *  Another methods - executeBefore and executeAfter - can be called before and after the execution in the same thread.
     *  @param arguments The array of arguments for the execution method (must be consistent with the prototype in constructor)
     *  @param executeBefore list of methods to call before registered method
     *  @param executeAfter list of methods to call after registered method
     *  @return method execution thread object. Method waitExecutionEnd of this object can be used to retrieve the results
     */
    public MethodThread backgroundExecute(Object[] arguments, List<Executable> executeBefore, List<Executable> executeAfter) throws NoSuchMethodException {
        // Begin execution in a new method thread
        MethodThread rtv = methodExecutor.backgroundExecute(arguments, executeBefore, executeAfter);
        
        // Store the thread in local list
        methodStartedList.add(rtv);
        
        return rtv;
    }
    
    /** Execute registered method by arguments on background. 
     *  @param arguments The array of arguments for the execution method (must be consistent with the prototype in constructor)
     *  @return method execution thread object. Method waitExecutionEnd of this object can be used to retrieve the results
     */
    public MethodThread backgroundExecute(Object... arguments) throws NoSuchMethodException {
        // Begin execution in a new method thread
        MethodThread rtv = methodExecutor.backgroundExecute(arguments);
        
        // Store the thread in local list
        methodStartedList.add(rtv);
        
        return rtv;
    }

    /** Wait for all operations executed on background to finish
     *  @return Number of currently finished methods
     *  @throws Exception if there was an exception during waiting (the waiting is not finished then)
     */
    public int waitBackgroundExecuteOperation() throws Exception {
        synchronized (methodStartedList) {
            // Wait for all the threads to finish
            for (Iterator<MethodThread> i = methodStartedList.iterator(); i.hasNext();) {
                // Get the method thread
                MethodThread thread = i.next();
                
                // Wait and handle the possible exception
                Exception e = thread.waitExecutionEnd();
                if (e != null) throw e;
                
                // Finished, move to finished list
                i.remove();
                methodFinishedList.add(thread);
            }

            return methodFinishedList.size();
        }
    }        
    
    
    /****************** Background execution list access ******************/
    
    /**
     * Returns list of selected arguments from each finished method.
     * The respective argument is specified by class, one argument
     * is returned for each method.
     * 
     * @param argClass the class of the selected argument
     * @return list of executed methods' arguments
     * @throws NoSuchElementException if a parameter with the argClass class was not found
     */
    public <E> List<E> getAllMethodsArgument(Class<E> argClass) {
        synchronized (methodFinishedList) {
            // Prepare return array
            List<E> rtv = new ArrayList<E>(methodFinishedList.size());

            // Add thread argument to return array
            for (MethodThread i:methodFinishedList)
                rtv.add(i.getArgument(argClass));
            
            return rtv;
        }
    }

    /** Get executed argument on given position from all finished methods
     *  @throws NoSuchElementException if a parameter with the argClass class was not found
     */
    public List getAllMethodsArgument(int position) {
        synchronized (methodFinishedList) {
            // Prepare return array
            List<Object> rtv = new ArrayList<Object>(methodFinishedList.size());

            // Add thread argument to return array
            for (MethodThread i:methodFinishedList)
                rtv.add(i.getArgument(position));                
            
            return rtv;
        }
    }

}
