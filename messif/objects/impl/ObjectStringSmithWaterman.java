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
import java.io.EOFException;
import java.io.IOException;
import java.util.StringTokenizer;
import messif.objects.keys.AbstractObjectKey;
import messif.objects.LocalAbstractObject;


/**
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ObjectStringSmithWaterman extends ObjectString {

    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    /** dimension of scoring matrix */
    private static final short SIZE = 127;

    /** comment starting character in matrices files */
    private static final char COMMENT_STARTER = '#';

    /** open gap penalty used for similarity computations */
    private static final float openGapPenalty = 10f;
    
    /** extend gap penalty used for similarity computations */
    private static final float extendGapPenalty = 0.5f;
    
    /** default scoring matrix (PAM250) */
    private static float[][] scoringMatrix; // Initialized by static initializer at the end of this class
    
    /** similarity score of this sequence to itself */
    private float selfSimilarityMeasure;


    //****************** Constructors ******************
    
    /**
     * Creates a new instance of ObjectStringSmithWaterman for the specified sequence.
     * @param sequence the protein sequence
     */
    public ObjectStringSmithWaterman(String sequence){
        super(sequence);
        selfSimilarityMeasure = measure(sequence, sequence, scoringMatrix);
    }
    
    /**
     * Creates a new instance of ObjectStringSmithWaterman for the given key and sequence.
     * @param sequence the protein sequence
     * @param key the key to associate with this object
     */
    public ObjectStringSmithWaterman(AbstractObjectKey key, String sequence) {
        this(sequence);
        setObjectKey(key);
    }
    
    /**
     * Creates a new instance of ObjectStringSmithWaterman for the given locator and sequence.
     * @param sequence the protein sequence
     * @param locator the locator to associate with this object
     */
    public ObjectStringSmithWaterman(String locator, String sequence) {
        this(sequence);
        setObjectKey(new AbstractObjectKey(locator));
    }
    
    /** 
     * Creates a new instance of ObjectStringSmithWaterman with random generated sequence
     * with length between minLength and maxLength characters.
     * @param minLength minimal length of the generated sequence
     * @param maxLength maximal length of the generated sequence
     */
    public ObjectStringSmithWaterman(int minLength, int maxLength) {
        this(generateRandom(minLength, maxLength));
    }
    
    
    //****************** Text file store/retrieve methods ******************
    
    /**
     * Creates a new instance of ObjectStringSmithWaterman from the text stream.
     * @param stream the text stream to read object from
     * @throws IOException when an error appears during reading from the given stream
     * @throws EOFException when end of the given text stream is reached
     */
    public ObjectStringSmithWaterman(BufferedReader stream) throws IOException, EOFException {
        super(stream);
        selfSimilarityMeasure = measure(text, text, scoringMatrix);
    }    


    //***************** Metric distance function ***********/
    
    /**
     * Metric distance function.
     * Returns distance between this object and the object that is supplied as argument.
     *
     * @return distance between this object and argument
     */
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        ObjectStringSmithWaterman castObj = (ObjectStringSmithWaterman)obj;
        return (float)Math.sqrt(
                this.selfSimilarityMeasure + castObj.selfSimilarityMeasure -
                2 * measure(this.text, castObj.text, scoringMatrix)
        );
    }
    
    
    //********** Smith-Waterman-Gotoh algorithm functions ************/
    
    /**
     * Compares two sequences using Smith-Waterman algorithm.
     *
     * @param s1 the first protein sequence
     * @param s2 the second protein sequence
     * @param scores the scoring matrix
     * @return the similarity of s1 and s2
     *
     */
    public static float measure(String s1, String s2, float[][] scores) {
        int m = s1.length() + 1;
        int n = s2.length() + 1;
        
        short[] sizesOfVerticalGaps = new short[m * n];
        short[] sizesOfHorizontalGaps = new short[m * n];
        for (int i = 0, k = 0; i < m; i++, k += n) {
            for (int j = 0; j < n; j++) {
                sizesOfVerticalGaps[k + j] = sizesOfHorizontalGaps[k + j] = 1;
            }
        }
        
        char[] a1 = s1.toCharArray();
        char[] a2 = s2.toCharArray();
        
        float f; // score of alignment x1...xi to y1...yi if xi aligns to yi
        float[] g = new float[n]; // score if xi aligns to a gap after yi
        float h; // score if yi aligns to a gap after xi
        float[] v = new float[n]; // best score of alignment x1...xi to y1...yi
        float vDiagonal;
        
        g[0] = Float.NEGATIVE_INFINITY;
        h = Float.NEGATIVE_INFINITY;
        v[0] = 0;
        
        for (int j = 1; j < n; j++) {
            g[j] = Float.NEGATIVE_INFINITY;
            v[j] = 0;
        }
        
        float similarityScore, g1, g2, h1, h2;
        
        float highestScore = 0;
        
        for (int i = 1, k = n; i < m; i++, k += n) {
            h = Float.NEGATIVE_INFINITY;
            vDiagonal = v[0];
            for (int j = 1, l = k + 1; j < n; j++, l++) {
                similarityScore = scores[a1[i - 1]][a2[j - 1]];
                
                // Fill the matrices
                f = vDiagonal + similarityScore;
                
                g1 = g[j] - extendGapPenalty;
                g2 = v[j] - openGapPenalty;
                if (g1 > g2) {
                    g[j] = g1;
                    sizesOfVerticalGaps[l] = (short) (sizesOfVerticalGaps[l - n] + 1);
                } else {
                    g[j] = g2;
                }
                
                h1 = h - extendGapPenalty;
                h2 = v[j - 1] - openGapPenalty;
                if (h1 > h2) {
                    h = h1;
                    sizesOfHorizontalGaps[l] = (short) (sizesOfHorizontalGaps[l - 1] + 1);
                } else {
                    h = h2;
                }
                
                vDiagonal = v[j];
                v[j] = maximum(f, g[j], h, 0);
                
                if (v[j] > highestScore) {
                    highestScore = v[j];
                }
            }
        }
        
        return highestScore;
    }
    
    
    
    
    /**
     * Loads scoring matrix from InputStream
     * @param reader is input stream
     * @return loaded matrix
     * @throws IOException if something bad happens during file read
     */
    public static float[][] loadMatrix(BufferedReader reader) throws IOException {
        char[] acids = new char[SIZE];
        
        // Initialize the acids array to null values (ascii = 0)
        for (int i = 0; i < SIZE; i++)
            acids[i] = 0;
        
        float[][] scores = new float[SIZE][SIZE];
        
        String line;
        
        try {
            // Skip the comment lines
            while ((line = reader.readLine()) != null && line.trim().charAt(0) == COMMENT_STARTER);
        } catch (IOException e) {
            String message = "Failed reading from input stream: " + e.getMessage();
            throw new IOException(message);
        }
        
        // Read the headers line (the letters of the acids)
        StringTokenizer tokenizer;
        tokenizer = new StringTokenizer( line.trim( ) );
        for (int j = 0; tokenizer.hasMoreTokens(); j++) {
            acids[j] = tokenizer.nextToken().charAt(0);
        }
        
        try {
            // Read the scores
            while ((line = reader.readLine()) != null) {
                tokenizer = new StringTokenizer( line.trim( ) );
                char acid = tokenizer.nextToken().charAt(0);
                for (int i = 0; i < SIZE; i++) {
                    if (acids[i] != 0) {
                        scores[acid][acids[i]] = Float.parseFloat(tokenizer.nextToken());
                    }
                }
            }
        } catch (IOException e) {
            String message = "Failed reading from input stream: " + e.getMessage();
            throw new IOException(message);
        }
        return scores;
    }
    
    
    /**
     * Returns the maximum of 4 float numbers.
     *
     * @param a
     *            float #1
     * @param b
     *            float #2
     * @param c
     *            float #3
     * @param d
     *            float #4
     * @return The maximum of a, b, c and d.
     */
    private static final float maximum(float a, float b, float c, float d) {
        if (a > b) {
            if (a > c) {
                return a > d ? a : d;
            } else {
                return c > d ? c : d;
            }
        } else if (b > c) {
            return b > d ? b : d;
        } else {
            return c > d ? c : d;
        }
    }

    /** initialization of defaultScoringMatrix to PAM250 */
    static{
        scoringMatrix = new float[SIZE][SIZE];
        int i,j;
        for(i = 0; i < SIZE; ++i)
            for(j = 0; j < SIZE; ++j)
                scoringMatrix[i][j] = Float.NaN;
        
        scoringMatrix['A']['A'] = 2;
        scoringMatrix['A']['R'] = -2;
        scoringMatrix['A']['N'] = 0;
        scoringMatrix['A']['D'] = 0;
        scoringMatrix['A']['C'] = -2;
        scoringMatrix['A']['Q'] = 0;
        scoringMatrix['A']['E'] = 0;
        scoringMatrix['A']['G'] = 1;
        scoringMatrix['A']['H'] = -1;
        scoringMatrix['A']['I'] = -1;
        scoringMatrix['A']['L'] = -2;
        scoringMatrix['A']['K'] = -1;
        scoringMatrix['A']['M'] = -1;
        scoringMatrix['A']['F'] = -3;
        scoringMatrix['A']['P'] = 1;
        scoringMatrix['A']['S'] = 1;
        scoringMatrix['A']['T'] = 1;
        scoringMatrix['A']['W'] = -6;
        scoringMatrix['A']['Y'] = -3;
        scoringMatrix['A']['V'] = 0;
        scoringMatrix['A']['B'] = 0;
        scoringMatrix['A']['Z'] = 0;
        scoringMatrix['A']['X'] = 0;
        scoringMatrix['R']['A'] = -2;
        scoringMatrix['R']['R'] = 6;
        scoringMatrix['R']['N'] = 0;
        scoringMatrix['R']['D'] = -1;
        scoringMatrix['R']['C'] = -4;
        scoringMatrix['R']['Q'] = 1;
        scoringMatrix['R']['E'] = -1;
        scoringMatrix['R']['G'] = -3;
        scoringMatrix['R']['H'] = 2;
        scoringMatrix['R']['I'] = -2;
        scoringMatrix['R']['L'] = -3;
        scoringMatrix['R']['K'] = 3;
        scoringMatrix['R']['M'] = 0;
        scoringMatrix['R']['F'] = -4;
        scoringMatrix['R']['P'] = 0;
        scoringMatrix['R']['S'] = 0;
        scoringMatrix['R']['T'] = -1;
        scoringMatrix['R']['W'] = 2;
        scoringMatrix['R']['Y'] = -4;
        scoringMatrix['R']['V'] = -2;
        scoringMatrix['R']['B'] = -1;
        scoringMatrix['R']['Z'] = 0;
        scoringMatrix['R']['X'] = -1;
        scoringMatrix['N']['A'] = 0;
        scoringMatrix['N']['R'] = 0;
        scoringMatrix['N']['N'] = 2;
        scoringMatrix['N']['D'] = 2;
        scoringMatrix['N']['C'] = -4;
        scoringMatrix['N']['Q'] = 1;
        scoringMatrix['N']['E'] = 1;
        scoringMatrix['N']['G'] = 0;
        scoringMatrix['N']['H'] = 2;
        scoringMatrix['N']['I'] = -2;
        scoringMatrix['N']['L'] = -3;
        scoringMatrix['N']['K'] = 1;
        scoringMatrix['N']['M'] = -2;
        scoringMatrix['N']['F'] = -3;
        scoringMatrix['N']['P'] = 0;
        scoringMatrix['N']['S'] = 1;
        scoringMatrix['N']['T'] = 0;
        scoringMatrix['N']['W'] = -4;
        scoringMatrix['N']['Y'] = -2;
        scoringMatrix['N']['V'] = -2;
        scoringMatrix['N']['B'] = 2;
        scoringMatrix['N']['Z'] = 1;
        scoringMatrix['N']['X'] = 0;
        scoringMatrix['D']['A'] = 0;
        scoringMatrix['D']['R'] = -1;
        scoringMatrix['D']['N'] = 2;
        scoringMatrix['D']['D'] = 4;
        scoringMatrix['D']['C'] = -5;
        scoringMatrix['D']['Q'] = 2;
        scoringMatrix['D']['E'] = 3;
        scoringMatrix['D']['G'] = 1;
        scoringMatrix['D']['H'] = 1;
        scoringMatrix['D']['I'] = -2;
        scoringMatrix['D']['L'] = -4;
        scoringMatrix['D']['K'] = 0;
        scoringMatrix['D']['M'] = -3;
        scoringMatrix['D']['F'] = -6;
        scoringMatrix['D']['P'] = -1;
        scoringMatrix['D']['S'] = 0;
        scoringMatrix['D']['T'] = 0;
        scoringMatrix['D']['W'] = -7;
        scoringMatrix['D']['Y'] = -4;
        scoringMatrix['D']['V'] = -2;
        scoringMatrix['D']['B'] = 3;
        scoringMatrix['D']['Z'] = 3;
        scoringMatrix['D']['X'] = -1;
        scoringMatrix['C']['A'] = -2;
        scoringMatrix['C']['R'] = -4;
        scoringMatrix['C']['N'] = -4;
        scoringMatrix['C']['D'] = -5;
        scoringMatrix['C']['C'] = 12;
        scoringMatrix['C']['Q'] = -5;
        scoringMatrix['C']['E'] = -5;
        scoringMatrix['C']['G'] = -3;
        scoringMatrix['C']['H'] = -3;
        scoringMatrix['C']['I'] = -2;
        scoringMatrix['C']['L'] = -6;
        scoringMatrix['C']['K'] = -5;
        scoringMatrix['C']['M'] = -5;
        scoringMatrix['C']['F'] = -4;
        scoringMatrix['C']['P'] = -3;
        scoringMatrix['C']['S'] = 0;
        scoringMatrix['C']['T'] = -2;
        scoringMatrix['C']['W'] = -8;
        scoringMatrix['C']['Y'] = 0;
        scoringMatrix['C']['V'] = -2;
        scoringMatrix['C']['B'] = -4;
        scoringMatrix['C']['Z'] = -5;
        scoringMatrix['C']['X'] = -3;
        scoringMatrix['Q']['A'] = 0;
        scoringMatrix['Q']['R'] = 1;
        scoringMatrix['Q']['N'] = 1;
        scoringMatrix['Q']['D'] = 2;
        scoringMatrix['Q']['C'] = -5;
        scoringMatrix['Q']['Q'] = 4;
        scoringMatrix['Q']['E'] = 2;
        scoringMatrix['Q']['G'] = -1;
        scoringMatrix['Q']['H'] = 3;
        scoringMatrix['Q']['I'] = -2;
        scoringMatrix['Q']['L'] = -2;
        scoringMatrix['Q']['K'] = 1;
        scoringMatrix['Q']['M'] = -1;
        scoringMatrix['Q']['F'] = -5;
        scoringMatrix['Q']['P'] = 0;
        scoringMatrix['Q']['S'] = -1;
        scoringMatrix['Q']['T'] = -1;
        scoringMatrix['Q']['W'] = -5;
        scoringMatrix['Q']['Y'] = -4;
        scoringMatrix['Q']['V'] = -2;
        scoringMatrix['Q']['B'] = 1;
        scoringMatrix['Q']['Z'] = 3;
        scoringMatrix['Q']['X'] = -1;
        scoringMatrix['E']['A'] = 0;
        scoringMatrix['E']['R'] = -1;
        scoringMatrix['E']['N'] = 1;
        scoringMatrix['E']['D'] = 3;
        scoringMatrix['E']['C'] = -5;
        scoringMatrix['E']['Q'] = 2;
        scoringMatrix['E']['E'] = 4;
        scoringMatrix['E']['G'] = 0;
        scoringMatrix['E']['H'] = 1;
        scoringMatrix['E']['I'] = -2;
        scoringMatrix['E']['L'] = -3;
        scoringMatrix['E']['K'] = 0;
        scoringMatrix['E']['M'] = -2;
        scoringMatrix['E']['F'] = -5;
        scoringMatrix['E']['P'] = -1;
        scoringMatrix['E']['S'] = 0;
        scoringMatrix['E']['T'] = 0;
        scoringMatrix['E']['W'] = -7;
        scoringMatrix['E']['Y'] = -4;
        scoringMatrix['E']['V'] = -2;
        scoringMatrix['E']['B'] = 3;
        scoringMatrix['E']['Z'] = 3;
        scoringMatrix['E']['X'] = -1;
        scoringMatrix['G']['A'] = 1;
        scoringMatrix['G']['R'] = -3;
        scoringMatrix['G']['N'] = 0;
        scoringMatrix['G']['D'] = 1;
        scoringMatrix['G']['C'] = -3;
        scoringMatrix['G']['Q'] = -1;
        scoringMatrix['G']['E'] = 0;
        scoringMatrix['G']['G'] = 5;
        scoringMatrix['G']['H'] = -2;
        scoringMatrix['G']['I'] = -3;
        scoringMatrix['G']['L'] = -4;
        scoringMatrix['G']['K'] = -2;
        scoringMatrix['G']['M'] = -3;
        scoringMatrix['G']['F'] = -5;
        scoringMatrix['G']['P'] = 0;
        scoringMatrix['G']['S'] = 1;
        scoringMatrix['G']['T'] = 0;
        scoringMatrix['G']['W'] = -7;
        scoringMatrix['G']['Y'] = -5;
        scoringMatrix['G']['V'] = -1;
        scoringMatrix['G']['B'] = 0;
        scoringMatrix['G']['Z'] = 0;
        scoringMatrix['G']['X'] = -1;
        scoringMatrix['H']['A'] = -1;
        scoringMatrix['H']['R'] = 2;
        scoringMatrix['H']['N'] = 2;
        scoringMatrix['H']['D'] = 1;
        scoringMatrix['H']['C'] = -3;
        scoringMatrix['H']['Q'] = 3;
        scoringMatrix['H']['E'] = 1;
        scoringMatrix['H']['G'] = -2;
        scoringMatrix['H']['H'] = 6;
        scoringMatrix['H']['I'] = -2;
        scoringMatrix['H']['L'] = -2;
        scoringMatrix['H']['K'] = 0;
        scoringMatrix['H']['M'] = -2;
        scoringMatrix['H']['F'] = -2;
        scoringMatrix['H']['P'] = 0;
        scoringMatrix['H']['S'] = -1;
        scoringMatrix['H']['T'] = -1;
        scoringMatrix['H']['W'] = -3;
        scoringMatrix['H']['Y'] = 0;
        scoringMatrix['H']['V'] = -2;
        scoringMatrix['H']['B'] = 1;
        scoringMatrix['H']['Z'] = 2;
        scoringMatrix['H']['X'] = -1;
        scoringMatrix['I']['A'] = -1;
        scoringMatrix['I']['R'] = -2;
        scoringMatrix['I']['N'] = -2;
        scoringMatrix['I']['D'] = -2;
        scoringMatrix['I']['C'] = -2;
        scoringMatrix['I']['Q'] = -2;
        scoringMatrix['I']['E'] = -2;
        scoringMatrix['I']['G'] = -3;
        scoringMatrix['I']['H'] = -2;
        scoringMatrix['I']['I'] = 5;
        scoringMatrix['I']['L'] = 2;
        scoringMatrix['I']['K'] = -2;
        scoringMatrix['I']['M'] = 2;
        scoringMatrix['I']['F'] = 1;
        scoringMatrix['I']['P'] = -2;
        scoringMatrix['I']['S'] = -1;
        scoringMatrix['I']['T'] = 0;
        scoringMatrix['I']['W'] = -5;
        scoringMatrix['I']['Y'] = -1;
        scoringMatrix['I']['V'] = 4;
        scoringMatrix['I']['B'] = -2;
        scoringMatrix['I']['Z'] = -2;
        scoringMatrix['I']['X'] = -1;
        scoringMatrix['L']['A'] = -2;
        scoringMatrix['L']['R'] = -3;
        scoringMatrix['L']['N'] = -3;
        scoringMatrix['L']['D'] = -4;
        scoringMatrix['L']['C'] = -6;
        scoringMatrix['L']['Q'] = -2;
        scoringMatrix['L']['E'] = -3;
        scoringMatrix['L']['G'] = -4;
        scoringMatrix['L']['H'] = -2;
        scoringMatrix['L']['I'] = 2;
        scoringMatrix['L']['L'] = 6;
        scoringMatrix['L']['K'] = -3;
        scoringMatrix['L']['M'] = 4;
        scoringMatrix['L']['F'] = 2;
        scoringMatrix['L']['P'] = -3;
        scoringMatrix['L']['S'] = -3;
        scoringMatrix['L']['T'] = -2;
        scoringMatrix['L']['W'] = -2;
        scoringMatrix['L']['Y'] = -1;
        scoringMatrix['L']['V'] = 2;
        scoringMatrix['L']['B'] = -3;
        scoringMatrix['L']['Z'] = -3;
        scoringMatrix['L']['X'] = -1;
        scoringMatrix['K']['A'] = -1;
        scoringMatrix['K']['R'] = 3;
        scoringMatrix['K']['N'] = 1;
        scoringMatrix['K']['D'] = 0;
        scoringMatrix['K']['C'] = -5;
        scoringMatrix['K']['Q'] = 1;
        scoringMatrix['K']['E'] = 0;
        scoringMatrix['K']['G'] = -2;
        scoringMatrix['K']['H'] = 0;
        scoringMatrix['K']['I'] = -2;
        scoringMatrix['K']['L'] = -3;
        scoringMatrix['K']['K'] = 5;
        scoringMatrix['K']['M'] = 0;
        scoringMatrix['K']['F'] = -5;
        scoringMatrix['K']['P'] = -1;
        scoringMatrix['K']['S'] = 0;
        scoringMatrix['K']['T'] = 0;
        scoringMatrix['K']['W'] = -3;
        scoringMatrix['K']['Y'] = -4;
        scoringMatrix['K']['V'] = -2;
        scoringMatrix['K']['B'] = 1;
        scoringMatrix['K']['Z'] = 0;
        scoringMatrix['K']['X'] = -1;
        scoringMatrix['M']['A'] = -1;
        scoringMatrix['M']['R'] = 0;
        scoringMatrix['M']['N'] = -2;
        scoringMatrix['M']['D'] = -3;
        scoringMatrix['M']['C'] = -5;
        scoringMatrix['M']['Q'] = -1;
        scoringMatrix['M']['E'] = -2;
        scoringMatrix['M']['G'] = -3;
        scoringMatrix['M']['H'] = -2;
        scoringMatrix['M']['I'] = 2;
        scoringMatrix['M']['L'] = 4;
        scoringMatrix['M']['K'] = 0;
        scoringMatrix['M']['M'] = 6;
        scoringMatrix['M']['F'] = 0;
        scoringMatrix['M']['P'] = -2;
        scoringMatrix['M']['S'] = -2;
        scoringMatrix['M']['T'] = -1;
        scoringMatrix['M']['W'] = -4;
        scoringMatrix['M']['Y'] = -2;
        scoringMatrix['M']['V'] = 2;
        scoringMatrix['M']['B'] = -2;
        scoringMatrix['M']['Z'] = -2;
        scoringMatrix['M']['X'] = -1;
        scoringMatrix['F']['A'] = -3;
        scoringMatrix['F']['R'] = -4;
        scoringMatrix['F']['N'] = -3;
        scoringMatrix['F']['D'] = -6;
        scoringMatrix['F']['C'] = -4;
        scoringMatrix['F']['Q'] = -5;
        scoringMatrix['F']['E'] = -5;
        scoringMatrix['F']['G'] = -5;
        scoringMatrix['F']['H'] = -2;
        scoringMatrix['F']['I'] = 1;
        scoringMatrix['F']['L'] = 2;
        scoringMatrix['F']['K'] = -5;
        scoringMatrix['F']['M'] = 0;
        scoringMatrix['F']['F'] = 9;
        scoringMatrix['F']['P'] = -5;
        scoringMatrix['F']['S'] = -3;
        scoringMatrix['F']['T'] = -3;
        scoringMatrix['F']['W'] = 0;
        scoringMatrix['F']['Y'] = 7;
        scoringMatrix['F']['V'] = -1;
        scoringMatrix['F']['B'] = -4;
        scoringMatrix['F']['Z'] = -5;
        scoringMatrix['F']['X'] = -2;
        scoringMatrix['P']['A'] = 1;
        scoringMatrix['P']['R'] = 0;
        scoringMatrix['P']['N'] = 0;
        scoringMatrix['P']['D'] = -1;
        scoringMatrix['P']['C'] = -3;
        scoringMatrix['P']['Q'] = 0;
        scoringMatrix['P']['E'] = -1;
        scoringMatrix['P']['G'] = 0;
        scoringMatrix['P']['H'] = 0;
        scoringMatrix['P']['I'] = -2;
        scoringMatrix['P']['L'] = -3;
        scoringMatrix['P']['K'] = -1;
        scoringMatrix['P']['M'] = -2;
        scoringMatrix['P']['F'] = -5;
        scoringMatrix['P']['P'] = 6;
        scoringMatrix['P']['S'] = 1;
        scoringMatrix['P']['T'] = 0;
        scoringMatrix['P']['W'] = -6;
        scoringMatrix['P']['Y'] = -5;
        scoringMatrix['P']['V'] = -1;
        scoringMatrix['P']['B'] = -1;
        scoringMatrix['P']['Z'] = 0;
        scoringMatrix['P']['X'] = -1;
        scoringMatrix['S']['A'] = 1;
        scoringMatrix['S']['R'] = 0;
        scoringMatrix['S']['N'] = 1;
        scoringMatrix['S']['D'] = 0;
        scoringMatrix['S']['C'] = 0;
        scoringMatrix['S']['Q'] = -1;
        scoringMatrix['S']['E'] = 0;
        scoringMatrix['S']['G'] = 1;
        scoringMatrix['S']['H'] = -1;
        scoringMatrix['S']['I'] = -1;
        scoringMatrix['S']['L'] = -3;
        scoringMatrix['S']['K'] = 0;
        scoringMatrix['S']['M'] = -2;
        scoringMatrix['S']['F'] = -3;
        scoringMatrix['S']['P'] = 1;
        scoringMatrix['S']['S'] = 2;
        scoringMatrix['S']['T'] = 1;
        scoringMatrix['S']['W'] = -2;
        scoringMatrix['S']['Y'] = -3;
        scoringMatrix['S']['V'] = -1;
        scoringMatrix['S']['B'] = 0;
        scoringMatrix['S']['Z'] = 0;
        scoringMatrix['S']['X'] = 0;
        scoringMatrix['T']['A'] = 1;
        scoringMatrix['T']['R'] = -1;
        scoringMatrix['T']['N'] = 0;
        scoringMatrix['T']['D'] = 0;
        scoringMatrix['T']['C'] = -2;
        scoringMatrix['T']['Q'] = -1;
        scoringMatrix['T']['E'] = 0;
        scoringMatrix['T']['G'] = 0;
        scoringMatrix['T']['H'] = -1;
        scoringMatrix['T']['I'] = 0;
        scoringMatrix['T']['L'] = -2;
        scoringMatrix['T']['K'] = 0;
        scoringMatrix['T']['M'] = -1;
        scoringMatrix['T']['F'] = -3;
        scoringMatrix['T']['P'] = 0;
        scoringMatrix['T']['S'] = 1;
        scoringMatrix['T']['T'] = 3;
        scoringMatrix['T']['W'] = -5;
        scoringMatrix['T']['Y'] = -3;
        scoringMatrix['T']['V'] = 0;
        scoringMatrix['T']['B'] = 0;
        scoringMatrix['T']['Z'] = -1;
        scoringMatrix['T']['X'] = 0;
        scoringMatrix['W']['A'] = -6;
        scoringMatrix['W']['R'] = 2;
        scoringMatrix['W']['N'] = -4;
        scoringMatrix['W']['D'] = -7;
        scoringMatrix['W']['C'] = -8;
        scoringMatrix['W']['Q'] = -5;
        scoringMatrix['W']['E'] = -7;
        scoringMatrix['W']['G'] = -7;
        scoringMatrix['W']['H'] = -3;
        scoringMatrix['W']['I'] = -5;
        scoringMatrix['W']['L'] = -2;
        scoringMatrix['W']['K'] = -3;
        scoringMatrix['W']['M'] = -4;
        scoringMatrix['W']['F'] = 0;
        scoringMatrix['W']['P'] = -6;
        scoringMatrix['W']['S'] = -2;
        scoringMatrix['W']['T'] = -5;
        scoringMatrix['W']['W'] = 17;
        scoringMatrix['W']['Y'] = 0;
        scoringMatrix['W']['V'] = -6;
        scoringMatrix['W']['B'] = -5;
        scoringMatrix['W']['Z'] = -6;
        scoringMatrix['W']['X'] = -4;
        scoringMatrix['Y']['A'] = -3;
        scoringMatrix['Y']['R'] = -4;
        scoringMatrix['Y']['N'] = -2;
        scoringMatrix['Y']['D'] = -4;
        scoringMatrix['Y']['C'] = 0;
        scoringMatrix['Y']['Q'] = -4;
        scoringMatrix['Y']['E'] = -4;
        scoringMatrix['Y']['G'] = -5;
        scoringMatrix['Y']['H'] = 0;
        scoringMatrix['Y']['I'] = -1;
        scoringMatrix['Y']['L'] = -1;
        scoringMatrix['Y']['K'] = -4;
        scoringMatrix['Y']['M'] = -2;
        scoringMatrix['Y']['F'] = 7;
        scoringMatrix['Y']['P'] = -5;
        scoringMatrix['Y']['S'] = -3;
        scoringMatrix['Y']['T'] = -3;
        scoringMatrix['Y']['W'] = 0;
        scoringMatrix['Y']['Y'] = 10;
        scoringMatrix['Y']['V'] = -2;
        scoringMatrix['Y']['B'] = -3;
        scoringMatrix['Y']['Z'] = -4;
        scoringMatrix['Y']['X'] = -2;
        scoringMatrix['V']['A'] = 0;
        scoringMatrix['V']['R'] = -2;
        scoringMatrix['V']['N'] = -2;
        scoringMatrix['V']['D'] = -2;
        scoringMatrix['V']['C'] = -2;
        scoringMatrix['V']['Q'] = -2;
        scoringMatrix['V']['E'] = -2;
        scoringMatrix['V']['G'] = -1;
        scoringMatrix['V']['H'] = -2;
        scoringMatrix['V']['I'] = 4;
        scoringMatrix['V']['L'] = 2;
        scoringMatrix['V']['K'] = -2;
        scoringMatrix['V']['M'] = 2;
        scoringMatrix['V']['F'] = -1;
        scoringMatrix['V']['P'] = -1;
        scoringMatrix['V']['S'] = -1;
        scoringMatrix['V']['T'] = 0;
        scoringMatrix['V']['W'] = -6;
        scoringMatrix['V']['Y'] = -2;
        scoringMatrix['V']['V'] = 4;
        scoringMatrix['V']['B'] = -2;
        scoringMatrix['V']['Z'] = -2;
        scoringMatrix['V']['X'] = -1;
        scoringMatrix['B']['A'] = 0;
        scoringMatrix['B']['R'] = -1;
        scoringMatrix['B']['N'] = 2;
        scoringMatrix['B']['D'] = 3;
        scoringMatrix['B']['C'] = -4;
        scoringMatrix['B']['Q'] = 1;
        scoringMatrix['B']['E'] = 3;
        scoringMatrix['B']['G'] = 0;
        scoringMatrix['B']['H'] = 1;
        scoringMatrix['B']['I'] = -2;
        scoringMatrix['B']['L'] = -3;
        scoringMatrix['B']['K'] = 1;
        scoringMatrix['B']['M'] = -2;
        scoringMatrix['B']['F'] = -4;
        scoringMatrix['B']['P'] = -1;
        scoringMatrix['B']['S'] = 0;
        scoringMatrix['B']['T'] = 0;
        scoringMatrix['B']['W'] = -5;
        scoringMatrix['B']['Y'] = -3;
        scoringMatrix['B']['V'] = -2;
        scoringMatrix['B']['B'] = 3;
        scoringMatrix['B']['Z'] = 2;
        scoringMatrix['B']['X'] = -1;
        scoringMatrix['Z']['A'] = 0;
        scoringMatrix['Z']['R'] = 0;
        scoringMatrix['Z']['N'] = 1;
        scoringMatrix['Z']['D'] = 3;
        scoringMatrix['Z']['C'] = -5;
        scoringMatrix['Z']['Q'] = 3;
        scoringMatrix['Z']['E'] = 3;
        scoringMatrix['Z']['G'] = 0;
        scoringMatrix['Z']['H'] = 2;
        scoringMatrix['Z']['I'] = -2;
        scoringMatrix['Z']['L'] = -3;
        scoringMatrix['Z']['K'] = 0;
        scoringMatrix['Z']['M'] = -2;
        scoringMatrix['Z']['F'] = -5;
        scoringMatrix['Z']['P'] = 0;
        scoringMatrix['Z']['S'] = 0;
        scoringMatrix['Z']['T'] = -1;
        scoringMatrix['Z']['W'] = -6;
        scoringMatrix['Z']['Y'] = -4;
        scoringMatrix['Z']['V'] = -2;
        scoringMatrix['Z']['B'] = 2;
        scoringMatrix['Z']['Z'] = 3;
        scoringMatrix['Z']['X'] = -1;
        scoringMatrix['X']['A'] = 0;
        scoringMatrix['X']['R'] = -1;
        scoringMatrix['X']['N'] = 0;
        scoringMatrix['X']['D'] = -1;
        scoringMatrix['X']['C'] = -3;
        scoringMatrix['X']['Q'] = -1;
        scoringMatrix['X']['E'] = -1;
        scoringMatrix['X']['G'] = -1;
        scoringMatrix['X']['H'] = -1;
        scoringMatrix['X']['I'] = -1;
        scoringMatrix['X']['L'] = -1;
        scoringMatrix['X']['K'] = -1;
        scoringMatrix['X']['M'] = -1;
        scoringMatrix['X']['F'] = -2;
        scoringMatrix['X']['P'] = -1;
        scoringMatrix['X']['S'] = 0;
        scoringMatrix['X']['T'] = 0;
        scoringMatrix['X']['W'] = -4;
        scoringMatrix['X']['Y'] = -2;
        scoringMatrix['X']['V'] = -1;
        scoringMatrix['X']['B'] = -1;
        scoringMatrix['X']['Z'] = -1;
        scoringMatrix['X']['X'] = -1;
    } //end of scoringMatrix static initialization

}



