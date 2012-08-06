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
package messif.algorithms.impl;

import messif.algorithms.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import messif.network.NetworkNode;
import messif.operations.AbstractOperation;
import messif.statistics.OperationStatistics;
import messif.utility.reflection.NoSuchInstantiatorException;

/**
 * Uses a RMI connection to remote algorithm to simulate local algorithm.
 * 
 * @see AlgorithmRMIServer
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class RMIConnection implements Cloneable {

    /** class id for serialization */
    private static final long serialVersionUID = 687001L;

    //****************** Attributes ******************//
    /** Remote algorithm's IP address */
    private final InetAddress host;

    /** Remote algorithm's RMI port */
    private final int port;

    /** Number of reconnection tries if the RMI connection fails */
    private final int connectionRetries;

    /** Opened RMI connection to remote algorithm */
    private transient Socket socket;

    /** RMI connection output stream for sending method invocations to the remote algorithm */
    private transient ObjectOutputStream out;

    /** RMI connection input stream for reading remote algorithm results */
    private transient ObjectInputStream in;

    //****************** Constructors ******************//
    /**
     * Creates a new instance of RMI algorithm.
     * @param host the remote algorithm's IP address
     * @param port the remote algorithm's RMI port
     * @param connectionRetries the number of reconnection tries if the RMI connection fails
     */
    public RMIConnection(InetAddress host, int port, int connectionRetries) {
        this.host = host;
        this.port = port;
        this.connectionRetries = connectionRetries;
    }

    /**
     * Creates a new instance of RMI algorithm.
     * @param host the remote algorithm's IP address
     * @param port the remote algorithm's RMI port
     */
    public RMIConnection(InetAddress host, int port) {
        this(host, port, 1);
    }

    /**
     * Creates a new instance of RMI algorithm.
     * @param host the remote algorithm's host name
     * @param port the remote algorithm's RMI port
     * @param connectionRetries the number of reconnection tries if the RMI connection fails
     * @throws UnknownHostException if the host name cannot be resolved to IP address
     */
    public RMIConnection(String host, int port, int connectionRetries) throws UnknownHostException {
        this(InetAddress.getByName(host), port, connectionRetries);
    }

    /**
     * Creates a new instance of RMI algorithm.
     * @param host the remote algorithm's host name
     * @param port the remote algorithm's RMI port
     * @throws UnknownHostException if the host name cannot be resolved to IP address
     */
    public RMIConnection(String host, int port) throws UnknownHostException {
        this(InetAddress.getByName(host), port);
    }

    /**
     * Creates a new instance of RMI algorithm.
     * @param networkNode host + RMI port of the remote algorithm
     */
    public RMIConnection(NetworkNode networkNode) {
        this(networkNode.getHost(), networkNode.getPort());
    }

    @Override
    public void finalize() throws Throwable {
        // Clean up connection
        disconnect();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return new RMIConnection(host, port, connectionRetries);
    }

    //****************** Connection control methods ******************//
    /**
     * Returns the remote algorithm's IP address.
     * @return the remote algorithm's IP address
     */
    public InetAddress getHost() {
        return host;
    }

    /**
     * Returns the remote algorithm's RMI port.
     * @return the remote algorithm's RMI port
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns <tt>true</tt> if the algorithm is currently connected.
     * @return <tt>true</tt> if the algorithm is currently connected
     */
    public boolean isConnected() {
        return (socket != null) && socket.isConnected();
    }

    /**
     * Connects this algorithm to the RMI service.
     * @throws IOException if there was a problem connecting
     */
    public synchronized void connect() throws IOException {
        if (!isConnected()) {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            out.flush();
            in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
        }
    }

    /**
     * Disconnects this algorithm from the RMI service.
     */
    public synchronized void disconnect() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignore) {
            } // The IO exceptions when closing connection are ignored
            socket = null;
            in = null;
            out = null;
        }
    }

    //****************** Remote method invocation ******************//
    /**
     * Executes a given method on the remote algorithm and returns result.
     * If an I/O error occurrs during the communication, the connection is tried
     * to be reestablished for <code>reconnectRetries</code> times. After that
     * an {@link IllegalStateException} is thrown.
     * Note that the result returned might be an exception.
     * @param methodName the name of the method to execute on the remote algorithm
     * @param reconnectRetries the number of 
     * @param methodArguments the arguments for the method
     * @return the method result or exception
     * @throws IllegalStateException if there was a problem communicating with the remote algorithm
     * @throws IllegalArgumentException if there was a problem reading the class in the remote algorithm's result
     */
    private synchronized void methodExecute(String methodName, int reconnectRetries, Object... methodArguments) throws IllegalStateException, IllegalArgumentException {
        for (;;) {
            try {
                connect(); // Does nothing if already connected
                out.writeUTF(methodName);
                out.writeUnshared(methodArguments);
                out.reset();
                out.flush();
                return;
            } catch (IOException e) {
                disconnect();
                if (reconnectRetries-- <= 0)
                    throw new IllegalStateException("Error communicating with remote algorithm", e);
            }
        }
    }

    /**
     * If the given exception is {@link RuntimeException}, it is returned directly.
     * Otherwise the original cause is searched, then wrapped into IllegalArgumentException
     * and returned.
     * @param exception the exception object to handle
     * @return returns the wrapped runtime exception
     * @returns the handled exception
     */
    private RuntimeException handleException(Object exception) {
        if (exception instanceof RuntimeException) {
            return (RuntimeException) exception;
        } else {
            // Unwrap exception
            Throwable ex = (Exception) exception;
            while (ex.getCause() != null) {
                ex = ex.getCause();
            }

            // Wrap into runtime exception of illegal state
            return new IllegalArgumentException(ex);
        }
    }

    /**
     * Waits for finishing the previously (remotely) executed operation and returns the result (operation or exceptions).
     * 
     * @return the resulting operation (or an exception) after execution remotely
     * @throws IllegalArgumentException if deserialization of the result failed
     * @throws IllegalStateException if communication with the remote algorithm failed
     */
    protected Object waitLastExecutionResult() throws IllegalArgumentException, IllegalStateException {
        try {
            return in.readUnshared();
        } catch (IOException e) {
            disconnect();
            throw new IllegalStateException("Error communicating with remote algorithm", e);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot read result of the last executed method: " + e);
        }
    }

    /**
     * Waits for finishing the previously (remotely) executed operation and returns the result.
     * 
     * @return the resulting operation after execution remotely
     * @throws AlgorithmMethodException if the processing returned this exception
     * @throws NoSuchMethodException if the execution failed to execute specified method
     */
    public AbstractOperation waitLastExecutionOperation() throws AlgorithmMethodException, NoSuchMethodException {
        Object rtv = waitLastExecutionResult();
        if (rtv instanceof Exception) {
            if (rtv instanceof AlgorithmMethodException)
                throw (AlgorithmMethodException) rtv;
            if (rtv instanceof NoSuchMethodException)
                throw (NoSuchMethodException) rtv;
            throw handleException(rtv);
        } else {
            return (AbstractOperation) rtv;
        }
    }

    //****************** Wrappers for algorithm methods ******************//
    @SuppressWarnings("unchecked")
    public <T extends AbstractOperation> T executeOperation(T operation) throws AlgorithmMethodException, NoSuchMethodException {
        methodExecute("setupStatsAndExecuteOperation", connectionRetries, operation, null);
        Object rtv = waitLastExecutionResult();
        if (rtv instanceof Exception) {
            if (rtv instanceof AlgorithmMethodException)
                throw (AlgorithmMethodException) rtv;
            if (rtv instanceof NoSuchMethodException)
                throw (NoSuchMethodException) rtv;
            throw handleException(rtv);
        } else {
            return (T) rtv;
        }
    }

    /**
     * This method only executes the operation in the remote algorithm (does not wait for the response)
     * @param operation 
     */
    public void backgroundExecuteOperation(AbstractOperation operation) {
        methodExecute("setupStatsAndExecuteOperation", connectionRetries, operation, null);
    }

    Object methodExecute(String methodName, boolean convertStringArguments, Map<String, Object> namedInstances, Object... methodArguments) throws NoSuchMethodException {
        methodExecute("methodExecute", connectionRetries, methodName, convertStringArguments, namedInstances, methodArguments);
        Object rtv = waitLastExecutionResult();
        if (rtv instanceof Exception) {
            if (rtv instanceof InvocationTargetException)
                throw new  NoSuchMethodException(((InvocationTargetException) rtv).getMessage());
            if (rtv instanceof NoSuchInstantiatorException)
                throw new NoSuchMethodException(((NoSuchInstantiatorException) rtv).getMessage());
            throw handleException(rtv);
        } else {
            return rtv;
        }
    }

    /**
     * Executes a given method on this algorithm and returns the result.
     * @param methodName the name of the method to execute on the remote algorithm
     * @param methodArguments the arguments for the method
     * @return the method result or exception
     * @throws InvocationTargetException if the executed method throws an exception
     * @throws NoSuchInstantiatorException if the there is no method for the given name and prototype
     * @throws IllegalArgumentException if there was a problem reading the class in the remote algorithm's result
     */
    public Object methodExecute(String methodName, Object[] methodArguments) throws NoSuchMethodException {
        return methodExecute(methodName, false, null, methodArguments);
    }

    /**
     * Executes a given method on the remote algorithm and returns result.
     * If an exception is thrown on the other side, it is wrapped into IllegalStateException.
     * @param methodName the name of the method to execute on the remote algorithm
     * @param methodArguments the arguments for the method
     * @return the method result or exception
     * @throws IllegalStateException if there was a problem communicating with the remote algorithm
     * @throws IllegalArgumentException if there was a problem reading the class in the remote algorithm's result
     */
    private Object methodExecuteHandleException(String methodName, Object... methodArguments) throws NoSuchMethodException {
        Object rtv = methodExecute(methodName, false, null, methodArguments);
        if (rtv instanceof Exception)
            throw handleException(rtv);
        return rtv;
    }
    
    public OperationStatistics getOperationStatistics() throws NoSuchMethodException {
        return (OperationStatistics)methodExecuteHandleException("getOperationStatistics");
    }
    
}
