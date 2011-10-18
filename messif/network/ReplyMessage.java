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
package messif.network;


/**
 * The ancestor of all reply messages that are returned back during communication with other network nodes.
 * The reply message is always a reply for some other message and it inherits the message ID and all
 * the navigation path from the original message. Specifically, the reply message is a continuation
 * of an existing message that can be sent back to a waiting node. Only ReplyMessage can be returned from
 * {@link ReplyReceiver}, thus whenever we want to inform the original node about our findings, we must transform
 * the "forward" message with the ReplyMessage.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class ReplyMessage extends Message {

    /** Class version id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Constructors ******************//

    /**
     * Creates a new instance of ReplyMessage.
     * @param message the original message this message is response to
     */
    protected ReplyMessage(Message message) {
        super(message);
    }


    //****************** Cloning ******************//

    /**
     * Always throws CloneNotSupportedException exception, because conning is not supported for replies.
     * @return nothing, because this method always throws CloneNotSupportedException
     * @throws CloneNotSupportedException if this instance cannot be cloned
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Reply messages cannot be cloned");
    }


    //****************** String representation ******************//

    /**
     * Returns a string representation of this response message.
     * @return a string representation of this response message
     */
    @Override
    public String toString() {
        return "ReplyMessage (ID:" + messageID + ") <<" + getClass().getName() + ">>";
    }

}
