/*
 * HttpApplication
 *
 */

package messif.utility;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.zip.GZIPInputStream;
import messif.algorithms.Algorithm;
import messif.executor.MethodExecutor.ExecutableMethod;
import messif.objects.LocalAbstractObject;
import messif.objects.extraction.Extractor;
import messif.objects.extraction.ExtractorDataSource;
import messif.objects.extraction.ExtractorException;
import messif.objects.util.AbstractObjectList;
import messif.objects.util.RankedAbstractObject;
import messif.operations.AbstractOperation;
import messif.operations.RankingQueryOperation;
import messif.utility.reflection.Instantiators;

/**
 *
 * @author xbatko
 */
public class HttpApplication extends Application {
    //****************** Attributes ******************//

    /** HTTP server for algorithm API */
    protected HttpServer httpServer;
    /** Remembered list of contexts */
    protected Map<String, HttpContext> httpServerContexts;

    //****************** HTTP server command functions ******************//

    /**
     * Adds a context to the HTTP server that is processed by the specified operation.
     * Additional arguments can be specified for the constructor of the operation.
     * For constructor arguments where a {@link LocalAbstractObject} is required,
     * an extractor must be provided. If a parameter is specified via
     * the URL, use a quoted URL parameter name. Otherwise the argument is a constant
     * that will be used for each query.
     *  
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; httpAddContext /search messif.operations.ApproxKNNQueryOperation messif.objects.extraction.Extractors.createTextExtractor(messif.objects.impl.MetaObjectSAPIRWeightedDist) "k" REMOTE_OBJECTS
     * </pre>
     * This will create a Context that will execute {@link messif.operations.ApproxKNNQueryOperation}
     * using a three parameters. The first parameter will be instance of an extractor created by call to
     * {@link messif.objects.extraction.Extractors#createTextExtractor} with
     * a {@link messif.objects.impl.MetaObjectSAPIRWeightedDist} parameter. This extractor will be used
     * create a {@link LocalAbstractObject object} from the HTTP request body. The second parameter will be the
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
            HttpContext context = httpServer.createContext(
                    args[1],
                    new HttpApplicationHandler<AbstractOperation>(
                        algorithm,
                        Convert.getClassForName(args[2], AbstractOperation.class), // operation class
                        args, 3,
                        args.length - 3,
                        namedInstances
                    )
            );
            httpServerContexts.put(args[1], context);
            return true;
        } catch (Exception e) {
            out.println("Cannot add HTTP context " + args[1] + ": " + e);
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

    /**
     * List current contexts of the HTTP server.
     *
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; httpListContexts
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args none
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "list HTTP server contexts", arguments = {})
    public boolean httpListContexts(PrintStream out, String... args) {
        if (httpServer == null) {
            out.println("There is no HTTP server started");
            return false;
        }

        for (Map.Entry<String, HttpContext> entry : httpServerContexts.entrySet()) {
            Authenticator authenticator = entry.getValue().getAuthenticator();
            // Show authorized users if the authenticator is set
            if (authenticator != null) {
                out.print(entry.getKey());
                out.print(" (authorized users: ");
                out.print(authenticator);
                out.println(")");
            } else {
                out.println(entry.getKey());
            }
        }

        return true;
    }

    /**
     * Updates a context auth to use HTTP Basic authentication mechanism.
     * The simple username/password pairs can be added to this method to
     * authorize a given user to use the specified context path. Several
     * users can be added by subsequent calls to this method.
     * If a password is not provided for a user, the user is removed from
     * the context. If a last user is removed, the authentication is removed
     * from the context completely.
     *
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; httpSetContextAuth /search myuser mypassword
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args the context path for which to set auth, the user name and the password (optional, if not set, the user is removed)
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "update authorized users for the HTTP context", arguments = {"context path", "user name", "password (if not set, user is removed)"})
    public boolean httpSetContextAuth(PrintStream out, String... args) {
        if (httpServer == null) {
            out.println("There is no HTTP server started");
            return false;
        }

        if (args.length < 3) {
            out.println("The context path and user must be provided");
            return false;
        }

        HttpContext context = httpServerContexts.get(args[1]);
        if (context == null) {
            out.println("There is no context for " + args[1]);
            return false;
        }

        HttpApplicationAuthenticator authenticator = (HttpApplicationAuthenticator)context.getAuthenticator();
        if (authenticator == null)
            authenticator = new HttpApplicationAuthenticator();
        authenticator.updateCredentials(args[2], args.length > 3 ? args[3] : null);
        context.setAuthenticator(authenticator.hasCredentials() ? authenticator : null);

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
    protected String usage() {
        return "<http port> " + super.usage();
    }

    @Override
    protected boolean parseArguments(String[] args, int argIndex) {
        if (argIndex >= args.length)
            return false;

        try {
            httpServer = HttpServer.create(new InetSocketAddress(Integer.parseInt(args[argIndex])), 0);
            httpServerContexts = new HashMap<String, HttpContext>();
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

    //****************** HTTP authenticator ******************//

