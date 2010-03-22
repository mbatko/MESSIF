/*
 *  This file is part of MESSIF library.
 *
 *  MESSIF library is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MESSIF library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MESSIF library.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.netcreator;

import java.io.Serializable;
import java.util.logging.Logger;
import messif.network.MessageDispatcher;
import messif.network.NetworkNode;

/**
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class NetworkNodeDispatcher implements Startable, Serializable {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;    

    /** Logger */
    protected static Logger log = Logger.getLogger(NetworkNodeDispatcher.class.getName());

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
