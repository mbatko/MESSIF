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

import messif.operations.data.GetCandidateSetOperation;

/**
 * This operation starts refinement of the encapsulated {@link RankingSingleQueryOperation} using
 *  the candidate set stored in the provided {@link GetCandidateSetOperation}.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class RefineCandidateOperation extends AbstractOperation {

    /** Class id for serialization. */
    private static final long serialVersionUID = 332201L;
    /**
     * Operation encapsulating the candidate set.
     */
    private final GetCandidateSetOperation candidateOperation;
    
    /**
     * Operation to be refined using the given candidate set.
     */
    private final RankingSingleQueryOperation operationToRefine;
    
    /**
     * Creates the operation given a {@link RankingSingleQueryOperation} to be refined and the candidate set
     * within a {@link GetCandidateSetOperation}.
     *
     * @param candidateOperation encapsulation of the candidate set
     * @param operationToRefine ranking operation to be refined
     */
    public RefineCandidateOperation(GetCandidateSetOperation candidateOperation, RankingSingleQueryOperation operationToRefine) {
        this.candidateOperation = candidateOperation;
        this.operationToRefine = operationToRefine;
    }    

    /**
     * Get the candidate operation.
     * @return the candidate operation.
     */
    public GetCandidateSetOperation getCandidateOperation() {
        return candidateOperation;
    }
    
    /**
     * Returns the ranking operation to be refined.
     * @return the ranking operation to be refined.
     */
    public RankingSingleQueryOperation getRankingOperation() {
        return operationToRefine;
    }
    
    // *****************************        Overrides         ********************************* //
    
    @Override
    public Object getArgument(int index) throws IndexOutOfBoundsException {
        switch (index) {
        case 0:
            return getRankingOperation().getQueryObject().getLocatorURI();
        case 1:
            return getCandidateOperation().getCandidateSetSize();
        default:
            throw new IndexOutOfBoundsException("GetObjectByLocatorOperation has only one argument");
        }
    }

    @Override
    public int getArgumentCount() {
        return 2;
    }
    
    @Override
    public void updateFrom(AbstractOperation operation) throws ClassCastException {
        if (! (operation instanceof RefineCandidateOperation))
            throw new IllegalArgumentException(getClass().getSimpleName() + " cannot be updated from " + operation.getClass().getSimpleName());        
        if (this == operation) {
            return;
        }
        RefineCandidateOperation castOp = (RefineCandidateOperation) operation;
        this.operationToRefine.updateFrom(castOp.getRankingOperation());
        super.updateFrom(operation);                
    }
    
    @Override
    public boolean wasSuccessful() {
        return isErrorCode(OperationErrorCode.RESPONSE_RETURNED);
    }

    @Override
    public void endOperation() {
        endOperation(OperationErrorCode.RESPONSE_RETURNED);
    }
    
}
