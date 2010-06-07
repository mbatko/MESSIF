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



/**
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public final class StatisticCounter extends Statistics<StatisticCounter> {    

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    @Override
    protected StatisticCounter cast() {
        return this;
    }

    //****************** Counter operation ******************//

    protected long value;
    private long valueCheckpoint = 0;         /* backup value for checkpoint feature */

    public void set(long value) {
        if (!canPerformOperation()) return;
        synchronized (this) {
            this.value = value;
            for (StatisticCounter stat : getBoundStats())
                stat.set(value);
        }
    }

    public void add(long value) {
        if (!canPerformOperation()) return;
        synchronized (this) {
            this.value += value;
            for (StatisticCounter stat : getBoundStats())
                stat.add(value);
        }
    }

    public void add() { add(1); }
    public void sub(long value) { add(-value); }
    public void sub() { add(-1); }
    
    public long get() { return value; }

    @Override
    public Object getValue() {
        return get();
    }

    public void max(long value) {
        if (!canPerformOperation()) return;
        synchronized (this) {
            if (this.value < value)
                this.value = value;
            for (StatisticCounter stat : getBoundStats())
                stat.max(value);
        }
    }
    
    public void min(long value) {
        if (!canPerformOperation()) return;
        synchronized (this) {
            if (this.value > value)
                this.value = value;
            for (StatisticCounter stat : getBoundStats())
                stat.min(value);
        }
    }


    //****************** Statistics merging ******************//
    
    protected synchronized void updateFrom(StatisticCounter sourceStat) {
        this.value += sourceStat.value;
    }
    
    protected synchronized void setFrom(StatisticCounter sourceStat) {
        this.value = sourceStat.value;
    }
    
    /** Reset the current statistic (this one only).
     */
    public void reset() {
        this.value = 0;
        setCheckpoint();
    }
    
    //****************** Constructors ******************//

    /** Creates a new instance of StatisticCounter */
    protected StatisticCounter(String name) {
        super(name);
        this.value = 0;
    }

    /** Creates a new instance of StatisticCounter */
    protected StatisticCounter(String name, long value) {
        super(name);
        this.value = value;
    }

    //****************** Creator ******************//
    
    /** Create new statistic counter with specified name or get the one already existing */
    public static StatisticCounter getStatistics(String name) {
        return getStatistics(name, StatisticCounter.class);
    }

    //****************** Text representation ******************//
    
    @Override
    public String toString() {
        return getName() + ": " + get();
    }

    /** Reports if value of counter has been changed since the last setCheckpoint() call.
     */
    public boolean changedSinceCheckpoint() {
        return (value != valueCheckpoint);
    }
    
    /** Sets checkpoint. Stores the current state of counter.
     */
    public void setCheckpoint() {
        valueCheckpoint = value;
    }
    
}
