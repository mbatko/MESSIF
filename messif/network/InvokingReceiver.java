/*
 * InvokingReceiver.java
 *
 * Created on 6. kveten 2003, 0:34
 */

package messif.network;
import java.lang.reflect.Modifier;
import messif.executor.MethodClassExecutor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;



/**
 * Receiver that allows to execute a method for received message.
 * The receiver is constructed for a specified object on which the methods are executed.
 * Once the InvokingReceiver is registered to a {@link MessageDispatcher} and the dispatcher
 * offers a message to this receiver, the message is accepted if there exists a method with
 * that message as an argument in the provided object. The method is then invoked with the message
 * as its parameter. However, the method is invoked from the message dispatcher thread context and
 * thus blocks the dispatcher's message receiving until the method finishes. See {@link QueueInvokingReceiver}
 * and {@link ThreadInvokingReceiver} for non-blocking invokers.
 * 
 * <p>
 * For example, if there is an instance <code>example</code> the following class:
 * <pre>
 *   class Example {
 *     public void receive(MySpecialMessage msg) { ... }
 *     public void receive(MyOtherMessage msg) { ... }
 *     public void receive(ExampleMessage msg) { ... }
 *   }</pre>
 * where the <code>MySpecialMessage</code>, <code>MyOtherMessage</code> and <code>ExampleMessage</code> are descendants of {@link Message}.
 * </p>
 * 
 * <p>
 * The following code registers a new invoking receiver with the message dispatcher:
 * <pre>
 *   messageDispather.registerReceiver(new InvokingReceiver(example, "receive"));</pre>
 * 
 * Then, whenever an instance of message <CODE>MySpecialMessage</CODE> arrives at the dispatcher, it will be accepted by this
 * created <CODE>InvokingReceiver</CODE> and the first method of <CODE>Example</CODE> class will be invoked with the received message in
 * its parameter.
 * </p>
 * @author xbatko
 * @see MessageDispatcher
 */
public class InvokingReceiver extends MethodClassExecutor implements Receiver {
    
    /****************** Constructors ******************/
    
    /**
     * Creates a new instance of InvokingReceiver for message methods.
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
    public InvokingReceiver(Object executionObject, String methodsName) throws IllegalArgumentException {
        super(executionObject, 0, methodsName, Modifier.PUBLIC | Modifier.PROTECTED, Object.class, Message.class);
    }

    /****************** Message processor ******************/
    
    /**
     * Accepts the message if a there is a method for the message's class.
     *
     * @param msg the message offered to acceptance
     * @param allowSuperclass First, the message is offered with <tt>allowSuperclass</tt> set to <tt>false</tt>.
     *                        If no receiver accepts it, another offering round is issued with <tt>allowSuperclass</tt>
     *                        set to <tt>true</tt> (so the receiver can relax its acceptance conditions).
     * @return <tt>true</tt> if a there is a method for the message's class
     */
    public boolean acceptMessage(Message msg, boolean allowSuperclass) {
        // Get invoking method
        Method method = getMethod(msg.getClass(), allowSuperclass);
        if (method == null)
            return false;

        // We have the method and its argument so we will process it
        processMessage(msg, method);
        
        // Message accepted
        return true;
    }

    /**
     * Invoke the method associtated with the accepted message.
     *
     * @param msg the accepted message (it will be the parameter for the invoked method)
     * @param method the method to invoke on the executionObject
     */
    protected void processMessage(Message msg, Method method) {
        // Try to invoke the operation executor
        try {
            method.invoke(executionObject, msg);
        } catch (Exception e) {
            MessageDispatcher.log.severe(e);
        }
    }
}
