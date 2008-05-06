/*
 * NodeRemovedException.java
 *
 * Created on 11. duben 2007, 10:37
 */

package messif.loadbalancing;

import messif.algorithms.AlgorithmMethodException;

/**
 * This exception is thrown if a node is removed (migrated or deleted) during a query processing at the node.
 * It is thrown only when this peer is the initiator of the query.
 *
 * @author David Novak, FI Masaryk University, Brno, Czech Republic; <a href="mailto:xnovak8@fi.muni.cz">xnovak8@fi.muni.cz</a>
 */
public class NodeRemovedException extends AlgorithmMethodException {

    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of <code>NodeRemovedException</code> without detail message.
     */
    public NodeRemovedException() {
        super("The node was removed while this query was processed.");
    }
    
    
    /**
     * Constructs an instance of <code>NodeRemovedException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public NodeRemovedException(String msg) {
        super(msg);
    }
    
    /**
     * Constructs an instance of <code>NodeRemovedException</code> with the specified detail message.
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method). (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     */
    public NodeRemovedException(Throwable cause) {
        super(cause);
    }
    
}
