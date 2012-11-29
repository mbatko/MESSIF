/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.objects.util;

/**
 * Approximate costs of a pair of objects is measured as an inverse function of their distance (which must be within
 * the interval ({@link SequenceMatchingCost#distMatch};{@link SequenceMatchingCost#distMismatch}].
 * 
 * The approximate costs is a return value of a continuous function from the interval 
 * [{@link SequenceMatchingCost#matchExact};{@link SequenceMatchingCost#matchApprox].
 * 
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 */
public class SequenceMatchingCostInverse extends SequenceMatchingCost {
    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;

    /** The same setting as {@link SequenceMatchingCost#SIFT_DEFAULT} */
    public static SequenceMatchingCost DEFAULT = new SequenceMatchingCostInverse();
    
    public SequenceMatchingCostInverse() {
    }

    public SequenceMatchingCostInverse(float gapOpen, float gapCont, float match, float semimatch, float mismatch, float matchDist, float mismatchDist) {
        super(gapOpen, gapCont, match, semimatch, mismatch, matchDist, mismatchDist);
    }

    @Override
    protected float getApproxCost(float dist) {
        // norma vzdalenosti mezi 0..1
        float d1 = (dist - distMatch)/(distMismatch - distMatch);
        // prevod na krivku 1/x mezi 10 a 0.5 (tedy 1/0.1 a 1/2)
        float d2 = (1/((2-0.1f)*d1+0.1f));
        // Prevod z 10..0.5 na matchExact..matchApprox
        return (d2 - 10) / (10-0.5f) * (matchExact - matchApprox) + matchExact;
    }
    
}
