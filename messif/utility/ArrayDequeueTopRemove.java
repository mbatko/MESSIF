/*
 *  This file is part of M-Index library.
 *
 *  M-Index library is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  M-Index library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with M-Index library.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.utility;

import java.util.ArrayDeque;
import java.util.NoSuchElementException;

/**
 * This is an extension of the standard Java {@link ArrayDeque} class that also implements
 *  method(s) from {@link QueueTopRemove}.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ArrayDequeueTopRemove<E> extends ArrayDeque<E> implements QueueTopRemove<E> {

    public ArrayDequeueTopRemove() {
    }

    public ArrayDequeueTopRemove(int numElements) {
        super(numElements);
    }    
    
    public void removeFirstN(int count) throws NoSuchElementException {
        for (int i = 0; i < count; i++) {
            removeFirst();
        }
    }
    
}
