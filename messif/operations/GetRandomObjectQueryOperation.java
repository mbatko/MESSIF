/*
 * GetRandomObjectQueryOperation
 *
 */

package messif.operations;

import messif.objects.util.AbstractObjectIterator;
import messif.objects.LocalAbstractObject;


/**
 * Operation for retriving a random object.
 *
 * @author xbatko
 */
@AbstractOperation.OperationName("Get random object query")
public class GetRandomObjectQueryOperation extends SingletonQueryOperation {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of GetRandomObjectQueryOperation.
     * {@link AnswerType#REMOTE_OBJECTS} will be returned in the result.
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
    protected boolean dataEqualsImpl(AbstractOperation obj) {
        // The argument obj is always GetRandomObjectQueryOperation or its descendant, because it has only abstract ancestors
        return true;
    }

    @Override
    public int dataHashCode() {
        return 0;
    }

}
