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
 * Receiver allows to accept messages received by the {@link MessageDispatcher}.
 * Once a receiver is registered through {@link MessageDispatcher#registerReceiver registerReceiver}
 * method, every message that arrives at the dispatcher is passed to the registered receivers through
 * {@link #acceptMessage acceptMessage} in sequence until a receiver accepts the message.
 *
 * @see MessageDispatcher
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface Receiver {

    /**
     * Offers a message to this receiver for acceptance.
     * This method is called by {@link MessageDispatcher} when new message arrives.
     * <tt>True</tt> is returned, if this receiver accepts the offered message
     * (and the processing of the message is stopped at message dispatcher's level).
     *
     * @param msg the message offered for acceptance
     * @param allowSuperclass First, the message is offered with <tt>allowSuperclass</tt> set to <tt>false</tt>.
     *                        If no receiver accepts it, another offering round is issued with <tt>allowSuperclass</tt>
     *                        set to <tt>true</tt> (so the receiver can relax its acceptance conditions).
     * @return <tt>true</tt> if the message is accepted by this receiver; otherwise, the processing
     *         continues by offering the message to the next receiver
     */
    public boolean acceptMessage(Message msg, boolean allowSuperclass);
        
}
