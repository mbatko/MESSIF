/*
 * SocketThreadUDP.java
 *
 * Created on 24. duben 2004, 18:04
 */

package messif.network;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import messif.utility.Logger;


/**
 *
 * @author  xbatko
 */
public class SocketThreadUDP extends Thread {
    
    /** Logger */
    protected static Logger log = Logger.getLoggerEx("messif.network");
    
    /****************** Data ******************/
    protected static final int MAX_UDP_LENGTH = 1450/*63000*/;
    protected static final String NAME = "SocketThread";
    
    protected final MessageDispatcher messageDispatcher;
    protected final DatagramSocket socket;
    
    /****************** Constructors ******************/
    
    /**
     * Creates a new instance of SocketThreadUDP 
     */
    public SocketThreadUDP(DatagramSocket socket, MessageDispatcher messageDispatcher) {
        super(NAME);
        this.messageDispatcher = messageDispatcher;
        this.socket = socket;
    }    

    /****************** Message receiving methods for sockets ******************/
        
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
                log.severe(e);
            }
    }
    
}
