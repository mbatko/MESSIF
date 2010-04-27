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
package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import messif.objects.LocalAbstractObject;


/**
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ObjectStringDNASeqDist extends ObjectStringEditDist {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Constructors ******************//

    /** Creates a new instance of Object */
    public ObjectStringDNASeqDist(String text) {
        super(text);
    }

    /** Creates a new instance of Object random generated */
    public ObjectStringDNASeqDist() {
        super();
    }

    /** Creates a new instance of Object random generated 
     * with minimal length equal to minLength and maximal 
     * length equal to maxLength */
    public ObjectStringDNASeqDist(int minLength, int maxLength) {
        super(minLength, maxLength);
    }

    /** Creates a new instance of Object from stream */
    public ObjectStringDNASeqDist(BufferedReader stream) throws IOException {
        super(stream);
    }


    //****************** Metric function ******************//

    private static int[][] changeMatrix = {
      // A  R  N  D  C  Q  E  G  H  I  L  K  M  F  P  S  T  W  Y  V  X
        {0, 5, 2, 3, 5, 3, 3, 2, 4, 3, 5, 4, 4, 6, 2, 1, 2, 9, 6, 3, 3 },
        {5, 0, 3, 5, 8, 3, 5, 6, 3, 5, 6, 2, 5, 7, 5, 3, 5, 5, 7, 5, 3 },
        {2, 3, 0, 1, 7, 2, 2, 3, 1, 4, 5, 2, 5, 6, 3, 1, 2, 7, 5, 4, 3 },
        {3, 5, 1, 0, 8, 2, 1, 3, 3, 5, 7, 4, 6, 8, 4, 3, 3,10, 7, 5, 3 },
        {5, 8, 7, 8, 0, 8, 8, 7, 7, 6, 9, 9, 9, 8, 7, 4, 5,10, 6, 6, 6 },
        {3, 3, 2, 2, 8, 0, 2, 5, 2, 5, 5, 3, 4, 8, 4, 4, 4, 8, 7, 5, 3 },
        {3, 5, 2, 1, 8, 2, 0, 4, 3, 5, 6, 4, 5, 8, 4, 3, 3,10, 7, 5, 3 },
        {2, 6, 3, 3, 7, 5, 4, 0, 5, 6, 7, 5, 6, 8, 4, 2, 3, 9, 8, 5, 3 },
        {4, 3, 1, 3, 7, 2, 3, 5, 0, 5, 6, 4, 6, 6, 5, 4, 4, 7, 5, 6, 3 },
        {3, 5, 4, 5, 6, 5, 5, 6, 5, 0, 3, 5, 3, 4, 5, 4, 3, 9, 6, 1, 3 },
        {5, 6, 5, 7, 9, 5, 6, 7, 6, 3, 0, 6, 2, 4, 6, 5, 5, 7, 5, 3, 3 },
        {4, 2, 2, 4, 9, 3, 4, 5, 4, 5, 6, 0, 4, 8, 5, 3, 3, 7, 7, 5, 3 },
        {4, 5, 5, 6, 9, 4, 5, 6, 6, 3, 2, 4, 0, 5, 6, 5, 4, 8, 6, 2, 3 },
        {6, 7, 6, 8, 8, 8, 8, 8, 6, 4, 4, 8, 5, 0, 8, 6, 6, 6, 2, 5, 6 },
        {2, 5, 3, 4, 7, 4, 4, 4, 5, 5, 6, 5, 6, 8, 0, 2, 4, 9, 8, 4, 3 },
        {1, 3, 1, 3, 4, 4, 3, 2, 4, 4, 5, 3, 5, 6, 2, 0, 1, 6, 6, 3, 3 },
        {2, 5, 2, 3, 5, 4, 3, 3, 4, 3, 5, 3, 4, 6, 4, 1, 0, 8, 6, 4, 3 },
        {9, 5, 7,10,10, 8,10, 9, 7, 9, 7, 7, 8, 6, 9, 6, 8, 0, 6, 9, 6 },
        {6, 7, 5, 7, 6, 7, 7, 8, 5, 6, 5, 7, 6, 2, 8, 6, 6, 6, 0, 5, 4 },
        {3, 5, 4, 5, 6, 5, 5, 5, 6, 1, 3, 5, 2, 5, 4, 3, 4, 9, 5, 0, 3 },
        {3, 3, 3, 3, 6, 3, 3, 3, 3, 3, 3, 3, 3, 6, 3, 3, 3, 6, 4, 3, 0 }
    };

    private int getCharMatrixIndex(char chr) {
        switch (chr) {
            case 'A': return 0;
            case 'R': return 1;
            case 'N': return 2;
            case 'D': return 3;
            case 'C': return 4;
            case 'Q': return 5;
            case 'E': return 6;
            case 'G': return 7;
            case 'H': return 8;
            case 'I': return 9;
            case 'L': return 10;
            case 'K': return 11;
            case 'M': return 12;
            case 'F': return 13;
            case 'P': return 14;
            case 'S': return 15;
            case 'T': return 16;
            case 'W': return 17;
            case 'Y': return 18;
            case 'V': return 19;
            case 'X': return 20;
        }

        return -1;
    }

    @Override
    protected int getInsertDeleteWeight() {
        return 5;
    }

    @Override
    protected int getChangeWeight(char chr1, char chr2) {
        try {
            return changeMatrix[getCharMatrixIndex(chr1)][getCharMatrixIndex(chr2)];
        } catch (IndexOutOfBoundsException e) {
            return getInsertDeleteWeight();
        }
    }

    @Override
    protected float getDistanceUpperBoundImpl(LocalAbstractObject obj, int accuracy) {
        return Math.abs(this.text.length() + ((ObjectStringDNASeqDist)obj).text.length())*10;
    }    

}
