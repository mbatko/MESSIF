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

import java.util.*;

/**
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class StatisticObject extends Statistics<StatisticObject> {
    
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    /****************** Object operation ******************/
    protected Object value = null;
    
    private Object valueCheckpoint = null;      /* backup value for checkpoints */
    
    
    public void set(Object object) {
        if (!canPerformOperation())
            return;

        synchronized (this) {
            this.value = object;
            for (StatisticObject stat : getBoundStats())
                stat.set(object);
        }
    }
    
    public Object get() { return value; }
    
    
    /****************** Statistics merging ******************/
    
    protected synchronized void updateFrom(StatisticObject sourceStat) {
        this.value = sourceStat.value;
    }
    
    protected synchronized void setFrom(StatisticObject sourceStat) {
        this.value = sourceStat.value;
    }
    
    /** Reset the current statistic (this one only).
     */
    public void reset() {
        this.value = null;
        setCheckpoint();
    }
    
    
    /****************** Constructors ******************/
        
    /** Creates a new instance of StatisticObject */
    protected StatisticObject(String name) {
        super(name);
    }
    

    /****************** Creator ******************/
    
    /** Create new statistic object with specified name or get the one already existing */
    public static StatisticObject getStatistics(String name) throws ClassCastException {
        return statistics.get(name, StatisticObject.class);
    }

    
    /****************** Text representation ******************/
    
    @Override
    public String toString() {
        return getName() + ": " + value;
    }


    /** Reports if value of statistic has been changed since the last setCheckpoint() call.
     */
    public boolean changedSinceCheckpoint() {
        return (value != valueCheckpoint);
    }
    
    /** Sets checkpoint. Stores the current state of value object.
     */
    public void setCheckpoint() {
        valueCheckpoint = value;
    }

    @Override
    protected StatisticObject cast() {
        return this;
    }
    
}
