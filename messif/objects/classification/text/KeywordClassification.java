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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import messif.objects.classification.ClassificationWithConfidence;
import messif.objects.classification.ClassificationWithConfidenceBase;
import messif.utility.ModifiableParametric;

/**
 * Encapsulation object for the data on which the {@link KeywordClassifier} operates.
 * 
 * @param <T> the class that represents keywords in this classification
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 */
public class KeywordClassification<T> extends ClassificationWithConfidenceBase<T> implements Serializable, ModifiableParametric {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    ///////////////// Attributes /////////////////

    /** Internal parameters */
    private final Map<String, Serializable> parameters;
    /** Internal list of keyword multiple-confidence parameters for every added keyword */
    private final Map<T, List<Float>> keywordMultiConfidence;
    /** The lowest confidence seen in this classification so far */
    private float currentLowestConfidence;
    /** The highest confidence seen in this classification so far */
    private float currentHighestConfidence;


    ///////////////// Constructors /////////////////

    /**
     * Creates a new instance of keyword classification for the given keyword-representing class.
     * @param keywordClass the class that represents keywords in this classification
     * @param lowestConfidence the lowest possible confidence of this classification
     * @param highestConfidence the highest possible confidence of this classification
     */
    public KeywordClassification(Class<? extends T> keywordClass, float lowestConfidence, float highestConfidence) {
        super(keywordClass, lowestConfidence, highestConfidence);
        currentLowestConfidence = highestConfidence; // This is intentional, so that any confidence put into will be smaller
        currentHighestConfidence = lowestConfidence;
        parameters = new HashMap<String, Serializable>();
        keywordMultiConfidence = new HashMap<T, List<Float>>();
    }

    /**
     * Creates a new instance of keyword classification copying the settings from the given classification.
     * @param keywordClass the class that represents keywords in this classification
     * @param classification the classification to copy the class, the lowest/highest confidence, and parameters from
     */
    public KeywordClassification(Class<? extends T> keywordClass, ClassificationWithConfidence<?> classification) {
        super(keywordClass, classification.getLowestConfidence(), classification.getHighestConfidence());
        currentLowestConfidence = getHighestConfidence(); // This is intentional, so that any confidence put into will be smaller
        currentHighestConfidence = getLowestConfidence();
        if (classification instanceof KeywordClassification) {
            this.parameters = ((KeywordClassification<?>)classification).parameters;
        } else {
            this.parameters = new HashMap<String, Serializable>();
        }
        this.keywordMultiConfidence = new HashMap<T, List<Float>>();
    }

    /**
     * Creates a new instance of keyword classification copying the settings from the given classification.
     * @param classification the classification to copy the class, the lowest/highest confidence, and parameters from
     */
    public KeywordClassification(ClassificationWithConfidence<? extends T> classification) {
        this(classification.getStoredClass(), classification);
    }


    ///////////////// Computation of keyword confidences and frequencies /////////////////

    /**
     * Update multi confidence for the given keyword.
     * @param keyword the keyword for which the confidence parameters are updated
     * @param confidence the added confidence
     */
    protected void addKeywordConfidence(T keyword, float confidence) {
        // Update current low/high confidences
        if (getLowestConfidence() < getHighestConfidence()) { // Distance ordering
            if (confidence < currentLowestConfidence)
                currentLowestConfidence = confidence;
            if (confidence > currentHighestConfidence)
                currentHighestConfidence = confidence;
        } else {
            if (confidence > currentLowestConfidence)
                currentLowestConfidence = confidence;
            if (confidence < currentHighestConfidence)
                currentHighestConfidence = confidence;
        }

        // Update multi confidence array
        if (keywordMultiConfidence != null) {
            List<Float> confidences = keywordMultiConfidence.get(keyword);
            if (confidences == null) {
                confidences = new ArrayList<Float>();
                keywordMultiConfidence.put(keyword, confidences);
            }
            confidences.add(confidence);
        }
    }

    @Override
    public ClassificationWithConfidenceBase<T> add(T category, float confidence) throws IllegalArgumentException {
        addKeywordConfidence(category, confidence);
        return super.add(category, confidence);
    }

