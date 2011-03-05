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
import java.util.ArrayList;
import java.util.List;


/**
 *  This class allows the background method execution (i.e. in a new thread)
 *  using the MethodExecutor interface.
 *
 *  Use backgroundExecute method in MethodExecutor to start the execution.
 *
 *  Use waitExecutionEnd method to block until the thread finished the
 *  execution of the invocated method.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public final class MethodThread extends SingleMethodExecutor implements Runnable {

    //****************** Internal data ******************//

    /** Thread in which the operation is run */
    private final Thread runningThread = new Thread(this);

    /** Exception catched during the thread execution */
    private Exception threadException = null;

    /** List of executed methods */
    private final List<Executable> methods;


    //****************** Constructors ******************//

    /**
     * Create new instance of MethodThread
     *
     * The constructor can't be called directly, use backgroundExecute "factory" member of MethodExecutor
     * 
     * @param method method to call on the specified object
     * @param object the executor object, that is used for invocation of methods
     * @param arguments the arguments of the executed method
     */
    protected MethodThread(Method method, Object object, Object[] arguments) {
        super(method, object, arguments);
        this.methods = null;
        runningThread.start();
    }

    /**
     * Create new instance of MethodThread
     *
     * The constructor can't be called directly, use backgroundExecute "factory" member of MethodExecutor
     * 
     * @param method method to call on the specified object
     * @param object the executor object, that is used for invocation of methods
     * @param arguments the arguments of the executed method
     * @param executeBefore list of methods to call before the execution of the <tt>method</tt> (can be null if no pre/post execution is required)
     * @param executeAfter list of methods to call after the successful execution of the <tt>method</tt> (can be null if no pre/post execution is required)
     */
    protected MethodThread(Method method, Object object, Object[] arguments, List<Executable> executeBefore, List<Executable> executeAfter) {
        super(method, object, arguments);
        this.methods = new ArrayList<Executable>();
        if (executeBefore != null)
            this.methods.addAll(executeBefore);
        this.methods.add(this);
        if (executeAfter != null)
            this.methods.addAll(executeAfter);
        runningThread.start();        
    }

    /**
     * Create new instance of MethodThread
     *
     * The constructor can't be called directly, use backgroundExecute "factory" member of MethodExecutor
     * 
     * @param method method to call on the specified object
     * @param object the executor object, that is used for invocation of methods
     * @param arguments the arguments of the executed method
     * @param executeBefore a method to call before the execution of the <tt>method</tt> (can be null if no pre/post execution is required)
     * @param executeAfter a method to call after the successful execution of the <tt>method</tt> (can be null if no pre/post execution is required)
     */
    protected MethodThread(Method method, Object object, Object[] arguments, Executable executeBefore, Executable executeAfter) {
        super(method, object, arguments);
        this.methods = new ArrayList<Executable>();
        if (executeBefore != null)
            this.methods.add(executeBefore);
        this.methods.add(this);
        if (executeAfter != null)
            this.methods.add(executeAfter);
        runningThread.start();        
    }


    //****************** State access ******************//

    /**
     * Returns <tt>true</tt> if the method is still being executed, otherwise <tt>false</tt> is returned.
     * @return <tt>true</tt> if the method is still being executed, otherwise <tt>false</tt> is returned
     */
    public boolean isRunning() {
        return runningThread.isAlive();
    }

    /**
     * This method waits for the end of execution.
     * If the method is already finished, this method returns immediately.
     * @return <tt>true</tt> if the execution was successful
     * @throws InterruptedException if the waiting was interrupted
     */
    public boolean isSuccess() throws InterruptedException {
        waitExecutionEnd();
        return threadException == null;
    }

    /**
     * Returns the exception thrown while executing or <tt>null</tt>
     * if the execution finished with no exception.
     * @return the exception thrown in execution
     */
    public Exception getException() {
        return threadException;
    }


    //****************** Execution methods ******************//

    /**
     * Wait for the end of the operation execution in specified thread (returned by backgroundExecute)
     * This method will block until the background operation finishes its processing.
     * @throws InterruptedException if the waiting was interrupted
     */
    public void waitExecutionEnd() throws InterruptedException {
        synchronized (runningThread) {
            // Wait the end of thread run
            while (runningThread.isAlive())
                runningThread.wait();
        }
    }

    /** Execute the method inside the thread */
    @Override
    public void run() {
        synchronized (runningThread) {
            threadException = null;
            try {
                if (methods != null)
                    for (Executable executable : methods)
                        executable.execute();
                else execute();
            } catch (Exception e) {
                threadException = e;
            }
            runningThread.notifyAll();
        }
    }

}