/*
 * HttpApplication
 *
 */

package messif.utility;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import messif.algorithms.Algorithm;
import messif.executor.MethodExecutor.ExecutableMethod;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectList;
import messif.objects.util.RankedAbstractObject;
import messif.objects.util.StreamGenericAbstractObjectIterator;
import messif.operations.AbstractOperation;
import messif.operations.RankingQueryOperation;

/**
 *
 * @author xbatko
 */
public class HttpApplication extends Application {
    //****************** Attributes ******************//

    /** HTTP server for algorithm API */
    protected HttpServer httpServer;

    //****************** HTTP server command functions ******************//

    /**
     * Adds a context to the HTTP server that is processed by the specified operation.
     * Additional arguments can be specified for the constructor of the operation.
     * For constructor arguments where a {@link LocalAbstractObject} is required,
     * the specific class name must be provided. If a parameter is specified via
     * the URL, use a quoted URL parameter name. Otherwise the argument is a constant
     * that will be used for each query.
     *  
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; httpAddContext /search messif.operations.ApproxKNNQueryOperation messif.objects.impl.MetaObjectSAPIRWeightedDist "k" REMOTE_OBJECTS
     * </pre>
     * This will create a Context that will execute {@link messif.operations.ApproxKNNQueryOperation}
     * using a three parameters. The first parameter will be a {@link messif.objects.impl.MetaObjectSAPIRWeightedDist}
     * object created from the HTTP request body. The second parameter will be the
     * HTTP request get parameter <em>k</em> (e.g. the URL will contain .../search?k=30).
     * The third parameter will be always the {@link messif.operations.AnswerType#REMOTE_OBJECTS} constant.
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args the context path and the operation class (fully specified)
     *          followed by the additional arguments for the operation's constructor
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "add HTTP server context", arguments = {"context path", "operation class", "additional operation arguments ..."})
    public boolean httpAddContext(PrintStream out, String... args) {
        if (httpServer == null) {
            out.println("There is no HTTP server started");
            return false;
        }

        if (algorithm == null) {
            out.println("There is no algorithm selected");
            return false;
        }

        if (args.length < 3) {
            out.println("At least the context path and the operation class must be provided");
            return false;
        }

        try {
            httpServer.createContext(args[1], new HttpApplicationHandler<AbstractOperation>(
                    algorithm,
                    Convert.getClassForName(args[2], AbstractOperation.class), // operation class
                    args, 3,
                    args.length - 3
            ));
            return true;
        } catch (Exception e) {
            out.println("Cannot add HTTP context " + args[0] + ": " + e);
            return false;
        }
    }

    /**
     * Removes a context from the HTTP server.
     *
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; httpRemoveContext /search
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args the context path to remove
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "add HTTP server context", arguments = {"context path"})
    public boolean httpRemoveContext(PrintStream out, String... args) {
        if (httpServer == null) {
            out.println("There is no HTTP server started");
            return false;
        }

        if (args.length < 2) {
            out.println("The context path must be provided");
            return false;
        }

        httpServer.removeContext(args[1]);
        return true;
    }

    @ExecutableMethod(description = "close the whole application (all connections will be closed including the HTTP server)", arguments = { })
    @Override
    public boolean quit(PrintStream out, String... args) {
        if (!super.quit(out, args))
            return false;
        httpServer.stop(0);
        return true;
    }


    //****************** Standalone application's main method ******************//

    @Override
    String usage() {
        return "<http port> " + super.usage();
    }

