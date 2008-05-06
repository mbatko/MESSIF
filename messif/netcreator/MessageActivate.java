/*
 * MessageInitUse.java
 *
 * Created on 4. kveten 2003, 12:05
 */

package messif.netcreator;

import java.util.Set;
import messif.network.Message;
import messif.network.NetworkNode;

/**
 *
 * @author  xbatko
 */
public class MessageActivate extends Message {

    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    /****************** Message extensions ******************/
    protected Set<NetworkNode> netnodes;
    public int getCount() {
        return (netnodes != null)?netnodes.size():-1;
    }
    public Set<NetworkNode> getServerList() {
        return netnodes;
    }
    
    /****************** Constructors ******************/
    
    /** Creates a new instance of MessageInitUse from supplied data */
    public MessageActivate(Set<NetworkNode> netnodes) {
        super();
        
        this.netnodes = netnodes;
    }
    
}
