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

import messif.objects.LocalAbstractObject;
import messif.objects.classification.Classification;
import messif.objects.classification.ClassificationException;
import messif.objects.classification.Classifier;
import messif.objects.extraction.Extractor;
import messif.objects.extraction.ExtractorDataSource;
import messif.utility.ModifiableParametric;
import messif.utility.Parametric;

/**
 * Implementation of a classifier that provides an extracted object on which the
 * classification is computed using an encapsulated classifier.
 *
 * @param <C> the class of instances that represent the classification categories
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ExtractorClassifier<C> implements Classifier<ExtractorDataSource, C> {

    /** Classifier used to compute the object classification */
    private final Classifier<? super LocalAbstractObject, C> classifier;
    /** Extractor that supplies the extracted objects */
    private final Extractor<? extends LocalAbstractObject> extractor;
    /** Name of the parameter to put the extracted object into when classifying */
    private final String extractedObjectParameter;

    /**
     * Creates a new extractor classifier.
     * @param classifier the classifier used to compute the object classification
     * @param extractor the extractor that supplies the extracted objects
     * @param extractedObjectParameter the name of the parameter to put the extracted object into when classifying
     */
    public ExtractorClassifier(Classifier<? super LocalAbstractObject, C> classifier, Extractor<? extends LocalAbstractObject> extractor, String extractedObjectParameter) {
        this.classifier = classifier;
        this.extractor = extractor;
        this.extractedObjectParameter = extractedObjectParameter;
    }

    @Override
    public Classification<C> classify(ExtractorDataSource dataSource, Parametric parameters) throws ClassificationException {
        try {
            LocalAbstractObject object = extractor.extract(dataSource);
            if (parameters instanceof ModifiableParametric && extractedObjectParameter != null)
                ((ModifiableParametric)parameters).setParameter(extractedObjectParameter, object);
            return classifier.classify(object, parameters);
        } catch (Exception e) {
            throw new ClassificationException("There was an error extracting object: " + e, e.getCause());
        }
    }

    @Override
    public Class<? extends C> getCategoriesClass() {
        return classifier.getCategoriesClass();
    }

    /**
     * Returns the extracted object stored by this classifier in the given parameters.
     * @param parameters the parameters to get the stored object from
     * @return the extracted object or <tt>null</tt> if no object was stored in the parameters
     */
    public LocalAbstractObject getExtractedObject(Parametric parameters) {
        if (extractedObjectParameter == null || parameters == null)
            return null;
        return parameters.getParameter(extractedObjectParameter, LocalAbstractObject.class);
    }
}
