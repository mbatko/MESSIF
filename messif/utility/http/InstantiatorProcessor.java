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
import java.util.Map;
import messif.algorithms.Algorithm;
import messif.utility.reflection.Instantiator;
import messif.utility.reflection.InstantiatorSignature;

/**
 * Processor that returns a value created by a given {@link Instantiator}.
 * @param <T> type of object created by the instantiator
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class InstantiatorProcessor<T> implements HttpApplicationProcessor<T> {

    //****************** Attributes ******************//

    /** Instantiator called by this processor */
    private final Instantiator<T> instantiator;
    /** Method arguments processors */
    private final HttpApplicationProcessor<?>[] processors;


    //****************** Constructor ******************//

    /**
     * Creates a new processor that executes the given instantiator.
     * Additional arguments for the operation's constructor can be specified in {@code args}.
     *
     * @param algorithm the algorithm for which to create the processor
     * @param returnClass the class of the instances created by the instantiator
     * @param signature the signature of a method/constructor/field that provides the value
     * @param namedInstances collection of named instances that are used when converting string parameters
     * @throws IndexOutOfBoundsException if the {@code offset} or {@code length} are not valid for {@code args} array
     * @throws IllegalArgumentException if the operation does not have an annotated constructor with {@code length} argument or
     *              if any of the provided {@code args} cannot be converted to the type specified in the operation's constructor
     */
    public InstantiatorProcessor(Algorithm algorithm, String signature, Class<? extends T> returnClass, Map<String, Object> namedInstances) throws IndexOutOfBoundsException, IllegalArgumentException {
        InstantiatorSignature instantiatorSignature = new InstantiatorSignature(signature);
        this.instantiator = instantiatorSignature.createInstantiator(returnClass);
        Class<?>[] prototype = instantiator.getInstantiatorPrototype();

        this.processors = new HttpApplicationProcessor<?>[prototype.length];
        int argsProcessed = 0;
        String args[] = instantiatorSignature.getParsedArgs();
        for (int i = 0; i < prototype.length; i++) {
            processors[i] = HttpApplicationUtils.createProcessor(
                    prototype[i], algorithm,
                    args, argsProcessed, args.length - argsProcessed,
                    namedInstances
            );
            argsProcessed += processors[i].getProcessorArgumentCount();
        }
    }


    //****************** Processor implementation ******************//

    public T processHttpExchange(HttpExchange httpExchange, Map<String, String> httpParams) throws Exception {
        Object[] arguments = new Object[processors.length];
        for (int i = 0; i < processors.length; i++)
            arguments[i] = processors[i].processHttpExchange(httpExchange, httpParams);
        return instantiator.instantiate(arguments);
    }

    public int getProcessorArgumentCount() {
        return 1;
    }

    public Class<? extends T> getProcessorReturnType() {
        return instantiator.getInstantiatorClass();
    }

}
