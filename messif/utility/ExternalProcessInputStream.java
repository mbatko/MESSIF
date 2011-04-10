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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Allows to capture input stream from an external process with error-checking.
 * Whenever the data are read from this process, the exit status of the
 * process is checked (this is not very efficient, since it requires exception
 * catching, but there is no other way in Java). If the process has exited
 * with an error code, the error input is read and thrown as {@link IOException}.
 * 
 * <p>
 * Note that this is a single-thread workaround. A multi-thread processing
 * would be a better solution.
 * </p>
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ExternalProcessInputStream extends InputStream {
    /** Encapsulated process the input stream of which is wrapped */
    private final Process process;
    /** Encapsulated process input stream that is wrapped */
    private final InputStream processInputStream;
    /** List of exit codes that represent a successful exit of the process */
    private final int[] successExitCodes;
    /** Internal flag whether the exit code check was already performed */
    private volatile boolean exited;
    /** Created unsuccessful exit exception */
    private volatile IOException exitException;

    /**
     * Creates a new process input stream.
     * @param process the process the input stream of which to encapsulate
     * @param successExitCodes the list of exit codes that represent a successful exit of the process
     */
    public ExternalProcessInputStream(Process process, int... successExitCodes) {
        this.process = process;
        this.processInputStream = process.getInputStream();
        this.successExitCodes = successExitCodes;
    }

    /**
     * Creates a new process input stream.
     * Zero exit code is considered successful everything else is unsuccessful.
     * @param process the process the input stream of which to encapsulate
     */
    public ExternalProcessInputStream(Process process) {
        this(process, 0);
    }

    /**
     * Returns whether the encapsulated process has exited (<tt>true</tt>) or
     * it is still running (<tt>false</tt>).
     * @param waitForExit flag whether to wait for the process to exit (used when input streams are finished)
     * @return <tt>true</tt> if the encapsulated process has exited or <tt>false</tt> if
     *      it is still running
     * @throws IOException if the process has exited with non-success exit code,
     *      the text passed on std-err is returned as the exception message
     */
    protected boolean hasExited(boolean waitForExit) throws IOException {
        try {
            if (waitForExit)
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    return false;
                }
            int processExitCode = process.exitValue();
            synchronized (this) {
                if (!exited) { // The success codes were not searched yet
                    exited = true;
                    for (int i = 0; i < successExitCodes.length; i++) {
                        if (processExitCode == successExitCodes[i])
                            return true;
                    }
                    // Exit code is not one of the successful, create exception
                    StringBuilder error = new StringBuilder("External extractor returned ").append(processExitCode).append(": ");
                    error = Convert.readStringData(process.getErrorStream(), error);
                    exitException = new IOException(error.toString());
                }
                if (exitException != null)
                    throw exitException;
                return true;
            }
        } catch (IllegalThreadStateException ignore) {// Process is still running
            return false;
        }
    }

    @Override
    public int read() throws IOException {
        hasExited(false);
        int byteRead = processInputStream.read();
        hasExited(byteRead == -1);
        return byteRead;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = processInputStream.read(b, off, len);
        hasExited(bytesRead == -1);
        return bytesRead;
    }

    @Override
    public long skip(long n) throws IOException {
        long bytesSkipped = processInputStream.skip(n);
        hasExited(false);
        return bytesSkipped;
    }

    @Override
    public int available() throws IOException {
        return processInputStream.available();
    }

    @Override
    public void close() throws IOException {
        processInputStream.close();
    }

}
