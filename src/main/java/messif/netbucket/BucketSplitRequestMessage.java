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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import messif.buckets.Bucket;
import messif.buckets.BucketDispatcher;
import messif.buckets.BucketStorageException;
import messif.buckets.LocalBucket;
import messif.buckets.split.SplitPolicy;

/**
 * Message requesting to split a bucket creating new ones.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 * @see NetworkBucketDispatcher
 */
public class BucketSplitRequestMessage extends BucketRequestMessage<BucketSplitReplyMessage> {
    
    /** Class serial id for serialization */
    private static final long serialVersionUID = 4651101L;

    /** Logger */
    private static final Logger log = Logger.getLogger("netnode.creator");
    
    //****************** Attributes ******************//

    /** Split policy for given bucket */
    private final SplitPolicy splitPolicy;

    /** 
     * Identification of a partition whose objects stay in this bucket; if lower than zero, no objects are to 
     *  stay in this bucket and, due to efficiency, the objects are not removed from this bucket.
     * {@link Bucket#split}
     */
    private final int whoStays;

    
    // ************************   Getters    *********************** //
    
    public SplitPolicy getSplitPolicy() {
        return splitPolicy;
    }        
    
    //****************** Constructors ******************//

    /**
     * Creates a new instance of BucketSplitRequestMessage.
     * @param bucketID the ID of a remote bucket to be split
     * @param splitPolicy split policy for given bucket
     * @param whoStays Identification of a partition whose objects stay in the split bucket
     */
    
    public BucketSplitRequestMessage(int bucketID, SplitPolicy splitPolicy, int whoStays) {
        super(bucketID);
        this.splitPolicy = splitPolicy;
        this.whoStays = whoStays;
    }


    //****************** Executing the request ******************//

    @Override
    public BucketSplitReplyMessage execute(BucketDispatcher bucketDispatcher) throws RuntimeException, BucketStorageException {
        List<Bucket> newBuckets = new ArrayList<Bucket>(splitPolicy.getPartitionsCount());
        LocalBucket bucketToSplit = bucketDispatcher.getBucket(bucketID);        
        log.log(Level.INFO, "Requesting split of bucket ID {0} (request from {1})", new Object[]{bucketID, getSender()});
        
        int objectsMoved = bucketToSplit.split(splitPolicy, newBuckets, bucketDispatcher, whoStays);
        
        return new BucketSplitReplyMessage(this, newBuckets, objectsMoved);
    }

    @Override
    public Class<BucketSplitReplyMessage> replyMessageClass() {
        return BucketSplitReplyMessage.class;
    }

}