    @Override
    public boolean updateConfidence(T category, float confidence) throws IllegalArgumentException {
        if (super.updateConfidence(category, confidence))
            return true; // confidence parameters are updated in add
        addKeywordConfidence(category, confidence);
        return false;
    }

    /**
     * Returns all confidences seen for the given keyword in this classification.
     * @param keyword the keyword the get the list of confidences for
     * @return all the confidences of the given keyword
     */
    public List<Float> getAllConfidence(T keyword) {
        List<Float> ret = keywordMultiConfidence.get(keyword);
        return ret == null ? null : Collections.unmodifiableList(ret);
    }

    /**
     * Returns all confidences seen for the given keyword in this classification
     * normalized by subtracting the seen minimal value and dividing by the difference
     * of the seen minimal and maximal values.
     * @param keyword the keyword the get the list of confidences for
     * @return all the confidences of the given keyword
     */
    public List<Float> getAllNormalizedConfidence(T keyword) {
        List<Float> multiConfidence = keywordMultiConfidence.get(keyword);
        if (multiConfidence == null)
            return null;
        List<Float> ret = new ArrayList<Float>(multiConfidence.size());
        for (int i = 0; i < multiConfidence.size(); i++)
            ret.add((multiConfidence.get(i) - currentLowestConfidence) / (currentHighestConfidence - currentLowestConfidence));
        return ret;
    }

    /**
     * Returns the number of confidences seen for the given keyword in this classification.
     * This represents the frequency of a keyword.
     * @param keyword the keyword the get the number of confidences for
     * @return the number of confidences of the given keyword
     */
    public int getAllConfidenceCount(T keyword) {
        List<Float> ret = keywordMultiConfidence.get(keyword);
        return ret == null ? 0 : ret.size();
    }

    /**
     * Returns the score of the given keyword.
     * Note that the score is computed as the sum of all the
     * multi-confidences normalized by subtracting the current lowest confidence
     * and dividing by the difference of the current lowest and highest confidences.
     * 
     * @param keyword the keyword the get the normalized confidence for
     * @return the normalized multi-confidence
     */
    public float getKeywordBoostedScore(T keyword) {
        List<Float> multiConfidence = keywordMultiConfidence.get(keyword);
        if (multiConfidence == null || multiConfidence.isEmpty())
            return getLowestConfidence();
        Iterator<Float> iterator = multiConfidence.iterator();
        float ret = (iterator.next() - currentLowestConfidence) / (currentHighestConfidence - currentLowestConfidence);
        while (iterator.hasNext())
            ret += (iterator.next() - currentLowestConfidence) / (currentHighestConfidence - currentLowestConfidence);
        return ret;
    }


    ///////////////// Parametric interface implementation /////////////////

    @Override
    public int getParameterCount() {
        return parameters.size();
    }

    @Override
    public Collection<String> getParameterNames() {
        return Collections.unmodifiableCollection(parameters.keySet());
    }

    @Override
    public boolean containsParameter(String name) {
        return parameters.containsKey(name);
    }

    @Override
    public Serializable getParameter(String name) {
        return parameters.get(name);
    }

    @Override
    public <P> P getParameter(String name, Class<? extends P> parameterClass) {
        return getParameter(name, parameterClass, null);
    }

    @Override
    public Serializable getRequiredParameter(String name) throws IllegalArgumentException {
        Serializable value = getParameter(name);
        if (value != null)
            return value;
        throw new IllegalArgumentException("The parameter '" + name + "' is not set");
    }

    @Override
    public <T> T getRequiredParameter(String name, Class<? extends T> parameterClass) throws IllegalArgumentException, ClassCastException {
        return parameterClass.cast(getRequiredParameter(name));
    }

    @Override
    public <T> T getParameter(String name, Class<? extends T> parameterClass, T defaultValue) {
        Object value = getParameter(name);
        return value != null && parameterClass.isInstance(value) ? parameterClass.cast(value) : defaultValue;
    }

    @Override
    public Map<String, ? extends Serializable> getParameterMap() {
        return Collections.unmodifiableMap(parameters);
    }

