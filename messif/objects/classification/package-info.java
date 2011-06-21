/**
 * Support for object classification.
 * Classifiers determine categories (classes) that a given object belongs to.
 * A category is represented by any object, typically a {@link java.lang.String}
 * or {@link java.lang.Integer int identifier}.
 * An object can belong to multiple categories and a category can have multiple
 * objects.
 *
 * <p>
 * There are basically two approaches to classification:
 * <ul>
 * <li>similarity classification - the classified object belongs to the
 *      categories where its most similar objects belong;</li>
 * <li>training-based classification - the classifier is trained first
 *      (by providing positive and negative examples) and then it is able
 *      to classify objects on its own.</li>
 * </ul>
 * </p>
 *
 * 
 */
package messif.objects.classification;

