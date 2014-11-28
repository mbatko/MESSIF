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

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Uses a pool of RMI connections (see {@link RMIMethodExecutor}) to remote algorithm to simulate local algorithm. 
 *  The connections are used in a round robin manner.
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class RMIAlgorithmMultiThread extends RMIAlgorithm {
    /** class id for serialization */
    private static final long serialVersionUID = 659874590001L;

    /** Default number of connections to the RMI server to be used. */
    public static final int NUMBER_OF_CONNECTIONS = 5;
    
    //****************** Attributes ******************//

    /** Number of connections to be used by this this algorithm for multi-thread processing. */
    protected int numberOfConnections;
    
    //****************** Constructors ******************//

    /**
     * Creates a new instance of RMI algorithm.
     * @param host the remote algorithm's IP address
     * @param port the remote algorithm's RMI port
     * @param connectionRetries the number of reconnection tries if the RMI connection fails
     * @param numberOfConnections number of actual connections to the RMI server to be held and used
     */
    public RMIAlgorithmMultiThread(InetAddress host, int port, int connectionRetries, int numberOfConnections) {
        super(host, port, connectionRetries);
        this.numberOfConnections = numberOfConnections;
    }

    /**
     * Creates a new instance of RMI algorithm.
     * @param host the remote algorithm's IP address
     * @param port the remote algorithm's RMI port
     * @param connectionRetries the number of reconnection tries if the RMI connection fails
     */
    public RMIAlgorithmMultiThread(InetAddress host, int port, int connectionRetries) {
        this(host, port, connectionRetries, NUMBER_OF_CONNECTIONS);
    }

    /**
     * Creates a new instance of RMI algorithm.
     * @param host the remote algorithm's IP address
     * @param port the remote algorithm's RMI port
     */
    public RMIAlgorithmMultiThread(InetAddress host, int port) {
        this(host, port, 1);
    }

    /**
     * Creates a new instance of RMI algorithm.
     * @param host the remote algorithm's host name
     * @param port the remote algorithm's RMI port
     * @param numberOfConnections number of actual connections to the RMI server to be held and used
     * @throws UnknownHostException if the host name cannot be resolved to IP address
     */
    @AlgorithmConstructor(description = "creates an RMI algorithm stub", arguments = {"host", "RMI port", "number of connections"})
    public RMIAlgorithmMultiThread(String host, int port, int numberOfConnections) throws UnknownHostException {
        this(InetAddress.getByName(host), port, 1, numberOfConnections);
    }
    
    /**
     * Creates a new instance of RMI algorithm.
     * @param host the remote algorithm's host name
     * @param port the remote algorithm's RMI port
     * @throws UnknownHostException if the host name cannot be resolved to IP address
     */
    @AlgorithmConstructor(description = "creates an RMI algorithm stub", arguments = {"host", "RMI port"})
    public RMIAlgorithmMultiThread(String host, int port) throws UnknownHostException {
        this(InetAddress.getByName(host), port, NUMBER_OF_CONNECTIONS);
    }
    
    @Override
    public void finalize() throws Throwable {
        // Clean up connection
        disconnect();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return new RMIAlgorithmMultiThread(getHost(), getPort(), getConnectionRetries(), numberOfConnections);
    }

    
    //****************** Connection control methods ******************//

    /**
     * The number connections to the RMI server to be held and used.
     * @return return the number connections to the RMI server to be held and used
     */
    @Override
    public int getNumberOfConnections() {
        return numberOfConnections;
    }
    
}
