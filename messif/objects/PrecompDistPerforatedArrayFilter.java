
package messif.objects;

/**
 *
 * @author David Novak, FI Masaryk University, Brno, Czech Republic; <a href="mailto:xnovak8@fi.muni.cz">xnovak8@fi.muni.cz</a>
 */
public class PrecompDistPerforatedArrayFilter extends PrecomputedDistancesFixedArrayFilter {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    /** Creates a new instance of ProcmpDistPerforatedArrayFilter */
    public PrecompDistPerforatedArrayFilter() {
    }

    /** Creates a new instance of ProcmpDistPerforatedArrayFilter */
    public PrecompDistPerforatedArrayFilter(int initialSize) {
        super(initialSize);
    }
    
    protected boolean excludeUsingPrecompDistImpl(PrecomputedDistancesFixedArrayFilter targetFilter, float radius) {
        // We have no precomputed distances either in the query or this object
        if (precompDist == null || targetFilter.precompDist == null)
            return false;
        
        // Traverse the precomputed distances by array
        int maxIndex = Math.min(actualSize, targetFilter.actualSize);
        for (int i = 0; i < maxIndex; i++)
            if ((precompDist[i] != LocalAbstractObject.UNKNOWN_DISTANCE) && (targetFilter.precompDist[i] != LocalAbstractObject.UNKNOWN_DISTANCE) && (Math.abs(precompDist[i] - targetFilter.precompDist[i]) > radius))
                return true;        
        return false;
    }

    protected boolean includeUsingPrecompDistImpl(PrecomputedDistancesFixedArrayFilter targetFilter, float radius) {
        // We have no precomputed distances either in the query or this object
        if (precompDist == null || targetFilter.precompDist == null)
            return false;
        
        // Traverse the precomputed distances by array
        int maxIndex = Math.min(actualSize, targetFilter.actualSize);
        for (int i = 0; i < maxIndex; i++)
            if ((precompDist[i] != LocalAbstractObject.UNKNOWN_DISTANCE) && (targetFilter.precompDist[i] != LocalAbstractObject.UNKNOWN_DISTANCE) && (Math.abs(precompDist[i] + targetFilter.precompDist[i]) <= radius))
                return true;        
        return false;
    }

}
