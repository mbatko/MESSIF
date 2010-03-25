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
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import messif.executor.MethodExecutor.ExecutableMethod;
import messif.objects.LocalAbstractObject;
import messif.utility.http.HttpApplicationAuthenticator;
import messif.utility.http.HttpApplicationUtils;

/**
 * Provides a HTTP extension to {@link Application} that allows to execute operations
 * via REST-like services using HTTP protocol.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class HttpApplication extends Application {

    //****************** Attributes ******************//

    /** HTTP server for algorithm API */
    private HttpServer httpServer;
    /** Remembered list of contexts */
    private Map<String, HttpContext> httpServerContexts;


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
     * MESSIF &gt;&gt;&gt; httpAddContext /search messif.operations.query.ApproxKNNQueryOperation messif.objects.extraction.Extractors.createTextExtractor(messif.objects.impl.MetaObjectMap) "k" REMOTE_OBJECTS
     * </pre>
     * This will create a Context that will execute {@link messif.operations.query.ApproxKNNQueryOperation}
     * using a three parameters. The first parameter will be instance of an extractor created by call to
     * {@link messif.objects.extraction.Extractors#createTextExtractor} with
     * a {@link messif.objects.impl.MetaObjectMap} parameter. This extractor will be used
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
                    HttpApplicationUtils.createHandler(log, algorithm, args, 2, args.length - 2, namedInstances)
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

}
