/*
 * FilterRejectException.java
 *
 * Created on 24. duben 2004, 12:11
 */

package messif.buckets;

/**
 * Thrown to indicate that the bucket filter rejects current operation.
 *
 * @author  xbatko
 */
public class FilterRejectException extends RuntimeException {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;    

    /**
     * Creates a new instance of <code>FilterRejectException</code> without detail message.
     */
    public FilterRejectException() {
    }
    
    
    /**
     * Constructs an instance of <code>FilterRejectException</code> with the specified detail message.
     * @param msg the detail message
     */
    public FilterRejectException(String msg) {
        super(msg);
    }
}
