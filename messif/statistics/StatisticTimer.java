/*
 * StatisticTimer.java
 *
 * Created on 25. duben 2004, 13:55
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
 * @author  xbatko
 */
public final class StatisticTimer extends Statistics<StatisticTimer> {
    
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    //****************** Counter operation ******************//
    protected long time = 0;
    protected long lastStartTime = 0;
    private long timeCheckpoint = 0;         /* backup time for checkpoint feature */
    
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

    public synchronized long get() {
        if (lastStartTime > 0)
            return time + System.currentTimeMillis() - lastStartTime;
        else return time;
    }

    protected void addBoundStat(StatisticTimer stat) {
        super.addBoundStat(stat);
        
        // If this statistic is started now, start the bound statistic as well (no need to synchronize)
        if (lastStartTime > 0)
            stat.start();
    }

    protected void removeBoundStat(StatisticTimer stat) {
        super.removeBoundStat(stat);
        
        // If this statistic is started now, stop the bound statistic on removal (no need to synchronize)
        if (lastStartTime > 0)
            stat.stop();
    }


    //****************** Statistics merging ******************//
    
    protected synchronized void updateFrom(StatisticTimer sourceStat) {
        time += sourceStat.get();
    }
    
    protected synchronized void setFrom(StatisticTimer sourceStat) {
        time = sourceStat.get();
    }
    
    public void reset() {
        time = 0;
        lastStartTime = 0;
        setCheckpoint();
    }
    
    //****************** Constructors ******************//

    /** Creates a new instance of StatisticTimer */
    protected StatisticTimer(String name) {
        super(name);
    }

    
    //****************** Creator ******************//
    
    /** Create new statistic timer with specified name or get the one already existing */
    public static StatisticTimer getStatistics(String name) throws ClassCastException {
        return statistics.get(name, StatisticTimer.class);
    }

    
    //****************** Text representation ******************//
    
    @Override
    public String toString() {
        return getName() + ": " + get();
    }

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
