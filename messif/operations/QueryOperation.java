/*
 * QueryOperation.java
 *
 * Created on 22. cervenec 2004, 13:16
 */

package messif.operations;

import java.util.Iterator;
import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;


/**
 * The base class for all query operations.
 * Query operations retrive data from indexing structures according to their specification,
 * but they do not modify the indexed data.
 * Once a query operation is executed on an index structure, its answer is updated.
 * 
 * <p>
 * There are three cathegories of query operations that return different types of answers:
 * <ul>
 * <li>{@link SingletonQueryOperation} - returns a single {@link AbstractObject}</li>
 * <li>{@link ListingQueryOperation} - returns a collection of {@link AbstractObject}</li>
 * <li>{@link RankingQueryOperation} - returns a distance-ranked collection of {@link AbstractObject}</li>
 * </ul>
 * </p>
 * 
 * 
 * @param <TAnswer> the class of objects returned in the query answer
 * @author Vlastislav Dohnal, xdohnal@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
public abstract class QueryOperation<TAnswer> extends AbstractOperation {
    /** class id for serialization */
    private static final long serialVersionUID = 2L;

    //****************** Attributes ******************//

    /** Types of objects this query operation will return */
    protected final AnswerType answerType;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of QueryOperation.
     * @param answerType the type of objects this operation stores in its answer
     */
    protected QueryOperation(AnswerType answerType) {
        this.answerType = answerType;
    }


    //****************** Error code utilities ******************//

    /**
     * Returns <tt>true</tt> if this operation has finished successfuly.
     * Otherwise, <tt>false</tt> is returned - the operation was either unsuccessful or is has not finished yet.
     *
     * @return <tt>true</tt> if this operation has finished successfuly
     */
    public boolean wasSuccessful() {
        return errValue.equals(OperationErrorCode.RESPONSE_RETURNED);
    }

    /** End operation successfully */
    public void endOperation() {
        errValue = OperationErrorCode.RESPONSE_RETURNED;
    }


    //****************** Operation evaluation ******************//

    /**
     * Evaluate this query on a given set of objects.
     * The objects found by this evaluation are added to the answer of the particular query.
     *
     * @param objects the collection of objects on which to evaluate this query
     * @return number of objects satisfying the query
     */
    public abstract int evaluate(AbstractObjectIterator<? extends LocalAbstractObject> objects);
    

    //****************** Answer methods ******************//

    /**
     * Returns the type of objects this operation stores in its answer.
     * @return the type of objects this operation stores in its answer
     */
    public AnswerType getAnswerType() {
        return answerType;
    }

    /**
     * Returns the class of objects this operation stores in its answer.
     * @return the class of objects this operation stores in its answer
     */
    public abstract Class<? extends TAnswer> getAnswerClass();

    /**
     * Returns the number of objects in this query answer.
     * @return the number of objects in this query answer
     */
    public abstract int getAnswerCount();
    
    /**
     * Returns an iterator over all objects in the answer to this query.
     * @return an iterator over all objects in the answer to this query
     */
    public abstract Iterator<TAnswer> getAnswer();

    /**
     * Returns an iterator over all objects in the answer skipping the first
     * {@code skip} items and returning only {@code count} elements. If {@code count}
     * is less than or equal to zero, all objects from the answer (except for
     * {@code skip}) are returned.
     *
     * @param skip number of answer objects to skip
     * @param count number of answer objects to iterate (maximally, actual number of results can be smaller)
     * @return an iterator over the objects in the answer to this query
     */
    public abstract Iterator<TAnswer> getAnswer(int skip, int count);

    /**
     * Returns an iterator over all {@link AbstractObject}s in the answer to this query.
     * This method unwraps the objects from the results.
     * @return an iterator over all {@link AbstractObject}s in the answer to this query
     */
    public abstract Iterator<AbstractObject> getAnswerObjects();

    /**
     * Reset the current query answer.
     * All objects from the answer are deleted, {@link #getAnswerCount()} will return zero.
     */
    public abstract void resetAnswer();


    //****************** Textual representation ******************//

    /**
     * Appends the error code of this query to the specified string along with
     * the information about the number of objects in the current answer.
     * @param str the string to add the error code to
     */
    @Override
    protected void appendErrorCode(StringBuilder str) {
        str.append(" returned ").append(getAnswerCount()).append(" objects");
        super.appendErrorCode(str);
    }

}
