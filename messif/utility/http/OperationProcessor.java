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
package messif.utility.http;

import com.sun.net.httpserver.HttpExchange;
import java.lang.reflect.Constructor;
import java.util.Map;
import messif.algorithms.Algorithm;
import messif.operations.AbstractOperation;

/**
 * Processor that creates and executes {@link AbstractOperation operation}.
 * The arguments for the operation's constructor are resolved by additional processors.
 *
 * @param <T> type of operation created by this processor
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class OperationProcessor<T extends AbstractOperation> implements HttpApplicationProcessor<T> {

    //****************** Attributes ******************//

    /** Algorithm on which this processor execute the operation */
    private final Algorithm algorithm;
    /** Constructor for the operation executed by this processor */
    private final Constructor<? extends T> operationConstructor;
    /** Operation constructor arguments processors */
    private final HttpApplicationProcessor<?>[] processors;


    //****************** Constructor ******************//

    /**
     * Creates a new processor that executes the given {@code operationClass} on the given {@code algorithm}.
     * Additional arguments for the operation's constructor can be specified in {@code args}.
     *
     * @param algorithm the algorithm on which the created operation will be executed
     * @param operationClass the class of the operation executed by this handler
     * @param args additional arguments for the constructor
     * @param offset the index into {@code args} where the first constructor argument is
     * @param length the number of constructor arguments to use
     * @param namedInstances collection of named instances that are used when converting string parameters
     * @throws IndexOutOfBoundsException if the {@code offset} or {@code length} are not valid for {@code args} array
     * @throws IllegalArgumentException if the operation does not have an annotated constructor with {@code length} argument or
     *              if any of the provided {@code args} cannot be converted to the type specified in the operation's constructor
     */
    public OperationProcessor(Algorithm algorithm, Class<? extends T> operationClass, String args[], int offset, int length, Map<String, Object> namedInstances) throws IndexOutOfBoundsException, IllegalArgumentException {
        this.algorithm = algorithm;
        try {
            this.operationConstructor = AbstractOperation.getAnnotatedConstructor(operationClass, length);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        Class<?>[] operationParamTypes = operationConstructor.getParameterTypes();

        this.processors = new HttpApplicationProcessor<?>[length];
        int argsProcessed = 0;
        for (int i = 0; i < length; i++) {
            processors[i] = HttpApplicationUtils.createProcessor(operationParamTypes[i], algorithm, args, offset + argsProcessed, length - argsProcessed, namedInstances);
            argsProcessed += processors[i].getProcessorArgumentCount();
        }
    }


    //****************** Processor implementation ******************//

    public T processHttpExchange(HttpExchange httpExchange, Map<String, String> httpParams) throws Exception {
        Object[] arguments = new Object[processors.length];
        for (int i = 0; i < processors.length; i++)
            arguments[i] = processors[i].processHttpExchange(httpExchange, httpParams);
        return algorithm.executeOperation(operationConstructor.newInstance(arguments));
    }

    public int getProcessorArgumentCount() {
        return processors.length;
    }

    public Class<? extends T> getProcessorReturnType() {
        return operationConstructor.getDeclaringClass();
    }

}
