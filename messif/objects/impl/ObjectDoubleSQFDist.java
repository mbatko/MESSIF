package messif.objects.impl;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;

/**
 * First version, just VB code rewritten into JAVA
 * @author Andreas, nevelik@gmail.com
 */
public class ObjectDoubleSQFDist extends ObjectFloatVector {
    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** Distance function setup ******************//

    private static float alpha = 0.1f;
    private static float[] weights = {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f}; //to change impact of X, Y, L, a, b

    public static double getAlpha() {
        return alpha;
    }

    public static void setAlpha(float alpha) {
        ObjectDoubleSQFDist.alpha = alpha;
    }

    public static float[] getWeights() {
        return weights.clone();
    }

    public static void setWeights(float[] weights) {
        ObjectDoubleSQFDist.weights = weights.clone();
    }


    //****************** Constructors ******************//

    public ObjectDoubleSQFDist(float[] data) {
        super(data);
    }

    public ObjectDoubleSQFDist(BufferedReader stream) throws EOFException, IOException, NumberFormatException {
        super(stream);
    }

    protected ObjectDoubleSQFDist(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }


    //****************** Attribute access methods ******************//

    public int getClusterCount() {
        return (int)data[0];
    }

    public int getClusterDimension() {
        return (int)data[1];
    }


    //****************** Distance function ******************//

    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        ObjectDoubleSQFDist fs1 = (ObjectDoubleSQFDist)obj;
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
                   // System.err.println("k: " + k + " i: " + i + " d: " + d + " j" + j);
                    
                    r += weights[k] * Math.pow(fs1.data[3 + i * d + k] - fs1.data[3 + j * d + k], 2);
                }
                r = Math.pow(Math.E, (-alpha) * r);
                result += fs1.data[2 + i * d] * fs1.data[2 + j * d] * r / div;
            }
        }
        
        div = div2 * div2;
        for (int i=0; i<this.getClusterCount(); i++) {
            for (int j=0; j<this.getClusterCount(); j++) {
                r = 0;
                for (int k=0; k< d - 1; k++) {
                    r += weights[k] * Math.pow(this.data[3 + i * d + k] - this.data[3 + j * d + k], 2);
                }
                r = Math.pow(Math.E, (-alpha) * r);
                result += this.data[2 + i * d] * this.data[2 + j * d] * r / div;
            }
        }
        
        // from symmetry of the matrix use - 2 * wi * aij * wj
        div = div1 * div2;
        for (int i=0; i<length1; i++) {
            for (int j=0; j<this.getClusterCount(); j++) {
                r = 0;
                for (int k=0; k<d-1; k++) {
                    r += weights[k] * Math.pow(fs1.data[3 + i * d + k] - this.data[3 + j * d + k], 2); 
                }
                r = Math.pow(Math.E, (-alpha) * r); 
                result -= 2 * fs1.data[2 + i * d] * this.data[2 + j * d] * r /div;
            }
        }
        
        if (result < 0.0000000000001) return 0; // for rounding mistakes
        return (float)Math.sqrt(result);
    }
}
