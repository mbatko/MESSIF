/*
 * Factory.java
 *
 * Created on 10. kveten 2005, 21:09
 */

package messif.statistics;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;


/**
 *
 * @author xbatko
 */
class StatisticsList extends AbstractCollection<Statistics<?>> implements Serializable {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /**
     * Internal representation of the list of statistics 
     */
    protected final Map<String, Statistics<?>> statistics;
    
    /****************** Constructors ******************/

    /**
     * Creates a new instance of StatisticsList 
     */
    public StatisticsList() {
        statistics = new TreeMap<String, Statistics<?>>();
    }


    /****************** Collection operations  ******************/
    
    /** Internal iterator for statistics with names that match a regular expression */
    protected static class StatisticsRegexpIterator implements Iterator<Statistics<?>> {
        protected final Iterator<Statistics<?>> iterator;
        protected final String regexp;
        protected Statistics<?> nextObject;

        /** Creates new instance of StatisticsRegexpIterator */
        public StatisticsRegexpIterator(Iterator<Statistics<?>> iterator, String regexp) {
            this.iterator = iterator;
            this.regexp = (regexp == null)?"":regexp;
            
            this.nextObject = (iterator == null)?null:getNextMatching();
        }
        
        protected Statistics<?> getNextMatching() {
            // Search for next statistic
            while (iterator.hasNext()) {
                Statistics stat = iterator.next();
                
                // If its name matches specified regular expression
                if (stat.getName().matches(regexp))
                    return stat;
            }
            
            return null;
        }

        /** Return true if there is another statistic with a matching name */
        public boolean hasNext() {
            return nextObject != null;
        }

        /** Returns next statistic object with name that matches the regexp */
        public Statistics<?> next() {
            if (nextObject == null)
                throw new NoSuchElementException();

            // Save current next object that will be returned later
            Statistics rtv = nextObject;
            nextObject = getNextMatching();
            return rtv;
        }

        /** Removal is unsupported */
        public void remove() {
            throw new UnsupportedOperationException("StatisticsRegexpIterator cannot remove statistics");
        }
    }

    /** Returns iterator through all the stored statistics */
    public Iterator<Statistics<?>> iterator() {
        return Collections.unmodifiableCollection(statistics.values()).iterator();
    }

    /** Returns iterator through all the stored statistics that match the provided regular expression */
    public Iterator<Statistics<?>> iterator(String regex) {
        if (regex == null)
            return iterator();
        return new StatisticsRegexpIterator(statistics.values().iterator(), regex);
    }
    
    /** Readonly collection of all the stored statistics */
    protected List<Statistics<?>> getAllStatistics(String regex) {
        List<Statistics<?>> rtv = new ArrayList<Statistics<?>>();
        for (Iterator<Statistics<?>> iterator = iterator(regex); iterator.hasNext();)
            rtv.add(iterator.next());
        return rtv;
    }

    /** Returns number of the stored statistics */
    public int size() {
        return statistics.size();
    }
    
    /** Add specified statistic to the collection. Returns false if a statistic with the same name already exists. */
    public boolean add(Statistics<?> stat) {
        // Disallow duplicate names
        if (statistics.containsKey(stat.getName()))
            return false;
        
        statistics.put(stat.getName(), stat);
        
        return true;
    }
    
    /** Get statistic of specified name or null if it doesn't exist */
    public Statistics<?> get(String name) {
        return statistics.get(name);
    }
    
    /**
     * Removes given statistic.
     * @return the removed statistic or null, if it does not exist
     */
    public Statistics remove(String name) {
        Statistics stat = statistics.remove(name);
        if (stat != null)
            stat.unbind();
        return stat;
    }

    /** Check if the list contains statistics with specified name */
    public boolean contains(String name) {
        return statistics.containsKey(name);
    }
    
    // More operations of the original `values()' collection may be overriden to enhance the processing
    

    /****************** Special operations ******************/
    
    /** Resets all statistics */
    public void reset() {
        reset(null);
    }
    
    /** Resets statistics matching the regular expression */
    public void reset(String regex) {
        for (Iterator<Statistics<?>> iterator = iterator(regex); iterator.hasNext();)
            iterator.next().reset();
    }
    
    /** Unbind statistics matching the regular expression from their parents */
    public void unbind(String regex) {
        for (Iterator<Statistics<?>> iterator = iterator(regex); iterator.hasNext();)
            iterator.next().unbind();
    }
    
    /** Unbind all stored statistics from their parents */
    public void unbind() {
        unbind(null);
    }

    /** Returns String containing current states of registered statistics with names matching the provided regular expression */
    public String print(String regex, String statSeparator) {
        StringBuffer rtv = new StringBuffer();
        
        for (Iterator<Statistics<?>> iterator = iterator(regex); iterator.hasNext();) {
            if (rtv.length() > 0)
                rtv.append(statSeparator);
            rtv.append(iterator.next().toString());
        }
        
        return rtv.toString();
    }
    
    /** Returns String containing current states of registered statistics with names matching the provided regular expression */
    public String print(String regex) {
        return print(regex, "\n");
    }
    
    /** Returns String containing current states of all registered statistics */
    public String print() {
        return print(null);
    }

    /** toString
     */
    public String toString() {
        return print(null, ", ");
    }
    
    /****************** Factory ******************/
    
    /** Create new statistics with specified name or get the one already existing */
    public <T extends Statistics<? extends T>> T get(String statisticName, Class<? extends T> statisticClass) throws ClassCastException {
        // Get statistics from current registry
        Statistics<?> stat = get(statisticName);
        
        if (stat == null) {
            // Create a new instance of the statistics
            stat = Statistics.createInstance(statisticName, statisticClass);

            // Register to map
            statistics.put(statisticName, stat);
        }

        // Check correct type of the returned stat
        return statisticClass.cast(stat);
    }

}
