/*
 * LocalGetAllObjectsQueryOperation.java
 * 
 * Created on 6.8.2007, 13:49:45
 * 
 */

package messif.operations;

import messif.network.NetworkNode;

/**
 * Operation for retrieving all objects locally stored at the given peer.
 *
 * @author Vlastislav Dohnal, dohnal@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
@AbstractOperation.OperationName("Get all objects of a peer query")
public class LocalGetAllObjectsQueryOperation extends GetAllObjectsQueryOperation {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;


    /** Identification of the peer which data will be returned. Accessed directly. */
    public final NetworkNode peer;

    /****************** Constructors ******************/

    /**
     * Creates a new instance of GetPeersAllObjectsQuery for the specified node id.
     * 
     * @param peer the peer's network identification
     */
    @AbstractOperation.OperationConstructor({"Network node"})
    public LocalGetAllObjectsQueryOperation(NetworkNode peer) {
        this.peer = peer;
    }

    /**
     * Creates a new instance of GetPeersAllObjectsQuery for the specified node id and answer type.
     * 
     * @param peer the peer's network identification
     * @param answerType the type of objects this operation stores in its answer
     */
    @AbstractOperation.OperationConstructor({"Network node", "Answer type"})
    public LocalGetAllObjectsQueryOperation(NetworkNode peer, AnswerType answerType) {
        super(answerType);
        this.peer = peer;
    }
    
    /**
     * Returns argument that was passed while constructing instance.
     * If the argument is not stored within operation, <tt>null</tt> is returned.
     * @param index index of an argument passed to constructor
     * @return argument that was passed while constructing instance
     * @throws IndexOutOfBoundsException if index parameter is out of range
     */
    @Override
    public Object getArgument(int index) throws IndexOutOfBoundsException {
        if (index == 0)
            return peer;
        else
            throw new IndexOutOfBoundsException("GetPeersAllObjectsQueryOperation has only one argument");
    }

    /**
     * Returns number of arguments that were passed while constructing this instance.
     * @return number of arguments that were passed while constructing this instance
     */
    @Override
    public int getArgumentCount() {
        return 1;
    }

    /**
     * Returns a string representation of this operation.
     * @return a string representation of this operation.
     */
    @Override
    public String toString() {
        return new StringBuffer("Node "+peer).append(": ").append(super.toString()).toString();
    }


    /****************** Equality driven by operation data ******************/

    /** 
     * Indicates whether some other operation has the same data as this one.
     * @param   obj   the reference object with which to compare.
     * @return  <code>true</code> if this object has the same data as the obj
     *          argument; <code>false</code> otherwise.
     */
    @Override
    protected boolean dataEqualsImpl(AbstractOperation obj) {
        if (!(obj instanceof LocalGetAllObjectsQueryOperation) || !super.dataEquals(obj))
            return false;
        return peer.equals(((LocalGetAllObjectsQueryOperation)obj).peer);
    }

    @Override
    public int dataHashCode() {
        return super.dataHashCode() << 8 + peer.hashCode();
    }

}
