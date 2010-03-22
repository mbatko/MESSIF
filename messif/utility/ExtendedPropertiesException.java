/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package messif.utility;

/**
 *
 * @author xbatko
 */
public class ExtendedPropertiesException extends RuntimeException {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new ExtendedPropertiesException with <code>null</code> as its
     * detail message. The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}.
     */
    public ExtendedPropertiesException() {
	super();
    }

    /**
     * Constructs a new ExtendedPropertiesException with the specified detailed message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detailed message
     */
    public ExtendedPropertiesException(String message) {
	super(message);
    }

    /**
     * Constructs a new ExtendedPropertiesException with the specified detailed message and
     * cause.
     *
     * @param message the detaile message
     * @param cause the cause for this exception
     */
    public ExtendedPropertiesException(String message, Throwable cause) {
        super(message, cause);
    }

}
