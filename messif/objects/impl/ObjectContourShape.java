
package messif.objects.impl;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;


/**
 * This is the MPEG-7 Countour Shape descriptor.
 * 
 * @author David Novak, FI Masaryk University, Brno, Czech Republic; <a href="mailto:david.novak@fi.muni.cz">david.novak@fi.muni.cz</a>
 */
public class ObjectContourShape extends LocalAbstractObject implements BinarySerializable {

    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;

    /****************** Attributes ******************/

    // number of peaks is peak.length / 2
	protected final byte [] globalCurvatureVector; // length 2
	protected final byte [] prototypeCurvatureVector; // length 0-2; NOT USED IN THE DISTANCE FUNCTION
	//protected final byte highestPeakY; // NOT USED IN THE DISTANCE FUNCTION
	protected final byte [] peak; // 2 x (1-62): 


    /****************** Constructors ******************/

    /** 
     * Creates a new instance of ObjectContourShape
     * @param globalCurvatureVector
     * @param prototypeCurvatureVector
     * @param peak 
     */
    public ObjectContourShape(byte [] globalCurvatureVector, byte [] prototypeCurvatureVector, byte [] peak) {
        this.globalCurvatureVector = globalCurvatureVector;
        this.prototypeCurvatureVector = prototypeCurvatureVector;
        this.peak = peak;
    }

    //****************** Text file store/retrieve methods ******************
    
    /** Creates a new instance of ObjectContourShape from stream.
     * @param stream input stream to read the data from
     * @throws IOException when an error appears during reading from given stream.
     * @throws EOFException when eof of the given stream is reached.
     * @throws NumberFormatException when the line read from given stream does not consist of 
     *   comma-separated or space-separated numbers.
     * @throws IndexOutOfBoundsException when the line is not of this format: <br/>
     *    globalCurvatureVector; prototypeCurvatureVector; highhestPeakY; peaks vector
     */
    public ObjectContourShape(BufferedReader stream) throws IOException, EOFException, NumberFormatException, IndexOutOfBoundsException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line;
        do {
            line = stream.readLine();
            if (line == null)
                throw new EOFException("EoF reached while initializing ObjectHomogeneousTexture.");
        } while (processObjectComment(line));

        String[] fields = line.trim().split(";\\p{Space}*");
        
        // Read globalCurvatureVector
        String[] globalCurvatureVectorStrings = fields[0].trim().split(",\\p{Space}*");
        this.globalCurvatureVector = new byte[globalCurvatureVectorStrings.length];
        for (int i = 0; i < globalCurvatureVectorStrings.length; i++)
            this.globalCurvatureVector[i] = Byte.parseByte(globalCurvatureVectorStrings[i]);

        // Read prototypeCurvatureVector
        String[] prototypeCurvatureVectorStrings = fields[1].trim().split(",\\p{Space}*");
        this.prototypeCurvatureVector = new byte[prototypeCurvatureVectorStrings.length];
        for (int i = 0; i < prototypeCurvatureVectorStrings.length; i++)
            this.prototypeCurvatureVector[i] = Byte.parseByte(prototypeCurvatureVectorStrings[i]);
        
