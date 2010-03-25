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
package messif.objects.extraction;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Provides a data source for {@link Extractor}s.
 * It can be constructed either from a {@link InputStream}, {@link File} or a {@link URL}.
 * Depending on that, the source's name is set to either <tt>null</tt>, the name of the file, or the URL.
 * The data can be used by the extractors as either
 * <ul>
 * <li>{@link InputStream} - use {@link #getInputStream()} method,</li>
 * <li>{@link BufferedReader} - use {@link #getBufferedReader()} method,</li>
 * <li>{@code byte[]} - use {@link #getBinaryData()} method, or</li>
 * <li>piped to an {@link OutputStream} - use {@link #pipe(java.io.OutputStream)} method.</li>
 * </ul>
 * <p>
 * Note that the data can be used only as one of the aforementioned types and, once read,
 * they are no longer available from the source.
 * </p>
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ExtractorDataSource implements Closeable {
    /** Number of bytes that the {@link #getBinaryData()} method allocates */
    private static final int readStreamDataAllocation = 4096;

    //****************** Attributes *************//

    /** Remembered data source (can be {@link File}, {@link InputStream} or {@link URL}) */
    private final Object dataSource;
    /** Name of the data source */
    private final String name;
    /** Input stream that provides data for this data source */
    private InputStream inputStream;
    /** Number of bytes available in the input stream or zero if this is unknown in advance */
    private int bytesAvailable;
    /** Internal buffered reader that access the input stream */
    private BufferedReader bufferedReader;


    //****************** Constructors *************//

    /**
     * Create new instance of ExtractorDataSource using data from {@link InputStream}.
     * @param inputStream the input stream from which to download the data
     * @param name the name of this data source
     */
    public ExtractorDataSource(InputStream inputStream, String name) {
        this.name = name;
        this.dataSource = inputStream;
        openDataSource(inputStream);
    }

    /**
     * Create new instance of ExtractorDataSource using data downloaded from {@link URL}.
     * @param url the URL from which to download the data
     * @param mimeTypeRegexp regular expression for the mimetype of the data on the given {@code url}
     * @throws IOException if there was an error reading the data
     */
    public ExtractorDataSource(URL url, String mimeTypeRegexp) throws IOException {
        // Open url connection
        URLConnection conn = url.openConnection();

        // Check content type
        if (mimeTypeRegexp != null && conn.getContentType() != null && !mimeTypeRegexp.matches(conn.getContentType()))
            throw new IOException("Cannot read '" + conn.getContentType() + "' data");

        this.name = url.toString();
        this.dataSource = url;
        openDataSource(conn);
    }

    /**
     * Create new instance of ExtractorDataSource using data from {@link File}.
     * @param file the file from which to download the data
     * @throws IOException if there was an error opening the file
     */
    public ExtractorDataSource(File file) throws IOException {
        this.name = file.getPath();
        this.dataSource = file;
        openDataSource(file);
    }


    //****************** Data source open *************//

    /**
     * Open anonymous data source.
     * This method chooses the correct {@code openDataSource(...)} method.
     * @param dataSource the data source to use
     * @throws IOException if there was an I/O error opening the given data source
     */
    private void openDataSourceAnonymous(Object dataSource) throws IOException {
        if (dataSource instanceof InputStream) {
            openDataSource((InputStream)dataSource);
        } else if (dataSource instanceof File) {
            openDataSource((File)dataSource);
        } else if (dataSource instanceof URL) {
            openDataSource(((URL)dataSource).openConnection());
        } else if (dataSource instanceof URLConnection) {
            openDataSource((URLConnection)dataSource);
        } else {
            throw new InternalError("Unknown data source - added constructor without modification of openDataSource method");
        }
    }

    /**
     * Open data source from an {@link InputStream}.
     * @param stream the stream to use as data source
     */
    private void openDataSource(InputStream stream) {
        this.inputStream = stream;
        this.bytesAvailable = -1;
    }

    /**
     * Open data source from a {@link File}.
     * @param file the file to use as data source
     * @throws IOException if there was an I/O error opening the given file
     */
    private void openDataSource(File file) throws IOException {
        long fileSize = file.length();
        if (fileSize >= Integer.MAX_VALUE)
            throw new IOException("Cannot load data from " + file + ": file is too big");
        this.inputStream = new FileInputStream(file);
        this.bytesAvailable = (int)fileSize;
    }

    /**
     * Open data source from a {@link URLConnection}.
     * @param conn the URL connection to use as data source
     * @throws IOException if there was an I/O error opening the given URL connection
     */
    private void openDataSource(URLConnection conn) throws IOException {
        this.inputStream = conn.getInputStream();
        this.bytesAvailable = conn.getContentLength();
    }


    //****************** Data access methods *************//

    /**
     * Returns the name of this data source.
     * @return the name of this data source
     */
    public String getName() {
        return name;
    }

    /**
     * Return this data source as input stream.
     * <p>Note that the data source is <i>not</i> closed - use {@link InputStream#close()} method instead.</p>
     * @return this data source as input stream
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * Return this data source as buffered reader.
     * <p>Note that the data source is <i>not</i> closed - use {@link BufferedReader#close()} method instead.</p>
     *
     * @return a buffer containing the data
     */
    public BufferedReader getBufferedReader() {
        if (bufferedReader == null)
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        return bufferedReader;
    }

    /**
     * Return this data source as a byte buffer.
     * <p>Note that the data source is closed after this method is used.</p>
     *
     * @return a buffer containing the data
     * @throws IOException if there was a problem reading from the data source
     */
    public byte[] getBinaryData() throws IOException {
        if (bufferedReader != null)
            throw new IOException("Cannot use binary data getter - the buffered reader was used");
        // Create buffer (has always at least bufferSize bytes available)
        byte[] buffer = new byte[bytesAvailable > 0 ? bytesAvailable : readStreamDataAllocation];
        int offset = 0;
        int bytes;
        while ((bytes = inputStream.read(buffer, offset, buffer.length - offset)) > 0) {
            offset += bytes;
            // Check if the buffer is not full
            if (offset == buffer.length && bytesAvailable <= 0) {
                // Add some space
                byte[] copy = new byte[offset + readStreamDataAllocation];
                System.arraycopy(buffer, 0, copy, 0, offset);
                buffer = copy;
            }
        }

        // Close the input stream, since all data was read
        inputStream.close();

        // Shrink the array
        if (offset != buffer.length) {
            byte[] copy = new byte[offset];
            System.arraycopy(buffer, 0, copy, 0, offset);
            buffer = copy;
        }

        return buffer;
    }

    /**
     * Output all data from this data source to the given {@code outputStream}.
     * <p>Note that the data source is closed after this method is used.</p>
     * 
     * @param outputStream the stream to which to write the data
     * @throws IOException if there was an error reading from this data source or writing to the output stream
     */
    public void pipe(OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[readStreamDataAllocation];
        int bytes;
        while ((bytes = inputStream.read(buffer)) > 0)
            outputStream.write(buffer, 0, bytes);

        // Close the input stream, since all data was read
        inputStream.close();
    }

    public void close() throws IOException {
        inputStream.close();
    }

    /**
     * Reset this data source, i.e. the data will be provided from beginning.
     * Note that reset is available only if a resetable data source was used
     * in constructor (e.g. file or url).
     * @throws IOException if there was an I/O error re-opening the data source
     */
    public void reset() throws IOException {
        // Check if file name was remembered
        if (dataSource == null)
            throw new IOException("Cannot reset this stream, file name not provided");

        // Reset current stream
        inputStream.close();
        openDataSourceAnonymous(dataSource);
    }

}
