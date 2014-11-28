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
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Uses a RMI connection to remote algorithm to execute methods on the distant algorithm.
 * @see AlgorithmRMIServer
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class RMIMethodExecutor implements Cloneable {

    //****************** Attributes ******************//

    /** Remote algorithm's IP address */
    private final InetAddress host;

    /** Remote algorithm's RMI port */
    private final int port;

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
     */
    public RMIMethodExecutor(InetAddress host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Creates a new instance of RMI algorithm.
     * @param host the remote algorithm's host name
     * @param port the remote algorithm's RMI port
     * @throws UnknownHostException if the host name cannot be resolved to IP address
     */
    public RMIMethodExecutor(String host, int port) throws UnknownHostException {
        this(InetAddress.getByName(host), port);
    }

    @Override
    public void finalize() throws Throwable {
        // Clean up connection
        disconnect();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return new RMIMethodExecutor(host, port);
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
    protected synchronized Object methodExecute(String methodName, int reconnectRetries, Object... methodArguments) throws IllegalStateException, IllegalArgumentException {
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

}
