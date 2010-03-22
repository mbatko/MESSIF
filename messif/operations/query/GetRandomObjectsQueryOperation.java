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
 * @author xbatko
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
     * {@link AnswerType#REMOTE_OBJECTS} will be returned in the result.
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
