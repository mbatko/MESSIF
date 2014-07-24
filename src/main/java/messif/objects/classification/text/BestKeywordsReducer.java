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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import messif.objects.classification.ClassificationException;
import messif.utility.Parametric;

/**
 * Reduces the given classification by selecting the best keywords.
 * Best keywords are selected either by frequency, best confidence, or boosted scores.
 * 
 * @param <C> the class of the classification categories
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 */
public class BestKeywordsReducer<C> implements KeywordClassifier<C,C> {
    /** Specifies the method that provides the values */
    public static enum BestKeywordValueSelector {
        /** Keyword quality is defined by frequency */
        FREQUENCY,
        /** Keyword quality is defined by maximal confidence */
        CONFIDENCE,
        /** Keyword quality is defined by {@link KeywordClassification#getKeywordBoostedScore boosted keyword score} */
        BOOSTED_SCORE;

        /**
         * Returns the selector value for the given keyword in the given classification
         * @param <C> the type of categories the classification contains
         * @param classification the keyword classification to use for computing
         * @param keyword the keyword to get the value for
         * @return the keyword selector value
         */
        private <C> float getValue(KeywordClassification<C> classification, C keyword) {
            switch (this) {
                case FREQUENCY:
                    return classification.getAllConfidenceCount(keyword); // Higher frequency means better, thus smaller (so it is sorted at the beginning)
                case CONFIDENCE:
                    if (classification.getLowestConfidence() <= classification.getHighestConfidence())
                        return -classification.getConfidence(keyword);
                    else
                        return classification.getConfidence(keyword);
                case BOOSTED_SCORE:
                    return classification.getKeywordBoostedScore(keyword); // Higher score means better, thus smaller (so it is sorted at the beginning)
                default:
                    throw new InternalError("Cannot get value for " + this);
            }
        }
        /**
         * Returns the ordering multiplier (-1 or 1) according to the type.
         * @param classification the classification to use
         * @return the ordering multiplier
         */
        private byte getOrderMult(KeywordClassification<?> classification) {
            switch (this) {
                case FREQUENCY:
                    return -1; // Higher frequency means better, thus smaller (so it is sorted at the beginning)
                case CONFIDENCE:
                    return classification.getLowestConfidence() <= classification.getHighestConfidence() ? (byte)-1 : (byte)1;
                case BOOSTED_SCORE:
                    return -1; // Higher score means better, thus smaller (so it is sorted at the beginning)
                default:
                    throw new InternalError("Cannot get value for " + this);
            }
        }
    }

    /** Class of the classification categories */
    private final Class<? extends C> categoriesClass;
    /** Method for deciding the keyword quality */
    private final BestKeywordValueSelector selector;
    /** Number of best keywords to select */
    private final int bestCount;

    /**
     * Creates a new reducer for selecting best keywords.
     * @param categoriesClass the class of the classification categories
     * @param selector the method for deciding the keyword quality
     * @param bestCount the number of keywords to select
     */
    public BestKeywordsReducer(Class<? extends C> categoriesClass, BestKeywordValueSelector selector, int bestCount) {
        this.categoriesClass = categoriesClass;
        this.selector = selector;
        this.bestCount = bestCount;
    }

    @Override
    public Class<? extends C> getInputCategoriesClass() {
        return categoriesClass;
    }

    @Override
    public Class<? extends C> getCategoriesClass() {
        return categoriesClass;
    }

    private class Pair implements Comparable<Pair> {
        private final C keyword;
        private final float value;
        private final byte order;
        private Pair(C keyword, float value, byte order) {
            this.keyword = keyword;
            this.value = value;
            this.order = order;
        }
        @Override
        public int compareTo(Pair o) {
            return order * Float.compare(value, o.value);
        }
    }

    @Override
    public KeywordClassification<C> classify(KeywordClassification<C> inputClassification, Parametric parameters) throws ClassificationException {
        List<Pair> sortedKeywords = new ArrayList<Pair>();
        byte order = selector.getOrderMult(inputClassification);
        for (C keyword : inputClassification) {
            sortedKeywords.add(new Pair(keyword, selector.getValue(inputClassification, keyword), order));
        }
        Collections.sort(sortedKeywords);

        // Put all data into the classification
        KeywordClassification<C> ret = new KeywordClassification<C>(inputClassification);
        Iterator<Pair> it = sortedKeywords.iterator();
        for (int i = 0; it.hasNext() && i < bestCount; i++) {
            Pair sortedKeyword = it.next();
            ret.add(sortedKeyword.keyword, sortedKeyword.value);
        }
        return ret;
    }
}
