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
package messif.operations.query;

import messif.network.NetworkNode;
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;

/**
 * Operation for retrieving all objects locally stored at the given peer.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
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
