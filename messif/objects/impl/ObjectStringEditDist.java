/*
 * Object.java
 *
 * Created on 3. kveten 2003, 20:09
 */

package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import messif.objects.LocalAbstractObject;


/**
 *
 * @author Vlastislav Dohnal, xdohnal@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
public class ObjectStringEditDist extends ObjectString {
    
    /** class id for serialization */
    private static final long serialVersionUID = 1L;
    
    /****************** Constructors ******************/
    
    /** Creates a new instance of Object */
    public ObjectStringEditDist(String text) {
        super(text);
    }
    
    /** Creates a new instance of Object random generated */
    public ObjectStringEditDist() {
        super();
    }
    
    /** Creates a new instance of Object random generated 
     * with minimal length equal to minLength and maximal 
     * length equal to maxLength */
    public ObjectStringEditDist(int minLength, int maxLength) {
        super(minLength, maxLength);
    }
    
    /** Creates a new instance of Object from stream */
    public ObjectStringEditDist(BufferedReader stream) throws IOException {
        super(stream);
    }
    

    /****************** Metric function ******************/
    private static int min(int a, int b, int c) {
        int mi = a;
        if (b < mi) mi = b;
        if (c < mi) mi = c;

        return mi;
    }
    
    /** Function to obtain weight of changing char1 into char2 during the edit distance computation.
        The function MUST be symetric. */
    protected int getChangeWeight(char chr1, char chr2) {
        return (chr1 == chr2)?0:1;
    }
    
    /** Weight of the insert and delete operations during the edit distance computation */
    protected int insertDeleteWeight = 1;

    /*
     * Old implementation of the edit distance which uses a matrix. 
     * This has been superseded by a more economical implementation 
     * which employes just an array.
     *
    public float getDistance(LocalAbstractObject obj) {
        counterDistanceComputations.add();
        
        int str1Len = this.text.length();
        int str2Len = ((ObjectString)obj).text.length();

        // Singularities
        if (str1Len == 0) return str2Len;
        if (str2Len == 0) return str1Len;

        // Initialize matrix
        int matrix[][] = new int[str1Len + 1][str2Len + 1];
        for (int i = 0; i <= str1Len; i++) matrix[i][0] = i;
        for (int j = 0; j <= str2Len; j++) matrix[0][j] = j;

        // Fill matrix
        for (int i = 1; i <= str1Len; i++) {
            char str1chr = this.text.charAt(i - 1);
            
            for (int j = 1; j <= str2Len; j++) {
                char str2chr = ((ObjectString)obj).text.charAt(j - 1);
                
                // Computation of one edit step
                matrix[i][j] = min(
                    matrix[i-1][j]+1, 
                    matrix[i][j-1]+1, 
                    matrix[i-1][j-1] + ((str1chr == str2chr)?0:1)
                );
            }
        }

        // Return value    
        return matrix[str1Len][str2Len];
    }
    */
    
    /** Metric distance function.
     *    This method is intended to be used in situations such as:
     *    We are executing a range query, so all objects distant from the query object up to the query radius
     *    must be returned. In other words, all objects farther from the query object than the query radius are
     *    uninteresting. From the distance point of view, during the evaluation process of the distance between 
     *    a pair of objects we can find out that the distance cannot be lower than a certain value. If this value 
     *    is greater than the query radius, we can safely abort the distance evaluation since we are dealing with one
     *    of those uninteresting objects.
     * @param obj            The object to compute distance to.
     * @param distThreshold  The threshold value on the distance (the query radius from the example above).
     * @return Returns the actual distance between obj and this if the distance is lower than distThreshold.
     *         Otherwise the returned value is not guaranteed to be exact, but in this respect the returned value
     *         must be greater than the threshold distance.
     *
     *  When defining this method do not forget to add the following line 
     *      counterDistanceComputations.add();
     *  for statistics to be maintained. This is correct because we compute the actual distance but we can end
     *  prematurely.
     */
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        int str1Len = this.text.length();
        int str2Len = ((ObjectString)obj).text.length();

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
    
    /** Lower Bound Metric distance function
     *    Returns lower bound on distance between obj1 and obj2 (supplied as arguments).
     *    The function can provide several levels of precision which is specified by 
     *    argument 'accuracy'.
     */
    public float getDistanceLowerBound(LocalAbstractObject obj, int accuracy) {
        counterLowerBoundDistanceComputations.add();
        return Math.abs(this.text.length() - ((ObjectString)obj).text.length()) * insertDeleteWeight;
    }
    
    /** Upper Bound Metric distance function
     *    Returns upper bound on distance between obj1 and obj2 (supplied as arguments).
     *    The function can provide several levels of precision which is specified by 
     *    argument 'accuracy'.
     */
    public float getDistanceUpperBound(LocalAbstractObject obj, int accuracy) {
        counterUpperBoundDistanceComputations.add();
        return Math.abs(this.text.length() + ((ObjectString)obj).text.length()) * insertDeleteWeight;
    }

}
