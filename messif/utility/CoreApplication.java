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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import messif.objects.util.RankedSortedCollection;
import messif.objects.util.StreamGenericAbstractObjectIterator;
import messif.operations.AbstractOperation;
import messif.operations.QueryOperation;
import messif.operations.RankingQueryOperation;
import messif.statistics.OperationStatistics;
import messif.statistics.Statistics;
import messif.utility.reflection.FactoryMethodInstantiator;
import messif.utility.reflection.Instantiators;


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
 *  &lt;actionName&gt;.outputFile = &lt;filename&gt;
 *  &lt;actionName&gt;.assign = &lt;variable name&gt;
 *  &lt;actionName&gt;.postponeUntil = hh:mm:ss</pre>
 * <ul>
 * <li>&lt;actionName&gt; is a user specified name for the action which can be reffered from other
 *                    actions (&lt;otherActionName1&gt; &lt;otherActionName2&gt;) or command line parameter <i>[action]</i>.</li>
 * <li>&lt;methodName&gt; can be any {@link CoreApplication} method, which is to be executed if &lt;actionName&gt; is called.
 *                    If a space-separated list of other action names is provided, they will be executed one by one
 *                    in the order they were specified. Parameters for the method are specified using &lt;actionName&gt;.param.<i>x</i>,
 *                    see the documentation of the respective {@link CoreApplication} methods for their parameters.</li>
 * <li><i>repeat</i> parameter is optional and allows to specify multiple execution of
 *                 the same action &lt;repeats&gt; times. It can be used together with "block" method name to implement
 *                 a loop of commands with specified number of repeats.</li>
 * <li><i>foreach</i> parameter is also optional and similarly to <i>repeat</i> it allows the action to be
 *                executed multiple times - the number of repeats is equal to the number of values provided.
 *                Moreover, in each iteration the variable &lt;actionName&gt; is assigned &lt;value&gt; taken
 *                one by one from the <i>foreach</i> parameter</li>
 * <li><i>outputFile</i> parameter is optional and allows to redirect output of this block to a file
 *  &lt;filename&gt;. When this filename is reached for the first time, it is opened for writing
 *  (previous contents are destroyed) and all succesive writes are appended to this file
 *  until this batch run finishes.</li>
 * <li><i>assign</i> parameter is optional and allows to redirect output of this block to a variable
 *  &lt;variable name&gt;. The previous contents of the variable are replaced by the new value and the
 *  variable is available after the action with "assign" is finished</li>
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
public class CoreApplication {
    /** Logger */
    protected static Logger log = Logger.getLogger("application");

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
        List<Constructor<Algorithm>> constructors = Algorithm.getAnnotatedConstructors(algorithmClass);
        try {
            // Create a new instance of the algorithm
            algorithm = Instantiators.createInstanceWithStringArgs(constructors, args, 2, namedInstances);
            algorithms.add(algorithm);
            return true;
        } catch (InvocationTargetException e) {
            Throwable ex = e.getCause();
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
     */
    private AbstractOperation createOperation(PrintStream out, String[] args) {
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
                        2, // skip the method name and operation class arguments
                        namedInstances
                    )
            );
        } catch (InvocationTargetException e) {
            out.println(e.getCause().toString());
        } catch (Exception e) {
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
     */    
    @ExecutableMethod(description = "execute specified operation on current algorithm instance", arguments = {"operation class", "arguments for constructor ..."})
    public boolean operationExecute(PrintStream out, String... args) {
        if (algorithm == null) {
            out.println("No running algorithm is selected");
            return false;
        }

        AbstractOperation operation = createOperation(out, args);
        if (operation == null)
            return false;

        try {
            // Execute operation
            algorithm.resetOperationStatistics();
            if (bindOperationStatsRegexp != null)
                OperationStatistics.getLocalThreadStatistics().registerBoundAllStats(bindOperationStatsRegexp);
            lastOperation = algorithm.executeOperation(operation);
            if (bindOperationStatsRegexp != null)
                OperationStatistics.getLocalThreadStatistics().unbindAllStats(bindOperationStatsRegexp);
            return true;
        } catch (AlgorithmMethodException e) {
            logException(e.getCause());
            out.println(e.getCause().toString());
            return false;
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
     */    
    @ExecutableMethod(description = "execute on background specified operation on current algorithm instance", arguments = {"operation class", "arguments for constructor ..."})
    public boolean operationBgExecute(PrintStream out, String... args) {       
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
     */
    @ExecutableMethod(description = "wait for all background operations", arguments = {})
    public boolean operationWaitBg(PrintStream out, String... args) {
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
        } catch (AlgorithmMethodException e) {
            logException(e.getCause());
            out.println(e.getCause().toString());
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
     */    
    @ExecutableMethod(description = "execute the last operation once more", arguments = {"boolean whether to reset operation answer (default: false)"})
    public boolean operationExecuteAgain(PrintStream out, String... args) {
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
        } catch (AlgorithmMethodException e) {
            logException(e.getCause());
            out.println(e.getCause().toString());
            return false;
        } catch (NoSuchMethodException e) {
            out.println(e.getMessage());
            algorithmSupportedOperations(out, args);
            return false;
        }
    }

    /**
     * Show information about the last executed operation.
     * Specifically, the information about the operation created by last call to
     * {@link #operationExecute} or {@link #operationBgExecute} is shown. Note that
     * the operation might be still running if the {@link #operationBgExecute} was
     * used and thus the results might not be complete. Use {@link #operationWaitBg}
     * to wait for background operations to finish.
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
     * Changes the answer collection of the last executed operation.
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
            @SuppressWarnings("unchecked")
            RankedSortedCollection newAnswerCollection = Instantiators.createInstanceWithStringArgs(Arrays.asList((Constructor<RankedSortedCollection>[])clazz.getConstructors()), args, 2);

            // Set the instance in the operation
            ((RankingQueryOperation)operation).setAnswerCollection(newAnswerCollection);

            return true;
        } catch (ClassNotFoundException e) {
            out.println(e);
            return false;
        } catch (InvocationTargetException e) {
            out.println(e.getCause());
            return false;
        }
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
     */
    @ExecutableMethod(description = "process the last executed operation by a static method", arguments = {"object class", "method name", "additional arguments for the method (optional) ..."})
    public boolean operationProcessByMethod(PrintStream out, String... args) {
        AbstractOperation operation = lastOperation;
        if (operation == null) {
            out.println("No operation has been executed yet");
            return false;
        }

        try {
            // Prepare method
            FactoryMethodInstantiator<AbstractOperation> method = new FactoryMethodInstantiator<AbstractOperation>(AbstractOperation.class, Class.forName(args[1]), args[2], args.length - 2);

            // Prepare arguments
            String[] stringArgs = args.clone();
            stringArgs[2] = null;
            Object[] methodArgs = Convert.parseTypesFromString(stringArgs, method.getInstantiatorPrototype(), 2, namedInstances);
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
        } catch (IllegalArgumentException e) {
            out.println(e.getMessage());
            return false;
        } catch (InvocationTargetException e) {
            logException(e.getCause());
            out.println("Error executing method " + args[1] + "." + args[2] + ": " + e.getCause());
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
     *            'Object' = displays just objects or 'Locator' = display just locators</li>
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
            // Get executed method
            for (Method method : algorithm.getClass().getMethods()) {
                // Check method name
                if (!method.getName().equals(args[1]))
                    continue;

                // Check method argument count
                Class<?>[] argTypes = method.getParameterTypes();
                if (argTypes.length != args.length - 2)
                    if (argTypes.length == 0 || !argTypes[argTypes.length - 1].isArray() || argTypes.length > args.length - 1)
                        continue;

                // Try to invoke the method
                Object rtv = method.invoke(algorithm, Convert.parseTypesFromString(args, argTypes, 2, namedInstances));
                if (!method.getReturnType().equals(void.class))
                    out.println(rtv);
                return true;
            }

            out.println("Method '" + args[1] + "' with " + (args.length - 2) + " arguments was not found in algorithm");
            return false;
        } catch (RuntimeException e) {
            logException(e);
            out.println(e.toString());
            return false;
        } catch (InvocationTargetException e) {
            Throwable ex = e.getCause();
            if (ex instanceof AlgorithmMethodException || ex instanceof InvocationTargetException)
                ex = ex.getCause();
            logException(ex);
            out.println(ex.toString());
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
            out.println(Instantiators.createInstanceUsingFactoryMethod(Convert.getClassForName(args[2], Statistics.class), "getStatistics", args[1]));
        } catch (InvocationTargetException e) {
            out.println("Cannot get global statistics: " + e.getCause());
            return false;
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
                objectStream.setConstructorParameter((args.length > 3)?Integer.parseInt(args[3]):0, args[2]);
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
            Object instance = Instantiators.createInstanceWithStringArgs(args[1], Object.class, namedInstances);
            namedInstances.put(args[2], instance);
            return true;
        } catch (ClassNotFoundException e) {
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
     * Get or set global level of logging.
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
     * Adds a file for writing loging messages.
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
     * is applied to - see {@link Logging.RegexpFilterAgainst} values for explanation.
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
     * @param args the file name, logging level, append to file flag, simple/xml format selector, regular expression and which part of the message is matched
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
                    (args.length > 6)?                                                          // regexp against
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
     * Removes loging file.
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
     * Changes the loging level of an opened logging file.
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
     * @param args number of miliseconds to sleep after the garbage collection
     * @return <tt>true</tt> if the method completes successfully, otherwise <tt>false</tt>
     */
    @ExecutableMethod(description = "shedule full garbage collection", arguments = { "time to sleep (optional)" })
    public boolean collectGarbage(PrintStream out, String... args) {
        System.gc();
        if (args.length >= 2) {
            try {
                Thread.sleep(Integer.parseInt(args[1]));
            } catch (InterruptedException e) {
                out.println("Sleep was interrupted: " + e.toString());
                return false;
            }
        } else Thread.yield();

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
     * This method reads and executes one action (with name actionName) from the control file (props).
     * For a full explanation of the command sytax see {@link CoreApplication}.
     * 
     * @param out the stream to write the output to
     * @param props the properties with actions
     * @param actionName the name of the action to execute
     * @param variables the current variables' environment
     * @param outputStreams currently opened output streams
     * @return <tt>true</tt> if the action was executed successfuly
     */
    protected boolean controlFileExecuteAction(PrintStream out, Properties props, String actionName, Map<String,String> variables, Map<String, PrintStream> outputStreams) {
        // Check for postponed execution
        String postponeUntil = Convert.substituteVariables(props.getProperty(actionName + ".postponeUntil"), variablePattern, 1, 2, variables);
        if (postponeUntil != null && postponeUntil.trim().length() > 0) {
            try {
                long sleepTime = Convert.timeToMiliseconds(postponeUntil) - System.currentTimeMillis();
                if (sleepTime > 0)
                    Thread.sleep(sleepTime);
            } catch (NumberFormatException e) {
                out.println(e.getMessage() + " for postponeUntil parameter of '" + actionName + "'");
                return false;
            } catch (InterruptedException e) {
                out.println("Thread interrupted while waiting for posponed execution");
            }
        }

        // Prepare array for arguments
        List<String> arguments = new ArrayList<String>();

        // First argument is the method name
        String arg = props.getProperty(actionName, actionName);
        do {
            // Add var-substituted arg to arguments list
            arguments.add(Convert.substituteVariables(arg, variablePattern, 1, 2, variables));

            // Read next property with name <actionName>.param.{1,2,3,4,...}
            arg = props.getProperty(actionName + ".param." + Integer.toString(arguments.size()));
        } while (arg != null);

        // Store the method name in a separate variable to speed things up
        String methodName = Convert.substituteVariables(arguments.get(0), variablePattern, 1, 2, variables);

        // SPECIAL! For objectStreamOpen/namedInstanceAdd method a third/second parameter is automatically added from action name
        if ((methodName.equals("objectStreamOpen") && arguments.size() == 3) || (methodName.equals("namedInstanceAdd") && arguments.size() == 2))
            arguments.add(actionName);

        // Read description
        String description = props.getProperty(actionName + ".description");

        // Read outputFile parameter and set output to correct stream (if parameter outputFile was specified, file is opened, otherwise the default 'out' is used)
        PrintStream outputStream;
        try {
            String fileName = Convert.substituteVariables(props.getProperty(actionName + ".outputFile"), variablePattern, 1, 2, variables);
            if (fileName != null && fileName.length() > 0) {
                outputStream = outputStreams.get(fileName);
                if (outputStream == null) // Output stream not opened yet
                    outputStreams.put(fileName, outputStream = new PrintStream(fileName));
            } else {
                // Default output stream is the current output stream
                outputStream = out;
            }
        } catch (FileNotFoundException e) {
            out.println("Wrong outputFile for action '" + actionName + "': " + e.getMessage());
            return false;
        }

        // Read assign parameter
        String assignVariable = Convert.substituteVariables(props.getProperty(actionName + ".assign"), variablePattern, 1, 2, variables);
        ByteArrayOutputStream assignOutput;
        if (assignVariable != null) {
            assignOutput = new ByteArrayOutputStream();
            outputStream = new PrintStream(assignOutput);
        } else {
            assignOutput = null;
        }

        // Read number of repeats of this method
        int repeat;
        try {
            repeat = Integer.valueOf(Convert.substituteVariables(props.getProperty(actionName + ".repeat", "1"), variablePattern, 1, 2, variables));
        } catch (NumberFormatException e) {
            out.println("Number of repeats specified in action '" + actionName + "' is not a valid integer");
            return false;
        }

        // Read foreach parameter of this method
        String foreach = Convert.substituteVariables(props.getProperty(actionName + ".foreach"), variablePattern, 1, 2, variables);

        // Parse foreach values
        String[] foreachValues;
        if (foreach != null) {
            if (repeat > 1)
                out.println("WARNING: Action '" + actionName + "' has both the 'repeat' and the 'foreach' parameters defined. Using foreach.");
            foreachValues = foreach.trim().split("[ \t]+");
            repeat = foreachValues.length;
        } else foreachValues = null;

        // Execute action
        try {
            for (int i = 0; i < repeat; i++) {
                // Add foreach value
                if (foreachValues != null)
                    variables.put(actionName, foreachValues[i]);

                // Show description if set
                if (description != null)
                    outputStream.println(Convert.substituteVariables(description, variablePattern, 1, 2, variables));

                // Perform action
                if (methodName.indexOf(' ') != -1) {
                    // Special "block" method 
                    for (String blockActionName : methodName.split("[ \t]+"))
                        if (!controlFileExecuteAction(outputStream, props, blockActionName, variables, outputStreams))
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
                        if (!controlFileExecuteAction(outputStream, props, methodName, variables, outputStreams))
                            return false;
                }
            }

            // Remove foreach value
            if (foreachValues != null)
                variables.remove(actionName);

            // Assign variable is requested
            if (assignVariable != null && assignOutput != null)
                variables.put(assignVariable, assignOutput.toString().trim());

            return true; // Execution successful
        } catch (InvocationTargetException e) {
            logException(e.getCause());
            out.println(e.getCause());
            out.println(e.getMessage());
        }

        return false; // Execution unsuccessful - exception was printed
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
        boolean rtv = controlFileExecuteAction(out, props, action, variables, outputStreams);
        
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

        // Show prompt
        out.print("MESSIF >>> ");
        out.flush();

        // Read lines from the socket
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            // Execute method with the specified name and the provide the array of arguments
            String[] arguments = removeBackspaces(line.trim()).split("[ \t]+");

            // Handle close command
            if (arguments[0].equalsIgnoreCase("close") || Thread.currentThread().isInterrupted())
                break;

            // Handle normal method
            try {
                // Get appropriate method
                methodExecutor.execute(out, arguments);
            } catch (InvocationTargetException e) {
                out.println(e.getCause().toString());
            } catch (NoSuchMethodException e) {
                out.println("Unknown command: " + arguments[0]);
                out.println("Use 'help' to see all available commands");
            }

            // Show prompt
            out.print("MESSIF >>> ");
            out.flush();
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
            log.warning("Can't open telnet interface: " + e.toString());
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
                            connection.close();
                        } catch (ClosedByInterruptException e) {
                            // Ignore this exception because it is a correct exit
                        } catch (IOException e) {
                            log.warning(e.toString());
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
