/*
 * MessageStartStopBalancing.java
 *
 * Created on September 26, 2006, 9:44
 */

package messif.loadbalancing;

import messif.network.Message;

/**
 * This message is used to start and stop the load-balancing process
 * usually in all hosts at one moment (broadcast).
 *
 * @author <a href="mailto:xnovak8@fi.muni.cz">xnovak8@fi.muni.cz</a> David Novak, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
public class MessageStartStopBalancing extends Message {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 2L;    
    
    /** Flag saying whether to stop or start the load-balancing process */
    final boolean start;

    /** Flag saying whether to clear all statistics or not (default: false) */
    final boolean clearStatistics;
    
    /** Creates a new instance of MessageStartStopBalancing */
    public MessageStartStopBalancing(boolean start) {
        this.start = start;
        this.clearStatistics = false;
    }
    
    /** Creates a new instance of MessageStartStopBalancing */
    public MessageStartStopBalancing(boolean start, boolean clearStatistics) {
        this.start = start;
        this.clearStatistics = clearStatistics;
    }
    
}