        // Read peaks vector
        String[] peaksStrings = fields[2].trim().split(",\\p{Space}*");
        this.peak = new byte[peaksStrings.length];
        for (int i = 0; i < peaksStrings.length; i++)
            this.peak[i] = Byte.parseByte(peaksStrings[i]);
    }

    /** 
     * Write object to text stream. The format of the representation is:
     *   globalCurvatureVector; prototypeCurvatureVector; highhestPeakY; peaks vector
     * @throws java.io.IOException when the output stream throws an expeption during the write operations
     */
    public void writeData(OutputStream stream) throws IOException {
        // Write globalCurvatureVector
        for (int i = 0; i < globalCurvatureVector.length; i++) {
            stream.write(String.valueOf(globalCurvatureVector[i]).getBytes());
            stream.write((i + 1 < globalCurvatureVector.length)?',':';');
            stream.write(' ');
        }

        // Write prototypeCurvatureVector
        for (int i = 0; i < prototypeCurvatureVector.length; i++) {
            stream.write(String.valueOf(prototypeCurvatureVector[i]).getBytes());
            stream.write((i + 1 < prototypeCurvatureVector.length)?',':';');
            stream.write(' ');
        }

        // Write the peaks vector
        for (int i = 0; i < peak.length; i++) {
            stream.write(String.valueOf(peak[i]).getBytes());
            if (i + 1 < peak.length) {
                stream.write(',');
                stream.write(' ');
            }
        }

        stream.write('\n');
    }


    /****************** Size function ******************/

    public int getSize() {
        return (this.globalCurvatureVector.length + this.prototypeCurvatureVector.length + this.peak.length) * Byte.SIZE / 8;
    }


    /****************** Data equality functions ******************/

    public boolean dataEquals(Object obj) {
        if (!(obj instanceof ObjectContourShape))
            return false;
        ObjectContourShape castObj = (ObjectContourShape)obj;
        return
                Arrays.equals(globalCurvatureVector, castObj.globalCurvatureVector) &&
                Arrays.equals(prototypeCurvatureVector, castObj.prototypeCurvatureVector) && 
                Arrays.equals(peak, castObj.peak);
    }

    public int dataHashCode() {
        return Arrays.hashCode(peak);
    }


    /****************** Distance function ******************/

    private static float range(float x) {
        while (x < 0f) {
            x += 1f;
        }
        while (x >= 1f) {
            x -= 1f;
        }
        return x;
    }
    
    /** private function for making a maximal number having number of bits */
    private static final int bitsToMask(byte a) {
        return ((2 << (a - 1)) - 1);
    }
    
    //private static final float CONTOURSHAPE_YP = 0.05f;

    //private static final float CONTOURSHAPE_AP = 0.09f;
    //private static final byte CONTOURSHAPE_MAXCSS = 10;
    //private static final float CONTOURSHAPE_T = 0.000001f;
    //private static final float CONTOURSHAPE_TXA0 = 3.8f;
    //private static final float CONTOURSHAPE_TXA1 = 0.6f;

    //private static final byte CONTOURSHAPE_CSSPEAKBITS = 6;
    private static final byte CONTOURSHAPE_XBITS = 6;
    private static final byte CONTOURSHAPE_YBITS = 7;
    private static final byte CONTOURSHAPE_YnBITS = 3;
    private static final byte CONTOURSHAPE_CBITS = 6;
    private static final byte CONTOURSHAPE_EBITS = 6;

    private static final float CONTOURSHAPE_ETHR = 0.6f;
    private static final float CONTOURSHAPE_CTHR = 1.0f;
    private static final float CONTOURSHAPE_ECOST = 0.4f;
    private static final float CONTOURSHAPE_CCOST = 0.3f;

    private static final byte CONTOURSHAPE_NMATCHPEAKS = 2;
    //private static final float CONTOURSHAPE_TMATCHPEAKS = 0.9f;

    private static final float CONTOURSHAPE_XMAX = 1.0f;
    private static final float CONTOURSHAPE_YMAX = 1.7f;
    private static final float CONTOURSHAPE_CMIN = 12.0f;
    private static final float CONTOURSHAPE_CMAX = 110.0f;
    private static final float CONTOURSHAPE_EMIN = 1.0f;
    private static final float CONTOURSHAPE_EMAX = 10.0f;
    
    //private static final int CONTOURSHAPE_CSSPEAKMASK = bitsToMask(CONTOURSHAPE_CSSPEAKBITS);
    private static final int CONTOURSHAPE_XMASK = bitsToMask(CONTOURSHAPE_XBITS);
    private static final int CONTOURSHAPE_YMASK = bitsToMask(CONTOURSHAPE_YBITS);
    private static final int CONTOURSHAPE_YnMASK = bitsToMask(CONTOURSHAPE_YnBITS);
    private static final int CONTOURSHAPE_CMASK = bitsToMask(CONTOURSHAPE_CBITS);
    private static final int CONTOURSHAPE_EMASK = bitsToMask(CONTOURSHAPE_EBITS);
    
               
    /**
     * An auxiliary class used by the distance function.
     */
    protected static class Node {

        protected double cost = 0d; // the algorithm tries to find a minimal score 
        
        protected int nr; // size of the rPeaks arrays
        protected int nq; // size of the qPeaks arrays
        protected float rPeaksX [];
        protected float rPeaksY [];
        protected float qPeaksX [];
        protected float qPeaksY [];

        public Node(int nr, int nq) {
            this.rPeaksX = new float[nr];
            this.rPeaksY = new float[nr];
            this.qPeaksX = new float[nq];
            this.qPeaksY = new float[nq];
            this.nr = nr;
            this.nq = nq;
        }
    }
    
    /**
     * The distance algorithm is taken from the ContourShapeSearch.cpp of the XM library.
     * The "ref" object from the algorithm is the <code>obj</code> object in this method
     * and the "query" is <code>this</code> object.
     * 
     * @param obj the object to compute distance to
     * @param distThreshold the threshold value on the distance
     * @return the actual distance between obj and this if the distance is lower than distThreshold
     */
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {

        ObjectContourShape castObj = (ObjectContourShape) obj;        

        int nRefPeaks   = castObj.peak.length / 2;
        int nQueryPeaks = this.peak.length / 2;        
          
        float [] m_rPeaksX = new float[nRefPeaks];
        float [] m_rPeaksY = new float[nRefPeaks];
        float [] m_qPeaksX = new float[nQueryPeaks];
        float [] m_qPeaksY = new float[nQueryPeaks];
        Node [] m_nodeList = new Node[(nQueryPeaks * nRefPeaks == 0) ? 2 : (4 * nQueryPeaks * nRefPeaks)];
        
        float fRefE = 0.5f + (float) castObj.globalCurvatureVector[1];
        float fRefC = 0.5f + (float) castObj.globalCurvatureVector[0];
        float fQueryE = 0.5f + (float) this.globalCurvatureVector[1];
        float fQueryC = 0.5f + (float) this.globalCurvatureVector[0];
        
        float fDenomE = (fRefE > fQueryE) ? fRefE : fQueryE;
        float fDenomC = (fRefC > fQueryC) ? fRefC : fQueryC;
        
        fDenomE += CONTOURSHAPE_EMIN * (CONTOURSHAPE_EMASK+1) / (CONTOURSHAPE_EMAX - CONTOURSHAPE_EMIN);
        fDenomC += CONTOURSHAPE_CMIN * (CONTOURSHAPE_CMASK+1) / (CONTOURSHAPE_CMAX - CONTOURSHAPE_CMIN);
        
        if ((Math.abs(fRefE - fQueryE) >= CONTOURSHAPE_ETHR * fDenomE) || (Math.abs(fRefC - fQueryC) >= CONTOURSHAPE_CTHR * fDenomC)) {
            return LocalAbstractObject.MAX_DISTANCE;
        }
        
        float tCost = (CONTOURSHAPE_ECOST * Math.abs(fRefE - fQueryE)/fDenomE) + (CONTOURSHAPE_CCOST * Math.abs(fRefC - fQueryC)/fDenomC);

        // initialize values of "m_rPeaksX" and "m_rPeaksY" arrays (for "obj")
        for (int nr = 0; nr < nRefPeaks; nr++) {
            byte irx = castObj.peak[nr];
            byte iry = castObj.peak[nr + 1];
            m_rPeaksX[nr] = (irx * CONTOURSHAPE_XMAX / (float)CONTOURSHAPE_XMASK);
            if (nr == 0)
              m_rPeaksY[nr] = (iry * CONTOURSHAPE_YMAX / (float)CONTOURSHAPE_YMASK);
            else
              m_rPeaksY[nr] = (iry * m_rPeaksY[nr-1] / (float)CONTOURSHAPE_YnMASK);
        }

        // initialize values of "m_rPeaksX" and "m_rPeaksY" arrays (for "this")
        for (int nq = 0; nq < nQueryPeaks; nq++) {
            byte iqx = this.peak[nq];
            byte iqy = this.peak[nq + 1];
            m_qPeaksX[nq] = (iqx * CONTOURSHAPE_XMAX / (float)CONTOURSHAPE_XMASK);
            if (nq == 0)
              m_qPeaksY[nq] = (iqy * CONTOURSHAPE_YMAX / (float)CONTOURSHAPE_YMASK);
            else
              m_qPeaksY[nq] = (iqy * m_qPeaksY[nq-1] / (float)CONTOURSHAPE_YnMASK);
        }

        // now start to fill the m_nodeList
        // this is a double for-cycle over all preprocessed peaks in both objects
        int nNodes = 0;
        for (int i0 = 0; (i0 < nRefPeaks) && (i0 < CONTOURSHAPE_NMATCHPEAKS); i0++) {
            float iRefX = m_rPeaksX[i0];
            float iRefY = m_rPeaksY[i0];

            for (int j0 = 0; j0 < nQueryPeaks; j0++) {
                float iQueryX = m_qPeaksX[j0];
                float iQueryY = m_qPeaksY[j0];
                float denom = (iRefY > iQueryY) ? iRefY : iQueryY;
                if ((Math.abs(iRefY - iQueryY) / denom) < 0.7) {
                    m_nodeList[nNodes] = new Node(nRefPeaks, nQueryPeaks);
                    m_nodeList[nNodes + 1] = new Node(nRefPeaks, nQueryPeaks);

                    for (int pr = 0; pr < nRefPeaks; pr++) {
                        float frx = range(m_rPeaksX[pr] - iRefX);

                        m_nodeList[nNodes].rPeaksX[pr] = frx;
                        m_nodeList[nNodes].rPeaksY[pr] = m_rPeaksY[pr];
                        m_nodeList[nNodes + 1].rPeaksX[pr] = frx;
                        m_nodeList[nNodes + 1].rPeaksY[pr] = m_rPeaksY[pr];
                        //m_nodeList[nNodes].rPeaks[pr] = new Point2(frx, m_rPeaksY[pr]);
                        //m_nodeList[nNodes + 1].rPeaks[pr] = new Point2(frx, m_rPeaksY[pr]);
                    }

                    for (int pq = 0; pq < nQueryPeaks; pq++) {
                        float fqx = range(m_qPeaksX[pq] - iQueryX);

                        m_nodeList[nNodes].qPeaksX[pq] = fqx;
                        m_nodeList[nNodes].qPeaksY[pq] = m_qPeaksY[pq];
                        m_nodeList[nNodes + 1].qPeaksX[pq] = range(-1f * fqx);
                        m_nodeList[nNodes + 1].qPeaksY[pq] = m_qPeaksY[pq];
                        //m_nodeList[nNodes].qPeaks[pq] = new Point2(fqx, m_qPeaksY[pq]);
                        //m_nodeList[nNodes + 1].qPeaks[pq] = new Point2(range(-1f * fqx), m_qPeaksY[pq]);
                    }

                    nNodes += 2;
                }
            }
        }

        if (nRefPeaks == 0) {
            m_nodeList[nNodes] = new Node(nRefPeaks, nQueryPeaks);

            for (int pq = 0; pq < nQueryPeaks; pq++) {
                m_nodeList[nNodes].qPeaksX[pq] = m_qPeaksX[pq];
                m_nodeList[nNodes].qPeaksY[pq] = m_qPeaksY[pq];
                //m_nodeList[nNodes].qPeaks[pq] = new Point2(m_qPeaksX[pq], m_qPeaksY[pq]);
            }
            nNodes++;
        }

        for (int i1 = 0; (i1 < nQueryPeaks) && (i1 < CONTOURSHAPE_NMATCHPEAKS); i1++) {
            float iQueryX = m_qPeaksX[i1];
            float iQueryY = m_qPeaksY[i1];

            for (int j1 = 0; j1 < nRefPeaks; j1++) {
                float iRefX = m_rPeaksX[j1];
                float iRefY = m_rPeaksY[j1];
                float denom = (iQueryY > iRefY) ? iQueryY : iRefY;
                if ((Math.abs(iQueryY - iRefY) / denom) < 0.7) {
                    m_nodeList[nNodes] = new Node(nQueryPeaks, nRefPeaks);
                    m_nodeList[nNodes + 1] = new Node(nQueryPeaks, nRefPeaks);

                    for (int pq = 0; pq < nQueryPeaks; pq++) {
                        float fqx = range(m_qPeaksX[pq] - iQueryX);
                        
                        m_nodeList[nNodes].rPeaksX[pq] = fqx;
                        m_nodeList[nNodes].rPeaksY[pq] = m_qPeaksY[pq];
                        m_nodeList[nNodes + 1].rPeaksX[pq] = range(-1f * fqx);
                        m_nodeList[nNodes + 1].rPeaksY[pq] = m_qPeaksY[pq];
                        //m_nodeList[nNodes].rPeaks[pq] = new Point2(fqx, m_qPeaksY[pq]);
                        //m_nodeList[nNodes + 1].rPeaks[pq] = new Point2(range(-1f * fqx), m_qPeaksY[pq]);
                    }

                    for (int pr = 0; pr < nRefPeaks; pr++) {
                        float frx = range(m_rPeaksX[pr] - iRefX);

                        m_nodeList[nNodes].qPeaksX[pr] = frx;
                        m_nodeList[nNodes].qPeaksY[pr] = m_rPeaksY[pr];
                        m_nodeList[nNodes + 1].qPeaksX[pr] = frx;
                        m_nodeList[nNodes + 1].qPeaksY[pr] = m_rPeaksY[pr];
                        //m_nodeList[nNodes].qPeaks[pr] = new Point2(frx, m_rPeaksY[pr]);
                        //m_nodeList[nNodes + 1].qPeaks[pr] = new Point2(frx, m_rPeaksY[pr]);
                    }

                    nNodes += 2;
                }
            }
        }

        if (nQueryPeaks == 0) {
            m_nodeList[nNodes] = new Node(nQueryPeaks, nRefPeaks);

            for (int pr = 0; pr < nRefPeaks; pr++) {
                m_nodeList[nNodes].qPeaksX[pr] = m_rPeaksX[pr];
                m_nodeList[nNodes].qPeaksY[pr] = m_rPeaksY[pr];
                //m_nodeList[nNodes].qPeaks[pr] = new Point2(m_rPeaksX[pr], m_rPeaksY[pr]);
            }

            nNodes++;
        }

        if (nNodes == 0) 
            return LocalAbstractObject.MAX_DISTANCE;

        int index = 0;
        // this is the main loop when finding the node with the lowest cost
        while ((m_nodeList[index].nr > 0) || (m_nodeList[index].nq > 0)) {
            int ir = -1, iq = -1;

            if ((m_nodeList[index].nr > 0) && (m_nodeList[index].nq > 0)) {
                ir = 0;
                // here, they are searching for the maximal peak in the "index" of "referenced object"
                // the index of the max is stored in "ir"
                for (int mr = 1; mr < m_nodeList[index].nr; mr++) {
                    if (m_nodeList[index].rPeaksY[ir] < m_nodeList[index].rPeaksY[mr]) {
                        ir = mr;
                    }
                }
                iq = 0;
                float xd = Math.abs(m_nodeList[index].rPeaksX[ir] - m_nodeList[index].qPeaksX[iq]);
                if (xd > 0.5) {
                    xd = 1f - xd;
                }
                float yd = Math.abs(m_nodeList[index].rPeaksY[ir] - m_nodeList[index].qPeaksY[iq]);
                float sqd = xd * xd + yd * yd;
                for (int mq = 1; mq < m_nodeList[index].nq; mq++) {
                    xd = Math.abs(m_nodeList[index].rPeaksX[ir] - m_nodeList[index].qPeaksX[mq]);
                    if (xd > 0.5) {
                        xd = 1f - xd;
                    }
                    yd = Math.abs(m_nodeList[index].rPeaksY[ir] - m_nodeList[index].qPeaksY[mq]);
                    float d = xd * xd + yd * yd;
                    if (d < sqd) {
                        sqd = d;
                        iq = mq;
                    }
                }

                float dx = Math.abs(m_nodeList[index].rPeaksX[ir] - m_nodeList[index].qPeaksX[iq]);
                if (dx > 0.5f) {
                    dx = 1f - dx;
                }

                if (dx < 0.1f) {
                    float dy = Math.abs(m_nodeList[index].rPeaksY[ir] - m_nodeList[index].qPeaksY[iq]);
                    m_nodeList[index].cost += Math.sqrt(dx * dx + dy * dy);
                    if (iq < --m_nodeList[index].nq) {
                        System.arraycopy(m_nodeList[index].qPeaksX, iq + 1, m_nodeList[index].qPeaksX, iq, m_nodeList[index].nq - iq);
                        System.arraycopy(m_nodeList[index].qPeaksY, iq + 1, m_nodeList[index].qPeaksY, iq, m_nodeList[index].nq - iq);
                        //memmove(&  m_nodeList[index].qPeaks[iq], &  m_nodeList[index].qPeaks[iq + 1], (m_nodeList[index].nq - iq) * sizeof(m_nodeList[index].qPeaks[0]));
                    }
                } else {
                    m_nodeList[index].cost += m_nodeList[index].rPeaksY[ir];
                }
                // this array shrinkage is done in both if-branches
                if (ir < --m_nodeList[index].nr) {
                    System.arraycopy( m_nodeList[index].rPeaksX, ir + 1, m_nodeList[index].rPeaksX, ir, m_nodeList[index].nr - ir);
                    System.arraycopy( m_nodeList[index].rPeaksY, ir + 1, m_nodeList[index].rPeaksY, ir, m_nodeList[index].nr - ir);
                    //memmove(&  m_nodeList[index].rPeaks[ir], &  m_nodeList[index].rPeaks[ir + 1], (m_nodeList[index].nr - ir) * sizeof(m_nodeList[index].rPeaks[0]));
                }
            } else if (m_nodeList[index].nr > 0) {
                m_nodeList[index].cost += m_nodeList[index].rPeaksY[0];
                if (--m_nodeList[index].nr > 0) {
                    System.arraycopy(m_nodeList[index].rPeaksX, 1, m_nodeList[index].rPeaksX, 0, m_nodeList[index].nr);
                    System.arraycopy(m_nodeList[index].rPeaksY, 1, m_nodeList[index].rPeaksY, 0, m_nodeList[index].nr);
                    //memmove(&  m_nodeList[index].rPeaks[0], &  m_nodeList[index].rPeaks[1], (m_nodeList[index].nr) * sizeof(m_nodeList[index].rPeaks[0]));
                }
            } else { // if (m_nodeList[index].nq > 0)            
                m_nodeList[index].cost += m_nodeList[index].qPeaksY[0];
                if (--m_nodeList[index].nq > 0) {
                    System.arraycopy(m_nodeList[index].qPeaksX, 1, m_nodeList[index].qPeaksX, 0, m_nodeList[index].nq);
                    System.arraycopy(m_nodeList[index].qPeaksY, 1, m_nodeList[index].qPeaksY, 0, m_nodeList[index].nq);
                    //memmove(&  m_nodeList[index].qPeaks[0], &  m_nodeList[index].qPeaks[1], (m_nodeList[index].nq) * sizeof(m_nodeList[index].qPeaks[0]));
                }
            }

            index = 0;
            double minCost = m_nodeList[index].cost;
            // find the node with the smallest cost and start "reducing" it
            for (int c0 = 1; c0 < nNodes; c0++) {
                if (m_nodeList[c0].cost < minCost) {
                    index = c0;
                    minCost = m_nodeList[c0].cost;
                }
            }
        }

        return (float) (m_nodeList[index].cost + tCost);
    }

    
    /*****************************  Cloning **********************************/
    
    /**
     * Creates and returns a randomly modified copy of this object.
     *
     * @param  args  
     * @return a randomly modified clone of this instance.
     * @throws java.lang.CloneNotSupportedException when random clonning not supported
     */
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        throw new CloneNotSupportedException("cloneRandomlyModify not supported yet");
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectByteVector loaded from binary input buffer.
     *
     * @param input the buffer to read the ObjectByteVector from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectContourShape(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        globalCurvatureVector = serializator.readByteArray(input);
        prototypeCurvatureVector = serializator.readByteArray(input);
        peak = serializator.readByteArray(input);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator) +
               serializator.write(output, globalCurvatureVector) + serializator.write(output, prototypeCurvatureVector) +
               + serializator.write(output, peak);
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return  super.getBinarySize(serializator) + 
                serializator.getBinarySize(globalCurvatureVector) + serializator.getBinarySize(prototypeCurvatureVector) +
                serializator.getBinarySize(peak);
    }

}
