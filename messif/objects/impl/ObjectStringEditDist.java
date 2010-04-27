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
import messif.objects.LocalAbstractObject;


/**
 * Object with string content and <em>edit distance</em> metric function.
 * See relevant literature (e.g. a book "Similarity Search: The Metric Space Approach")
 * for a definition of the edit distance function.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ObjectStringEditDist extends ObjectString {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Constants ******************//

    /** Default weight of the character insertion, deletion, or change operations during the computation of the metric function */
    private static int DEFAULT_WEIGHT = 1;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of ObjectStringEditDist.
     * @param text the string content of the new object
     */
    public ObjectStringEditDist(String text) {
        super(text);
    }

    /**
     * Creates a new instance of ObjectStringEditDist with randomly generated string content.
     */
    public ObjectStringEditDist() {
        super();
    }
    
    /**
     * Creates a new instance of ObjectStringEditDist with randomly generated string content.
     * The string content is genereated with at least {@code minLength} characters
     * and at most {@code maxLength} characters.
     *
     * @param minLength minimal length of the randomly generated string content
     * @param maxLength maximal length of the randomly generated string content
     */
    public ObjectStringEditDist(int minLength, int maxLength) {
        super(minLength, maxLength);
    }
    
    /**
     * Creates a new instance of ObjectString from text stream.
     * @param stream the stream from which to read lines of text
     * @throws EOFException if the end-of-file of the given stream is reached
     * @throws IOException if there was an I/O error during reading from the stream
     */
    public ObjectStringEditDist(BufferedReader stream) throws EOFException, IOException {
        super(stream);
    }


    //****************** Metric function ******************//

    /**
     * Computes a minimum of the three integer values.
     * @param a first integer value
     * @param b second integer value
     * @param c third integer value
     * @return a minumum of the tree values
     */
    private static int min(int a, int b, int c) {
        int mi = a;
        if (b < mi) mi = b;
        if (c < mi) mi = c;

        return mi;
    }

    /**
     * Returns the weight of changing {@code char1} into {@code char2} during
     * the edit distance computation. The returned value must be greater than or
     * equal to zero and must be symetric, that is {@code getChangeWeight(x,y) == getChangeWeight(y,x)}.
     * @param chr1 the source character
     * @param chr2 the target character
     * @return the weight of changing {@code char1} into {@code char2}
     */
    protected int getChangeWeight(char chr1, char chr2) {
        return chr1 == chr2 ? 0 : DEFAULT_WEIGHT;
    }

    /**
     * Returns the weight of deleting or inserting a character during
     * the edit distance computation. The returned value must be greater than or
     * equal to zero.
     * @return the weight of deleting or inserting a character
     */
    protected int getInsertDeleteWeight() {
        return DEFAULT_WEIGHT;
    }

    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        int str1Len = this.text.length();
        int str2Len = ((ObjectString)obj).text.length();
        int insertDeleteWeight = getInsertDeleteWeight();

        // Singularities  (Step 1)
        if (str1Len == 0) 
            return str2Len * insertDeleteWeight;
        if (str2Len == 0) 
            return str1Len * insertDeleteWeight;
        int minD = Math.abs(str2Len-str1Len) * insertDeleteWeight;
        if (minD > distThreshold)
            return minD;

        // Allocate array
        int[] d = new int[str2Len + 1];

        // Initialize the array (Step 2)
        for (int j = 1; j <= str2Len; j++)
            d[j] = j * insertDeleteWeight;                  // Initialing insertion of all characters in the second string???

        // For all characters in the string1 (Step 3)
        for (int i = 1; i <= str1Len; i++) {
            char str1chr = this.text.charAt(i - 1);
            int minDist = i * insertDeleteWeight;           // lower bound on distance (Deleting i letters from the first string ???)

            d[0] = minDist;                                 // Deleting i letters from the first string ???
            int prevDiagElem = (i-1) * insertDeleteWeight;  // Remember the previous diagonal element.

            // For all characters in the string2 (Step 4)
            for (int j = 1; j <= str2Len; j++) {
                char str2chr = ((ObjectString)obj).text.charAt(j - 1);

                // Compute new diagonal element (Step 5)
                int diag = prevDiagElem + getChangeWeight(str1chr, str2chr);

                // Update the previous diagonal element
                prevDiagElem = d[j];

                // Compute the above element (Step 6)
                int above = d[j] + insertDeleteWeight;
                int left = d[j-1] + insertDeleteWeight;

                // Store minimum of diag, above and left values
                d[j] = min(diag, above, left);
                if (d[j] < minDist)
                    minDist = d[j];
            }

            // Test the condition to end prematurely
            if (minDist > distThreshold)
                return minDist;
        }

        // Return the correct edit distance (Step 7)
        return d[str2Len];
    }

    @Override
    protected float getDistanceLowerBoundImpl(LocalAbstractObject obj, int accuracy) {
        return Math.abs(this.text.length() - ((ObjectString)obj).text.length()) * getInsertDeleteWeight();
    }
    
    @Override
    protected float getDistanceUpperBoundImpl(LocalAbstractObject obj, int accuracy) {
        return Math.abs(this.text.length() + ((ObjectString)obj).text.length()) * getInsertDeleteWeight();
    }

}
