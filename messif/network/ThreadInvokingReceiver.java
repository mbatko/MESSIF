/*
 * ThreadInvokingReceiver.java
 *
 * Created on 15. kveten 2003, 14:01
 */

package messif.network;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Receiver that allows to execute a method for received message without blocking the dispather.
 * This object invokes message methods in threads (new thread is created for every accepted message).
 *
 * @author  xbatko
 */
public class ThreadInvokingReceiver extends InvokingReceiver {

    /****************** Constructors ******************/

    /**
     * Creates a new instance of ThreadInvokingReceiver for message methods.
     * During the construction, the public and protected methods of the <code>executionObject</code>
     * that have the specified <code>methodsName</code> and one message argument
     * (i.e. a class that is a descendant of <code>Message</code>) are remebered and associated with
     * their message argument class.
     * Invoking then uses this fast association to invoke a method specific for the received message
     * (according to its class).
     *
     * @param executionObject the object on which the message methods are invoked
     * @param methodsName the name of the methods to inspect (if <tt>null</tt>, all methods are inspected)
     * @throws IllegalArgumentException if the supplied execution object is <tt>null</tt>
     */
    public ThreadInvokingReceiver(Object executionObject, String methodsName) throws IllegalArgumentException {
        super(executionObject, methodsName);
    }


    /****************** Invoker manager threading and semaphores ******************/

    /** Invoking receiver thread counter */
    private static AtomicInteger nextThreadNumber = new AtomicInteger(1);

    /** Invoking receivers thread group */
    private static ThreadGroup threadGroup = new ThreadGroup("tgInvokingReceivers");

    /**
     * Run a separate thread that invokes the accepted message's method.
     * @param msg the accepted message (it will be the parameter for the invoked method)
     * @param method the method to invoke on the executionObject
     */
    protected void processMessage(final Message msg, final Method method) {
        new Thread(threadGroup, "thInvokingReceiver-" + nextThreadNumber.getAndIncrement()) {
            public void run() {
                ThreadInvokingReceiver.super.processMessage(msg, method);
            }
        }.start();
    }
    
}