    /**
     * @inheritDoc
     * @throws ClassCastException if the given value is not {@link Serializable}
     */
    @Override
    public Serializable setParameter(String name, Object value) throws ClassCastException {
        return parameters.put(name, (Serializable)value);
    }

    @Override
    public Serializable removeParameter(String name) {
        return parameters.remove(name);
    }

    /**
     * Add a new parameter {@code name} with the given value.
     * If the parameter with the given {@code name} already exists,
     * IllegalArgumentException is thrown.
     * @param name the parameter name to set
     * @param value the new value for the parameter
     * @throws IllegalArgumentException if the parameter with the given {@code name} already exists
     */
    public void addParameter(String name, Serializable value) throws IllegalArgumentException {
        if (parameters.containsKey(name))
            throw new IllegalArgumentException("Parameter '" + name + "' already exists");
        setParameter(name, value);
    }


    ///////////////// Parametric extension for keyword parameters /////////////////

    /**
     * Returns the keyword parameter map for the given parameter {@code name}.
     * This is internal utility that is used in keyword-parameter methods.
     * 
     * @param name the name of the keyword parameter to get
     * @param create flag whether to create the parameter if it does not exist
     * @return the keyword parameter map
     */
    @SuppressWarnings("unchecked")
    public Map<T, Serializable> getKeywordParameterMap(String name, boolean create) {
        Object param = getParameter(name);
        if (param == null) {
            if (!create)
                return null;
            HashMap<T, Serializable> keywordParams = new HashMap<T, Serializable>();
            setParameter(name, keywordParams);
            return keywordParams;
        } else if (param instanceof Map) {
            return (Map)param; // This cast IS unchecked, but the setter cannot put map as parameter
        } else {
            throw new IllegalArgumentException("Global parameter accessed as keyword parameter");
        }
        
    }

    /**
     * Returns an additional parameter with the given {@code name} for the given {@code keyword}.
     * @param name the name of the additional parameter to get
     * @param keyword the keyword for which to get the additional parameter
     * @return the value of the keyword parameter {@code name} or <tt>null</tt> if it is not set
     */
    public Serializable getKeywordParameter(String name, T keyword) {
        Map<T, Serializable> param = getKeywordParameterMap(name, false);
        return param == null ? null : param.get(keyword);
    }
    
    /**
     * Set the parameter {@code name} of the given {@code keyword} to the given {@code value}.
     * Note that the previous value is <em>replaced</em> with the new one.
     * @param name the parameter name to set
     * @param keyword the parameter keyword to set
     * @param value the new value for the parameter
     * @return the previous value of the parameter or <tt>null</tt> if it was not set
     */
    public Serializable setKeywordParameter(String name, T keyword, Serializable value) {
        Map<T, Serializable> keywordParams = getKeywordParameterMap(name, true);
        return keywordParams.put(keyword, value);
    }

    /**
     * Add a new parameter {@code name} of the given {@code keyword} with the given {@code value}.
     * If the parameter with the given {@code name} already exists for the given {@code keyword},
     * an IllegalArgumentException is thrown.
     * @param name the parameter name to set
     * @param keyword the parameter keyword to set
     * @param value the new value for the parameter
     * @throws IllegalArgumentException if the parameter with the given {@code name} already exists
     */
    public void addKeywordParameter(String name, T keyword, Serializable value) {
        Map<T, Serializable> keywordParams = getKeywordParameterMap(name, true);
        if (keywordParams.containsKey(keyword))
            throw new IllegalArgumentException("Keyword parameter '" + name + "' already exists for keyword " + keyword);
        keywordParams.put(keyword, value);
    }

    /**
     * Removes the parameter {@code name} of the given {@code keyword}.
     * @param name the parameter name to remove
     * @param keyword the parameter keyword to remove
     * @return the previous value of the parameter or <tt>null</tt> if it was not set
     */
    public Serializable removeKeywordParameter(String name, T keyword) {
        Map<T, Serializable> keywordParams = getKeywordParameterMap(name, false);
        if (keywordParams == null)
            return null;
        return keywordParams.remove(keyword);
    }

}
