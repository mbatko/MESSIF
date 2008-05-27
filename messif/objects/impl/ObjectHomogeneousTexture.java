/*
 * ObjectHomogeneousTexture.java
 *
 * Created on 3. duben 2007, 12:54
 *
 */

package messif.objects.impl;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Random;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInputStream;
import messif.objects.nio.BinaryOutputStream;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;


/**
 *
 * @author xbatko
 */
public class ObjectHomogeneousTexture extends LocalAbstractObject implements BinarySerializable {

    /** Class id for serialization. */
    private static final long serialVersionUID = 2L;

    /****************** Attributes ******************/

    protected final short average;
    protected final short standardDeviation;
    protected short[] energy;
    protected final short[] energyDeviation;


    /****************** Constructors ******************/

    /** Creates a new instance of ObjectHomogeneousTexture */
    public ObjectHomogeneousTexture(short average, short standardDeviation, short[] energy, short[] energyDeviation) {
        this.average = average;
        this.standardDeviation = standardDeviation;
        this.energy = energy;
        this.energyDeviation = energyDeviation;
    }

    public ObjectHomogeneousTexture(short[] averageDeviationEnergyTogether) {
        this.average = averageDeviationEnergyTogether[0];
        this.standardDeviation = averageDeviationEnergyTogether[1];
        this.energy = new short[(NUMofFEATURE-2)/2];
        System.arraycopy(averageDeviationEnergyTogether, 2, this.energy, 0, this.energy.length);
        if (this.standardDeviation != 0) {
            this.energyDeviation = new short[this.energy.length];
            System.arraycopy(averageDeviationEnergyTogether, 2+this.energy.length, this.energyDeviation, 0, this.energyDeviation.length);
        } else this.energyDeviation = null;
    }

    //****************** Text file store/retrieve methods ******************
    
    /** Creates a new instance of ObjectHomogeneousTexture from stream.
     * Throws IOException when an error appears during reading from given stream.
     * Throws EOFException when eof of the given stream is reached.
     * Throws NumberFormatException when the line read from given stream does
     * not consist of comma-separated or space-separated numbers.
     */
    public ObjectHomogeneousTexture(BufferedReader stream) throws IOException, NumberFormatException, IndexOutOfBoundsException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line;
        do {
            line = stream.readLine();
            if (line == null)
                throw new EOFException("EoF reached while initializing ObjectHomogeneousTexture.");
        } while (processObjectComment(line));
        
