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
package messif.statistics;


/** Statistics for counting time. The time is incremented by the amount of
 *  time elapsed between calls to methods start() & stop().
 *
 *  Note that additional calls to start will have no affect until stop is called.
 *  Update & set methods will leave current statistics stopped/started state untouched,
 *  but they will add values including elapsed time if started.
 *
 *  An example:
 *  get() => 0 milis
 *  start()
 *  1000 milis elapsed
 *  get() => 1000 milis
 *  100 milis elapsed
 *  get() => 1100 milis
 *  stop()
 *  1000 milis elapsed
 *  get() => 1100 milis
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public final class StatisticTimer extends Statistics<StatisticTimer> {
    
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    //****************** Counter operation ******************//

    /** Time elapsed between calls to {@link #start()} and {@link #stop()}. */
    protected long time = 0;
    /** Time of the last call to {@link #start()} of started statistics */
    protected long lastStartTime = 0;
    /** Backup time for checkpoint feature */
    private long timeCheckpoint = 0;
    
    /** Starts incrementing the timer */
    public void start() {
        if (!canPerformOperation())
            return;
        synchronized (this) {
            if (lastStartTime == 0)
                lastStartTime = System.currentTimeMillis();
            for (StatisticTimer stat : getBoundStats())
                stat.start();
        }
    }
    
    /** Stops incrementing timer */
    public void stop() {
        if (!canPerformOperation())
            return;
        synchronized (this) {
            if (lastStartTime > 0) {
                this.time += System.currentTimeMillis() - lastStartTime;
                lastStartTime = 0;
            }
            for (StatisticTimer stat : getBoundStats())
                stat.stop();
        }
    }

    /**
     * Time elapsed in msec.
     * If the statistics has been stopped, the elapsed time is returned, otherwise current time minus start time is returned.
     * @return time elapsed in msec.
     */
    public synchronized long get() {
        if (lastStartTime > 0)
            return time + System.currentTimeMillis() - lastStartTime;
        else return time;
    }

    @Override
    public Object getValue() {
        return get();
    }

    @Override
    protected void addBoundStat(StatisticTimer stat) {
        super.addBoundStat(stat);
        
        // If this statistic is started now, start the bound statistic as well (no need to synchronize)
        if (lastStartTime > 0)
            stat.start();
    }

    @Override
    protected void removeBoundStat(StatisticTimer stat) {
        super.removeBoundStat(stat);
        
        // If this statistic is started now, stop the bound statistic on removal (no need to synchronize)
        if (lastStartTime > 0)
            stat.stop();
    }


    //****************** Statistics merging ******************//
    
    @Override
    protected synchronized void updateFrom(StatisticTimer sourceStat) {
        time += sourceStat.get();
    }
    
    @Override
    protected synchronized void setFrom(StatisticTimer sourceStat) {
        time = sourceStat.get();
    }
    
    @Override
    public void reset() {
        time = 0;
        lastStartTime = 0;
        setCheckpoint();
    }
    
    //****************** Constructors ******************//

    /** Creates a new instance of StatisticTimer
     * @param name requested name of the statitics
     */
    protected StatisticTimer(String name) {
        super(name);
    }

    
    //****************** Creator ******************//
    
    /** Factory method for creating a new statistic timer with the specified name or get the one already existing.
     * @param name requested name of the statistics
     * @return instance of {@link StatisticTimer} having the passed name.
     * @throws ClassCastException if the statistics of the given name exists, but is of a different class than {@link StatisticTimer}
     */
    public static StatisticTimer getStatistics(String name) throws ClassCastException {
        return getStatistics(name, StatisticTimer.class);
    }

    
    //****************** Text representation ******************//
    
    @Override
    public String toString() {
        return getName() + ": " + get();
    }

    /**
     * Test whether this statistics has been changed since the last checkpoint.
     * @return <code>true</code> if it has been changed, otherwise <code>false</code>.
     */
    public boolean changedSinceCheckpoint() {
        return (time != timeCheckpoint || lastStartTime > 0);
    }
    
    /** Sets checkpoint. Stores the current state of timer.
     */
    public void setCheckpoint() {
        timeCheckpoint = time;
    }

    @Override
    protected StatisticTimer cast() {
        return this;
    }
    
}
