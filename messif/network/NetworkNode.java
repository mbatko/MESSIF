/*
 * NetworkNode.java
 *
 * Created on 3. kveten 2003, 19:16
 */

package messif.network;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Identification of a network node, that is able to communicate over network.
 * Basically, it is an IP address and a TCP/UDP port of a {@link MessageDispatcher}
 * on a networked computer. The NetworkNode is used identify senders and receivers
 * of messages. For scenarios with multiple nodes per peer that shares the top-level
 * message dispatcher, there is a sub-ID identification.
 * 
 * @see MessageDispatcher
 * @author  xbatko
 */
public class NetworkNode implements Serializable {

    /** class serial id for serialization */     
    private static final long serialVersionUID = 3L;

    /****************** Attributes ******************/

    /** IP address of this network node */
    protected InetAddress host;

    /** TCP/UDP port of this network node */
    protected int port;

    /**
     * Identification of a specific node, when several logical nodes are running on one physical peer.
     * They will also share the message dispatchers, where the top-level dispatcher has no nodeID set.
     * It is <tt>null</tt> if nodeID is not used.
     */
    protected final Integer nodeID;


    /****************** Attribute access ******************/

    /**
     * Returns the IP address of this network node.
     * @return the IP address of this network node
     */
    public final InetAddress getHost() {
        return host;
    }

    /**
     * Returns the TCP/UDP port of this network node.
     * @return the TCP/UDP port of this network node
     */
    public final int getPort() {
        return port;
    }

    /**
     * Returns the sub-ID of this network node.
     * This is used if the message dispatcher, that returns this network node, is not the top-level one.
     *
     * @return the sub-ID of this network node or <tt>null</tt> if it was not set
     */
    public final Integer getNodeID() {
        return nodeID;
    }

    /**
     * Returns <tt>true</tt> if the nodeID was set.
     * @return <tt>true</tt> if the nodeID was set
     */
    public final boolean hasNodeID() {
        return nodeID != null;
    }


    /****************** Constructors ******************/

    /**
     * Creates a new instance of NetworkNode for the specified host, port and nodeID.
     *
     * @param host the IP address of the network node
     * @param port the TCP/UDP port of the network node
     * @param nodeID the sub-ID of the network node
     */
    public NetworkNode(InetAddress host, int port, Integer nodeID) {
        this.host = host;
        this.port = port;
        this.nodeID = nodeID;
    }

    /**
     * Creates a new instance of NetworkNode for the specified host, port and nodeID.
     * 
     * 
     * @param host the FQDN address of the network node
     * @param port the TCP/UDP port of the network node
     * @param nodeID the sub-ID of the network node
     * @throws UnknownHostException if the provided FQDN address cannot be resolved to IP address
     */
    public NetworkNode(String host, int port, Integer nodeID) throws UnknownHostException {
        this(InetAddress.getByName(host), port, nodeID);
    }

    /**
     * Creates a new instance of NetworkNode for the specified host/port pair.
     * NodeID is set to <tt>null</tt>.
     *
     * @param host the IP address of the network node
     * @param port the TCP/UDP port of the network node
     */
    public NetworkNode(InetAddress host, int port) {
        this(host, port, null);
    }

    /**
     * Creates a new instance of NetworkNode for the specified host/port pair.
     * NodeID is set to <tt>null</tt>.
     *
     * @param host the FQDN address of the network node
     * @param port the TCP/UDP port of the network node
     * @throws UnknownHostException if the provided FQDN address cannot be resolved to IP address
     */
    public NetworkNode(String host, int port) throws UnknownHostException {
        this(InetAddress.getByName(host), port, null);
    }

    /**
     * Creates a copy of the provided NetworkNode.
     * Host, port and sub-ID attributes are copied.
     *
     * @param node the source NetworkNode to copy from
     */
    public NetworkNode(NetworkNode node) {
        this(node.host, node.port, node.nodeID);
    }

