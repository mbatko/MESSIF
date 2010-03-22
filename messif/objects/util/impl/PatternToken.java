
package messif.objects.util.impl;

/**
 * This is an abstract ancestor for individual tokens that can appear in
 *   aggregation function string.
 *
 * @author xnovak8
 */
public interface PatternToken extends java.io.Serializable {

    /** Given an array of subdistances, each token must return a float value.
     * @param subdistances array of subdistances corresponding to the pattern
     * @return partial value of this part of the aggregation function pattern
     */
    abstract public float evaluate(float [] subdistances);

}
