/*
 * LBSettings.java
 *
 * Created on 7. listopad 2006, 9:32
 */

package messif.loadbalancing;

import messif.utility.Logger;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Encapsulates the load-balancing settings stored in a load-balancing.properties file
 *
 * @author xnovak8
 */
public class LBSettings {
    
    /****************************************    Constants   *******************************/
    /** load DONT_KNOW constant */
    public static final int LOAD_DONT_KNOW = Integer.MAX_VALUE;
    
    
    /****************************************    Settings     *******************************/
    
    /** Load balancing atempt period */
    public static long BALANCING_DELTA_T = 3000;
    /** Number of balancing attempts to really do the balancing action */
    public static int OVERLOAD_RECHECKS = 1;
    /** Size of the time window to measure the busy load over */
    public static long BUSY_LOAD_WINDOW = 30000;
    /** # Number of queries to measure the single-load over */
    public static int SINGLE_LOAD_AVG = 10;
    /** Time period to send a gossip message if there is no traffic */
    public static long GOSSIP_T = 3000;
    /** Size of the HostList interchanged by the hosts */
    public static int PEER_LIST_SIZE = 5;
    /** Do not take any balancing action if the load is smaller then a certain value */
    public static double MIN_BUSY_LOAD = 10;
    /** Do not take any balancing action if the load is smaller then a certain value */
    public static double MIN_SINGLE_LOAD = 10;
    
    
    /*******************************    Load/store the settings   ****************************/
    
    /** Load the settings from default file. @return true if everything ok */
    protected static boolean loadSettings() {
        return loadSettings(LBSettings.class.getClassLoader().getResource("messif/loadbalancing/load-balancing.properties"));
    }
    /** Load the settings from given file. @return true if everything ok */
    protected static boolean loadSettings(URL url) {
        InputStream stream = null;
        try {
            stream = url.openStream();
            Properties props = new Properties();
            props.load(stream);
            
            // parse the settings now
            BALANCING_DELTA_T = Long.valueOf(props.getProperty("BalancingDeltaT"));
            OVERLOAD_RECHECKS = Integer.valueOf(props.getProperty("OverloadRechecks"));
            BUSY_LOAD_WINDOW = Integer.valueOf(props.getProperty("BusyLoadWindowMilis"));
            SINGLE_LOAD_AVG = Integer.valueOf(props.getProperty("SingleLoadAverage"));
            GOSSIP_T = Long.valueOf(props.getProperty("GossipT"));
            PEER_LIST_SIZE = Integer.valueOf(props.getProperty("PeerListSize"));
            MIN_BUSY_LOAD = Double.valueOf(props.getProperty("MinBusyLoad"));
            MIN_SINGLE_LOAD = Double.valueOf(props.getProperty("MinSingleLoad"));
            
            stream.close();
        } catch (FileNotFoundException ex) {
            Logger.getLoggerEx("Host").warning(ex.getMessage());
            return false;
        } catch (IOException ex) {
            Logger.getLoggerEx("Host").warning(ex.getMessage());
            return false;
        } catch (NumberFormatException ex) {
            Logger.getLoggerEx("Host").warning(ex.getMessage());
            return false;
        } catch (NullPointerException ex) {
            Logger.getLoggerEx("Host").warning(ex.getMessage());
            return false;
        } finally {
            try {
                if (stream != null)
                    stream.close();
            } catch (IOException ex) {
                Logger.getLoggerEx("Host").warning(ex.getMessage());
                return false;
            }
        }
        return true;
    }
}
