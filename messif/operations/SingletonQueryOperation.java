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

import java.util.Collections;
import messif.objects.AbstractObject;
import java.util.Iterator;
import messif.objects.util.CollectionProviders;


/**
 * The base class for query operations that return a single {@link AbstractObject object}.
 * These are, for example, operations that retrieve objects by ID or locator.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class SingletonQueryOperation extends QueryOperation<AbstractObject> {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Answer of this query, will be <tt>null</tt> if the object cannot be found */
    private AbstractObject answer;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of SingletonQueryOperation.
     * The object stored in answer is {@link AnswerType#CLEARED_OBJECTS cleared and cloned}.
     */
    protected SingletonQueryOperation() {
        this(AnswerType.CLEARED_OBJECTS);
    }

    /**
     * Creates a new instance of ListingQueryOperation.
     * @param answerType the type of object this operation stores in its answer
     */
    protected SingletonQueryOperation(AnswerType answerType) {
        super(answerType);
    }


    //****************** Cloning ******************//
    
    /**
     * Create a duplicate of this operation.
     * The answer of the query is not cloned.
     *
     * @return a clone of this operation
     * @throws CloneNotSupportedException if the operation instance cannot be cloned
     */
    @Override
    public SingletonQueryOperation clone() throws CloneNotSupportedException {
        SingletonQueryOperation operation = (SingletonQueryOperation)super.clone();
        operation.answer = null;
        return operation;
    }


    //****************** Answer access methods ******************//

    /**
     * Returns the class of objects this operation stores in its answer.
     * @return the class of objects this operation stores in its answer
     */
    @Override
    public Class<? extends AbstractObject> getAnswerClass() {
        return AbstractObject.class;
    }

    /**
     * Returns the number of objects in this query answer.
     * For this operation, only 0 or 1 can be returned.
     * @return the number of objects in this query answer
     */
    @Override
    public int getAnswerCount() { 
        return (answer == null)?0:1;
    }
    
    /**
     * Returns an iterator over all objects in the answer to this query.
     * @return an iterator over all objects in the answer to this query
     */
    @Override
    public Iterator<AbstractObject> getAnswer() {
        return getAnswer(0, 1);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<AbstractObject> getAnswer(int skip, int count) {
        if (answer == null || skip >= 1)
            return Collections.EMPTY_LIST.iterator();
        else
            return Collections.singleton(answer).iterator();
    }

    @Override
    public Iterator<AbstractObject> getAnswerObjects() {
        return getAnswer();
    }

    /**
     * Returns an object that is the answer to this query.
     * @return an object that is the answer to this query
     */
    public AbstractObject getAnswerObject() {
        return answer;
    }

   @Override
    public int getSubAnswerCount() {
        return CollectionProviders.getCollectionCount(answer);
    }

    @Override
    public Iterator<? extends AbstractObject> getSubAnswer(int index) throws IndexOutOfBoundsException {
        return CollectionProviders.getCollectionIterator(answer, index, getAnswerClass());
    }

    @Override
    public Iterator<? extends AbstractObject> getSubAnswer(Object key) {
        return CollectionProviders.getCollectionByKeyIterator(answer, key, getAnswerClass(), false);
    }


    //****************** Answer modification methods ******************//

    /**
     * Add an object to the answer.
     * The object is updated according to {@link #answerType}.
     * @param object the object to add
     * @return <code>true</code> if the <code>object</code> has been added to the answer. Otherwise <code>false</code>.
     * @throws IllegalArgumentException if the object cannot be added to the answer, e.g. because it cannot be cloned
     */
    public boolean addToAnswer(AbstractObject object) throws IllegalArgumentException {
        try {
            if (object == null)
                return false;
            answer = answerType.update(object);
            return true;
        } catch (CloneNotSupportedException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Reset the current query answer.
     */
    @Override
    public void resetAnswer() {
        answer = null;
    }

    /**
     * Update the error code and answer of this operation from another operation.
     * Answer of this operation is updated only it is not set yet.
     * @param operation the source operation from which to get the update
     * @throws IllegalArgumentException if the answer of the specified operation is incompatible with this one
     */
    @Override
    public void updateFrom(AbstractOperation operation) throws IllegalArgumentException {
        if (!(operation instanceof SingletonQueryOperation))
            throw new IllegalArgumentException(getClass().getSimpleName() + " cannot be updated from " + operation.getClass().getSimpleName());
        if (answer == null)
            addToAnswer(((SingletonQueryOperation)operation).answer);
        super.updateFrom(operation);
    }

}
