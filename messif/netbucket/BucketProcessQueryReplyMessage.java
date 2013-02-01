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

import messif.operations.QueryOperation;

/**
 * Message for returning results of a query processed on a remote bucket.
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class BucketProcessQueryReplyMessage extends BucketReplyMessage {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Query operation processed on a remote bucket */
    private final QueryOperation<?> query;
    /** Number of objects that were added to answer */
    private final int count;


    //****************** Constructor ******************//
    
    /**
     * Creates a new instance of BucketProcessQueryReplyMessage for the supplied data.
     *
     * @param message the original message this message is response to
     * @param query the query operation processed on a remote bucket
     * @param count the number of objects that were added to answer
     */
    public BucketProcessQueryReplyMessage(BucketProcessQueryRequestMessage message, QueryOperation<?> query, int count) {
        super(message);
        this.query = query;
        this.count = count;
    }


    //****************** Attribute access methods ******************//

    /**
     * Returns the query operation processed on a remote bucket.
     * @return the query operation processed on a remote bucket
     */
    public QueryOperation<?> getQuery() {
        return query;
    }

    /**
     * Returns the number of objects that were added to answer.
     * @return the number of objects that were added to answer
     */
    public int getCount() {
        return count;
    }
}
