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
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;

/**
 * Signatures 
 *
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ObjectSignatureSQFD extends ObjectFloatVector {
    
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
    private transient final float sumOfWeights;
    
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
    protected float[] precomputedWeights = null;

    /**
     * Number of clusters.
     */
    protected final int nClusters;
    
    /**
     * Dimensionality of the clusters
     */
    protected final int nDim;
    
    //****************** Constructors ******************//

    public ObjectSignatureSQFD(int nClusters, int nDim, float[] data) {
        super(data);
        this.nClusters = nClusters;
        this.nDim = nDim;
        this.sumOfWeights = calculateSumOfWeights();
        precomputeSelfDistance();   
    }

    public ObjectSignatureSQFD(String locatorURI, int nClusters, int nDim, float[] data) {
        super(locatorURI, data);
        this.nClusters = nClusters;
        this.nDim = nDim;
        this.sumOfWeights = calculateSumOfWeights();
        precomputeSelfDistance();   
    }

//    public ObjectSignatureSQFD(BufferedReader stream) throws EOFException, IOException, NumberFormatException {
//        this(stream, true);
//    }
//
//    public ObjectSignatureSQFD(BufferedReader stream, boolean precomputeSelfDistance) throws EOFException, IOException, NumberFormatException {
//        super(stream);
//        this.sumOfWeights = calculateSumOfWeights();
//        if (precomputeSelfDistance) {
//            precomputeSelfDistance();
//        }
//    }

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
            retVal += data[i * d];
        }
        return retVal;
    }

    //****************** Attribute access methods ******************//

    public int getClusterCount() {
        return nClusters;
    }

    public int getClusterDimension() {
        return nDim;
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

    protected float[] getWeights() {
        return defaultWeights;
    }

    public float[] getWeightsClone() {
        return defaultWeights.clone();
    }
    
    @Override
    public float getMaxDistance() {
        return 1f;
    }    

    //****************** Distance function ******************//

    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        ObjectSignatureSQFD other = (ObjectSignatureSQFD)obj;
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
                
        if (result < 0.0000000001f) 
            return 0f; // for rounding mistakes
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
    protected static float computePartialResult(ObjectSignatureSQFD obj1, ObjectSignatureSQFD obj2, float[] weights, float alpha) {
        float retVal = 0;
        
        int d = obj1.getClusterDimension() + 1;
        float div = obj1.getSumOfWeights() * obj2.getSumOfWeights();
        double r;
        for (int i=0; i < obj1.getClusterCount(); i++) {
            for (int j=0; j < obj2.getClusterCount(); j++) {
                r = 0;
                for (int k=0; k < (d-1); k++) {
                    float diff = obj1.data[1 + i * d + k] - obj2.data[1 + j * d + k];
                    r += weights[k] * diff * diff;
                }
                r = Math.exp((-alpha) * r);
                retVal += obj1.data[i * d] * obj2.data[j * d] * r / div;
            }
        }
        return retVal;
    }

    
    
    protected ObjectSignatureSQFD(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        this.nClusters = serializator.readInt(input);
        this.nDim = serializator.readInt(input);
        this.precomputedDist = serializator.readFloat(input);
        this.precomputedAlpha = serializator.readFloat(input);
        this.precomputedWeights = serializator.readFloatArray(input);
        this.sumOfWeights = calculateSumOfWeights();
    }
    

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator)
               + serializator.write(output, nClusters) + serializator.write(output, nDim)
               + serializator.write(output, precomputedDist) + serializator.write(output, precomputedAlpha) + serializator.write(output, precomputedWeights) ;
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return  super.getBinarySize(serializator) + 
               + serializator.getBinarySize(nClusters) + serializator.getBinarySize(nDim)
               + serializator.getBinarySize(precomputedDist) + serializator.getBinarySize(precomputedAlpha) + serializator.getBinarySize(precomputedWeights) ;
    }

    
}
