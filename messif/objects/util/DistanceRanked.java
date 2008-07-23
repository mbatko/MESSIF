/*
 * DistanceRanked
 * 
 */

package messif.objects.util;

import java.util.Comparator;


/**
 * Interface for marking objects that can be ranked according to distance.
 * 
 * @see messif.operations.RankingQueryOperation
 * @see 
 * @author xbatko
 */
public interface DistanceRanked {

    /**
     * Default comparator that can be used on DistanceRanked objects.
     * This comparator compares the objects based on their distance rank.
     */
    public static Comparator<DistanceRanked> comparator = new Comparator<DistanceRanked>() {
        public int compare(DistanceRanked o1, DistanceRanked o2) {
            return Float.compare(o1.getDistance(), o2.getDistance());
        }
    };

    /**
     * Returns the ranking distance.
     * @return the ranking distance
     */
    public float getDistance();

}
