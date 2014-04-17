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
package messif.algorithms;

import messif.operations.AbstractOperation;

/**
 * Interface for classes (typically {@link Algorithm algorithms})
 * that can provide an {@link NavigationProcessor} for evaluating
 * operations. Typically, the navigation directory identifies one
 * or more partitions on which the particular operation should be
 * evaluated and returns a processor that encapsulates them.
 * The processor is then used to evaluate the operation generically,
 * so that parallel or distributed environment can be utilized
 * and redundant data accessed can be avoided.
 * 
 * @param <O> the type of operations processed by this navigation directory
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface NavigationDirectory <O extends AbstractOperation> {
    /**
     * Returns the {@link NavigationProcessor navigator} that provides the steps
     * in which the given operation should be evaluated.
     * 
     * @param operation the operation to get the navigator for
     * @return a navigator for processing the given operation
     */
    public NavigationProcessor<? extends O> getNavigationProcessor(O operation);
}
