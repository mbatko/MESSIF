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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import messif.algorithms.Algorithm;
import messif.algorithms.AlgorithmMethodException;
import messif.algorithms.RMIAlgorithm;
import messif.network.NetworkNode;
import messif.operations.AbstractOperation;

/**
 * This is a centralized algorithm which connects itself to several remote algorithm (host + RMI port)
 *  and executes operations on all of them.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class MultipleOverlaysAlgorithm extends Algorithm {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    /** Multiple remote algorithms. */
    Map<Object, RMIAlgorithm> overlays = new HashMap<Object, RMIAlgorithm>();

    /**
     * Constructs the algorithm with empty set of overlays.
     * 
     * @throws java.lang.IllegalArgumentException
     */
    @Algorithm.AlgorithmConstructor(description = "Basic empty constructor", arguments = {})
    public MultipleOverlaysAlgorithm() throws IllegalArgumentException {
        super("Multioverlay Algorithm");
    }

    /**
     * Constructs the algorithm for a given list of overlays.
     * 
     * @param overlays array of host+RMI port values to connect to
     * @throws java.lang.IllegalArgumentException
     */
    @Algorithm.AlgorithmConstructor(description = "Constructor with given list of host+port ids of algorithms", arguments = {"list of host+port identifications"})
    public MultipleOverlaysAlgorithm(NetworkNode [] overlays) throws IllegalArgumentException {
        super("Multioverlay Algorithm");

        try {
            for (int i = 0; i < overlays.length; i++) {
                addAlgorithm(overlays[i]);
            }
        } catch (AlgorithmMethodException e) {
            log.log(Level.WARNING, e.getClass().toString(), e);
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Disconnect from all overlays.
     * 
     * @throws java.lang.Throwable if any disconnection goes wrong.
     */
    @Override
    public void finalize() throws Throwable {
        super.finalize();
        for (RMIAlgorithm rMIAlgorithm : overlays.values()) {
            rMIAlgorithm.finalize();
        }
    }


    // ************************************     Overlays management     *********************************** //

    /**
     * Add a new remote algorithm stub to the list of overlays - generate a random new identifier
     * @param networkNode
     * @throws messif.algorithms.AlgorithmMethodException
     */
    public final void addAlgorithm(NetworkNode networkNode) throws AlgorithmMethodException {
        addAlgorithm(networkNode.toString(), networkNode);
    }
    
    /**
     * Add a new remote algorithm stub to the
     * @param identifier overlay identifier
     * @param networkNode
     * @throws messif.algorithms.AlgorithmMethodException
     */
    public void addAlgorithm(String identifier, NetworkNode networkNode) throws AlgorithmMethodException {
        try {
            RMIAlgorithm newAlgorithm = new RMIAlgorithm(networkNode.getHost(), networkNode.getPort());
            log.log(Level.INFO, "adding: {0}: {1}", new Object[]{identifier, networkNode});
            newAlgorithm.connect();
            log.info("... connected");
            overlays.put(identifier, newAlgorithm);
        } catch (IOException e) {
            log.log(Level.WARNING, e.getClass().toString(), e);
            throw new AlgorithmMethodException(e);
        }
    }

    /**
     * Remove given algorithm (host+RMI port) from this multiple-overlays algorithm.
     * 
     * @param networkNode host+RMI port to remove
     * @return true if the specified algorithm was managed by this alg, false otherwise
     */
    public boolean removeAlgorithm(NetworkNode networkNode) {
        for (Map.Entry<Object, RMIAlgorithm> pair : overlays.entrySet()) {
            if (pair.getValue().getHost().equals(networkNode.getHost()) && (pair.getValue().getPort() == networkNode.getPort())) {
                try {
                    pair.getValue().finalize();
                } catch (Throwable e) {
                    log.log(Level.WARNING, e.getClass().toString(), e);
                }
                overlays.remove(pair.getKey());
                return true;
            }
        }
        return false;
    }

    // *******************************     Operations     *********************************** //

    /**
     * This method processes the operation on each of the remote algorithms (running each of it in a separate thread)
     *   and merges the answers.
     * @param operation operation to execute on each of the overlays
     * @throws AlgorithmMethodException if there was an exception during the background execution
     * @throws InterruptedException if the waiting was interrupted
     */
    public void processOperation(AbstractOperation operation) throws AlgorithmMethodException, InterruptedException {
        try {
            
            // execute operation at all remote algorithms (background)
            for (RMIAlgorithm rMIAlgorithm : overlays.values()) {
                rMIAlgorithm.backgroundExecuteOperation(operation);
            }

            // wait for the results from all overlays and merge them
            for (RMIAlgorithm rMIAlgorithm : overlays.values()) {
                List<AbstractOperation> returnedOperation = rMIAlgorithm.waitBackgroundExecuteOperation();
                for (AbstractOperation abstractOperation : returnedOperation) {
                    operation.updateFrom(abstractOperation);
                }
            }
            
            // merge the statistics
//            for (OperationStatistics operationStatistics : returnedStatistics) {
//                operationStatistics.removeStatistic("OperationTime");
//                OperationStatistics.getLocalThreadStatistics().updateFrom(operationStatistics);
//            }
            
        } catch (NoSuchMethodException e) {
            log.log(Level.WARNING, e.getClass().toString(), e);
            throw new AlgorithmMethodException(e);
        } catch (ClassCastException e) {
            log.log(Level.WARNING, e.getClass().toString(), e);
            throw new AlgorithmMethodException(e);
        }
    }

}
