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

import java.util.Collection;
import messif.buckets.BucketErrorCode;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.util.AbstractObjectList;
import messif.operations.QueryOperation;

/**
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class BucketManipulationReplyMessage extends BucketReplyMessage {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    protected final BucketErrorCode errorCode;
    protected final LocalAbstractObject object;
    protected final AbstractObjectList<LocalAbstractObject> objects;
    protected final QueryOperation<?> query;
    
    //****************** Attribute access methods ******************//

    public LocalAbstractObject getObject() {
        return object;
    }
    
    public AbstractObjectList<LocalAbstractObject> getObjects() {
        return objects;
    }
    
    public BucketErrorCode getErrorCode() {
        return errorCode;
    }

    public QueryOperation<?> getQuery() {
        return query;
    }

    public int getChangesCount() {
        return 0;
    }


    //****************** Constructors ******************//

    /**
     * Creates a new instance of BucketManipulationReplyMessage for adding object
     */
    public BucketManipulationReplyMessage(BucketManipulationRequestMessage message, BucketErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.object = null;
        this.objects = null;
        this.query = null;
    }

    /**
     * Creates a new instance of BucketManipulationReplyMessage for getting object
     */
    public BucketManipulationReplyMessage(BucketManipulationRequestMessage message, LocalAbstractObject object) {
        this(message, object, false);
    }

    /**
     * Creates a new instance of BucketManipulationReplyMessage for getting object
     */
    public BucketManipulationReplyMessage(BucketManipulationRequestMessage message, LocalAbstractObject object, boolean deleteObject) {
        super(message);
        this.errorCode = deleteObject?BucketErrorCode.OBJECT_DELETED:null;
        this.object = object;
        this.objects = null;
        this.query = null;
    }
     
    /**
     * Creates a new instance of BucketManipulationReplyMessage for getting
     */
    public BucketManipulationReplyMessage(BucketManipulationRequestMessage message, AbstractObjectIterator<? extends LocalAbstractObject> objects) {
        super(message);
        errorCode = null;
        object = null;
        this.objects = new AbstractObjectList<LocalAbstractObject>(objects);
        this.query = null;
    }

    /**
     * Creates a new instance of BucketManipulationReplyMessage for getting
     */
    public BucketManipulationReplyMessage(BucketManipulationRequestMessage message, Collection<? extends LocalAbstractObject> objects, QueryOperation<?> query) {
        super(message);
        errorCode = null;
        object = null;
        this.objects = new AbstractObjectList<LocalAbstractObject>(objects);
        this.query = query;
    }
}
