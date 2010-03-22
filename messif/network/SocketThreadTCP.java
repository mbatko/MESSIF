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

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;


/**
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class SocketThreadTCP extends Thread {
        
    //****************** Data ******************//
    protected static final String NAME = "SocketThread";
    
    protected final MessageDispatcher messageDispatcher;
    protected final ServerSocket socket;
    
    //****************** Constructors ******************//
    
    /**
     * Creates a new instance of SocketThreadTCP
     */
    public SocketThreadTCP(ServerSocket socket, MessageDispatcher messageDispatcher) {
        super(NAME);
        this.messageDispatcher = messageDispatcher;
        this.socket = socket;
    }
    
    
    //****************** Message receiving methods for sockets ******************//
    
    protected class TCPStreamReceiver extends Thread {
        protected final ObjectInputStream in;
        
        public TCPStreamReceiver(Socket connection) throws IOException {
            super(NAME);
            in = new ObjectInputStream(connection.getInputStream());
        }
        
        public void run() {
            try {
                for (;;)
                    messageDispatcher.receiveMessage(messageDispatcher.getMessageFromStream(in));
            } catch (EOFException ignore) {
                try { in.close(); } catch (IOException ignore2) {};
            } catch (Exception e) {
                MessageDispatcher.log.log(Level.SEVERE, e.getClass().toString(), e);
                try { in.close(); } catch (IOException ignore) {};
            }
        }
    }
    
    public void run() {
        for (;;)
            try {
                // Get next connection from socket
                new TCPStreamReceiver(socket.accept()).start();
            } catch (IOException e) {
                // If socket was closed, terminate
                if ((socket != null) && socket.isClosed()) break;
                
                // Show error and continue otherwise
                MessageDispatcher.log.log(Level.SEVERE, e.getClass().toString(), e);
            }
    }
    
}
