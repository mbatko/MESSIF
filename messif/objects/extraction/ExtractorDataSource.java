/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package messif.objects.extraction;

import java.io.BufferedReader;
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
 * @author xbatko
 */
public class ExtractorDataSource {
    /** Number of bytes that the {@link #getBinaryData()} method allocates */
    private static final int readStreamDataAllocation = 4096;

    /** Name of the data source */
    private final String name;
    /** Input stream that provides data for this data source */
    private final InputStream inputStream;
    /** Number of bytes available in the input stream or zero if this is unknown in advance */
    private final int bytesAvailable;
    /** Internal buffered reader that access the input stream */
    private BufferedReader bufferedReader;

    /**
     * Create new instance of ExtractorDataSource using data from {@link InputStream}.
     * @param inputStream the input stream from which to download the data
     * @param name the name of this data source
     */
    public ExtractorDataSource(InputStream inputStream, String name) {
        this.inputStream = inputStream;
        this.bytesAvailable = -1;
        this.name = name;
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

        this.inputStream = conn.getInputStream();
        this.bytesAvailable = conn.getContentLength();
        this.name = url.toString();
    }

    /**
     * Create new instance of ExtractorDataSource using data from {@link File}.
     * @param file the file from which to download the data
     * @throws IOException if there was an error opening the file
     */
    public ExtractorDataSource(File file) throws IOException {
        long fileSize = file.length();
        if (fileSize >= Integer.MAX_VALUE)
            throw new IOException("Cannot load data from " + file + ": file is too big");

        this.inputStream = new FileInputStream(file);
        this.bytesAvailable = (int)fileSize;
        this.name = file.getPath();
    }

    /**
     * Return this data source as input stream.
     * <p>Note that the data source is <i>not</i> closed - use {@link InputStream#close()} method instead.</p>
     * @return
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

    /**
     * Returns the name of this data source.
     * @return the name of this data source
     */
    public String getName() {
        return name;
    }

}
