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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import messif.objects.LocalAbstractObject;
import messif.operations.AbstractOperation;
import messif.statistics.FutureWithStatistics;
import messif.statistics.OperationStatistics;
import messif.utility.Convert;
import messif.utility.reflection.NoSuchInstantiatorException;

/**
 * Uses a (set of) RMI connection(s) to remote algorithm to simulate local algorithm.
 * @see AlgorithmRMIServer
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class RMIAlgorithm extends Algorithm implements Cloneable {
    /** class id for serialization */
    private static final long serialVersionUID = 659874589001L;
    
    //****************** Attributes ******************//

    /** Remote algorithm's IP address */
    private final InetAddress host;

    /** Remote algorithm's RMI port */
    private final int port;

    /** Number of reconnection tries if the RMI connection fails */
    private final int connectionRetries;

    /** A synchronized queue of RMI connections to be used by this algorithm */
    private transient final BlockingDeque<RMIMethodExecutor> connectionQueue;

    /** A list of all connections - used especially to close them all during finalization. */
    private transient final Collection<RMIMethodExecutor> allConnections;
    
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
        this.allConnections = new ArrayList();
        this.connectionQueue = new LinkedBlockingDeque<>();
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
     * Returns the number of times the connection is retried, in case it returns IO exception.
     * @return the number of times the connection is retried, in case it returns IO exception.
     */
    public int getConnectionRetries() {
        return connectionRetries;
    }
    
    /**
     * For backwards compatibility, this RMI algorithm has only one connection.
     * @return return the number connections to the RMI server to be held and used
     */
    public int getNumberOfConnections() {
        return 1;
    }
    
    /**
     * Returns <tt>true</tt> if the algorithm is currently connected.
     * @return <tt>true</tt> if the algorithm is currently connected
     */
    public boolean isConnected() {
        return (! allConnections.isEmpty());
    }
    
    /**
     * Connects this algorithm to the RMI service.
     * @throws IOException if there was a problem connecting
     */
    public synchronized void connect() throws IOException {
        if (!isConnected()) {
            for (int i = 0; i < getNumberOfConnections(); i++) {
                allConnections.add(new RMIMethodExecutor(host, port));
            }
            connectionQueue.addAll(allConnections);
        }
    }

    /**
     * Disconnects this algorithm from the RMI service.
     */
    public synchronized void disconnect() {
        for (RMIMethodExecutor connection : allConnections) {
            connection.disconnect();
        }
    }


    //****************** Remote method invocation ******************//

    /**
     * Executes a given method on the remote algorithm and returns result.
     * One of the pooled connections to the server is used; if none is available, this method blocks and waits.
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
    protected Object methodExecute(String methodName, int reconnectRetries, Object... methodArguments) throws IllegalStateException, IllegalArgumentException {
        RMIMethodExecutor connection = null;
        try {
            if (! isConnected()) {
                connect();
            }
            connection = connectionQueue.takeFirst();
            return connection.methodExecute(methodName, reconnectRetries, methodArguments);
        } catch (InterruptedException | IOException ex) {
            return ex;
        } finally {
            if (connection != null) {
                connectionQueue.addFirst(connection);
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
    public AbstractOperation getRunningOperationById(UUID operationId) {
        return (AbstractOperation)methodExecuteHandleException("getRunningOperationById", operationId);
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
    public <T extends AbstractOperation> Future<T> backgroundExecuteOperation(T operation) {
        throw new UnsupportedOperationException("Background execution with waiting cannot be used on RMI algorithm");
    }

    @Override
    public <T extends AbstractOperation> FutureWithStatistics<T> backgroundExecuteOperationWithStatistics(T operation) {
        throw new UnsupportedOperationException("Background execution with waiting cannot be used on RMI algorithm");
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
