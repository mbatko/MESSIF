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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import messif.algorithms.AlgorithmRMIServer;
import messif.executor.MethodExecutor.ExecutableMethod;


/**
 * Extends the {@link DistributedApplication} with RMI support.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class Application extends DistributedApplication {

    /** List of RMI services for algorithms */
    private final List<AlgorithmRMIServer> rmiServers = new ArrayList<AlgorithmRMIServer>();

    /**
     * Creates an RMI service for the current algorithm.
     * An argument specifying the RMI TCP port is required.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; rmiStart 12345
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args operation class followed by constructor arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "create RMI service for the current algorithm", arguments = { "TCP port", "flag whether to clear surplus data (defaults to true)" })
    public boolean rmiStart(PrintStream out, String... args) {
        if (!hasAlgorithm()) {
            out.println("No running algorithm is selected");
            return false;
        }

        // Read port
        int port;
        try {
            port = Integer.parseInt(args[1]);
        } catch (RuntimeException ignore) {
            out.println("Invalid port specified");
            return false;
        }

        boolean clearSurplusData = true;
        if (args.length >= 3 && args[2].equalsIgnoreCase("false"))
            clearSurplusData = false;
        
        // Create RMI service
        try {
            AlgorithmRMIServer rmiServer = new AlgorithmRMIServer(getAlgorithm(), port, clearSurplusData);
            rmiServers.add(rmiServer);
            rmiServer.start();
            return true;
        } catch (Exception e) {
            logException(e);
            out.println("Cannot open RMI service: " + e);
            return false;
        }
    }

    /**
     * Destroys all RMI services for the current algorithm.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; rmiStop
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args operation class followed by constructor arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "destroy RMI service for the current algorithm", arguments = {})
    public boolean rmiStop(PrintStream out, String... args) {
        Iterator<AlgorithmRMIServer> iterator = rmiServers.iterator();
        while (iterator.hasNext()) {
            AlgorithmRMIServer rmiServer = iterator.next();
            if (getAlgorithm() == rmiServer.getAlgorithm()) {
                rmiServer.interrupt();
                iterator.remove();
            }
        }
        return true;
    }

    /**
     * Destroys all RMI services for all running algorithms.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; rmiStop
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args operation class followed by constructor arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "destroy RMI services for all running algorithms", arguments = {})
    public boolean rmiStopAll(PrintStream out, String... args) {
        Iterator<AlgorithmRMIServer> iterator = rmiServers.iterator();
        while (iterator.hasNext()) {
            iterator.next().interrupt();
            iterator.remove();
        }
        return true;
    }

    /**
     * Shows information about ports of the RMI services of the current algorithm.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; rmiInfo
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args operation class followed by constructor arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "shows information about RMI services for the current algorithm", arguments = {})
    public boolean rmiInfo(PrintStream out, String... args) {
        if (!hasAlgorithm()) {
            out.println("No running algorithm is selected");
            return false;
        }

        int count = 0;
        for (AlgorithmRMIServer rmiServer : rmiServers) {
            if (getAlgorithm() == rmiServer.getAlgorithm()) {
                out.println("RMI service is started at port " + rmiServer.getPort());
                count++;
            }
        }

        if (count == 0)
            out.println("There is no RMI service for current algorithm");

        return true;
    }

    /**
     * Shows information about all RMI services for all running algorithms.
     * The information about RMI port and the algorithm name is printed.
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; rmiInfoAll
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args operation class followed by constructor arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "shows information about all RMI services", arguments = {})
    public boolean rmiInfoAll(PrintStream out, String... args) {
        if (rmiServers.isEmpty()) {
            out.println("There are no started RMI services");
        } else {
            for (AlgorithmRMIServer rmiServer : rmiServers) {
                out.print("RMI service (port ");
                out.print(rmiServer.getPort());
                out.print(") started for ");
                out.println(rmiServer.getAlgorithm().getName());
            }
        }

        return true;
    }

    /**
     * Start a MESSIF application.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // Create new instance of application
        new Application().startApplication(args);
    }

}
