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

import java.util.Iterator;
import messif.objects.LocalAbstractObject;
import messif.objects.ObjectProvider;
import messif.objects.classification.Classification;
import messif.objects.classification.ClassificationException;
import messif.objects.classification.Classifier;
import messif.objects.util.RankedAbstractObject;

/**
 * Implementation of a classifier that computes the classification using a {@link ObjectProvider provider}
 * of a classified set by measuring distances. The actual classification is computed
 * from the distances by a given {@link Classifier}.
 *
 * @param <C> the class of instances that represent the classification categories
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ObjectProviderClassifier<C> implements Classifier<LocalAbstractObject, C> {

    /** Classifier used to compute the object classification */
    private final Classifier<? super Iterator<? extends RankedAbstractObject>, C> classifier;
    /** Provider of the classified objects */
    private final ObjectProvider<? extends LocalAbstractObject> classifiedObjects;

    /**
     * Creates a new kNN classifier.
     * @param classifier the classifier used to compute the object classification
     * @param classifiedObjects the provider of the classified objects
     */
    public ObjectProviderClassifier(Classifier<? super Iterator<? extends RankedAbstractObject>, C> classifier, ObjectProvider<? extends LocalAbstractObject> classifiedObjects) {
        this.classifier = classifier;
        this.classifiedObjects = classifiedObjects;
    }

    @Override
    public Classification<C> classify(final LocalAbstractObject object) throws ClassificationException {
        final Iterator<? extends LocalAbstractObject> iterator = classifiedObjects.provideObjects();
        return classifier.classify(new Iterator<RankedAbstractObject>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }
            @Override
            public RankedAbstractObject next() {
                return new RankedAbstractObject(object, iterator.next());
            }
            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported.");
            }
        });
    }

    @Override
    public Class<? extends C> getCategoriesClass() {
        return classifier.getCategoriesClass();
    }

    @Override
    public Class<? extends LocalAbstractObject> getClassifiedClass() {
        return LocalAbstractObject.class;
    }

}
