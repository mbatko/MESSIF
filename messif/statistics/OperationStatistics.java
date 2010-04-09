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

import java.io.Serializable;
import java.util.Iterator;

/**
 * A local (operation's) list of statistics.
 * This is typically used to retrieve statistics only during a single operation
 * execution - the statistics registered in this collection are not influenced
 * by other running operations (or other methods possibly modyfying statistics).
 *
 * <p>
 * To retrieve a statistic, use {@link Statistics#bindTo(messif.statistics.Statistics) binding}.
 * There are several methods to ease the task of biding a {@link OperationStatistics}
 * to global statistics, such as {@link #registerBoundStat(java.lang.String)}.
 * </p>
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public final class OperationStatistics implements Serializable {

    /** class id for serialization */
    private static final long serialVersionUID = 1L;


    /****************** Constructors ******************/

    /** Creates a new instance of OperationStatistics */
    protected OperationStatistics() {
        Statistics.getCurrentThreadLock(); // Just get the value to initialized it (first thread)
    }
    
    /** Destructor - unbind all binded objects */
    protected void finalize() throws Throwable {
        statistics.unbind();
        super.finalize();
    }

    /****************** Creator ******************/

    // Thread specific operation statistics object factory
    private static ThreadLocal<OperationStatistics> threadFactory = new ThreadLocal<OperationStatistics>() {
     protected synchronized OperationStatistics initialValue() {
         return new OperationStatistics();
     }
    };
    
    /** OperationStatistics creator */
    public static OperationStatistics getLocalThreadStatistics() {
        return threadFactory.get();
    }
    
    /** Reset current thread statistic */
    public static void resetLocalThreadStatistics() {
        threadFactory.get().statistics.unbind();
        threadFactory.set(new OperationStatistics());
    }


    /****************** Local operation statistic objects ******************/
    
    protected final StatisticsList statistics = new StatisticsList();
    
    /** Access all statistics */
    public Iterator<Statistics<?>> getAllStatistics() {
        return statistics.iterator();
    }
        
    /** Access statistics whose names match the given regular expression */
    public Iterator<Statistics<?>> getAllStatistics(String regex) {
        return statistics.iterator(regex);
    }
    
    /** Returns String containing current states of registered statistics with names matching the provided regular expression
        and separated by specified separator
     */
    public String printStatistics(String regex, String statSeparator) {
        return statistics.print(regex, statSeparator);
    }

    /** Returns String containing current states of registered statistics with names matching the provided regular expression */
    public String printStatistics(String regex) {
        return statistics.print(regex);
    }

    /** Returns String containing current states of registered statistics */
    public String printStatistics() {
        return statistics.print();
    }

    /** Resets statistics within this operation statistic object names of which match the regular expression */
    public void resetStatistics(String regex) {
        statistics.reset(regex);
    }

    /** Resets all statistics within this operation statistic object. */
    public void resetStatistics() {
        statistics.reset();
    }
    
    /** Return a statistics of defined class from this operation statistics namespace */
    public <T extends Statistics<T>> T getStatistics(String statisticName, Class<? extends T> statisticClass) throws ClassCastException {
        T stat = statistics.get(statisticName, statisticClass);
        stat.lockToThread();
        return stat;
    }
    
    /** Returns statistics counter from current thread operation statistics namespace */
    public static <T extends Statistics<T>> T getOpStatistics(String name, Class<? extends T> statisticsClass) throws ClassCastException {
        return getLocalThreadStatistics().getStatistics(name, statisticsClass);
    }
    
    /** Returns statistics counter from this operation statistics namespace */
    public StatisticCounter getStatisticCounter(String name) throws ClassCastException {
        return getStatistics(name, StatisticCounter.class);
    }

    /** Returns statistics counter from current thread operation statistics namespace */
    public static StatisticCounter getOpStatisticCounter(String name) throws ClassCastException {
        return getLocalThreadStatistics().getStatisticCounter(name);
    }
    
    /** Returns statistics minmaxcounter from this operation statistics namespace */
    public StatisticMinMaxCounter getStatisticMinMaxCounter(String name) throws ClassCastException {
        return getStatistics(name, StatisticMinMaxCounter.class);
    }

    /** Returns statistics counter from current thread operation statistics namespace */
    public static StatisticMinMaxCounter getOpStatisticMinMaxCounter(String name) throws ClassCastException {
        return getLocalThreadStatistics().getStatisticMinMaxCounter(name);
    }
    
    /** Returns statistics reference counter from this operation statistics namespace */
    public StatisticRefCounter getStatisticRefCounter(String name) throws ClassCastException {
        return getStatistics(name, StatisticRefCounter.class);
    }

    /** Returns statistics reference counter from current thread operation statistics namespace */
    public static StatisticRefCounter getOpStatisticRefCounter(String name) throws ClassCastException {
        return getLocalThreadStatistics().getStatisticRefCounter(name);
    }
    
    /** Removes given statistic. @return <b>true</b> if statistic existed and was removed, <b>false</b> otherwise. */
    public boolean removeStatistic(String name) {
        return (statistics.remove(name) != null);
    }
    
    /****************** Binding statistic registry ******************/

    /**
     * Register bound statistic (using asName name) in this operation statistics namespace.
     * If there is no global stat of specified name, it is created.
     */
    public <T extends Statistics<T>> T registerBoundStat(Class<? extends T> statClass, String name, String asName) throws ClassCastException {
        // Get the duplicate of the source statistic from global namespace
        T stat = Statistics.getStatistics(name, statClass);

        // Get the new statistic from the local namespace
        T newStat = getStatistics(asName, statClass);

        newStat.bindTo(stat);

        return newStat;
    }

    /**
     * Register statistic in this operation statistics namespace.
     * The name <code>asName</code> will be used and the same class as the <code>bindToStat</code>.
     * If there is a statistics with this name already, the statistic is not created
     * but the old one is used instead.
     * 
     * @param asName the name of the statistic in this operation statistics namespace
     * @param bindToStat the statistics to which the new statistics is bound
     * @return the newly registered statistics
     * @throws ClassCastException if there is a statistics with this name, but has different class than <code>bindToStat</code>
     */
    protected <T extends Statistics<T>> T registerBoundStat(String asName, Statistics<T> bindToStat) throws ClassCastException {
        // Get the new statistic from the local namespace
        @SuppressWarnings("unchecked")
        T newStat = getStatistics(asName, (Class<? extends T>)bindToStat.getClass());
        newStat.bindTo(bindToStat.cast());
        return newStat;
    }

    /** Register bound statistic (using asName name) in this operation statistics namespace */
    public Statistics<?> registerBoundStat(String name, String asName) throws IllegalArgumentException {
        // Get the duplicate of the source statistic from global namespace
        Statistics<?> stat = Statistics.getStatistics(name);

        if (stat == null)
            throw new IllegalArgumentException("Statistic '" + name + "' not found in the registry");

        return registerBoundStat(asName, stat);
    }

    /** Register bound statistic in this operation statistics namespace */
    public Statistics<?> registerBoundStat(String name) throws IllegalArgumentException {
        return registerBoundStat(name, name);
    }
    
    /** In this operation statistics namespace, register and bind statistics which are present in the global namespace and 
     * match the given regular expression. */
    public void registerBoundAllStats(String regex) {
        Iterator<Statistics<?>> iterator = Statistics.getAllStatistics(regex);
        while (iterator.hasNext()) {
            Statistics<?> stat = iterator.next();
            registerBoundStat(stat.getName(), stat);
        }
    }

    /** Unbind statistics matching the regular expression from their parents */
    public void unbindAllStats(String regex) {
        statistics.unbind(regex);
    }

    /**
     * Unbind all stored statistics from their parents.
     */
    public void unbindAllStats() {
        statistics.unbind();
    }

    //****************** Statistics merging ******************//
    
    /**
     * Update this statistics with other operation statistics values.
     * @param sourceStats the operation statistics that are merged with this ones
     * @throws IllegalArgumentException if there was a statistic of the same name
     *          in both this and {@code sourceStats} but with of different class
     */
    @SuppressWarnings("unchecked")
    public synchronized void updateFrom(OperationStatistics sourceStats) throws IllegalArgumentException {
        // For every source statistics update values in our stat
        for (Statistics<?> stat : sourceStats.statistics) {
            // Update our statistics
            getStatistics(stat.getName(), stat.getClass()).updateFrom(stat); // This cast IS checked, since stat and new stat have exactly the same class...
        }
    }
    

    //****************** Text representation ******************//

    @Override
    public String toString() {
        return statistics.toString();
    }

}
