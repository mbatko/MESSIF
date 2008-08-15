/*
 * RMIAlgorithm
 */

package messif.algorithms;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import messif.operations.AbstractOperation;
import messif.statistics.OperationStatistics;

/**
 * Uses a RMI connection to remote algorithm to simulate local algorithm.
 * @see AlgorithmRMIServer
 * @author xbatko
 */
public class RMIAlgorithm extends Algorithm {
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
        super(null);
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
            in = new ObjectInputStream(socket.getInputStream());
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
    private synchronized Object methodExecute(String methodName, int reconnectRetries, Object... methodArguments) throws IllegalStateException, IllegalArgumentException {
        for (;;) {
            try {
                connect(); // Does nothing if already connected
                out.writeUTF(methodName);
                out.writeObject(methodArguments);
                out.flush();
                return in.readObject();
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
    private Object methodExecute(String methodName, Object... methodArguments) throws IllegalArgumentException, IllegalStateException {
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
        return (String)methodExecute("getName");
    }

    @Override
    public int getRunningOperationsCount() {
        return (Integer)methodExecute("getRunningOperationsCount");
    }

    @Override
    public OperationStatistics getOperationStatistics() {
        return (OperationStatistics)methodExecute("getOperationStatistics");
    }

    @Override
    public void resetOperationStatistics() {
        methodExecute("resetOperationStatistics");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Class<AbstractOperation>> getSupportedOperations() {
        return (List<Class<AbstractOperation>>)methodExecute("getSupportedOperations"); // This cast IS checked
    }
    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractOperation> List<Class<T>> getSupportedOperations(Class<T> subclassToSearch) {
        return (List<Class<T>>)methodExecute("getSupportedOperations", subclassToSearch); // This cast IS checked
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

}
