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

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import messif.algorithms.Algorithm;
import messif.executor.MethodExecutor.ExecutableMethod;
import messif.objects.LocalAbstractObject;
import messif.objects.extraction.ExtractorDataSource;
import messif.objects.extraction.ExtractorException;
import messif.objects.extraction.Extractors;
import messif.objects.extraction.MultiExtractor;
import messif.operations.AbstractOperation;

/**
 * Provides a HTTP extension to {@link Application} that allows to execute operations
 * via REST-like services using HTTP protocol.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class HttpApplication extends Application {

    //****************** Constants ******************//

    /** Parser regular expression for HTTP request parameters */
    private static final Pattern paramParser = Pattern.compile("([^=]+)=([^&]*)(?:&|$)");
    /** Constant holding a HTTP error code of successful operation */
    private static final int ERROR_CODE_SUCCESS = 200;
    /** Constant holding a HTTP error code of internal error */
    private static final int ERROR_CODE_INTERNAL_ERROR = 500;
    /** Constant holding a HTTP error code of invalid argument error */
    private static final int ERROR_CODE_INVALID_ARGUMENT = 400;


    //****************** Attributes ******************//

    /** HTTP server for algorithm API */
    private HttpServer httpServer;
    /** Remembered list of contexts */
    private Map<String, HttpContext> httpServerContexts;
    /** Thread-safe last executed operation */
    private final ThreadLocal<AbstractOperation> lastOperation = new ThreadLocal<AbstractOperation>();
    /** Thread-safe current selected algorithm */
    private final ThreadLocal<Algorithm> algorithm = new ThreadLocal<Algorithm>();


    //****************** Attribute access methods ******************//

    @Override
    AbstractOperation getLastOperation() {
        return this.lastOperation.get();
    }

    @Override
    boolean setLastOperation(AbstractOperation lastOperation) {
        this.lastOperation.set(lastOperation);
        return lastOperation != null;
    }

    /**
     * {@inheritDoc}
     * Note that if an algorithm was selected using {@link #algorithmSelectInThread},
     * it will be returned in that thread only. Otherwise, the globally {@link #algorithmSelect}
     * selected running algorithm will be used.
     *
     * @return {@inheritDoc}
     */
    @Override
    Algorithm getAlgorithm() {
        Algorithm alg = this.algorithm.get();
        return alg == null ? super.getAlgorithm() : alg;
    }

    /**
     * Select algorithm to manage in current thread.
     * That means that the running algorithm for the other operations is set only
     * for the current execution of the actual HTTP context.
     * A parameter with algorithm sequence number is required for specifying, which algorithm to select.
     * To unset the algorithm, use a value of -1.
     *
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; algorithmSelectInThread 0
     * </pre>
     *
     * @param out a stream where the application writes information for the user
     * @param args the algorithm sequence number
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     * @see #algorithmInfoAll
     */
    @ExecutableMethod(description = "select algorithm to manage", arguments = {"# of the algorithm to select"})
    public boolean algorithmSelectInThread(PrintStream out, String... args) {
        try {
            int algorithmIndex = Integer.parseInt(args[1]);
            if (algorithmIndex == -1)
                this.algorithm.remove();
            else
                this.algorithm.set(getAlgorithm(algorithmIndex));
        } catch (IndexOutOfBoundsException ignore) {
        } catch (NumberFormatException ignore) {
        }

        out.print("Algorithm # must be specified - use a number between 0 and ");
        out.println(getAlgorithmCount() - 1);

        return false;
    }


    //****************** HTTP server command functions ******************//

    /**
     * Adds a context to the HTTP server that is processed by the specified action.
     * 
     * If this method is called from <i>control file</i>, the first argument is automatically
     * set to the control file properties. Otherwise, the property file where the
     * action is defined must be specified as first argument.
     * Next, a name of the context must be given and then the action name to call.
     * The output of the action will be written to the response using the
     * content type as specified in the next argument and converted to the given charset.
     * 
     * Optionally, additional argument specifying map of context extractors can be specified.
     * The value is a list of "data=extractor" pairs, where the "extractor" is a name
     * of an extractor instance (e.g. created previously with {@link #namedInstanceAdd(java.io.PrintStream, java.lang.String[])}).
     * The "data" is a new name for a named instance that will get the extracted data in each
     * context call by passing the request input stream and the passed arguments to the extractor
     * as {@link ExtractorDataSource}. Note that the new named instance is a {@link InheritableThreadLocal}
     * value, so it is safe in multi-threaded HTTP servers.
     * 
     * Optionally, additional arguments specifying the content-type (defaults to "text/plain"),
     * and charset (defaults to "utf-8") can be specified.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; httpAddContext myfile.cf /search my_search extracted_data=extractor
     * </pre>
     * This will create a HTTP context that will execute the "my_search" action in "myfile.cf" and
     * the named instance "extracted_data" will contain the data extracted by the named instances "extractor"
     * from the respective HTTP request.
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args the control file (only if not called from a control file), the context path, the action name to call, the map of context extractors, the content type of the output, and the charset of the output
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "add HTTP server context", arguments = {"control file", "context path", "action name", "context extractors map (optional)", "content type (defaults to text/plain)", "charset (defaults to utf8)"})
    public boolean httpAddContext(PrintStream out, String... args) {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(args[1]));
        } catch (IndexOutOfBoundsException e) {
            out.println("Property file with action definition must be specified");
            return false;
        } catch (IOException e) {
            out.println("Error reading " + args[1] + ": " + e);
            return false;
        }
        return httpAddContext(out, props, args, 2, null);
    }

    /**
     * Internal method for adding a context to the HTTP server.
     *
     * @param out a stream where the application writes information for the user
     * @param props control-file properties where the action is defined
     * @param args the context path, the action name to call, the map of context extractors, the content type of the output, and the charset of the output
     * @param argIndex index in the {@code args} array where to start reading
     * @param variables list of variables set by the application
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    private boolean httpAddContext(PrintStream out, Properties props, String[] args, int argIndex, Map<String, String> variables) {
        if (httpServer == null) {
            out.println("There is no HTTP server started");
            return false;
        }

        if (args.length < argIndex + 2) {
            out.println("At least the context path and action name must be specified");
            return false;
        }

        if (httpServerContexts.containsKey(args[argIndex])) {
            out.println("Context '" + args[argIndex] + "' already exists");
            return false;
        }

        String actionName = args[argIndex + 1];
        Map<String, String> contextExtractorNames = args.length >= argIndex + 3 ? Convert.stringToMap(args[argIndex + 2]) : null;
        String contentType = args.length >= argIndex + 4 ? args[argIndex + 3] : "text/plain";
        String charset = args.length >= argIndex + 5 ? args[argIndex + 4] : "utf8";

        if (!props.containsKey(actionName))
            throw new IllegalArgumentException("Cannot find action '" + actionName + "' in the given control file data");

        try {
            HttpContext context = httpServer.createContext(
                    args[argIndex],
                    new HttpApplicationHandler(props, actionName, contextExtractorNames, contentType, charset, variables)
            );
            httpServerContexts.put(args[argIndex], context);
            return true;
        } catch (Exception e) {
            out.println("Cannot add HTTP context " + args[argIndex] + ": " + e);
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
        httpServerContexts.remove(args[1]);
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

    @Override
    protected boolean controlFileExecuteMethod(PrintStream out, Properties props, String actionName, Map<String, String> variables, List<String> arguments) throws NoSuchMethodException, InvocationTargetException {
        if (arguments.get(0).equals("httpAddContext"))
            return httpAddContext(out, props, arguments.toArray(new String[arguments.size()]), 1, variables);
        return super.controlFileExecuteMethod(out, props, actionName, variables, arguments);
    }


    //****************** Standalone application's main method ******************//

    @Override
    protected String usage() {
        return "<http port> [-httpThreads <0|n>] [-httpBacklog <m>]" + super.usage();
    }

    @Override
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    protected boolean parseArguments(String[] args, int argIndex) {
        if (argIndex >= args.length)
            return false;

        // Read http port argument
        int httpPort;
        try {
            httpPort = Integer.parseInt(args[argIndex]);
            argIndex++;
        } catch (NumberFormatException e) {
            System.err.println("HTTP port is not valid: " + e.getMessage());
            return false;
        }

        // Read http threads parameter
        int httpThreads = 1;
        if (argIndex < args.length && args[argIndex].equalsIgnoreCase("-httpThreads")) {
            try {
                httpThreads = Integer.parseInt(args[argIndex + 1]);
                argIndex += 2;
            } catch (IndexOutOfBoundsException e) {
                System.err.println("httpThreads parameter requires a number of threads");
                return false;
            } catch (NumberFormatException e) {
                System.err.println("Number of httpThreads is invalid: " + e.getMessage());
                return false;
            }
        }

        int httpBacklog = 0;
        if (argIndex < args.length && args[argIndex].equalsIgnoreCase("-httpBacklog")) {
            try {
                httpBacklog = Integer.parseInt(args[argIndex + 1]);
                argIndex += 2;
            } catch (IndexOutOfBoundsException e) {
                System.err.println("httpBacklog parameter requires a number of incoming connections in the backlog");
                return false;
            } catch (NumberFormatException e) {
                System.err.println("Size of httpBacklog is invalid: " + e.getMessage());
                return false;
            }
        }

        try {
            httpServer = HttpServer.create(new InetSocketAddress(httpPort), httpBacklog);
            if (httpThreads == 0) {
                httpServer.setExecutor(Executors.newCachedThreadPool());
            } else if (httpThreads > 1) {
                httpServer.setExecutor(Executors.newFixedThreadPool(httpThreads));
            }
            httpServerContexts = new HashMap<String, HttpContext>();
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


    //****************** Handler implementation ******************//

    /**
     * Parse query string parameters.
     * The query string has the following format:
     * <code>name1=value1&amp;name2=value2&amp;...</code>
     * Note that the query string can be get by
     * {@link HttpExchange#getRequestURI()}{@link java.net.URI#getQuery() .getQuery()}.
     *
     * @param query the string with URI-like query parameters
     * @param parameters an existing hash map to add the parsed parameters to or <tt>null</tt>
     * @return a hash map with parsed query string parameters (key represents the
     *          parameter name and value is the parameter value)
     */
    public static Map<String, String> parseParameters(String query, Map<String, String> parameters) {
        if (query == null || query.isEmpty()) {
            if (parameters == null)
                return Collections.emptyMap();
            return parameters;
        }
        Matcher matcher = paramParser.matcher(query);
        if (parameters == null)
            parameters = new HashMap<String, String>();
        while (matcher.find())
            try {
                parameters.put(matcher.group(1), URLDecoder.decode(matcher.group(2), "utf-8"));
            } catch (UnsupportedEncodingException e) {
                throw new InternalError("Charset utf-8 should be always supported, but there was " + e);
            }
        return parameters;
    }

    /**
     * Internal implementation of the {@link HttpHandler} that processes the action of the given control file.
     */
    private class HttpApplicationHandler implements HttpHandler {
        /** Control-file properties where the action is defined */
        private final Properties props;
        /** Action name to call */
        private final String actionName;
        /** Content type of the output */
        private final String contentType;
        /** Charset of the output */
        private final String charset;
        /** List of variables set by the application */
        private final Map<String, String> variables;
        /** List of context extractors that set internal local-thread variables */
        private final Map<ThreadLocal<Iterator<?>>, MultiExtractor<?>> contextExtractors;

        /**
         * Creates a new control-file action handler executed from a HTTP context.
         * @param props the control-file properties where the action is defined
         * @param actionName the action name to call
         * @param contextExtractorNames the map of named instances for extracted objects
         * @param contentType the content type of the output
         * @param charset the charset of the output
         * @param variables list of variables set by the application
         */
        private HttpApplicationHandler(Properties props, String actionName, Map<String, String> contextExtractorNames, String contentType, String charset, Map<String, String> variables) {
            this.props = props;
            this.actionName = actionName;
            this.contentType = contentType;
            this.charset = charset;
            this.variables = new HashMap<String, String>(variables);
            if (contextExtractorNames != null && !contextExtractorNames.isEmpty()) {
                contextExtractors = new LinkedHashMap<ThreadLocal<Iterator<?>>, MultiExtractor<?>>(contextExtractorNames.size());
                for (Map.Entry<String, String> contextExtractorName : contextExtractorNames.entrySet()) {
                    Object extractor = getNamedInstance(contextExtractorName.getValue());
                    if (extractor == null)
                        throw new IllegalArgumentException("Extractor instance '" + contextExtractorName.getValue() + "' does not exist");
                    ThreadLocal<Iterator<?>> variable = new InheritableThreadLocal<Iterator<?>>();
                    if (!addNamedInstance(contextExtractorName.getKey(), variable, false))
                        throw new IllegalArgumentException("Duplicate identifier for named instance: " + contextExtractorName.getKey());
                    contextExtractors.put(variable, Extractors.castToMultiExtractor(extractor, LocalAbstractObject.class, true));
                }
            } else {
                contextExtractors = null;
            }
        }

        /**
         * Handles a HTTP request by executing a control-file action and returns its response.
         *
         * @param exchange the HTTP request/response exchange
         * @throws IOException if there was a problem reading the HTTP request or writing the HTTP response
         */
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-type", contentType + ";charset=" + charset);            
            try {
                Map<String, String> requestVariables = getVariables(exchange);
                updateContextExtractors(exchange, requestVariables);
                handleWithBufferedOutput(exchange, requestVariables);
            } catch (InvocationTargetException e) {
                handleThrowable(exchange, e.getCause());
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                handleThrowable(exchange, e);
            }
        }

        /**
         * Handles a HTTP request by executing a control-file action and returns its response to buffered HTTP output.
         * Note that this method handles invalid arguments passed to action but requires more memory to buffer
         * the whole output of the action.
         * 
         * @param exchange the HTTP request/response exchange
         * @param requestVariables an independent map of variables for this request
         * @throws IOException if there was a problem reading the HTTP request or writing the HTTP response
         * @throws InvocationTargetException if there was an exception during the action execution 
         */
        private void handleWithBufferedOutput(HttpExchange exchange, Map<String, String> requestVariables) throws IOException, InvocationTargetException {
            ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
            PrintStream out = new PrintStream(outBuffer, true, charset);
            int errorCode = controlFileExecuteAction(out, props, actionName, requestVariables, null, true) ? ERROR_CODE_SUCCESS : ERROR_CODE_INVALID_ARGUMENT;
            exchange.sendResponseHeaders(errorCode, outBuffer.size());
            out.close();
            OutputStream response = exchange.getResponseBody();
            outBuffer.writeTo(response);
            response.close();
        }

        /**
         * Handles a HTTP request by executing a control-file action and returns its response directly to HTTP output.
         * Note that this method cannot handle invalid execution correctly.
         * 
         * @param exchange the HTTP request/response exchange
         * @param requestVariables an independent map of variables for this request
         * @throws IOException if there was a problem reading the HTTP request or writing the HTTP response
         * @throws InvocationTargetException if there was an exception during the action execution 
         */
        private void handleWithDirectOutput(HttpExchange exchange, Map<String, String> requestVariables) throws IOException, InvocationTargetException { // This is currently not used
            exchange.sendResponseHeaders(ERROR_CODE_SUCCESS, 0);
            PrintStream out = new PrintStream(exchange.getResponseBody(), true, charset);
            controlFileExecuteAction(out, props, actionName, requestVariables, null, true);
            out.close();
        }

        /**
         * Handles a HTTP request by executing a control-file action and returns its response directly to HTTP output.
         * Note that this method cannot handle invalid execution correctly.
         * 
         * @param exchange the HTTP request/response exchange
         * @param throwable the {@link Throwable} exception to handle
         * @throws IOException if there was a problem reading the HTTP request or writing the HTTP response
         * @throws NullPointerException if the given {@code throwable} was <tt>null</tt>
         */
        private void handleThrowable(HttpExchange exchange, Throwable throwable) throws IOException, NullPointerException {
            // Unwrap the basic cause
            while (throwable.getCause() != null)
                throwable = throwable.getCause();
            byte[] outBuffer = throwable.toString().getBytes(charset);
            exchange.sendResponseHeaders(ERROR_CODE_INTERNAL_ERROR, outBuffer.length);
            OutputStream response = exchange.getResponseBody();
            response.write(outBuffer);
            response.close();
        }

        /**
         * Returns an independent map of variables that are updated with values from the HTTP query string.
         * @param exchange the HTTP exchange from which to get the query string
         * @return map of variables
         */
        private Map<String, String> getVariables(HttpExchange exchange) {
            return parseParameters(exchange.getRequestURI().getQuery(), new HashMap<String, String>(variables));
        }

        /**
         * Execute context extractors.
         * @param exchange the HTTP request/response exchange
         * @param requestVariables an independent map of variables for this request
         * @throws ExtractorException if there was a problem with one of the context extractors
         */
        private void updateContextExtractors(HttpExchange exchange, Map<String, String> requestVariables) throws ExtractorException {
            if (contextExtractors == null || contextExtractors.isEmpty())
                return;
            ExtractorDataSource dataSource = new ExtractorDataSource(exchange.getRequestBody(), requestVariables);
            for (Map.Entry<ThreadLocal<Iterator<?>>, MultiExtractor<?>> contextExtractor : contextExtractors.entrySet()) {
                try {
                    contextExtractor.getKey().set(contextExtractor.getValue().extract(dataSource));
                } catch (IOException e) {
                    throw new ExtractorException("Error executing context extractor: " + e, e);
                }
            }
        }
    }

    /**
     * Authenticator for the {@link com.sun.net.httpserver.HttpServer HTTP server}
     * that provides a basic HTTP authentication based on the stored user/password
     * values.
     * 
     * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
     * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
     * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
     */
    public static class HttpApplicationAuthenticator extends BasicAuthenticator {

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
}
