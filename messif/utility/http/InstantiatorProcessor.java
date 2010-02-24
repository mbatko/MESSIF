/*
 * InstantiatorProcessor
 *
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
 * @author xbatko
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
