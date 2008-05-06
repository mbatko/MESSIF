
package messif.operations;

import messif.network.NetworkNode;
import messif.objects.LocalAbstractObject;

/**
 *
 * @author  David Novak, david.novak@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
@AbstractOperation.OperationName("Local range query")
public class LocalRangeQueryOperation extends RangeQueryOperation {
    
    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;
    
    /****************** Query request attributes ******************/
    
    /** Identification of the peer which data will be returned. Accessed directly. */
    public final NetworkNode peer;

        
    /****************** Constructors ******************/

    /**
     * Creates a new instance of RangeQueryOperation given the query object and radius.
     * @param queryObject the query object
     * @param radius the query radius
     * @param peer the NNID of the peer to run range query on
     */
    @AbstractOperation.OperationConstructor({"Query object", "Query radius", "Network-node ID"})
    public LocalRangeQueryOperation(LocalAbstractObject queryObject, float radius, NetworkNode peer) {
        super(queryObject, radius);
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
        switch (index) {
        case 0:
            return queryObject;
        case 1:
            return radius;
        case 2:
            return peer;
        default:
            throw new IndexOutOfBoundsException("RangeQueryOperation has only two arguments");
        }
    }

    /**
     * Returns number of arguments that were passed while constructing this instance.
     * @return number of arguments that were passed while constructing this instance
     */
    @Override
    public int getArgumentCount() {
        return 3;
    }

    
    /**
     * Prints out all the content of answer in a fancy way.
     * @return textual representation of this query
     */
    @Override
    public String toString() {
        return new StringBuffer("Local range query <").append(queryObject).append(',').append(radius).append("> returned ").append(getAnswerCount()).append(" objects").toString();
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

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return super.dataHashCode() << 8 + peer.hashCode();
    }

}
