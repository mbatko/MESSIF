/*
 * Startable.java
 *
 * Created on 8. kveten 2003, 16:09
 */

package messif.netcreator;

/**
 *
 * @author  xbatko
 */
public interface Startable {
    
    /** Start server (i.e. listening mode) - requested from other node */
    public void start() throws CantStartException;
}
