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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.XMLFormatter;

/**
 * This class provides functionality to control logging via static methods.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class Logging {
    /** Internal map of opened file log handlers */
    private static Map<String, FileHandler> handlers = new HashMap<String, FileHandler>();

    /**
     * Enumeration of possible regexp matcher targets for {@link #addLogFile}.
     */
    public static enum RegexpFilterAgainst {
        /** The regular expression is matched against the record's text (the message that is logged) */
        MESSAGE,
        /** The regular expression is matched against the name of the logger that was used to dispatch the log record */
        LOGGER_NAME,
        /** The regular expression is matched against the name of the class from which the log record was invoked */
        CLASS_NAME,
        /** The regular expression is matched against the name of the method that from which the log record was invoked */
        METHOD_NAME
    };

    /**
     * This is a supplementory class providing the functionality of filtering
     *  log messages according to a regular experssion
     */
    private static class RegexpFilter implements java.util.logging.Filter {

        /** Regular expression to match the message */
        private final String regexp;
        /** Target to match the regexp against */
        private final RegexpFilterAgainst regexpAgainst;

        /**
         * Create a new regular expression filter.
         * @param regexp the regular expression to match the message
         * @param regexpAgainst the target to match the regexp against
         */
        public RegexpFilter(String regexp, RegexpFilterAgainst regexpAgainst) {
            this.regexp = regexp;
            this.regexpAgainst = regexpAgainst;
        }

        /** Check if a given log record should be published. The record must match the regular expression to be published. */
        public boolean isLoggable(LogRecord record) {
            String text;
            switch (regexpAgainst) {
                case LOGGER_NAME:
                    text = record.getLoggerName();
                    break;
                case CLASS_NAME:
                    text = record.getSourceClassName();
                    break;
                case METHOD_NAME:
                    text = record.getSourceMethodName();
                    break;
                case MESSAGE:
                default:
                    text = record.getMessage();
                    break;
            }
            if (text == null)
                return "".matches(regexp);
            return text.matches(regexp);
        }
    }

    /**
     * Returns the root (top-level) logger from the actual log manager.
     * @return the top-level logger
     */
    protected static java.util.logging.Logger getRootLogger() {
        return LogManager.getLogManager().getLogger("");
    }

    /**
     * Set global logging level.
     *  Every message, that has higher level is discarded.
     *  Note that higher level messages will show neither on console nor in any log file,
     *  eventhoug they migh have higher log level set.
     * @param level New global logging level
     */
    public static void setLogLevel(Level level) {
        // Set level of root logger
        getRootLogger().setLevel(level);
    }

    /**
     * Get global logging level. Values can be found in Level class (OFF, SEVERE, WARNING, INFO, ..., ALL).
     * @return Current global log level
     */
    public static Level getLogLevel() {
        return getRootLogger().getLevel();
    }

    /**
     * Set logging level for an opened log file.
     * @param fileName the name of the log file
     * @param level the new logging level to set
     * @return <tt>true</tt> if the logging level was successfuly set for the specified file
     */
    public static boolean setLogFileLevel(String fileName, Level level) {
        FileHandler handler = handlers.get(fileName);
        if (handler == null)
            return false;
        handler.setLevel(level);
        return true;
    }

    /**
     * Set logging level of the console.
     * If there is no console handler available nothing is modified.
     *
     * @param level the new logging level for console
     */
    public static void setConsoleLevel(Level level) {
        for (Handler handler : getRootLogger().getHandlers())
            if (handler instanceof ConsoleHandler)
                handler.setLevel(level);
    }

    /**
     * Adds a new logging file.
     * @param fileName the path of the newly opened logging file - can be absolute or relative to the current working directory
     * @param level the logging level of the file - only messages with lower level will be stored in the file; can be changed by calls to {@link #setLogFileLevel}
     * @param append the flag whether the target file should be truncated prior to writing (<tt>false</tt>) or not
     * @param formatter the formatter instance that will format messages sent to the file; default formater will be used if <tt>null</tt>
     * @param regexp the regular expression used to filter the messages stored to this log file; if <tt>null</tt> all messages are stored
     * @param regexpAgainst the part of the log record to match the regexp against
     * @throws IOException if there were problems opening the file
     */
    public static void addLogFile(String fileName, Level level, boolean append, Formatter formatter, String regexp, RegexpFilterAgainst regexpAgainst) throws IOException {
        FileHandler handler = new FileHandler(fileName, append);
        handler.setLevel(level);
        if (formatter != null)
            handler.setFormatter(formatter);
        if (regexp != null && regexp.length() > 0)
            handler.setFilter(new RegexpFilter(regexp, regexpAgainst));
        handlers.put(fileName, handler);
        getRootLogger().addHandler(handler);
    }

    /**
     * Adds a new logging file.
     * @param fileName the path of the newly opened logging file - can be absolute or relative to the current working directory
     * @param level the logging level of the file - only messages with lower level will be stored in the file; can be changed by calls to {@link #setLogFileLevel}
     * @param append the flag whether the target file should be truncated prior to writing (<tt>false</tt>) or not
     * @param formatter the formatter instance that will format messages sent to the file; default formater will be used if <tt>null</tt>
     * @throws IOException if there were problems opening the file
     */
    public static void addLogFile(String fileName, Level level, boolean append, Formatter formatter) throws IOException {
        addLogFile(fileName, level, append, formatter, null, RegexpFilterAgainst.MESSAGE);
    }

    /**
     * Adds a new logging file.
     * @param fileName the path of the newly opened logging file - can be absolute or relative to the current working directory
     * @param level the logging level of the file - only messages with lower level will be stored in the file; can be changed by calls to {@link #setLogFileLevel}
     * @param append the flag whether the target file should be truncated prior to writing (<tt>false</tt>) or not
     * @param useSimpleFormatter controls whether the {@link java.util.logging.SimpleFormatter} (if <tt>true</tt>)
     *                           or {@link java.util.logging.XMLFormatter} (if <tt>false</tt>) is used to format messages sent to the file
     * @param regexp the regular expression used to filter the messages stored to this log file; if <tt>null</tt> all messages are stored
     * @param regexpAgainst the part of the log record to match the regexp against
     * @throws IOException if there were problems opening the file
     */
    public static void addLogFile(String fileName, Level level, boolean append, boolean useSimpleFormatter, String regexp, RegexpFilterAgainst regexpAgainst) throws IOException {
        addLogFile(fileName, level, append, useSimpleFormatter?new SimpleFormatter():new XMLFormatter(), regexp, regexpAgainst);
    }

    /**
     * Adds a new logging file.
     * @param fileName the path of the newly opened logging file - can be absolute or relative to the current working directory
     * @param level the logging level of the file - only messages with lower level will be stored in the file; can be changed by calls to {@link #setLogFileLevel}
     * @param append the flag whether the target file should be truncated prior to writing (<tt>false</tt>) or not
     * @param useSimpleFormatter controls whether the {@link java.util.logging.SimpleFormatter} (if <tt>true</tt>)
     *                           or {@link java.util.logging.XMLFormatter} (if <tt>false</tt>) is used to format messages sent to the file
     * @throws IOException if there were problems opening the file
     */
    public static void addLogFile(String fileName, Level level, boolean append, boolean useSimpleFormatter) throws IOException {
        addLogFile(fileName, level, append, useSimpleFormatter, null, RegexpFilterAgainst.MESSAGE);
    }

    /**
     * Close a log file and remove it from logging.
     * @param fileName the name of the log file to remove
     * @return <tt>true</tt> if the logging file was successfuly removed
     */
    public static boolean removeLogFile(String fileName) {
        FileHandler handler = handlers.get(fileName);
        if (handler != null) {
            getRootLogger().removeHandler(handler);
            handler.close();
            return true;
        } else return false;
    }
}