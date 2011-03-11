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

import messif.objects.AbstractObject;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectList;
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;
import messif.operations.ListingQueryOperation;


/**
 * Operation for retriving a list of random objects.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("Get list of random objects query")
public class GetRandomObjectsQueryOperation extends ListingQueryOperation {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Number of random objects to retrieve */
    private final int count;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of GetRandomObjecstQueryOperation.
     * {@link AnswerType#NODATA_OBJECTS} will be returned in the result.
     * @param count the number of random objects to retrieve
     */
    @AbstractOperation.OperationConstructor({"Number of random objects"})
    public GetRandomObjectsQueryOperation(int count) {
        super();
        this.count = count;
    }

    /**
     * Creates a new instance of GetRandomObjectsQueryOperation.
     * @param count the number of random objects to retrieve
     * @param answerType the type of objects this operation stores in its answer
     */
    @AbstractOperation.OperationConstructor({"Number of random objects", "Answer type"})
    public GetRandomObjectsQueryOperation(int count, AnswerType answerType) {
        super(answerType);
        this.count = count;
    }


    //****************** Parameter access methods ******************//

    @Override
    public Object getArgument(int index) throws IndexOutOfBoundsException {
        if (index == 0)
            return count;
        throw new IndexOutOfBoundsException("GetRandomObjectsQueryOperation has only one argument");
    }

    @Override
    public int getArgumentCount() {
        return 1;
    }

    /** Returns the number of objects to be returned by this operation */
    public int getCount() {
        return count;
    }

    //****************** Default implementation of query evaluation ******************//

    @Override
    public int evaluate(AbstractObjectIterator<? extends LocalAbstractObject> objects) {
        if (getAnswerCount() >= count)
            return 0;
        AbstractObjectList<? extends LocalAbstractObject> randomObjects = objects.getRandomObjects(count - getAnswerCount(), false);
        for (LocalAbstractObject obj : randomObjects)
            addToAnswer(obj);
        return randomObjects.size();
    }

    @Override
    public boolean addToAnswer(AbstractObject object) throws IllegalArgumentException {
        if (getAnswerCount() >= count)
            return false;
        return super.addToAnswer(object);
    }


    //****************** Equality driven by operation data ******************//

    @Override
    protected boolean dataEqualsImpl(AbstractOperation obj) {
        return count == ((GetRandomObjectsQueryOperation)obj).count;
    }

    @Override
    public int dataHashCode() {
        return count;
    }

}
