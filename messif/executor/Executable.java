/*
 * Executable.java
 *
 */

package messif.executor;

import java.lang.reflect.InvocationTargetException;

/**
 * This class represents an executable code.
 * It is mainly intended to be used in execution using {@link MethodExecutor}.
 *
 * @see MethodExecutor
 * @see SingleMethodExecutor
 *
 * @author xbatko
 */
public interface Executable {
    /**
     * Execute the executable code represented by this class.
     * 
     * @throws NoSuchMethodException if there is no code to execute
     * @throws InvocationTargetException if there was an exception during executing the code
     */
    public void execute() throws NoSuchMethodException, InvocationTargetException;
}
