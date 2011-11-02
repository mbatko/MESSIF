/*
 * This file is part of MESSIF library.
 *
 * MESSIF library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MESSIF library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.operations;

/**
 * Special operation that returns the algorithm info (i.e. the algorithm toString() value).
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("Algorithm info")
public class AlgorithmInfoOperation extends AbstractOperation {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Answer of this query, will be <tt>null</tt> if the info is not set */
    private String answer;


    //****************** Constructors ******************//

    /**
     * Creates a new algorithm info operation.
     */
    @AbstractOperation.OperationConstructor({})
    public AlgorithmInfoOperation() {
    }


    //****************** Attribute access ******************//

    /**
     * Returns the operation answer text.
     * @return the operation answer text
     */
    public String getAnswer() {
        return answer;
    }

    /**
     * Set the operation answer text.
     * @param answer the new operation answer
     */
    public void addToAnswer(String answer) {
        this.answer = answer;
    }

    @Override
    public Object getArgument(int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException("AlgorithmInfoOperation has no arguments");
    }

    @Override
    public int getArgumentCount() {
        return 0;
    }

    @Override
    protected boolean dataEqualsImpl(AbstractOperation operation) {
        if (!(operation instanceof AlgorithmInfoOperation))
            return false;
        AlgorithmInfoOperation castOperation = (AlgorithmInfoOperation)operation;
        if (answer == null)
            return castOperation.answer == null;
        if (castOperation.answer == null)
            return false;
        return answer.equals(castOperation.answer);
    }

    @Override
    public int dataHashCode() {
        return answer == null ? 0 : answer.hashCode();
    }

    @Override
    public void updateFrom(AbstractOperation operation) throws ClassCastException {
        if (!(operation instanceof AlgorithmInfoOperation))
            throw new IllegalArgumentException(getClass().getSimpleName() + " cannot be updated from " + operation.getClass().getSimpleName());
        if (answer == null)
            addToAnswer(((AlgorithmInfoOperation)operation).answer);
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

    /**
     * Return a string representation of this operation.
     * @return a string representation of this operation
     */
    @Override
    public String toString() {
        return wasSuccessful() ? answer : getErrorCode().toString();
    }

}
