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
package messif.algorithms;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import messif.objects.LocalAbstractObject;
import messif.operations.AbstractOperation;
import messif.statistics.OperationStatistics;
import messif.utility.Convert;
import messif.utility.reflection.NoSuchInstantiatorException;

/**
 * Uses a RMI connection to remote algorithm to simulate local algorithm.
 * @see AlgorithmRMIServer
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class RMIAlgorithm extends Algorithm implements Cloneable {
    /** class id for serialization */
    private static final long serialVersionUID = 659874587001L;

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
    public RMIAlgorithm(InetAddress host, int port, int connectionRetries) {
        super(Convert.inetAddressToString(host, port));
        this.host = host;
        this.port = port;
        this.connectionRetries = connectionRetries;
    }

    /**
     * Creates a new instance of RMI algorithm.
     * @param host the remote algorithm's IP address
     * @param port the remote algorithm's RMI port
     */
    public RMIAlgorithm(InetAddress host, int port) {
        this(host, port, 1);
    }

    /**
     * Creates a new instance of RMI algorithm.
     * @param host the remote algorithm's host name
     * @param port the remote algorithm's RMI port
     * @param connectionRetries the number of reconnection tries if the RMI connection fails
     * @throws UnknownHostException if the host name cannot be resolved to IP address
     */
    public RMIAlgorithm(String host, int port, int connectionRetries) throws UnknownHostException {
        this(InetAddress.getByName(host), port, connectionRetries);
    }

    /**
     * Creates a new instance of RMI algorithm.
     * @param host the remote algorithm's host name
     * @param port the remote algorithm's RMI port
     * @throws UnknownHostException if the host name cannot be resolved to IP address
     */
    @AlgorithmConstructor(description = "creates an RMI algorithm stub", arguments = {"host", "RMI port"})
    public RMIAlgorithm(String host, int port) throws UnknownHostException {
        this(InetAddress.getByName(host), port);
    }

    @Override
    public void finalize() throws Throwable {
        // Clean up connection
        disconnect();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return new RMIAlgorithm(host, port, connectionRetries);
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
            } catch (IOException ignore) {} // The IO exceptions when closing connection are ignored
            socket = null;
            in = null;
            out = null;
        }
    }


    //****************** Remote method invocation ******************//

    /**
     * Executes a given method on the remote algorithm and returns result.
     * If an I/O error occurs during the communication, the connection is tried
     * to be reestablished for <code>reconnectRetries</code> times. After that
     * an {@link IllegalStateException} is thrown.
     * Note that the result returned might be an exception.
     * @param methodName the name of the method to execute on the remote algorithm
     * @param reconnectRetries the number of reconnection retries if there is an
     *          {@link IOException} while establishing the RMI connection
     * @param methodArguments the arguments for the method
     * @return the method result or exception
     * @throws IllegalStateException if there was a problem communicating with the remote algorithm
     * @throws IllegalArgumentException if there was a problem reading the class in the remote algorithm's result
     */
    private synchronized Object methodExecute(String methodName, int reconnectRetries, Object... methodArguments) throws IllegalStateException, IllegalArgumentException {
        for (;;) {
            try {
                connect(); // Does nothing if already connected
                out.writeUTF(methodName);
                out.writeUnshared(methodArguments);
                out.reset();
                out.flush();
                return in.readUnshared();
            } catch (IOException e) {
                disconnect();
                if (reconnectRetries-- <= 0)
                    throw new IllegalStateException("Error communicating with remote algorithm", e);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Cannot read result of '" + methodName + "': " + e);
            }
        }
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
    private Object methodExecuteHandleException(String methodName, Object... methodArguments) throws IllegalArgumentException, IllegalStateException {
        Object rtv = methodExecute(methodName, connectionRetries, methodArguments);
        if (rtv instanceof Exception)
            throw handleException(rtv);
        return rtv;
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
            return (RuntimeException)exception;
        } else {
            // Unwrap exception
            Throwable ex = (Exception)exception;
            while (ex.getCause() != null)
                ex = ex.getCause();

            // Wrap into runtime exception of illegal state
            return new IllegalArgumentException(ex);
        }
    }


    //****************** Wrappers for algorithm methods ******************//

    @Override
    public String getName() {
        return (String)methodExecuteHandleException("getName");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends LocalAbstractObject> getObjectClass() {
        return (Class<? extends LocalAbstractObject>)methodExecuteHandleException("getObjectClass"); // This cast IS checked
    }

    @Override
    public int getRunningOperationsCount() {
        return (Integer)methodExecuteHandleException("getRunningOperationsCount");
    }

    @Override
    public AbstractOperation getRunningOperationByThread(Thread thread) {
        throw new UnsupportedOperationException("This operation cannot be called via RMI");
    }

    @Override
    public AbstractOperation getRunningOperationById(UUID operationId) {
        return (AbstractOperation)methodExecuteHandleException("getRunningOperationById", operationId);
    }

    @Override
    public AbstractOperation getRunningOperation() {
        return (AbstractOperation)methodExecuteHandleException("getRunningOperation");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<AbstractOperation> getAllRunningOperations() {
        return (Collection<AbstractOperation>)methodExecuteHandleException("getAllRunningOperations");
    }

    @Override
    public OperationStatistics getOperationStatistics() {
        return (OperationStatistics)methodExecuteHandleException("getOperationStatistics");
    }

    @Override
    public void resetOperationStatistics() {
        methodExecuteHandleException("resetOperationStatistics");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Class<? extends AbstractOperation>> getSupportedOperations() {
        return (List<Class<? extends AbstractOperation>>)methodExecuteHandleException("getSupportedOperations"); // This cast IS checked
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractOperation> List<Class<? extends T>> getSupportedOperations(Class<? extends T> subclassToSearch) {
        return (List<Class<? extends T>>)methodExecuteHandleException("getSupportedOperations", subclassToSearch); // This cast IS checked
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractOperation> T executeOperation(T operation) throws AlgorithmMethodException, NoSuchMethodException {
        Object rtv = methodExecute("executeOperation", connectionRetries, operation);
        if (rtv instanceof Exception) {
            if (rtv instanceof AlgorithmMethodException)
                throw (AlgorithmMethodException)rtv;
            if (rtv instanceof NoSuchMethodException)
                throw (NoSuchMethodException)rtv;
            throw handleException(rtv);
        } else {
            return (T)rtv;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractOperation> T setupStatsAndExecuteOperation(T operation, String operationStatsRegexp) throws AlgorithmMethodException, NoSuchMethodException {
        Object rtv = methodExecute("setupStatsAndExecuteOperation", connectionRetries, operation, operationStatsRegexp);
        if (rtv instanceof Exception) {
            if (rtv instanceof AlgorithmMethodException)
                throw (AlgorithmMethodException)rtv;
            if (rtv instanceof NoSuchMethodException)
                throw (NoSuchMethodException)rtv;
            throw handleException(rtv);
        } else {
            return (T)rtv;
        }
    }

    @Override
    public void backgroundExecuteOperationIndependent(AbstractOperation operation) {
         methodExecuteHandleException("backgroundExecuteOperationIndependent", operation);
    }

    @Override
    Object methodExecute(String methodName, boolean convertStringArguments, Map<String, Object> namedInstances, Object... methodArguments) throws InvocationTargetException, NoSuchInstantiatorException, IllegalArgumentException {
        Object rtv = methodExecute(
                "methodExecute", connectionRetries, methodName, convertStringArguments,
                // FIXME: This is a partial fix that allows to call no-arg methods on RMI algorithm even when namedInstances contain non-serializable items
                convertStringArguments && methodArguments.length > 0 ? namedInstances : null,
                methodArguments
        );
        if (rtv instanceof Exception) {
            if (rtv instanceof InvocationTargetException)
                throw (InvocationTargetException)rtv;
            if (rtv instanceof NoSuchInstantiatorException)
                throw (NoSuchInstantiatorException)rtv;
            throw handleException(rtv);
        } else {
            return rtv;
        }
    }
}
