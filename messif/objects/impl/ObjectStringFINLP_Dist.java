/*
 * ObjectStringFINLP_Dist.java
 *
 * Created on 4. listopad 2004, 13:27
 */

package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import messif.objects.LocalAbstractObject;


/**
 *
 * @author  Vlastislav Dohnal, xdohnal@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 *
 * Class that implements a distance function specially taylored to NLP-FI lab needs.
 * This distance function is based on comparison of whole paragraphs of text. It considers only words as
 * the basic unit and does not use the edit distance measure to check difference between individual words.
 */
public class ObjectStringFINLP_Dist extends ObjectString {
    
    /** class id for serialization */
    private static final long serialVersionUID = 1L;
    
    /** Creates a new instance of ObjectStringFINLP_Dist */
    public ObjectStringFINLP_Dist(String text) {
        super(text);
    }
    
    /** Creates a new instance of Object random generated */
    public ObjectStringFINLP_Dist() {
        // each data object is of format: <DOCUMENT_ID>|free text
        // The format of <DOCUMENT_ID> is <DOCUMENT>:<PARAGRAPH>
        super("foo:1|" + ObjectString.generateRandom());
    }
    
    /** Creates a new instance of Object from stream */
    public ObjectStringFINLP_Dist(BufferedReader stream) throws IOException {
        super(stream);
    }
    

    /****************** Metric function ******************/
    private static String[] getWords(ObjectStringFINLP_Dist obj) {
        String data = obj.text.substring(obj.text.indexOf("|") + 1);
        
        data = data.toLowerCase();
        return data.split("\\s+");
    }
    
    private static int Minimum (int a, int b, int c) {
        int mi = a;
        if (b < mi) mi = b;
        if (c < mi) mi = c;

        return mi;
    }

    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        String[] o1_words = getWords(this);
        String[] o2_words = getWords((ObjectStringFINLP_Dist)obj);
        
        // o1_words should be the shorter array
        if (o1_words.length > o2_words.length) {
            String[] aux = o2_words;
            o2_words = o1_words;
            o1_words = aux;
        }
        
        // go throught the longer array and try to find the shorer one as a subsequence.
        for (int i = 0; i < o2_words.length - o1_words.length + 1; i++) {
            if (o1_words[0].equals(o2_words[i])) {
                // the begging matches, try to match the rest
                boolean found = true;
                for (int j = 1; j < o1_words.length; j++) {
                    // if any of the words does not match, quit the cycle and try to find it again on next positions
                    if (! o1_words[j].equals(o2_words[i+j])) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    // the sequence o1_words has been found in the sequence o2_words
                    return 0;
                }
            }
        }
        
        // the sequence o1_words was not found in o2_words, return the length of the shorter sequence
        return o1_words.length;
  
        // simple normalized differnce of lengths
        //return (Math.abs(o1_words.length - o2_words.length) / Math.max(o1_words.length, o2_words.length));
        
/*
        
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
                matrix[i][j] = Minimum(
                    matrix[i-1][j]+1, 
                    matrix[i][j-1]+1, 
                    matrix[i-1][j-1] + ((str1chr == str2chr)?0:1)
                );
            }
        }

        // Return value    
        return matrix[str1Len][str2Len];*/
    }

    public float getDistanceLowerBound(LocalAbstractObject obj, int accuracy) {
        return 0;
/*      counterLowerBoundDistanceComputations.add();
        return Math.abs(this.text.length() - ((ObjectString)obj).text.length());*/
    }
    
    public float getDistanceUpperBound(LocalAbstractObject obj, int accuracy) {
        return 0;
/*      counterUpperBoundDistanceComputations.add();
        return Math.abs(this.text.length() + ((ObjectString)obj).text.length());*/
    }
    
}
