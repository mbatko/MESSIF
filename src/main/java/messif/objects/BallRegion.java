/*
 *  This file is part of MESSIF library.
 *
 *  MESSIF library is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MESSIF library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MESSIF library.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.objects;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import messif.utility.Convert;

/**
 * This class represents a ball region, i.e. a partition of the metric space that
 * holds objects that are within a specified radius from the central object (pivot).
 *
 * The distance function is compatible if and only if the pivot distance function is compatible.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class BallRegion extends LocalAbstractObject {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /****************** Ball region attributes ******************/

    /** Center of the ball region */
    protected LocalAbstractObject pivot;

    /** Radius of this region */
    protected float radius;


    /****************** Constructors ******************/

    /**
     * Creates a new instance of BallRegion with initially empty ball region
     */
    public BallRegion() {
        this(null, LocalAbstractObject.MIN_DISTANCE);
    }

    /**
     * Creates a new instance of BallRegion with specified pivot and radius
     * @param pivot the pivot for the new ball region
     * @param radius the radius for the new ball region
     */
    public BallRegion(LocalAbstractObject pivot, float radius) {
        this.pivot = pivot;
        this.radius = radius;
    }


    /****************** Serialization ******************/

    /**
     * Creates a new instance of BallRegion from stream.
     * @param stream the stream to load ball region from
     * @throws IOException if an error occurs during reading from the stream
     * @throws NumberFormatException if the stream's object is not valid ball region
     * @throws IllegalArgumentException if the stream's object is not valid ball region
     * @throws ClassNotFoundException if the stream's object is not valid ball region
     */
    public BallRegion(BufferedReader stream) throws IOException, NumberFormatException, IllegalArgumentException, ClassNotFoundException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line = readObjectComments(stream);
        
        int pos = line.indexOf(';');
        if (pos == -1)
            throw new IllegalArgumentException("Wrong BallRegion object format");
        radius = Float.parseFloat(line.substring(0, pos));
        if (pos < line.length() - 1) {
            // There is a specifier for pivot object
            try {
                pivot = Convert.getClassForName(line.substring(pos + 1), LocalAbstractObject.class).getConstructor(BufferedReader.class).newInstance(stream);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("Constructor is missing", e);
            } catch (InstantiationException e) {
                throw new IllegalArgumentException("Specified class is abstract", e);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("The stream constructor is not accessible", e);
            } catch (InvocationTargetException e) {
                throw new IllegalArgumentException("There was an error creating object", e.getCause());
            }
        }
    }

    /**
     * Store this object's data to a text stream.
     * This method have the opposite deserialization in constructor.
     *
     * @param stream the stream to store this object to
     * @throws IOException if there was an error while writing to stream
     */
    @Override
    protected void writeData(OutputStream stream) throws IOException {
        stream.write(String.valueOf(this.radius).getBytes());
        stream.write(';');
        if (pivot != null) {
            stream.write(pivot.getClass().getName().getBytes());
            stream.write('\n');
            pivot.write(stream);
        } else stream.write('\n');
        
    }


    /****************** Data getter/setter methods ******************/

    /**
     * Returns current radius of this ball region.
     * @return current radius of this ball region
     */
    public float getRadius() {
        return radius;
    }

    /**
     * Returns current pivot of this ball region.
     * @return current pivot of this ball region
     */
    public LocalAbstractObject getPivot() {
        return pivot;
    }

    /**
     * Sets the radius for this ball region.
     * @param radius the new radius
     * @throws IllegalArgumentException if the specified radius is negative
     */
    public void setRadius(float radius) throws IllegalArgumentException {
        if (radius < 0)
            throw new IllegalArgumentException("Radius must be non-negative");
        this.radius = radius;
    }


    /**
     * Sets the pivot for this ball region.
     * Note, that the radius is set to {@link messif.objects.LocalAbstractObject#MAX_DISTANCE} if <code>updateRadius</code> is <tt>true</tt>.
     *
     * @param pivot the new pivot
     * @param updateRadius specifies whether to update the region's radius or not
     */
    public void setPivot(LocalAbstractObject pivot, boolean updateRadius) {
        this.pivot = pivot;
        if (updateRadius)
            this.radius = LocalAbstractObject.MAX_DISTANCE;
    }


    /****************** Distance functions ******************/

    /**
     * Metric distance function that measures the distance of an arbitrary object to this region.
     * If the object is within this region, the distance is zero.
     * Otherwise, the distance is measured from this region's boundary.
     *
     * @param obj the object to compute distance to
     * @param distThreshold the threshold value on the distance
     * @return the actual distance between obj and this region if the distance is lower than distThreshold.
     *         Otherwise the returned value is not guaranteed to be exact, but in this respect the returned value
     *         must be greater than the threshold distance.
     */
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        if (obj instanceof BallRegion)
            return getDistanceRegionImpl((BallRegion)obj, distThreshold);
        if (pivot == null)
            return LocalAbstractObject.MAX_DISTANCE;
        float distance = pivot.getDistanceImpl(obj, distThreshold + radius); // Do not increment statistics
        if (distance <= radius)
            return 0;
        else return distance - radius;
    }

    /**
     * Metric distance function between two ball regions.
     * @param region         the ball region to compute distance to
     * @param distThreshold  the threshold value on the distance
     * @return returns the actual distance between the specified region and this one if the distance is lower than distThreshold.
     *         Otherwise the returned value is not guaranteed to be exact, but in this respect the returned value
     *         must be greater than the threshold distance.
     */
    public float getDistanceRegion(BallRegion region, float distThreshold) {
        return getDistanceRegion(region.pivot, region.radius, distThreshold);
    }

    /**
     * Metric distance function between two ball regions.
     * @param regionPivot    the pivot (center) of the ball region to compute distance to
     * @param regionRadius   the radius of the ball region to compute distance to
     * @param distThreshold  the threshold value on the distance
     * @return returns the actual distance between the specified region and this one if the distance is lower than distThreshold.
     *         Otherwise the returned value is not guaranteed to be exact, but in this respect the returned value
     *         must be greater than the threshold distance.
     */
    public float getDistanceRegion(LocalAbstractObject regionPivot, float regionRadius, float distThreshold) {
        return getDistanceRegionImpl(pivot.getDistance(regionPivot, distThreshold + radius + regionRadius), regionRadius);
    }

    /**
     * Metric distance function between two ball regions.
     * Wrapper method to avoid incrementing statistics, because the statistics have already been incremented.
     * @param region         the ball region to compute distance to
     * @param distThreshold  the threshold value on the distance
     * @return returns the actual distance between the specified region and this one if the distance is lower than distThreshold.
     *         Otherwise the returned value is not guaranteed to be exact, but in this respect the returned value
     *         must be greater than the threshold distance.
     */
    protected float getDistanceRegionImpl(BallRegion region, float distThreshold) {
        return getDistanceRegionImpl(pivot.getDistanceImpl(region.pivot, distThreshold + radius + region.radius), region.radius);
    }

    /**
     * Actual computation of the distance if the distance between regions' pivots is known.
     *
     * @param regionsPivotDistance the distance of the regions' pivots
     * @param regionRadius the radius of the ball region to compute distance to
     * @return returns the actual distance between the two regions
     */
    protected float getDistanceRegionImpl(float regionsPivotDistance, float regionRadius) {
        if (regionsPivotDistance <= radius + regionRadius)
            return 0;
        else return regionsPivotDistance - radius - regionRadius;
    }

    /**
     * Returns the covering overlap (as distance) between this region and the
     * region specified by <code>regionPivot</code> and <code>regionRadius</code>.
     * If the two regions touch but have no common area, zero is returned.
     * If the two regions do not even touch, this method returns negative value.
     *
     * @param regionPivot    the pivot (center) of the ball region to get the coverage for
     * @param regionRadius   the radius of the ball region to get the coverage for
     * @return returns the covering overlap (as distance) between this region and the specified one
     */
    public float getOverlapWith(LocalAbstractObject regionPivot, float regionRadius) {
        return (radius + regionRadius) - pivot.getDistance(regionPivot);
    }

    /**
     * Returns <tt>true</tt> if this ball region covers at least <code>distThreshold</code> area of the ball
     * region specified by <code>regionPivot</code> and <code>regionRadius</code>.
     * The <code>distThreshold</code> is the maximal distance that a region's object can be outside this region.
     * If the threshold is zero, the evaluated region must be fully within this one.
     * If the two regions do not even touch, this method returns <tt>false</tt> whatever the value of the threshold is.
     *
     * @param regionPivot    the pivot (center) of the ball region to get the coverage for
     * @param regionRadius   the radius of the ball region to get the coverage for
     * @param distThreshold  the threshold distance value on the non-overlaping area
     * @return returns <tt>true</tt> if the specified ball region is within this one
     */
    public boolean isCoveringRegion(LocalAbstractObject regionPivot, float regionRadius, float distThreshold) {
        // Resize the threshold, so that at least a small part of the region must overlap
        if (distThreshold > 2*regionRadius)
            distThreshold = 2*regionRadius;
        // Compare the distances
        return 2*regionRadius - getOverlapWith(regionPivot, regionRadius) <= distThreshold;
    }

    /**
     * Returns <tt>true</tt> if this ball region is covered by at least <code>distThreshold</code> area of the ball
     * region specified by <code>regionPivot</code> and <code>regionRadius</code>.
     * The <code>distThreshold</code> is the maximal distance that an object from this region can be outside the specified region.
     * If the threshold is zero, the this region must be fully within the specified region.
     * If the two regions do not even touch, this method returns <tt>false</tt> whatever the value of the threshold is.
     *
     * @param regionPivot    the pivot (center) of the ball region to get the coverage for
     * @param regionRadius   the radius of the ball region to get the coverage for
     * @param distThreshold  the threshold distance value on the non-overlaping area
     * @return returns <tt>true</tt> if the this ball region is within the specified one
     */
    public boolean isCoveredByRegion(LocalAbstractObject regionPivot, float regionRadius, float distThreshold) {
        // Resize the threshold, so that at least a small part of the region must overlap
        if (distThreshold > 2*radius)
            distThreshold = 2*radius;
        // Compare the distances
        return 2*radius - getOverlapWith(regionPivot, regionRadius) <= distThreshold;
    }

    /****************** Overrides for local abstract object ******************/

    /**
     * Returns the size of this ball region in bytes.
     * Specifically, it is the size of the pivot plus the float number for radius.
     * @return the size of this ball region in bytes
     */
    @Override
    public int getSize() {
        return Float.SIZE/8 + ((pivot != null)?pivot.getSize():0);
    }

    /** 
     * Indicates whether some other object has the same data as this one.
     * @param   obj   the reference object with which to compare.
     * @return  <code>true</code> if this object is the same as the obj
     *          argument; <code>false</code> otherwise.
     */
    @Override
    public boolean dataEquals(Object obj) {
        // Only compare with other ball regions
        if (!(obj instanceof BallRegion))
            return false;
        BallRegion castObj = (BallRegion)obj;
        // If either this or obj's pivot is null, return true also the opposite pivot is null
        if (pivot == null || castObj.pivot == null)
            return pivot == castObj.pivot;
        // Test pivots for data equality
        if (!pivot.dataEquals(castObj.pivot))
            return false;
        // And finally, test radii
        return radius == castObj.radius;
    }

    /**
     * Returns a hash code value for this ball region's data, i.e. the hash code of the pivot.
     * @return a hash code value for this ball region's data
     */
    @Override
    public int dataHashCode() {
        return (pivot != null)?pivot.dataHashCode():0;
    }

    /** 
     * Random copy of ball region is not implemented, thus this method
     * always throws an exception.
     *
     * @param args the value is ignored
     * @return nothing because an exception is thrown
     * @throws UnsupportedOperationException always
     */
    @Override
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Can't randomly modify ball region");
    }

    
}
