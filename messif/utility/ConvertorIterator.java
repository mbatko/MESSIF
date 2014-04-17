/*
 * This file is part of MESSIF library.
 *
 * MESSIF library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MESSIF library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.utility;

import java.util.Iterator;

/**
 * Iterator that converts all objects from the encapsulated iterator.
 *
 * @param <F> the source class of the conversion, i.e. the class of objects returned by the encapsulated iterator
 * @param <T> the destination class of the conversion, i.e. the class of objects returned by this iterator
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ConvertorIterator<F, T> implements Iterator<T> {
    /** Encapsulated iterator that provides the objects to convert */
    private final Iterator<? extends F> iterator;
    /** Convertor to apply to iterated items */
    private final Convertor<? super F, ? extends T> convertor;
    
    /**
     * Creates a new iterator converts all objects from the encapsulated iterator.
     * @param iterator the encapsulated iterator that provides the objects to convert
     * @param convertor the convertor to apply to iterated items
     */
    public ConvertorIterator(Iterator<? extends F> iterator, Convertor<? super F, ? extends T> convertor) {
        this.iterator = iterator;
        this.convertor = convertor;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public T next() {
        try {
            return convertor.convert(iterator.next());
        } catch (Exception e) {
            throw new IllegalStateException("Error converting object: " + e, e);
        }
    }

    @Override
    public void remove() {
        iterator.remove();
    }
}
