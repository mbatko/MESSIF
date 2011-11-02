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
package messif.objects.text;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import messif.buckets.BucketStorageException;
import messif.buckets.index.LocalAbstractObjectOrder;
import messif.buckets.storage.IntStorageIndexed;
import messif.buckets.storage.IntStorageSearch;

/**
 * Various utility methods for text conversion.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class TextConversion {
    /** Pattern used to search for normalized strings, each group corresponds to the replacement string in {@link #NORMALIZER_REPLACE_STRINGS} */
    private static final Pattern NORMALIZER_REPLACE_PATTERN = Pattern.compile("(\\p{InCombiningDiacriticalMarks}+)|(ß)|(-)|(\\b[\\p{javaLowerCase}\\p{javaUpperCase}]\\b)|('[\\p{javaLowerCase}\\p{javaUpperCase}]{0,2})");
    /** Replacement strings for the {@link #NORMALIZER_REPLACE_PATTERN} */
    private static final String[] NORMALIZER_REPLACE_STRINGS = new String[] { "", "ss", "", "", "" };


    //****************** Conversion methods ******************//

    /**
     * Returns the group that has matched in the given matcher.
     * Note that the returned value is the number of the pattern group that
     * are counted starting from 1.
     * @param matcher the pattern matcher with a currently matched data
     * @return the group that has matched in the given matcher or zero if no groups are matching
     * @throws IllegalStateException if no match has yet been attempted, or if the previous match operation failed on the given matcher
     */
    private static int matcherGroupMatched(Matcher matcher) {
        for (int i = 1; i <= matcher.groupCount(); i++)
            if (matcher.start(i) != -1)
                return i;
        return 0;
    }

    /**
     * Normalizes the given string by lower-casing, replacing the diacritics characters
     * with their Latin counter-parts, and removing/replacing other unwanted character sequences.
     * @param string the string to normalize
     * @return the normalized string
     * @see #NORMALIZER_REPLACE_PATTERN
     * @see #NORMALIZER_REPLACE_STRINGS
     */
    public static String normalizeString(String string) {
        Matcher matcher = NORMALIZER_REPLACE_PATTERN.matcher(Normalizer.normalize(string.toLowerCase(), Form.NFD));
        StringBuffer str = new StringBuffer();
        while (matcher.find())
            matcher.appendReplacement(str, NORMALIZER_REPLACE_STRINGS[matcherGroupMatched(matcher) - 1]);
        matcher.appendTail(str);
        return str.toString();
    }

    /**
     * The string is {@link #normalizeString(java.lang.String) normalized} and
     * then split into separate words by the given {@code stringSplitRegexp}.
     *
     * @param string the string with the words to normalized and split
     * @param stringSplitRegexp the regular expression used to split the string into words
     * @return words from the split string
     */
    public static String[] normalizeAndSplitString(String string, String stringSplitRegexp) {
        return string == null ? null : normalizeString(string).split(stringSplitRegexp);
    }

    /**
     * Return a collection of stemmed, non-ignored words.
     * Note that the ignore words are updated whenever a non-ignored word is found,
     * thus if {@code ignoreWords} is not <tt>null</tt> the resulting collection
     * does not contain duplicate words.
     * @param keyWords the list of keywords to transform
     * @param ignoreWords set of words to ignore (e.g. the previously added keywords);
     *          if <tt>null</tt>, all keywords are added
     * @param stemmer a {@link Stemmer} for word transformation
     * @param normalize if <tt>true</tt>, each keyword is first {@link #normalizeString(java.lang.String) normalized}
     * @return a set of stemmed, non-duplicate, non-ignored words
     * @throws TextConversionException if there was an error stemming the word
     */
    public static Collection<String> unifyWords(String[] keyWords, Set<String> ignoreWords, Stemmer stemmer, boolean normalize) throws TextConversionException {
        Collection<String> processedKeyWords = new ArrayList<String>(keyWords.length);
        for (int i = 0; i < keyWords.length; i++) {
            String keyWord = unifyWord(keyWords[i], ignoreWords, stemmer, normalize);
            if (keyWord != null)
                processedKeyWords.add(keyWord);
        }
        return processedKeyWords;
    }

    /**
     * Return a stemmed, non-ignored word.
     * Note that the ignore words are updated whenever a non-ignored word is found,
     * thus if {@code ignoreWords} is not <tt>null</tt> the resulting collection
     * does not contain duplicate words.
     * @param keyWord the keyword to transform
     * @param ignoreWords set of words to ignore (e.g. the previously added keywords);
     *          if <tt>null</tt>, all keywords are added
     * @param stemmer a {@link Stemmer} for word transformation
     * @param normalize if <tt>true</tt>, the keyword is first {@link #normalizeString(java.lang.String) normalized}
     * @return a stemmed, non-ignored word or <tt>null</tt>
     * @throws TextConversionException if there was an error stemming the word
     */
    public static String unifyWord(String keyWord, Set<String> ignoreWords, Stemmer stemmer, boolean normalize) throws TextConversionException {
        keyWord = keyWord.trim();
        if (normalize)
            keyWord = normalizeString(keyWord);
        if (keyWord.isEmpty())
            return null;
        // Perform stemming
        if (stemmer != null)
            keyWord = stemmer.stem(keyWord);
        // Check if not ignored
        if (ignoreWords != null && !ignoreWords.add(keyWord))
            return null;
        return keyWord;
    }

    /**
     * Convert the given array of word identifiers to words using the given storage.
     * @param wordIndex the index used to transform the integers to words
     * @param ids the array of integers to convert
     * @return an array of converted words
     * @throws TextConversionException if there was an error reading a word with a given identifier from the index
     */
    public static String[] identifiersToWords(IntStorageIndexed<String> wordIndex, int[] ids) throws TextConversionException {
        if (ids == null)
            return null;
        try {
            String[] ret = new String[ids.length];
            for (int i = 0; i < ids.length; i++)
                ret[i] = wordIndex.read(ids[i]);
            return ret;
        } catch (BucketStorageException e) {
            throw new TextConversionException("Cannot transform word identifiers to string: " + e.getMessage(), e);
        }
    }

    /**
     * Transforms a list of words into array of addresses.
     * Note that unknown words are added to the index.
     *
     * @param words the list of words to transform
     * @param ignoreWords set of words to ignore (e.g. the previously added keywords);
     *          if <tt>null</tt>, all keywords are added
     * @param stemmer a {@link Stemmer} for word transformation
     * @param wordIndex the index for translating words to addresses
     * @param normalize if <tt>true</tt>, each word is first {@link #normalizeString(java.lang.String) normalized}
     * @return array of translated addresses
     * @throws TextConversionException if there was an error stemming the word or reading the index
     */
    public static int[] wordsToIdentifiers(String[] words, Set<String> ignoreWords, Stemmer stemmer, IntStorageIndexed<String> wordIndex, boolean normalize) throws TextConversionException {
        if (words == null)
            return new int[0];

        // Convert array to a set, ignoring words from ignoreWords (e.g. words added by previous call)
        Collection<String> processedKeyWords = unifyWords(words, ignoreWords, stemmer, normalize);

        // If the keywords list is empty after ignored words, return
        if (processedKeyWords.isEmpty())
            return new int[0];

        // Search the index
        int retIndex = 0;
        int[] ret = new int[processedKeyWords.size()];
        try {
            IntStorageSearch<String> search = wordIndex.search(LocalAbstractObjectOrder.trivialObjectComparator, processedKeyWords);
            while (search.next()) {
                while (processedKeyWords.remove(search.getCurrentObject())) {
                    ret[retIndex++] = search.getCurrentObjectIntAddress();
                }
            }
            search.close();
        } catch (IllegalStateException e) {
            throw new TextConversionException(e.getMessage(), e.getCause());
        }

        // Add all missing keywords
        for (Iterator<String> it = processedKeyWords.iterator(); it.hasNext();) {
            String keyWord = it.next();
            try {
                ret[retIndex] = wordIndex.store(keyWord).getAddress();
                retIndex++;
            } catch (BucketStorageException e) {
                Logger.getLogger(TextConversion.class.getName()).log(Level.WARNING, "Cannot insert ''{0}'': {1}", new Object[]{keyWord, e.toString()});
            }
        }

        // Resize the array if some keywords could not be added to the database
        if (retIndex != ret.length) {
            int[] saved = ret;
            ret = new int[retIndex];
            System.arraycopy(saved, 0, ret, 0, retIndex);
        }

        return ret;
    }

    /**
     * Transforms a string of words into array of addresses.
     * The string is {@link #normalizeAndSplitString(java.lang.String, java.lang.String) normalized and split}
     * first, then the words are {@link #wordsToIdentifiers converted to identifiers}.
     * Note that unknown words are added to the index.
     *
     * @param string the string with the words to transform
     * @param stringSplitRegexp the regular expression used to split the string into words
     * @param ignoreWords set of words to ignore (e.g. the previously added keywords);
     *          if <tt>null</tt>, all keywords are added
     * @param stemmer a {@link Stemmer} for word transformation
     * @param wordIndex the index for translating words to addresses
     * @return array of translated addresses
     * @throws IllegalStateException if there was a problem reading the index
     * @throws TextConversionException if there was an error stemming the word
     */
    public static int[] textToWordIdentifiers(String string, String stringSplitRegexp, Set<String> ignoreWords, Stemmer stemmer, IntStorageIndexed<String> wordIndex) throws TextConversionException {
        return textToWordIdentifiers(string, stringSplitRegexp, ignoreWords, null, stemmer, wordIndex);
    }

    /**
     * Transforms a string of words into array of addresses.
     * The string is {@link #normalizeAndSplitString(java.lang.String, java.lang.String) normalized and split}
     * first, then the words are {@link WordExpander#expandWords(java.lang.String[]) expanded},
     * and finally all the expanded words are {@link #wordsToIdentifiers converted to identifiers}.
     * Note that unknown words are added to the index.
     *
     * @param string the string with the words to transform
     * @param stringSplitRegexp the regular expression used to split the string into words
     * @param ignoreWords set of words to ignore (e.g. the previously added keywords);
     *          if <tt>null</tt>, all keywords are added
     * @param expander instance for expanding the list of words
     * @param stemmer a {@link Stemmer} for word transformation
     * @param wordIndex the index for translating words to addresses
     * @return array of translated addresses
     * @throws IllegalStateException if there was a problem reading the index
     * @throws TextConversionException if there was an error expanding or stemming the words
     */
    public static int[] textToWordIdentifiers(String string, String stringSplitRegexp, Set<String> ignoreWords, WordExpander expander, Stemmer stemmer, IntStorageIndexed<String> wordIndex) throws TextConversionException {
        // Normalize and spit the string
        String[] words = normalizeAndSplitString(string, stringSplitRegexp);

        // Expand words
        if (expander != null)
            words = expander.expandWords(words);

        return wordsToIdentifiers(words, ignoreWords, stemmer, wordIndex, false);
    }

}