    /**
     * Authenticator for the {@link com.sun.net.httpserver.HttpServer HTTP server}
     * that provides a basic HTTP authentication based on the stored user/password
     * values.
     */
    protected static class HttpApplicationAuthenticator extends BasicAuthenticator {

        //****************** Constants ******************//

        /** Default realm for the constructor */
        public static final String DEFAULT_REALM = "MESSIF HTTP Application";


        //****************** Attributes ******************//

        /** List of users and their passwords for the authentication */
        private final Map<String, String> credentials;


        //****************** Constructors ******************//

        /**
         * Creates a HttpApplicationAuthenticator for the given HTTP realm.
         * @param realm the HTTP Basic authentication realm
         */
        public HttpApplicationAuthenticator(String realm) {
            super(realm);
            this.credentials = new HashMap<String, String>();
        }

        /**
         * Creates a HttpApplicationAuthenticator for the {@link #DEFAULT_REALM default realm}.
         */
        public HttpApplicationAuthenticator() {
            this(DEFAULT_REALM);
        }

        /**
         * Updates the stored credentials of this authenticator.
         * If the password is <tt>null</tt>, the user is removed.
         * Otherwise, the user identified by the password is added to the
         * list of known users (replacing the previous password if the user
         * was already known).
         * 
         * @param username the user name to add to the list
         * @param password the password for the username to add to the list
         */
        public void updateCredentials(String username, String password) {
            if (password == null)
                credentials.remove(username);
            else
                credentials.put(username, password);
        }

        /**
         * Returns whether this authenticator has at least one user set.
         * @return <tt>true</tt> if there are credentials for at least one user
         */
        public boolean hasCredentials() {
            return !credentials.isEmpty();
        }

        /**
         * Called for each incoming request to verify the given name and password
         * in the context of this Authenticator's realm.
         * @param username the username from the request
         * @param password the password from the request
         * @return <tt>true</tt> if the credentials are valid, <tt>false</tt> otherwise
         */
        @Override
        public boolean checkCredentials(String username, String password) {
            String checkPasswd = credentials.get(username);
            return checkPasswd != null && checkPasswd.equals(password);
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder();
            Iterator<String> iterator = credentials.keySet().iterator();
            while (iterator.hasNext()) {
                str.append(iterator.next());
                if (iterator.hasNext())
                    str.append(", ");
            }
            return str.toString();
        }

    }

    //****************** HTTP connection handler ******************//

    /**
     * Handler for the {@link com.sun.net.httpserver.HttpServer HTTP server}
     * that executes the given operation with the specified arguments.
     * See the constructor for more informations.
     *
     * @param <T> the operation type executed by this handler
     * @author xbatko
     */
    protected static class HttpApplicationHandler<T extends AbstractOperation> implements HttpHandler {

        //****************** Additional types ******************//

        /** Types of output that this handler supports */
        public static enum OutputType {
            /** Outputs data as XML */
            XML,
            /** Outputs data as plain text */
            TEXT;
        }

        /**
         * Filler for creating {@link LocalAbstractObject} instances from {@link InputStream input streams}.
         */
        protected static class ObjectFiller {
            /** Extractor for the object created from the input stream */
            private final Extractor<? extends LocalAbstractObject> extractor;
            /** Index of the array element to fill */
            private final int itemToFill;
            /**
             * Flag whether the filler creates {@link AbstractObjectList list of objects}
             * from the provided input stream, otherwise a single {@link LocalAbstractObject object} is created
             */
            private final boolean createList;

