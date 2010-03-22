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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

/**
 * This class provides a continous {@link InputStream} over all
 * files from a given directory (and subdirectories if specified).
 * The file names can be filtered using a {@link FileFilter} and
 * automatic decompression of {@link GZIPInputStream} is used on
 * files that have "gz" suffix.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class DirectoryInputStream extends InputStream {

    //****************** Attributes ******************//

    /** Current input stream */
    private InputStream currentInputStream;

    /** Iterator holding the next files to open */
    private Iterator<File> nextFiles;


    //****************** Constructors ******************//

    /**
     * Creates a new DirectoryInputStream over files that match <code>filter</code> in <code>dirs</code>directories.
     * @param dirs the collection of directories where to search for files
     * @param filter the filter to apply on the files; all files are accepted if it is <tt>null</tt>
     * @param descendDirectories if <tt>true</tt>, the subdirectories are descended and searched for matching files
     * @throws NullPointerException if one of the specified <code>dirs</code> was <tt>null</tt>
     * @throws FileNotFoundException if some of the specified <code>dirs</code> was not found or was not a directory
     * @throws IOException if there was a problem opening file from the directories
     */
    public DirectoryInputStream(Collection<File> dirs, FileFilter filter, boolean descendDirectories) throws NullPointerException, FileNotFoundException, IOException {
        // Prepare iterator on file names
        Collection<File> files = new ArrayList<File>();
        for (File dir : dirs)
            searchFiles(files, dir, filter, descendDirectories);
        nextFiles = files.iterator();

        // Open next stream
        if (!nextFile())
            throw new FileNotFoundException(
                    "There were no files " +
                    ((filter == null)?"":("matching " + filter + " ")) +
                    " in " + dirs.toString()
            );
    }

    /**
     * Creates a new DirectoryInputStream over files that match <code>filter</code> in the directory <code>dir</code>.
     * @param dir the directory where to search for files
     * @param filter the filter to apply on the files; all files are accepted if it is <tt>null</tt>
     * @param descendDirectories if <tt>true</tt>, the subdirectories are descended and searched for matching files
     * @throws NullPointerException if one of the specified <code>dirs</code> was <tt>null</tt>
     * @throws FileNotFoundException if some of the specified <code>dirs</code> was not found or was not a directory
     * @throws IOException if there was a problem opening file from the directories
     */
    public DirectoryInputStream(File dir, FileFilter filter, boolean descendDirectories) throws NullPointerException, FileNotFoundException, IOException {
        this(Collections.singletonList(dir), filter, descendDirectories);
    }

    /**
     * Creates a new DirectoryInputStream over files that match <code>filter</code> in the directory <code>dir</code>.
     * @param dir the directory where to search for files
     * @param filter the filter to apply on the files; all files are accepted if it is <tt>null</tt>
     * @param descendDirectories if <tt>true</tt>, the subdirectories are descended and searched for matching files
     * @throws NullPointerException if one of the specified <code>dirs</code> was <tt>null</tt>
     * @throws FileNotFoundException if some of the specified <code>dirs</code> was not found or was not a directory
     * @throws IOException if there was a problem opening file from the directories
     */
    public DirectoryInputStream(String dir, FileFilter filter, boolean descendDirectories) throws NullPointerException, FileNotFoundException, IOException {
        this(new File(dir), filter, descendDirectories);
    }


    //****************** Factory method ******************//

    /**
     * Open input stream for the specified path.
     * <ul>
     * <li>If the <code>path</code> is <tt>null</tt>, empty or "-", the {@link System#in} is returned.</li>
     * <li>If the <code>path</code> is a directory, all the files in this directory and its subdirectories
     * will be merged into a {@link DirectoryInputStream single input stream} that is returned.</li>
     * <li>If the <code>path</code> contains wildcards (<tt>*, ?, [ab-z]</tt> are supported)
     * in the last component, the begining is treated as the directory name and the last
     * component as the wildcard for the filenames. <br/> For example: <tt>/data/test/*.txt</tt>
     * will search for files with ".txt" suffix in all subdirectories under "/data/test".</li>
     * <li>Otherwise, a {@link FileInputStream} is returned.</li>
     * </ul>
     *
     * If any of the file names end with "gz", {@link GZIPInputStream GZIP stream decompression}
     * is automatically used.
     * 
     * @param path the path to open
     * @return a new instance of InputStream
     * @throws FileNotFoundException if the specified <code>path</code> was not found
     * @throws IOException if there was a problem opening file from the directories
     */
    public static InputStream open(String path) throws FileNotFoundException, IOException {
        if (path == null || path.length() == 0 || path.equals("-"))
            return System.in;

        // Handle file glob patterns
        int pos = WildcardFileFilter.getFilenameGlobPosition(path);
        if (pos == -1)
            return open(new File(path), null);
        else if (pos == 0)
            return open(new File("."), new WildcardFileFilter(path));
        else
            return open(new File(path.substring(0, pos)), new WildcardFileFilter(path.substring(pos + 1)));
    }

    /**
     * Open input stream for the specified path.
     * If the <code>path</code> is <tt>null</tt>, the {@link System#in} is returned.
     * If the <code>path</code> is a directory, all the files in this directory and its subdirectories
     * will be merged into a {@link DirectoryInputStream single input stream} that is returned.
     * Otherwise, a {@link FileInputStream} is returned.
     *
     * <p>
     * If any of the file names end with "gz", {@link GZIPInputStream GZIP stream decompression}
     * is automatically used.
     * </p>
     * @param path the path to open
     * @return a new instance of InputStream
     * @throws FileNotFoundException if the specified <code>path</code> was not found
     * @throws IOException if there was a problem opening file from the directories
     */
    public static InputStream open(File path) throws FileNotFoundException, IOException {
        return open(path, null);
    }

    /**
     * Open input stream for the specified path.
     * If the <code>path</code> is <tt>null</tt>, the {@link System#in} is returned.
     * If the <code>path</code> is a directory, all the files in this directory and its subdirectories
     * will be merged into a {@link DirectoryInputStream single input stream} that is returned.
     * Otherwise, a {@link FileInputStream} is returned.
     *
     * <p>
     * If any of the file names end with "gz", {@link GZIPInputStream GZIP stream decompression}
     * is automatically used.
     * </p>
     * @param path the path to open
     * @param filter the filter to apply on the files; all files are accepted if it is <tt>null</tt>
     * @return a new instance of InputStream
     * @throws FileNotFoundException if the specified <code>path</code> was not found
     * @throws IOException if there was a problem opening file from the directories
     */
    public static InputStream open(File path, FileFilter filter) throws FileNotFoundException, IOException {
        if (path == null)
            return System.in;
        else if (path.isDirectory())
            return new DirectoryInputStream(path, filter, true);
        else if (filter == null || filter.accept(path))
            return openInputStream(path);
        else
            throw new FileNotFoundException("File " + path + " does not match filter " + filter);
    }

    /**
     * Open input stream for the specified file name.
     * If the file name ends with "gz", GZIP stream decompression is used.
     * @param file the file path to open
     * @return a new instance of InputStream
     * @throws IOException if there was an error opening the file
     */
    private static InputStream openInputStream(File file) throws IOException {
        if (file.getPath().endsWith("gz"))
            return new GZIPInputStream(new FileInputStream(file));
        else
            return new FileInputStream(file);
    }


    //****************** Internal methods ******************//

    /**
     * Updates the <code>currentInputStream</code> with a next file from <code>nextFiles</code>.
     * The current input stream is closed and a new is opened using {@link #openInputStream(java.io.File)}.
     *
     * @return <tt>true</tt> if a new file was opened
     * @throws IOException if there was an error opening a next file
     */
    private boolean nextFile() throws IOException {
        if (!nextFiles.hasNext())
            return false;
        if (currentInputStream != null)
            currentInputStream.close();
        currentInputStream = openInputStream(nextFiles.next());
        return true;
    }

    /**
     * Search the directory for files that match the filter.
     * If the <code>descendDirectories</code> parameter is <tt>true</tt>,
     * the subdirectories are descended and searched too.
     *
     * <p>
     * The found files are added to the <code>files</code> collection.
     * There is no guarantee that the files in the resulting collection will appear
     * in any specific order; they are not, in particular, guaranteed to appear in alphabetical order.
     * </p>
     *
     * @param files the collection that receives the files found; if <tt>null</tt>, a new {@link ArrayList} is created
     * @param dir the directory to search for files
     * @param filter the filter to apply on the files; all files are accepted if it is <tt>null</tt>
     * @param descendDirectories if <tt>true</tt>, the subdirectories are descended and searched for matching files
     * @return the collection of files found, i.e. the collection provided in <code>files</code> argument
     * @throws FileNotFoundException if the specified <code>dir</code> was not found or it is not a directory
     * @throws NullPointerException if the specified <code>dir</code> was <tt>null</tt>
     */
    public static Collection<File> searchFiles(Collection<File> files, File dir, FileFilter filter, boolean descendDirectories) throws FileNotFoundException, NullPointerException {
        // Read all files in the directory
        File[] dirFiles = dir.listFiles();
        if (dirFiles == null)
            throw new FileNotFoundException("Cannot read contents of directory " + dir);

        // Create the holding collection if it is null
        if (files == null)
            files = new ArrayList<File>();

        // Search the directory for the matching files
        for (File file : dirFiles) {
            if (file.isDirectory()) {
                if (descendDirectories)
                    searchFiles(files, file, filter, descendDirectories);
            } else if (filter == null || filter.accept(file)) {
                files.add(file);
            }
        }

        return files;
    }


    //****************** InputStream method implementations ******************//

    public int read() throws IOException {
        int ret;
        do {
            ret = currentInputStream.read();
        } while (ret == -1 && nextFile());
        return ret;
    }

    @Override
    public int read(byte b[]) throws IOException {
        int ret;
        do {
            ret = currentInputStream.read(b);
        } while (ret == -1 && nextFile());
        return ret;
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        int ret;
        do {
            ret = currentInputStream.read(b, off, len);
        } while (ret == -1 && nextFile());
        return ret;
    }

    @Override
    public long skip(long n) throws IOException {
        return currentInputStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return currentInputStream.available();
    }

    @Override
    public void close() throws IOException {
        currentInputStream.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        currentInputStream.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        currentInputStream.reset();
    }

    @Override
    public boolean markSupported() {
        return currentInputStream.markSupported();
    }
}
