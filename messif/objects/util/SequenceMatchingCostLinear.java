/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.objects.util;

/**
 * Approximate costs of a pair of objects is measured as a linear function of their distance (which must be within
 * the interval ({@link SequenceMatchingCost#distMatch};{@link SequenceMatchingCost#distMismatch}].
 * 
 * The approximate costs is a return value of a continuous function from the interval 
 * [{@link SequenceMatchingCost#matchExact};{@link SequenceMatchingCost#matchApprox].
 * 
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 */
public class SequenceMatchingCostLinear extends SequenceMatchingCost {
    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;

    /** The same setting as {@link SequenceMatchingCost#SIFT_DEFAULT} */
    public static SequenceMatchingCost DEFAULT = new SequenceMatchingCostLinear();

    public SequenceMatchingCostLinear() {
    }

    public SequenceMatchingCostLinear(float gapOpen, float gapCont, float match, float semimatch, float mismatch, float matchDist, float mismatchDist) {
        super(gapOpen, gapCont, match, semimatch, mismatch, matchDist, mismatchDist);
    }

    @Override
    protected float getApproxCost(float dist) {
        return matchExact + ((dist - distMatch)*(matchExact - matchApprox))/(distMatch - distMismatch);
    }
}
