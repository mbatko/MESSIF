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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * This is a wrapper for the MethodExecutor object, which automatically remembers
 * all the method threads that were executed on background. The list of the
 * threads is kept per ThreadLocal variable, thus accessible only by one thread
 * - the one that has started the background execute operation.
 * 
 * <p>
 * A wait function is available - it is a blocking method, which waits for the
 * end of execution for all stored threads.
 * </p>
 * 
 * <p>
 * Parameters from all the stored methods can be retrieved by get operations.
 * </p>
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class MethodThreadList {

    //****************** Attributes ******************//

    /** MethodExecutor object that is bound to this */
    protected final MethodExecutor methodExecutor;

    /** List of MethodThreads currently executed on background */
    protected final List<MethodThread> methodStartedList = Collections.synchronizedList(new ArrayList<MethodThread>());

    /** List of MethodThreads executed on background and now finished */
    protected final List<MethodThread> methodFinishedList = Collections.synchronizedList(new ArrayList<MethodThread>());


    //****************** Constructors ******************//

    /**
     * Create a new instance of MethodThreadList.
     * @param methodExecutor
     */
    public MethodThreadList(MethodExecutor methodExecutor) {
        this.methodExecutor = methodExecutor;
    }


    //****************** Execute wrapper ******************//

    /**
     * Execute registered method by arguments
     * @param arguments The array of arguments for the execution method (must be consistent with the prototype in constructor)
     * @throws NoSuchMethodException if the specified arguments are invalid or a method for them was not found
     * @throws InvocationTargetException if there was an exception during the method execution
     */
    public void execute(Object... arguments) throws NoSuchMethodException, InvocationTargetException {
        methodExecutor.execute(arguments);
    }


    //****************** Background execute wrapper ******************//

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

    /**
     * Execute registered method by arguments on background.
     * Another methods - executeBefore and executeAfter - can be called before and after the execution in the same thread.
     * @param arguments The array of arguments for the execution method (must be consistent with the prototype in constructor)
     * @param executeBefore list of methods to call before registered method
     * @param executeAfter list of methods to call after registered method
     * @return method execution thread object. Method waitExecutionEnd of this object can be used to retrieve the results
     * @throws NoSuchMethodException if there was no valid method for the specified arguments
     */
    public MethodThread backgroundExecute(Object[] arguments, List<Executable> executeBefore, List<Executable> executeAfter) throws NoSuchMethodException {
        // Begin execution in a new method thread
        MethodThread rtv = methodExecutor.backgroundExecute(arguments, executeBefore, executeAfter);
        
        // Store the thread in local list
        methodStartedList.add(rtv);
        
        return rtv;
    }

    /**
     * Execute registered method by arguments on background.
     * @param arguments The array of arguments for the execution method (must be consistent with the prototype in constructor)
     * @return method execution thread object. Method waitExecutionEnd of this object can be used to retrieve the results
     * @throws NoSuchMethodException if there was no valid method for the specified arguments
     */
    public MethodThread backgroundExecute(Object... arguments) throws NoSuchMethodException {
        // Begin execution in a new method thread
        MethodThread rtv = methodExecutor.backgroundExecute(arguments);
        
        // Store the thread in local list
        methodStartedList.add(rtv);
        
        return rtv;
    }

    /**
     * Wait for all operations executed on background to finish
     * @return Number of currently finished methods
     * @throws InterruptedException if the waiting was interrupted
     */
    public int waitBackgroundExecuteOperation() throws InterruptedException {
        synchronized (methodStartedList) {
            // Wait for all the threads to finish
            for (Iterator<MethodThread> i = methodStartedList.iterator(); i.hasNext();) {
                // Get the method thread and wait for its execution
                MethodThread thread = i.next();
                thread.waitExecutionEnd();
                
                // Finished (either normally or by exception), move to finished list
                i.remove();
                methodFinishedList.add(thread);
            }

            return methodFinishedList.size();
        }
    }


    //****************** Background execution list access ******************//

    /**
     * Clears both the lists - started and finished threads.
     */
    public void clearThreadLists() {
        methodStartedList.clear();
        methodFinishedList.clear();
    }

    /**
     * Returns list of selected arguments from each finished method.
     * The respective argument is specified by class, one argument
     * is returned for each method.
     * 
     * @param <E> the class of the selected argument
     * @param argClass the class of the selected argument
     * @return list of executed methods' arguments
     * @throws NoSuchElementException if a parameter with the argClass class was not found
     * @throws Exception if there was an exception during execution of any of the methods
     */
    public <E> List<E> getAllMethodsArgument(Class<E> argClass) throws NoSuchElementException, Exception {
        synchronized (methodFinishedList) {
            // Prepare return array
            List<E> rtv = new ArrayList<E>(methodFinishedList.size());

            // Add thread argument to return array
            for (MethodThread i : methodFinishedList) {
                Exception e = i.getException();
                if (e != null)
                    throw e;
                rtv.add(i.getArgument(argClass));
            }
            
            return rtv;
        }
    }

    /**
     * Get executed argument on the given position from all finished methods
     * @param position the argument position to get
     * @return a list of the argument on the given position from all finished methods
     * @throws NoSuchElementException if a parameter with the argClass class was not found
     * @throws Exception if there was an exception during execution of any of the methods
     */
    public List<Object> getAllMethodsArgument(int position) throws NoSuchElementException, Exception {
        synchronized (methodFinishedList) {
            // Prepare return array
            List<Object> rtv = new ArrayList<Object>(methodFinishedList.size());

            // Add thread argument to return array
            for (MethodThread i : methodFinishedList) {
                Exception e = i.getException();
                if (e != null)
                    throw e;
                rtv.add(i.getArgument(position));
            }
            
            return rtv;
        }
    }

    /**
     * Returns a list of values returned from each finished method.
     * @param <E> the class of the return types
     * @param valuesClass the class of the return types
     * @return list of executed methods' returned values
     * @throws ClassCastException if some of the returned values cannot be cast to <code>valuesClass</code>
     * @throws Exception if there was an exception during execution of any of the methods
     */
    public <E> List<E> getAllMethodsReturnValue(Class<E> valuesClass) throws ClassCastException, Exception {
        synchronized (methodFinishedList) {
            // Prepare return array
            List<E> rtv = new ArrayList<E>(methodFinishedList.size());

            // Add thread argument to return array
            for (MethodThread i : methodFinishedList) {
                Exception e = i.getException();
                if (e != null)
                    throw e;
                rtv.add(valuesClass.cast(i.getReturnedValue()));
            }

            return rtv;
        }
    }

    /**
     * Returns a list of values returned from each finished method.
     * @return list of executed methods' returned values
     * @throws Exception if there was an exception during execution of any of the methods
     */
    public List<Object> getAllMethodsReturnValue() throws Exception {
        return getAllMethodsReturnValue(Object.class);
    }

}
