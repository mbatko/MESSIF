
package messif.objects.util.impl;

import java.util.List;

/**
 * The subdistance identifier for the aggregation function evalutator.
 * @author xnovak8
 */
public class SubdistanceToken implements PatternToken {

    /** Class id for object serialization. */
    private static final long serialVersionUID = 1L;

    /** Subdistance string name */
    protected final String name;

    /** Subdistance index */
    protected final int index;

    public String getName() {
        return name;
    }

    /**
     * Create subdistance token given the subdistance name and a list of already created subdistances.
     *  If the passed name is present in the list, the current index is used or the distance is added
     *  in the end of the passed list.
     * 
     * @param subdistanceName name of the subdistance (e.g. ScalableColor)
     * @param currentSubdistanceList list of subdistances that already appeared in the aggregation string
     */
    public SubdistanceToken(String subdistanceName, List<SubdistanceToken> currentSubdistanceList) {
        this.name = subdistanceName;
        for (int i = 0; i < currentSubdistanceList.size(); i++) {
            if (subdistanceName.equals(currentSubdistanceList.get(i).getName())) {
                this.index = i;
                return;
            }
        }
        currentSubdistanceList.add(this);
        this.index = currentSubdistanceList.size() - 1;
    }

    /**
     * The subdistance knows a priori its index to the array of subdistances
     * @param subdistances specific subdistances for the two meta objects compared
     * @return one of the subdistances given
     */
    public final float evaluate(float[] subdistances) {
        return subdistances[index];
    }

    @Override
    public String toString() {
        return name;
    }

}
