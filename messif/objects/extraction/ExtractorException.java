/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package messif.objects.extraction;

/**
 * Throwable that indicates an error during extraction.
 * 
 * @author xbatko
 */
public class ExtractorException extends Exception {
    /** Serial version for {@link java.io.Serializable} */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of <code>ExtractorException</code> without detail message.
     */
    public ExtractorException() {
    }

    /**
     * Constructs an instance of <code>ExtractorException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public ExtractorException(String msg) {
        super(msg);
    }

    /**
     * Constructs an instance of <code>ExtractorException</code> with the
     * specified detail message and cause.
     * @param msg the detail message.
     * @param cause the cause which is saved for later retrieval by the
     *         {@link #getCause()} method); a <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown
     */
    public ExtractorException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
