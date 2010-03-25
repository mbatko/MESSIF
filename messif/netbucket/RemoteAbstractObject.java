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
package messif.netbucket;

import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;

/**
 * Object of this class represents an object stored in a {@link RemoteBucket}.
 * It does not contain any data.
 *
 * @see messif.objects.AbstractObject
 * @see messif.objects.LocalAbstractObject
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class RemoteAbstractObject extends AbstractObject {
    /** Class version id for serialization. */
    private static final long serialVersionUID = 3L;

    //****************** Attributes ******************//

    /** Bucket in which this object resides */
    private final RemoteBucket bucket;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of RemoteAbstractObject from the specified LocalAbstractObject.
     * @param object the local object from which to create the new one
     * @param bucket the bucket in which the object is stored
     */
    public RemoteAbstractObject(LocalAbstractObject object, RemoteBucket bucket) {
        super(object); // Copy object ID and key
        this.bucket = bucket;
    }


    //****************** Local object converter ******************//

    /**
     * Returns the actual object.
     * The object is downloaded from the stored {@link RemoteBucket}.
     *
     * @return the local object this remote object represents
     * @throws IllegalStateException if there was an error communicating with the remote bucket dispatcher
     */
    public LocalAbstractObject getLocalAbstractObject() throws IllegalStateException {
        return bucket.getObject(this);
    }
}
