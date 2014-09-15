/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package messif.operations.data;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import messif.operations.AbstractOperation;
import static messif.operations.OperationErrorCode.RESPONSE_RETURNED;
import messif.operations.RankingSingleQueryOperation;

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
        this.encapsulatedOperation = encapsulatedOperation;
        this.candidateSetSize = candidateSetSize;
        this.candidateSetLocators = new ArrayBlockingQueue<>(candidateSetSize);
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
    
    
    // ************************      Overrides     ******************************** //
    
    @Override
    public boolean wasSuccessful() {
        return (getErrorCode() == RESPONSE_RETURNED);
    }

    @Override
    public void endOperation() {
        endOperation(RESPONSE_RETURNED);
    }
    
}
