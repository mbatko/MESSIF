package messif.objects.impl;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
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
    private static final long serialVersionUID = 1021002L;

    //****************** Constants ******************//

    /** Vector of weights to be used for weighted L2 (distance between centroids) */
    private static final float[] defaultWeights = { 1f, 1f, 1f, 1f, 1f, 1f, 1f }; //to change impact of X, Y, L, a, b

    /** parameter of the Gaussian distance/similarity conversion */
    private static final float defaultAlpha = 1f;

    
    //****************  Additional (precomputed) data  ***************//
    
    /**
     * Sum of weights needed for every distance computation
     */
    private final float sumOfWeights;
    
    /**
     * Partially precomputed distance function (FS1)
     */
    protected float precomputedDist = Float.MIN_VALUE;
    
    /**
     * Alpha for which the distance was partially precomputed.
     */
    protected float precomputedAlpha = -1f;
    
    /**
     * Weights for which the distance was partially precomputed.
     */
    protected float[] precomputedWeights = defaultWeights;

    
    //****************** Constructors ******************//

    public ObjectFloatSQFDist(float[] data) {
        super(data);
        this.sumOfWeights = calculateSumOfWeights();
        precomputeSelfDistance();   
    }

    public ObjectFloatSQFDist(String locatorURI, float[] data) {
        super(locatorURI, data);
        this.sumOfWeights = calculateSumOfWeights();
        precomputeSelfDistance();   
    }

    public ObjectFloatSQFDist(BufferedReader stream) throws EOFException, IOException, NumberFormatException {
        super(stream);
        this.sumOfWeights = calculateSumOfWeights();
        precomputeSelfDistance();
    }

    protected ObjectFloatSQFDist(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        this.sumOfWeights = calculateSumOfWeights();
        precomputeSelfDistance();
    }

    /**
     * Compute and store self-distance (for default alpha and weights).
     * @return partial result of multiplication of corresponding parts of the vectors and matrix
     */
    protected final float precomputeSelfDistance() {
        return precomputeSelfDistance(getWeights(), getAlpha());
    }

    /**
     * Compute and store self-distance (fog given alpha).
     * 
     * @param weights vector of weights to be used for weighted L2 (distance between centroids)
     * @param alpha parameter of the Gaussian distance/similarity conversion
     * @return partial result of multiplication of corresponding parts of the vectors and matrix
     */
    protected final float precomputeSelfDistance(float[] weights, float alpha) {
        float retVal = computePartialResult(this, this, weights, alpha);
        setPrecomputedAlpha(alpha);
        setPrecomputedWeights(weights.clone());
        setPrecomputedDist(retVal);
        return retVal;
    }

    /**
     * Pre-compute sum of all clusters weights (needed for normalization during distance computations).
     * @return sum of cluster weights
     */
    private float calculateSumOfWeights() {
        float retVal = 0f;
        int d = getClusterDimension() + 1;
        for (int i = 0; i < getClusterCount(); i++) {
            retVal += data[2 + i * d];
        }
        return retVal;
    }

    //****************** Attribute access methods ******************//

    public int getClusterCount() {
        return (int)data[0];
    }

    public int getClusterDimension() {
        return (int)data[1];
    }

    public float getPrecomputedAlpha() {
        return precomputedAlpha;
    }

    public void setPrecomputedAlpha(float precomputedAlpha) {
        this.precomputedAlpha = precomputedAlpha;
    }

    public float getPrecomputedDist() {
        return precomputedDist;
    }

    public void setPrecomputedDist(float precomputedDist) {
        this.precomputedDist = precomputedDist;
    }

    public float[] getPrecomputedWeights() {
        return precomputedWeights;
    }

    public void setPrecomputedWeights(float[] precomputedWeights) {
        this.precomputedWeights = precomputedWeights;
    }

    protected float getSumOfWeights() {
        return sumOfWeights;
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
        ObjectFloatSQFDist other = (ObjectFloatSQFDist)obj;
        float[] weights = getWeights();
        float alpha = getAlpha();
        
        if (other.getClusterDimension() != this.getClusterDimension()) {
            throw new IllegalArgumentException("Dimension of all centroids has to be the same.");
        }
        
        // r - Gaussian function: f(ci,cj) = e^(-alpha*d^2(ci,cj))
        // compute SQFD = FS1 + FS2 - 2 * FS1FS2
        
        // FS1: either use precomputed or compute it
        float result = other.getOrPrecomputeSelfDistance(weights, alpha);
        
        // FS2: either use precomputed or compute it
        result += this.getOrPrecomputeSelfDistance(weights, alpha);
        
        // from symmetry of the matrix use - 2 * wi * aij * wj
        result -= 2 * computePartialResult(other, this, weights, alpha) ;
                
        if (result < 0.00000000001) 
            return 0; // for rounding mistakes
        return (float)Math.sqrt(result);
    }

    /**
     * Either retrieve stored precomputed self-distance or compute it and store (fog given alpha).
     * @param weights vector of weights to be used for weighted L2 (distance between centroids)
     * @param alpha parameter of the Gaussian distance/similarity conversion
     * @return partial result of multiplication of corresponding parts of the vectors and matrix
     */
    protected float getOrPrecomputeSelfDistance(float[] weights, float alpha) {
        if (alpha == getPrecomputedAlpha() && Arrays.equals(weights, getPrecomputedWeights())) {
            return getPrecomputedDist();
        }
        return precomputeSelfDistance(weights, alpha);
    }
    
    /**
     * Pre-compute partial distance between two signatures using the matrix based on centroid distances.
     * @param obj1 first signature object
     * @param obj2 second signature object
     * @param weights vector of weights to be used for weighted L2 (distance between centroids)
     * @param alpha parameter of the Gaussian distance/similarity conversion
     * @return partial result of multiplication of corresponding parts of the vectors and matrix
     */
    protected static float computePartialResult(ObjectFloatSQFDist obj1, ObjectFloatSQFDist obj2, float[] weights, float alpha) {
        float retVal = 0;
        
        int d = obj1.getClusterDimension() + 1;
        float div = obj1.getSumOfWeights() * obj2.getSumOfWeights();
        double r = 0;
        for (int i=0; i < obj1.getClusterCount(); i++) {
            for (int j=0; j < obj2.getClusterCount(); j++) {
                r = 0;
                for (int k=0; k < (d-1); k++) {
                    float diff = obj1.data[3 + i * d + k] - obj2.data[3 + j * d + k];
                    r += weights[k] * diff * diff;
                }
                r = Math.exp((-alpha) * r);
                retVal += obj1.data[2 + i * d] * obj2.data[2 + j * d] * r / div;
            }
        }
        return retVal;
    }
    
    /**
     * Object that allows to set weights and alpha for SQFD object.
     */
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

    /**
     * Special extension of the {@link ObjectFloatSQFDist} that reads the original CSV data format.
     * It also allows to set the alpha and weights in a static manner, but this MUST be set prior to indexing!
     */
    public static class ObjectFloatSQFDistOrigData extends ObjectFloatSQFDist {
        private static final long serialVersionUID = 1L;
        private static float[] weights = defaultWeights;
        private static float alpha = defaultAlpha;

        public ObjectFloatSQFDistOrigData(BufferedReader stream) throws IOException {
            this(parseStreamLine(stream));
        }

        private ObjectFloatSQFDistOrigData(String[] stringData) {
            super(stringData[0], convertFloatVector(stringData, 1, stringData.length - 1));
        }

        private static String[] parseStreamLine(BufferedReader stream) throws IOException {
            String line = stream.readLine();
            if (line == null)
                throw new EOFException();
            return line.replaceAll(",(?=\\d)", ".").split(", ");
        }

        @Override
        public float getAlpha() {
            return alpha;
        }

        public static void setAlpha(float alpha) {
            ObjectFloatSQFDistOrigData.alpha = alpha;
        }

        @Override
        public float[] getWeights() {
            return weights.clone();
        }

        public static void setWeights(float[] weights) {
            ObjectFloatSQFDistOrigData.weights = weights.clone();
        }
    }
}