    @Override
    boolean parseArguments(String[] args, int argIndex) {
        if (argIndex >= args.length)
            return false;

        try {
            httpServer = HttpServer.create(new InetSocketAddress(Integer.parseInt(args[argIndex])), 0);
            argIndex++;
        } catch (NumberFormatException e) {
            System.err.println("HTTP port is not valid: " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.err.println("Cannot start HTTP server: " + e);
            return false;
        }

        boolean ret = super.parseArguments(args, argIndex);

        // Start the HTTP server
        httpServer.start();

        return ret;
    }


    /**
     * Start a MESSIF application with HTTP server.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new HttpApplication().startApplication(args);
    }


    //****************** Dynamic parameters utility methods ******************//

    /**
     * Handler for the {@link com.sun.net.httpserver.HttpServer HTTP server}
     * that executes the given operation with the specified arguments.
     * See the {@link #HttpApplicationHandler(messif.algorithms.Algorithm, java.lang.Class, java.lang.String[], int, int) constructor}
     * for more informations.
     *
     * @param <T> the operation type executed by this handler
     * @author xbatko
     */
    protected static class HttpApplicationHandler<T extends AbstractOperation> implements HttpHandler {

        //****************** Constants ******************//

        /**
         * Internal interface for external argument fillers.
         * This is used to fill a value from the HTTP request
         * (see {@link #createParamFiller(java.lang.Class, int)} and {@link #createObjectFiller(java.lang.String, int)}).
         * @param <V> the type of value the filler expects
         */
        protected static interface ArgumentFiller<V> {
            /**
             * Use the supplied value to fill the appropriate element in {@code args} array.
             * @param value the value to fill (usually converted by this filler before filled)
             * @param args the array to fill the value into
             * @throws IllegalArgumentException if there was a problem reading or converting the value
             */
            public void fill(V value, Object[] args) throws IllegalArgumentException;
        }


        //****************** Attributes ******************//

        /** Algorithm on which this handler will execute the operation */
        private final Algorithm algorithm;
        /** Constructor for the operation executed by this handler */
        private final Constructor<? extends T> operationConstructor;
        /** Parameter array with filled constants (note that this is clonned so that the contents remain the same between threads) */
        private final Object[] params;
        /** Dynamic param fillers - these are taken from the HTTP request parameters */
        private final Map<String, ArgumentFiller<String>> paramFillers;
        /** Dynamic filler for the {@link LocalAbstractObject} parameter */
        private final ArgumentFiller<InputStream> objectFiller;
        /** Charset used to encode results */
        private final Charset charset;


        //****************** Constructor ******************//

        /**
         * Creates a new HttpApplicationHandler that executes the given {@code operationClass} on the given {@code algorithm}.
         * Additional arguments for the operation's constructor can be specified in {@code args}.
         *
         * @param algorithm the algorithm on which this handler will execute the operation
         * @param operationClass the class of the operation executed by this handler
         * @param args additional arguments for the constructor
         * @param offset the index into {@code args} where the first constructor argument is
         * @param length the number of constructor arguments to use
         * @throws NoSuchMethodException if the operation does not have an annotated constructor with {@code length} arguments
         * @throws InstantiationException if any of the provided {@code args} cannot be converted to the type specified in the operation's constructor
         * @throws IndexOutOfBoundsException if the {@code offset} or {@code length} are not valid for {@code args} array
         */
        public HttpApplicationHandler(Algorithm algorithm, Class<? extends T> operationClass, String args[], int offset, int length) throws NoSuchMethodException, InstantiationException, IndexOutOfBoundsException {
            this.algorithm = algorithm;
            this.operationConstructor = AbstractOperation.getAnnotatedConstructor(operationClass, length);
            this.charset = Charset.defaultCharset();
            Class<?>[] operationParamTypes = operationConstructor.getParameterTypes();

            // Fill arguments and fillers
            this.params = new Object[length];
            this.paramFillers = new HashMap<String, ArgumentFiller<String>>();
            ArgumentFiller<InputStream> objectFillerTemp = null;
            for (int i = 0; i < length; i++) {
                // Parse quoted argument
                String name = parseQuoted(args[i + offset]);
                if (name == null) { // Parametrized value is NOT quoted
                    // Check for LocalAbstractObject parameter, which is handled differently
                    int whichObjectFiller = whichObjectFillerRequired(operationParamTypes[i], objectFillerTemp);
                    if (whichObjectFiller != 0) {
                        objectFillerTemp = createObjectFiller(args[i + offset], i, whichObjectFiller);
                    } else {
                        params[i] = Convert.stringToType(args[i + offset], operationParamTypes[i]);
                    }
                } else {
                    // Parametrized value is quoted
                    paramFillers.put(name, createParamFiller(operationParamTypes[i], i));
                }
            }
            this.objectFiller = objectFillerTemp;
        }


        //****************** Dynamic parameters utility methods ******************//

        /**
         * Returns the unquoted string from {@code quotedStr} or <tt>null</tt>,
         * if the {@code quotedStr} was not quoted.
         * @param quotedStr the string to unquote
         * @return the unquoted string or <tt>null</tt>
         */
        private String parseQuoted(String quotedStr) {
            if (quotedStr == null || quotedStr.length() < 2 || quotedStr.charAt(0) != '"' || quotedStr.charAt(quotedStr.length() - 1) != '"')
                return null;
            else
                return quotedStr.substring(1, quotedStr.length() - 1);
        }

        /**
         * Returns which type of object filler to use for the given parameter class.
         * <ul>
         *  <li>2 - fills an {@link AbstractObjectList} from all objects given in the {@link InputStream}</li>
         *  <li>1 - fills a single {@link LocalAbstractObject} from the first object given in the {@link InputStream}</li>
         *  <li>0 - not an object filler (e.g. param filler is used instead)</li>
         * </ul>
         *
         * @param parameterType the class of the constructor parameter
         * @param currentFiller the current object filler
         * @return the type of the object filler to use
         * @throws IllegalArgumentException if the {@code currentFiller} is already set
         */
        private int whichObjectFillerRequired(Class<?> parameterType, ArgumentFiller<InputStream> currentFiller) throws IllegalArgumentException {
            int ret;
            if (parameterType.isAssignableFrom(LocalAbstractObject.class))
                ret = 1;
            else if (parameterType.isAssignableFrom(AbstractObjectList.class))
                ret = 2;
            else
                return 0;

            if (currentFiller != null)
                throw new IllegalArgumentException("Cannot have two LocalAbstractObject parameters");

            return ret;
        }

        /**
         * Creates a filler that puts an instance of {@code objectClass} into the {@code itemToFill}
         * element of the filled array. The object is constructed from the {@link InputStream}
         * that is provided as the filling value.
         *
         * @param objectClass the class of the objects to create (must be a descendant of {@link LocalAbstractObject})
         * @param itemToFill the index of the array element to fill
         * @param whichFillerType the type of the filler object according to {@link #whichObjectFillerRequired(java.lang.Class, messif.utility.HttpApplicationHandler.ArgumentFiller)}
         * @return a new filler instance
         * @throws IllegalArgumentException if the specified {@code objectClass} is not valid
         */
        protected ArgumentFiller<InputStream> createObjectFiller(String objectClass, final int itemToFill, final int whichFillerType) throws IllegalArgumentException {
            // Prepare constructor
            final Constructor<? extends LocalAbstractObject> objectConstructor;
            try {
                objectConstructor = Convert.getClassForName(objectClass, LocalAbstractObject.class).getConstructor(BufferedReader.class);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Class " + objectClass + " is not valid for LocalAbstractObject parameter");
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("No valid constructor for " + objectClass + " was found: " + e.getMessage());
            }


            return new ArgumentFiller<InputStream>() {
                public void fill(InputStream value, Object[] args) throws IllegalArgumentException {
                    try {
                        BufferedReader valueReader = new BufferedReader(new InputStreamReader(value));
                        if (whichFillerType == 2) {
                            // Type is list, so provide list
                            args[itemToFill] = new AbstractObjectList<LocalAbstractObject>(new StreamGenericAbstractObjectIterator<LocalAbstractObject>(objectConstructor, new Object[] {valueReader}));
                        } else {
                            // Provide a single object
                            args[itemToFill] = objectConstructor.newInstance(valueReader);
                        }
                    } catch (InvocationTargetException e) {
                        throw new IllegalArgumentException("Error reading object: " + e.getCause(), e.getCause());
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Error using object constructor " + objectConstructor + ": " + e, e);
                    }
                }
            };
        }

        /**
         * Creates a filler that puts the given value into the {@code itemToFill}
         * element of the filled array. The value is {@link Convert#stringToType(java.lang.String, java.lang.Class) converted}
         * to the given {@code type}.
         *
         * @param type the class of value
         * @param itemToFill the index of the array element to fill
         * @return a new filler instance
         */
        protected ArgumentFiller<String> createParamFiller(final Class<?> type, final int itemToFill) {
            return new ArgumentFiller<String>() {
                public void fill(String value, Object[] args) throws IllegalArgumentException {
                    try {
                        args[itemToFill] = Convert.stringToType(value, type);
                    } catch (InstantiationException e) {
                        throw new IllegalArgumentException(e.getMessage(), e.getCause());
                    }
                }
            };
        }


        //****************** HTTP request handling ******************//

        /** Parser regular expression for HTTP request parameters */
        private static final Pattern paramParser = Pattern.compile("([^=]+)=([^&]*)(?:&|$)");

        /**
         * Prepares constructor arguments for the operation constructor.
         * @param exchange the HTTP request used to fill dynamic parameters
         * @return array of arguments for the operation constructor
         * @throws IllegalArgumentException if there was a problem filling a dynamic parameter
         */
        protected Object[] getOperationArguments(HttpExchange exchange) throws IllegalArgumentException {
            Object[] args = params.clone();

            // Fill object
            if (objectFiller != null)
                objectFiller.fill(exchange.getRequestBody(), args);

            // Fill params
            Matcher matcher = paramParser.matcher(exchange.getRequestURI().getQuery());
            while (matcher.find()) {
                ArgumentFiller<String> filler = paramFillers.get(matcher.group(1));
                if (filler != null)
                    filler.fill(matcher.group(2), args);
            }

            return args;
        }


        //****************** HTTP response writers ******************//

        /**
         * Send the HTTP response as text.
         * @param exchange the HTTP exchange to use
         * @param success if the response should be successful (code 200) or unsuccessful (code 400)
         * @param response the text to send
         * @throws IOException if there was a problem sending the response back to client
         */
        private void sendTextResponse(HttpExchange exchange, boolean success, String response) throws IOException {
            // Send response back to client
            exchange.getResponseHeaders().add("Content-type", "text/plain; charset=" + charset.name());
            exchange.sendResponseHeaders(success ? 200 : 400, response.length() + 2);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(charset));
            os.write("\r\n".getBytes());
            exchange.close();
        }

        /**
         * Send the HTTP response as XML.
         * @param exchange the HTTP exchange to use
         * @param success if the response should be successful (code 200) or unsuccessful (code 400)
         * @param response the text to send
         * @throws IOException if there was a problem sending the response back to client
         */
        private void sendXmlResponse(HttpExchange exchange, boolean success, String response) throws IOException {
            // Prepare XML header
            StringBuilder header = new StringBuilder("<?xml version=\"1.0\" encoding=\"").append(charset.name()).append("\"?>");
            String footer;
            if (success) {
                header.append("<result>");
                footer = "</result>";
            } else {
                header.append("<fail>");
                footer = "</fail>";
            }

            // Send response back to client
            exchange.getResponseHeaders().add("Content-type", "text/xml; charset=" + charset.name());
            exchange.sendResponseHeaders(success ? 200 : 400, response.length() + header.length() + footer.length());
            OutputStream os = exchange.getResponseBody();
            os.write(header.toString().getBytes(charset));
            os.write(response.getBytes(charset));
            os.write(footer.getBytes(charset));
            exchange.close();
        }

        /**
         * Create response text for ranking query.
         * JSON arrays with items containing the distance and the object's locator are returned.
         * @param query the ranking query to use
         * @return a response text
         */
        private String getResponseRankingQuery(RankingQueryOperation query) {
            StringBuilder response = new StringBuilder();
            response.append('[');
            Iterator<RankedAbstractObject> iterator = query.getAnswer();
            while (iterator.hasNext()) {
                RankedAbstractObject object = iterator.next();
                response.append('[');
                response.append(object.getDistance());
                response.append(",\"");
                response.append(object.getObject().getLocatorURI());
                response.append("\"]");
                if (iterator.hasNext())
                    response.append(',');
            }
            response.append(']');

            return response.toString();
        }


        //****************** HttpHandler implementation ******************//

        /**
         * Handles a HTTP request by executing the operation and returning its error code or answer (for queries).
         *
         * @param exchange the HTTP request/response exchange
         * @throws IOException if there was a problem reading the HTTP request or writing the HTTP response
         */
        public void handle(HttpExchange exchange) throws IOException {
            // Prepare response buffer
            String response;
            boolean success;

            try {
                // Execute operation
                T operation = algorithm.executeOperation(operationConstructor.newInstance(getOperationArguments(exchange)));

                // Prepare response
                if (operation.isFinished() && !operation.wasSuccessful()) {
                    response = "Operation failed: " + operation.getErrorCode();
                    success = false;
                } else if (operation instanceof RankingQueryOperation) {
                    response = getResponseRankingQuery((RankingQueryOperation)operation);
                    success = true;
                } else {
                    response = "Operation finished successfully";
                    success = true;
                }
            } catch (Exception e) {
                // Error creating/executing operation
                log.severe(e);
                response = e.toString();
                success = false;
            }

            sendTextResponse(exchange, success, response);
        }

    }
}
