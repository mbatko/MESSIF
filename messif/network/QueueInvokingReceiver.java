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
package messif.network;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;


/**
 * Receiver that allows to execute a method for received message without blocking the dispather.
 * This object maintains a queue of messages and invoke their associated methods one after another.
 * The queue is fed by accepted messages from the dispatcher.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class QueueInvokingReceiver extends InvokingReceiver implements Runnable {
    
    /****************** Message queue ******************/
    
    /** The list of messages to execute */
    private final Queue<Message> messageQueue;
    /** The list of methods to execute */
    private final Queue<Method> methodQueue;
    /** The thread for executing methods on messages */
    private final Thread queueThread;
    

    /****************** Constructors ******************/
    
    /**
     * Creates a new instance of QueueInvokingReceiver for message methods.
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
    public QueueInvokingReceiver(Object executionObject, String methodsName) throws IllegalArgumentException {
        super(executionObject, methodsName);
        
        // Create new message queue (vector, adding at end, retrieving from begining)
        messageQueue = new LinkedList<Message>();
        methodQueue = new LinkedList<Method>();
        
        // Start queing thread
        queueThread = new Thread(this);
        queueThread.start();
    }

    /**
     * Stops the queue thread and clears the queue.
     */
    public void finalize() {
        queueThread.interrupt();
        synchronized (messageQueue) {
            messageQueue.clear();
            methodQueue.clear();
        }
    }
    
    
    /****************** Message queue manipulation ******************/
    
    /**
     * Put the accepted message and the associated method to the queue.
     * @param msg the accepted message (it will be the parameter for the invoked method)
     * @param method the method to invoke on the executionObject
     */
    protected void processMessage(Message msg, Method method) {
        synchronized(messageQueue) {
            // Put message into queue
            messageQueue.offer(msg);
            methodQueue.offer(method);
                      
            // Inform of producing
            messageQueue.notifyAll();
        }
    }
    
    /**
     * Retrieves one message from queue and invoke the associated method for it.
     * The method puts the thread into waiting state if the queue is empty.
     *
     * @throws InterruptedException if the thread was interrupted in waiting
     */
    protected final void invokeNextMessage() throws InterruptedException {
        for (;;) {
            Message msg;
            Method method;
            synchronized(messageQueue) {
                // If there are no messages waiting, wait for the producer thread
                if (messageQueue.isEmpty())
                    messageQueue.wait(); 

                // Get a message with its associated method from the queue
                msg = messageQueue.remove();
                method = methodQueue.remove();
            }
            // Invoke the message method
            super.processMessage(msg, method);
        }
    }
    
    
    /****************** Queue thread operations ******************/
    
    /**
     * Queue invoker processor.
     * All messages in the queue is serialy processed.
     * If there is no message in the queue, the thread is put into waiting state and 
     * it is notified when a new message arrives to the queue.
     */
    public void run() {
        try {
            // Receive and process message in this thread
            for (;;) invokeNextMessage();
        } catch (InterruptedException e) {
            // End thread on interuption
        }
    }
    
}
