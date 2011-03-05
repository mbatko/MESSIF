/*
 *  This file is part of MESSIF library.
 *
 *  MESSIF library is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MESSIF library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MESSIF library.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;


/**
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
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
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        // Access data of the other object (throws ClassCastException if trying to compare with another type of object)
        byte[] objData = ((ObjectVectorEdgecomp)obj).data;

        float [] Total_EdgeHist_Ref = new float [150];
        float [] Total_EdgeHist_Query = new float [150];
        
        // to use XM distance function
        for (int i=0; i<80; i++) {
            Total_EdgeHist_Ref[i+5] = QuantTable[i%5][this.data[i]];
            Total_EdgeHist_Query[i+5]	= QuantTable[i%5][objData[i]];
        }
        
        EHD_Make_Global_SemiGlobal(Total_EdgeHist_Ref);
        EHD_Make_Global_SemiGlobal(Total_EdgeHist_Query);

        float dist = 0.0f;
        for(int i=0; i < 80+70; i++){
            // Global(5)+Semi_Global(65)
            double dTemp = (Total_EdgeHist_Ref[i] - Total_EdgeHist_Query[i]);
            if (dTemp < 0.0) dTemp = -dTemp;
            dist += dTemp;
        }

        return dist;
    }

    private static float [][] QuantTable = {
        {0.010867f,0.057915f,0.099526f,0.144849f,0.195573f,0.260504f,0.358031f,0.530128f},
        {0.012266f,0.069934f,0.125879f,0.182307f,0.243396f,0.314563f,0.411728f,0.564319f},
        {0.004193f,0.025852f,0.046860f,0.068519f,0.093286f,0.123490f,0.161505f,0.228960f},
        {0.004174f,0.025924f,0.046232f,0.067163f,0.089655f,0.115391f,0.151904f,0.217745f},
        {0.006778f,0.051667f,0.108650f,0.166257f,0.224226f,0.285691f,0.356375f,0.450972f},
    };

    private void EHD_Make_Global_SemiGlobal(float[] TotalHistogram) {
        int i, j;
        // Make Global Histogram Start
        for(i=0; i<5; i++)
            TotalHistogram[i]=0.0f;
        for( j=0; j < 80; j+=5) {
            for( i=0; i < 5; i++) {
                TotalHistogram[i] += TotalHistogram[5+i+j];
            }
        }  // for( j )
        for(i=0; i<5; i++)
            // Global *5.
            TotalHistogram[i] = TotalHistogram[i]*5/16;
        
        // Make Global Histogram end
        
        
        // Make Semi-Global Histogram start
        for(i=85; i <105; i++) {
            j = i-85;
            TotalHistogram[i] =
                    (TotalHistogram[5+j]
                    +TotalHistogram[5+20+j]
                    +TotalHistogram[5+40+j]
                    +TotalHistogram[5+60+j])/4;
        }
        for(i=105; i < 125; i++) {
            j = i-105;
            TotalHistogram[i] =
                    (TotalHistogram[5+20*(j/5)+j%5]
                    +TotalHistogram[5+20*(j/5)+j%5+5]
                    +TotalHistogram[5+20*(j/5)+j%5+10]
                    +TotalHistogram[5+20*(j/5)+j%5+15])/4;
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
                    +TotalHistogram[5+10*(j/5)+25+j%5])/4;
        }
        //  Down area 2.
        for(i=135; i < 145; i++) {
            j = i-135;    // j = 0 ~ 9
            TotalHistogram[i] =
                    (TotalHistogram[5+10*(j/5)+40+j%5]
                    +TotalHistogram[5+10*(j/5)+45+j%5]
                    +TotalHistogram[5+10*(j/5)+60+j%5]
                    +TotalHistogram[5+10*(j/5)+65+j%5])/4;
        }
        // Center Area 1
        for(i=145; i < 150; i++) {
            j = i-145;    // j = 0 ~ 9
            TotalHistogram[i] =
                    (TotalHistogram[5+25+j%5]
                    +TotalHistogram[5+30+j%5]
                    +TotalHistogram[5+45+j%5]
                    +TotalHistogram[5+50+j%5])/4;
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