            /**
             * Creates a filler that puts an instance of {@code objectClass} into the {@code itemToFill}
             * element of the filled array. The object is constructed from the {@link InputStream}
             * that is provided as the filling value.
             *
             * @param extractor the extractor for objects to create
             * @param itemToFill the index of the array element to fill
             * @param createList if <tt>true</tt>, the filler will create {@link AbstractObjectList list of objects}
             *          from the provided input stream, otherwise a single {@link LocalAbstractObject object} is created
             * @throws IllegalArgumentException if the specified {@code objectClass} is not valid
             */
            public ObjectFiller(Extractor<? extends LocalAbstractObject> extractor, int itemToFill, boolean createList) throws IllegalArgumentException {
                this.itemToFill = itemToFill;
                this.createList = createList;
                this.extractor = extractor;
            }

            /**
             * Fills the appropriate element in {@code args} array with the object(s)
             * created from the {@code inputStream}.
             * @param inputStream the input stream used to create instance(s) of {@link LocalAbstractObject}
             * @param name the name of the object in the input stream
             * @param args the array to fill the value into
             * @throws ExtractorException if there was a problem reading or extracting objects from the stream
             * @throws IOException if there was a problem reading or extracting objects from the stream
             */
            public void fill(InputStream inputStream, String name, Object[] args) throws ExtractorException, IOException {
                ExtractorDataSource dataSource = new ExtractorDataSource(inputStream, name);
                if (createList) {
                    AbstractObjectList<LocalAbstractObject> list = new AbstractObjectList<LocalAbstractObject>();
                    while (true) {
                        try {
                            list.add(extractor.extract(dataSource));
                        } catch (EOFException e) {
                            break;
                        }
                    }
                } else {
                    args[itemToFill] = extractor.extract(dataSource);
                }
            }
        }

        /**
         * Filler for arguments passed from HTTP request.
         * The filler is able to {@link #fill(java.lang.String, java.lang.Object[]) fill}
         * the given value into the {@code itemToFill} element of the filled array.
         * The value is {@link Convert#stringToType(java.lang.String, java.lang.Class) converted}
         * to the given {@code type}.
         */
        protected static class ParamFiller {
            /** Type to convert the http request parameter to */
            private final Class<?> type;
            /** Index of the array element to fill */
            private final int itemToFill;
            /** Collection of named instances that are used when converting string parameters */
            private final Map<String, Object> namedInstances;

            /**
             * Creates a new instance of ParamFiller.
             *
             * @param type the class of value
             * @param itemToFill the index of the array element to fill
             * @param namedInstances collection of named instances that are used when converting string parameters
             */
            public ParamFiller(Class<?> type, int itemToFill, Map<String, Object> namedInstances) {
                this.type = type;
                this.itemToFill = itemToFill;
                this.namedInstances = namedInstances;
            }

            /**
             * Use the supplied value to fill the appropriate element in {@code args} array.
             * @param value the value to fill (usually converted by this filler before filled)
             * @param args the array to fill the value into
             * @throws IllegalArgumentException if there was a problem reading or converting the value
             */
            public void fill(String value, Object[] args) throws IllegalArgumentException {
                try {
                    args[itemToFill] = Convert.stringToType(value, type, namedInstances);
                } catch (InstantiationException e) {
                    throw new IllegalArgumentException(e.getMessage(), e.getCause());
                }
            }
        }


        //****************** Attributes ******************//

