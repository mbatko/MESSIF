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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.util.StreamGenericAbstractObjectIterator;
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;
import messif.operations.OperationErrorCode;
import messif.operations.QueryOperation;

/**
 * A batch of several K-nearest neighbors query operations encapsulated as a single operation.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("k-nearest neighbors query")
public class BatchKNNQueryOperation extends QueryOperation {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    private final List<KNNQueryOperation> knnOperations ;

    //****************** Constructors ******************//

    /**
     * Creates a list of {@link KNNQueryOperation} for all specified query objects.
     * @param queryObjects iterator over the query objects
     * @param k the number of nearest neighbors to retrieve for each operation
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects"})
    public BatchKNNQueryOperation(StreamGenericAbstractObjectIterator queryObjects, int k) {
        this(queryObjects, k, AnswerType.NODATA_OBJECTS);
    }

    /**
     * Creates a list of {@link KNNQueryOperation} for all specified query objects.
     * @param queryObjects iterator over the query objects
     * @param k the number of nearest neighbors to retrieve for each operation
     * @param answerType the type of objects this operation stores in its answer
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects", "Answer type"})
    public BatchKNNQueryOperation(StreamGenericAbstractObjectIterator queryObjects, int k, AnswerType answerType) {
        this(queryObjects, Integer.MAX_VALUE, k, answerType);
    }

    /**
     * Creates a list of {@link KNNQueryOperation} for all specified query objects.
     * @param queryObjects iterator over the query objects
     * @param maxNQueries maximal number of query objects read from the iterator (can be {@code Integer.MAX_VALUE}
     * @param k the number of nearest neighbors to retrieve for each operation
     * @param answerType the type of objects this operation stores in its answer
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects", "Store the meta-object subdistances?", "Answer type"})
    public BatchKNNQueryOperation(StreamGenericAbstractObjectIterator queryObjects, int maxNQueries, int k, AnswerType answerType) {
        super(answerType);
        List<KNNQueryOperation> operations = new ArrayList<>();
        int i = 0;
        while (queryObjects.hasNext() && i ++ < maxNQueries) {
            operations.add(new KNNQueryOperation(queryObjects.next(), k, answerType));
        }
        this.knnOperations = Collections.unmodifiableList(operations);
    }

    //****************** Attribute access ******************//

    public int getNOperations() {
        return knnOperations.size();
    }
    
    public KNNQueryOperation getOperation(int index) {
        return knnOperations.get(index);
    }

    public List<KNNQueryOperation> getKnnOperations() {
        return knnOperations;
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
        default:
            throw new IndexOutOfBoundsException("kNNQueryOperation has only two arguments");
        }
    }

    /**
     * Returns number of arguments that were passed while constructing this instance.
     * @return number of arguments that were passed while constructing this instance
     */
    @Override
    public int getArgumentCount() {
        return 0;
    }

    @Override
    public boolean wasSuccessful() {
        return getErrorCode() == OperationErrorCode.RESPONSE_RETURNED;
    }

    @Override
    public void endOperation() {
        endOperation(OperationErrorCode.RESPONSE_RETURNED);
    }

    @Override
    public int evaluate(AbstractObjectIterator objects) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Class getAnswerClass() {
        if (knnOperations.isEmpty()) {
            return null;
        }
        return (knnOperations.get(0).getAnswerClass());
    }

    @Override
    public int getAnswerCount() {
        return 0;
    }

    @Override
    public Iterator getAnswer() {
        return Collections.emptyIterator();
    }

    @Override
    public Iterator getAnswer(int skip, int count) {
        return Collections.emptyIterator();        
    }

    @Override
    public Iterator getAnswerObjects() {
        return Collections.emptyIterator();
    }

    @Override
    public void resetAnswer() {
    }

    @Override
    public int getSubAnswerCount() {
        return knnOperations.size();
    }

    @Override
    public Iterator getSubAnswer(int index) throws IndexOutOfBoundsException {
        return knnOperations.get(index).getAnswer();
    }

    @Override
    public Iterator getSubAnswer(Object key) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected boolean dataEqualsImpl(QueryOperation operation) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int dataHashCode() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
