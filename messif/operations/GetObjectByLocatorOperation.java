/*
 * GetObjectByLocator.java
 *
 * Created on 18.7.2007, 17:13:03
 */

package messif.operations;

import java.util.NoSuchElementException;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;


/**
 * This query retrieves from the structure a set of objects given their locators.
 * 
 * @author <a href="mailto:xnovak8@fi.muni.cz">xnovak8@fi.muni.cz</a> David Novak, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
@AbstractOperation.OperationName("Get object by locator")
public class GetObjectByLocatorOperation extends SingletonQueryOperation {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 2L;    

    //****************** Attributes ******************//

    /** The locator of the desired object */
    protected final String locator;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of GetObjectByLocatorOperation for a specified locator.
     * @param locator the locator to be searched by this operation
     */
    @AbstractOperation.OperationConstructor({"The object locator"})
    public GetObjectByLocatorOperation(String locator) {
        super();
        this.locator = locator;
    }

    /**
     * Creates a new instance of GetObjectByLocatorOperation for a specified locator.
     * @param answerType the type of objects this operation stores in its answer
     * @param locator the locator to be searched by this operation
     */
    protected GetObjectByLocatorOperation(AnswerType answerType, String locator) {
        super(answerType);
        this.locator = locator;
    }


    //****************** Attribute access ******************//

    /**
     * Returns the locator that this query searches for.
     * @return the locator that this query searches for
     */
    public String getLocator() {
        return locator;
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
        case 0:
            return locator;
        default:
            throw new IndexOutOfBoundsException("GetObjectByLocatorOperation has only one argument");
        }
    }

    /**
     * Returns number of arguments that were passed while constructing this instance.
     * @return number of arguments that were passed while constructing this instance
     */
    @Override
    public int getArgumentCount() {
        return 1;
    }


    //****************** Implementation of query evaluation ******************//

    /**
     * Evaluate this query on a given set of objects.
     * @param objects set of objects to evaluate the operation on
     * @return number of objects satisfying the query (should be zero or one object if the locator is unique)
     */
    @Override
    public int evaluate(AbstractObjectIterator<? extends LocalAbstractObject> objects) {
        // Iterate through all supplied objects
        try {
            addToAnswer(objects.getObjectByLocator(locator));
            return 1;
        } catch (NoSuchElementException e) {
            // If there was no object with the specified locator
            return 0;
        }
    }


    //****************** Equality driven by operation data ******************//

    /** 
     * Indicates whether some other operation has the same data as this one.
     * @param   obj   the reference object with which to compare.
     * @return  <code>true</code> if this object has the same data as the obj
     *          argument; <code>false</code> otherwise.
     */
    @Override
    protected boolean dataEqualsImpl(AbstractOperation obj) {
        // The argument obj is always GetObjectByLocatorOperation or its descendant, because it has only abstract ancestors
        return locator.equals(((GetObjectByLocatorOperation)obj).locator);
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return locator.hashCode();
    }

}
