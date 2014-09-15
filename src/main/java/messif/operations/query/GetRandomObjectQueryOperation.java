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

import messif.objects.util.AbstractObjectIterator;
import messif.objects.LocalAbstractObject;
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;
import messif.operations.QueryOperation;
import messif.operations.SingletonQueryOperation;


/**
 * Operation for retriving a random object.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("Get random object query")
public class GetRandomObjectQueryOperation extends SingletonQueryOperation {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of GetRandomObjectQueryOperation.
     * {@link AnswerType#NODATA_OBJECTS} will be returned in the result.
     */
    @AbstractOperation.OperationConstructor({})
    public GetRandomObjectQueryOperation() {
        super();
    }

    /**
     * Creates a new instance of GetRandomObjectQueryOperation.
     * @param answerType the type of objects this operation stores in its answer
     */
    @AbstractOperation.OperationConstructor({"Answer type"})
    public GetRandomObjectQueryOperation(AnswerType answerType) {
        super(answerType);
    }


    //****************** Parameter access methods ******************//

    @Override
    public Object getArgument(int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException("GetRandomObjectQueryOperation has no arguments");
    }

    @Override
    public int getArgumentCount() {
        return 0;
    }


    //****************** Default implementation of query evaluation ******************//

    @Override
    public int evaluate(AbstractObjectIterator<? extends LocalAbstractObject> objects) {
        if (!objects.hasNext()) {
            return 0;
        } else {
            addToAnswer(objects.getRandomObject());
            return 1;
        }
    }


    //****************** Equality driven by operation data ******************//

    @Override
    protected boolean dataEqualsImpl(QueryOperation obj) {
        // The argument obj is always GetRandomObjectQueryOperation or its descendant, because it has only abstract ancestors
        return true;
    }

    @Override
    public int dataHashCode() {
        return 0;
    }

}
