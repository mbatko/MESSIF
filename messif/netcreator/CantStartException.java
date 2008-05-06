/*
 * CantStartException.java
 *
 * Created on 6. kveten 2003, 11:08
 */

package messif.netcreator;

/**
 *
 * @author  xbatko
 */
public class CantStartException extends Exception {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;    

    /**
     * Creates a new instance of <code>AlgorithmCantStartException</code> without detail message.
     */
    public CantStartException() {
    }
    
    
    /**
     * Constructs an instance of <code>AlgorithmCantStartException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public CantStartException(String msg) {
        super(msg);
    }
}
