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

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;


/**
 * The base class for all query operations.
 * Query operations retrieve data from indexing structures according to their specification,
 * but they do not modify the indexed data.
 * Once a query operation is executed on an index structure, its answer is updated.
 * 
 * <p>
 * There are three categories of query operations that return different types of answers:
 * <ul>
 * <li>{@link SingletonQueryOperation} - returns a single {@link AbstractObject}</li>
 * <li>{@link ListingQueryOperation} - returns a collection of {@link AbstractObject}</li>
 * <li>{@link RankingQueryOperation} - returns a distance-ranked collection of {@link AbstractObject}</li>
 * </ul>
 * </p>
 * 
 * 
 * @param <TAnswer> the class of objects returned in the query answer
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
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

    @Override
    public boolean wasSuccessful() {
        return isErrorCode(OperationErrorCode.RESPONSE_RETURNED);
    }

    /** End operation successfully */
    @Override
    public void endOperation() {
        endOperation(OperationErrorCode.RESPONSE_RETURNED);
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

    /**
     * Execute query operation on the given objects iterator and return the answer.
     * The operation to execute is created according to the given class and arguments.
     * This is a shortcut method for calling {@link AbstractOperation#createOperation(java.lang.Class, java.lang.Object[])},
     * {@link #evaluate} and {@link #getAnswer()}.
     *
     * @param <T> the type of query operation answer
     * @param objects the collection of objects on which to evaluate this query
     * @param operationClass the class of the operation to execute on this algorithm
     * @param arguments the arguments for the operation constructor
     * @return iterator for the answer of the executed query
     * @throws IllegalArgumentException if the argument count or their types don't match the specified operation class constructor
     * @throws InvocationTargetException if the operation constructor has thrown an exception
     * @throws NoSuchMethodException if the operation is unknown or unsupported by this algorithm
     */
    public static <T> Iterator<? extends T> getQueryAnswer(AbstractObjectIterator<? extends LocalAbstractObject> objects, Class<? extends QueryOperation<? extends T>> operationClass, Object... arguments) throws IllegalArgumentException, InvocationTargetException, NoSuchMethodException {
        QueryOperation<? extends T> queryOperation = AbstractOperation.createOperation(operationClass, arguments);
        queryOperation.evaluate(objects);
        return queryOperation.getAnswer();
    }


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

    /**
     * Returns the number of answer sub-collections.
     * @return the number of answer sub-collections
     */
    public abstract int getSubAnswerCount();

    /**
     * Returns an iterator over all objects in the answer sub-collection with the given index.
     * Note that the returned collection (typically) cannot be modified.
     * @param index the index of the answer sub-collection to return
     * @return an iterator over all objects in the answer sub-collection
     * @throws IndexOutOfBoundsException if the given index is negative or
     *          greater or equal to {@link #getSubAnswerCount()}
     */
    public abstract Iterator<? extends TAnswer> getSubAnswer(int index) throws IndexOutOfBoundsException;

    /**
     * Returns an iterator over all objects in the answer sub-collection with the given key.
     * If no collection is available for the given key, <tt>null</tt> is returned.
     * Note that the returned collection (typically) cannot be modified.
     * @param key the key of the answer sub-collection to return
     * @return the answer sub-collection with the given key or <tt>null</tt>
     */
    public abstract Iterator<? extends TAnswer> getSubAnswer(Object key);


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
