/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.utility;

import java.util.Iterator;

/**
 * Iterator that can be restarted and that returns its size.
 * 
 * @param <E> any class 
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface ResetableIterator<E> extends Iterator<E> {
    /**
     * Reset this iterator and start iterating from the beginning.
     */
    public void reset();
    
    /**
     * Number of objects this iterator returns.
     * @return number of objects
     */
    public int size();
}
