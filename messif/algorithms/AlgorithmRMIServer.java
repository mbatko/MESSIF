/*
 * AlgorithmRMIServer
 */
package messif.algorithms;

import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import messif.operations.AbstractOperation;
import messif.utility.Convert;
import messif.utility.Logger;

/**
 * Encapsulates an algorithm with an RMI server.
 * The server starts a new thread for every incoming connection.
 * The connection handling is done in the {@link #run()} method, which can
 * be run in a separate thread or directly by the caller.
 * 
 * @author xbatko
 */
public class AlgorithmRMIServer extends Thread {
    /** Logger */
    private static Logger log = Logger.getLoggerEx("rmi");

    /** Incoming connections socket */
    private final ServerSocketChannel socket;

    /** Encapsulated algorithm */
    private final Algorithm algorithm;

    /**
     * Creates a new instance of AlgorithmRMIServer listening on the specified port.
     * @param algorithm the algorithm to encapsulate
     * @param port the TCP port of the RMI service
     * @throws IOException if the RMI service cannot be opened on the specified port
     */
    public AlgorithmRMIServer(Algorithm algorithm, int port) throws IOException {
        super("RMIServerThread");
        this.algorithm = algorithm;
        socket = ServerSocketChannel.open();
        socket.socket().bind(new InetSocketAddress(port));
        socket.configureBlocking(true);
    }

    /**
     * Returns the encapsulated algorithm.
     * @return the encapsulated algorithm
     */
    public Algorithm getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns the TCP port of this RMI service.
     * @return the TCP port of this RMI service
     */
    public int getPort() {
        return socket.socket().getLocalPort();
    }

    /**
     * Waits for incoming connections and start a new thread for each.
     */
    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                // Get a connection (blocking mode)
                final Socket connection = socket.accept().socket();
                new Thread("RMIServerConnectionThread") {
                    @Override
                    public void run() {
                        try {
                            ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(connection.getOutputStream()));
                            ObjectInputStream in = new ObjectInputStream(connection.getInputStream());

                            for (;;) {
                                String methodName = in.readUTF();
                                Object[] methodArguments = (Object[]) in.readObject();
                                try {
                                    if (algorithm == null) {
                                        out.writeObject(null);
                                    } else if (methodName.equals("executeOperation") && methodArguments.length > 0 && AbstractOperation.class.isInstance(methodArguments[0])) {
                                        AbstractOperation operation = (AbstractOperation) methodArguments[0];
                                        algorithm.executeOperation(operation);
                                        operation.clearSurplusData();
                                        out.writeObject(operation);
                                    } else {
                                        out.writeObject(algorithm.getClass().getMethod(methodName, Convert.getObjectTypes(methodArguments)).invoke(algorithm, methodArguments));
                                    }
                                } catch (AlgorithmMethodException e) {
                                    out.writeObject(e);
                                } catch (InvocationTargetException e) {
                                    out.writeObject(e.getCause());
                                } catch (NoSuchMethodException e) {
                                    out.writeObject(e);
                                } catch (IllegalAccessException e) {
                                    out.writeObject(e);
                                } catch (RuntimeException e) {
                                    out.writeObject(e);
                                }
                                out.flush();
                            }
                        } catch (ClosedByInterruptException e) {
                            // Exit this thread by interruption
                        } catch (EOFException e) {
                            // Connection closed, exiting
                        } catch (IOException e) {
                            log.warning("Error communicating with RMI client: " + e);
                        } catch (ClassNotFoundException e) {
                            log.severe("Received unknown class from RMI client: " + e.getMessage());
                        } finally {
                            // ignore exceptions when closing
                            try {
                                connection.close();
                            } catch (IOException ignore) {
                            } 
                        }
                    }
                }.start();
            }
        } catch (ClosedByInterruptException e) {
            // Exit this thread by interruption
        } catch (IOException e) {
            log.severe(e);
        }
    }

}