    /**
     * Creates a copy of the provided NetworkNode.
     * Host and port attributes are copied always, the sub-ID
     * attribute is copied if the <tt>copyNodeID</tt> parameter is <tt>true</tt>,
     * otherwise it is set to <tt>null</tt>
     *
     * @param node the source NetworkNode to copy from
     * @param copyNodeID if <tt>true</tt>, the sub-ID attribute is copied, otherwise, it is set to <tt>null</tt>
     */
    public NetworkNode(NetworkNode node, boolean copyNodeID) {
        this(node.host, node.port, copyNodeID?node.nodeID:null);
    }

    /**
     * Creates a copy of the provided NetworkNode.
     * Host and port attributes are copied from the provided NetworkNode and
     * sub-ID is set from the parameter.
     *
     * @param node the source NetworkNode to copy from
     * @param nodeID the sub-ID of the network node
     */
    public NetworkNode(NetworkNode node, Integer nodeID) {
        this(node.host, node.port, nodeID);
    }

    /**
     * Creates a new instance of NetworkNode from string "host:port#node".
     * The port part is optional - a free port is then assigned by OS automatically.
     * The node part is optional - if it is ommited, the nodeID is set to <tt>null</tt>.
     * 
     * 
     * @param hostPortNode the string to parse the NetworkNode from - its format should be "host", "host:port" or "host:port#node"
     * @return a new instance of NetworkNode for the specified host, port and nodeID
     * @throws UnknownHostException if the host name cannot be resolved to IP address
     */
    public static NetworkNode valueOf(String hostPortNode) throws UnknownHostException {
        if (hostPortNode == null || hostPortNode.length() == 0)
            return null;
        int colonPos = hostPortNode.lastIndexOf(':');
        if (colonPos == -1)
            return new NetworkNode(hostPortNode, 0);
        int hashPos = hostPortNode.lastIndexOf('#');
        if (hashPos == -1)
            return new NetworkNode(hostPortNode.substring(0, colonPos),  Integer.parseInt(hostPortNode.substring(colonPos + 1)));
        return new NetworkNode(hostPortNode.substring(0, colonPos), Integer.parseInt(hostPortNode.substring(colonPos + 1, hashPos)), Integer.valueOf(hostPortNode.substring(hashPos + 1)));
    }


    /****************** Comparison override ******************/

    /**
     * Compares this network node with another object.
     * The result is <tt>true</tt> if and only if the argument is not
     * <tt>null</tt> and is a <tt>NetworkNode</tt> object that
     * has the same address and port as this NetworkNode.
     * If both the nodes has set the nodeID, their values must be equal too,
     * otherwise, the nodeID is ignored.
     *
     * @param obj the object to compare with
     * @return <tt>true</tt> if this object is the same as the obj
     *         argument; <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj) {
	if (!(obj instanceof NetworkNode))
            return false;
        
        // Object is network node, test equality of host & port
        NetworkNode netNode = (NetworkNode) obj;
        if (!equalsIgnoreNodeID(netNode))
            return false;
        
        // Compare nodeIDs (if none of them NULL)
        return ((this.nodeID == null) || (netNode.nodeID == null) || (this.nodeID.equals(netNode.nodeID)));
    }
    
    /**
     * Compare the address and port of actual network node with other network node's host and port.
     * @param node the other network node to compare this netnode with
     * @return <code>true</code> if host and port of this network node is equal to the host and port of the netNode argument;
     *         <code>false</code> is returned otherwise
     */
    public boolean equalsIgnoreNodeID(NetworkNode node) {
        return (node != null) && host.equals(node.host) && (port == node.port);
    }

    /**
     * Returns a hash code value for this network node.
     * @return a hash code value for this network node
     */
    @Override
    public int hashCode() {
	return (host.hashCode() << 16) + port;
    }


    /****************** Serialization overrides ******************/

    /** Mapping table for translating original host names (optionally plus ports) to new ones */
    protected static Map<InetAddress, Map<Integer, NetworkNode>> netnodeMappingTable = null;

    /** Mapping table for translating parent message dispatchers - key is port and value is a top-level dispatcher for that port */
    protected static Map<Integer, MessageDispatcher> messageDispMappingTable = null;

