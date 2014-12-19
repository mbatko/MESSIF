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
package messif.objects.classification.impl;

import messif.objects.classification.Classification;
import messif.objects.classification.ClassificationException;
import messif.objects.classification.Classifications;
import messif.objects.classification.Classifier;
import messif.utility.Parametric;

/**
 * Simple classifier that only retrieves an existing {@link Classification}
 * from the given object parameter.
 * @param <C> the class of instances that represent the classification categories
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ParametricSimpleGetClassifier<C> implements Classifier<Parametric, C> {
    /** Class of instances that represent the classification categories */
    private final Class<? extends C> categoriesClass;
    /** Name of the {@link Parametric} parameter that contains the classification categories */
    private final String parameterName;

    /**
     * Creates a new simple classifier that retrieves an existing {@link Classification}
     * from the given object parameter.
     * @param categoriesClass the class of instances that represent the classification categories
     * @param parameterName the name of the {@link Parametric} parameter that contains the classification categories
     * @throws NullPointerException if either categoriesClass or parameterName is <tt>null</tt>
     */
    public ParametricSimpleGetClassifier(Class<? extends C> categoriesClass, String parameterName) throws NullPointerException {
        if (categoriesClass == null)
            throw new NullPointerException("Categories class cannot be null");
        this.categoriesClass = categoriesClass;
        if (parameterName == null)
            throw new NullPointerException("Parameter name cannot be null");
        this.parameterName = parameterName;
    }

    @Override
    public Classification<C> classify(Parametric object, Parametric parameters) throws ClassificationException {
        return Classifications.castToClassification(object.getRequiredParameter(parameterName), categoriesClass);
    }

    @Override
    public Class<? extends C> getCategoriesClass() {
        return categoriesClass;
    }

}
