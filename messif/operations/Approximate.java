/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.operations;

/**
 *
 * @author xbatko
 */
public interface Approximate {
    /**
     * Enumeration of types of the stop condition for approximation early termination strategy.
     */
    public static enum LocalSearchType {
        /**
         * Stop after inspecting given percentage of data.
         * The {@link #getLocalSearchParam() parameter} holds the value between 0-100.
         */
        PERCENTAGE,
        /**
         * Stop after inspecting the specific number of objects.
         * The {@link #getLocalSearchParam() parameter} is the number of objects.
         */
        ABS_OBJ_COUNT,
        /**
         * Stop after the specific number of evaluations of distance functions.
         * The {@link #getLocalSearchParam() parameter} is the threshold on the number of distance computations.
         */
        ABS_DC_COUNT,
        /**
         * Stop after a specific number of "data regions" (buckets, clusters) is accessed and searched.
         * The {@link #getLocalSearchParam() parameter} is the limit on "data regions" (partitions, buckets, clusters) to be accessed.
         */
        DATA_PARTITIONS,
        /**
         * Stop after a specific number of I/O operations (page reads)..
         * The {@link #getLocalSearchParam() parameter} is the limit on "data regions" (partitions, buckets, clusters) to be accessed.
         */
        BLOCK_READS
    }
    
    /**
     * Returns the {@link LocalSearchType type of the local approximation} parameter used.
     * @return the {@link LocalSearchType type of the local approximation} parameter used
     */
    public LocalSearchType getLocalSearchType();

    /**
     * Returns the value of the local approximation parameter.
     * Its interpretation depends on the value of {@link #getLocalSearchType() local search type}.
     * @return the value of the local approximation parameter
     */
    public int getLocalSearchParam();

    /**
     * Setter for the type of the local search parameter.
     * @param localSearchType new type of the local search parameter
     */
    public void setLocalSearchType(LocalSearchType localSearchType);

    /**
     * Setter for the local search parameter value.
     * Its interpretation depends on the value of {@link #getLocalSearchType() local search type}.
     * @param localSearchParam new value for the local search parameter
     */
    public void setLocalSearchParam(int localSearchParam);

    /**
     * Returns a currently set value of radius within which the results are guaranteed as correct.
     * An evaluation algorithm is completely responsible for setting the correct value.
     * @return the value of the currently guaranteed radius
     */
    public float getRadiusGuaranteed();

    /**
     * Set a different value of radius within which the results are guaranteed as correct.
     * An evaluation algorithm is completely responsible for setting the correct value.
     * @param radiusGuaranteed new guaranteed radius value
     */
    public void setRadiusGuaranteed(float radiusGuaranteed);

}
