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
package messif.objects.util.impl;

import java.util.List;

/**
 * The subdistance identifier for the aggregation function evalutator.
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
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
    @Override
    public final float evaluate(float[] subdistances) {
        return subdistances[index];
    }

    @Override
    public String toString() {
        return name;
    }

}
