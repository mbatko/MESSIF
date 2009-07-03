/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 * @author xbatko
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
                    replace("*", ".*?").
                    replace("?", ".").
                    replace("\\[", "[").
                    replace("[!", "[^"),
                // Make the pattern case insensitive on Windows
                (File.separatorChar == '/')?0:Pattern.CASE_INSENSITIVE
        );
    }

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
