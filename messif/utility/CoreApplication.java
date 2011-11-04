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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import messif.algorithms.Algorithm;
import messif.algorithms.AlgorithmMethodException;
import messif.executor.MethodExecutor;
import messif.executor.MethodExecutor.ExecutableMethod;
import messif.executor.MethodNameExecutor;
import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractStreamObjectIterator;
import messif.objects.util.RankedAbstractObject;
import messif.objects.util.RankedSortedCollection;
import messif.objects.util.StreamGenericAbstractObjectIterator;
import messif.operations.AbstractOperation;
import messif.operations.QueryOperation;
import messif.operations.RankingQueryOperation;
import messif.statistics.OperationStatistics;
import messif.statistics.Statistics;
import messif.utility.reflection.ConstructorInstantiator;
import messif.utility.reflection.InstantiatorSignature;
import messif.utility.reflection.MethodInstantiator;
import messif.utility.reflection.NoSuchInstantiatorException;


/**
 * This class implements a standalone client for MESSIF-enabled algorithms.
 * Via the client, a user can use methods provided in this class. For example,
 * an algorithm is started by {@link #algorithmStart}.
 * To start the client issue the following command:
 * <pre>
 *      java -classpath MESSIF.jar:&lt;algorithm's jar file or directory&gt; messif.utility.CoreApplication [parameters]
 * </pre>
 * The parameters can be any combination of
 * <ul>
 *   <li><code>&lt;cmdport&gt;</code> a TCP port with telnet interface</li>
 *   <li><code>-register &lt;host&gt;:&lt;port&gt;</code> send UDP "alive" announcements to the specified &lt;host&gt;:&lt;port&gt;</li>
 *   <li><code>&lt;controlFile&gt; [action] [var=value ...]]</code> executes <code>action</code> in the specified <code>controlFile</code> (optionally with setting variables)</li>
 * </ul>
 * 
 * The telnet interface (when <tt>cmdport</tt> is specified) allows to execute {@link CoreApplication}'s methods.
 * Automatic (context) help is generated whenever user provides incorrect data. For example, entering empty text on the
 * MESSIF prompt will list all available commands. Type just the command name to get help on its arguments (of course
 * there are commands without arguments that will get executed that way).
 * For example, to get information about the last executed operation a method {@link #operationInfo} is offered
 * by {@link CoreApplication}. To use it from the command line, do something like:
 * <pre>
 * .... telnet &lt;localhost&gt; &lt;cmdport&gt;
 * MESSIF >>> operationInfo
 * Range query .... returned 8 objects
 * MESSIF >>> 
 * </pre>
 * 
 * The <i>control file</i> is another way of issuing application commands. It allows to
 * prepare batches of commands that can be run either immediately after the process is started
 * (see the parameters above) or through the {@link #controlFile} method.
 * The control file is a text file with the following syntax. Empty lines or lines beginning
 * with # are ignored. All other lines are actions with the following syntax:
 * <pre>
 *  &lt;actionName&gt; = &lt;methodName | otherActionName1 otherActionName2 ...&gt;
 *  &lt;actionName&gt;.param.1 = &lt;first parameter of the method methodName&gt;
 *  &lt;actionName&gt;.param.2 = &lt;second parameter of the method methodName&gt;
 *  &lt;actionName&gt;.param.3 = &lt;third parameter of the method methodName&gt;
 *  &lt;actionName&gt;.param.4 = &lt;fourth parameter of the method methodName&gt;
 *  ...
 *  &lt;actionName&gt;.repeat = &lt;repeats&gt;
 *  &lt;actionName&gt;.foreach = &lt;value&gt; &lt;value&gt; ...
 *  &lt;actionName&gt;.repeatUntilException = &lt;some exception class, e.g. java.util.NoSuchElementException&gt;
 *  &lt;actionName&gt;.outputFile = &lt;filename&gt;
 *  &lt;actionName&gt;.assign = &lt;variable name&gt;
 *  &lt;actionName&gt;.postponeUntil = hh:mm:ss</pre>
 * <ul>
 * <li>&lt;actionName&gt; is a user specified name for the action which can be referred from other
 *                    actions (&lt;otherActionName1&gt; &lt;otherActionName2&gt;) or command line parameter <i>[action]</i>.</li>
 * <li>&lt;methodName&gt; can be any {@link CoreApplication} method, which is to be executed if &lt;actionName&gt; is called.
 *                    If a space-separated list of other action names is provided, they will be executed one by one
 *                    in the order they were specified. Parameters for the method are specified using &lt;actionName&gt;.param.<i>x</i>,
 *                    see the documentation of the respective {@link CoreApplication} methods for their parameters.</li>
 * <li><i>repeat</i> parameter is optional and allows to specify multiple execution of
 *                 the same action &lt;repeats&gt; times. It can be used together with "block" method name to implement
 *                 a loop of commands with specified number of repeats. In each iteration the variable &lt;actionName&gt;
 *                 is assigned the number of the actual iteration (starting from 1).</li>
 * <li><i>foreach</i> parameter is also optional and similarly to <i>repeat</i> it allows the action to be
 *                executed multiple times - the number of repeats is equal to the number of values provided.
 *                Moreover, in each iteration the variable &lt;actionName&gt; is assigned &lt;value&gt; taken
 *                one by one from the <i>foreach</i> parameter.</li>
 * <li><i>repeatUntilException</i> parameter is optional and allows to stop repeating the action
 *                when the exception given as the value of this parameter occurs. Note
 *                that if either "repeat" or "foreach" parameter is also specified,
 *                the repeating ends after their number of repeats or an exception
 *                whichever comes first. If no "repeat" or "foreach" is specified
 *                the action is repeated until an exception occurs.
 * <li><i>outputFile</i> parameter is optional and allows to redirect output of this block to a file
 *  &lt;filename&gt;. When this filename is reached for the first time, it is opened for writing
 *  (previous contents are destroyed) and all successive writes are appended to this file
 *  until this batch run finishes.</li>
 * <li><i>assign</i> parameter is optional and allows to redirect output of this block to a variable
 *  &lt;variable name&gt;. The previous contents of the variable are replaced by the new value and the
 *  variable is available after the action with "assign" is finished.</li>
 * <li><i>postponeUntil</i> parameter is optional and allows to postpone the action until the specified
 *  time. The whole execution of the control file is paused. If the specified time is in the past,
 *  this parameter is ignored. Note that the postponeUntil is working within one day.</li>
 * </ul>
 * <p>
 * All parameters, method name and output file are subject to variable expansion.
 * Variables can be specified as additional arguments to controlFile command and referred
 * to using "&lt;" and "&gt;" delimiters. For example:
 * <pre>
 *  execmyop = operationExecute
 *  execmyop.param.1 = messif.operations.&lt;myop&gt;
 *  execmyop.param.2 = &lt;myparam1:0&gt;
 *  execmyop.param.3 = ...
 * </pre>
 * This action will execute the operation whose name is provided in the variable <i>myop</i>.
 * If the variable is not set, it is replaced by an empty string, which in this particular
 * case will result in error. Therefore, it is possible to provide a default value for
 * a variable by appending a colon and the default value to the variable name.
 * This is shown in the second parameter in the example above - if the
 * <i>myparam1</i> variable is not set, the zero value is used for the execmyop.param.2.
 * </p>
 * <p>
 * The default action name that is looked up in the control file is <i>actions</i>
 * if another name is not provided on command line or by a parameter.
 * </p>
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class CoreApplication {
    /** Logger */
    protected static final Logger log = Logger.getLogger("application");

    /** Currently running algorithm */
    protected Algorithm algorithm = null;

    /** List of running algorithms */
    protected List<Algorithm> algorithms = new ArrayList<Algorithm>();

    /** Last executed operation */
    protected AbstractOperation lastOperation = null;

    /** Regular expression for binding {@link messif.statistics.OperationStatistics} in every {@link #operationExecute} call */
    protected String bindOperationStatsRegexp = null;

    /** Internal list of methods that can be executed */
    protected final MethodExecutor methodExecutor;

    /** List of currently created named instances */
    protected final Map<String, Object> namedInstances = new HashMap<String, Object>();

    /** Socket used for command communication */
    protected ServerSocketChannel cmdSocket;

    /**
     * Create new instance of CoreApplication.
     * The instance is initialized from the {@link #main} method.
     */
    protected CoreApplication() {
        methodExecutor = new MethodNameExecutor(this, PrintStream.class, String[].class);
    }

    /**
     * Log an exception with a {@link Level#SEVERE} level.
     * @param e the exception to log
     */
    protected static void logException(Throwable e) {
        log.log(Level.SEVERE, e.getClass().toString(), e);
    }

    /**
     * Returns the cause exception unwrapped from the {@link InvocationTargetException}s.
     * @param invocationTargetException the {@link InvocationTargetException} to unwrap
     * @return the cause exception
     */
    protected Throwable getRootCauseInvocationTargetException(InvocationTargetException invocationTargetException) {
        Throwable cause = invocationTargetException.getCause();
        while (cause instanceof InvocationTargetException)
            cause = cause.getCause();
        return cause;
    }

    /**
     * Process exception from exception.
     * If the passed exception is either {@link InvocationTargetException} or {@link AlgorithmMethodException},
     * the causing exception is unwrapped first.
     * @param exception the exception to process
     * @param out the output stream where to show the error (if not <tt>null</tt>)
     * @param logException if <tt>true</tt>, the exception is logged via {@link #logException(java.lang.Throwable)}
     */
    protected void processException(Throwable exception, PrintStream out, boolean logException) {
        String message = null;
        if (exception instanceof InvocationTargetException || exception instanceof AlgorithmMethodException) {
            message = exception.getMessage();
            exception = exception.getCause();
        }
        if (logException)
            logException(exception);
        if (out != null) {
            out.println(exception);
            if (message != null)
                out.println(message);
        }
    }


    //****************** Algorithm command functions ******************//

    /**
     * Creates a new instance of algorithm.
     * This application is only a wrapper class, it doesn't provide
     * any searching or indexing capabilities. To actually store and query data,
     * an algorithm must be created using this method. To implement an algorithm,
     * simply inherit from the {@link messif.algorithms.Algorithm}.
     * 
     * <p>
     * To create algorithm, at least the name of the algorithm class must be provided.
     * Additional parameters are passed to the algorithm's constructor. The following
     * example starts ExampleTree algorithm from package exampletree (do not forget
     * to add a jar file with ExampleTree).
     * <pre>
     * MESSIF &gt;&gt;&gt; algorithmStart messif.algorithms.impl.ParallelSequentialScan 4
     * </pre>
     * Note, that the name of the class is provided fully qualified.
     * The number 4 (after the class name) is passed to the ParallelSequentialScan's constructor
     * (see {@link messif.algorithms.impl.ParallelSequentialScan} for more informations).
     * If some wrong constructor parameters are specified, the constructor annotations are shown for the class.
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args algorithm class followed by constructor arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "start specified algorithm instance", arguments = {"algorithm class", "arguments for constructor ..."})
    public boolean algorithmStart(PrintStream out, String... args) {
        if (args.length < 2) {
            out.println("algorithmStart requires a class parameter (see 'help algorithmStart')");
            return false;
        }

        // Get class from the first argument
        Class<Algorithm> algorithmClass;
        try {
            algorithmClass = Convert.getClassForName(args[1], Algorithm.class);
        } catch (ClassNotFoundException e) {
            out.println("Can't find algorithm class: " + e.getMessage());
            return false;
        }

        // Get all constructors of the specified algorithm class
        Constructor<Algorithm>[] constructors = Algorithm.getAnnotatedConstructorsArray(algorithmClass);
        try {
            // Create a new instance of the algorithm
            algorithm = ConstructorInstantiator.createInstanceWithStringArgs(constructors, args, 2, args.length - 1, namedInstances);
            algorithms.add(algorithm);
            return true;
        } catch (Exception e) {
            Throwable ex = e;
            while (ex instanceof InvocationTargetException)
                ex = ex.getCause();
            logException((ex != null)?ex:e);
            out.println((ex != null)?ex:e);
            out.println("---------------- Available constructors ----------------");
            
            for (Constructor<Algorithm> constructor : constructors)
                out.println(Algorithm.getConstructorDescription(constructor));
            return false;
        }
    }

    /**
     * Restores a previously serialized algorithm from file.
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; algorithmRestore /some/where/file/algorithm.serialized
     * </pre>
     * 
     * @param out a stream where the application writes information for the user
     * @param args file name with the serialized algorithm
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     * @see #algorithmStore
     */
    @ExecutableMethod(description = "loads the algorithm from a given file", arguments = {"file name" })
    public boolean algorithmRestore(PrintStream out, String... args) {
        if (args.length < 2) {
            out.println("algorithmRestore requires a file name parameter (see 'help algorithmRestore')");
            return false;
        }

        try {
            // Load algorithm from file
            algorithm = Algorithm.restoreFromFile(args[1]);
            algorithms.add(algorithm);

            return true;
        } catch (IOException e) {
            out.println(e.toString());
        } catch (ClassNotFoundException e) {
            out.println(e.toString());            
        }
        return false;
    }

    /**
     * Serialize current algorithm to file.
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; algorithmStore /some/where/file/algorithm.serialized
     * </pre>
     * 
     * @param out a stream where the application writes information for the user
     * @param args file name where the serialized algorithm is stored
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     * @see #algorithmRestore
     */
    @ExecutableMethod(description = "save the algorithm to the given file", arguments = {"file or dir name"})
    public boolean algorithmStore(PrintStream out, String... args) {
        if (args.length < 2) {
            out.println("algorithmStore requires a file name parameter (see 'help algorithmStore')");
            return false;
        }

        try {
            if (algorithm != null) {
                // Store algorithm to file
                algorithm.storeToFile(args[1]);
                return true;
            } else out.println("No running algorithm is selected");
        } catch (IOException e) {
            out.println(e.toString());
        }
        return false;
    }

    /**
     * Stops current algorithm and clear the memory used.
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; algorithmStop
     * </pre>
     * 
     * @param out a stream where the application writes information for the user
     * @param args this method has no arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "stop current algorithm", arguments = {})
    public boolean algorithmStop(PrintStream out, String... args) {
        try {
            if (algorithm != null)
                algorithm.finalize();
        } catch (Throwable e) {
            out.println(e.toString());
        } finally {
            algorithms.remove(algorithm);
            algorithm = null;
        }
        return true;
    }

    /**
     * Stops all algorithms and clear the memory used.
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; algorithmStopAll
     * </pre>
     * 
     * @param out a stream where the application writes information for the user
     * @param args this method has no arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "stop all algorithms", arguments = {})
    public boolean algorithmStopAll(PrintStream out, String... args) {
        for (Algorithm alg : algorithms)
            try {
                if (alg != null)
                    alg.finalize();
            } catch (Throwable e) {
                out.println(e.toString());
            }
        algorithm = null;
        algorithms.clear();
        return true;
    }

    /**
     * Show some information about the current algorithm.
     * The text returned by algorithm's {@link Object#toString} method is used.
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; algorithmInfo
     * </pre>
     * 
     * @param out a stream where the application writes information for the user
     * @param args this method has no arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "show info about current algorithm", arguments = {})
    public boolean algorithmInfo(PrintStream out, String... args) {
        if (algorithm != null) {
            out.println(algorithm.toString());
            return true;
        } else out.println("No running algorithm is selected");
        return false;
    }

    /**
     * Show some information about all algorithms.
     * The text returned by algorithms' {@link Object#toString} method is used.
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; algorithmInfoAll
     * </pre>
     * 
     * @param out a stream where the application writes information for the user
     * @param args this method has no arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "show info about all algorithms", arguments = {})
    public boolean algorithmInfoAll(PrintStream out, String... args) {
        if (algorithms.isEmpty()) {
            out.println("No algorithm is running");
            return false;
        } else {
            for (int i = 0; i < algorithms.size(); i++) {
                out.print("Algorithm #");
                out.print(i);
                out.println(":");
                out.println(algorithms.get(i).toString());
            }
            return true;
        }
    }

    /**
     * Select algorithm to manage.
     * A parameter with algorithm sequence number is required for specifying, which algorithm to select.
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; algorithmSelect 0
     * </pre>
     * 
     * @param out a stream where the application writes information for the user
     * @param args the algorithm sequence number
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     * @see #algorithmInfoAll
     */
    @ExecutableMethod(description = "select algorithm to manage", arguments = {"# of the algorithm to select"})
    public boolean algorithmSelect(PrintStream out, String... args) {
        try {
            algorithm = algorithms.get(Integer.parseInt(args[1]));
            return true;
        } catch (IndexOutOfBoundsException ignore) {
        } catch (NumberFormatException ignore) {
        }

        out.print("Algorithm # must be specified - use a number between 0 and ");
        out.println(algorithms.size() - 1);

        return false;
    }

    /**
     * Show information about supported operations for the current algorithm.
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; algorithmSupportedOperations
     * </pre>
     *
     * @param out a stream where the application writes information for the user
     * @param args this method has no arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     * @see #operationExecute
     */
    @ExecutableMethod(description = "show all operations supported by current algorithm", arguments = {})
    public boolean algorithmSupportedOperations(PrintStream out, String... args) {
        if (algorithm == null) {
            out.println("No running algorithm is selected");
            return false;
        }

        out.println("---------------- Available operations ----------------");
        for (Class<? extends AbstractOperation> opClass : algorithm.getSupportedOperations())
            try {
                out.println(AbstractOperation.getConstructorDescription(opClass));
            } catch (NoSuchMethodException ex) {
                out.println(opClass.getName() + " can be processed but not instantiated");
            }

        return true;
    }


    //****************** Operation command functions ******************//

    /**
     * Creates an operation from the given parameters.
     * This is internal method used in various operationXXX methods.
     *
     * @param out a stream where the application writes information for the user
     * @param args operation class followed by constructor arguments
     * @return an instance of operation or <tt>null</tt> if the specified arguments are invalid
     * @throws InvocationTargetException if there was an error while creating an instance of the operation
     */
    private AbstractOperation createOperation(PrintStream out, String[] args) throws InvocationTargetException {
        if (args.length < 2) {
            out.println("The class of the operation must be specified (see 'help " + args[0] + "')");
            return null;
        }

        // Get operation class from the first argument
        Class<AbstractOperation> operationClass;
        try {
            operationClass = Convert.getClassForName(args[1], AbstractOperation.class);
        } catch (ClassNotFoundException e) {
            out.println("Can't find operation class: " + e.getMessage());
            return null;
        }

        // Create new instance of the operation
        try {
            return AbstractOperation.createOperation(
                    operationClass,
                    Convert.parseTypesFromString(
                        args,
                        AbstractOperation.getConstructorArguments(operationClass, args.length - 2),
                        false, // do not use var-args, since number of constructor parameters is given
                        2, // skip the method name and operation class arguments
                        namedInstances
                    )
            );
        } catch (NoSuchMethodException e) {
            out.println(e.toString());
        } catch (InstantiationException e) {
            out.println(e.toString());
        }

        // Show operation description if there was an error
        try {
            String description = AbstractOperation.getConstructorDescription(operationClass);
            out.println("---------------- Operation parameters ----------------");
            out.println(description);
        } catch (NoSuchMethodException e) {
            out.println(e.getMessage());
        }
        return null;
    }


    /**
     * Prepares a new instance of the specified operation without executing it.
     * Similarly to the {@link #algorithmStart}, the name of operation's class
     * must be provided and all the additional arguments are passed to its constructor.
     * The operation can be modified using {@link #operationChangeAnswerCollection}
     * or {@link #operationParam} and the executed by {@link #operationExecuteAgain}.
     * Note that if there is another call to {@link #operationPrepare}, {@link #operationExecute},
     * or {@link #operationBgExecute}, the prepared operation is replaced.
     *
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; operationPrepare messif.operations.query.RangeQueryOperation objects 1.3
     * </pre>
     * Note that the {@link messif.operations.query.RangeQueryOperation range query operation}
     * requires two parameters - a {@link messif.objects.LocalAbstractObject}
     * and a radius. The {@link messif.objects.LocalAbstractObject} is usually entered
     * as a next object from a stream (see {@link #objectStreamOpen}).
     *
     * @param out a stream where the application writes information for the user
     * @param args operation class followed by constructor arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     * @throws InvocationTargetException if there was an error while creating an instance of the operation
     */
    @ExecutableMethod(description = "prepare the specified operation", arguments = {"operation class", "arguments for constructor ..."})
    public boolean operationPrepare(PrintStream out, String... args) throws InvocationTargetException {
        lastOperation = createOperation(out, args);
        return lastOperation != null;
    }

    /**
     * Executes a specified operation on current algorithm.
     * Operations allows querying and manipulating data stored by the algorithm.
     * If no argument for operationExecute is provided, a list of supported operations
     * is shown. In order to execute an operation, an operation instance must be created.
     * Similarly to the {@link #algorithmStart}, the name of operation's class
     * must be provided and all the additional arguments are passed to its constructor.
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; operationExecute messif.operations.query.RangeQueryOperation objects 1.3
     * </pre>
     * Note that the {@link messif.operations.query.RangeQueryOperation range query operation}
     * requires two parameters - a {@link messif.objects.LocalAbstractObject}
     * and a radius. The {@link messif.objects.LocalAbstractObject} is usually entered
     * as a next object from a stream (see {@link #objectStreamOpen}).
     * 
     * @param out a stream where the application writes information for the user
     * @param args operation class followed by constructor arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     * @throws InvocationTargetException if there was an error while creating an instance of the operation
     * @throws AlgorithmMethodException if there was an error executing the operation
     */    
    @ExecutableMethod(description = "execute specified operation on current algorithm instance", arguments = {"operation class", "arguments for constructor ..."})
    public boolean operationExecute(PrintStream out, String... args) throws InvocationTargetException, AlgorithmMethodException {
        if (algorithm == null) {
            out.println("No running algorithm is selected");
            return false;
        }

        AbstractOperation operation = createOperation(out, args);
        if (operation == null)
            return false;

        try {
            // Execute operation
            lastOperation = algorithm.setupStatsAndExecuteOperation(operation, bindOperationStatsRegexp);
            return true;
        } catch (NoSuchMethodException e) {
            out.println(e.getMessage());
            algorithmSupportedOperations(out, args);
            return false;
        }
    }

    /**
     * Executes a specified operation on current algorithm in a new thread (i.e., on the background).
     * Operations allows querying and manipulating data stored by the algorithm.
     * If no argument for operationBgExecute is provided, a list of supported operations
     * is shown. In order to execute an operation, an operation instance must be created.
     * Similarly to the {@link #algorithmStart}, the name of operation's class
     * must be provided and all the additional arguments are passed to its constructor.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; operationBgExecute messif.operations.query.RangeQueryOperation objects 1.3
     * </pre>
     * </p>
     * 
     * <p>
     * Note that the last operation is updated, however, the control is returned immediately.
     * So if there is another operation executed meanwhile (either background or normal), the results
     * of this operation will be replaced. Use {@link #operationWaitBg} method to wait for the operation
     * to finish.
     * </p>
     * @param out a stream where the application writes information for the user
     * @param args operation class followed by constructor arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     * @throws InvocationTargetException if there was an error while creating an instance of the operation
     */
    @ExecutableMethod(description = "execute on background specified operation on current algorithm instance", arguments = {"operation class", "arguments for constructor ..."})
    public boolean operationBgExecute(PrintStream out, String... args) throws InvocationTargetException {       
        if (algorithm == null) {
            out.println("No running algorithm is selected");
            return false;
        }

        AbstractOperation operation = createOperation(out, args);
        if (operation == null)
            return false;

        try {
            // Execute operation
            OperationStatistics.resetLocalThreadStatistics();
            if (bindOperationStatsRegexp != null)
                OperationStatistics.getLocalThreadStatistics().registerBoundAllStats(bindOperationStatsRegexp);
            algorithm.backgroundExecuteOperation(operation);
            return true;
        } catch (NoSuchMethodException e) {
            out.println(e.getMessage());
            algorithmSupportedOperations(out, args);
            return false;
        }
    }

    /**
     * Synchronize on all operations run on the background.
     * After this method finishes, there are no running operations on background.
     *  
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; operationWaitBg
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args this method has no arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     * @throws AlgorithmMethodException if there was an error executing the operation
     */
    @ExecutableMethod(description = "wait for all background operations", arguments = {})
    public boolean operationWaitBg(PrintStream out, String... args) throws AlgorithmMethodException {
        if (algorithm == null) {
            out.println("No running algorithm is selected");
            return false;
        }

        try {
            List<AbstractOperation> waitBackgroundExecuteOperation = algorithm.waitBackgroundExecuteOperation();
            if (! waitBackgroundExecuteOperation.isEmpty()) {
                lastOperation = waitBackgroundExecuteOperation.get(0);
            }
            if (bindOperationStatsRegexp != null)
                OperationStatistics.getLocalThreadStatistics().unbindAllStats(bindOperationStatsRegexp);
            return true;
        } catch (InterruptedException e) {
            out.println(e.toString());
            return false;
        }
    }
    
    /**
     * Executes the last operation once more.
     * Note that the operation instance remains the same except for its answer, which
     * might be reset, if <tt>true</tt> is passed as an argument. The default behavior
     * is <em>not</em> to reset the answer.
     * Statistics are always reset.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; operationExecuteAgain true
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args flag whether to reset operation answer
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     * @throws AlgorithmMethodException if there was an error executing the operation
     */    
    @ExecutableMethod(description = "execute the last operation once more", arguments = {"boolean whether to reset operation answer (default: false)"})
    public boolean operationExecuteAgain(PrintStream out, String... args) throws AlgorithmMethodException {
        try {
            AbstractOperation operation = lastOperation;
            if (algorithm != null && operation != null) {
                // Execute operation
                OperationStatistics.resetLocalThreadStatistics();
                if (bindOperationStatsRegexp != null)
                    OperationStatistics.getLocalThreadStatistics().registerBoundAllStats(bindOperationStatsRegexp);
                if (args.length >= 2 && args[1].equalsIgnoreCase("true") && operation instanceof QueryOperation)
                    ((QueryOperation)operation).resetAnswer();
                algorithm.executeOperation(operation);
                if (bindOperationStatsRegexp != null)
                    OperationStatistics.getLocalThreadStatistics().unbindAllStats(bindOperationStatsRegexp);
                return true;
            } else {
                out.println("No operation has been executed yet. Use operationExecute method first.");
                return false;
            }
        } catch (NoSuchMethodException e) {
            out.println(e.getMessage());
            algorithmSupportedOperations(out, args);
            return false;
        }
    }

    /**
     * Show information about the last executed operation.
     * Specifically, the information about the operation created by the last call to
     * {@link #operationPrepare}, {@link #operationExecute}, or {@link #operationBgExecute}
     * is shown. Note that the operation might be still running if the
     * {@link #operationBgExecute} was used and thus the results might not be complete.
     * Use {@link #operationWaitBg} to wait for background operations to finish.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; operationInfo
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args this method has no arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */    
    @ExecutableMethod(description = "show information about the last executed operation", arguments = {})
    public boolean operationInfo(PrintStream out, String... args) {
        out.println(lastOperation);
        return true;
    }

    /**
     * Show argument of the last executed operation.
     * Specifically, the argument of the operation created by the last call to
     * {@link #operationPrepare}, {@link #operationExecute}, or {@link #operationBgExecute}
     * is shown. Note that the operation might be still running if the
     * {@link #operationBgExecute} was used and thus the results might not be complete.
     * Use {@link #operationWaitBg} to wait for background operations to finish.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; operationArgument 0
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args zero-based index of the argument to show
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */    
    @ExecutableMethod(description = "show an argument of the last executed operation", arguments = {"index of the argument to show"})
    public boolean operationArgument(PrintStream out, String... args) {
        if (lastOperation == null) {
            out.println("No operation has been executed yet. Use operationExecute method first.");
            return false;
        }

        // Read the argument
        int argIndex = -1;
        try {
            argIndex = Integer.parseInt(args[1]);
        } catch (IndexOutOfBoundsException ignore) {
            out.println("operationArgument method requires the index of the argument");
            return false;
        } catch (NumberFormatException ignore) {
        }
        if (argIndex < 0 || argIndex >= lastOperation.getArgumentCount()) {
            out.println("operationArgument index '" + args[1] + "' is not within <0;" + lastOperation.getArgumentCount() + ") bounds");
            return false;
        }

        // Display it
        out.println(lastOperation.getArgument(argIndex));
        return true;
    }

    /**
     * Show or set a parameter of the last executed operation.
     * Specifically, the parameter of the operation created by the last call to
     * {@link #operationPrepare}, {@link #operationExecute}, or {@link #operationBgExecute}
     * is shown. Note that the operation might be still running if the
     * {@link #operationBgExecute} was used and thus the results might not be complete.
     * Use {@link #operationWaitBg} to wait for background operations to finish.
     * Note that a set parameter can be used when {@link #operationExecuteAgain} is called.
     *
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; operationParam myparam 1 java.lang.Integer
     * MESSIF &gt;&gt;&gt; operationParam myparam
     * </pre>
     * </p>
     * The first example set the parameter "myparam" to "1" converted to integer.
     * The second example shows the value of the parameter myparam, i.e. "1" is displayed.
     *
     * @param out a stream where the application writes information for the user
     * @param args name of the parameter to show (if this is the only argument) or set (if a value is provided),
     *              the new value of the parameter, and the class of the parameter (must be {@link Convert#stringToType convertible})
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "show or set a parameter of the last executed operation", arguments = {"name of the parameter to show or set", "new value of the parameter (optional)", "class of the value being set (optional)"})
    public boolean operationParam(PrintStream out, String... args) {
        if (lastOperation == null) {
            out.println("No operation has been executed yet. Use operationExecute method first.");
            return false;
        }
        if (args.length < 2) {
            out.println("operationParam method requires the name of the parameter");
            return false;
        }

        if (args.length > 2) { // Set parameter
            // Read parameter class (defaults to String)
            Class<? extends Serializable> parameterClass;
            try {
                parameterClass = args.length > 3 ? Convert.getClassForName(args[3], Serializable.class) : String.class;
            } catch (ClassNotFoundException e) {
                out.println("Cannot set parameter " + args[1] + ": " + e);
                return false;
            }
            // Convert the parameter and set it
            try {
                lastOperation.setParameter(args[1], Convert.stringToType(args[2], parameterClass, namedInstances));
            } catch (InstantiationException e) {
                out.println("Cannot convert '" + args[2] + "' to " + parameterClass);
                return false;
            }
        } else { // Show parameter
            out.println(lastOperation.getParameter(args[1]));
        }
        return true;
    }

    /**
     * Changes the answer collection of the prepared or last executed operation.
     * This method is valid only if the last executed operation was
     * a descendant of {@link RankingQueryOperation}.
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; operationChangeAnswerCollection messif.objects.util.RankedSortedCollection
     * </pre>
     *
     * @param out a stream where the application writes information for the user
     * @param args answer collection class followed by its constructor arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "change the answer collection of the last executed operation", arguments = {"collection class", "arguments for constructor ..."})
    public boolean operationChangeAnswerCollection(PrintStream out, String... args) {
        AbstractOperation operation = lastOperation;
        if (operation == null) {
            out.println("No operation has been executed yet");
            return false;
        }
        if (!(operation instanceof RankingQueryOperation)) {
            out.println("Answer collection can be changed only for ranked results");
            return false;
        }
        if (args.length < 2) {
            out.println("operationChangeAnswerCollection requires a class (see 'help operationChangeAnswerCollection')");
            return false;
        }

        try {
            // Get sorted collection class
            Class<? extends RankedSortedCollection> clazz = Convert.getClassForName(args[1], RankedSortedCollection.class);

            // Create new instance of sorted collection
            RankedSortedCollection newAnswerCollection = ConstructorInstantiator.createInstanceWithStringArgs(Convert.getConstructors(clazz), args, 2, args.length - 1, namedInstances);

            // Set the instance in the operation
            ((RankingQueryOperation)operation).setAnswerCollection(newAnswerCollection);

            return true;
        } catch (ClassNotFoundException e) {
            out.println(e);
            return false;
        } catch (NoSuchInstantiatorException e) {
            out.println(e.getMessage());
            return false;
        } catch (InvocationTargetException e) {
            out.println(e.getCause());
            return false;
        }
    }

    /**
     * Changes the answer collection of the prepared or last executed operation.
     * This method is valid only if the last executed operation was
     * a descendant of {@link RankingQueryOperation}. The collection must be
     * prepared as a named instance first. Note that the collection will be cleared
     * and its contents replaced by the current operation answer.
     * 
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; operationChangeAnswerNamedInstance collection_named_instance
     * </pre>
     *
     * @param out a stream where the application writes information for the user
     * @param args answer collection named instance name
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "change the answer collection of the last executed operation", arguments = {"collection instance"})
    public boolean operationChangeAnswerNamedInstance(PrintStream out, String... args) {
        AbstractOperation operation = lastOperation;
        if (operation == null) {
            out.println("No operation has been executed yet");
            return false;
        }
        if (!(operation instanceof RankingQueryOperation)) {
            out.println("Answer collection can be changed only for ranked results");
            return false;
        }
        if (args.length < 2) {
            out.println("operationChangeAnswerNamedInstance requires a named instance (see 'help operationChangeAnswerNamedInstance')");
            return false;
        }
        Object newAnswerCollection = namedInstances.get(args[1]);
        if (newAnswerCollection == null || !(newAnswerCollection instanceof RankedSortedCollection)) {
            out.println("Named instance '" + args[1] + "' is not collection for the ranking query");
            return false;
        }
        ((RankingQueryOperation)operation).setAnswerCollection((RankedSortedCollection)newAnswerCollection);
        return true;
    }

    /**
     * Processes the last executed operation by a given method. The method
     * must be static and must have the {@link AbstractOperation} (or its
     * descendant) as its first argument. Additional arguments for the method
     * can be specified. The modified (or the original) operation must be returned
     * from the method.
     *
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; operationProcessByMethod somePackage.someClass someMethod methodArg2 methodArg3
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args the fully specified name of the class where the method is defined, the name of the method and
     *          any number of additional arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     * @throws InvocationTargetException if there was an error while executing the method
     */
    @ExecutableMethod(description = "process the last executed operation by a static method", arguments = {"object class", "method name", "additional arguments for the method (optional) ..."})
    public boolean operationProcessByMethod(PrintStream out, String... args) throws InvocationTargetException {
        AbstractOperation operation = lastOperation;
        if (operation == null) {
            out.println("No operation has been executed yet");
            return false;
        }

        try {
            // Prepare method
            MethodInstantiator<AbstractOperation> method = new MethodInstantiator<AbstractOperation>(AbstractOperation.class, Class.forName(args[1]), args[2], args.length - 2);

            // Prepare arguments
            String[] stringArgs = args.clone();
            stringArgs[2] = null;
            Object[] methodArgs = Convert.parseTypesFromString(stringArgs, method.getInstantiatorPrototype(), true, 2, namedInstances);
            methodArgs[0] = lastOperation;

            // Execute method
            lastOperation = method.instantiate(methodArgs);
            return true;
        } catch (ClassNotFoundException e) {
            out.println("Class not found: " + args[1]);
            return false;
        } catch (InstantiationException e) {
            out.println("Error converting string: " + e.getMessage());
            return false;
        } catch (NoSuchInstantiatorException e) {
            out.println(e.getMessage());
            return false;
        }
    }

    /**
     * Show the answer of the last executed query operation.
     * Specifically, the information about the operation created by last call to
     * {@link #operationExecute} or {@link #operationBgExecute} is shown. Note that
     * the operation might be still running if the {@link #operationBgExecute} was
     * used and thus the results might not be complete. Use {@link #operationWaitBg}
     * to wait for background operations to finish.
     * <p>
     * If the last operation was not {@link messif.operations.QueryOperation query} operation,
     * this method will fail.
     * </p>
     * <p>
     * An optional argument is accepted:
     *   <ul>
     *     <li>objects separator (defaults to newline)</li>
     *     <li>result type - can be 'All' = display everything,
     *            'Objects' = displays just objects, 'Locators' = display just locators,
     *             or 'DistanceLocators' = display format 'distance: locator'</li>
     *   </ul>
     * </p>
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; operationAnswer , Locators
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args display separator for the list of objects and type of the display
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */  
    @ExecutableMethod(description = "list objects retrieved by the last executed query operation", arguments = {"objects separator (not required)", "display All/Object/Locator (defaults to All)"})
    public boolean operationAnswer(PrintStream out, String... args) {
        AbstractOperation operation = lastOperation;
        if (operation == null || !(operation instanceof QueryOperation)) {
            out.println("The operationAnswer method must be called after some QueryOperation was executed");
            return false;
        }

        // Separator is second argument (get newline if not specified)
        String separator = (args.length > 1)?args[1]:System.getProperty("line.separator");
        switch ((args.length > 2 && args[2].length() > 0) ? Character.toUpperCase(args[2].charAt(0)) : 'A') {
            case 'A':
                Iterator<?> itAll = ((QueryOperation<?>)operation).getAnswer();
                while (itAll.hasNext()) {
                    out.print(itAll.next());
                    if (itAll.hasNext())
                        out.print(separator);
                }
                break;
            case 'O':
                Iterator<AbstractObject> itObjects = ((QueryOperation<?>)operation).getAnswerObjects();
                while (itObjects.hasNext()) {
                    out.print(itObjects.next());
                    if (itObjects.hasNext())
                        out.print(separator);
                }
                break;
            case 'L':
                itObjects = ((QueryOperation<?>)operation).getAnswerObjects();
                while (itObjects.hasNext()) {
                    out.print(itObjects.next().getLocatorURI());
                    if (itObjects.hasNext())
                        out.print(separator);
                }
                break;
            case 'D':
                Iterator<RankedAbstractObject> answer = ((RankingQueryOperation)operation).getAnswer();
                while (answer.hasNext()) {
                    RankedAbstractObject next = answer.next();
                    out.print(next.getDistance());
                    out.print(": ");
                    out.print(next.getObject().getLocatorURI());
                    if (answer.hasNext())
                        out.print(separator);
                }
                break;
        }
        out.println();

        return true;
    }


    //****************** Direct algoritm methods execution ******************//

    /**
     * Directly execute a method of the running algorithm.
     * The method name and its arguments must be provided.
     * Only {@link messif.utility.Convert#stringToType convertible} types can
     * be passed as arguments and if there are several methods with the same name,
     * the first one that matches the number of arguments is selected.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; methodExecute mySpecialAlgorithmMethod 1 false string_string
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args method name followed by the values for its arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "directly execute a method of the running algorithm", arguments = {"method name", "arguments for the method ..."})
    public boolean methodExecute(PrintStream out, String... args) {
        if (algorithm == null) {
            out.println("No running algorithm is selected");
            return false;
        }
        if (args.length < 2) {
            out.println("methodExecute requires at least the method name (see 'help methodExecute')");
            return false;
        }

        try {
            Object rtv = algorithm.executeMethodWithStringArguments(args, 1, namedInstances);
            if (rtv != null)
                out.println(rtv);
            return true;
        } catch (NoSuchInstantiatorException e) {
            out.println("Method '" + args[1] + "' with " + (args.length - 2) + " arguments was not found in algorithm");
            return false;
        } catch (Exception e) {
            logException(e);
            out.println(e.toString());
            return false;
        }
    }


    //****************** Statistics command functions ******************//

    /**
     * Disable (or enable) gathering of statistics.
     * If passed without parameters, statistics are disabled, so the other
     * statistic methods are useless.
     * If <tt>false</tt> is passed as parameter, statistics are enabled again.
     * By default, statistics are enabled when application starts.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; statisticsDisable false
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args flag whether to disable (true) or enable (false) the statistics
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */ 
    @ExecutableMethod(description = "enable/disable statistics - if disabled, all statistics are useless", arguments = { "false to enable statistics (not required)" })
    public boolean statisticsDisable(PrintStream out, String... args) {
        if (args.length <= 1 || Boolean.parseBoolean(args[1]))
            Statistics.disableGlobally();
        else Statistics.enableGlobally();
        return true;
    }

    /**
     * Print all global statistics.
     * Statistics are shown as <code>name: value</code>.
     * <p>
     * Three optional arguments are accepted:
     *   <ul>
     *     <li>regular expression applied on names as a filter</li>
     *     <li>separator of statistics (defaults to newline)</li>
     *     <li>separator appended after printed statistics (defaults to newline)</li>
     *   </ul>
     * </p>
     * <p>
     * Example of usage:
     * To print statistics comma-separated, use:
     * <pre>
     * MESSIF &gt;&gt;&gt; statisticsGlobal DistanceComputations.* ,
     * </pre>
     * To avoid appending newline and append comma, use:
     * <pre>
     * MESSIF &gt;&gt;&gt; statisticsGlobal DistanceComputations.* , ,
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args regular expression to match statistic names and the display separators for the statistic values
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */ 
    @ExecutableMethod(description = "show global statistics", arguments = { "statistic name regexp (not required)", "separator of statistics (not required)", "final separator (not required)" })
    public boolean statisticsGlobal(PrintStream out, String... args) {
        if (args.length >= 3) {
            String stats = Statistics.printStatistics(args[1], args[2]);
            if (args.length >= 4) {
                out.print(stats);
                out.print(args[3]);
            } else
                out.println(stats);
        } else if (args.length >= 2)
            out.println(Statistics.printStatistics(args[1]));
        else out.println(Statistics.printStatistics());
        return true;
    }

    /**
     * Gets a value from a global statistic.
     * If the global statistic does not exist, a new one is created.
     * <p>
     * Two arguments are required:
     *   <ul>
     *     <li>the name of the statistic</li>
     *     <li>the class of the statistic</li>
     *   </ul>
     * </p>
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; statisticsGlobalGet DistanceComputations messif.statistics.StatisticCounter
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args the name and class of the global statistic
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "get/create global statistic", arguments = { "statistic name", "statistic class" })
    public boolean statisticsGlobalGet(PrintStream out, String... args) {
        try {
            @SuppressWarnings("unchecked")
            Class<Statistics<?>> statClass = (Class)Convert.getClassForName(args[2], Statistics.class);
            out.println(Statistics.getStatistics(args[1], statClass));
        } catch (Exception e) {
            out.println("Cannot get global statistics: " + e);
            return false;
        }
        return true;
    }

    /**
     * Reset all global statistics.
     * An optional parameter is a regular expression applied on names as a filter.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; statisticsResetGlobal DistanceComputations
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args regular expression to match statistic names
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "reset global statistics", arguments = { "statistic name regexp (not required)" })
    public boolean statisticsResetGlobal(PrintStream out, String... args) {
        if (args.length > 1)
            Statistics.resetStatistics(args[1]);
        else Statistics.resetStatistics();
        return true;
    }

    /**
     * Print statistics gathered by the last executed operation.
     * Only the {@link messif.statistics.Statistics#bindTo bound} statistics are
     * reported. Usually, algorithms bind the relevant statistics automatically, but
     * it can be done explicitely using the {@link #statisticsSetAutoBinding} method.
     * Statistics are shown as <code>name: value</code>.
     * <p>
     * Three optional arguments are accepted:
     *   <ul>
     *     <li>regular expression applied on names as a filter</li>
     *     <li>separator of statistics (defaults to newline)</li>
     *     <li>separator appended after printed statistics (defaults to newline)</li>
     *   </ul>
     * </p>
     * <p>
     * Example of usage:
     * To print statistics comma-separated, use:
     * <pre>
     * MESSIF &gt;&gt;&gt; statisticsLastOperation DistanceComputations.* ,
     * </pre>
     * To avoid appending newline and append comma, use:
     * <pre>
     * MESSIF &gt;&gt;&gt; statisticsLastOperation DistanceComputations.* , ,
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args regular expression to match statistic names and the display separators for the statistic values
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */ 
    @ExecutableMethod(description = "show last operation statistics", arguments = { "statistic name regexp (not required)", "separator of statistics (not required)" })
    public boolean statisticsLastOperation(PrintStream out, String... args) {
        if (args.length >= 3) {
            String stats = algorithm.getOperationStatistics().printStatistics(args[1], args[2]);
            if (args.length >= 4) {
                out.print(stats);
                out.print(args[3]);
            } else
                out.println(stats);
        } else if (args.length >= 2)
            out.println(algorithm.getOperationStatistics().printStatistics(args[1]));
        else out.println(algorithm.getOperationStatistics().printStatistics());
        return true;
    }

    /**
     * Regular expression on global statistics' names that are bound for each executed operation.
     * A required argument sets the regular expression applied on names as a filter.
     * If the passed argument is <tt>null</tt>, the autobinding is disabled.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; statisticsSetAutoBinding .*
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args regular expression to match statistic names to bind
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */ 
    @ExecutableMethod(description = "set auto binding operation statistics to global ones", arguments = { "statistic name regexp (if null, auto binding is disabled)" })
    public boolean statisticsSetAutoBinding(PrintStream out, String... args) {
        if (args.length >= 2)
            bindOperationStatsRegexp = args[1];
        else bindOperationStatsRegexp = null;
        return true;
    }


    //****************** Object stream command functions ******************//

    /**
     * Open a named stream which allows to read {@link LocalAbstractObject objects} from a file.
     * The first required argument specifies a file name from which to open the stream.
     * The second required argument gives a fully-qualified name of the stored {@link LocalAbstractObject object class}.
     * The third required argument is a name under which the stream is opened.
     * 
     * <p>
     * If the name (third argument) is then specified in place where {@link LocalAbstractObject}
     * is argument required, the next object is read from the stream and used as the argument's value.
     * </p>
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; objectStreamOpen /my/data/file.xx messif.objects.impl.ObjectByteVectorL1 my_data
     * MESSIF &gt;&gt;&gt; operationExecute messif.operations.query.RangeQueryOperation my_data 1.3
     * MESSIF &gt;&gt;&gt; operationExecute messif.operations.query.kNNQueryOperation my_data 10
     * </pre>
     * 
     * Note that the first two objects are read from the stream file /my/data/file.xx, first is used
     * as a query object for the range query, the second is used in the k-NN query.
     * 
     * A second example with a special class that requires some additional constructor parameters:
     * <pre>
     * public class MyClass {
     *     public MyClass(BufferedReader stream, int value, String text) {
     *         ... construct object from the stream ...
     *     }
     * }
     * MESSIF &gt;&gt;&gt; objectStreamOpen /my/data/file.xx MyClass other_data 10 string_value
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args file name to read from,
     *             class name of objects to be read from the file,
     *             optional name of the object stream,
     *             optional additional arguments for the object constructor
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "create new stream of LocalAbstractObjects", arguments = { "filename", "class of objects in the stream", "name of the stream", "additional arguments for the object constructor (optional)" })
    public boolean objectStreamOpen(PrintStream out, String... args) {
        if (args.length < 4) {
            out.println("objectStreamOpen requires a filename, object class and name (see 'help objectStreamOpen')");
            return false;
        }
        try {
            // Build collection of additional arguments
            List<String> additionalArgs;
            if (args.length > 4) {
                additionalArgs = new ArrayList<String>();
                for (int i = 4; i < args.length; i++)
                    additionalArgs.add(args[i]);
            } else {
                additionalArgs = null;
            }

            // Store new stream into stream registry
            if (namedInstances.put(
                args[3],
                new StreamGenericAbstractObjectIterator<LocalAbstractObject>(
                    Convert.getClassForName(args[2], LocalAbstractObject.class),
                    args[1],
                    namedInstances,
                    additionalArgs
                )
            ) != null)
                out.println("Previously opened stream changed to a new file");
            return true;
        } catch (IOException e) {
            out.println(e.toString());
            return false;
        } catch (ClassNotFoundException e) {
            out.println(e.toString());
            return false;
        }
    }

    /**
     * Sets a value of additional constructor parameter of an opened object stream.
     * See {@link #objectStreamOpen} method for explanation of the concept of 
     * additional constructor parameters.
     * This method requires three arguments:
     * <ul>
     *   <li>the name of the stream the name of which to change,</li>
     *   <li>the the new value for the parameter,</li>
     *   <li>the constructor parameter index to change (zero-based).</li>
     * </ul>
     * The third argument is optional. If not specified, <tt>0</tt> is assumed.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; objectStreamSetParameter other_data new_string_value 1
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args name of the object stream, new parameter value to object's contructor, and zero-based index of the parameter
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "set parameter of objects' constructor", arguments = { "name of the stream", "parameter value", "index of parameter (not required -- zero if not given)" })
    public boolean objectStreamSetParameter(PrintStream out, String... args) {
        if (args.length < 3) {
            out.println("objectStreamSetParameter requires a stream name and parameter value (see 'help objectStreamSetParameter')");
            return false;
        }
        AbstractStreamObjectIterator<?> objectStream = (AbstractStreamObjectIterator<?>)namedInstances.get(args[1]);
        if (objectStream != null) 
            try {
                // Set parameter
                objectStream.setConstructorParameterFromString((args.length > 3)?Integer.parseInt(args[3]):0, args[2], namedInstances);
                return true;
            } catch (IndexOutOfBoundsException e) {
                out.println(e.toString());
            } catch (IllegalArgumentException e) {
                out.println(e.toString());
            } catch (InstantiationException e) {
                out.println(e.toString());
            }
        else
            out.print("Stream '" + args[1] + "' is not opened");
        return false;

    }

    /**
     * Closes a named object stream.
     * An argument specifying the name of the stream to close is required.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; objectStreamClose my_data
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args name of an opened object stream
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "close a stream of LocalAbstractObjects", arguments = { "name of the stream" })
    public boolean objectStreamClose(PrintStream out, String... args) {
        return namedInstanceRemove(out, args);
    }

    /**
     * Resets a named object stream. It means that the objects are read from
     * the beginning of the stream's file again.
     * An argument specifying the name of the stream to reset is required.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; objectStreamReset my_data
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args name of an opened object stream
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "reset an AbstractObjectStream stream to read objects from the beginning", arguments = { "name of the stream" })
    public boolean objectStreamReset(PrintStream out, String... args) {
        if (args.length < 2) {
            out.println("objectStreamReset requires a stream name (see 'help objectStreamReset')");
            return false;
        }
        AbstractStreamObjectIterator<?> objectStream = (AbstractStreamObjectIterator<?>)namedInstances.get(args[1]);
        if (objectStream != null) 
            try {
                // Reset the returned stream
                objectStream.reset();
                return true;
            } catch (IOException e) {
                out.println(e.toString());
            }
        else out.print("Stream '" + args[1] + "' is not opened");
        return false;
    }


    //****************** Property file ******************//

    /**
     * Creates a new named properties.
     * The first required argument specifies the name from which to load the properties.
     * The second required argument specifies the name for the properties instance that can be used in other methods.
     * The third optional argument specifies a prefix of keys that the create properties will be restricted to (defaults to <tt>null</tt>).
     * The fourth optional argument specifies a hashtable of variables that will be replaced in the property values (defaults to <tt>null</tt>).
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; propertiesOpen someparameters.cf my_props begins_with_this host=localhost,port=1000
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args the property file, the new name, the restrict prefix (not required) and the variables (not required)
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "opens a new named property file", arguments = { "property file", "new name", "restrict prefix (not required)", "variables (not required)" })
    public boolean propertiesOpen(PrintStream out, String... args) {
        return propertiesOpen(out, args[1], args[2], (args.length > 3)?args[3]:null, (args.length > 4)?Convert.stringToMap(args[4]):null);
    }

    /**
     * Internal method for propertiesOpen.
     * @param out a stream where the application writes information for the user
     * @param fileName the property file
     * @param name the name for the instance
     * @param prefix the restrict prefix
     * @param variables the map of variables
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    private boolean propertiesOpen(PrintStream out, String fileName, String name, String prefix, Map<String, String> variables) {
        try {
            ExtendedProperties properties = new ExtendedProperties();
            properties.load(new FileInputStream(fileName));
            if (prefix != null || variables != null)
                properties = ExtendedProperties.restrictProperties(properties, prefix, variables);
            if (namedInstances.put(name, properties) != null)
                out.println("Previous named instance changed to a new one");
            return true;
        } catch (IOException e) {
            out.println("Cannot read properties: " + e);
            return false;
        }
    }


    //****************** Named instances ******************//

    /**
     * Creates a new named instance.
     * An argument specifying the signature of a constructor, a factory method or a static field
     * is required. Additional argument specifies the name for the instance (defaults to
     * name of the action where this is specified). If the instance already exists,
     * this method fails (use {@link #namedInstanceReplace namedInstanceReplace} instead.
     *
     * <p>
     * Example of usage for constructor, factory method and static field:
     * <pre>
     * MESSIF &gt;&gt;&gt; namedInstanceAdd messif.objects.impl.ObjectByteVectorL1(1,2,3,4,5,6,7,8,9,10) my_object
     * MESSIF &gt;&gt;&gt; namedInstanceAdd messif.utility.ExtendedProperties.getProperties(someparameters.cf) my_props
     * MESSIF &gt;&gt;&gt; namedInstanceAdd messif.buckets.index.LocalAbstractObjectOrder.locatorToLocalObjectComparator my_comparator
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args the instance constructor, factory method or static field signature and the name to register
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "creates a new named instance", arguments = { "instance constructor, factory method or static field signature", "name to register"})
    public boolean namedInstanceAdd(PrintStream out, String... args) {
        if (namedInstances.containsKey(args[2])) {
            out.println("Named instance '" + args[2] + "' already exists");
            return false;
        } else {
            return namedInstanceReplace(out, args);
        }
    }

    /**
     * Creates a new named instance or replaces an old one.
     * An argument specifying the signature of a constructor, a factory method or a static field
     * is required. Additional argument specifies the name for the instance (defaults to
     * name of the action where this is specified).
     * <p>
     * Example of usage for constructor, factory method and static field:
     * <pre>
     * MESSIF &gt;&gt;&gt; namedInstanceReplace messif.objects.impl.ObjectByteVectorL1(1,2,3,4,5,6,7,8,9,10) my_object
     * MESSIF &gt;&gt;&gt; namedInstanceReplace messif.utility.ExtendedProperties.getProperties(someparameters.cf) my_props
     * MESSIF &gt;&gt;&gt; namedInstanceReplace messif.buckets.index.LocalAbstractObjectOrder.locatorToLocalObjectComparator my_comparator
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args the instance constructor, factory method or static field signature and the name to register
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "creates a new named instance or replaces old one", arguments = { "instance constructor, factory method or static field signature", "name to register"})
    public boolean namedInstanceReplace(PrintStream out, String... args) {
        try {
            Object instance = InstantiatorSignature.createInstanceWithStringArgs(args[1], Object.class, namedInstances);
            namedInstances.put(args[2], instance);
            return true;
        } catch (NoSuchInstantiatorException e) {
            out.println("Error creating named instance for " + args[1] + ": " + e);
            return false;
        } catch (InvocationTargetException e) {
            out.println("Error creating named instance for " + args[1] + ": " + e.getCause());
            return false;
        }
    }

    /**
     * Prints the list of all named instances.
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; namedInstanceList
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args no arguments required
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "list all named instances", arguments = {})
    public boolean namedInstanceList(PrintStream out, String... args) {
        for(Map.Entry<String, Object> entry : namedInstances.entrySet())
            out.println(entry);
        return true;
    }

    /**
     * Removes a named instances.
     * An argument specifying the name of the instance to remove is required.
     *
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; namedInstanceRemove my_object
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args the name of the instance to remove
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "close a stream of LocalAbstractObjects", arguments = { "name of the stream" })
    public boolean namedInstanceRemove(PrintStream out, String... args) {
        Object instance = namedInstances.remove(args[1]);
        if (instance != null) {
            // Try to close the instance
            if (instance instanceof Closeable) {
                try {
                    ((Closeable)instance).close();
                } catch (IOException e) {
                    out.println("Error closing named instance: " + e.toString());
                    return false;
                }

            // Try to clear the instance
            } else if (instance instanceof Clearable) {
                ((Clearable)instance).clearSurplusData();
            }
        } else {
            out.print("There is no instance with name '" + args[1] + "'");
        }
        return true;
    }


    //****************** Logging command functions ******************//

    /**
     * Get or set the global level of logging.
     * If an argument is passed, the logging level is set.
     * Allowed argument values are names of {@link Level logging level} constants.
     * Otherwise the current logging level is printed out.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; loggingLevel warning
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args the new logging level
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "get/set global level of logging", arguments = { "[new logging level]" })
    public boolean loggingLevel(PrintStream out, String... args) {
        try {
            if (args.length < 2)
                out.println("Current global logging level: " + Logging.getLogLevel());
            else
                Logging.setLogLevel(Level.parse(args[1].toUpperCase()));
        } catch (IllegalArgumentException e) {
            out.println(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Set the logging level of console.
     * One argument specifying the logging level is required.
     * Allowed argument values are names of {@link Level logging level} constants.
     * Note that the messages with lower logging level than the current global
     * logging level will not be printed regardless of the console logging level setting.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; loggingConsoleChangeLevel info
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args the new logging level
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "set logging level for console", arguments = { "new logging level" })
    public boolean loggingConsoleChangeLevel(PrintStream out, String... args) {
        try {
            Logging.setConsoleLevel(Level.parse(args[1].toUpperCase()));
        } catch (IllegalArgumentException e) {
            out.println(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Adds a file for writing logging messages.
     * The first required argument specifies the name of the file to open.
     * The second argument (defaults to global logging level) specifies the
     * logging level of messages that will be stored in the file.
     * The third argument is a flag if the file is overwritten or appended to if it exists
     * and defaults to appending.
     * The fourth argument specifies whether to use XML or text (the default) format.
     * Allowed argument values are names of {@link Level logging level} constants.
     * The fifth optional argument specifies a regular expression that the message must satisfy
     * in order to be written in this file. 
     * The sixth argument specifies the message part that the regular expression
     * is applied to - see {@link Logging.RegexpFilterAgainst} for explanation of
     * the various values.
     * The seventh argument is the maximum number of bytes to write to a logging
     * file before it is rotated (zero means unlimited).
     * The ninth argument is the number of logging files for rotation. This only
     * applies, if the maximal file size is given in previous argument.
     * In that case, once the file reaches the size, it is renamed with a numbering
     * prefix and all other logging file numbers are increased by one (log rotation).
     * The files with a greater or equal number than this argument specifies are deleted.
     * 
     * Note that the messages with lower logging level than the current global
     * logging level will not be printed regardless of the file's logging level.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; loggingFileAdd myFile.log info true false messif.* CLASS_NAME
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args the file name,
     *              logging level,
     *              append to file flag,
     *              simple/xml format selector,
     *              regular expression,
     *              which part of the message is matched by regular expression,
     *              maximal log size, and
     *              number of rotated logs
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "add logging file to write logs", arguments = { "file name", "logging level", "append to file", "use simple format (true), XML (false) or given formatter (named instance)", "regexp to filter", "match regexp agains MESSAGE, LOGGER_NAME, CLASS_NAME or METHOD_NAME", "maximal log size", "number of rotated logs" })
    public boolean loggingFileAdd(PrintStream out, String... args) {
        try {
            Logging.addLogFile(
                    args[1],                                                                    // fileName
                    (args.length > 2)?Level.parse(args[2].toUpperCase()):Logging.getLogLevel(), // level
                    (args.length > 3)?Convert.stringToType(args[3], boolean.class):true,        // append
                    (args.length > 4)?args[4]:null,                                             // formatter
                    (args.length > 5)?args[5]:null,                                             // regexp
                    (args.length > 6 && args[6] != null && !args[6].isEmpty())?                 // regexp against
                        Logging.RegexpFilterAgainst.valueOf(args[6].toUpperCase()):Logging.RegexpFilterAgainst.MESSAGE,
                    (args.length > 7)?Convert.stringToType(args[7], int.class):0,               // maxSize
                    (args.length > 8)?Convert.stringToType(args[8], int.class):10,              // maxCount
                    namedInstances
            );
            return true;
        } catch (IOException e) {
            out.println("Can't open file '" + args[1] + "': " + e.toString());
            return false;
        } catch (InstantiationException e) {
            out.println("Invalid parameter for loggingFileAdd");
            return false;
        } catch (IllegalArgumentException e) {
            out.println(e.getMessage());
            return false;
        }
    }

    /**
     * Removes a logging file.
     * The file must be previously opened by {@link #loggingFileAdd}.
     * A required argument specifies the name of the opened logging file to close.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; loggingFileRemove myFile.log
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args the logging file name to remove
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "close log file", arguments = { "file name" })
    public boolean loggingFileRemove(PrintStream out, String... args) {
        try {
            Logging.removeLogFile(args[1]);
            return true;
        } catch (NullPointerException e) {
            out.println("Log file '" + args[1] + "' is not opened");
            return false;
        }
    }

    /**
     * Changes the logging level of an opened logging file.
     * The file must be previously opened by {@link #loggingFileAdd}.
     * The first required argument specifies the name of the opened logging file to close
     * and the second one the new logging level to set.
     * Note that the messages with lower logging level than the current global
     * logging level will not be printed regardless of the file's logging level.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; loggingFileChangeLevel myFile.log fine
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args the logging file name and the new logging level
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "change file log level", arguments = { "file name", "new logging level" })
    public boolean loggingFileChangeLevel(PrintStream out, String... args) {
        try {
            Logging.setLogFileLevel(args[1], Level.parse(args[2].toUpperCase()));
            return true;
        } catch (NullPointerException e) {
            out.println("Log file '" + args[1] + "' is not opened");
            return false;
        } catch (IllegalArgumentException e) {
            out.println(e.getMessage());
            return false;
        }
    }


    //****************** Miscellaneous command functions ******************//


    /**
     * Schedules {@link System#gc() full garbage collection}.
     * If an optional argument is passed, the application will sleep for the number
     * of miliseconds specified.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; collectGarbage 30000
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args number of milliseconds to sleep after the garbage collection
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "shedule full garbage collection", arguments = { "time to sleep (optional)" })
    @SuppressWarnings("CallToThreadYield")
    public boolean collectGarbage(PrintStream out, String... args) {
        System.gc();
        if (args.length >= 2) {
            try {
                Thread.sleep(Integer.parseInt(args[1]));
            } catch (InterruptedException e) {
                out.println("Sleep was interrupted: " + e.toString());
                return false;
            }
        } else {
            Thread.yield();
        }

        return true;
    }


    /**
     * Displays the memory usage of this virtual machine.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; memoryUsage
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args this method has no arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "display memory usage", arguments = { })
    public boolean memoryUsage(PrintStream out, String... args) {
        Runtime runtime = Runtime.getRuntime();
        out.print("Memory free/total: ");
        out.print(runtime.freeMemory());
        out.print("/");
        out.println(runtime.totalMemory());
        return true;
    }

    /**
     * Returns current time in miliseconds.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; currentTime
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args this method has no arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "returns current time in milis", arguments = { })
    public boolean currentTime(PrintStream out, String... args) {
        out.print(System.currentTimeMillis());
        return true;
    }
    
    /**
     * Shows a list of commands with help.
     *
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; help
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args this method has no arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "show help", arguments = { "command name (optional)" })
    public boolean help(PrintStream out, String... args) {
        if (args.length > 1) {
            if (args[1].equals("close")) {
                out.println("close");
                out.println("\tclose this connection (the algorithm keeps running)");
            } else {
                try {
                    methodExecutor.printUsage(out, true, true, new Object[] { out, new String[] { args[1] } });
                } catch (NoSuchMethodException e) {
                    out.println("There is no command " + args[1]);
                }
            }
        } else {
            out.println("Use 'help <command>' to get more details about any command");
            out.println("---------------- Available commands ----------------");
            out.println("close");
            methodExecutor.printUsage(out, false, false);
        }
        return true;
    }

    /**
     * Exits this application.
     * Note that there cannot be any algorithms running.
     * All command connections to the application will be closed.
     *
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; quit
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args this method has no arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "close the whole application (all connections will be closed)", arguments = { })
    public boolean quit(PrintStream out, String... args) {
        if (!algorithms.isEmpty()) {
            out.println("Cannot quit application interface if there are some algorithms running");
            return false;
        }
        Thread.currentThread().getThreadGroup().interrupt();
        return true;
    }

    /**
     * Prints the parameters to the output.
     * If the first argument contains "+", it is treated as "<em>inner</em>+<em>final</em>"
     * separators. In the separators, special texts "SPACE" and "NEWLINE" can be specified.
     * All additional parameters are then added to the output separated by the inner separators
     * and followed by the final separator.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; echo NEWLINE+NEWLINE x y z
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args output type separator and list of values to print
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "prints a specified message", arguments = { "output type separator (defaults to SPACE+NEWLINE)", "values..." })
    public boolean echo(PrintStream out, String... args) {
        // Set default separator
        String separator = "SPACE";
        String lastSeparator = "NEWLINE";
        int argIndex = 1;

        // Parse separators
        if (args.length > 2) {
            int pos = args[argIndex].indexOf('+');
            if (pos != -1) {
                separator = args[argIndex].substring(0, pos);
                lastSeparator = (pos >= args[argIndex].length() - 1)?"":args[argIndex].substring(pos + 1);
                argIndex++;
            }
        }

        // Replace "magic" words from separators
        if (separator.equals("NEWLINE"))
            separator = null;
        else
            separator = separator.replace("SPACE", " ");
        if (lastSeparator.equals("NEWLINE"))
            lastSeparator = null;
        else
            lastSeparator = separator.replace("SPACE", " ");

        // Print first value
        if (argIndex < args.length)
            out.print(args[argIndex++]);
        // Print additional values
        while (argIndex < args.length) {
            if (separator != null)
                out.print(separator);
            else
                out.println();
            out.print(args[argIndex++]);
        }
        if (lastSeparator != null)
            out.print(lastSeparator);
        else
            out.println();
        return true;
    }

    /**
     * Computes a sum of the parameters and prints the result to the output.
     * The first argument is the {@link DecimalFormat output format} used
     * for printing the value. All computations are done in doubles.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; sum #,##0.## 10 20 30
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args format of the result followed by the list of values to sum
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "computes a sum of values", arguments = { "format", "numeric values..." })
    public boolean sum(PrintStream out, String... args) {
        int i = 2;
        try {
            NumberFormat format = new DecimalFormat(args[1]);
            double sum = Double.parseDouble(args[i]);
            for (i++; i < args.length; i++)
                sum += Double.parseDouble(args[i]);
            out.println(format.format(sum));
            return true;
        } catch (IndexOutOfBoundsException e) {
            out.println("Number format and at least one value must be specified");
            return false;
        } catch (NumberFormatException e) {
            out.println("Cannot parse number #" + i + ": " + e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            out.println("Number format '" + args[1] + "' is invalid: " + e.getMessage());
            return false;
        }
    }

    /**
     * Decodes a value to some other value according to a regular expression.
     * The first argument is the value to be decoded.
     * Second parameter is a regular expression to match the value against.
     * If a match is found, the third parameter is added to output followed by a new line.
     * Otherwise, fourth parameter's regular expression is tried, and so on until a match
     * is found or parameters end.
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; decode mySpecVal ^your Y ^my M .* unknown
     * </pre>
     * </p>
     * 
     * <p>
     * Additionally, a map-based syntax can be used with only two parameters:
     * <pre>
     * MESSIF &gt;&gt;&gt; decode mySpecVal "^your"="Y","^my"=M,.*="unknown"
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args checked value followed by matched and result value pairs
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "decodes a value", arguments = { "checked value", "match1", "result1 ..." })
    public boolean decode(PrintStream out, String... args) {
        try {
            if (args.length == 3) {
                for (Map.Entry<String, String> entry : Convert.stringToMap(args[2]).entrySet()) {
                    if (args[1].matches(entry.getKey())) {
                        out.println(entry.getValue());
                        return true;
                    }
                }
            } else {
                for (int i = 3; i < args.length; i += 2) {
                    if (args[1].matches(args[i - 1])) {
                        out.println(args[i]);
                        return true;
                    }
                }
            }
        } catch (IndexOutOfBoundsException e) {
            out.println("Decode requires at least two parameters");
            return false;
        }

        return true;
    }


    /****************** Control file command functions ******************/

    /** Pattern that match variables in control files */
    private static final Pattern variablePattern = Pattern.compile("(?:<|\\$\\{)([^>}]+?)(?::-?([^>}]+))?(?:>|\\})", Pattern.MULTILINE);

    /**
     * Returns the value with substituted variables.
     * @param value the value in which to substitute the variables
     * @param variables the current defined variables
     * @return the value with substituted variables
     */
    protected String substituteVariables(String value, Map<String,String> variables) {
        return Convert.substituteVariables(value, variablePattern, 1, 2, variables);
    }

    /**
     * Returns a control file action arguments from properties with variable substitution.
     * @param props the properties with parameters
     * @param actionName the current action name
     * @param variables the current defined variables
     * @return the action arguments
     */
    protected List<String> getCFActionArguments(Properties props, String actionName, Map<String,String> variables) {
        List<String> arguments = new ArrayList<String>();

        // First argument is the method name (defaults to action name itself)
        String arg = props.getProperty(actionName, actionName);
        do {
            // Add var-substituted arg to arguments list
            arguments.add(Convert.trimAndUnquote(substituteVariables(arg, variables)));

            // Read next property with name <actionName>.param.{1,2,3,4,...}
            arg = props.getProperty(actionName + ".param." + Integer.toString(arguments.size()));
        } while (arg != null);

        return arguments;
    }

    /**
     * Postpone the current action according to the "postponeUntil" argument.
     * @param out the stream to write the output to
     * @param props the properties with actions and their parameters
     * @param actionName the name of the postponed action (used in errors)
     * @param variables the current variables environment
     * @return <tt>true</tt> if the postponing was successful
     */
    protected boolean postponeCFAction(PrintStream out, Properties props, String actionName, Map<String,String> variables) {
        String postponeUntil = substituteVariables(props.getProperty(actionName + ".postponeUntil"), variables);
        if (postponeUntil == null)
            return true;
        postponeUntil = postponeUntil.trim();
        if (postponeUntil.length() == 0)
            return true;
        try {
            long sleepTime = Convert.timeToMiliseconds(postponeUntil) - System.currentTimeMillis();
            if (sleepTime > 0)
                Thread.sleep(sleepTime);
        } catch (NumberFormatException e) {
            out.println(e.getMessage() + " for postponeUntil parameter for action '" + actionName + "'");
            return false;
        } catch (InterruptedException e) {
            out.println("Thread interrupted while waiting for posponed execution");
        }
        return true;
    }

    /**
     * Returns the output stream for the given action.
     * @param out the stream to write the output to
     * @param props the properties with actions
     * @param actionName the name of the action to execute
     * @param variables the current variables' environment
     * @param outputStreams currently opened output streams
     * @param assignOutput the virtual output to which the result is stored
     *          in order to put it into an "assign" variable
     * @return the action output stream or <tt>null</tt> if there was a problem opening a file
     */
    protected PrintStream getCFActionOutput(PrintStream out, Properties props, String actionName, Map<String,String> variables, Map<String, PrintStream> outputStreams, ByteArrayOutputStream assignOutput) {
        try {
            String fileName = substituteVariables(props.getProperty(actionName + ".outputFile"), variables);
            if (fileName != null && fileName.length() > 0) {
                if (assignOutput != null)
                    out.println("WARNING: Action '" + actionName + "' has both the 'outputFile' and the 'assign' parameters defined. Using outputFile.");
                PrintStream outputStream = outputStreams.get(fileName);
                if (outputStream == null) // Output stream not opened yet
                    outputStreams.put(fileName, outputStream = new PrintStream(fileName));
                return outputStream;
            } else {
                // No output file specified, use either the assign output (if specified) or the current output
                return assignOutput == null ? out : new PrintStream(assignOutput);
            }
        } catch (FileNotFoundException e) {
            out.println("Wrong outputFile for action '" + actionName + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns foreach values from properties with variable substitution.
     * @param out the stream to write the output to
     * @param props the properties with actions
     * @param actionName the name of the action to execute
     * @param variables the current variables' environment
     * @return the action's foreach values or <tt>null</tt> if they are not specified
     */
    protected String[] getCFActionForeach(PrintStream out, Properties props, String actionName, Map<String,String> variables) {
        String foreach = props.getProperty(actionName + ".foreach");
        return foreach != null ? substituteVariables(foreach, variables).trim().split("[ \t]+") : null;
    }

    /**
     * Returns the repeat-until exception class from properties with variable substitution.
     * Note that only subclasses of {@link Exception} are allowed and a
     * {@link Throwable} class is returned if the given class is not exception.
     *
     * @param out the stream to write the output to
     * @param props the properties with actions
     * @param actionName the name of the action to execute
     * @param variables the current variables' environment
     * @return the action's repeat-until exception class or <tt>null</tt> if they are not specified
     */
    protected Class<? extends Throwable> getCFActionException(PrintStream out, Properties props, String actionName, Map<String,String> variables) {
        String repeatUntilException = props.getProperty(actionName + ".repeatUntilException");
        if (repeatUntilException == null)
            return null;
        try {
            return Convert.getClassForName(substituteVariables(repeatUntilException, variables).trim(), Exception.class);
        } catch (ClassNotFoundException e) {
            out.println("Wrong repeatUntilException for action '" + actionName + "': " + e.getMessage());
            return Throwable.class;
        }
    }

    /**
     * Returns the number of repeats with variable substitution.
     * Note that number of repeats is the number of foreach values (if not <tt>null</tt>).
     * If the repeatUntilException is not <tt>null</tt> and there is not a "repeat" or
     * a "foreach" action parameter, {@link Integer#MAX_VALUE} is returned and
     * thus the action will be repeated until there is an exception.
     *
     * @param out the stream to write the output to
     * @param props the properties with actions
     * @param actionName the name of the action to execute
     * @param variables the current variables' environment
     * @param foreachValues the foreach values for this action
     * @param repeatUntilExceptionClass the class of the repeat-until exception
     * @return the action's number of repeats, -1 is returned on error
     */
    protected int getCFActionRepeat(PrintStream out, Properties props, String actionName, Map<String,String> variables, String[] foreachValues, Class<? extends Throwable> repeatUntilExceptionClass) {
        String repeatValue = props.getProperty(actionName + ".repeat");
        if (foreachValues != null) {
            if (repeatValue != null)
                out.println("WARNING: Action '" + actionName + "' has both the 'repeat' and the 'foreach' parameters defined. Using foreach.");
            return foreachValues.length;
        } else {
            try {
                return repeatValue == null ? 
                    (repeatUntilExceptionClass == null ? 1 : Integer.MAX_VALUE) : // No repeats specified, but repeat until exception
                    Integer.valueOf(substituteVariables(repeatValue, variables));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
    }

    /**
     * This method reads and executes one action (with name actionName) from the control file (props).
     * For a full explanation of the command syntax see {@link CoreApplication}.
     * 
     * @param out the stream to write the output to
     * @param props the properties with actions
     * @param actionName the name of the action to execute
     * @param variables the current variables environment
     * @param outputStreams currently opened output streams
     * @param throwException flag whether to throw the {@link InvocationTargetException} when an action encounters error instead of handling it
     * @return <tt>true</tt> if the action was executed successfully
     * @throws InvocationTargetException if there was an error executing the action while the {@code throwException} is <tt>true</tt>
     */
    protected boolean controlFileExecuteAction(PrintStream out, Properties props, String actionName, Map<String,String> variables, Map<String, PrintStream> outputStreams, boolean throwException) throws InvocationTargetException {
        // Parse action name and arguments
        List<String> arguments = getCFActionArguments(props, actionName, variables);
        String methodName = arguments.get(0);

        // SPECIAL! For objectStreamOpen/namedInstanceAdd method a third/second parameter is automatically added from action name
        if ((methodName.equals("objectStreamOpen") && arguments.size() == 3) || (methodName.equals("namedInstanceAdd") && arguments.size() == 2))
            arguments.add(actionName);

        // Read description (variable substition in description is done during the repeat)
        String description = props.getProperty(actionName + ".description");

        // Read the assign parameter and set the output stream
        String assignVariable = substituteVariables(props.getProperty(actionName + ".assign"), variables);
        ByteArrayOutputStream assignOutput = assignVariable != null ? new ByteArrayOutputStream() : null;
        PrintStream outputStream = getCFActionOutput(out, props, actionName, variables, outputStreams, assignOutput);
        if (outputStream == null)
            return false;

        // Read number of repeats of this method
        String[] foreachValues = getCFActionForeach(out, props, actionName, variables);
        Class<? extends Throwable> repeatUntilExceptionClass = getCFActionException(out, props, actionName, variables);
        if (repeatUntilExceptionClass == Throwable.class)
            return false;
        int repeat = getCFActionRepeat(out, props, actionName, variables, foreachValues, repeatUntilExceptionClass);
        if (repeat < 0) {
            out.println("Number of repeats specified in action '" + actionName + "' is not a valid non-negative integer");
            return false;
        }

        // Postpone action
        if (!postponeCFAction(out, props, actionName, variables))
            return false;

        // Execute action
        try {
            for (int i = 0; i < repeat; i++) {
                // Add foreach/repeat value
                if (foreachValues != null)
                    variables.put(actionName, foreachValues[i]);
                else if (repeat > 1)
                    variables.put(actionName, Integer.toString(i + 1));

                // Show description if set
                if (description != null)
                    outputStream.println(substituteVariables(description, variables));

                // Perform action
                if (methodName.indexOf(' ') != -1) {
                    // Special "block" method 
                    for (String blockActionName : methodName.split("[ \t]+"))
                        if (!controlFileExecuteAction(outputStream, props, blockActionName, variables, outputStreams, throwException || repeatUntilExceptionClass != null))
                            return false; // Stop execution of block if there was an error
                } else try {
                    Object rtv;
                    // SPECIAL! Method propertiesOpen is called with additional arguments
                    if (methodName.equals("propertiesOpen"))
                        rtv = propertiesOpen(out,
                                arguments.get(1), // fileName
                                (arguments.size() > 2)?arguments.get(2):actionName, // name
                                (arguments.size() > 3)?arguments.get(3):null, // prefix
                                (arguments.size() > 4)?Convert.stringToMap(arguments.get(4)):variables
                        );
                    else // Normal method
                        rtv = methodExecutor.execute(outputStream, arguments.toArray(new String[arguments.size()]));
                    outputStream.flush();
                    if (rtv instanceof Boolean && !((Boolean)rtv).booleanValue()) {
                        if (assignOutput != null)
                            out.print(assignOutput.toString());
                        out.println("Action '" + actionName + "' failed - control file execution was terminated");
                        return false;
                    }
                } catch (NoSuchMethodException e) {
                    if (methodName.equals(actionName)) {
                        out.println("Action '" + actionName + "' not found in the control file");
                        return false; // Execution unsuccessful, method/action not found
                    } else
                        // There was no method for the action, so we try is as a name of block
                        if (!controlFileExecuteAction(outputStream, props, methodName, variables, outputStreams, throwException || repeatUntilExceptionClass != null))
                            return false;
                }
            }
        } catch (InvocationTargetException e) {
            // Check whether the repeat-until is not captured
            if (repeatUntilExceptionClass == null || !repeatUntilExceptionClass.isInstance(getRootCauseInvocationTargetException(e))) {
                if (throwException) // Exception is being handled by a higher call
                    throw e;
                processException(e.getCause(), out, true);
                out.println("Action '" + actionName + "' failed - control file execution was terminated");
                return false;
            }
        }

        // Remove foreach/repeat value
        if (foreachValues != null || repeat > 1)
            variables.remove(actionName);

        // Assign variable is requested
        if (assignVariable != null && assignOutput != null)
            variables.put(assignVariable, assignOutput.toString().trim());

        return true; // Execution successful
    }

    /**
     * Executes actions from a control file.
     * All commands that can be started from the prompt can be used in control files.
     * The first argument is required to specify the file with commands.
     * Additional arguments are either variable specifications in the form of "varname=value"
     * or the action name that is started (which defaults to "actions").
     * For a full explanation of the command sytax see {@link CoreApplication}.
     * 
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; controlFile commands.cf var1=100 var2=data.file my_special_action
     * </pre>
     * </p>
     * 
     * @param out a stream where the application writes information for the user
     * @param args file name followed by variable specifications and start action
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "execute actions from control file", arguments = { "control file path", "<var>=<value> ... (optional)", "actions block name (optional)" })
    public boolean controlFile(PrintStream out, String... args) {
        // Open control file and create properties
        Properties props = new Properties();
        try {
            InputStream stream;
            if (args[1].equals("-"))
                stream = System.in;
            else stream = new FileInputStream(args[1]);
            props.load(stream);
            stream.close();
        } catch (FileNotFoundException e) {
            out.println(e.toString());
            return false;
        } catch (IOException e) {
            out.println(e.toString());
            return false;
        }

        // Prepare variable for output streams
        Map<String, PrintStream> outputStreams = new HashMap<String, PrintStream>();
        
        // Read variables and action from arguments
        String action = "actions";
        Map<String, String> variables = new HashMap<String,String>();
        for (int i = 2; i < args.length; i++) {
            String[] varVal = args[i].split("=", 2);
            if (varVal.length == 2)
                variables.put(varVal[0], varVal[1]);
            else action = args[i];
        }

        // Execute the first action
        boolean rtv;
        try {
            rtv = controlFileExecuteAction(out, props, action, variables, outputStreams, false);
        } catch (InvocationTargetException e) {
            throw new InternalError("Action execution cannot throw exception when throwException is false: " + e.getCause());
        }
        
        // Close all opened output streams
        for (PrintStream stream : outputStreams.values())
            stream.close();
        outputStreams.clear();
        
        return rtv;
    }


    //****************** Socket interface processor ******************//

    /**
     * Removes backspace characters (by deleting the previous char as well)
     * from the given string.
     * @param input the string from which to remove backspace characters
     * @return the string with removed backspace characters
     */
    private String removeBackspaces(String input) {
        int pos = input.indexOf('\b');
        if (pos == -1)
            return input;
        StringBuilder str = new StringBuilder(input);
        while (pos != -1) {
            str.delete(pos > 0 ? pos - 1 : 0, pos + 1);
            pos = str.indexOf("\b");
        }

        return str.toString();
    }

    /**
     * Process an incoming command-prompt connection.
     * @param connection the connection to process
     * @throws IOException if there was a communication error
     */
    protected void processInteractiveSocket(SocketChannel connection) throws IOException {
        // Get next connection stream from socket
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.socket().getInputStream()));
        PrintStream out = new PrintStream(connection.socket().getOutputStream());

        try {
            // Show prompt
            out.print("MESSIF >>> ");
            out.flush();

            // Read lines from the socket
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                // Execute method with the specified name and the provide the array of arguments
                try {
                    String[] arguments = Convert.splitBySpaceWithQuotes(removeBackspaces(line.trim()));

                    // Handle close command
                    if ((arguments.length > 0 && arguments[0].equalsIgnoreCase("close")) || Thread.currentThread().isInterrupted())
                        break;

                    // Handle normal method
                    try {
                        // Get appropriate method
                        methodExecutor.execute(out, arguments);
                    } catch (InvocationTargetException e) {
                        processException(e.getCause(), out, false);
                    } catch (NoSuchMethodException e) {
                        out.println("Unknown command: " + line);
                        out.println("Use 'help' to see all available commands");
                    }
                } catch (IllegalArgumentException e) {
                    out.println(e.getMessage());
                }

                // Show prompt
                out.print("MESSIF >>> ");
                out.flush();
            }
        } finally {
            try {
                out.close();
                in.close();
            } catch (Exception ignore) {
            }
        }
    }


    //****************** Standalone application's main method ******************//

    /**
     * Internal method called from {@link #main(java.lang.String[]) main} method
     * to initialize this application. Basically, this method calls {@link #parseArguments(java.lang.String[], int)}
     * and prints a usage if <tt>false</tt> is returned.
     * @param args the command line arguments
     */
    protected void startApplication(String[] args) {
        if (!parseArguments(args, 0)) {
            System.err.println("Usage: " + getClass().getName() + " " + usage());
        } else if (cmdSocket != null) {
            cmdSocketLoop();
        }
    }

    /**
     * Returns the command line arguments description.
     * @return the command line arguments description
     */
    protected String usage() {
        return "[<cmdport>] [<controlFile> [<action>] [<var>=<value> ...]]";
    }

    /**
     * Internal method called from {@link #main(java.lang.String[]) main} method
     * to read parameters and initialize the application.
     * @param args the command line arguments
     * @param argIndex the index of the argument where to start
     * @return <tt>true</tt> if the arguments were valid
     */
    protected boolean parseArguments(String[] args, int argIndex) {
        if (argIndex >= args.length)
            return false;

        // Prepare port for telnet interface (first argument is an integer)
        try {
            cmdSocket = openCmdSocket(Integer.parseInt(args[argIndex]));
            argIndex++;
        } catch (NumberFormatException ignore) { // First argument is not a number (do not start telnet interface)
            cmdSocket = null;
        }

        // Put the rest of arguments to "controlFile" method
        if (args.length > argIndex) {
            String[] newArgs = new String[args.length - argIndex + 1];
            System.arraycopy(args, argIndex, newArgs, 1, args.length - argIndex);
            newArgs[0] = "controlFile";
            controlFile(System.out, newArgs);
        }

        return true;
    }

    /**
     * Open the port for telnet interface.
     * @param port the TCP port to use
     * @return the opened socket or <tt>null</tt> if there was an error opening the port
     */
    private ServerSocketChannel openCmdSocket(int port) {
        try {
            ServerSocketChannel ret = ServerSocketChannel.open();
            ret.socket().bind(new InetSocketAddress(port));
            return ret;
        } catch (IOException e) {
            System.err.println("Can't open telnet interface: " + e.toString());
            log.log(Level.WARNING, "Can't open telnet interface: {0}", e);
            return null;
        }
    }

    /**
     * Telnet interface loop.
     * It waits for the next connection on {@link #cmdSocket} and then starts a new thread that
     * executes the commands given at the prompt.
     */
    private void cmdSocketLoop() {
        try {
            cmdSocket.configureBlocking(true);
            for (;;) {
                // Get a connection (blocking mode)
                final SocketChannel connection = cmdSocket.accept();
                new Thread("thApplicationCmdSocket") {
                    @Override
                    public void run() {
                        try {
                            processInteractiveSocket(connection);
                        } catch (ClosedByInterruptException e) {
                            // Ignore this exception because it is a correct exit
                        } catch (IOException e) {
                            log.warning(e.toString());
                        } finally {
                            try { connection.close(); } catch (Exception ignore) {}
                        }
                    }
                }.start();
            }
        } catch (ClosedByInterruptException e) {
            // Ignore this exception because it is a correct exit
        } catch (IOException e) {
            log.warning(e.toString());
        }
    }

    /**
     * Start a MESSIF application.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // Create new instance of application
        new CoreApplication().startApplication(args);
    }

}
