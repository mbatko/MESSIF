
package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;

/**
 * Implements the Jaccard coeficient distance function. The data is
     *  expected to be sorted and without duplicites!
 * @author david
 */
public class ObjectIntSortedVectorJaccard extends ObjectIntSortedVector implements Serializable {

    /** Class id for serialization. */
    private static final long serialVersionUID = 20501L;


    //****************** Constructors ******************

    /**
     * Creates a new instance of object - make sure the data is sorted
     * @param data int vector data
     * @param forceSort if false, the data is expected to be sorted
     */
    public ObjectIntSortedVectorJaccard(int[] data, boolean forceSort) {
        super(data, forceSort);
    }

    /**
     * Creates a new instance of object - make sure the data is sorted
     * @param data int vector data
     */
    public ObjectIntSortedVectorJaccard(int[] data) {
        super(data);
    }

    /**
     * Creates a new instance of randomly generated object
     * @param dimension vector dimensionality
     */
    public ObjectIntSortedVectorJaccard(int dimension) {
        super(dimension);
    }


    /**
     * Creates a new instance of Object from stream - force sort of the data
     * @param stream text stream to read the data from
     * @throws IOException when an error appears during reading from given stream.
     *  or  EOFException when eof of the given stream is reached.
     * @throws NumberFormatException when the line read from given stream does
     * not consist of comma-separated or space-separated numbers.
     */
    public ObjectIntSortedVectorJaccard(BufferedReader stream) throws IOException, NumberFormatException {
        super(stream);
    }

    /**
     * Creates a new instance of ObjectIntSortedVector loaded from binary input buffer - it gotta be sorted already.
     *
     * @param input the buffer to read the ObjectIntVector from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectIntSortedVectorJaccard(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }

    /**
     * Implements the Jaccard coeficient distance function. The data is
     *  expected to be sorted and without duplicites!
     * @param obj
     * @param distThreshold
     * @return 0, if both sets are emtpy; 1, if only one set is empty; Jaccard distance otherwise
     */
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        int [] objData = ((ObjectIntSortedVector) obj).data;

        int thisLastIndex = this.data.length - 1;
        int objLastIndex = objData.length - 1;

        // return 0, if both sets are emtpy; 1, if only one set is empty
        if ((thisLastIndex == -1) || (objLastIndex == -1)) {
            return (thisLastIndex == objLastIndex) ? 0f : 1f;
        }

        float unionCount = 0;
        float intersectCount = 0;

        // keep a pointer in each of the data arrays
        int thisPointer = 0;
        int objPointer = 0;

        do {
            if ((objPointer > objLastIndex) || ((thisPointer <= thisLastIndex) && (this.data[thisPointer] < objData[objPointer]))) {
                unionCount ++;
                thisPointer ++;
                continue;
            }
            if ((thisPointer > thisLastIndex) || (this.data[thisPointer] > objData[objPointer])) {
                unionCount ++;
                objPointer ++;
                continue;
            }
            unionCount ++;
            intersectCount ++;
            thisPointer ++;
            objPointer ++;
        } while ((thisPointer <= thisLastIndex) || (objPointer <= objLastIndex));

        return 1f - (intersectCount / unionCount);
    }

}
