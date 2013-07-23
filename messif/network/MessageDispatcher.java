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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The component responsible for sending and receiving messages.
 * This dispatcher registers to a TCP/UDP port (as specified in constructor)
 * and possibly also to a multicast group on a specified UDP port.
 * Using the local host IP address, the {@link NetworkNode}
 * identifier is created.
 *
 * <p>
 * After the initialization, messages can be sent to another host, where
 * the {@link MessageDispatcher} is running using {@link #sendMessage sendMessage} method.
 * This method sends a {@link Message} to the destination {@link MessageDispatcher}, addressed
 * by its {@link NetworkNode}.<br/>
 * If a multicast group has been initialized, it is also possible to send a message
 * to all participants, i.e. no destination is given.
 * <p>
 *
 * <p>
 * It is also possible to wait for a {@link ReplyMessage}, i.e. we expect another message to return
 * in response to a message sent by us. This can be done through {@link #sendMessageWaitReply sendMessageWaitReply}
 * method, which returns an instance of {@link ReplyReceiver} associated with the sent message - see its
 * documentation for detailed info on how to get the received reply messages.
 * </p>
 *
 * <p>
 * The waiting mechanism allows also more complex scenarios, when multiple hosts are involved:<br/>
 * <img src="messif-network-messaging.png" alt="MESSIF network messaging example" /><br/>
 * The method that waits for results ensures the completeness of the answer, even if the message was
 * sent to several network nodes that forward it (either replying or not) to another network nodes and even
 * if the message pathes contain cycles.
 * </p>
 *
 * <p>
 * On the other hand, the dispatcher allows to receive messages. The replies are received and passed to
 * an associated ReplyReceiver automatically. For normal messages, we can register a message {@link Receiver receiver}.
 * Once a message is received on any socket, it is passed to every registered receiver through its
 * {@link Receiver#acceptMessage acceptMessage} until <tt>true</tt> is returned. Thus the first
 * receiver, that accepts the message stops the traversal. If there is no receiver that accepts the provided
 * message, a warning is logged.
 * </p>
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 * @see NetworkNode
 * @see Message
 * @see ReplyMessage
 * @see ReplyReceiver
 */
public class MessageDispatcher implements Receiver, Serializable {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Constants ******************//

    /** Multicast group IP address constant */
    protected static final InetAddress BROADCAST_GROUP = InitBroadcastGroup();

    /**
     * Returns a broadcast group address for all message dispatchers.
     * @return a broadcast group address
     */
    private static InetAddress InitBroadcastGroup() { // initializer
        try { return InetAddress.getByName("230.0.0.1"); } catch (UnknownHostException e) { return null; }
    }

    /** Logger */
    protected static final Logger log = Logger.getLogger("messif.network");

    /**
     * The size of the TCP connection pool.
     * Represents the number of simultaneous TCP connections that are kept open for sending messages.
     */
    private static final int tcpConnectionPoolSize = 100;


    //****************** Receivers ******************//

    /** List of currently registered receivers */
    private final List<Receiver> receivers;

    /** List of currently registered reply receivers */
    private final ReplyReceiverList replyReceivers;


    //****************** Communication sockets ******************//

    /** UDP socket for communication */
    protected final DatagramSocket udpSocket;

    /** TCP socket for communication */
    protected final ServerSocket tcpSocket;

    /** The pool of opened TCP connections */
    private final transient Map<NetworkNode, ObjectOutputStream> tcpConnectionPool;

    /**
     * UDP multicast socket for broadcast communication
     * It is <tt>null</tt> if broadcast is not initialized.
     */
    protected final MulticastSocket broadcastSocket;

    /** Identification of this network node */
    protected final NetworkNode ourNetworkNode;

    /** Top most message dispatcher in the hierarchy */
    protected final MessageDispatcher topMessageDispatcher;

    /**
     * Returns the broadcast port number if created or zero if broadcast is not initialized by this dispatcher.
     * @return the broadcast port number if created or zero if broadcast is not initialized by this dispatcher
     */
    public final int getBroadcastPort() {
        return (broadcastSocket == null)?0:broadcastSocket.getLocalPort();
    }

    /**
     * Returns the network node identification of this message dispatcher.
     * @return the network node identification of this message dispatcher
     */
    public final NetworkNode getNetworkNode() {
        return ourNetworkNode;
    }


    //****************** Constructors ******************//

    /**
     * Creates a new instance of MessageDispatcher with automatically assigned port (decided by OS).
     * Broadcast is disabled.
     * @throws IOException if there was error when opening communication sockets
     */
    public MessageDispatcher() throws IOException {
        this(0); // Open first available port
    }

    /**
     * Creates a new instance of MessageDispatcher with specified TCP/UDP port.
     * Broadcast is disabled.
     * @param port the TCP/UDP port used for communication.
     * @throws IOException if there was error when opening communication sockets
     */
    public MessageDispatcher(int port) throws IOException {
        this(port, 0); // Do not start broadcast socket by default
    }

    /**
     * Creates a new instance of MessageDispatcher with specified TCP/UDP and broadcast ports.
     * @param port the TCP/UDP port used for communication
     * @param broadcastPort the UDP port used for sending and receiving broadcasts
     * @throws IOException if there was error when opening communication sockets
     */
    public MessageDispatcher(int port, int broadcastPort) throws IOException {
        this(new NetworkNode(InetAddress.getLocalHost(), port), broadcastPort);
    }

    /**
     * Creates a new instance of MessageDispatcher with specified TCP/UDP port.
     * Broadcast is disabled.
     * @param localHost the local IP address to bind the communication to
     * @param port the TCP/UDP port used for communication
     * @throws IOException if there was error when opening communication sockets
     */
    public MessageDispatcher(String localHost, int port) throws IOException {
        this(localHost, port, 0);
    }

    /**
     * Creates a new instance of MessageDispatcher with specified TCP/UDP and broadcast ports.
     * @param localHost the local IP address to bind the communication to
     * @param port the TCP/UDP port used for communication
     * @param broadcastPort the UDP port used for sending and receiving broadcasts
     * @throws IOException if there was error when opening communication sockets
     */
    public MessageDispatcher(String localHost, int port, int broadcastPort) throws IOException {
        this(new NetworkNode(localHost, port), broadcastPort);
    }

    /**
     * Creates a new instance of MessageDispatcher with specified TCP/UDP port.
     * Broadcast is disabled.
     * @param localAddress local address to bind the TCP/UDP communication to
     * @throws IOException if there was error when opening communication sockets
     */
    public MessageDispatcher(NetworkNode localAddress) throws IOException {
        this(localAddress, 0);
    }

    /**
     * Creates a new instance of MessageDispatcher with specified TCP/UDP and broadcast ports.
     * @param localAddress local address to bind the TCP/UDP communication to
     * @param broadcastPort the UDP port used for sending and receiving broadcasts
     * @throws IOException if there was error when opening communication sockets
     */
    public MessageDispatcher(NetworkNode localAddress, int broadcastPort) throws IOException {
        // Set network node info
        ourNetworkNode = localAddress;
        
        // Create server TCP socket endpoint
        tcpSocket = new ServerSocket(localAddress.getPort(), 0, localAddress.getHost());
        
        // Create server UDP socket endpoint
        udpSocket = new DatagramSocket(tcpSocket.getLocalPort(), tcpSocket.getInetAddress());
        
        // Initialize tcp connection pool
        tcpConnectionPool = Collections.synchronizedMap(new HashMap<NetworkNode, ObjectOutputStream>(tcpConnectionPoolSize));
        
        // Start socket threads
        new SocketThreadTCP(tcpSocket, this).start();
        new SocketThreadUDP(udpSocket, this).start();
        
        if (broadcastPort > 0) {
            // Create broadcat socket and join it to the broadcast group
            broadcastSocket = new MulticastSocket(broadcastPort);
            broadcastSocket.joinGroup(BROADCAST_GROUP);
            
            // Start broadcast socket thread
            new SocketThreadUDP(broadcastSocket, this).start();
        } else broadcastSocket = null;
        
        // Create receiver lists
        replyReceivers = new ReplyReceiverList();
        receivers = new ArrayList<Receiver>();
        registerReceiver(replyReceivers);
        
        // This constructor is for topmost message dispatcher
        topMessageDispatcher = this;
    }

    /**
     * Creates a new server instance of MessageDispatcher connected to a higher level dispatcher.
     * @param parentDispatcher the higher level dispatcher this instance is connected to
     * @param nodeID the sub-identification of this dispatcher for the higher level
     */
    public MessageDispatcher(MessageDispatcher parentDispatcher, int nodeID) {
        // Use sockets of higher level (no receiving, used only for sending)
        this.tcpSocket = parentDispatcher.tcpSocket;
        this.udpSocket = parentDispatcher.udpSocket;
        this.broadcastSocket = parentDispatcher.broadcastSocket;
        this.topMessageDispatcher = parentDispatcher.topMessageDispatcher;
        
        // Share tcp connection pool among dispatchers
        this.tcpConnectionPool = parentDispatcher.tcpConnectionPool;
        
        // Create subdispatcher network node
        ourNetworkNode = new NetworkNode(parentDispatcher.ourNetworkNode, nodeID);
        
        // Create receiver lists
        replyReceivers = new ReplyReceiverList();
        receivers = new ArrayList<Receiver>();
        registerReceiver(replyReceivers);
        
        // Register this dispatcher to the parent's receiver list
        parentDispatcher.registerReceiver(this);
    }

    /**
     * Close all opened communication sockets and disable this dispatcher.
     * This dispatcher stops listening for incoming messages.
     * Any attempt to send a message will fail and throw an IO exception.
     *
     * @throws IOException if the sockets are no longer opened or another error occurs at OS level
     */
    public void closeSockets() throws IOException {
        if (topMessageDispatcher.equals(this)) {
            udpSocket.close();
            tcpSocket.close();
            if (broadcastSocket != null) broadcastSocket.close();
        } else
            topMessageDispatcher.deregisterReceiver(this);
    }


    //****************** Serialization ******************//

    /**
     * Returns an alternative object to be used when serializing MessageDispatcher.
     * During its deserialization the correct full MessageDispatcher is recreated.
     * @return an alternative object to MessageDispatcher
     */
    private Object writeReplace() {
        return new Serialized(ourNetworkNode, getBroadcastPort());
    }

    /** Wrapper class used when serializing MessageDispatcher */
    private static class Serialized extends NetworkNode {
        /** class serial id for serialization */     
        private static final long serialVersionUID = 1L;

        /** Serialized broadcast port */
        private final int broadcastPort;

        /**
         * Creates a new instance of Serialized message dispatcher.
         * @param dispatcherNetworkNode the network node of the serialized message dispatcher
         * @param broadcastPort the broadcast port of the serialized message dispatcher
         */
        private Serialized(NetworkNode dispatcherNetworkNode, int broadcastPort) {
            super(dispatcherNetworkNode);
            this.broadcastPort = broadcastPort;
        }

        /**
         * Deserialization method that replaces this object with a fully functional
         * {@link MessageDispatcher}.
         * @return a new instance of {@link MessageDispatcher} based on the serialized data
         * @throws ObjectStreamException if there was an error reading the serialized data or creating a new {@link MessageDispatcher}
         */
        private Object readResolve() throws ObjectStreamException {
            try {
                // Sanity check for the localhost (correctness of the remap file)
                if (!InetAddress.getLocalHost().equals(host))
                    throw new InvalidObjectException("Host loaded for message dispatcher " + host + " differs from the localhost " + InetAddress.getLocalHost() + " (wrong remap file?)");
                if (hasNodeID()) {
                    // Get or create parent message dispatcher
                    MessageDispatcher parent = messageDispMappingTable.get(port);
                    if (parent == null) {
                        parent = new MessageDispatcher(port, broadcastPort);
                        messageDispMappingTable.put(port, parent);
                    }
                    return new MessageDispatcher(parent, nodeID);
                } else {
                    return new MessageDispatcher(port, broadcastPort);
                }
            } catch (InvalidObjectException e) {
                throw e;
            } catch (IOException e) {
                throw new InvalidObjectException(e.toString());
            } catch (NullPointerException e) {
                throw new InvalidObjectException("Cannot deserialize message dispatcher with nodeIDs without mapper");
            }
        }
    }


    //****************** Send methods ******************//

    /**
     * Send the message to the specified network node.
     * Note that a particular message can be sent several times (to different nodes).
     *
     * @param msg the message to send
     * @param node the destination network node
     * @throws IOException if the communication failed, e.g. the destination node cannot be reached, etc.
     */
    public void sendMessage(Message msg, NetworkNode node) throws IOException {
        sendMessage(msg, node, false);
    }

    /**
     * Send the message to multiple network nodes.
     *
     * @param msg the message to send
     * @param nodes the list of destination network nodes
     * @throws IOException if the communication failed, e.g. the destination node cannot be reached, etc.
     */
    public void sendMessage(Message msg, Collection<NetworkNode> nodes) throws IOException {
        sendMessage(msg, nodes, false);
    }

    /**
     * Send the message to the specified network node.
     * The flag <tt>willNotReply</tt> allows to inform that this node will not send any replies
     * for the message. It is only important if somebody is waiting for the message's replies.
     *
     * Note that a particular message can be sent several times (to different nodes).
     * However, if a message is sent with <tt>willNotReply</tt> flag set to true, that particular
     * message cannot be sent anymore.
     *
     * @param msg the message to send
     * @param node the destination network node
     * @param willNotReply if this flag is <tt>true</tt>, this node will not send any replies
     * @throws IOException if the communication failed, e.g. the destination node cannot be reached, etc.
     */
    public void sendMessage(Message msg, NetworkNode node, boolean willNotReply) throws IOException {
        // Update navigation path
        msg.addNotWaitingDestination(getNetworkNode(), node, willNotReply);
        send(msg, node);
    }

    /**
     * Send the message to multiple network nodes.
     * The flag <tt>willNotReply</tt> allows to inform that this node will not send any replies
     * for the message. It is only important if somebody is waiting for the message's replies.
     *
     * Note that a particular message can be sent several times (to different nodes).
     * However, if a message is sent with <tt>willNotReply</tt> flag set to true, that particular
     * message cannot be sent anymore.
     *
     * @param msg the message to send
     * @param nodes the list of destination network nodes
     * @param willNotReply if this flag is <tt>true</tt>, this node will not send any replies
     * @throws IOException if the communication failed, e.g. the destination node cannot be reached, etc.
     */
    public void sendMessage(Message msg, Collection<NetworkNode> nodes, boolean willNotReply) throws IOException {
        Iterator<NetworkNode> iterator = nodes.iterator();
        while (iterator.hasNext())
            sendMessage(msg, iterator.next(), willNotReply && !iterator.hasNext()); // If "willNotReply" is requested, it is sent only for the last node
    }

    /**
     * Send the message to all network nodes (that are listening for broadcast on the same port).
     * Warning: there is no guarantee that the message reached all the nodes.
     *
     * @param msg the message to send
     * @throws IOException if the communication failed, e.g. the broadcast is not initialized
     */
    public void sendMessage(Message msg) throws IOException {
        // Send method
        msg.getActualNavigationElement().setSender(getNetworkNode());
        send(msg);
    }

    /**
     * Send the reply message, i.e. send the message to its original sender node.
     *
     * @param msg the reply message to send
     * @return the node the reply has been sent to
     * @throws IOException if the communication failed, e.g. the broadcast is not initialized
     */
    public NetworkNode replyMessage(ReplyMessage msg) throws IOException {
        // Get reply destination node and update navigation path
        NetworkNode replyNode = msg.setReply(getNetworkNode());
        if (replyNode != null)
            send(msg, replyNode);
        return replyNode;
    }

    /**
     * Send the message to multiple network nodes and wait for the replies.
     * A reply is expected from each of these nodes.
     *
     * <p>
     * The returned <tt>ReplyReceiver</tt> can be used to block until all replies are gathered or to control
     * the process of waiting. It is also used to retrieve the list of received replies (instances of {@link ReplyMessage}).
     * Note that the receiver is registered automatically and unregistered when all the expected reply messages
     * arrive. The removal can be postponed until the {@link ReplyReceiver#getReplies} is called
     * (the <code>removeOnAccept</code> is <tt>false</tt>), which is suitable if this method
     * is called several times to send a message to multiple nodes. In that case, the same
     * receiver is returned from all the calls and can be used afterwards to gather all
     * reply messages.
     * </p>
     *
     * @param <E> the type of reply message to wait for
     * @param msg the message to send
     * @param replyMessageClass the reply messages class to wait for (other messages are ignored even if the message ID matches)
     * @param removeOnAccept flag that controls whether the returned receiver is removed
     *                       from the waiting list when the last message arrives (<tt>true</tt>)
     *                       or when the getReplies is called (<tt>false</tt>)
     * @param nodes the list of destination network nodes
     * @return the receiver used to gather all the replies
     * @throws IOException if the communication failed, e.g. the destination node cannot be reached, etc.
     */
    public <E extends ReplyMessage> ReplyReceiver<E> sendMessageWaitReply(Message msg, Class<E> replyMessageClass, boolean removeOnAccept, Collection<NetworkNode> nodes) throws IOException {
        synchronized (replyReceivers) {
            // Create reply receiver for the message
            ReplyReceiver<E> receiver = replyReceivers.createReplyReceiver(msg, replyMessageClass, removeOnAccept);

            for (NetworkNode node : nodes) {
                // Update navigation path
                msg.addWaitingDestination(getNetworkNode(), node);

                // Update receiver's waiting paths
                List<NetworkNode> path = msg.getSenderList();
                path.add(node);
                receiver.addWaitingPath(path);

                // Send message to destination
                send(msg, node);
            }

            // Return the receiver to allow results retrieval
            return receiver;
        }
    }

    /**
     * Send the message to the specified network node and wait for the replies.
     * Note that a particular message can be sent several times (to different nodes) using this method -
     * a reply is then expected from each of these nodes. The same instance of <tt>ReplyReceiver</tt> is returned
     * for the repeatedly sent message.
     *
     * <p>
     * The returned <tt>ReplyReceiver</tt> can be used to block until all replies are gathered or to control
     * the process of waiting. It is also used to retrieve the list of received replies (instances of {@link ReplyMessage}).
     * Note that the receiver is registered automatically and unregistered when all the expected reply messages
     * arrive. The removal can be postponed until the {@link ReplyReceiver#getReplies} is called
     * (the <code>removeOnAccept</code> is <tt>false</tt>), which is suitable if this method
     * is called several times to send a message to multiple nodes. In that case, the same
     * receiver is returned from all the calls and can be used afterwards to gather all
     * reply messages.
     * </p>
     *
     * @param <E> the type of reply message to wait for
     * @param msg the message to send
     * @param replyMessageClass the reply messages class to wait for (other messages are ignored even if the message ID matches)
     * @param removeOnAccept flag that controls whether the returned receiver is removed
     *                       from the waiting list when the last message arrives (<tt>true</tt>)
     *                       or when the getReplies is called (<tt>false</tt>)
     * @param node the destination network node
     * @return the receiver used to gather all the replies
     * @throws IOException if the communication failed, e.g. the destination node cannot be reached, etc.
     */
    public <E extends ReplyMessage> ReplyReceiver<E> sendMessageWaitReply(Message msg, Class<E> replyMessageClass, boolean removeOnAccept, NetworkNode node) throws IOException {
        return sendMessageWaitReply(msg, replyMessageClass, removeOnAccept, Collections.singleton(node));
    }

    /**
     * Send the message to multiple network nodes and wait for the replies.
     * A reply is expected from each of these nodes.
     *
     * <p>
     * The returned <tt>ReplyReceiver</tt> can be used to block until all replies are gathered or to control
     * the process of waiting. It is also used to retrieve the list of received replies (instances of {@link ReplyMessage}).
     * Note that the receiver is registered automatically and unregistered when all the expected reply messages
     * arrive and the {@link ReplyReceiver#getReplies} is called.
     * </p>
     *
     * @param <E> the type of reply message to wait for
     * @param msg the message to send
     * @param replyMessageClass the reply messages class to wait for (other messages are ignored even if the message ID matches)
     * @param nodes the list of destination network nodes
     * @return the receiver used to gather all the replies
     * @throws IOException if the communication failed, e.g. the destination node cannot be reached, etc.
     */
    public <E extends ReplyMessage> ReplyReceiver<E> sendMessageWaitReply(Message msg, Class<E> replyMessageClass, Collection<NetworkNode> nodes) throws IOException {
        return sendMessageWaitReply(msg, replyMessageClass, false, nodes);
    }

    /**
     * Send the message to the specified network node and wait for the replies.
     * Note that a particular message can be sent several times (to different nodes) using this method -
     * a reply is then expected from each of these nodes. The same instance of <tt>ReplyReceiver</tt> is returned
     * for the repeatedly sent message.
     *
     * <p>
     * The returned <tt>ReplyReceiver</tt> can be used to block until all replies are gathered or to control
     * the process of waiting. It is also used to retrieve the list of received replies (instances of {@link ReplyMessage}).
     * Note that the receiver is registered automatically and unregistered when all the expected reply messages
     * arrive and the {@link ReplyReceiver#getReplies} is called.
     * </p>
     *
     * @param <E> the type of reply message to wait for
     * @param msg the message to send
     * @param replyMessageClass the reply messages class to wait for (other messages are ignored even if the message ID matches)
     * @param node the destination network node
     * @return the receiver used to gather all the replies
     * @throws IOException if the communication failed, e.g. the destination node cannot be reached, etc.
     */
    public <E extends ReplyMessage> ReplyReceiver<E> sendMessageWaitReply(Message msg, Class<E> replyMessageClass, NetworkNode node) throws IOException {
        return sendMessageWaitReply(msg, replyMessageClass, Collections.singleton(node));
    }

    /**
     * Send the message to the specified network node and wait for the replies.
     * Note that a particular message can be sent several times (to different nodes) using this method -
     * a reply is then expected from each of these nodes. The same instance of <tt>ReplyReceiver</tt> is returned
     * for the repeatedly sent message.
     *
     * <p>
     * The returned <tt>ReplyReceiver</tt> can be used to block until all replies are gathered or to control
     * the process of waiting. It is also used to retrieve the list of received replies (instances of {@link ReplyMessage}).
     * Note that the receiver is registered automatically and unregistered when all the expected reply messages
     * arrive.
     * </p>
     *
     * @param msg the message to send
     * @param node the destination network node
     * @return the receiver used to gather all the replies
     * @throws IOException if the communication failed, e.g. the destination node cannot be reached, etc.
     */
    public ReplyReceiver<? extends ReplyMessage> sendMessageWaitReply(Message msg, NetworkNode node) throws IOException {
        return sendMessageWaitReply(msg, ReplyMessage.class, true, node);
    }

    /**
     * Send the message to multiple network nodes and wait for the replies.
     * A reply message is expected from each of these nodes.
     *
     * <p>
     * The returned <tt>ReplyReceiver</tt> can be used to block until all replies are gathered or to control
     * the process of waiting. It is also used to retrieve the list of received replies (instances of {@link ReplyMessage}).
     * Note that the receiver is registered automatically and unregistered when all the expected reply messages
     * arrive.
     * </p>
     *
     * @param msg the message to send
     * @param nodes the list of destination network nodes
     * @return the receiver used to gather all the replies
     * @throws IOException if the communication failed, e.g. the destination node cannot be reached, etc.
     */
    public ReplyReceiver<? extends ReplyMessage> sendMessageWaitReply(Message msg, Collection<NetworkNode> nodes) throws IOException {
        return sendMessageWaitReply(msg, ReplyMessage.class, true, nodes);
    }

    /**
     * Send the message to a network node and wait for a single reply.
     * This method blocks until the respective reply message arrives
     * (or forever if something bad happens).
     *
     * @param <E> the type of reply message to wait for
     * @param msg the message to send
     * @param replyMessageClass the reply messages class to wait for (other messages are ignored even if the message ID matches)
     * @param node the destination network node
     * @return the received reply message
     * @throws IOException if the communication failed, e.g. the destination node cannot be reached, etc.
     */
    public <E extends ReplyMessage> E sendMessageWaitSingleReply(Message msg, Class<E> replyMessageClass, NetworkNode node) throws IOException {
        return sendMessageWaitReply(msg, replyMessageClass, true, node).getFirstReply();
    }

    /**
     * Send the message to a network node and wait for a single reply.
     * This method blocks until the respective reply message arrives
     * (or forever if something bad happens).
     *
     * @param <E> the type of reply message to wait for
     * @param msg the message to send
     * @param replyMessageClass the reply messages class to wait for (other messages are ignored even if the message ID matches)
     * @param node the destination network node
     * @param timeout timeout to wait for replies
     * @return the received reply message
     * @throws IOException if the communication failed, e.g. the destination node cannot be reached, etc.
     * @throws InterruptedException if there was no message within the specified timeout or the thread was interrupted while waiting
     */
    public <E extends ReplyMessage> E sendMessageWaitSingleReply(Message msg, Class<E> replyMessageClass, NetworkNode node, long timeout) throws IOException, InterruptedException {
        try {
            return sendMessageWaitReply(msg, replyMessageClass, true, node).getReplies(timeout).get(0);
        } catch (IndexOutOfBoundsException e) {
            throw new InterruptedException("Thare was no reply for message " + msg + " withing the specified timeout " + timeout + "ms");
        }
    }


    //****************** Recieve methods ******************//

    /**
     * Register a message receiver.
     * The method {@link Receiver#acceptMessage acceptMessage} of the receiver is called whenever
     * a message arrives at this dispatcher. The first receiver (in the order of their registrations) that
     * accepts the message stops the processing.
     *
     *
     * @param receiver the message receiver; see for example {@link ThreadInvokingReceiver} or {@link QueueInvokingReceiver}
     */
    public void registerReceiver(Receiver receiver) {
        synchronized (receivers) {
            if (receiver instanceof MessageDispatcher) {
                receivers.add(receiver);
                return;
            }
            ListIterator<Receiver> it = receivers.listIterator(receivers.size());
            while (it.hasPrevious()) {
                if (! (it.previous() instanceof MessageDispatcher)) {
                    receivers.add(it.nextIndex()+1, receiver);
                    return;
                }
            }
            receivers.add(0, receiver);
        }
    }

    /**
     * Remove a previously registered receiver.
     *
     * @param receiver a previously registered receiver that has to be removed
     * @return <tt>true</tt> if the specified receiver was registered
     */
    public boolean deregisterReceiver(Receiver receiver) {
        synchronized (receivers) {
            return receivers.remove(receiver);
        }
    }

    /**
     * Process a message received through sockets.
     * Since the dispatcher implements the receiver interface, the message is passed to the receiving.
     *
     * @param msg the message received
     */
    protected void receiveMessage(Message msg) {
        if (acceptMessage(msg, false))
            return;
        
        if (acceptMessage(msg, true))
            return;
        
        // Report unknown message
        log.log(Level.WARNING, "Received {0} (id={1}@{2}) ''{3}'' was not accepted by any receiver", new Object[]{msg.getClass().getSimpleName(), msg.messageID, msg.getOriginalSender(), msg.navigationPath});
    }

    /**
     * Offers a message to this message dispatcher, i.e. run through the dispatcher's list
     * of registered receivers and offer the message to them.
     *
     * @param msg the message offered to acceptance
     * @param allowSuperclass First, the message is offered with <tt>allowSuperclass</tt> set to <tt>false</tt>.
     *                        If no receiver accepts it, another offering round is issued with <tt>allowSuperclass</tt>
     *                        set to <tt>true</tt> (so the receiver can relax its acceptance conditions).
     * @return <tt>true</tt> if the message is accepted by any receiver from the internal list
     */
    @Override
    public boolean acceptMessage(Message msg, boolean allowSuperclass) {
        // Do not accept the message if it is not addressed directely to this message dispatcher or its sub-dispatchers
        NetworkNode actualDest = msg.getDestination();
        if ((actualDest != null) && !getNetworkNode().equals(actualDest))
            return false;
        
        // Search all registered receivers
        synchronized (receivers) {
            // Search through all other receivers then for exact message type
            for (Receiver receiver : receivers)
                if (receiver.acceptMessage(msg, allowSuperclass))
                    return true;
        }
        
        return false;
    }


    //****************** Message sending internals ******************//

    /**
     * Do the actual message sending.
     * This method works in the following steps:
     * <ul>
     * <li>the sending socket is chosen</li>
     * <li>a connection to the destination network node is opened or an already opened one is picked from the connection pool</li>
     * <li>the message is packed into byte stream</li>
     * <li>the byte stream is passed to the socket</li>
     * </ul>
     *
     * @param msg the message to be sent
     * @param node the destination network node
     * @throws IOException if the communication failed, e.g. the destination node cannot be reached, etc.
     */
    private void send(Message msg, NetworkNode node) throws IOException {
        if (node == null) {
            log.warning("Trying to send message to a NULL node");
            return;
        }
        
        // Try connection from pool
        ObjectOutputStream stream = tcpConnectionPool.get(node);
        if (stream != null) {
            try {
                synchronized (stream) {
                    putMessageIntoStream(msg, stream, node);
                    
                    // Remove all references from the stream memory thus next objects will be fully stored (and the memory is freed)
                    stream.reset();
                }
                return;
            } catch (IOException e) {
                tcpConnectionPool.remove(node);
            }
        }
        
        // No connection in pool, must create new
        // Socket socket = new Socket(node.getHost(), node.getPort());
        Socket socket = new Socket();
        socket.setReuseAddress(true);
        socket.connect(new InetSocketAddress(node.getHost(), node.getPort()));

        //socket.shutdownInput();
        stream = new ObjectOutputStream(socket.getOutputStream());
        
        // Send message
        putMessageIntoStream(msg, stream, node);
        
        if (tcpConnectionPoolSize > 0) {
            // Remove all references from the stream memory thus next objects will be fully stored (and the memory is freed)
            stream.reset();
            
            synchronized (tcpConnectionPool) {
                // If capacity limit reached, remove one message
                while (tcpConnectionPool.size() >= tcpConnectionPoolSize) {
                    Map.Entry<NetworkNode, ObjectOutputStream> removedConnection = tcpConnectionPool.entrySet().iterator().next();
                    synchronized (removedConnection.getValue()) {
                        removedConnection.getValue().close();
                    }
                    tcpConnectionPool.remove(removedConnection.getKey());
                }
                
                // Add connection to pool
                tcpConnectionPool.put(new NetworkNode(node.getHost(), node.getPort()), stream);
            }
        } else stream.close();
    }

    /**
     * Do the actual message broadcasting (i.e. sending to all network nodes).
     * The message is packed into a byte stream and sent though the broadcast socket.
     *
     * @param msg the message to be sent
     * @throws IOException if the communication failed, e.g. the broadcast socket is not initialized
     */
    private void send(Message msg) throws IOException {
        if (broadcastSocket == null)
            throw new IOException("Broadcast not supported - no port specified");
        
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream objStream = new ObjectOutputStream(stream);
        putMessageIntoStream(msg, objStream, null);
        objStream.close();
        byte[] data = stream.toByteArray();
        if (data.length > SocketThreadUDP.MAX_UDP_LENGTH)
            throw new IOException("Cannot broadcast message bigger than " + SocketThreadUDP.MAX_UDP_LENGTH + " bytes");
        
        broadcastSocket.send(new DatagramPacket(data, data.length, BROADCAST_GROUP, broadcastSocket.getLocalPort()));
    }

    /**
     * Packs the provided message into byte stream.
     * Java serialization is used to do the job.
     *
     * @param msg the message to pack to the stream
     * @param stream the stream to pack the message into
     * @param destinationNode the destination network node
     * @throws IOException if the message cannot be serialized
     */
    protected void putMessageIntoStream(Message msg, ObjectOutputStream stream, NetworkNode destinationNode) throws IOException {
        stream.writeObject(msg);
    }

    /**
     * Unpacks a message from the byte stream.
     * Java serialization is used to do the job.
     *
     * @param stream the stream from which the message should be unpacked
     * @return the unpacked message
     * @throws IOException if the message cannot be deserialized
     */
    protected Message getMessageFromStream(ObjectInputStream stream) throws IOException {
        try {
            return (Message)stream.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e.getMessage());
        }
    }

}