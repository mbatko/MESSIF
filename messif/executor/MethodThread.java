/*
 * MethodThread.java
 *
 * Created on 20. unor 2005, 10:12
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
 * @author  xbatko
 */
public final class MethodThread extends SingleMethodExecutor implements Runnable {
    
    /****************** Internal data ******************/
    
    /** Thread in which the operation is run */
    protected final Thread runningThread = new Thread(this);
    
    /** Exception catched during the thread execution */
    protected Exception threadException = null;

    /** List of executed methods */
    protected final List<Executable> methods;
    
    /****************** Constructors ******************/
    
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

    /****************** State access ******************/
    
    /** Returns true if the method is still being executed, otherwise false is returned */
    public boolean isRunning() {
        return runningThread.isAlive();
    }
    
    /** This method waits for the end of execution (if not already done) and returns true if the execution was successful */
    public boolean isSuccess() {
        return waitExecutionEnd() == null;
    }


    /****************** Execution methods ******************/
    
    /** Wait for the end of the operation execution in specified thread (returned by backgroundExecute)
     *  This method will block until the background operation finishes its processing.
     *  @return exception that occurred during the thread run or null if execution was not interrupted
     */
    public Exception waitExecutionEnd() {
        synchronized (runningThread) {
            // Wait the end of thread run
            while (runningThread.isAlive())
                try { runningThread.wait(); } catch (InterruptedException e) { return e; }
        
            return threadException;
        }
    }

    /** Execute the method inside the thread */
    public void run() {
        synchronized (runningThread) {
            threadException = null;
            try {
                if (methods != null)
                    for (Executable method : methods)
                        method.execute();
                else execute();
            } catch (Exception e) {
                threadException = e;
            }
            runningThread.notifyAll();
        }
    }

}