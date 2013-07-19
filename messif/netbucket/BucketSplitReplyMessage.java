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
import messif.buckets.Bucket;
import messif.buckets.BucketDispatcher;
import messif.buckets.CapacityFullException;
import messif.buckets.LocalBucket;
import messif.buckets.split.SplitPolicy;
import messif.buckets.split.SplitResult;

/**
 * Message for returning results of a remote bucket split.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class BucketSplitReplyMessage extends BucketReplyMessage implements SplitResult {
    
    /** Class serial id for serialization */
    private static final long serialVersionUID = 2325101L;
    
    private static final int NO_BUCKET_ID = Integer.MIN_VALUE;

    //****************** Attributes ******************//

    /** Split policy for given bucket (can contain output data) */
    private final SplitPolicy splitPolicy;    
    /** IDs of the buckets that were created on the remote network node during the split */
    private final int [] bucketIDs;
    /** Capacity of the created bucket */
    private final long [] capacities;
    /* The number of objects moved from the bucket to the newly created ones. */
    private final int objectsMoved;
            

    //****************** Constructor ******************//
    
    /**
     * Creates a new instance of BucketCreateReplyMessage for the supplied data.
     *
     * @param message the original message this message is response to
     * @param bucketID the ID of the bucket that was created on the remote network node
     * @param capacity the capacity of the created bucket
     */
    public BucketSplitReplyMessage(BucketSplitRequestMessage message, List<Bucket> newBuckets, int objectsMoved) {
        super(message);
        splitPolicy = message.getSplitPolicy();
        bucketIDs = new int [newBuckets.size()];
        capacities = new long [newBuckets.size()];
        this.objectsMoved = objectsMoved;
        
        for (int i = 0; i < newBuckets.size(); i++) {
            LocalBucket newBucket = (LocalBucket) newBuckets.get(i);
            bucketIDs[i] = (newBucket == null) ? NO_BUCKET_ID : newBucket.getBucketID();
            capacities[i] = (newBucket == null) ? 0L : newBucket.getCapacity();
        }
    }


    //****************** Attribute access methods ******************//

    /**
     * Returns the list remote buckets for the newly created buckets (the list can contain nulls). The list 
     *  is of length {@link SplitPolicy#getPartitionsCount() }.
     * @param netbucketDisp the network bucket dispatcher that will handle the remote bucket's processing
     * @return the list remote buckets for the newly created buckets
     * @throws CapacityFullException if no bucket was created on the remote network node
     */
    @Override
    public List<? extends Bucket> getBuckets(BucketDispatcher netbucketDisp) throws CapacityFullException {
        if (bucketIDs == null)
            throw new CapacityFullException();
        if (! (netbucketDisp instanceof NetworkBucketDispatcher)) {
            return null;
        }
        List<RemoteBucket> retVal = new ArrayList<RemoteBucket>(bucketIDs.length);
        for (int i = 0; i < bucketIDs.length; i++) {
            retVal.add((bucketIDs[i] == NO_BUCKET_ID) ? null :
                    new RemoteBucket((NetworkBucketDispatcher) netbucketDisp, bucketIDs[i], getSender(), capacities[i]));
        }
        return retVal;
    }

    /**
     * Returns the used split policy that can contain output values.
     * @return the used split policy 
     */
    @Override
    public SplitPolicy getSplitPolicy() {
        return splitPolicy;
    }

    /**
     * Returns the number of objects moved from the split bucket to the newly created ones.
     * @return the number of objects moved
     */
    @Override
    public int getObjectsMoved() {
        return objectsMoved;
    }
    
}