    /**
     *  Setter method for the mapping table.
     *  Use this method before deserialization of algorithm to change host names (and optionally ports and node IDs) of
     *  loaded NetworkNodes.
     *  After the deserialization, method {@link #resetHostMappingTable} should be used to disable remapping.
     * 
     * @param netnodeMappingTable the new host names mapping table or <tt>null</tt> to disable it.
     * @param messageDispMappingTable mapping table for translating parent message dispatchers - key is port and value is a top-level dispatcher for that port
     */
    public static void setHostMappingTable(Map<InetAddress, Map<Integer, NetworkNode>> netnodeMappingTable, Map<Integer, MessageDispatcher> messageDispMappingTable) {
        NetworkNode.netnodeMappingTable = netnodeMappingTable;
        NetworkNode.messageDispMappingTable = messageDispMappingTable;
    }

    /**
     * Load the mapping table from the specified file.
     * The file should contain one mapping per line in the format 'old_host_name = new_host_name'.
     * Comments (lines that begins with #) and empty lines are ignored.
     *
     *  Use this method before deserialization of algorithm to change host names of
     *  loaded NetworkNodes.
     *  After the deserialization, method {@link #resetHostMappingTable} should be used to disable remapping.
     *
     * @param fileName the file to load mapping table from
     * @throws UnknownHostException if a host specified in mapping cannot be resolved (mappings loaded before the exception occurred are kept loaded)
     * @throws IOException if there was a problem loading the file (mappings loaded before the exception occurred are kept loaded)
     */
    public static void loadHostMappingTable(String fileName) throws UnknownHostException, IOException {
        // Open the mapping table file
        BufferedReader mappingFile = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));

        // Prepare mapping table
        netnodeMappingTable = new HashMap<InetAddress, Map<Integer, NetworkNode>>();

        for (String line = mappingFile.readLine(); line != null && (line.length() > 0); line = mappingFile.readLine()) {
            line = line.trim();
            // Skip comments
            if (line.startsWith("#"))
                continue;
            String[] hosts = line.split("\\p{Space}*=\\p{Space}*", 2);
            if (hosts.length != 2)
                continue; // Skip lines with invalid format or comments

            // Get port colon and parse host & port
            int colonPos = hosts[0].indexOf(':');
            InetAddress host;
            int port;
            if (colonPos != -1) {
                host = InetAddress.getByName(hosts[0].substring(0, colonPos));
                port = Integer.parseInt(hosts[0].substring(colonPos + 1));
            } else {
                host = InetAddress.getByName(hosts[0]);
                port = 0;
            }

            // Get port map for the specified host
            Map<Integer, NetworkNode> portMap = netnodeMappingTable.get(host);
            if (portMap == null) {
                portMap = new HashMap<Integer, NetworkNode>();
                netnodeMappingTable.put(host, portMap);
            }

            // Store the mapping
            portMap.put(port, NetworkNode.valueOf(hosts[1]));
        }

        messageDispMappingTable = new HashMap<Integer, MessageDispatcher>();
    }

    /**
     *  Reset the mapping table - it will no longer provide any remapping.
     *  This methods should be used after the deserialization.
     */
    public static void resetHostMappingTable() {
        netnodeMappingTable = null;
        messageDispMappingTable = null;
    }

    /**
     * Deserialization method.
     * Object is loaded as usuall, but its host (and optionally port) is changed according to the mapping table.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (netnodeMappingTable != null) {
            Map<Integer, NetworkNode> portMap = netnodeMappingTable.get(host);
            NetworkNode newNode;
            if (portMap != null) {
                newNode = portMap.get(port);
                if (newNode == null)
                    newNode = portMap.get(0);
            } else newNode = null;

            if (newNode != null) {
                host = newNode.host;
                if (newNode.port != 0)
                    port = newNode.port;
            }
        }
    }


    /****************** String representation ******************/
    
    /**
     * Returns the string representation of this network node.
     * @return the string representation of this network node
     */
    @Override
    public String toString() {
        StringBuffer rtv = new StringBuffer(host.getHostName());
        rtv.append(":").append(port);
        if (nodeID != null)
            rtv.append("#").append(nodeID);
        return rtv.toString();
    }
    
}
