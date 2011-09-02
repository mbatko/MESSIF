package messif.objects.impl;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;

/**
 * First version, just VB code rewritten into JAVA
 * @author Andreas, nevelik@gmail.com
 */
public class ObjectFloatSQFDist extends ObjectFloatVector {
    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** Constants ******************//

    private static final float[] defaultWeights = { 1f, 1f, 1f, 1f, 1f, 1f, 1f }; //to change impact of X, Y, L, a, b
    private static final float defaultAlpha = 0.1f;


    //****************** Constructors ******************//

    public ObjectFloatSQFDist(float[] data) {
        super(data);
    }

    public ObjectFloatSQFDist(BufferedReader stream) throws EOFException, IOException, NumberFormatException {
        super(stream);
    }

    protected ObjectFloatSQFDist(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }


    //****************** Attribute access methods ******************//

    public int getClusterCount() {
        return (int)data[0];
    }

    public int getClusterDimension() {
        return (int)data[1];
    }


    //****************** Distance function setup ******************//

    public float getAlpha() {
        return defaultAlpha;
    }

    public float[] getWeights() {
        return defaultWeights.clone();
    }


    //****************** Distance function ******************//

    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        ObjectFloatSQFDist fs1 = (ObjectFloatSQFDist)obj;
        float[] weights = getWeights();
        float alpha = getAlpha();
        
        // Id, number of points, dimension, w1, p1, ..., pdim, wdim, ...
        int length1 = fs1.getClusterCount();
        int dim1 = fs1.getClusterDimension();

        if (dim1 != this.getClusterDimension()) {
            throw new IllegalArgumentException("Dimension of all centroids has to be the same.");
        }

        // compute sum of weights
        int d = dim1 + 1;
        double r;
        float div = 0, div1 = 0, div2 = 0, result = 0;
        for (int i = 0; i < length1; i++) {
            div1 += fs1.data[2 + i * d];
        }
        for (int i = 0; i < this.getClusterCount(); i++) {
            div2 += this.data[2 + i * d];
        }
        
        // r - Gaussian function: f(ci,cj) = e^(-alpha*d^2(ci,cj))
        
        // compute SQFD - FS1 + FS2 - 2 * FS1FS2
        div = div1 * div1;
     
        for (int i=0; i<length1; i++) {
            for (int j=0; j<length1; j++) {
                r = 0;
                for (int k=0; k<(d-1); k++) {
                    float diff = fs1.data[3 + i * d + k] - fs1.data[3 + j * d + k];
                    r += weights[k] * diff * diff;
                }
                r = Math.exp((-alpha) * r);
                result += fs1.data[2 + i * d] * fs1.data[2 + j * d] * r / div;
            }
        }
        
        div = div2 * div2;
        for (int i=0; i<this.getClusterCount(); i++) {
            for (int j=0; j<this.getClusterCount(); j++) {
                r = 0;
                for (int k=0; k< d - 1; k++) {
                    float diff = this.data[3 + i * d + k] - this.data[3 + j * d + k];
                    r += weights[k] * diff * diff;
                }
                r = Math.exp((-alpha) * r);
                result += this.data[2 + i * d] * this.data[2 + j * d] * r / div;
            }
        }
        
        // from symmetry of the matrix use - 2 * wi * aij * wj
        div = div1 * div2;
        for (int i=0; i<length1; i++) {
            for (int j=0; j<this.getClusterCount(); j++) {
                r = 0;
                for (int k=0; k<d-1; k++) {
                    float diff = fs1.data[3 + i * d + k] - this.data[3 + j * d + k];
                    r += weights[k] * diff * diff; 
                }
                r = Math.exp((-alpha) * r);
                result -= 2 * fs1.data[2 + i * d] * this.data[2 + j * d] * r /div;
            }
        }
        
        if (result < 0.0000000000001) return 0; // for rounding mistakes
        return (float)Math.sqrt(result);
    }

    public static class ObjectFloatSQFDistWeights extends ObjectFloatSQFDist {
        private static final long serialVersionUID = 1L;
        private final float[] weights;
        private final float alpha;

        public ObjectFloatSQFDistWeights(ObjectFloatVector object, float[] weights, float alpha) {
            super(object.data);
            this.weights = weights.clone();
            this.alpha = alpha;
        }

        @Override
        public float getAlpha() {
            return alpha;
        }

        @Override
        public float[] getWeights() {
            return weights.clone();
        }

    }

    public static MetaObject replaceSQFDistObject(MetaObject object, String objectName, float[] weights, float alpha) {
        if (!object.containsObject(objectName))
            throw new IllegalArgumentException("Object '" + objectName + "' is not in " + object);
        Map<String, LocalAbstractObject> objects = new HashMap<String, LocalAbstractObject>(object.getObjectMap());
        objects.put(objectName, new ObjectFloatSQFDistWeights((ObjectFloatVector)objects.get(objectName), weights, alpha));
        return new MetaObjectMap(object.getLocatorURI(), objects);
    }

    public static MetaObject replaceSQFDistObject(MetaObject object, String objectName, String weights, float alpha) throws EOFException {
        if (weights == null || weights.isEmpty() || alpha == 0)
            return object;
        return replaceSQFDistObject(object, objectName, parseFloatVector(weights), alpha);
    }
}
