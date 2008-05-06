/*
 * GetAllObjectsQueryOperation.java
 *
 * Created on 6. kveten 2004, 17:31
 */

package messif.operations;

import messif.objects.AbstractObject;
import messif.objects.GenericAbstractObjectIterator;
import messif.objects.GenericAbstractObjectList;
import messif.objects.LocalAbstractObject;
import messif.objects.MeasuredAbstractObjectList;
import java.util.Iterator;
import messif.objects.GenericObjectIterator;


/**
 * Operation for retrieving all objects locally stored (organized by an algorithm).
 * Is is usually applied to centralized algorithms only.
 *
 * @author  xbatko
 */
@AbstractOperation.OperationName("Get all objects query")
public class GetAllObjectsQueryOperation extends QueryOperation {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;


    /****************** Query answer attributes ******************/

    /** The answer list of this operation */
    protected final GenericAbstractObjectList<AbstractObject> answer = new GenericAbstractObjectList<AbstractObject>();


    /****************** Constructors ******************/

    /** Creates a new instance of GetAllObjectsQuery */
    @AbstractOperation.OperationConstructor({})
    public GetAllObjectsQueryOperation() {
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
        throw new IndexOutOfBoundsException("GetAllObjectsQueryOperation has no arguments");
    }

    /**
     * Returns number of arguments that were passed while constructing this instance.
     * @return number of arguments that were passed while constructing this instance
     */
    @Override
    public int getArgumentCount() {
        return 0;
    }


    /**
     * Clear non-messif data stored in operation.
     * This method is intended to be called whenever the operation is
     * sent back to client in order to minimize problems with unknown
     * classes after deserialization.
     */
    @Override
    public void clearSuplusData() {
        super.clearSuplusData();
        for (AbstractObject obj : answer) {
            obj.clearSurplusData();
        }
    }

    /****************** Default implementation of query evaluation ******************/

    /**
     * Evaluate this query on a given set of objects.
     * The objects found by this evaluation are added to answer of this query via {@link #addToAnswer}.
     *
     * @param objects the collection of objects on which to evaluate this query
     * @return number of objects satisfying the query
     */
    public int evaluate(GenericObjectIterator<LocalAbstractObject> objects) {
        int count = 0;
        while (objects.hasNext()) {
            LocalAbstractObject object = objects.next();
            addToAnswer(object, LocalAbstractObject.UNKNOWN_DISTANCE);
            count++;
        }
        return count;
    }


    /****************** Answer methods ******************/

    /**
     * Returns the number of objects in this query answer.
     * @return the number of objects in this query answer
     */
    public int getAnswerCount() { 
        return answer.size();
    }

    /**
     * Returns an iterator over all objects in the answer to this query.
     * @return an iterator over all objects in the answer to this query
     */
    public Iterator<AbstractObject> getAnswer() { 
        return answer.iterator();
    }

    /**
     * Returns an iterator over pairs of objects and their distances from the query object of this query. 
     * The object of a pair is accessible through {@link messif.objects.MeasuredAbstractObjectList.Pair#getObject}.
     * The associated distance of a pair is accessible through {@link messif.objects.MeasuredAbstractObjectList.Pair#getDistance}.
     * 
     * @return an iterator over pairs of objects and their distances from the query object of this query
     */
    public Iterator<MeasuredAbstractObjectList.Pair<AbstractObject>> getAnswerDistances() {
        final GenericAbstractObjectIterator<AbstractObject> iterator = answer.iterator();
        return new Iterator<MeasuredAbstractObjectList.Pair<AbstractObject>>() {
            public boolean hasNext() {
                return iterator.hasNext();
            }

            public MeasuredAbstractObjectList.Pair<AbstractObject> next() {
                return new MeasuredAbstractObjectList.Pair<AbstractObject>(iterator.next(), 0.0f);
            }

            public void remove() {
                throw new UnsupportedOperationException("Cannot delete from operation answer");
            }
        };
    }


    /**
     * Add an object with a measured distance to the answer.
     * 
     * @param object the object to add
     * @param distance the distance of the object
     * @return <code>true</code> if the <code>object</code> has been added to the answer. Otherwise <code>false</code>.
     */
    public boolean addToAnswer(AbstractObject object, float distance) { 
        if (object instanceof LocalAbstractObject)
            try {
                answer.add(((LocalAbstractObject) object).clone(false));
            } catch (CloneNotSupportedException e) {
                answer.add(object.getRemoteAbstractObject());
            }
        else 
            answer.add(object);
        return true;
    }

    /**
     * Reset the current query answer.
     */
    @Override
    public void resetAnswer() {
        answer.clear();
    }

    /**
     * Returns a string representation of this operation.
     * @return a string representation of this operation.
     */
    @Override
    public String toString() {
        return new StringBuffer("Get all objects query").append(" returned ").append(getAnswerCount()).append(" objects").toString();
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
        // The argument obj is always GetAllObjectsQueryOperation or its descendant, because it has only abstract ancestors
        return true;
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return 0;
    }

}
