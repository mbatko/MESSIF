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
package messif.operations.query;

import java.lang.reflect.InvocationTargetException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import messif.algorithms.AlgorithmMethodException;
import messif.algorithms.RMIAlgorithm;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.util.RankedAbstractObject;
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;
import messif.operations.RankingQueryOperation;
import messif.utility.reflection.ConstructorInstantiator;
import messif.utility.reflection.NoSuchInstantiatorException;

/**
 * Similarity join query operation evaluated using range queries on an external index.
 * 
 * It works as documented at {@link #evaluate(messif.objects.util.AbstractObjectIterator) }.
 * 
 * See {@link JoinQueryOperation} for details.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("similairity join query by range search")
public class RangeJoinQueryOperation extends JoinQueryOperation {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//
    
    /** Remote algorithm for evaluating "range" queries */
    private final RMIAlgorithm remoteAlgo;
    
    /** Cosntructor for the operation to execute on the remote algorithm */
    private final ConstructorInstantiator<RankingQueryOperation> operConstructor;
    
    /** Paramaters to instantiate the operation {@link #operConstructor} */
    private final Object[] operParams;

    //****************** Constructors ******************//

    /**
     * Creates an instance of range join query.
     * 
     * @param mu the distance threshold
     * @param k the number of nearest pairs to retrieve
     * @param skipSymmetricPairs flag whether symmetric pairs should be avoided in the answer
     * @param answerType the type of objects this operation stores in pairs in its answer
     * @param host the remote algorithm's IP address
     * @param port the remote algorithm's RMI port
     * @param queryCls name of class of {@link RankingQueryOperation} to execute on the algorithm at host:port;
     *                 so {@link RangeQueryOperation} or {@link KNNQueryOperation} can be passed
     * @param queryParams parameters of a constructor of queryCls but the first one, which is a query object
     * @throws UnknownHostException thrown during the construction of {@link RMIAlgorithm}
     * @throws NoSuchMethodException thrown during the construction of the passed class of {@link RankingQueryOperation}
     * @throws IllegalArgumentException thrown during the construction of the passed class of {@link RankingQueryOperation}
     * @throws InvocationTargetException thrown during the construction of the passed class of {@link RankingQueryOperation}
     */
    @AbstractOperation.OperationConstructor({"Distance threshold", "Number of nearest pairs", "Skip symmetric pairs", "Answer type",
                                             "Hostname of remote algorithm", "Port number of remote algorithm",
                                             "Class name of query to execute on remote algorithm", "Parameters of the query class..."})
    public RangeJoinQueryOperation(float mu, int k, boolean skipSymmetricPairs, AnswerType answerType,
                                   String host, int port, Class<RankingQueryOperation> queryCls, String... queryParams) throws UnknownHostException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        super(mu, k, skipSymmetricPairs, answerType);
        
        remoteAlgo = new RMIAlgorithm(host, port);

        // Prepare parameters first, since they will be converted from Strings to correct type by ConstructorInstantiator below.
        operParams = new Object[queryParams.length+1];
        System.arraycopy(queryParams, 0, operParams, 1, queryParams.length);
        try {
            operConstructor = new ConstructorInstantiator<RankingQueryOperation>(queryCls, true, null, operParams);
        } catch (NoSuchInstantiatorException ex) {
            throw new RuntimeException("RangeJoin: Cannot instantiate the passed class " + queryCls.getName() + ": " + ex.getMessage(), ex);
        }
    }

    
    //****************** Implementation of query evaluation ******************//

    /**
     * Evaluate this join query on a given set of objects.
     * The objects found by this evaluation are added to answer of this query via {@link #addToAnswer}.
     * 
     * For each object in the passed iterator, a range query is evaluated on the external index given in a constructor.
     * The reange query is instantiated from the parameters passed in the constructor.
     *
     * @param objects the collection of objects on which to evaluate this query
     * @return number of objects satisfying the query
     */
    @Override
    public int evaluate(AbstractObjectIterator<? extends LocalAbstractObject> objects) {
        int beforeCount = getAnswerCount();
        
        try {
            while (objects.hasNext()) {
                LocalAbstractObject q = objects.next();
                
                // Run the query
                operParams[0] = q;
                RankingQueryOperation op = operConstructor.instantiate(operParams);
                op = remoteAlgo.executeOperation(op);
                
                for (Iterator<RankedAbstractObject> it = op.getAnswer(); it.hasNext();) {
                    RankedAbstractObject ranked = it.next();
                    addToAnswer(q, (LocalAbstractObject)ranked.getObject(), ranked.getDistance(), getDistanceThreshold());
                }
            }
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("RangeJoin: Cannot instantiate the passed RankingQueryOperation! " + ex.getMessage(), ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException("RangeJoin: Cannot instantiate the passed RankingQueryOperation! " + ex.getMessage(), ex);
        } catch (AlgorithmMethodException ex) {
            throw new RuntimeException("RangeJoin: Cannot run the passed RankingQueryOperation on the remote algorithm!", ex);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException("RangeJoin: Cannot run the passed RankingQueryOperation on the remote algorithm!", ex);
        }

        return getAnswerCount() - beforeCount;
    }
}
