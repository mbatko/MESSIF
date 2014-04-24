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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import messif.buckets.BucketStorageException;
import messif.buckets.index.LocalAbstractObjectOrder;
import messif.buckets.storage.IntStorageIndexed;
import messif.buckets.storage.IntStorageSearch;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;

/**
 * Various utility methods for text conversion.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class TextConversion {
    /** Pattern used to split words in a string */
    public static final String TEXT_SPLIT_REGEXP = "[^\\p{javaLowerCase}\\p{javaUpperCase}0-9]+";
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
     * Finds all occurrences of the given pattern in the {@code string} and
     * replace it with the replacement string according to the matched group.
     * @param string the string to apply the find-and-replace on
     * @param findPattern the pattern to find, must have the same number of
     *          groups as the replacement array
     * @param replacement the list of replacement strings
     * @return the modified string
     */
    public static String findReplace(CharSequence string, Pattern findPattern, String... replacement) {
        if (string == null)
            return null;
        Matcher matcher = findPattern.matcher(string);
        StringBuffer str = new StringBuffer();
        while (matcher.find())
            matcher.appendReplacement(str, replacement[matcherGroupMatched(matcher) - 1]);
        matcher.appendTail(str);
        return str.toString();
    }

    /**
     * Returns the string created by joining the {@code words} using the given {@code separator}.
     * @param words the strings to join
     * @param separator the separator to join the strings with
     * @return the joined string
     */
    public static String join(String[] words, String separator) {
        if (words == null)
            return null;
        if (words.length == 0)
            return "";
        StringBuilder str = new StringBuilder(words[0]);
        for (int i = 1; i < words.length; i++)
            str.append(separator).append(words[i]);
        return str.toString();
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
        return findReplace(Normalizer.normalize(string.toLowerCase(), Form.NFD), NORMALIZER_REPLACE_PATTERN, NORMALIZER_REPLACE_STRINGS);
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
     * The string is {@link #normalizeString(java.lang.String) normalized} and
     * then split into separate words by any sequence of non-alphanumeric characters.
     *
     * @param string the string with the words to normalized and split
     * @return words from the split string
     */
    public static String[] normalizeAndSplitString(String string) {
        return normalizeAndSplitString(string, TEXT_SPLIT_REGEXP);
    }

    /**
     * Creates a list of words that match the given regular expression.
     * @param words the source words to match
     * @param pattern the regular expression matching valid words
     * @return a list of matching words
     */
    public static List<String> matchingWords(Collection<String> words, Pattern pattern) {
        List<String> ret = new ArrayList<String>(words.size());
        for (String word : words) {
            if (pattern.matcher(word).matches())
                ret.add(word);
        }
        return ret;
    }

    /**
     * Return a collection of stemmed, non-ignored words.
     * Note that the ignore words are updated whenever a non-ignored word is found,
     * thus if {@code ignoreWords} is not <tt>null</tt> the resulting collection
     * does not contain duplicate words.
     * @param keyWords the list of keywords to transform
     * @param ignoreWords set of words to ignore (e.g. the previously added keywords);
     *          if <tt>null</tt>, all keywords are added
     * @param stopWords set of words to ignore but not update
     * @param stemmer a {@link Stemmer} for word transformation
     * @param normalize if <tt>true</tt>, each keyword is first {@link #normalizeString(java.lang.String) normalized}
     * @return a set of stemmed, non-duplicate, non-ignored words
     * @throws TextConversionException if there was an error stemming the word
     */
    public static Collection<String> unifyWords(String[] keyWords, Set<String> ignoreWords, Set<String> stopWords, Stemmer stemmer, boolean normalize) throws TextConversionException {
        if (keyWords == null)
            return Collections.emptyList();

        Collection<String> processedKeyWords = new ArrayList<String>(keyWords.length);
        for (String keyWord : keyWords) {
            String keyWordUnified = unifyWord(keyWord, ignoreWords, stopWords, stemmer, normalize);
            if (keyWordUnified != null)
                processedKeyWords.add(keyWordUnified);
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
     * @param stopWords set of words to ignore but not update
     * @param stemmer a {@link Stemmer} for word transformation
     * @param normalize if <tt>true</tt>, the keyword is first {@link #normalizeString(java.lang.String) normalized}
     * @return a stemmed, non-ignored word or <tt>null</tt>
     * @throws TextConversionException if there was an error stemming the word
     */
    public static String unifyWord(String keyWord, Set<String> ignoreWords, Set<String> stopWords, Stemmer stemmer, boolean normalize) throws TextConversionException {
        keyWord = keyWord.trim();
        if (normalize)
            keyWord = normalizeString(keyWord);
        if (keyWord.isEmpty())
            return null;
        // Perform stemming
        if (stemmer != null)
            keyWord = stemmer.stem(keyWord);
        if (stopWords != null && stopWords.contains(keyWord))
            return null;
        // Check if not ignored
        if (ignoreWords != null && !ignoreWords.add(keyWord))
            return null;
        return keyWord;
    }

    /**
     * Processes the given collection of words by stemming.
     * @param words the words to process
     * @param stemmer the stemmer to use
     * @return a set of stemmed words
     * @throws TextConversionException if a stemming error occurred
     */
    public static Set<String> stemWords(Collection<String> words, Stemmer stemmer) throws TextConversionException {
        if (words == null || words.isEmpty())
            return Collections.emptySet();
        Set<String> ret = new HashSet<String>();
        for (String word : words) {
            ret.add(stemmer.stem(word));
        }
        return ret;
    }

    /**
     * Processes the given collection of words by stemming.
     * @param words the words to process
     * @param stemmer the stemmer to use
     * @return a set of stemmed words
     * @throws TextConversionException if a stemming error occurred
     */
    public static Set<String> stemWords(Collection<String> words, Stemmer stemmer) throws TextConversionException {
        if (words == null || words.isEmpty())
            return Collections.emptySet();
        Set<String> ret = new HashSet<String>();
        for (String word : words) {
            ret.add(stemmer.stem(word));
        }
        return ret;
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
     * Transforms a list of words into array of addresses by reading the given word index.
     * This method searched the words as provided and does not modify the word index in any way.
     * The words found in the index are removed from the given collection and their
     * identifiers are added to the given {@code identifiers} array starting at {@code index}.
     *
     * @param words the words to transform
     * @param wordIndex the index for translating words to addresses
     * @param identifiers the destination array where the word identifiers will be put
     * @param index the starting index of the {@code identifiers} array where the identifiers will be put
     * @return index of the index of the {@code identifiers} array where the next identifier should be put;
     *      it is equal to the length of the identifiers array if and only if all the words were processed and
     *      the {@code words} array is empty
     * @throws TextConversionException if there was an error reading the index
     */
    public static int wordsToIdentifiersRead(Collection<String> words, IntStorageIndexed<String> wordIndex, int[] identifiers, int index) throws TextConversionException {
        if (words.isEmpty())
            return index;
        try {
            IntStorageSearch<String> search = wordIndex.search(LocalAbstractObjectOrder.trivialObjectComparator, words);
            while (search.next()) {
                while (words.remove(search.getCurrentObject())) {
                    identifiers[index] = search.getCurrentObjectIntAddress();
                    index++;
                }
            }
            search.close();
            return index;
        } catch (IllegalStateException e) {
            throw new TextConversionException(e.getMessage(), e.getCause());
        }
    }

    /**
     * Transforms a list of words into array of addresses by storing the words
     * into the given word index and retrieving the generated identifiers.
     * Note that all words in the given collection are added to the storage.
     * If there is an exception during the storing, it is logged but the process
     * is not interrupted.
     *
     * @param words the words to transform
     * @param wordIndex the index for translating words to addresses
     * @param identifiers the destination array where the word identifiers will be put
     * @param index the starting index of the {@code identifiers} array where the identifiers will be put
     * @return index of the index of the {@code identifiers} array where the next identifier should be put;
     *      it is equal to the length of the identifiers array if and only if all the words were processed and
     *      the {@code words} array is empty
     * @throws TextConversionException if there was an error reading the index
     */
    public static int wordsToIdentifiersStore(Collection<String> words, IntStorageIndexed<String> wordIndex, int[] identifiers, int index) throws TextConversionException {
        for (String word : words) {
            try {
                identifiers[index] = wordIndex.store(word).getAddress();
                index++;
            } catch (BucketStorageException e) {
                Logger.getLogger(TextConversion.class.getName()).log(Level.WARNING, "Cannot insert ''{0}'': {1}", new Object[]{word, e.toString()});
            }
        }
        return index;
    }

    /**
     * Transforms a list of words into array of addresses.
     * Note that unknown words are added to the index.
     * The identifiers in the returned array need not correspond to the given words.
     *
     * @param words the list of words to transform
     * @param ignoreWords set of words to ignore (e.g. the previously added keywords);
     *          if <tt>null</tt>, all keywords are added
     * @param stopWords set of words to ignore but not update
     * @param expander instance for expanding the list of words
     * @param stemmer a {@link Stemmer} for word transformation
     * @param wordIndex the index for translating words to addresses
     * @param normalize if <tt>true</tt>, each word is first {@link #normalizeString(java.lang.String) normalized}
     * @return array of translated addresses
     * @throws TextConversionException if there was an error stemming the word or reading the index
     */
    public static int[] wordsToIdentifiers(String[] words, Set<String> ignoreWords, Set<String> stopWords, WordExpander expander, Stemmer stemmer, IntStorageIndexed<String> wordIndex, boolean normalize) throws TextConversionException {
        if (words == null)
            return new int[0];

        // Expand words
        if (expander != null)
            words = expander.expandWords(words);

        // Convert array to a set, ignoring words from ignoreWords (e.g. words added by previous call)
        Collection<String> processedKeyWords = unifyWords(words, ignoreWords, stopWords, stemmer, normalize);

        // If the keywords list is empty after ignored words, return
        if (processedKeyWords.isEmpty())
            return new int[0];

        int[] ret = new int[processedKeyWords.size()];
        // Search the index
        int retIndex = wordsToIdentifiersRead(processedKeyWords, wordIndex, ret, 0);
        // Store the remaining words into the index
        retIndex = wordsToIdentifiersStore(processedKeyWords, wordIndex, ret, retIndex);
        // Resize the array if some keywords could not be added to the database
        return retIndex == ret.length ? ret : Arrays.copyOf(ret, retIndex);
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
     * @param stopWords set of words to ignore but not update
     * @param stemmer a {@link Stemmer} for word transformation
     * @param wordIndex the index for translating words to addresses
     * @return array of translated addresses
     * @throws IllegalStateException if there was a problem reading the index
     * @throws TextConversionException if there was an error stemming the word
     */
    public static int[] textToWordIdentifiers(String string, String stringSplitRegexp, Set<String> ignoreWords, Set<String> stopWords, Stemmer stemmer, IntStorageIndexed<String> wordIndex) throws TextConversionException {
        return textToWordIdentifiers(string, stringSplitRegexp, ignoreWords, stopWords, null, stemmer, wordIndex);
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
     * @param stopWords set of words to ignore but not update
     * @param expander instance for expanding the list of words
     * @param stemmer a {@link Stemmer} for word transformation
     * @param wordIndex the index for translating words to addresses
     * @return array of translated addresses
     * @throws IllegalStateException if there was a problem reading the index
     * @throws TextConversionException if there was an error expanding or stemming the words
     */
    public static int[] textToWordIdentifiers(String string, String stringSplitRegexp, Set<String> ignoreWords, Set<String> stopWords, WordExpander expander, Stemmer stemmer, IntStorageIndexed<String> wordIndex) throws TextConversionException {
        return wordsToIdentifiers(normalizeAndSplitString(string, stringSplitRegexp), ignoreWords, stopWords, expander, stemmer, wordIndex, false);
    }

    /**
     * Load a set of words from a given table.
     * @param dbConnUrl the database JDBC connection URL
     * @param tableName the table name to get the words from
     * @param columnName the table column name to get the words from
     * @return a set of words from a given table
     * @throws SQLException if there was a problem communicating with the database
     */
    public static Set<String> loadDatabaseWords(String dbConnUrl, String tableName, String columnName) throws SQLException {
        if (dbConnUrl == null)
            return null;
        Connection connection = DriverManager.getConnection(dbConnUrl);
        try {
            PreparedStatement stm = connection.prepareStatement("select " + columnName + " from " + tableName);
            ResultSet rs = stm.executeQuery();
            try {
                Set<String> words = new HashSet<String>();
                while (rs.next()) {
                    words.add(rs.getString(1));
                }
                return words;
            } finally {
                stm.close();
            }
        } finally {
            connection.close();
        }
    }

    /**
     * Transforms a string of words into array of addresses.
     * The string is {@link #normalizeAndSplitString(java.lang.String, java.lang.String) normalized and split}
     * first, then the readonly word indexes are sequentially used to transform the words to identifiers.
     * The remaining words are stemmed and read from the writable word index and if not found
     * the are inserted.
     *
     * @param string the string with the words to transform
     * @param stringSplitRegexp the regular expression used to split the string into words
     * @param ignoreWords set of words to ignore (e.g. the previously added keywords);
     *          if <tt>null</tt>, all keywords are added
     * @param stopWords set of words to ignore but not update
     * @param stemmer a {@link Stemmer} for word transformation
     * @param writableWordIndex the index for translating words to addresses where the unknown words can be inserted
     * @param readonlyWordIndexes the indexes for translating words to addresses
     * @return array of translated addresses
     * @throws IllegalStateException if there was a problem reading the index
     * @throws TextConversionException if there was an error expanding or stemming the words
     */
    public static int[] textToWordIdentifiersMultiIndex(String string, String stringSplitRegexp, Set<String> ignoreWords, Set<String> stopWords, Stemmer stemmer, IntStorageIndexed<String> writableWordIndex, IntStorageIndexed<String>[] readonlyWordIndexes) throws TextConversionException {
        Collection<String> words = TextConversion.unifyWords(TextConversion.normalizeAndSplitString(string, stringSplitRegexp), ignoreWords, stopWords, null, false);
        int[] data = new int[words.size()];
        int index = 0;
        for (IntStorageIndexed<String> wordIndex : readonlyWordIndexes) {
            index = wordsToIdentifiersRead(words, wordIndex, data, index);
        }
        // Perform stemming for the read/write index
        words = TextConversion.stemWords(words, stemmer);
        index = wordsToIdentifiersRead(words, writableWordIndex, data, index);
        index = wordsToIdentifiersStore(words, writableWordIndex, data, index);
        if (index != data.length) // Resize the array if some keywords were not stored
            data = Arrays.copyOf(data, index);
        return data;
    }

    /**
     * Transforms multiple strings of words into multi-array of addresses.
     * Each of the given strings is converted using
     * {@link #textToWordIdentifiersMultiIndex(java.lang.String, java.lang.String, java.util.Set, messif.objects.text.Stemmer, messif.buckets.storage.IntStorageIndexed, messif.buckets.storage.IntStorageIndexed...) textToWordIdentifiersMultiIndex}
     * method.
     *
     * @param strings the multiple strings of words to transform
     * @param stringSplitRegexp the regular expression used to split the string into words
     * @param stopWords set of words to ignore but not update
     * @param stemmer a {@link Stemmer} for word transformation
     * @param writableWordIndex the index for translating words to addresses where the unknown words can be inserted
     * @param readonlyWordIndexes the indexes for translating words to addresses
     * @return array of translated addresses
     * @throws IllegalStateException if there was a problem reading the index
     * @throws TextConversionException if there was an error expanding or stemming the words
     */
    public static int[][] textsToWordIdentifiersMultiIndex(String[] strings, String stringSplitRegexp, Set<String> stopWords, Stemmer stemmer, IntStorageIndexed<String> writableWordIndex, IntStorageIndexed<String>[] readonlyWordIndexes) throws TextConversionException {
        int[][] data = new int[strings.length][];
        Set<String> ignoreWords = new HashSet<String>();
        for (int i = 0; i < strings.length; i++) {
            data[i] = textToWordIdentifiersMultiIndex(strings[i], stringSplitRegexp, ignoreWords, stopWords, stemmer, writableWordIndex, readonlyWordIndexes);
        }
        return data;
    }

    /**
     * Transforms a string of words into array of addresses.
     * The string is {@link #normalizeAndSplitString(java.lang.String, java.lang.String) normalized and split}
     * first, then the readonly word indexes are sequentially used to transform the words to identifiers.
     * The remaining words are stemmed and read from the writable word index and if not found
     * the are inserted.
     *
     * @param string the string with the words to transform
     * @param stringSplitRegexp the regular expression used to split the string into words
     * @param ignoreWords set of words to ignore (e.g. the previously added keywords);
     *          if <tt>null</tt>, all keywords are added
     * @param stemmer a {@link Stemmer} for word transformation
     * @param writableWordIndex the index for translating words to addresses where the unknown words can be inserted
     * @param readonlyWordIndexes the indexes for translating words to addresses
     * @return array of translated addresses
     * @throws IllegalStateException if there was a problem reading the index
     * @throws TextConversionException if there was an error expanding or stemming the words
     */
    public static int[] textToWordIdentifiersMultiIndex(String string, String stringSplitRegexp, Set<String> ignoreWords, Stemmer stemmer, IntStorageIndexed<String> writableWordIndex, IntStorageIndexed<String>[] readonlyWordIndexes) throws TextConversionException {
        Collection<String> words = TextConversion.unifyWords(TextConversion.normalizeAndSplitString(string, stringSplitRegexp), ignoreWords, null, false);
        int[] data = new int[words.size()];
        int index = 0;
        for (IntStorageIndexed<String> wordIndex : readonlyWordIndexes) {
            index = wordsToIdentifiersRead(words, wordIndex, data, index);
        }
        // Perform stemming for the read/write index
        words = TextConversion.stemWords(words, stemmer);
        index = wordsToIdentifiersRead(words, writableWordIndex, data, index);
        index = wordsToIdentifiersStore(words, writableWordIndex, data, index);
        if (index != data.length) // Resize the array if some keywords were not stored
            data = Arrays.copyOf(data, index);
        return data;
    }

    /**
     * Transforms multiple strings of words into multi-array of addresses.
     * Each of the given strings is converted using
     * {@link #textToWordIdentifiersMultiIndex(java.lang.String, java.lang.String, java.util.Set, messif.objects.text.Stemmer, messif.buckets.storage.IntStorageIndexed, messif.buckets.storage.IntStorageIndexed...) textToWordIdentifiersMultiIndex}
     * method.
     *
     * @param strings the multiple strings of words to transform
     * @param stringSplitRegexp the regular expression used to split the string into words
     * @param stemmer a {@link Stemmer} for word transformation
     * @param writableWordIndex the index for translating words to addresses where the unknown words can be inserted
     * @param readonlyWordIndexes the indexes for translating words to addresses
     * @return array of translated addresses
     * @throws IllegalStateException if there was a problem reading the index
     * @throws TextConversionException if there was an error expanding or stemming the words
     */
    public static int[][] textsToWordIdentifiersMultiIndex(String[] strings, String stringSplitRegexp, Stemmer stemmer, IntStorageIndexed<String> writableWordIndex, IntStorageIndexed<String>[] readonlyWordIndexes) throws TextConversionException {
        int[][] data = new int[strings.length][];
        Set<String> ignoreWords = new HashSet<String>();
        for (int i = 0; i < strings.length; i++) {
            data[i] = textToWordIdentifiersMultiIndex(strings[i], stringSplitRegexp, ignoreWords, stemmer, writableWordIndex, readonlyWordIndexes);
        }
        return data;
    }

    /**
     * Creates a text from all fields of the {@link StringFieldDataProvider}.
     * @param textFieldDataProvider the text field data provider the text of which to combine
     * @param fieldSeparatorString the separator inserted between the text of the respective fields
     * @param addNullFields if <tt>true</tt> an empty string is added for <tt>null</tt> textual fields,
     *          otherwise the <tt>null</tt> fields are skipped
     * @return text from all fields
     */
    public static String getAllFieldsData(StringFieldDataProvider textFieldDataProvider, String fieldSeparatorString, boolean addNullFields) {
        StringBuilder ret = new StringBuilder();
        Iterator<String> fieldNames = textFieldDataProvider.getStringDataFields().iterator();
        boolean isFirst = true;
        while (fieldNames.hasNext()) {
            String fieldData = textFieldDataProvider.getStringData(fieldNames.next());
            if (fieldData == null && !addNullFields)
                continue;
            // Add separator before the field data except for the first
            if (isFirst)
                isFirst = false;
            else
                ret.append(fieldSeparatorString);
            ret.append(fieldData == null ? "" : fieldData);
        }
        return ret.toString();
    }

    /**
     * Converts the given {@link MetaObject} to a {@link StringFieldDataProvider}
     * using the encapsulated objects that implement the {@link StringDataProvider}.
     * Every encapsulated object represents a separate textual field, however,
     * <tt>null</tt> is returned for the encapsulated objects that do not implement
     * the {@link StringDataProvider} interface. The combined text from all fields
     * uses newline as a concatenation separator and the <tt>null</tt> fields
     * are skipped.
     * 
     * @param metaObject the metaobject to convert
     * @return a text field data provider with fields from the encapsulated objects of the given metaobject
     */
    public static StringFieldDataProvider metaobjectToTextProvider(final MetaObject metaObject) {
        if (metaObject instanceof StringFieldDataProvider)
            return (StringFieldDataProvider)metaObject;
        return new StringFieldDataProvider() {
            @Override
            public Collection<String> getStringDataFields() {
                return metaObject.getObjectNames();
            }

            @Override
            public String getStringData(String fieldName) throws IllegalArgumentException {
                LocalAbstractObject fieldObject = metaObject.getObject(fieldName);
                if (fieldObject == null)
                    throw new IllegalArgumentException("Uknown text field: '" + fieldName + "'");
                return fieldObject instanceof StringDataProvider ? ((StringDataProvider)fieldObject).getStringData() : null;
            }

            @Override
            public String getStringData() {
                return getAllFieldsData(this, "\n", false);
            }
        };
    }
}
