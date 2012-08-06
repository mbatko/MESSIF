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

import messif.buckets.BucketDispatcher;
import messif.buckets.BucketStorageException;
import messif.operations.QueryOperation;

/**
 * Message requesting to process a query on a remote bucket.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 * @see NetworkBucketDispatcher
 */
public class BucketProcessQueryRequestMessage extends BucketRequestMessage<BucketProcessQueryReplyMessage> {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Query operation to process on a remote bucket */
    private final QueryOperation<?> query;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of BucketProcessQueryRequestMessage.
     * @param bucketID the ID of a remote bucket on which to process the request
     * @param query the query operation to process on a remote bucket
     */
    public BucketProcessQueryRequestMessage(int bucketID, QueryOperation<?> query) {
        super(bucketID);
        this.query = query;
    }


    //****************** Executing the request ******************//

    @Override
    public BucketProcessQueryReplyMessage execute(BucketDispatcher bucketDispatcher) throws RuntimeException, BucketStorageException {
        int count = bucketDispatcher.getBucket(bucketID).processQuery(query);
        return new BucketProcessQueryReplyMessage(this, query, count);
    }

    @Override
    public Class<BucketProcessQueryReplyMessage> replyMessageClass() {
        return BucketProcessQueryReplyMessage.class;
    }

}
