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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Receiver that allows to execute a method for received message without blocking the dispather.
 * This object invokes message methods in threads (new thread is created for every accepted message).
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
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
