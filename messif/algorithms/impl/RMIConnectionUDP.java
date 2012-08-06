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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Map;
import messif.network.NetworkNode;
import messif.operations.AbstractOperation;
import messif.utility.reflection.NoSuchInstantiatorException;

/**
 * Uses a RMI connection to remote algorithm to simulate local algorithm.
 * @see AlgorithmRMIServer
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class RMIConnectionUDP implements Cloneable {

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
    //private transient Socket socket;
    private transient DatagramSocket udpSocket;

    
    
    /** RMI connection output stream for sending method invocations to the remote algorithm */
    //private transient ObjectOutputStream out;
    //private transient OutputStream out;
    
    private transient ByteArrayOutputStream outByteArrayStream;

    /** RMI connection input stream for reading remote algorithm results */
    //private transient InputStream in;

    private transient byte [] inByteArray;
    
    //****************** Constructors ******************//
    /**
     * Creates a new instance of RMI algorithm.
     * @param host the remote algorithm's IP address
     * @param port the remote algorithm's RMI port
     * @param connectionRetries the number of reconnection tries if the RMI connection fails
     */
    public RMIConnectionUDP(InetAddress host, int port, int connectionRetries) {
        this.host = host;
        this.port = port;
        this.connectionRetries = connectionRetries;
    }

    /**
     * Creates a new instance of RMI algorithm.
     * @param host the remote algorithm's IP address
     * @param port the remote algorithm's RMI port
     */
    public RMIConnectionUDP(InetAddress host, int port) {
        this(host, port, 1);
    }

    /**
     * Creates a new instance of RMI algorithm.
     * @param host the remote algorithm's host name
     * @param port the remote algorithm's RMI port
     * @param connectionRetries the number of reconnection tries if the RMI connection fails
     * @throws UnknownHostException if the host name cannot be resolved to IP address
     */
    public RMIConnectionUDP(String host, int port, int connectionRetries) throws UnknownHostException {
        this(InetAddress.getByName(host), port, connectionRetries);
    }

    /**
     * Creates a new instance of RMI algorithm.
     * @param host the remote algorithm's host name
     * @param port the remote algorithm's RMI port
     * @throws UnknownHostException if the host name cannot be resolved to IP address
     */
    public RMIConnectionUDP(String host, int port) throws UnknownHostException {
        this(InetAddress.getByName(host), port);
    }

    /**
     * Creates a new instance of RMI algorithm.
     * @param networkNode host + RMI port of the remote algorithm
     */
    public RMIConnectionUDP(NetworkNode networkNode) {
        this(networkNode.getHost(), networkNode.getPort());
    }

    @Override
    public void finalize() throws Throwable {
        // Clean up connection
        disconnect();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return new RMIConnectionUDP(host, port, connectionRetries);
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
        return (udpSocket != null) && udpSocket.isConnected();
    }

    /**
     * Connects this algorithm to the RMI service.
     * @throws IOException if there was a problem connecting
     */
    public synchronized void connect() throws IOException {
        if (!isConnected()) {
            //udpSocket = new DatagramSocket(port, host);
            udpSocket = new DatagramSocket(port, InetAddress.getLocalHost());
            //out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
//            out = new BufferedOutputStream(socket.getOutputStream());
//            out.flush();
            outByteArrayStream = new ByteArrayOutputStream(AlgorithmRMIServerUDP.MAX_UDP_SIZE);
            inByteArray = new byte[AlgorithmRMIServerUDP.MAX_UDP_SIZE];
            
            //in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
//            in = new BufferedInputStream(socket.getInputStream());
        }
    }

    /**
     * Disconnects this algorithm from the RMI service.
     */
    public synchronized void disconnect() {
        if (udpSocket != null) {
//            try {
                udpSocket.close();
//            } catch (IOException ignore) {
//            } // The IO exceptions when closing connection are ignored
            udpSocket = null;
            //in = null;
            //out = null;
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
                long timeStamp = System.currentTimeMillis();
                connect(); // Does nothing if already connected
                
                writeDataToSocket(methodName, methodArguments);
        
                StringBuilder string = new StringBuilder("writing time: ").append(System.currentTimeMillis() - timeStamp);
                System.out.println(string.toString());
                
                return;
            } catch (IOException e) {
                disconnect();
                if (reconnectRetries-- <= 0)
                    throw new IllegalStateException("Error communicating with remote algorithm", e);
            }
        }
    }

    
    private ObjectInputStream readDataFromSocket() throws IOException {
        DatagramPacket packet = new DatagramPacket(inByteArray, inByteArray.length);

        // Receive packet
        udpSocket.receive(packet);

        // Create input stream
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(packet.getData()));
        return in;
    }

    private void writeDataToSocket(Object ... data) throws IOException {
        outByteArrayStream.reset();
        ObjectOutputStream objectStream = new ObjectOutputStream(outByteArrayStream);
        for (Object object : data) {
            objectStream.writeObject(object);
        }
        objectStream.close();
        byte[] byteArray = outByteArrayStream.toByteArray();
        udpSocket.send(new DatagramPacket(byteArray, byteArray.length, host, port));
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

    public Object waitLastExecutionResult() throws IllegalArgumentException, IllegalStateException {
        try {
            long timeStamp = System.currentTimeMillis();
            StringBuilder string = new StringBuilder();
//            while (socket.getInputStream().available() <= 1) {
//                string.append(", in while: ").append(socket.getInputStream().available());
//                try {
//                    Thread.sleep(1);
//                } catch (InterruptedException ignore) {
//                }
//            }
//            string.append(", after while: ").append(socket.getInputStream().available()).append('\n');
//            string.append(", waiting: ").append(System.currentTimeMillis() - timeStamp);
//            timeStamp = System.currentTimeMillis();
//            Object retVal = in.readUnshared();
            Object retVal = readDataFromSocket().readUnshared();
            
            string.append(", waiting & reading: ").append(System.currentTimeMillis() - timeStamp);
            System.out.println(string.toString());
            return retVal;
        } catch (IOException e) {
            disconnect();
            throw new IllegalStateException("Error communicating with remote algorithm", e);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot read result of the last executed method: " + e);
        }
    }

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
        methodExecute("executeOperation", connectionRetries, operation);
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
        methodExecute("executeOperation", connectionRetries, operation);
    }

    Object methodExecute(String methodName, boolean convertStringArguments, Map<String, Object> namedInstances, Object... methodArguments) throws InvocationTargetException, NoSuchInstantiatorException, IllegalArgumentException {
        methodExecute("methodExecute", connectionRetries, methodName, convertStringArguments, namedInstances, methodArguments);
        Object rtv = waitLastExecutionResult();
        if (rtv instanceof Exception) {
            if (rtv instanceof InvocationTargetException)
                throw (InvocationTargetException) rtv;
            if (rtv instanceof NoSuchInstantiatorException)
                throw (NoSuchInstantiatorException) rtv;
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
    public Object methodExecute(String methodName, Object[] methodArguments) throws InvocationTargetException, NoSuchInstantiatorException, IllegalArgumentException {
        return methodExecute(methodName, false, null, methodArguments);
    }
}
