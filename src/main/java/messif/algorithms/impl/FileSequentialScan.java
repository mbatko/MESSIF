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
package messif.algorithms.impl;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import messif.algorithms.Algorithm;
import messif.objects.LocalAbstractObject;
import messif.objects.util.StreamGenericAbstractObjectIterator;
import messif.operations.query.GetAlgorithmInfoOperation;
import messif.operations.RankingSingleQueryOperation;
import messif.utility.Convert;

/**
 * Implementation of the naive sequential scan algorithm over a given file of objects.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class FileSequentialScan extends Algorithm {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    /** One instance of bucket where all objects are stored */
    protected final String file;
    protected final String clazz;

    /**
     * Creates a new instance of SequantialScan access structure with the given bucket and filtering pivots.
     *
     * @param file file with text representation of objects
     */
    @Algorithm.AlgorithmConstructor(description = "FileSequentialScan", arguments = {"data file name", "data class"})
    public FileSequentialScan(String file, String clazz) {
        super("SequentialScan");
        
        // Create an empty bucket (using the provided bucket class and parameters)
        this.file = file;
        this.clazz = clazz;
    }


    //******* ALGORITHM INFO OPERATION *************************************//

    /**
     * Method for processing {@link GetAlgorithmInfoOperation}.
     * The processing will fill the algorithm info with this
     * algorithm {@link #toString() toString()} value.
     * @param operation the operation to process
     */
    public void algorithmInfo(GetAlgorithmInfoOperation operation) {
        operation.addToAnswer(toString());
        operation.endOperation();
    }
        

    //******* SEARCH ALGORITHMS ************************************//

    /**
     * Evaluates a ranking single query object operation on this algorithm.
     * Note that the operation is evaluated sequentially on all objects of this algorithm.
     * @param operation the operation to evaluate
     */
    public void singleQueryObjectSearch(RankingSingleQueryOperation operation) throws IOException {
        
        try (StreamGenericAbstractObjectIterator<LocalAbstractObject> it = new StreamGenericAbstractObjectIterator<>(
                Convert.getClassForName(clazz, LocalAbstractObject.class), file)) {
            operation.evaluate(it);
            operation.endOperation();
        } catch (IllegalArgumentException | IOException | ClassNotFoundException ex) {
            Logger.getLogger(FileSequentialScan.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Converts the object to a string representation
     * @return String representation of this algorithm
     */
    @Override
    public String toString() {
        StringBuffer rtv = new StringBuffer("FileSequentialScan");
        return rtv.toString();
    }
}
