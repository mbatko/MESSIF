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
package messif.objects.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import messif.buckets.storage.IntStorageIndexed;
import messif.objects.LocalAbstractObject;
import messif.objects.text.Stemmer;
import messif.objects.text.TextConversion;
import messif.objects.text.TextConversionException;
import messif.utility.Convert;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * Implementation of the CoPhIR XML file parsing.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class CophirXmlParser extends DefaultHandler {
    //****************** Constants ******************//

    /** Name of the tag that encapsulates all MPEG7 visual descriptors */
    protected static final String descriptorTagName = "VisualDescriptor";
    /** Name of attribute of the {@link #descriptorTagName} tag that contains the descriptor name */
    protected static final String descriptorTypeAttributeName = "type";
    /** Names of tags that contain words for keyword data */
    protected static final List<String> wordTagNames = Arrays.asList("title", "tag", "description");
    /** Name of the key words object */
    protected static final String wordObjectName = "KeyWordsType";
    /** Regular expression used to split the tag text into words */
    protected static final String TEXT_SPLIT_REGEXP = "[^\\p{javaLowerCase}\\p{javaUpperCase}]+";


    //****************** Attributes ******************//

    /** List of parsed local abstract objects */
    private final Map<String, LocalAbstractObject> objects = new HashMap<String, LocalAbstractObject>();
    /** Parsed locator URI */
    private String locatorURI;
    /** {@link Stemmer} for word transformation */
    private final Stemmer stemmer;
    /** Index for translating words to addresses */
    private final IntStorageIndexed<String> wordIndex;


    //****************** Constructors ******************//

    /**
     * Creates a new handler for parsing CoPhIR XML files.
     * The key words descriptor is not extracted.
     */
    public CophirXmlParser() {
        this(null, null);
    }

    /**
     * Creates a new handler for parsing CoPhIR XML files.
     * The key words descriptor is created using the given stemmer and word index.
     * @param stemmer a {@link Stemmer} for word transformation
     * @param wordIndex the index for translating words to addresses
     *          (if <tt>null</tt> the key words descriptor is not created)
     */
    public CophirXmlParser(Stemmer stemmer, IntStorageIndexed<String> wordIndex) {
        this.stemmer = stemmer;
        this.wordIndex = wordIndex;
    }


    //****************** Factory method ******************//

    /**
     * Factory method that parses the given CoPhIR XML file.
     * @param file the CoPhIR XML file to read the object from
     * @param stemmer a {@link Stemmer} for word transformation
     * @param wordIndex the index for translating words to addresses
     *          (if <tt>null</tt> the key words descriptor is not created)
     * @return the parsed representation of the CoPhIR XML file
     * @throws ParserConfigurationException if a XML parser cannot be created
     * @throws SAXException if there was an error parsing the XML file
     * @throws IOException if there was an error reading the XML file
     */
    public static CophirXmlParser create(File file, Stemmer stemmer, IntStorageIndexed<String> wordIndex) throws ParserConfigurationException, SAXException, IOException {
        CophirXmlParser xmlHandler = new CophirXmlParser(stemmer, wordIndex);
        SAXParserFactory.newInstance().newSAXParser().parse(file, xmlHandler);
        return xmlHandler;
    }

    /**
     * Factory method that parses a CoPhIR XML file with the given identifier.
     * @param xmlDir the root directory where CoPhIR XML file are stored
     * @param identifier the CoPhIR object identifier to read
     * @param stemmer a {@link Stemmer} for word transformation
     * @param wordIndex the index for translating words to addresses
     *          (if <tt>null</tt> the key words descriptor is not created)
     * @return a new instance of MetaObjectCophirKeywords
     * @throws ParserConfigurationException if a XML parser cannot be created
     * @throws SAXException if there was an error parsing the XML file
     * @throws IOException if there was an error reading the XML file
     */
    public static CophirXmlParser create(File xmlDir, String identifier, Stemmer stemmer, IntStorageIndexed<String> wordIndex) throws ParserConfigurationException, SAXException, IOException {
        return create(new File(xmlDir, idToPath(identifier, "xml")), stemmer, wordIndex);
    }


    //****************** Attribute access methods ******************//

    /**
     * Returns the parsed descriptor objects.
     * @return the parsed descriptor objects
     */
    public Map<String, LocalAbstractObject> getObjects() {
        return Collections.unmodifiableMap(objects);
    }

    /**
     * Returns the number of the parsed descriptor objects.
     * @return the number of the parsed descriptor objects
     */
    public int getObjectCount() {
        return objects.size();
    }

    /**
     * Returns the parsed locator URI.
     * @return the parsed locator URI
     */
    public String getLocatorURI() {
        return locatorURI;
    }

    /**
     * Reset the parsed data to that this handler can be reused in additional parsing.
     */
    public void resetObjects() {
        objects.clear();
        locatorURI = null;

        elementNamesStack.clear();
        descriptorData.clear();
        wordData.clear();
        descriptorName = null;
        wordName = null;
    }


    //****************** Parsing methods ******************//

    /** Actual XML tree tag names stack (used only during parsing) */
    private final Deque<String> elementNamesStack = new ArrayDeque<String>();
    /** Actual name of the parsed visual descriptor (used only during parsing) */
    private String descriptorName = null;
    /** Visual descriptor data parsed from the XML (used only during parsing) */
    private final Map<String, StringBuilder> descriptorData = new HashMap<String, StringBuilder>();
    /** Actual name of the parsed text tag (used only during parsing) */
    private String wordName = null;
    /** Textual data parsed from the XML */
    private final Map<String, StringBuilder> wordData = new HashMap<String, StringBuilder>();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        String name = (localName.length() == 0)?qName:localName;
        elementNamesStack.push(name);

        // If this is a descriptor node and we are not inside one already
        if (descriptorName == null && name.equals(descriptorTagName)) {
            descriptorName = attributes.getValue(descriptorTypeAttributeName);
        } else if (name.equals("location")) { // HACK! for location
            try {
                float latitude = Float.parseFloat(attributes.getValue("latitude"));
                float longitude = Float.parseFloat(attributes.getValue("longitude"));
                objects.put("Location", new ObjectGPSCoordinate(latitude, longitude));
            } catch (Exception e) {
                throw new SAXException("Error parsing descriptor 'Location' at " + elementNamesStack.toString(), e);
            }
        } else if (wordTagNames.contains(name)) {
            wordName = name;
            if (!wordData.containsKey(wordName))
                wordData.put(wordName, new StringBuilder());
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        String name = elementNamesStack.pop();
        if (localName.length() == 0) {
            if (!qName.equals(name))
                throw new SAXException("Closing element '" + qName + "', but should close '" + name + "'");
        } else {
            if (!localName.equals(name))
                throw new SAXException("Closing element '" + localName + "', but should close '" + name + "'");
        }

        // If this is an end of descriptor node, create the descriptor
        if (descriptorName != null && name.equals(descriptorTagName)) { // Ignore additional stack checks: && elementNamesStack.size() >= 1 && elementNamesStack.peek().equals("Image")) {
            try {
                objects.put(descriptorName, createVisualDescriptor(descriptorName, descriptorData));
            } catch (InstantiationException e) {
                throw new SAXException("Error parsing descriptor '" + descriptorName + "' at " + elementNamesStack + ": " + e.getMessage(), e);
            }

            descriptorName = null;
            descriptorData.clear();
        } else if (wordName != null) {
            wordData.get(wordName).append(' ');
            wordName = null;
        }
    }

    @Override
    public void endDocument() throws SAXException {
        try {
            if (wordIndex != null)
                objects.put(wordObjectName, parseKeyWordsType(stemmer, wordIndex));
            wordData.clear();
        } catch (TextConversionException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        // If inside descriptor
        if (descriptorName != null) {
            String tagName = elementNamesStack.peek();
            // Get previous value of this tag
            StringBuilder value = descriptorData.get(tagName);
            // Prepare new value and put it under the name of the current element (append to the previous value)
            if (value == null) {
                value = new StringBuilder();
                descriptorData.put(tagName, value);
            }
            value.append(ch, start, length);
        } else if (wordName != null) {
            wordData.get(wordName).append(ch, start, length);

        // If this is a locator node
        } else if (elementNamesStack.size() >= 3 && elementNamesStack.peek().equals("MediaUri")) {
            locatorURI = new String(ch, start, length);
        }
    }


    //****************** Specific object descriptor creators ******************//

    /** Splitting pattern for the {@link #splitBySpace(java.lang.CharSequence)} method */
    private static final Pattern splitBySpacePattern = Pattern.compile("\\s+");

    /**
     * Parse the visual descriptor data.
     * @param descriptorName the name of the descriptor to create
     * @param data the tag data from the XML (tag name and the text)
     * @return a new instance of the respective descriptor object
     * @throws InstantiationException if there was an error parsing the data 
     */
    private LocalAbstractObject createVisualDescriptor(String descriptorName, Map<String, StringBuilder> data) throws InstantiationException {
        if (descriptorName.equals("ScalableColorType")) {
            return new ObjectIntVectorL1(Convert.stringToArray(data.get("Coeff"), splitBySpacePattern, 0, int[].class, null));
        } else if (descriptorName.equals("ColorStructureType")) {
            return new ObjectShortVectorL1(Convert.stringToArray(data.get("Values"), splitBySpacePattern, 0, short[].class, null));
        } else if (descriptorName.equals("EdgeHistogramType")) {
            return new ObjectVectorEdgecomp(Convert.stringToArray(data.get("BinCounts"), splitBySpacePattern, 0, byte[].class, null));
        } else if (descriptorName.equals("HomogeneousTextureType")) {
            return new ObjectHomogeneousTexture(
                    Short.parseShort(data.get("Average").toString()),
                    Short.parseShort(data.get("StandardDeviation").toString()),
                    Convert.stringToArray(data.get("Energy"), splitBySpacePattern, 0, short[].class, null),
                    Convert.stringToArray(data.get("EnergyDeviation"), splitBySpacePattern, 0, short[].class, null)
            );
        } else if (descriptorName.equals("ColorLayoutType")) {
            return parseColorLayoutType(data);
        } else {
            throw new InstantiationException("Unknown descriptor name: " + descriptorName);
        }
    }

    /**
     * Parse the color layout descriptor data.
     * @param data the tag data from the XML (tag name and the text)
     * @return a new instance of the {@link ObjectColorLayout}
     */
    private ObjectColorLayout parseColorLayoutType(Map<String, StringBuilder> data) {
        // Parse Y coeffs
        String[] numbers = splitBySpacePattern.split(data.get("YACCoeff5"));
        byte[] YCoeff = new byte[numbers.length + 1];
        YCoeff[0] = Byte.parseByte(data.get("YDCCoeff").toString());
        for (int i = 1; i <= numbers.length; i++)
            YCoeff[i] = Byte.parseByte(numbers[i - 1]);
        // Parse Cb coeffs
        numbers = splitBySpacePattern.split(data.get("CbACCoeff2"));
        byte[] CbCoeff = new byte[numbers.length + 1];
        CbCoeff[0] = Byte.parseByte(data.get("CbDCCoeff").toString());
        for (int i = 1; i <= numbers.length; i++)
            CbCoeff[i] = Byte.parseByte(numbers[i - 1]);
        // Parse Cr coeffs
        numbers = splitBySpacePattern.split(data.get("CrACCoeff2"));
        byte[] CrCoeff = new byte[numbers.length + 1];
        CrCoeff[0] = Byte.parseByte(data.get("CrDCCoeff").toString());
        for (int i = 1; i <= numbers.length; i++)
            CrCoeff[i] = Byte.parseByte(numbers[i - 1]);
        return new ObjectColorLayout(YCoeff, CbCoeff, CrCoeff);
    }

    /**
     * Parse the keywords descriptor data.
     * @param stemmer a {@link Stemmer} for word transformation
     * @param wordIndex the index for translating words to addresses
     * @return a new instance of the {@link ObjectIntMultiVectorJaccard}
     * @throws TextConversionException if there was an error stemming the word
     */
    private ObjectIntMultiVectorJaccard parseKeyWordsType(Stemmer stemmer, IntStorageIndexed<String> wordIndex) throws TextConversionException {
        String[] texts = new String[wordTagNames.size()];
        for (int i = 0; i < texts.length; i++) {
            StringBuilder dataString = wordData.get(wordTagNames.get(i));
            texts[i] = dataString == null ? null : dataString.toString();
        }
        int[][] wordIds = new int[texts.length][];
        Set<String> ignoreWords = new HashSet<String>();
        for (int i = 0; i < texts.length; i++) {
            wordIds[i] = TextConversion.textToWordIdentifiers(texts[i], TEXT_SPLIT_REGEXP, ignoreWords, stemmer, wordIndex);
        }
        return new ObjectIntMultiVectorJaccard(wordIds);
    }


    //****************** Cophir utility methods ******************//

    /**
     * Prepends the fileName with its first three chars and second three chars as directories.
     * @param fileName fileName to modify
     */
    protected static void addDirectoryTriples(StringBuilder fileName) {
        int len = fileName.length() - 3;
        int pad = len - 1;

        for (int i = 0; i < 6; i++) {
            if (i % 3 == 0) {
                fileName.insert(0, '/');
                pad++;
            }
            fileName.insert(0, (i < len)?fileName.charAt(pad):'0');
        }
    }

    /**
     * Returns a file path derived from the object identifier.
     * @param id the object identifier
     * @param extension the file extension to add (no extension is added if <tt>null</tt>)
     * @return a path to the file
     */
    public static String idToPath(String id, String extension) {
        // Prepare path buffer
        StringBuilder fileName = new StringBuilder(String.format("%09d", Long.parseLong(id)));

        // Add folder names
        addDirectoryTriples(fileName);

        // Append extension
        if (extension != null)
            fileName.append('.').append(extension);

        return fileName.toString();
    }

    /**
     * Returns an object identifier from a file.
     * @param file the file for which to get the identifier
     * @return an object identifier
     */
    public static String pathToId(File file) {
        return file.getName().replaceFirst(".(jpg|xml)$", "");
    }

    /**
     * Returns an iterator of object identifiers from an iterator of files.
     * @param iterator the iterator of files for which to get the identifier
     * @return an iterator of object identifiers
     */
    public static Iterator<String> pathToId(final Iterator<File> iterator) {
        return new Iterator<String>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }
            @Override
            public String next() {
                return pathToId(iterator.next());
            }
            @Override
            public void remove() {
                iterator.remove();
            }
        };
    }

}
