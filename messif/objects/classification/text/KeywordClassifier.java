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
package messif.objects.classification.text;

import messif.objects.classification.ClassificationException;
import messif.objects.classification.Classifier;
import messif.utility.Parametric;

/**
 * Special classifier for keywords.
 * Allows to implement the expanding, reducing, or transforming elements of the annotation framework.
 * 
 * @param <I> the class of the input classification categories
 * @param <O> the class of the output classification categories
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 */
public interface KeywordClassifier<I, O> extends Classifier<KeywordClassification<I>, O> {
    /**
     * Returns the class of the input classification categories.
     * @return the class of the input classification categories
     */
    public Class<? extends I> getInputCategoriesClass();

    /**
     * Returns the class of the output classification categories.
     * @return the class of the output classification categories
     */
    @Override
    public Class<? extends O> getCategoriesClass();

    /**
     * Transforms the given {@code inputClassification} into the output keyword classification.
     * @param inputClassification the keyword classification to transform
     * @param parameters additional parameters for the classification;
     *          the values for the parameters are specific to a given classifier
     *          implementation and can be updated during the process if they are {@link messif.utility.ModifiableParametric}
     * @return a set of categories the object belongs to
     * @throws ClassificationException if there was an error classifying the object
     */
    @Override
    public KeywordClassification<O> classify(KeywordClassification<I> inputClassification, Parametric parameters) throws ClassificationException;
}