        String[] fields = line.trim().split(";\\p{Space}*");
        this.average = Short.parseShort(fields[0]);
        this.standardDeviation = Short.parseShort(fields[1]);
        String[] energy = fields[2].trim().split(",\\p{Space}*");
        this.energy = new short[energy.length];
        for (int i = 0; i < energy.length; i++)
            this.energy[i] = Short.parseShort(energy[i]);
        if (fields.length >= 4 && fields[3].length() > 0) {
            String[] energyDeviation = fields[3].trim().split(",\\p{Space}*");
            this.energyDeviation = new short[energyDeviation.length];
            for (int i = 0; i < energyDeviation.length; i++)
                this.energyDeviation[i] = Short.parseShort(energyDeviation[i]);
        } else this.energyDeviation = null;
    }

    /** Write object to text stream */
    public void writeData(OutputStream stream) throws IOException {
        stream.write(String.valueOf(average).getBytes());
        stream.write(';');
        stream.write(' ');
        stream.write(String.valueOf(standardDeviation).getBytes());
        stream.write(';');
        stream.write(' ');

        // Write energy vector
        for (int i = 0; i < energy.length; i++) {
            stream.write(String.valueOf(energy[i]).getBytes());
            stream.write((i + 1 < energy.length)?',':';');
            stream.write(' ');
        }

        // Write energy deviation vector
        if (energyDeviation != null)
            for (int i = 0; i < energyDeviation.length; i++) {
                stream.write(String.valueOf(energyDeviation[i]).getBytes());
                if (i + 1 < energy.length) {
                    stream.write(',');
                    stream.write(' ');
                }
            }
        stream.write('\n');
    }


    /****************** Size function ******************/

    public int getSize() {
        return (2 + energy.length + ((energyDeviation != null)?energyDeviation.length:0)) * Short.SIZE / 8;
    }


    /****************** Data equality functions ******************/

    public boolean dataEquals(Object obj) {
        if (!(obj instanceof ObjectHomogeneousTexture))
            return false;
        ObjectHomogeneousTexture castObj = (ObjectHomogeneousTexture)obj;
        return
                (average == castObj.average) &&
                (standardDeviation == castObj.standardDeviation) &&
                Arrays.equals(energy, castObj.energy) &&
                Arrays.equals(energyDeviation, castObj.energyDeviation);
    }

    public int dataHashCode() {
        return Arrays.hashCode(energy);
    }


    /****************** Distance function ******************/

    private static final int Nray = 128;		// Num of ray
    private static final int Nview = 180;		// Num of view
    private static final int NUMofFEATURE = 62;
    private static final int Quant_level = 255;
    private static final int RadialDivision = 5;
    private static final int RadialDivision2  = 3;
    private static final int AngularDivision = 6;
    private static final double[] wm={0.42,1.00,1.00,0.08,1.00};
    private static final double[] wd={0.32,1.00,1.00,1.00,1.00};
    private static final double wdc=0.28;
    private static final double wstd=0.22;

    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {

        //---yjyu - 010217
        int i,m,n,flag;
        int size2=Nray/2;
        double temp_distance, distance=0;
        int [] RefFeature = new int [NUMofFEATURE];
        int [] QueryFeature = new int [NUMofFEATURE];
        double [] fRefFeature= new double[NUMofFEATURE] ;
        double [] fQueryFeature = new double[NUMofFEATURE];
        
        Arrays.fill(RefFeature,0);
        Arrays.fill(QueryFeature,0);
        Arrays.fill(fRefFeature,0);
        Arrays.fill(fQueryFeature,0);
        
        double Num_pixel=0;
        
        for(i=0;i<Nview;i++)		// # of angular feature channel = 6
            for(int j=0;j<size2;j++)
                Num_pixel++;
        
        ObjectHomogeneousTexture castObj = (ObjectHomogeneousTexture)obj;
        RefFeature[0]	= this.average;
        RefFeature[1]	= this.standardDeviation;
        QueryFeature[0] = castObj.average;
        QueryFeature[1] = castObj.standardDeviation;
        flag = (energyDeviation != null && castObj.energyDeviation != null)?1:0;
        
        for (i=0; i<30; i++) {
            RefFeature[i+2]	= this.energy[i];
            QueryFeature[i+2] = castObj.energy[i];
            
            if (flag == 1) {
                RefFeature[i+30+2] = this.energyDeviation[i];
                QueryFeature[i+30+2] = castObj.energyDeviation[i];
            }
        }
        
        HTDequantization(RefFeature,fRefFeature);
        HTDequantization(QueryFeature,fQueryFeature);
        HTNormalization(fRefFeature);
        HTNormalization(fQueryFeature);
        
        distance =(wdc*Math.abs(fRefFeature[0]-fQueryFeature[0]));
        distance +=(wstd*Math.abs(fRefFeature[1]-fQueryFeature[1]));
        
        
        double min = 100000.00;
        
        for (int j=0;j<3;j++) {
            for (i=AngularDivision;i>0;i--) {
                temp_distance =0.0;
                for(n=2;n<RadialDivision;n++) {
                    for(m=0;m<AngularDivision;m++) {
                        if (m >= i) {
                            temp_distance+=(wm[n]*Math.abs(fRefFeature[n*AngularDivision+m+2-i]-fQueryFeature[(n-j)*AngularDivision+m+2]))
                            + flag*(wd[n]*Math.abs(fRefFeature[n*AngularDivision+m+30+2-i]-fQueryFeature[(n-j)*AngularDivision+m+30+2]));
                        } else {
                            temp_distance+=(wm[n]*Math.abs(fRefFeature[(n+1)*AngularDivision+m+2-i]-fQueryFeature[(n-j)*AngularDivision+m+2]))
                            + flag*(wd[n]*Math.abs(fRefFeature[(n+1)*AngularDivision+m+30+2-i]-fQueryFeature[(n-j)*AngularDivision+m+30+2]));
                        }
                    }
                }
                if (temp_distance < min) min = temp_distance;
            }
        }
        
        for (int j=1;j<3;j++)
            for (i=AngularDivision;i>0;i--) {
            temp_distance =0.0;
            for(n=2;n<RadialDivision;n++)
                for(m=0;m<AngularDivision;m++) {
                if (m >= i) {
                    temp_distance+=(wm[n]*Math.abs(fRefFeature[(n-j)*AngularDivision+m+2-i]-fQueryFeature[n*AngularDivision+m+2]))
                    + flag*(wd[n]*Math.abs(fRefFeature[(n-j)*AngularDivision+m+30+2-i]-fQueryFeature[n*AngularDivision+m+30+2]));
                } else {
                    temp_distance+=(wm[n]*Math.abs(fRefFeature[(n+1-j)*AngularDivision+m+2-i]-fQueryFeature[n*AngularDivision+m+2]))
                    + flag*(wd[n]*Math.abs(fRefFeature[(n+1-j)*AngularDivision+m+30+2-i]-fQueryFeature[n*AngularDivision+m+30+2]));
                }
                }
            
            if (temp_distance < min) min = temp_distance;
            }
        distance = min + distance;

        return (float)distance;
    }

    private static final double dcmin=0.0;
    private static final double dcmax=255.0;
    private static final double stdmin=1.309462;
    private static final double stdmax=109.476530;

    private static final double [][] mmax=
    { {18.392888,18.014313,18.002143,18.083845,18.046575,17.962099},
      {19.368960,18.628248,18.682786,19.785603,18.714615,18.879544},
      {20.816939,19.093605,20.837982,20.488190,20.763511,19.262577},
      {22.298871,20.316787,20.659550,21.463502,20.159304,20.280403},
      {21.516125,19.954733,20.381041,22.129800,20.184864,19.999331}};

    private static final double[][] mmin=
    {{ 6.549734, 8.886816, 8.885367, 6.155831, 8.810013, 8.888925},
     { 6.999376, 7.859269, 7.592031, 6.754764, 7.807377, 7.635503},
     { 8.299334, 8.067422, 7.955684, 7.939576, 8.518458, 8.672599},
     { 9.933642, 9.732479, 9.725933, 9.802238,10.076958,10.428015},
     {11.704927,11.690975,11.896972,11.996963,11.977944,11.944282}};

    private static final double [][]dmax=
    { {21.099482,20.749788,20.786944,20.847705,20.772294,20.747129},
      {22.658359,21.334119,21.283285,22.621111,21.773690,21.702166},
      {24.317046,21.618960,24.396872,23.797967,24.329333,21.688523},
      {25.638742,24.102725,22.687910,25.216958,22.334769,22.234942},
      {24.692990,22.978804,23.891302,25.244315,24.281915,22.699811}};

    private static final double [][]dmin=
    {{ 9.052970,11.754891,11.781252, 8.649997,11.674788,11.738701},
     { 9.275178,10.386329,10.066189, 8.914539,10.292868,10.152977},
     {10.368594,10.196313,10.211122,10.112823,10.648101,10.801070},
     {11.737487,11.560674,11.551509,11.608201,11.897524,12.246614},
     {13.303207,13.314553,13.450340,13.605001,13.547492,13.435994}};
    
    private void HTDequantization(int[] intFeature, double[] floatFeature) {
        double dcstep,stdstep,mstep,dstep;
        
        dcstep=(dcmax-dcmin)/Quant_level;
        floatFeature[0]=(dcmin+intFeature[0]*dcstep);
        
        stdstep=(stdmax-stdmin)/Quant_level;
        floatFeature[1]=(stdmin+intFeature[1]*stdstep);
        
        for(int n=0;n<RadialDivision;n++)
            for(int m=0;m<AngularDivision;m++) {
                mstep=(mmax[n][m]-mmin[n][m])/Quant_level;
                floatFeature[n*AngularDivision+m+2]=(mmin[n][m]+intFeature[n*AngularDivision+m+2]*mstep);
            }
        for(int n=0;n<RadialDivision;n++)
            for(int m=0;m<AngularDivision;m++) {
                dstep=(dmax[n][m]-dmin[n][m])/Quant_level;
                floatFeature[n*AngularDivision+m+32]=(dmin[n][m]+intFeature[n*AngularDivision+m+32]*dstep);
            }
    }

    private static final double dcnorm=122.331353;
    private static final double stdnorm=51.314701;
    private static final double[][] mmean=
    {{13.948462, 15.067986, 15.077915, 13.865536, 15.031283, 15.145633},
     {15.557970, 15.172251, 15.357618, 15.166167, 15.414601, 15.414378},
     {17.212408, 16.173027, 16.742651, 16.913837, 16.911480, 16.582123},
     {17.911104, 16.761711, 17.065447, 17.867548, 17.250889, 17.050728},
     {17.942741, 16.891190, 17.101770, 18.032434, 17.295305, 17.202160}};
    private static final double[][] dmean=
    {{16.544933, 17.845844, 17.849176, 16.484509, 17.803377, 17.928810},
     {18.054886, 17.617800, 17.862095, 17.627794, 17.935352, 17.887453},
     {19.771456, 18.512341, 19.240444, 19.410559, 19.373478, 18.962496},
     {20.192045, 18.763544, 19.202494, 20.098207, 19.399082, 19.032280},
     {19.857040, 18.514065, 18.831860, 19.984838, 18.971045, 18.863575}};
    
    private void HTNormalization(double[] feature) {
        feature[0]/=dcnorm;
        feature[1]/=stdnorm;
        
        for(int n=0;n<RadialDivision;n++)
            for(int m=0;m<AngularDivision;m++)
                feature[n*AngularDivision+m+2]/=mmean[n][m];
        for(int n=0;n<RadialDivision;n++)
            for(int m=0;m<AngularDivision;m++)
                feature[n*AngularDivision+m+32]/=dmean[n][m];
    }
    
    /*****************************  Cloning **********************************/
    
    /**
     * Creates and returns a randomly modified copy of this vector.
     * Selects a position in the "energy" array in random and changes it - the final value stays in the given range.
     * The modification is small - only by (max-min)/1000
     *
     * @param  args  expected size of the array is 2: <b>minVector</b> vector with minimal values in all positions
     *         <b>maxVector</b> vector with maximal values in all positions
     * @return a randomly modified clone of this instance.
     */
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        ObjectHomogeneousTexture rtv = (ObjectHomogeneousTexture) this.clone();
        rtv.energy = this.energy.clone();
        
        try {
            ObjectHomogeneousTexture minVector = (ObjectHomogeneousTexture) args[0];
            ObjectHomogeneousTexture maxVector = (ObjectHomogeneousTexture) args[1];
            Random random = new Random(System.currentTimeMillis());
            
            // pick a vector position in random
            int position = random.nextInt(Math.min(rtv.energy.length, Math.min(minVector.energy.length, maxVector.energy.length)));
            
            // calculate 1/1000 of the possible range of this value and either add or substract it from the origival value
            int smallStep = Math.max((maxVector.energy[position] - minVector.energy[position]) / 1000, 1);
            if (rtv.energy[position] + smallStep <= maxVector.energy[position])
                rtv.energy[position] += smallStep;
            else rtv.energy[position] -= smallStep;
        } catch (ArrayIndexOutOfBoundsException ignore) {
        } catch (ClassCastException ignore) { }
        
        return rtv;
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectHomogeneousTexture loaded from binary input stream.
     * 
     * @param input the stream to read the ObjectHomogeneousTexture from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the stream
     */
    protected ObjectHomogeneousTexture(BinaryInputStream input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        average = serializator.readShort(input);
        standardDeviation = serializator.readShort(input);
        energy = serializator.readShortArray(input);
        energyDeviation = serializator.readShortArray(input);
    }

    /**
     * Binary-serialize this object into the <code>output</code>.
     * @param output the data output this object is binary-serialized into
     * @param serializator the serializator used to write objects
     * @return the number of bytes actually written
     * @throws IOException if there was an I/O error during serialization
     */
    @Override
    public int binarySerialize(BinaryOutputStream output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator) +
               serializator.write(output, average) +
               serializator.write(output, standardDeviation) +
               serializator.write(output, energy) +
               serializator.write(output, energyDeviation);
    }

    /**
     * Returns the exact size of the binary-serialized version of this object in bytes.
     * @param serializator the serializator used to write objects
     * @return size of the binary-serialized version of this object
     */
    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return  super.getBinarySize(serializator) + 2 + 2 + serializator.getBinarySize(energy) + serializator.getBinarySize(energyDeviation);
    }

}
