/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package messif.objects.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;

/**
 *
 * @author homola.jr
 */
public class ObjectFeatureSetSumOfSimilar extends ObjectFeatureSet {

    private static final long serialVersionUID = 1L;

    /**
     * Two objects with mutual distance less or equal to this limit will be cosidered as equal
     */
    private float equalityThreshold = 0.0f;

    /**
     * Creates a new ObjectFeatureSetSumOfSimilar with empty list of objects and a~default threshold
     */
    public ObjectFeatureSetSumOfSimilar () {
        super ();
    }

    /**
     * Creates a new instance of ObjectFeatureSetSumOfSimilar from a text stream.
     * @param stream the text stream to read an object from
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public ObjectFeatureSetSumOfSimilar (BufferedReader stream) throws IOException {
        super(stream);
    }

    /**
     * Creates a new instance of ObjectFeatureSetSumOfSimilar from a text stream and sets the threshold.
     * @param stream the text stream to read an object from
     * @param equalityThreshold  new equality threshold
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public ObjectFeatureSetSumOfSimilar (BufferedReader stream, float equalityThreshold) throws IOException {
        super(stream);
        this.equalityThreshold = equalityThreshold;
    }

    /**
      * Creates a new instance of ObjectFeatureSetSumOfSimilar from a file
      * @param file text file to read an object from
      * @throws IOException when an error appears during reading from given stream,
      *         EOFException is returned if end of the given stream is reached.
      */
    public ObjectFeatureSetSumOfSimilar(File file) throws IOException {
        this(new BufferedReader(new InputStreamReader(new FileInputStream(file))));
    }


    /**
      * Creates a new instance of ObjectFeatureSetSumOfSimilar from a file and sets equality threshold
      * @param file text file to read an object from
      * @param equalityThreshold new equality threshold
      * @throws IOException when an error appears during reading from given stream,
      *         EOFException is returned if end of the given stream is reached.
      */
    public ObjectFeatureSetSumOfSimilar(File file, float equalityThreshold) throws IOException {
        this(new BufferedReader(new InputStreamReader(new FileInputStream(file))));
        this.equalityThreshold = equalityThreshold;
    }

    public ObjectFeatureSetSumOfSimilar (BinaryInput input, BinarySerializator serializator) throws IOException {
        super (input, serializator);
    }

    public ObjectFeatureSetSumOfSimilar (ObjectFeatureSet superSet, float minX, float maxX, float minY, float maxY) {
        super (superSet, minX, maxX, minY, maxY);
    }

    /**
     * Sets the equality threshold
     * @param equalityThreshold 
     */
    public void setParameters (float equalityThreshold) {
        this.equalityThreshold = equalityThreshold;
    }

    /**
     * Returns the equality threshold
     * @return  equalityThreshold
     */
    public float getThreshold () {
        return equalityThreshold;
    }

    /**
     * The actual implementation of the distance function
     * 
     * @param o the object to compute distance to
     * @param distThreshold the threshold value on the distance (ignored)
     * @return the actual distance between obj and this
     * @see LocalAbstractObject#getDistance
     */
    @Override
    public float getDistanceImpl (LocalAbstractObject o, float distThreshold) {
        ObjectFeatureSet obj = (ObjectFeatureSet) o;
        int n = getObjectCount();
        int m = obj.getObjectCount();

        if (n == 0)
            return 0;
        if (m == 0)
            return 0;

        int soucet = 0;

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (getObject(j).getDistance(obj.getObject(i)) <= equalityThreshold)
                    soucet++;
            }
        }
        return soucet;
    }

}