        /** Algorithm on which this handler will execute the operation */
        private final Algorithm algorithm;
        /** Constructor for the operation executed by this handler */
        private final Constructor<? extends T> operationConstructor;
        /** Parameter array with filled constants (note that this is clonned so that the contents remain the same between threads) */
        private final Object[] params;
        /** Dynamic param fillers - these are taken from the HTTP request parameters */
        private final Map<String, ParamFiller> paramFillers;
        /** Dynamic filler for the {@link LocalAbstractObject} parameter */
        private final ObjectFiller objectFiller;
        /** Charset used to encode results */
        private final Charset charset;
        /** Flag which output to use (defaults to {@link OutputType#TEXT}) */
        private OutputType outputType = OutputType.TEXT;


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
         * @param namedInstances collection of named instances that are used when converting string parameters
         * @throws NoSuchMethodException if the operation does not have an annotated constructor with {@code length} arguments
         * @throws InstantiationException if any of the provided {@code args} cannot be converted to the type specified in the operation's constructor
         * @throws IndexOutOfBoundsException if the {@code offset} or {@code length} are not valid for {@code args} array
         */
        public HttpApplicationHandler(Algorithm algorithm, Class<? extends T> operationClass, String args[], int offset, int length, Map<String, Object> namedInstances) throws NoSuchMethodException, InstantiationException, IndexOutOfBoundsException {
            this.algorithm = algorithm;
            this.operationConstructor = AbstractOperation.getAnnotatedConstructor(operationClass, length);
            this.charset = Charset.defaultCharset();
            Class<?>[] operationParamTypes = operationConstructor.getParameterTypes();

            // Fill arguments and fillers
            this.params = new Object[length];
            this.paramFillers = new HashMap<String, ParamFiller>();
            ObjectFiller objectFillerTemp = null;
            for (int i = 0; i < length; i++) {
                // Parse quoted argument
                String name = parseQuoted(args[i + offset]);
                if (name != null) {
                    // Parametrized value is quoted
                    paramFillers.put(name, new ParamFiller(operationParamTypes[i], i, namedInstances));
                } else if (operationParamTypes[i].isAssignableFrom(LocalAbstractObject.class)) {
                    objectFillerTemp = new ObjectFiller(createExtractor(args[i + offset], namedInstances), i, false);
                } else if (operationParamTypes[i].isAssignableFrom(AbstractObjectList.class)) {
                    objectFillerTemp = new ObjectFiller(createExtractor(args[i + offset], namedInstances), i, true);
                } else {
                    params[i] = Convert.stringToType(args[i + offset], operationParamTypes[i], namedInstances);
                }
            }
            this.objectFiller = objectFillerTemp;
        }


        //****************** Attribute access ******************//

        /**
         * Set the output type to use in this handler.
         * @param outputType the new value of output type
         * @throws NullPointerException if the specified {@code outputType} is <tt>null</tt>
         */
        public void setOutputType(OutputType outputType) throws NullPointerException {
            if (outputType == null)
                throw new NullPointerException();
            this.outputType = outputType;
        }

        /**
         * Returns the current output type used in this handler.
         * @return the current output type used in this handler
         */
        public OutputType getOutputType() {
            return outputType;
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
         * Creates an extractor using the given signature.
         * @param signature signature of the extractor instance to use
         * @param namedInstances collection of named instances that are used when converting string parameters
         * @return a new instance of extractor
         * @throws IllegalArgumentException if there was an error creating the extractor instance
         */
        private Extractor<?> createExtractor(String signature, Map<String, Object> namedInstances) throws IllegalArgumentException {
            try {
                // Try to get named instance first
                Object instance = namedInstances.get(signature);
                if (instance != null && instance instanceof Extractor)
                    return (Extractor)instance;
                return Instantiators.createInstanceWithStringArgs(signature, Extractor.class, namedInstances);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Cannot create " + signature + ": class not found");
            } catch (InvocationTargetException e) {
                throw new IllegalArgumentException("Cannot create " + signature + ": " + e.getCause(), e.getCause());
            }
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
            String id = null;

            // Fill params
            Matcher matcher = paramParser.matcher(exchange.getRequestURI().getQuery());
            while (matcher.find()) {
                ParamFiller filler = paramFillers.get(matcher.group(1));
                if (filler != null)
                    filler.fill(matcher.group(2), args);
                else if ("id".equals(matcher.group(1)))
                    id = matcher.group(2);
            }

            // Fill object
            try {
                if (objectFiller != null) {
                    InputStream input = exchange.getRequestBody();
                    String contentEncoding = exchange.getRequestHeaders().getFirst("content-encoding");
                    if (contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip"))
                        input = new GZIPInputStream(input);
                    objectFiller.fill(input, id, args);
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("There was a problem reading the object from the data: " + e.getMessage(), e);
            } catch (ExtractorException e) {
                throw new IllegalArgumentException("There was a problem extracting descriptors from the object: " + e.getMessage(), e);
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
                logException(e);
                response = e.toString();
                success = false;
            }

            switch (outputType) {
                case XML:
                    sendXmlResponse(exchange, success, response);
                    break;
                case TEXT:
                    sendTextResponse(exchange, success, response);
                    break;
            }
        }

    }
}
