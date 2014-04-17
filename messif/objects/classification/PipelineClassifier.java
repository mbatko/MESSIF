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
package messif.objects.classification;

import messif.utility.Parametric;

/**
 * Provides a classification via executing a pipeline of several classifiers.
 * Note that the first classifier in the pipeline must accept an instance of {@code T}
 * and the last one must provide a {@link Classification} with categories {@code C}.
 * Each classifier in the pipeline also must accept the result of the previous classifier.
 * 
 * @param <T> the class of the input instance for the classification
 * @param <C> the class of the output classification categories
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 */
public final class PipelineClassifier<T, C> implements Classifier<T, C> {
    /** Class of instances that represent the classification categories of this classifier */
    private final Class<? extends C> categoriesClass;
    /** List of classifiers that for the pipeline */
    private final Classifier[] pipelineClassifiers;

    /**
     * Creates a pipeline of classifiers.
     * @param initialClassifier the first classifier in the pipeline
     * @param categoriesClass the class of the output classification categories
     * @param pipelineClassifiers 
     */
    public PipelineClassifier(Class<? extends C> categoriesClass, Classifier<T, ?> initialClassifier, Classifier... pipelineClassifiers) {
        this.categoriesClass = categoriesClass;
        if (pipelineClassifiers != null && pipelineClassifiers.length > 0) {
            this.pipelineClassifiers = new Classifier<?,?>[pipelineClassifiers.length + 1];
            this.pipelineClassifiers[0] = initialClassifier;
            System.arraycopy(pipelineClassifiers, 0, this.pipelineClassifiers, 1, pipelineClassifiers.length);
        } else {
            this.pipelineClassifiers = new Classifier<?,?>[] { initialClassifier };
        }
        
//        // Check pipeline input/output
//        if (pipelineClassifiers != null) {
//            Method prevClassifierMethod = Classifications.getClassifierClassifyMethod(initialClassifier);
//            for (int i = 0; i < pipelineClassifiers.length; i++) {
//                Method currClassifierMethod = Classifications.getClassifierClassifyMethod(pipelineClassifiers[i]);
//                if (!currClassifierMethod.getParameterTypes()[0].isAssignableFrom(prevClassifierMethod.getReturnType()))
//                    throw new IllegalArgumentException("Classifier #" + i + " in the pipeline does not accept the previous classifier output " + prevClassifierMethod.getReturnType().getCanonicalName());
//                prevClassifierMethod = currClassifierMethod;
//            }
//        }

        // Check last classifier category class
        if (!categoriesClass.isAssignableFrom(this.pipelineClassifiers[this.pipelineClassifiers.length - 1].getCategoriesClass()))
            throw new IllegalArgumentException("Last classifier in the pipeline must return classification of categories class");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Classification<C> classify(T object, Parametric parameters) throws ClassificationException {
        Classification<?> classification = pipelineClassifiers[0].classify(object, parameters); // This cast IS checked by the constructor
        for (int i = 1; i < pipelineClassifiers.length; i++) {
            try {
                classification = pipelineClassifiers[i].classify(classification, parameters); // This cast is not checked, but the exception will be thrown
            } catch (ClassCastException e) {
                throw new ClassificationException("Cannot cast output to output of " + pipelineClassifiers[i-1] + " to input of " + pipelineClassifiers[i] + ": " + e.getMessage());
            }
        }
        return (Classification<C>)classification; // This cast IS checked by the constructor
    }

    @Override
    public Class<? extends C> getCategoriesClass() {
        return categoriesClass;
    }
}
