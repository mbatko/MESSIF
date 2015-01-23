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
package messif.buckets;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread that periodically checks for {@link TemporaryCloseable} objects and
 * close them to save resources.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class TemporaryCloseableThread extends Thread {
    /** Logger to use for logging exceptions */
    private final static Logger log = Logger.getLogger(TemporaryCloseableThread.class.getName());

    /** List of closeable objects to check */
    private final Collection<TemporaryCloseable> temporaryCloseables;
    /** Checking period in milliseconds */
    private long period;

    /**
     * Creates a new thread that checks {@link TemporaryCloseable} objects.
     * Do not forget to call
     * @param period the checking period in milliseconds
     */
    public TemporaryCloseableThread(long period) {
        super(TemporaryCloseableThread.class.getSimpleName());
        this.temporaryCloseables = new CopyOnWriteArraySet<>();
        setPeriod(period);
    }

    /**
     * Add a {@link TemporaryCloseable} object to the waiting list.
     * @param temporaryCloseable the object to add to the waiting list
     * @return <tt>true</tt> if the object was added to the list
     */
    public boolean add(TemporaryCloseable temporaryCloseable) {
        if (temporaryCloseable == null)
            return false;
        return temporaryCloseables.add(temporaryCloseable);
    }

    /**
     * Removes the given {@link TemporaryCloseable} object from the waiting list.
     * @param temporaryCloseable the object to remove from the waiting list
     * @return <tt>true</tt> if the object was removed from the list
     */
    public boolean remove(TemporaryCloseable temporaryCloseable) {
        if (temporaryCloseable == null)
            return false;
        return temporaryCloseables.remove(temporaryCloseable);
    }

    /**
     * Change the checking period.
     * Note that the change will be used in next checking iteration.
     * @param period the new checking period in milliseconds
     */
    public final void setPeriod(long period) {
        if (period <= 0)
            throw new IllegalArgumentException("Period must be positive number: " + period);
        this.period = period;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                sleep(period);
            } catch (InterruptedException ignore) {
                break;
            }
            for (TemporaryCloseable temporaryCloseable : temporaryCloseables) {
                try {
                    temporaryCloseable.closeTemporarilyIfIdle(true);
                } catch (IOException e) {
                    log.log(Level.WARNING, "Cannot temporarily close instance: " + e, e);
                }
            }
        }
    }
}
