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
package messif.utility;

import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import messif.algorithms.Algorithm;
import messif.executor.MethodExecutor.ExecutableMethod;
import messif.network.NetworkNode;



/**
 * Provides extended network-related support to {@link CoreApplication}.
 * Specifically, support for transferring {@link NetworkNode}s to other machines
 * when algorithms are {@link #algorithmRestore(java.io.PrintStream, java.lang.String[]) restored}
 * and a distributed network controller registration support are added.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class DistributedApplication extends CoreApplication {

    /**
     * Restores a previously serialized algorithm from file.
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; algorithmRestore /some/where/file/algorithm.serialized
     * </pre>
     * 
     * Optionally, additional parameter can specify a file with network identification translation.
     * It is a simple text file with format "original_address:original_port=new_address:new_port".
     * If the ports are unspecified, only addresses are translated.
     * 
     * @param out a stream where the application writes information for the user
     * @param args file name with the serialized algorithm and, optionally, port remaping file
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     * @see #algorithmStore
     */
    @ExecutableMethod(description = "loads the algorithm from a given file", arguments = {"file name", "host remap file (not required)" })
    @Override
    public boolean algorithmRestore(PrintStream out, String... args) {
        try {
            if (args.length > 2) {
                try {
                    if (args[2] != null && args[2].length() > 0)
                        NetworkNode.loadHostMappingTable(args[2]);
                } catch (UnknownHostException e) {
                    out.println("Error parsing host remap file: unknown host " + e.getMessage());
                    return false;
                }
            }

            // Load algorithm from file
            algorithm = Algorithm.restoreFromFile(args[1]);
            algorithms.add(algorithm);

            // Reset host mapping table
            NetworkNode.resetHostMappingTable();

            return true;
        } catch (IOException e) {
            out.println(e.toString());
        } catch (ClassNotFoundException e) {
            out.println(e.toString());            
        }
        return false;
    }

    /** Thread for sending i-am-alive packets to the peer control center. */
    private static class ControllerKeepaliveThread extends Thread {
        /** Socket used for sending data */
        protected final DatagramSocket socket;
        /** Address of the peer control center to which the packets are sent */
        protected final NetworkNode remoteHost;
        /** Time between sending packets in miliseconds */
        protected long timeout;

        /**
         * Creates a new instance of ControllerKeepaliveThread.
         * @param localPort the local port used for sending data
         * @param remoteHost the address of the peer control center to which the packets are sent
         * @param timeout the time between sending packets in miliseconds
         * @throws SocketException if the sending socket cannot be opened on the specified port
         */
        public ControllerKeepaliveThread(int localPort, NetworkNode remoteHost, long timeout) throws SocketException {
            super("thApplicationControllerKeepalive");
            this.socket = new DatagramSocket(localPort);
            this.remoteHost = remoteHost;
            this.timeout = timeout;            
        }

        @Override
        public void run() {
            try {
                DatagramPacket packet = new DatagramPacket(new byte[] { 1 }, 1, remoteHost.getHost(), remoteHost.getPort());
                try {
                    for (;;) {
                        synchronized (this) {
                            socket.send(packet);
                            wait(timeout);
                        }
                    }
                } catch (InterruptedException e) {
                    // Interrupted means exiting - send exit notification
                    socket.send(new DatagramPacket(new byte[] { 0 }, 1, remoteHost.getHost(), remoteHost.getPort()));
                }
            } catch (IOException e) {
            }
        }
    }


    //****************** Standalone application's main method ******************//

    @Override
    protected String usage() {
        return "[-register <host>:<port>] " + super.usage();
    }

    @Override
    protected boolean parseArguments(String[] args, int argIndex) {
        // Register to central controller
        if (args.length > argIndex && args[argIndex].equalsIgnoreCase("-register")) {
            argIndex++;
            if (cmdSocket != null)
                try {
                    new ControllerKeepaliveThread(cmdSocket.socket().getLocalPort(), Convert.stringToType(args[argIndex], NetworkNode.class), 60000).start();
                    argIndex++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            else System.err.println("Can't register if there is no communication interface!");
        }

        return super.parseArguments(args, argIndex);
    }

    /**
     * Start a MESSIF application.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // Create new instance of application
        new DistributedApplication().startApplication(args);
    }

}
