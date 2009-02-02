/*
 * ObjectVectorEdgecomp.java
 *
 * Created on 3. kveten 2003, 20:09
 */

package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;


/**
 *
 * @author  xbatko
 */
public class ObjectVectorEdgecomp extends ObjectByteVector {

    /** class id for serialization */
    private static final long serialVersionUID = 2L;
    
    /****************** Constructors ******************/
    
    /** Creates a new instance of ObjectVectorEdgecomp */
    public ObjectVectorEdgecomp(byte[] data) {
        super(data);
    }
    
    /** Creates a new instance of randomly generated ObjectVectorEdgecomp */
    public ObjectVectorEdgecomp(int dimension) {
        super(dimension);
    }

    /** Creates a new instance of ObjectVectorEdgecomp from stream */
    public ObjectVectorEdgecomp(BufferedReader stream) throws IOException, NumberFormatException {
        super(stream);
    }

    /** Metric function
     *      Implements edge histogram distance
     */
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        // Access data of the other object (throws ClassCastException if trying to compare with another type of object)
        byte[] objData = ((ObjectVectorEdgecomp)obj).data;

        double [] Total_EdgeHist_Ref = new double [150];
        double [] Total_EdgeHist_Query = new double [150];
        
        double [] Local_EdgeHist = new double[80];
        double [] Local_EdgeHist_Query = new double[80];
        
        // to use XM distance function
        for (int i=0; i<80; i++) {
            Local_EdgeHist[i] = QuantTable[i%5][this.data[i]];
            Local_EdgeHist_Query[i]	= QuantTable[i%5][objData[i]];
        }
        
        EHD_Make_Global_SemiGlobal(Local_EdgeHist,  Total_EdgeHist_Ref);
        EHD_Make_Global_SemiGlobal(Local_EdgeHist_Query,  Total_EdgeHist_Query);
        double dist = 0.0;
        for(int i=0; i < 80+70; i++){
            // Global(5)+Semi_Global(65)
            double dTemp = (Total_EdgeHist_Ref[i] - Total_EdgeHist_Query[i]);
            if (dTemp < 0.0) dTemp = -dTemp;
            dist += dTemp;
        }

        return (float)dist;
    }

    private static double [][] QuantTable = {
        {0.010867,0.057915,0.099526,0.144849,0.195573,0.260504,0.358031,0.530128},
        {0.012266,0.069934,0.125879,0.182307,0.243396,0.314563,0.411728,0.564319},
        {0.004193,0.025852,0.046860,0.068519,0.093286,0.123490,0.161505,0.228960},
        {0.004174,0.025924,0.046232,0.067163,0.089655,0.115391,0.151904,0.217745},
        {0.006778,0.051667,0.108650,0.166257,0.224226,0.285691,0.356375,0.450972},
    };

    private void EHD_Make_Global_SemiGlobal(double[] LocalHistogramOnly, double[] TotalHistogram) {
        int i, j;
        System.arraycopy(LocalHistogramOnly, 0, TotalHistogram, 5, 80);
        // Make Global Histogram Start
        for(i=0; i<5; i++)
            TotalHistogram[i]=0.0;
        for( j=0; j < 80; j+=5) {
            for( i=0; i < 5; i++) {
                TotalHistogram[i] += TotalHistogram[5+i+j];
            }
        }  // for( j )
        for(i=0; i<5; i++)
            // Global *5.
            TotalHistogram[i] = TotalHistogram[i]*5/16.0;
        
        // Make Global Histogram end
        
        
        // Make Semi-Global Histogram start
        for(i=85; i <105; i++) {
            j = i-85;
            TotalHistogram[i] =
                    (TotalHistogram[5+j]
                    +TotalHistogram[5+20+j]
                    +TotalHistogram[5+40+j]
                    +TotalHistogram[5+60+j])/4.0;
        }
        for(i=105; i < 125; i++) {
            j = i-105;
            TotalHistogram[i] =
                    (TotalHistogram[5+20*(j/5)+j%5]
                    +TotalHistogram[5+20*(j/5)+j%5+5]
                    +TotalHistogram[5+20*(j/5)+j%5+10]
                    +TotalHistogram[5+20*(j/5)+j%5+15])/4.0;
        }
        ///////////////////////////////////////////////////////
        //				4 area Semi-Global
        ///////////////////////////////////////////////////////
        //  Upper area 2.
        for(i=125; i < 135; i++) {
            j = i-125;    // j = 0 ~ 9
            TotalHistogram[i] =
                    (TotalHistogram[5+10*(j/5)+0+j%5]
                    +TotalHistogram[5+10*(j/5)+5+j%5]
                    +TotalHistogram[5+10*(j/5)+20+j%5]
                    +TotalHistogram[5+10*(j/5)+25+j%5])/4.0;
        }
        //  Down area 2.
        for(i=135; i < 145; i++) {
            j = i-135;    // j = 0 ~ 9
            TotalHistogram[i] =
                    (TotalHistogram[5+10*(j/5)+40+j%5]
                    +TotalHistogram[5+10*(j/5)+45+j%5]
                    +TotalHistogram[5+10*(j/5)+60+j%5]
                    +TotalHistogram[5+10*(j/5)+65+j%5])/4.0;
        }
        // Center Area 1
        for(i=145; i < 150; i++) {
            j = i-145;    // j = 0 ~ 9
            TotalHistogram[i] =
                    (TotalHistogram[5+25+j%5]
                    +TotalHistogram[5+30+j%5]
                    +TotalHistogram[5+45+j%5]
                    +TotalHistogram[5+50+j%5])/4.0;
        }
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectVectorEdgecomp loaded from binary input buffer.
     * 
     * @param input the buffer to read the ObjectVectorEdgecomp from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectVectorEdgecomp(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }

}
