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
package messif.utility;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides an implementation of {@link FileFilter} and {@link FilenameFilter}
 * interfaces that can match filenames using a Glob pattern.
 * The pattern can use <tt>*, ?, [abc-z]</tt> placeholders.
 * The directory part of matched files is <em>not checked</em>.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class WildcardFileFilter implements FileFilter, FilenameFilter {

    /** Pattern used to recognize glob chars in file names */
    private static final Pattern filenameGlobPattern = Pattern.compile(
            "(?:^|" + Pattern.quote(File.separator) + ")[^" + Pattern.quote(File.separator) + "]*?[*?\\[]");

    /** Compiled pattern used to match on names */
    private final Pattern pattern;

    /**
     * Creates a new instance of WildcardFileFilter for the given pattern.
     * The pattern can use <pre>*, ?, [abc-z]</pre> placeholders.
     * @param pattern the Glob pattern to use
     * @throws NullPointerException if the <code>pattern</code> was <tt>null</tt>
     */
    public WildcardFileFilter(String pattern) throws NullPointerException {
        this.pattern = Pattern.compile(
                // Convert the Glob pattern to Regexp
                pattern.
                    replace(".", "\\.").
                    replace("?", ".").
                    replace("*", ".*?").
                    replace("\\[", "[").
                    replace("[!", "[^"),
                // Make the pattern case insensitive on Windows
                (File.separatorChar == '/')?0:Pattern.CASE_INSENSITIVE
        );
    }

    /**
     * Returns the position of the first glob char in the given filename.
     * @param name the file name in which to get the glob char
     * @return the position of the first glob char in the given filename or
     *          -1 if the {@code name} does not contain any glob char
     */
    public static int getFilenameGlobPosition(String name) {
        Matcher matcher = filenameGlobPattern.matcher(name);
        if (matcher.find())
            return matcher.start();
        else
            return -1;
    }

    /**
     * Returns if the specified <code>name</code> matches this wildcard.
     * @param name the string to match
     * @return <tt>true</tt> if the name matches
     */
    public boolean match(String name) {
        return pattern.matcher(name).matches();
    }

    public boolean accept(File pathname) {
        return match(pathname.getName());
    }

    public boolean accept(File dir, String name) {
        return match(name);
    }

    @Override
    public String toString() {
        return pattern.pattern();
    }

}
