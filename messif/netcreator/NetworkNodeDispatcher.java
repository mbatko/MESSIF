/*
 * NetworkNodeCreator.java
 *
 * Created on 1. kveten 2004, 18:09
 */

package messif.netcreator;

import java.io.Serializable;
import messif.network.MessageDispatcher;
import messif.network.NetworkNode;
import messif.utility.Logger;

/**
 *
 * @author  xbatko
 */
public abstract class NetworkNodeDispatcher implements Startable, Serializable {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;    

    /** Logger */
    protected static Logger log = Logger.getLoggerEx("netnode.creator");
    
    /****************** Registered startables ******************/
    protected final Startable[] startables;
    
    
    /****************** Internal variables ******************/
    protected transient MessageDispatcher messageDisp;


    /****************** Constructors ******************/

    /** Creates a new instance of NetworkNodeCreator */
    public NetworkNodeDispatcher(MessageDispatcher messageDisp, Startable[] startables) {
        // Set startables
        this.startables = startables;
        
        // Set message dispatcher
        this.messageDisp = messageDisp;
    }


    /****************** Deserialization methods ******************/

    /** Setter of message dispatcher for deserialization method */
    public void setMessageDispatcher(MessageDispatcher messageDisp) {
        this.messageDisp = messageDisp;
    }


    /****************** Create methods ******************/

    public abstract NetworkNode create() throws InstantiationException;
    
    
    /****************** Startable implementation ******************/

    /** Method "start" from interface Startable. Simply starts all registered startables */
    public void start() throws CantStartException {
        // Start all supplied startables
        if (startables != null)
            for (int i = 0; i < startables.length; i++) 
                startables[i].start();
    }
    
}
