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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
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
import messif.objects.MetaObject;
import messif.objects.util.AbstractStreamObjectIterator;
import messif.objects.util.RankedAbstractMetaObject;
import messif.objects.util.RankedAbstractObject;
import messif.objects.util.RankedSortedCollection;
import messif.objects.util.StreamGenericAbstractObjectIterator;
import messif.operations.AbstractOperation;
import messif.operations.QueryOperation;
import messif.operations.RankingQueryOperation;
import messif.operations.RankingSingleQueryOperation;
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
 *  &lt;actionName&gt;.loopVariable = &lt;variable name&gt;
 *  &lt;actionName&gt;.ignoreException = &lt;some exception class, e.g. messif.buckets.DuplicateObjectException&gt;
 *  &lt;actionName&gt;.outputFile = &lt;filename&gt;
 *  &lt;actionName&gt;.assign = &lt;variable name&gt;
 *  &lt;actionName&gt;.postponeUntil = hh:mm:ss
 *  &lt;actionName&gt;.description = &lt;any text with variable expansion&gt;
 *  &lt;actionName&gt;.descriptionAfter = &lt;any text with variable expansion&gt;</pre>
 * <ul>
 * <li>&lt;actionName&gt; is a user specified name for the action which can be referred from other
 *                    actions (&lt;otherActionName1&gt; &lt;otherActionName2&gt;) or command line parameter <i>[action]</i>.</li>
 * <li>&lt;methodName&gt; can be any {@link CoreApplication} method, which is to be executed if &lt;actionName&gt; is called.
 *                    If a space-separated list of other action names is provided, they will be executed one by one
 *                    in the order they were specified. Parameters for the method are specified using &lt;actionName&gt;.param.<i>x</i>,
 *                    see the documentation of the respective {@link CoreApplication} methods for their parameters.</li>
 * <li><i>repeat</i> parameter is optional and allows to specify multiple execution of
 *                 the same action &lt;repeats&gt; times. It can be used together with "block" method name to implement
 *                 a loop of commands with specified number of repeats. In each iteration the variable &lt;loopVariable&gt;
 *                 is assigned the number of the actual iteration (starting from 1) and <i>loopVariable</i>_iteration is assigned
 *                 the zero-based number of the iteration.</li>
 * <li><i>foreach</i> parameter is also optional and similarly to <i>repeat</i> it allows the action to be
 *                executed multiple times - the number of repeats is equal to the number of values provided.
 *                Moreover, in each iteration the variable <i>loopVariable</i> is assigned &lt;value&gt; taken
 *                one by one from the <i>foreach</i> parameter and <i>loopVariable</i>_iteration is assigned
 *                the zero-based number of the iteration.</li>
 * <li><i>repeatUntilException</i> parameter is optional and allows to stop repeating the action
 *                when the exception given as the value of this parameter occurs. Note
 *                that if either "repeat" or "foreach" parameter is also specified,
 *                the repeating ends after their number of repeats or an exception
 *                whichever comes first. If no "repeat" or "foreach" is specified
 *                the action is repeated until an exception occurs.
 * <li><i>loopVariable</i> the name of the variable to set in <i>foreach</i>, <i>repeat</i>,
 *                or <i>repeatUntilException</i> modifier. If not specified, the &lt;actionName&gt; is used.</li>
 * <li><i>repeatEvery</i> parameter is optional and allows to execute the action repeatedly
 *                at time intervals specified by the argument. Note that the action will
 *                be executed normally (as a normal action) at first and then it will
 *                be run asynchronously (in another thread!) at the given time intervals.
 * <li><i>ignoreException</i> parameter is optional and allows to ignore an exception thrown
 *                by the action (or any of its descendant actions) and continue processing normally
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
 * <li><i>description</i> parameter is optional and allows print the specified text before
 *  the respective action is executed.</li>
 * <li><i>descriptionAfter</i> parameter is optional and allows print the specified text after
 *  the respective action is executed.</li>
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
    private Algorithm algorithm = null;

    /** List of running algorithms */
    private final List<Algorithm> algorithms = new ArrayList<Algorithm>();

    /** Last executed operation */
    private AbstractOperation lastOperation = null;

    /** Regular expression for binding {@link messif.statistics.OperationStatistics} in every {@link #operationExecute} call */
    private String bindOperationStatsRegexp = null;

    /** Internal list of methods that can be executed */
    private final MethodExecutor methodExecutor;

    /** List of currently created named instances */
    private final Map<String, Object> namedInstances = new HashMap<String, Object>();

    /** List of asynchronous threads for "repeatEvery" actions */
    private final Map<String, Thread> repeatEveryThreads = new HashMap<String, Thread>();

    /** Socket used for command communication */
    private ServerSocketChannel cmdSocket;

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

    /**
     * Returns the given argument converted to integer.
     * @param args the list of arguments
     * @param index the index of the argument to convert
     * @param defaultValue the default value if the argument index is out of range, <tt>null</tt> or empty string
     * @param minValue the minimal value for the returned value
     * @param maxValue the maximal value for the returned value
     * @return the given argument converted to integer
     * @throws NumberFormatException if the value cannot be converted or is out of range
     */
    protected int retrieveIntArgument(String[] args, int index, int defaultValue, int minValue, int maxValue) throws NumberFormatException {
        if (index >= args.length || args[index] == null || args[index].isEmpty())
            return defaultValue;
        int ret = Integer.parseInt(args[index]);
        if (ret < minValue)
            throw new NumberFormatException("Number '" + ret + "' is not greater than or equal to " + minValue);
        if (ret > maxValue)
            throw new NumberFormatException("Number '" + ret + "' is not less than or equal to " + maxValue);
        return ret;
    }

    /**
     * Returns the socket used for command communication.
     * @return the socket used for command communication
     */
    protected final ServerSocketChannel getCmdSocket() {
        return cmdSocket;
    }

    /**
     * Returns the currently selected running algorithm.
     * @return the currently selected running algorithm
     */
    Algorithm getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns the running algorithm with the specified sequence number.
     * @param index the zero-based index of the running algorithm
     * @return the running algorithm with the specified sequence number
     * @throws IndexOutOfBoundsException if the specified index is not valid
     */
    Algorithm getAlgorithm(int index) throws IndexOutOfBoundsException {
        return algorithms.get(index);
    }

    /**
     * Returns whether there is a running algorithm.
     * @return <tt>true</tt> if there is a running algorithm
     */
    boolean hasAlgorithm() {
        return getAlgorithm() != null;
    }

    /**
     * Returns the number of running algorithms.
     * @return the number of running algorithms
     */
    int getAlgorithmCount() {
        return algorithms.size();
    }

    /**
     * Set the currently selected running algorithm.
     * @param algorithm the currently selected running algorithm
     * @return <tt>true</tt> if the current algorithm was set to a new value or <tt>false</tt> if the current algorithm is <tt>null</tt>
     */
    boolean setAlgorithm(Algorithm algorithm) {
        this.algorithm = algorithm;
        return algorithm != null;
    }

    /**
     * Adds the given algorithm to the list of running algorithms and select it as the current running algorithm.
     * @param algorithm the algorithm to add
     * @return <tt>true</tt> if the current algorithm was set to a new value or <tt>false</tt> if the current algorithm is <tt>null</tt>
     */
    boolean addAlgorithm(Algorithm algorithm) {
        this.algorithms.add(algorithm);
        return setAlgorithm(algorithm);
    }

    /**
     * Removes the given algorithm from the list of running algorithms and sets the last currently running algorithm as current.
     * @param algorithm the algorithm to remove
     * @return <tt>true</tt> if the algorithm was successfully removed
     * @throws Throwable if there was an error finializing the algorithm
     */
    boolean removeAlgorithm(Algorithm algorithm) throws Throwable {
        if (algorithm == null)
            return false;
        boolean ret = algorithms.remove(algorithm);
        // (the equality by instance IS correct)
        if (this.algorithm == algorithm) {
            // If the currently selected algorithm is removed, set it to the last added algorithm (or null)
            if (algorithms.isEmpty())
                this.algorithm = null;
            else
                this.algorithm = algorithms.get(algorithms.size() - 1);
        }
        algorithm.finalize();
        return ret;
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
            return addAlgorithm(ConstructorInstantiator.createInstanceWithStringArgs(constructors, args, 2, args.length - 1, namedInstances));
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
            return addAlgorithm(Algorithm.restoreFromFile(args[1]));
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
            Algorithm alg = getAlgorithm();
            if (alg != null) {
                // Store algorithm to file
                alg.storeToFile(args[1]);
                return true;
            } else out.println("No running algorithm is selected");
        } catch (IOException e) {
            out.println(e.toString());
        }
        return false;
    }

    /**
     * Stops current algorithm and clear the memory used. If other algorithm(s) is running, the last executed is
     *  selected as the current algorithm.
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; algorithmStop
     * </pre>
     *
     * @param out a stream where the application writes information for the user
     * @param args this method has no arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "stop current algorithm and select the previous as current", arguments = {})
    public boolean algorithmStop(PrintStream out, String... args) {
        try {
            return removeAlgorithm(getAlgorithm());
        } catch (Throwable e) {
            out.println(e.toString());
            return false;
        }
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
        for (Algorithm alg : algorithms) {
            try {
                removeAlgorithm(alg);
            } catch (Throwable e) {
                out.println(e.toString());
            }
        }
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
        Algorithm alg = getAlgorithm();
        if (alg != null) {
            out.println(alg.toString());
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
            return setAlgorithm(algorithms.get(Integer.parseInt(args[1])));
        } catch (IndexOutOfBoundsException ignore) {
        } catch (NumberFormatException ignore) {
        }

        out.print("Algorithm # must be specified - use a number between 0 and ");
        out.println(algorithms.size() - 1);

        return false;
    }

    /**
     * Assign one or more running algorithms to a given named instance.
     * The first parameter specifies the name to which the selected algorithms are assigned.
     * If the second parameter is not specified, the current algorithm is used.
     * Otherwise, the second parameter can be:
     * <ul>
     *   <li>a number, which select the algorithm (similarly to the {@link #algorithmSelect});</li>
     *   <li>a number range (dash-connected two numbers), which select all the algorithms with number from the given range
     *          - note that a static array with the selected algorithms will be assigned to the named instance;</li>
     *   <li>coma-separated list of numbers or number ranges;</li>
     *   <li>word "all" to get all currently running algorithms as a static array.</li>
     * </ul>
     *
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; algorithmToNamedInstace runningAlgs 0-2,6,9
     * </pre>
     *
     * @param out a stream where the application writes information for the user
     * @param args the name of the variable and, optionally, the algorithm sequence number selector
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "select algorithm to manage", arguments = {"name of the variable", "algorithm sequence number selector (optional)"})
    public boolean algorithmToNamedInstance(PrintStream out, String... args) {
        Object value;
        if (args.length <= 2) {
            value = getAlgorithm();
        } else if (args[2].equalsIgnoreCase("all")) {
            value = algorithms.toArray(new Algorithm[algorithms.size()]);
        } else {
            try {
                value = algorithms.get(Integer.parseInt(args[2]));
            } catch (NumberFormatException ignore) {
                Collection<Integer> algIndexes = Convert.rangeSelectorsToIndexes(args[2], false);
                Algorithm[] algs = new Algorithm[algIndexes.size()];
                int i = 0;
                for (Iterator<Integer> it = algIndexes.iterator(); it.hasNext(); i++)
                    algs[i] = algorithms.get(it.next());
                value = algs;
            }
        }

        namedInstances.put(args[1], value);

        return true;
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
        Algorithm alg = getAlgorithm();
        if (alg == null) {
            out.println("No running algorithm is selected");
            return false;
        }

        out.println("---------------- Available operations ----------------");
        for (Class<? extends AbstractOperation> opClass : alg.getSupportedOperations())
            try {
                out.println(AbstractOperation.getConstructorDescription(opClass));
            } catch (NoSuchMethodException ex) {
                out.println(opClass.getName() + " can be processed but not instantiated");
            }

        return true;
    }


    //****************** Operation command functions ******************//

    /**
     * Returns the last executed operation.
     * @return the last executed operation
     */
    AbstractOperation getLastOperation() {
        return lastOperation;
    }

    /**
     * Set the last executed operation.
     * @param lastOperation the executed operation to set
     * @return <tt>true</tt> if the operation was set to a new value or <tt>false</tt> if the operation is <tt>null</tt>
     */
    boolean setLastOperation(AbstractOperation lastOperation) {
        this.lastOperation = lastOperation;
        return lastOperation != null;
    }

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
        return setLastOperation(createOperation(out, args));
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
        Algorithm alg = getAlgorithm();
        if (alg == null) {
            out.println("No running algorithm is selected");
            return false;
        }

        AbstractOperation operation = createOperation(out, args);
        if (operation == null)
            return false;

        try {
            // Execute operation
            return setLastOperation(alg.setupStatsAndExecuteOperation(operation, bindOperationStatsRegexp));
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
        Algorithm alg = getAlgorithm();
        if (alg == null) {
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
            alg.backgroundExecuteOperation(operation);
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
        Algorithm alg = getAlgorithm();
        if (alg == null) {
            out.println("No running algorithm is selected");
            return false;
        }

        try {
            List<AbstractOperation> waitBackgroundExecuteOperation = alg.waitBackgroundExecuteOperation();
            if (! waitBackgroundExecuteOperation.isEmpty()) {
                setLastOperation(waitBackgroundExecuteOperation.get(0));
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
            Algorithm alg = getAlgorithm();
            AbstractOperation op = getLastOperation();
            if (alg != null && op != null) {
                // Reset operation answer if requested
                if (args.length >= 2 && args[1].equalsIgnoreCase("true") && op instanceof QueryOperation)
                    ((QueryOperation)op).resetAnswer();

                // Execute operation
                return setLastOperation(alg.setupStatsAndExecuteOperation(op, bindOperationStatsRegexp));
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
        out.println(getLastOperation());
        return true;
    }

    /**
     * Show the {@link ErrorCode} returned by the last executed operation.
     *
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; operationErrorCode
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args this method has no arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "show error code returned by the last executed operation", arguments = {})
    public boolean operationErrorCode(PrintStream out, String... args) {
        AbstractOperation op = getLastOperation();
        out.println(op == null ? ErrorCode.NOT_SET : op.getErrorCode());
        return true;
    }

    /**
     * Show the number of objects returned by the last executed operation.
     *
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; operationObjectCount
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args this method has no arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "show number of objects returned by the last executed operation", arguments = {})
    public boolean operationObjectCount(PrintStream out, String... args) {
        AbstractOperation op = getLastOperation();
        if (op instanceof QueryOperation) {
            out.println(((QueryOperation<?>)op).getAnswerCount());
        } else {
            out.println("Object count is available only for query operations");
        }
        return true;
    }

    /**
     * Show the {@link AbstractObject#getLocatorURI() locatorURI} of the query object of the last executed operation.
     * Note that a {@link RankingSingleQueryOperation} must have been executed prior to calling this method.
     * Optionally, the argument specifies text written after the locator (defaults to new line).
     *
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; operationQueryObjectLocator :
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args optional suffix to write
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "show the locator of the last executed query operation", arguments = {})
    public boolean operationQueryObjectLocator(PrintStream out, String... args) {
        AbstractOperation op = getLastOperation();
        if (!(op instanceof RankingSingleQueryOperation)) {
            out.println("A single-query ranking operation needed, but have " + op == null ? null : op.getClass().getName());
            return false;
        }
        LocalAbstractObject queryObject = ((RankingSingleQueryOperation)op).getQueryObject();
        String locator = queryObject == null ? null : queryObject.getLocatorURI();
        if (args.length > 1) {
            out.print(locator);
            out.print(args[1]);
        } else {
            out.println(locator);
        }
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
        AbstractOperation op = getLastOperation();
        if (op == null) {
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
        if (argIndex < 0 || argIndex >= op.getArgumentCount()) {
            out.println("operationArgument index '" + args[1] + "' is not within <0;" + op.getArgumentCount() + ") bounds");
            return false;
        }

        // Display it
        out.println(op.getArgument(argIndex));
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
        AbstractOperation op = getLastOperation();
        if (op == null) {
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
                op.setParameter(args[1], Convert.stringToType(args[2], parameterClass, namedInstances));
            } catch (InstantiationException e) {
                out.println("Cannot convert '" + args[2] + "' to " + parameterClass);
                return false;
            }
        } else { // Show parameter
            out.println(op.getParameter(args[1]));
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
        AbstractOperation op = getLastOperation();
        if (op == null) {
            out.println("No operation has been executed yet");
            return false;
        }
        if (!(op instanceof RankingQueryOperation)) {
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
            ((RankingQueryOperation)op).setAnswerCollection(newAnswerCollection);

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
        AbstractOperation op = getLastOperation();
        if (op == null) {
            out.println("No operation has been executed yet");
            return false;
        }
        if (!(op instanceof RankingQueryOperation)) {
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
        ((RankingQueryOperation)op).setAnswerCollection((RankedSortedCollection)newAnswerCollection);
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
        AbstractOperation op = getLastOperation();
        if (op == null) {
            out.println("No operation has been executed yet");
            return false;
        }

        try {
            // Prepare method
            MethodInstantiator<AbstractOperation> method = new MethodInstantiator<AbstractOperation>(AbstractOperation.class, Class.forName(args[1]), args[2], args.length - 2);

            // Prepare arguments
            String[] stringArgs = args.clone();
            stringArgs[2] = null;
            // Note that the output is added as "out" named instance temporarily
            Object[] methodArgs = Convert.parseTypesFromString(stringArgs, method.getInstantiatorPrototype(), true, 2, getExtendedNamedInstances("out", out, false));
            methodArgs[0] = op;

            // Execute method
            return setLastOperation(method.instantiate(methodArgs));
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
     * Returns static array with five elements that contains:
     * the given answer object as-is,
     * the {@link AbstractObject} from the answer object,
     * the {@link AbstractObject#getLocatorURI() locator} of the {@link AbstractObject},
     * the distance,
     * the meta-object sub-distance for the given {@code subAnswerInder}
     *
     * @param answerObject the answer object from a query operation (e.g. {@link AbstractObject}, {@link RankedAbstractObject}, or {@link RankedAbstractMetaObject})
     * @param subAnswerIndex the index of the sub-distance to get from the {@link RankedAbstractMetaObject}
     * @return a static array with the five formatting elements of the answer object
     */
    private Object[] parseAnswerObject(Object answerObject, Integer subAnswerIndex) {
        Object[] params = new Object[5]; // Format consists of answerObject itself, abstract object, locator, distance, and meta subdistance
        params[0] = answerObject;
        if (answerObject instanceof RankedAbstractObject) {
            RankedAbstractObject castObject = (RankedAbstractObject)answerObject;
            params[1] = castObject.getObject();
            params[2] = castObject.getObject().getLocatorURI();
            params[3] = castObject.getDistance();
            if (subAnswerIndex != null && answerObject instanceof RankedAbstractMetaObject) {
                params[4] = ((RankedAbstractMetaObject)answerObject).getSubDistance(subAnswerIndex);
            }
        } else if (answerObject instanceof AbstractObject) {
            params[1] = answerObject;
            params[2] = ((AbstractObject)answerObject).getLocatorURI();
        }
        return params;
    }

    /**
     * Internal method that prints the last operation answer to the output.
     *
     * @param subAnswerIndex the index of the sub-answer to display (if <tt>null</tt> the whole answer is used)
     * @param out a stream where the application writes information for the user
     * @param args additional optional arguments for the display
     * @param argIndex the index of the first optional argument
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    private boolean operationAnswer(Integer subAnswerIndex, PrintStream out, int argIndex, String[] args) {
        QueryOperation<?> op;
        try {
            op = (QueryOperation<?>)getLastOperation();
        } catch (ClassCastException ignore) {
            op = null;
        }
        if (op == null) {
            out.println("The operationAnswer method must be called after some QueryOperation was executed");
            return false;
        }

        // The next optional argument is the type of output (defaults to All)
        String answerFormatString = (args.length > argIndex && args[argIndex].length() > 0) ? args[argIndex] : "All";
        argIndex++;

        // The next optional argument is the separator (defaults to newline)
        String separator = (args.length > argIndex) ? args[argIndex] : System.getProperty("line.separator");
        argIndex++;

        // The next optional argument is a locale (defaults to system default locale)
        Locale locale = (args.length > argIndex) ? new Locale(args[argIndex]) : Locale.getDefault();
        argIndex++;

        // The next optional argument is the maximal number of results to display (all results are displayed if not specified)
        int maxCount;
        try {
            maxCount = retrieveIntArgument(args, argIndex, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
            argIndex++;
        } catch (NumberFormatException e) {
            out.println("Invalid number of objects to display: " + args[argIndex]);
            return false;
        }

        // The next optional argument is the number results to skip before the actual answer is displayed (defaults to zero)
        int skipCount;
        try {
            skipCount = retrieveIntArgument(args, argIndex, 0, 0, Integer.MAX_VALUE);
            argIndex++;
        } catch (NumberFormatException e) {
            out.println("Invalid number of objects to skip: " + args[argIndex]);
            return false;
        }

        // Prepare message format for the output
        MessageFormat answerFormat;
        switch (Character.toUpperCase(answerFormatString.charAt(0))) {
            case 'A':
                answerFormat = new MessageFormat("{0}", locale);
                break;
            case 'O':
                answerFormat = new MessageFormat("{1}", locale);
                break;
            case 'L':
                answerFormat = new MessageFormat("{2}", locale);
                break;
            case 'D':
                answerFormat = new MessageFormat("{3}: {2}", locale);
                break;
            case 'S':
                answerFormat = new MessageFormat("{2}: {3}", locale);
                break;
            case 'M':
                answerFormat = new MessageFormat("{2}: {4}", locale);
                break;
            case 'W':
                out.println("Use operationAnswerRawObjects method instead of operationAnswer to print raw objects");
                return false;
            default:
                answerFormat = new MessageFormat(answerFormatString, locale);
                break;
        }

        // Display output
        Iterator<?> answerIterator = subAnswerIndex != null ? op.getSubAnswer(subAnswerIndex.intValue()) : op.getAnswer();
        while (skipCount-- > 0 && answerIterator.hasNext())
            answerIterator.next();
        while (answerIterator.hasNext() && maxCount-- > 0) {
            Object[] parsedAnswerObject = parseAnswerObject(answerIterator.next(), subAnswerIndex);
            out.print(answerFormat.format(parsedAnswerObject));
            if (answerIterator.hasNext() && maxCount > 0)
                out.print(separator);
        }
        out.println();

        return true;
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
     * The following optional arguments are accepted:
     *   <ul>
     *     <li>result formating type (defaults to All) can be
     *       <ul>
     *          <li>All = display everything,</li>
     *          <li>Objects = displays just objects,</li>
     *          <li>Locators = display just locators,</li>
     *          <li>DistanceLocators = display format "distance: locator",</li>
     *          <li>SwappedDistanceLocators = display format "locator: distance",</li>
     *          <li>anything else will be treated as {@link MessageFormat} with four (zero-base indexed) arguments containing:
     *              the given answer object as-is,
     *              the {@link AbstractObject} from the answer object,
     *              the {@link AbstractObject#getLocatorURI() locator} of the {@link AbstractObject},
     *              the distance.</li>
     *       </ul>
     *     </li>
     *     <li>results separator (defaults to newline)</li>
     *     <li>language locale code for formating (defaults to system language)</li>
     *     <li>number of results to display (defaults to all)</li>
     *     <li>number of results to skip from the beginning (defaults to 0)</li>
     *   </ul>
     * </p>
     *
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; operationAnswer DistanceLocators , en
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args display separator for the list of objects and type of the display
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "list objects retrieved by the last executed query operation", arguments = {"objects separator (not required)", "display All/Objects/Locators/DistanceLocators/SLocatorsDistance (defaults to All)", "number of results to display (defaults to all)", "number of results to skip (defaults to 0)"})
    public boolean operationAnswer(PrintStream out, String... args) {
        return operationAnswer(null, out, 1, args);
    }

    /**
     * Show the sub-answer of the last executed query operation if possible.
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
     * The following arguments are accepted:
     *   <ul>
     *     <li>zero-based index of the sub-answer to show (required argument)</li>
     *     <li>result formating type (defaults to All) can be
     *       <ul>
     *          <li>All = display everything,</li>
     *          <li>Objects = displays just objects,</li>
     *          <li>Locators = display just locators,</li>
     *          <li>DistanceLocators = display format "distance: locator",</li>
     *          <li>SwappedDistanceLocators = display format "locator: distance",</li>
     *          <li>MetaObjectLocatorDistances = display format "locator: metaobject-subdistance",</li>
     *          <li>anything else will be treated as {@link MessageFormat} with five (zero-base indexed) arguments containing:
     *              the given answer object as-is,
     *              the {@link AbstractObject} from the answer object,
     *              the {@link AbstractObject#getLocatorURI() locator} of the {@link AbstractObject},
     *              the distance,
     *              the respective sub-distance if the meta-object distances were stored.</li>
     *       </ul>
     *     </li>
     *     <li>results separator (defaults to newline)</li>
     *     <li>language locale code for formating (defaults to system language)</li>
     *     <li>number of results to display (defaults to all)</li>
     *     <li>number of results to skip from the beginning (defaults to 0)</li>
     *   </ul>
     * </p>
     *
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; operationSubAnswer 1 Locators
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args sub-answer index, display separator for the list of objects, and type of the display
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "list sub-answer objects retrieved by the last executed query operation", arguments = {"sub-answer index to show", "objects separator (not required)", "display All/Objects/Locators/DistanceLocators/SLocatorsDistance (defaults to All)", "number of results to display (defaults to all)", "number of results to skip (defaults to 0)"})
    public boolean operationSubAnswer(PrintStream out, String... args) {
        Integer subAnswerIndex;
        try {
            subAnswerIndex = Integer.valueOf(args[1]);
        } catch (Exception e) {
            out.println("Error reading sub-answer index from the first argument");
            return false;
        }
        return operationAnswer(subAnswerIndex, out, 2, args);
    }

    /**
     * Write the raw objects (using {@link LocalAbstractObject#write}) from the answer of the last executed query operation.
     * Specifically, the answer of the operation created by last call to
     * {@link #operationExecute} or {@link #operationBgExecute} is written.
     * Note that the operation might be still running if the {@link #operationBgExecute} was
     * used and thus the results might not be complete. Use {@link #operationWaitBg}
     * to wait for background operations to finish.
     * <p>
     * If the last operation was not {@link messif.operations.QueryOperation query} operation,
     * this method will fail.
     * </p>
     *
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; operationAnswerRawObjects
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args no arguments required
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     * @throws IOException if some of the objects failed to write themselves to output
     * @throws ClassCastException if the operation did not provide {@link LocalAbstractObject}s in its answer
     */
    @ExecutableMethod(description = "list sub-answer objects retrieved by the last executed query operation", arguments = {"sub-answer index to show", "objects separator (not required)", "display All/Objects/Locators/DistanceLocators/SLocatorsDistance (defaults to All)", "number of results to display (defaults to all)", "number of results to skip (defaults to 0)"})
    public boolean operationAnswerRawObjects(PrintStream out, String... args) throws IOException, ClassCastException {
        Iterator<AbstractObject> it;
        try {
            it = ((QueryOperation<?>)getLastOperation()).getAnswerObjects();
        } catch (Exception ignore) {
            out.println("The operationAnswerRawObjects method must be called after some QueryOperation was executed");
            return false;
        }
        while (it.hasNext()) {
            ((LocalAbstractObject)it.next()).write(out);
        }
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
        Algorithm alg = getAlgorithm();
        if (alg == null) {
            out.println("No running algorithm is selected");
            return false;
        }
        if (args.length < 2) {
            out.println("methodExecute requires at least the method name (see 'help methodExecute')");
            return false;
        }

        try {
            Object rtv = alg.executeMethodWithStringArguments(args, 1, namedInstances);
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

    /**
     * Directly execute a method of specified running algorithm.
     * The first argument is the number of the running algorithm,
     * then the method name and its arguments must be provided.
     * Only {@link messif.utility.Convert#stringToType convertible} types can
     * be passed as arguments and if there are several methods with the same name,
     * the first one that matches the number of arguments is selected.
     *
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; methodExecuteOnAlgorithm 0 mySpecialAlgorithmMethod 1 false string_string
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args method name followed by the values for its arguments
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "directly execute a method of specified running algorithm", arguments = {"algorithm number", "method name", "arguments for the method ..."})
    public boolean methodExecuteOnAlgorithm(PrintStream out, String... args) {
        if (args.length < 3) {
            out.println("methodExecuteOnAlgorithm requires at least the algorithm number and method name (see 'help methodExecuteOnAlgorithm')");
            return false;
        }
        Algorithm alg;
        try {
            alg = getAlgorithm(Integer.parseInt(args[1]));
        } catch (IndexOutOfBoundsException ex) {
            out.println("Specified algorithm number is illegal");
            return false;
        }

        try {
            Object rtv = alg.executeMethodWithStringArguments(args, 2, namedInstances);
            if (rtv != null)
                out.println(rtv);
            return true;
        } catch (NoSuchInstantiatorException e) {
            out.println("Method '" + args[2] + "' with " + (args.length - 3) + " arguments was not found in algorithm");
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
    @ExecutableMethod(description = "show last operation statistics", arguments = { "statistic name regexp (not required)", "separator of statistics (not required)", "separator appended after printed statistics (defaults to newline)"})
    public boolean statisticsLastOperation(PrintStream out, String... args) {
        Algorithm alg = getAlgorithm();
        if (args.length >= 3) {
            String stats = alg.getOperationStatistics().printStatistics(args[1], args[2]);
            if (args.length >= 4) {
                out.print(stats);
                out.print(args[3]);
            } else {
                out.println(stats);
            }
        } else if (args.length >= 2) {
            out.println(alg.getOperationStatistics().printStatistics(args[1]));
        } else {
            out.println(alg.getOperationStatistics().printStatistics());
        }
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
        if (objectStream != null) {
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
        } else {
            out.print("Stream '" + args[1] + "' is not opened");
        }

        return false;
    }

    /**
     * Applies an additional conversion on the objects returned by an object stream.
     * The first required argument specifies an object stream.
     * The second required argument specifies the name of a {@link Convertor} instance.
     *
     * <p>
     * Extension: if the second argument (convertor instance name) starts with a dot,
     * a new convertor instance is created that converts a {@link MetaObject} to the
     * respective encapsulated object with that name (without the dot).
     * </p>
     *
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; objectStreamConvert my_data my_data_convertor_named_instance
     * MESSIF &gt;&gt;&gt; objectStreamConvert my_data .MySpecialLocalObjectName
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args opened object stream to restrict,
     *             the name of a convertor instance
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @SuppressWarnings("unchecked")
    @ExecutableMethod(description = "applies conversion to stream objects", arguments = { "opened object stream", "convertor named instance" })
    public boolean objectStreamConvert(PrintStream out, String... args) {
        if (args.length < 3) {
            out.println("objectStreamConvert requires a stream and convertor instance names");
            return false;
        }

        Iterator<?> objectStream = (Iterator<?>)namedInstances.get(args[1]);
        if (objectStream == null) {
            out.print("Stream '" + args[1] + "' is not opened");
            return false;
        }

        Convertor<Object, ?> convertor;
        if (args[2].startsWith(".")) {
            final String objectName = args[2].substring(1);
            convertor = new Convertor<Object, LocalAbstractObject>() {
                @Override
                public LocalAbstractObject convert(Object value) {
                    if (value == null)
                        return null;
                    return ((MetaObject)value).getObject(objectName);
                }
                @Override
                public Class<? extends LocalAbstractObject> getDestinationClass() {
                    return LocalAbstractObject.class;
                }
            };
        } else {
            convertor = (Convertor<Object, ?>)namedInstances.get(args[2]); // This cast in not checked, the convertor must accept any object
        }
        
        namedInstances.put(args[1], new ConvertorIterator<Object, Object>(objectStream, convertor));
        return true;
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

    /**
     * Skip objects in a named object stream.
     *
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; objectStreamSkip my_data 100 abcd
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args the name of an opened object stream,
     *          the number of objects to skip (or zero for infinite),
     *          the locator (regular expression) of an object to skip to
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "skip objects from the stream", arguments = { "name of the stream", "number of objects to skip (or -1 for infinite)", "locator (regexp) of an object to skip to" })
    public boolean objectStreamSkip(PrintStream out, String... args) {
        if (args.length < 3) {
            out.println("objectStreamSkip requires a stream name and number of objects to skip (see 'help objectStreamSkip')");
            return false;
        }
        AbstractStreamObjectIterator<?> objectStream = (AbstractStreamObjectIterator<?>)namedInstances.get(args[1]);
        if (objectStream == null) {
            out.print("Stream '" + args[1] + "' is not opened");
            return false;
        }

        // Skip by number of objects
        try {
            int skipObjects = Integer.parseInt(args[2]);
            if (skipObjects > 0)
                objectStream.skip(skipObjects);
        } catch (NumberFormatException e) {
            out.println("Cannot convert number of objects to skip: " + e);
            return false;
        }

        // Skip by locator
        if (args.length > 3 && !args[3].isEmpty())
            objectStream.getObjectByLocatorRegexp(args[3]);

        return true;
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

    /**
     * Creates a new named instance of properties from the values of configuration file.
     * Note that this method is not available from the command line interface.
     * This action does not have .parameter.#no syntax, but every value passed as
     * with this action name will be stored in the created properties.
     *
     * <p>
     * Example of usage:
     * <pre>
     * myProps = propertiesCreate
     * myProps.propName1 = propValue1
     * myProps.propName2 = propValue2
     * </pre>
     * </p>
     * This configuration file action will create (or replace) a named instance called "myProps"
     * with a {@link ExtendedProperties} instance that will contain properties "propName1" and
     * "propName2" with values "propValue1" and "propValue2" respectively.
     *
     * @param out a stream where the application writes information for the user
     * @param inputProperties the properties to copy from
     * @param name the name for the instance
     * @param prefix the restrict prefix
     * @param variables the map of variables
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    private boolean propertiesCreate(PrintStream out, Properties inputProperties, String name, String prefix, Map<String, String> variables) {
        ExtendedProperties properties = ExtendedProperties.restrictProperties(inputProperties, prefix, variables);
        if (namedInstances.put(name, properties) != null)
            out.println("Previous named instance changed to a new one");
        return true;
    }


    //****************** Named instances ******************//

    /**
     * Returns the stored named instance with the given name.
     * @param name the name of the instance to get
     * @return the stored named instance or <tt>null</tt>
     */
    protected final Object getNamedInstance(String name) {
        return namedInstances.get(name);
    }

    /**
     * Add the given named instance to the internal storage.
     * @param name the name of the instance to add
     * @param instance the instance to add
     * @param replace flag whether to replace existing instance with the given one (<tt>true</tt>) or not
     * @return <tt>true</tt> if the instance was successfully added or <tt>false</tt> if there was an instance with this name and replace was <tt>false</tt>
     */
    protected final boolean addNamedInstance(String name, Object instance, boolean replace) {
        if (!replace && namedInstances.containsKey(name))
            return false;
        namedInstances.put(name, instance);
        return true;
    }

    /**
     * Returns a copy of the current named instances with the given value set for the given key.
     * If key is <tt>null</tt>, no modifications to the named instances are made.
     * @param key the name for the new named instance
     * @param value the new named instance
     * @param replaceExisting flag whether to replace the instance if it already exists
     * @return a copy of the current named instances map
     */
    protected final Map<String, Object> getExtendedNamedInstances(String key, Object value, boolean replaceExisting) {
        Map<String, Object> ret = new HashMap<String, Object>(namedInstances);
        if (replaceExisting || !ret.containsKey(key)) {
            ret.put(key, value);
        }
        return ret;
    }

    /**
     * Creates a new named instance.
     * An argument specifying the signature of a constructor, a factory method or a static field
     * is required. Additional argument specifies the name for the instance (defaults to
     * name of the action where this is specified). If the instance already exists,
     * this method fails (use {@link #namedInstanceReplace namedInstanceReplace} instead.
     * Note that a "lastOperation" temporary named instance is available during the instantiation.
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
     * @throws InvocationTargetException if there was an error while creating a named instance
     */
    @ExecutableMethod(description = "creates a new named instance", arguments = { "instance constructor, factory method or static field signature", "name to register"})
    public boolean namedInstanceAdd(PrintStream out, String... args) throws InvocationTargetException {
        if (args.length <= 2) {
            out.println("Two arguments (signature and instance name) are required for namedInstanceAdd");
            return false;
        }
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
     * Note that a "lastOperation" temporary named instance is available during the instantiation.
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
     * @throws InvocationTargetException if there was an error while creating a named instance
     */
    @ExecutableMethod(description = "creates a new named instance or replaces old one", arguments = { "instance constructor, factory method or static field signature", "name to register"})
    public boolean namedInstanceReplace(PrintStream out, String... args) throws InvocationTargetException {
        if (args.length <= 2) {
            out.println("Two arguments (signature and instance name) are required for namedInstanceReplace");
            return false;
        }

        try {
            Object instance = InstantiatorSignature.createInstanceWithStringArgs(args[1], Object.class, getExtendedNamedInstances("lastOperation", getLastOperation(), true));
            namedInstances.put(args[2], instance);
            return true;
        } catch (NoSuchInstantiatorException e) {
            out.println("Error creating named instance for " + args[1] + ": " + e);
            return false;
        }
    }

    /**
     * Creates a named instance for the current running thread.
     * Note that if the given named instance exists, it must have been created by this method previously.
     * An argument specifying the signature of a constructor, a factory method or a static field
     * is required. Additional argument specifies the name for the instance.
     * Note that a "lastOperation" temporary named instance is available during the instantiation.
     * <p>
     * Example of usage for constructor, factory method and static field:
     * <pre>
     * MESSIF &gt;&gt;&gt; namedInstanceThread messif.objects.impl.ObjectByteVectorL1(1,2,3,4,5,6,7,8,9,10) my_object
     * MESSIF &gt;&gt;&gt; namedInstanceThread messif.utility.ExtendedProperties.getProperties(someparameters.cf) my_props
     * MESSIF &gt;&gt;&gt; namedInstanceThread messif.buckets.index.LocalAbstractObjectOrder.locatorToLocalObjectComparator my_comparator
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args the instance constructor, factory method or static field signature and the name to register
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     * @throws InvocationTargetException if there was an error while creating a named instance
     */
    @ExecutableMethod(description = "creates a named instance for the current running thread", arguments = { "instance constructor, factory method or static field signature", "name to register"})
    @SuppressWarnings("unchecked")
    public boolean namedInstanceThread(PrintStream out, String... args) throws InvocationTargetException {
        if (args.length <= 2) {
            out.println("Two arguments (signature and instance name) are required for namedInstanceThread");
            return false;
        }

        ThreadLocal<Object> valueHolder;
        synchronized (namedInstances) {
            if (namedInstances.containsKey(args[2])) {
                valueHolder = (ThreadLocal<Object>)namedInstances.get(args[2]);
            } else {
                valueHolder = new InheritableThreadLocal<Object>();
                namedInstances.put(args[2], valueHolder);
            }
        }

        try {
            valueHolder.set(InstantiatorSignature.createInstanceWithStringArgs(args[1], Object.class, getExtendedNamedInstances("lastOperation", getLastOperation(), true)));
            return true;
        } catch (NoSuchInstantiatorException e) {
            out.println("Error creating named instance for " + args[1] + ": " + e);
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
    @ExecutableMethod(description = "removes a named instance", arguments = { "name of the instance" })
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

    /**
     * Prints the value of a named instance.
     * An argument specifying the name of the instance to print is required.
     * Note that the argument also accepts a named instance invocation using the same
     * syntax as for {@link #namedInstanceReplace(java.io.PrintStream, java.lang.String[])}.
     *
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; namedInstanceEcho my_object
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args the name of the instance to print
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     * @throws InvocationTargetException if there was an error while printing a named instance
     */
    @ExecutableMethod(description = "prints the value of a named instance", arguments = { "name of the instance" })
    public boolean namedInstanceEcho(PrintStream out, String... args) throws InvocationTargetException {
        if (args.length <= 1) {
            out.println("The argument with the instance name is required for namedInstanceEcho");
            return false;
        }

        try {
            Object value = namedInstances.get(args[1]);
            if (value == null) // Named instance not accessed directly, try instantiation
                value = InstantiatorSignature.createInstanceWithStringArgs(args[1], Object.class, namedInstances);
            out.println(Convert.expandReferencedInstances(value));
            return true;
        } catch (NoSuchInstantiatorException e) {
            out.println("Error creating named instance for " + args[1] + ": " + e);
            return false;
        }

    }

    /**
     * Creates a new named instance from an object serialized in a file.
     * An argument specifying the file from which to read the instance is required.
     * Additional argument specifies the name for the instance (defaults to
     * name of the action where this is specified).
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; namedInstanceRestore path/to/serialized/object.bin my_object
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args the file with the serialized object and the name to register
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     * @throws IOException if there was an error while reading from the file
     * @throws ClassNotFoundException if there was an error while creating a named instance
     */
    @ExecutableMethod(description = "creates a new named instance serialized in a file", arguments = { "file path", "name to register"})
    public boolean namedInstanceRestore(PrintStream out, String... args) throws IOException, ClassNotFoundException {
        if (args.length <= 2) {
            out.println("Two arguments (file path and instance name) are required for namedInstanceRestore");
            return false;
        }
        if (namedInstances.containsKey(args[2])) {
            out.println("Named instance '" + args[2] + "' already exists");
            return false;
        }
        ObjectInputStream stream = new ObjectInputStream(new FileInputStream(args[1]));
        try {
            namedInstances.put(args[2], stream.readObject());
            return true;
        } finally {
            stream.close();
        }
    }

    /**
     * Serializes the value of a named instance into a file.
     * Two arguments specifying the name of the instance to write and
     * the file path where the object will be written are required.
     *
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; namedInstanceStore my_object path/to/serialized/object.bin
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args the name of the instance to write and the file name
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     * @throws IOException if there was an error while storing the named instance
     */
    @ExecutableMethod(description = "prints the value of a named instance", arguments = { "name of the instance" })
    public boolean namedInstanceStore(PrintStream out, String... args) throws IOException {
        if (args.length <= 2) {
            out.println("Two arguments are required for namedInstanceStore:  the instance name and the file path");
            return false;
        }

        Object value = namedInstances.get(args[1]);
        if (value == null) {
            out.print("There is no instance with name '" + args[1] + "'");
            return false;
        }

        ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(args[2]));
        try {
            stream.writeObject(value);
            return true;
        } finally {
            stream.close();
        }
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
     * of milliseconds specified.
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
     * Returns current time in milliseconds or the specified date format.
     *
     * <p>
     * Example of usage:
     * <pre>
     * MESSIF &gt;&gt;&gt; currentTime "d M yyyy HH:mm"
     * </pre>
     * </p>
     *
     * @param out a stream where the application writes information for the user
     * @param args an optional argument specifying the date/time format according to {@link SimpleDateFormat}
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "returns current time in milliseconds", arguments = { })
    public boolean currentTime(PrintStream out, String... args) {
        if (args.length > 1) {
            try {
                out.print(new SimpleDateFormat(args[1]).format(new Date()));
            } catch (IllegalArgumentException e) {
                out.println("Wrong date format '" + args[1] + "': " + e.getMessage());
                return false;
            }
        } else {
            out.print(System.currentTimeMillis());
        }
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
        else if (separator != null)
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
    private static final Pattern variablePattern = Pattern.compile("(?:\\\\?<|\\$\\{)([^>}]+?)([:!]-?[^>}]*)?(?:>|\\})", Pattern.MULTILINE);

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
            long sleepTime = Convert.timeToMilliseconds(postponeUntil) - System.currentTimeMillis();
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
                if (outputStreams == null) {
                    out.println("Cannot set outputFile for action '" + actionName + "' - output files are disabled in this context");
                    return null;
                }
                if (assignOutput != null)
                    out.println("WARNING: Action '" + actionName + "' has both the 'outputFile' and the 'assign' parameters defined. Using outputFile.");
                PrintStream outputStream = outputStreams.get(fileName);
                if (outputStream == null) { // Output stream not opened yet
                    outputStream = new PrintStream(fileName);
                    outputStreams.put(fileName, outputStream);
                }
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
     * @param modifierName the action modifier name that contains the exception
     * @param variables the current variables' environment
     * @return the action's repeat-until exception class or <tt>null</tt> if they are not specified
     */
    protected Class<? extends Throwable> getCFActionException(PrintStream out, Properties props, String actionName, String modifierName, Map<String,String> variables) {
        String repeatUntilException = props.getProperty(actionName + modifierName);
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
     * Creates a thread that processes the given action in given time intervals.
     * Note that the returned thread is not started.
     * @param repeatEveryModifier the {@link Convert#hmsToMilliseconds hour-minute-second specification} of the time interval
     * @param out the stream to write the output to
     * @param props the properties with actions
     * @param actionName the name of the action to execute
     * @param variables the current variables environment
     * @param outputStreams currently opened output streams
     * @return the created thread
     */
    protected Thread createCFActionRepeatEveryThread(String repeatEveryModifier, final PrintStream out, final Properties props, final String actionName, Map<String,String> variables, Map<String, PrintStream> outputStreams) {
        final long repeatTime = Convert.hmsToMilliseconds(substituteVariables(repeatEveryModifier, variables));
        final Map<String, String> variablesCopy = new HashMap<String, String>(variables);
        final Map<String, PrintStream> outputStreamsCopy = new HashMap<String, PrintStream>(outputStreams);
        return new Thread("Repeat action " + actionName + " every " + repeatEveryModifier) {
            @Override
            public void run() {
                while (!isInterrupted()) {
                    try {
                        sleep(repeatTime);
                    } catch (InterruptedException ignore) {
                        break;
                    }
                    try {
                        if (!controlFileExecuteAction(out, props, actionName, variablesCopy, outputStreamsCopy, false))
                            break;
                    } catch (InvocationTargetException e) {
                        throw new InternalError("Exception thrown even though it should not have been thrown: " + e);
                    }
                }
            }
        };
    }

    /**
     * This method reads and executes one action (with name actionName) from the control file (props).
     * For a full explanation of the command syntax see {@link CoreApplication}.
     *
     * @param out the stream to write the output to
     * @param props the properties with actions
     * @param actionName the name of the action to execute
     * @param variables the current variables environment
     * @param arguments the list of arguments parsed from the configuration file
     * @return <tt>true</tt> if the action was executed successfully
     * @throws NoSuchMethodException if there was no method for the given action
     * @throws InvocationTargetException if there was an error executing the action while the {@code throwException} is <tt>true</tt>
     */
    protected boolean controlFileExecuteMethod(PrintStream out, Properties props, String actionName, Map<String,String> variables, List<String> arguments) throws NoSuchMethodException, InvocationTargetException {
        // SPECIAL! Method propertiesOpen is called with additional arguments
        if (arguments.get(0).equals("propertiesOpen"))
            return propertiesOpen(out,
                    arguments.get(1), // fileName
                    (arguments.size() > 2)?arguments.get(2):actionName, // name
                    (arguments.size() > 3)?arguments.get(3):null, // prefix
                    (arguments.size() > 4)?Convert.stringToMap(arguments.get(4)):variables
            );
        // SPECIAL! Method propertiesCreate is called differently
        if (arguments.get(0).equals("propertiesCreate"))
            return propertiesCreate(out,
                    props,
                    actionName, // instance name
                    actionName + '.', // prefix
                    variables
            );
        // SPECIAL! Control file pass the variables to the called control file
        if (arguments.get(0).equals("controlFile"))
            return controlFileImpl(out, arguments.toArray(new String[arguments.size()]), new HashMap<String, String>(variables));

        // Normal method
        Object rtv = methodExecutor.execute(out, arguments.toArray(new String[arguments.size()]));
        out.flush();
        return !(rtv instanceof Boolean) || ((Boolean)rtv).booleanValue();
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
        if ((methodName.equals("objectStreamOpen") && arguments.size() == 3) || ((methodName.equals("namedInstanceAdd") || methodName.equals("namedInstanceRestore") || methodName.equals("namedInstanceReplace")) && arguments.size() == 2))
            arguments.add(actionName);

        // Read description (variable substition in description is done during the repeat)
        String description = props.getProperty(actionName + ".description");
        String descriptionAfter = props.getProperty(actionName + ".descriptionAfter");

        // Read the assign parameter and set the output stream
        String assignVariable = substituteVariables(props.getProperty(actionName + ".assign"), variables);
        ByteArrayOutputStream assignOutput = assignVariable != null ? new ByteArrayOutputStream() : null;
        PrintStream outputStream = getCFActionOutput(out, props, actionName, variables, outputStreams, assignOutput);
        if (outputStream == null)
            return false;

        // Read ignore exception modifier
        Class<? extends Throwable> ignoreExceptionClass = getCFActionException(out, props, actionName, ".ignoreException", variables);
        if (ignoreExceptionClass == Throwable.class)
            return false;

        // Read number of repeats (or foreach value) of this method
        String[] foreachValues = getCFActionForeach(out, props, actionName, variables);
        Class<? extends Throwable> repeatUntilExceptionClass = getCFActionException(out, props, actionName, ".repeatUntilException", variables);
        if (repeatUntilExceptionClass == Throwable.class)
            return false;
        int repeat = getCFActionRepeat(out, props, actionName, variables, foreachValues, repeatUntilExceptionClass);
        if (repeat < 0) {
            out.println("Number of repeats specified in action '" + actionName + "' is not a valid non-negative integer");
            return false;
        }

        // Loop variable setup
        String loopVariable = substituteVariables(props.getProperty(actionName + ".loopVariable"), variables);
        if (loopVariable == null)
            loopVariable = actionName;
        String savedLoopVariableValue = variables.get(loopVariable);

        // Postpone action
        if (!postponeCFAction(out, props, actionName, variables))
            return false;

        // Execute action
        try {
            for (int i = 0; i < repeat; i++) {
                // Add foreach/repeat value
                if (foreachValues != null) {
                    variables.put(loopVariable, foreachValues[i]);
                    variables.put(loopVariable + "_iteration", Integer.toString(i));
                } else if (props.containsKey(actionName + ".repeat")) {
                    variables.put(loopVariable, Integer.toString(i + 1));
                    variables.put(loopVariable + "_iteration", Integer.toString(i));
                }

                // Show description if set
                if (description != null)
                    outputStream.println(substituteVariables(description, variables));

                // Perform action
                if (methodName.indexOf(' ') != -1) {
                    // Special "block" method
                    for (String blockActionName : methodName.split("[ \t]+"))
                        if (!controlFileExecuteAction(outputStream, props, blockActionName, variables, outputStreams, throwException || repeatUntilExceptionClass != null || ignoreExceptionClass != null)) {
                            if (assignOutput != null)
                                out.print(assignOutput.toString());
                            return false; // Stop execution of block if there was an error
                        }
                } else try {
                    // Execute "normal" method
                    if (!controlFileExecuteMethod(outputStream, props, actionName, variables, arguments)) {
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
                        // There was no method for the action, so we try it as a name of block
                        if (!controlFileExecuteAction(outputStream, props, methodName, variables, outputStreams, throwException || repeatUntilExceptionClass != null || ignoreExceptionClass != null))
                            return false;
                }

                // Show descriptionAfter if set
                if (descriptionAfter != null)
                    outputStream.println(substituteVariables(descriptionAfter, variables));
            }
        } catch (InvocationTargetException e) {
            if (repeatUntilExceptionClass != null && repeatUntilExceptionClass.isInstance(getRootCauseInvocationTargetException(e))) {
                // Repeat-until exception is captured, continue processing
            } else if (ignoreExceptionClass != null && ignoreExceptionClass.isInstance(getRootCauseInvocationTargetException(e))) {
                // Ignore exception is captured, continue processing
            } else if (throwException) { // Exception is being handled by a higher call
                throw e;
            } else { // Action failed with exception
                processException(e.getCause(), out, true);
                out.println("Action '" + actionName + "' failed - control file execution was terminated");
                return false;
            }
        }

        // Restore loop variable value (after nested foreach/repeat)
        if (foreachValues != null || props.containsKey(actionName + ".repeat")) {
            variables.remove(loopVariable + "_iteration");
            if (savedLoopVariableValue == null)
                variables.remove(loopVariable);
            else
                variables.put(loopVariable, savedLoopVariableValue);
        }

        // Assign variable is requested
        if (assignVariable != null && assignOutput != null)
            variables.put(assignVariable, assignOutput.toString().trim());

        // If repeatEvery modifier was specified, start the thread
        String repeatEveryModifier = props.getProperty(actionName + ".repeatEvery");
        if (repeatEveryModifier != null) {
            synchronized (repeatEveryThreads) {
                if (!repeatEveryThreads.containsKey(actionName)) {
                    Thread repeatEveryThread = createCFActionRepeatEveryThread(repeatEveryModifier, out, props, actionName, variables, outputStreams);
                    repeatEveryThreads.put(actionName, repeatEveryThread);
                    repeatEveryThread.start();
                }
            }
        }

        return true; // Execution successful
    }

    /**
     * Implementation of the control file action that allows to pass the existing variables.
     * @param out a stream where the application writes information for the user
     * @param args file name followed by variable specifications and start action
     * @param variables the original variables (some may be overwritten 
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    private boolean controlFileImpl(PrintStream out, String[] args, Map<String, String> variables) {
        // Open control file and create properties
        Properties props = new Properties();
        try {
            InputStream stream;
            if (args[1].equals("-")) {
                stream = System.in;
            } else {
                try {
                    stream = new FileInputStream(args[1]);
                } catch (FileNotFoundException e) {
                    // Try to read the properties from class path
                    stream = getClass().getResourceAsStream(args[1]);
                    if (stream == null)
                        throw e;
                }
            }
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
        if (variables == null)
            variables = new HashMap<String, String>();
        for (int i = 2; i < args.length; i++) {
            String[] varVal = args[i].split("=", 2);
            if (varVal.length == 2)
                variables.put(Convert.trimAndUnquote(varVal[0]), Convert.trimAndUnquote(varVal[1]));
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

    /**
     * Executes actions from a control file.
     * All commands that can be started from the prompt can be used in control files.
     * The first argument is required to specify the file with commands.
     * Additional arguments are either variable specifications in the form of "varname=value"
     * or the action name that is started (which defaults to "actions").
     * For a full explanation of the command syntax see {@link CoreApplication}.
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
        return controlFileImpl(out, args, null);
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
                        } catch (ClosedChannelException ignore) {
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

