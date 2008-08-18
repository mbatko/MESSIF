/*
 * AlgorithmMethodException.java
 *
 * Created on 5. kveten 2003, 23:53
 */

package messif.algorithms;

/**
 *
 * @author  xbatko
 */
public class AlgorithmMethodException extends Exception {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;    

    /**
     * Creates a new instance of <code>AlgorithmMethodException</code> without detail message.
     */
    public AlgorithmMethodException() {
        super("Algorithm method failed");
        
        //getStackTrace()[0].getMethodName());
    }
    
    
    /**
     * Constructs an instance of <code>AlgorithmMethodException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public AlgorithmMethodException(String msg) {
        super(msg);
    }
    
    /**
     * Constructs an instance of <code>AlgorithmMethodException</code> with the specified detail message.
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method). (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     */
    public AlgorithmMethodException(Throwable cause) {
        super(cause);
    }
}
