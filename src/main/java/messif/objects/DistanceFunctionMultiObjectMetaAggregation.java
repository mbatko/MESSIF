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

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import messif.objects.util.AggregationFunction;

/**
 * Implementation of the the {@link DistanceFunctionMultiObject multi-object distance function}
 * for {@link MetaObject}s without their own metric function.
 * It computes a distances between the respective query objects and a given object
 * using {@link AggregationFunction}. The resulting individual query-object distances
 * are then aggregated using {@link DistanceFunctionMultiObjectAggregation}.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class DistanceFunctionMultiObjectMetaAggregation implements DistanceFunctionMultiObject<MetaObject>, Serializable {    
    /** class id for serialization */
    private static final long serialVersionUID = 874587001L;    
    
    /** Distance aggregation for the distances to the respective query objects */
    private final DistanceFunctionMultiObjectAggregation multiObjectAggregation;
    /** Distance function to compute single distance between one query object and the given object */
    private final AggregationFunction metaObjectAggregationDistance;

    /**
     * Creates a new DistanceFunctionMultiObjectMetaAggregation with the given aggregation functions.
     * @param multiObjectAggregation the distance aggregation for the distances to the respective query objects
     * @param metaObjectAggregationDistance the distance function to compute single distance between one query object and the given object
     * @throws NullPointerException if any of the two given aggregations is <tt>null</tt> 
     */
    public DistanceFunctionMultiObjectMetaAggregation(DistanceFunctionMultiObjectAggregation multiObjectAggregation, AggregationFunction metaObjectAggregationDistance) throws NullPointerException {
        if (multiObjectAggregation == null || metaObjectAggregationDistance == null)
            throw new NullPointerException();
        this.multiObjectAggregation = multiObjectAggregation;
        this.metaObjectAggregationDistance = metaObjectAggregationDistance;
    }

    @Override
    public float getDistanceMultiObject(Collection<? extends MetaObject> objects, MetaObject object, float[] individualDistances) throws IndexOutOfBoundsException {
        if (individualDistances == null)
            individualDistances = new float[objects.size()];
        Iterator<? extends MetaObject> objIterator = objects.iterator();
        for (int i = 0; objIterator.hasNext(); i++)
            individualDistances[i] = metaObjectAggregationDistance.getDistance(objIterator.next(), object);
        return multiObjectAggregation.evaluate(individualDistances);
    }

    @Override
    public Class<? extends MetaObject> getDistanceObjectClass() {
        return MetaObject.class;
    }

}
