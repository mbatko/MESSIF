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
package messif.operations;

import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import messif.objects.AbstractObject;
import static messif.operations.OperationErrorCode.RESPONSE_RETURNED;

/**
 * This operation encapsulates {@link RankingSingleQueryOperation} and returns a candidate set requested size for 
 *  this operation. The candidate set is returned in form of queue of string locators. It is possible that an algorithm
 *  returns the operation immediately after the processing starts and other threads continue in processing afterwards.
 *  After the processing is really finished, it is expected that method {@link AbstractOperation#endOperation(messif.utility.ErrorCode)} 
 *  is called; at the same time, the answer queue should stop blocking
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class GetCandidateSetOperation extends AbstractOperation {

    /** Class id for serialization. */
    private static final long serialVersionUID = 512301L;    
    /**
     * Operation for which the candidate set should be returned.
     */
    private final RankingSingleQueryOperation encapsulatedOperation;

    /**
     * Optional parameter saying how many candidate locators (IDs) should be return.
     */
    private final int candidateSetSize;
    
    /** 
     * Output of the operation stored in a blocking queue. 
     */
    protected final BlockingQueue<String> candidateSetLocators;    

    /**
     * Creates a new operation given a query operation and required size of candidate set.
     * @param encapsulatedOperation encapsulated query operation
     * @param candidateSetSize required size of candidate set
     */
    @AbstractOperation.OperationConstructor({"operation to get the CS for", "size of the CS"})
    public GetCandidateSetOperation(RankingSingleQueryOperation encapsulatedOperation, int candidateSetSize) {
        this(encapsulatedOperation, candidateSetSize, new ArrayBlockingQueue<String>(candidateSetSize));
    } 

    /**
     * Creates a new operation given a query operation and required size of candidate set.
     * @param encapsulatedOperation encapsulated query operation
     * @param candidateSetSize required size of candidate set
     * @param candidateLocatorQueue queue to be used for storing the candidate locators
     */
    public GetCandidateSetOperation(RankingSingleQueryOperation encapsulatedOperation, int candidateSetSize, BlockingQueue<String> candidateLocatorQueue) {
        this.encapsulatedOperation = encapsulatedOperation;
        this.candidateSetSize = candidateSetSize;
        this.candidateSetLocators = candidateLocatorQueue;
    } 
    
    // ************************      Getters and setters     ***************************** //
    
    /**
     * Get the required size of candidate set
     * @return required size of candidate set
     */
    public int getCandidateSetSize() {
        return candidateSetSize;
    }

    /**
     * Returns the queue of candidate locators. It is a blocking queue of capacity {@link #candidateSetSize}.
     * @return the queue of candidate locators
     */
    public BlockingQueue<String> getCandidateSetLocators() {
        return candidateSetLocators;
    }

    /**
     * Get the encapsulated query operation.
     * @return encapsulated query operation
     */
    public RankingSingleQueryOperation getEncapsulatedOperation() {
        return encapsulatedOperation;
    }
    
    // ************************     Data manipulation    ***************************** //
    
    // TODO: manage also the nubmer of ADDED objects so that the limit can be checked
    
    /**
     * Adds all objects from the iterator to the answer queue of this operation.
     * @param it iterator over objects to be added to the answer
     * @return true if all objects from the iterator were added to the answer, false otherwise
     */
    public boolean addAll(Iterator<AbstractObject> it) {
        try {
            while (it.hasNext()) {
                candidateSetLocators.add(it.next().getLocatorURI());
            }
            return true;
        } catch (IllegalStateException ex) {
            return false;
        }
    }
    
    // ************************      Overrides     ******************************** //
    
    @Override
    public Object getArgument(int index) throws IndexOutOfBoundsException {
        if (index != 0)
            throw new IndexOutOfBoundsException("BulkInsertOperation has only one argument");
        return getCandidateSetSize();
    }

    @Override
    public int getArgumentCount() {
        return 1;
    }
    
    @Override
    public void updateFrom(AbstractOperation operation) throws ClassCastException {
        if (! (operation instanceof GetCandidateSetOperation))
            throw new IllegalArgumentException(getClass().getSimpleName() + " cannot be updated from " + operation.getClass().getSimpleName());        
        if (this == operation) {
            return;
        }
        GetCandidateSetOperation castOp = (GetCandidateSetOperation) operation;
        this.candidateSetLocators.addAll(castOp.candidateSetLocators);
        super.updateFrom(operation);                
    }
    
    @Override
    public boolean wasSuccessful() {
        return (getErrorCode() == RESPONSE_RETURNED);
    }

    @Override
    public void endOperation() {
        endOperation(RESPONSE_RETURNED);
    }
    
}
