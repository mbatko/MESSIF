/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.objects.util;

/**
 * Only exact match score is counted, i.e. if the distance between a pair of objects is zero, {@link SequenceMatchingCost#matchExact}
 * cost is returned, otherwise the zero cost is returned.
 *
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 */
public class SequenceMatchingCostEquality extends SequenceMatchingCost {
    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;

    /** Default scores: gap opening 0.3, gap continue 0.05, exact match score 5. Only exact matches are accepted, i.e.
     * pairs of objects whose mutual distance is zero.
     */
    public static SequenceMatchingCost DEFAULT = new SequenceMatchingCost();
    
    public SequenceMatchingCostEquality() {
        super(0.3f, 0.05f, 5f, 0f, 0f, 0f, 0f);
    }

    public SequenceMatchingCostEquality(float gapOpen, float gapCont, float match, float semimatch, float mismatch, float matchDist, float mismatchDist) {
        super(gapOpen, gapCont, match, semimatch, mismatch, matchDist, mismatchDist);
    }

}
