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
package messif.objects.util;

import java.util.Iterator;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;

/**
 * Special iterator that iterates over the encapsulated objects of a given {@link MetaObject}.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class MetaObjectEncapsulatedIterator implements Iterator<LocalAbstractObject> {
    /** Encapsulated iterator that provides the envelope {@link MetaObject} */
    private final Iterator<? extends MetaObject> iterator;
    /** Name of the encapsulated object inside the {@link MetaObject} to get */
    private final String objectName;

    /**
     * Creates a new iterator that iterates over {@code objectName} object from
     * inside the {@link MetaObject}s provided by the given {@code iterator}.
     * @param iterator the encapsulated iterator that provides the envelope {@link MetaObject}
     * @param objectName the name of the encapsulated object inside the {@link MetaObject} to get
     */
    public MetaObjectEncapsulatedIterator(Iterator<? extends MetaObject> iterator, String objectName) {
        this.iterator = iterator;
        this.objectName = objectName;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public LocalAbstractObject next() {
        return iterator.next().getObject(objectName);
    }

    @Override
    public void remove() {
        iterator.remove();
    }
}
