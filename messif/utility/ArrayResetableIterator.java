/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.utility;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @param <E> any object class
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ArrayResetableIterator<E> implements ResetableIterator<E> {
    private final E[] objs;
    private int curIdx = 0;

    public ArrayResetableIterator(E[] objs) {
        this.objs = objs;
    }
       
    @SuppressWarnings({"unchecked", "cast"})
    public ArrayResetableIterator(List<E> list) {
        this.objs = (E[])((ArrayList<E>)list).toArray();
    }
            
    @Override
    public boolean hasNext() {
        return (curIdx < objs.length);
    }

    @Override
    public E next() {
        return objs[curIdx++];
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Delete is not supported.");
    }

    @Override
    public void reset() {
        curIdx = 0;
    }

    @Override
    public int size() {
        return objs.length;
    }
}
