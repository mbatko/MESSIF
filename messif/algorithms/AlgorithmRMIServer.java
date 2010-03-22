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
import java.util.logging.Level;
import java.util.logging.Logger;
import messif.utility.Clearable;
import messif.utility.reflection.Instantiators;

/**
 * Encapsulates an algorithm with an RMI server.
 * The server starts a new thread for every incoming connection.
 * The connection handling is done in the {@link #run()} method, which can
 * be run in a separate thread or directly by the caller.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class AlgorithmRMIServer extends Thread {
    /** Logger */
    private static Logger log = Logger.getLogger("rmi");

    /** Incoming connections socket */
    private final ServerSocketChannel socket;

    /** Encapsulated algorithm */
    private final Algorithm algorithm;

    /**
     * Creates a new instance of AlgorithmRMIServer listening on the specified port.
     * @param algorithm the algorithm to encapsulate
     * @param port the TCP port of the RMI service
     * @throws NullPointerException if the specified algorithm is <tt>null</tt>
     * @throws IOException if the RMI service cannot be opened on the specified port
     */
    public AlgorithmRMIServer(Algorithm algorithm, int port) throws NullPointerException, IOException {
        super("RMIServerThread");
        if (algorithm == null)
            throw new NullPointerException("Algorithm cannot be null");
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
                            out.flush();
                            ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
                            Class<? extends Algorithm> algorithmClass = algorithm.getClass();

                            for (;;) {
                                String methodName = in.readUTF();
                                Object[] methodArguments;
                                try {
                                    methodArguments = (Object[]) in.readUnshared();
                                } catch (ClassNotFoundException e) {
                                    log.severe("Received unknown class from RMI client: " + e.getMessage());
                                    out.writeUnshared(e);
                                    break;
                                }
                                try {
                                    Object retVal = Instantiators.getMethod(algorithmClass, methodName, false, null, methodArguments).invoke(algorithm, methodArguments);
                                    if (retVal instanceof Clearable)
                                        ((Clearable)retVal).clearSurplusData();
                                    out.writeUnshared(retVal);
                                } catch (InvocationTargetException e) {
                                    out.writeUnshared(e.getCause());
                                } catch (NoSuchMethodException e) {
                                    out.writeUnshared(e);
                                } catch (IllegalAccessException e) {
                                    out.writeUnshared(e);
                                } catch (RuntimeException e) {
                                    out.writeUnshared(e);
                                }
                                out.reset();
                                out.flush();
                            }
                        } catch (ClosedByInterruptException e) {
                            // Exit this thread by interruption
                        } catch (EOFException e) {
                            // Connection closed, exiting
                        } catch (IOException e) {
                            log.warning("Error communicating with RMI client: " + e);
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
            log.log(Level.SEVERE, e.getClass().toString(), e);
        }
    }

}
