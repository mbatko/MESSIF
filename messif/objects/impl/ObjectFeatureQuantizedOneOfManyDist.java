/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;

/**
 * Class for local image feature that is quantized to an array of long integers.
 * 
 * Distance function is implemented as equal if at least one pair of keys matches.
 * 
 * @author Vlastislav Dohnal, dohnal@fi.muni.cz
 * @author Tomáš Homola, xhomola@fi.muni.cz
 */
public class ObjectFeatureQuantizedOneOfManyDist extends ObjectFeatureQuantized {
    
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    public ObjectFeatureQuantizedOneOfManyDist() {
    }

    public ObjectFeatureQuantizedOneOfManyDist(float x, float y, float ori, float scl) {
        super(x, y, ori, scl);
    }

    public ObjectFeatureQuantizedOneOfManyDist(float x, float y, float ori, float scl, long[] keys) {
        super(x, y, ori, scl, keys);
    }

    public ObjectFeatureQuantizedOneOfManyDist(BufferedReader stream) throws IOException, NumberFormatException {
        super(stream);
    }

    public ObjectFeatureQuantizedOneOfManyDist(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }

    //****************** Distance function ******************

    /**
     * Metric function implemented as testing equality of at least one pair of keys.
     * @return <code>1</code> if at least one pair of keys between this and the passed object is equal, <code>0</code> otherwise.
     */
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        ObjectFeatureQuantizedOneOfManyDist o = (ObjectFeatureQuantizedOneOfManyDist)obj;
        
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] == o.keys[i])
                return 1;
        }
        return 0;
    }
    
}
