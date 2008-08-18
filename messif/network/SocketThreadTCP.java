/*
 * SocketThreadUDP.java
 *
 * Created on 24. duben 2004, 18:04
 */

package messif.network;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import messif.utility.Logger;


/**
 *
 * @author  xbatko
 */
public class SocketThreadTCP extends Thread {
    
    /** Logger */
    protected static Logger log = Logger.getLoggerEx("messif.network");
    
    /****************** Data ******************/
    protected static final String NAME = "SocketThread";
    
    protected final MessageDispatcher messageDispatcher;
    protected final ServerSocket socket;
    
    /****************** Constructors ******************/
    
    /**
     * Creates a new instance of SocketThreadTCP
     */
    public SocketThreadTCP(ServerSocket socket, MessageDispatcher messageDispatcher) {
        super(NAME);
        this.messageDispatcher = messageDispatcher;
        this.socket = socket;
    }
    
    
    /****************** Message receiving methods for sockets ******************/
    
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
                log.severe(e);
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
                log.severe(e);
            }
    }
    
}
