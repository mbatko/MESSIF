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
package messif.network;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.logging.Level;


/**
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class SocketThreadUDP extends Thread {

    //****************** Data ******************//
    protected static final int MAX_UDP_LENGTH = 63000;
    protected static final String NAME = "SocketThread";

    protected final MessageDispatcher messageDispatcher;
    protected final DatagramSocket socket;


    //****************** Constructors ******************//
    
    /**
     * Creates a new instance of SocketThreadUDP 
     */
    public SocketThreadUDP(DatagramSocket socket, MessageDispatcher messageDispatcher) {
        super(NAME);
        this.messageDispatcher = messageDispatcher;
        this.socket = socket;
    }    


    //****************** Message receiving methods for sockets ******************//

    @Override
    public void run() {
        for (;;) 
            try {
                // Prepare packet receiver
                byte[] buf = new byte[MAX_UDP_LENGTH + 1]; // Maximal length of UDP packet...
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                // Receive packet
                socket.receive(packet);

                // Create input stream
                ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(packet.getData()));

                // Try to construct message from data
                messageDispatcher.receiveMessage(messageDispatcher.getMessageFromStream(in));

                in.close();
            } catch (IOException e) {
                // If socket was closed, terminate
                if ((socket != null) && socket.isClosed()) break;
                
                // Show error and continue otherwise
                MessageDispatcher.log.log(Level.SEVERE, e.getClass().toString(), e);
            }
    }

}
