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
package messif.algorithms;

import messif.network.Message;
import messif.operations.AbstractOperation;

/**
 * Request message for the {@link DistributedAlgorithm}.
 * The message contains the operation to process by the distributed algorithm.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class DistAlgRequestMessage extends Message {

    /** Class version id for serialization */
    private static final long serialVersionUID = 1L;
    
    //****************** Attributes ******************//

    /** Operation processed by the algorithm */
    private AbstractOperation operation;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of DistAlgRequestMessage for the given operation.
     * @param operation the operation processed by the algorithm
     */
    public DistAlgRequestMessage(AbstractOperation operation) {
        super();
        this.operation = operation;
    }

    /**
     * Returns the operation processed by the algorithm.
     * @return the operation processed by the algorithm
     */
    public AbstractOperation getOperation() {
        return operation;
    }


    //****************** Cloning ******************//

    @Override
    public DistAlgRequestMessage clone() throws CloneNotSupportedException {
        DistAlgRequestMessage rtv = (DistAlgRequestMessage)super.clone();
        rtv.operation = operation.clone();
        return rtv;
    }


    //****************** String representation ******************//

    @Override
    public String toString() {
        return super.toString() + ": " + operation;
    }
}
