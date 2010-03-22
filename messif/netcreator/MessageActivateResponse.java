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

import messif.network.NetworkNode;
import messif.network.ReplyMessage;

/**
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class MessageActivateResponse extends ReplyMessage {

    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    /****************** Response constants ******************/
    public static final byte ACTIVATED = 0;
    public static final byte CANT_START = 1;
    public static final byte ALREADY_ACTIVE = 2;

    /****************** Message extensions ******************/
    protected byte response;
    public byte getResponse() {
        return response;
    }
    public boolean isSuccess() {
        return response == ACTIVATED;
    }

    protected final NetworkNode activatedNode;
    public NetworkNode getActivatedNode() {
        return activatedNode;
    }
    
    /****************** Constructors ******************/
        
    /** Creates a new instance of MessageActivateResponse from supplied data */
    public MessageActivateResponse(byte response, MessageActivate activationMessage) {
        super(activationMessage);
        
        this.response = response;
        this.activatedNode = null;
    }

    /** Creates a new instance of MessageActivateResponse from supplied data */
    public MessageActivateResponse(NetworkNode activatedNode, MessageActivate activationMessage) {
        super(activationMessage);
        
        this.response = ACTIVATED;
        this.activatedNode = activatedNode;
    }
    
}
