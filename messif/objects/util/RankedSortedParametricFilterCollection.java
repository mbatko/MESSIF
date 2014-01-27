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
package messif.objects.util;

import messif.utility.Parametric;

/**
 * Specialization of {@link RankedSortedCollection} that filters objects that have
 * not set a given {@link Parametric} parameter to a given value.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class RankedSortedParametricFilterCollection extends RankedSortedCollection {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /** The name of the parameter to filter */
    private final String parameterName;
    /** The filtering values of the parameter, i.e. the parameter value must be one of the given values */
    private final Object[] allowedValues;
    /** Flag whether to allow objects that either does not implement the {@link Parametric} or do not have the given parameter set */
    private final boolean allowEmptyParameter;

    /**
     * Constructs an empty collection with the specified initial and maximal capacity.
     * @param parameterName the name of the parameter to filter
     * @param allowedValues the filtering values of the parameter, i.e. the parameter value must be one of the given values
     * @param allowEmptyParameter a flag whether to allow objects that either does not implement the {@link Parametric} or do not have the given parameter set
     * @param initialCapacity the initial capacity of the collection
     * @param maximalCapacity the maximal capacity of the collection
     * @throws IllegalArgumentException if the specified initial or maximal capacity is invalid
     * @throws NullPointerException if the given parameter name or the allowed values are <tt>null</tt>
     */
    public RankedSortedParametricFilterCollection(String parameterName, Object[] allowedValues, boolean allowEmptyParameter, int initialCapacity, int maximalCapacity) throws IllegalArgumentException, NullPointerException {
        super(initialCapacity, maximalCapacity);
        if (parameterName == null)
            throw new NullPointerException();
        this.parameterName = parameterName;
        this.allowedValues = allowedValues.clone();
        this.allowEmptyParameter = allowEmptyParameter;
    }

    /**
     * Constructs an empty collection with default initial capacity and unlimited maximal capacity.
     * @param parameterName the name of the parameter to filter
     * @param allowedValues the filtering values of the parameter, i.e. the parameter value must be one of the given values
     * @param allowEmptyParameter a flag whether to allow objects that either does not implement the {@link Parametric} or do not have the given parameter set
     * @throws NullPointerException if the given parameter name or the allowed values are <tt>null</tt>
     */
    public RankedSortedParametricFilterCollection(String parameterName, Object[] allowedValues, boolean allowEmptyParameter) throws NullPointerException {
        super();
        if (parameterName == null)
            throw new NullPointerException();
        this.parameterName = parameterName;
        this.allowedValues = allowedValues.clone();
        this.allowEmptyParameter = allowEmptyParameter;
    }

    /**
     * Returns the respective parameter from the given object.
     * @param object the object from which to get the parameter value
     * @return the parameter value or <tt>null</tt>, if the object does not have the given parameter set
     */
    private Object getParameter(Object object) {
        if (!(object instanceof Parametric))
            return null;
        return ((Parametric)object).getParameter(parameterName);
    }

    @Override
    public boolean add(RankedAbstractObject e) {
        Object parameter = getParameter(e.getObject());
        if (parameter == null) { // Parameter is not set, behave according to "allow empty" flag
            if (allowEmptyParameter)
                return super.add(e);
            else
                return false;
        } else {
            for (Object allowedValue : allowedValues) {
                if (parameter.equals(allowedValue))
                    return super.add(e);
            }
            return false;
        }
    }
}
