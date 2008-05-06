/*
 * ObjectFaceDescriptor.java
 * 
 * Created on 25.10.2007, 14:49:59
 *
 */

package messif.objects.impl;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import messif.objects.LocalAbstractObject;

/**
 * This class encapsulates a face recognition descriptor. The values from
 * eigenface extraction method are stored and a weighted L2 metric is used.
 *
 * Extraction remarks:
 * The face images should be normalized before feature extraction.
 * The positions of two eyes should be at (24,16) and (24,31) in the scaled image(56 pixels in height and 46 pixels in width).
 *
 * @author xbatko
 */
public class ObjectFaceDescriptor extends ObjectIntVector {
    
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of ObjectFaceDescriptor from provided data.
     * @param data the eigen features extracted from face
     * @throws IllegalArgumentException if the provided data is not valid
     */
    public ObjectFaceDescriptor(int[] data) throws IllegalArgumentException {
        super(data);
        if (data.length < 48)
            throw new IllegalArgumentException("Face descriptor must have 48 values");
    }
    
    //****************** Text file store/retrieve methods ******************
    
    /**
     * Creates a new instance of ObjectFaceDescriptor from stream.
     * @param stream the stream to read object's data from
     * @throws IOException if there was an error during reading from the given stream
     * @throws EOFException when end-of-file of the given stream is reached
     * @throws NumberFormatException when the line read from given stream does not consist of comma-separated or space-separated numbers
     * @throws IllegalArgumentException if the read data is not valid
     */
    public ObjectFaceDescriptor(BufferedReader stream) throws IOException, EOFException, NumberFormatException, IllegalArgumentException {
        super(stream);
        if (data.length < 48)
            throw new IllegalArgumentException("Face descriptor must have 48 values");
    }


    /****************** Distance function ******************/

    /** Weights for metric function */
    private static int[] weight = {8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 16, 16, 16, 16, 16, 16, 16, 16, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32};

    /**
     * Metric distance function for face descriptors.
     *
     * @param obj the object to compute distance to
     * @param distThreshold the threshold value on the distance
     * @return the actual distance between obj and this if the distance is lower than distThreshold.
     *         Otherwise the returned value is not guaranteed to be exact, but in this respect the returned value
     *         must be greater than the threshold distance.
     */
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        ObjectFaceDescriptor castObj = (ObjectFaceDescriptor)obj;
        
        float dist = 0;
        for( int i=0; i < 48; i++)
            dist += weight[i]*Math.pow(data[i] - castObj.data[i], 2);

        return (float)Math.sqrt(dist);
    }

}
