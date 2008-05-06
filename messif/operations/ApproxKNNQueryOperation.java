/*
 * ApproxKNNQueryOperation.java
 *
 * Created on 21. cerven 2007, 16:52
 *
 */

package messif.operations;

import messif.objects.LocalAbstractObject;

/**
 * Approximate k-nearest neighbors query.
 *
 * @author xbatko
 */
@AbstractOperation.OperationName("Approximate k-nearest neighbors query")
public class ApproxKNNQueryOperation extends kNNQueryOperation {
    
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    /** Creates a new instance of ApproxKNNQueryOperation */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects"})
    public ApproxKNNQueryOperation(LocalAbstractObject queryObject, int k) {
        super(queryObject, k);
    }
    
}
