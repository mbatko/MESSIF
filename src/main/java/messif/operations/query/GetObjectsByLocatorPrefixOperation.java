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

import java.util.NoSuchElementException;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.util.RankedAbstractObject;
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;
import messif.operations.QueryOperation;
import messif.operations.RankingSingleQueryOperation;

/**
 * This operation returns objects with locator that have a given prefix.
 * 
  * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
  * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
  * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
*/
@AbstractOperation.OperationName("Get objects by locator prefix")
public class GetObjectsByLocatorPrefixOperation extends RankingSingleQueryOperation {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** The locator prefix of the desired objects */
    protected final String locatorPrefix;


    //****************** Constructors ******************//

    /**
     * Create a new instance of GetObjectsByLocatorPrefixOperation with the specified locator prefix.
     * 
     * @param locatorPrefix the locator prefix to search for
     * @param queryObjectForDistances the query object to use for computing distances
     * @param answerType the type of objects this operation stores in its answer
     * @param maxAnswerSize the limit for the number of objects kept in this operation answer
     */
    @AbstractOperation.OperationConstructor({"Locator prefix", "The object to compute answer distances to", "Answer type", "Limit for number of objects in answer"})
    public GetObjectsByLocatorPrefixOperation(String locatorPrefix, LocalAbstractObject queryObjectForDistances, AnswerType answerType, int maxAnswerSize) {
        super(queryObjectForDistances, answerType, maxAnswerSize);
        if (locatorPrefix == null)
            throw new NullPointerException();
        this.locatorPrefix = locatorPrefix;
    }

    /**
     * Create a new instance of GetObjectsByLocatorPrefixOperation with the specified locator prefix.
     * 
     * @param locatorPrefix the locator prefix to search for
     * @param queryObjectForDistances the query object to use for computing distances
     * @param answerType the type of objects this operation stores in its answer
     */
    @AbstractOperation.OperationConstructor({"Locator prefix", "The object to compute answer distances to", "Answer type"})
    public GetObjectsByLocatorPrefixOperation(String locatorPrefix, LocalAbstractObject queryObjectForDistances, AnswerType answerType) {
        this(locatorPrefix, queryObjectForDistances, answerType, Integer.MAX_VALUE);
    }

    /**
     * Create a new instance of GetObjectsByLocatorPrefixOperation with the specified locator prefix.
     * 
     * @param locatorPrefix the locator prefix to search for
     * @param queryObjectForDistances the query object to use for computing distances
     */
    @AbstractOperation.OperationConstructor({"Locator prefix", "The object to compute answer distances to"})
    public GetObjectsByLocatorPrefixOperation(String locatorPrefix, LocalAbstractObject queryObjectForDistances) {
        this(locatorPrefix, queryObjectForDistances, AnswerType.NODATA_OBJECTS, Integer.MAX_VALUE);
    }

    /**
     * Create a new instance of GetObjectsByLocatorPrefixOperation with the specified locator prefix.
     * 
     * @param locatorPrefix the locator prefix to search for
     */
    @AbstractOperation.OperationConstructor({"Locator prefix"})
    public GetObjectsByLocatorPrefixOperation(String locatorPrefix) {
        this(locatorPrefix, null);
    }


    //****************** Attribute access ******************//

    @Override
    public Object getArgument(int index) throws IndexOutOfBoundsException {
        switch (index) {
        case 0:
            return locatorPrefix;
        case 1:
            return getQueryObject();
        default:
            throw new IndexOutOfBoundsException("GetObjectsByLocatorPrefixOperation has maximally two arguments");
        }
    }

    @Override
    public int getArgumentCount() {
        return getQueryObject() == null ? 1 : 2;
    }

    /**
     * Returns the locator prefix this query searches for.
     * @return the locator prefix this query searches for
     */
    public String getLocatorPrefix() {
        return locatorPrefix;
    }


    //****************** Implementation of query evaluation ******************//
    
    @Override
    public int evaluate(AbstractObjectIterator<? extends LocalAbstractObject> objects) {
        int count = 0;
        try {
            while (objects.hasNext()) {
                addToAnswer(objects.getObjectByLocator(locatorPrefix, true));
                count++;
            }
        } catch (NoSuchElementException e) { // Search ended, there are no more objects in the bucket
        }

        return count;
    }


    //****************** Overrides for answer set ******************//

    @Override
    public Class<? extends RankedAbstractObject> getAnswerClass() {
        return RankedAbstractObject.class;
    }


    //****************** Equality driven by operation data ******************//

    @Override
    protected boolean dataEqualsImpl(QueryOperation obj) {
        // The argument obj is always DeleteOperation or its descendant, because it has only abstract ancestors
        return locatorPrefix.equals(((GetObjectsByLocatorPrefixOperation)obj).locatorPrefix);
    }

    @Override
    public int dataHashCode() {
        return locatorPrefix.hashCode();
    }

}
