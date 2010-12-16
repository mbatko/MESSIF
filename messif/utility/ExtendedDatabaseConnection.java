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

import java.io.Serializable;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Properties;

/**
 * This class is intended as an encapsulation of a common database {@link Connection}
 * that allows to reconnect after a failed execution of a statement.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class ExtendedDatabaseConnection implements Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Constants ******************//

    /** Number of miliseconds to sleep when an SQL command fail before another one is tried */
    private static final int connectionRetryTime = 500;


    //****************** Attributes ******************//

    /** Database connection URL */
    private final String dbConnUrl;
    /** Properties with database connection info */
    private final Properties dbConnInfo;
    /** Connection to database (according to {@link #dbConnUrl}) */
    private transient Connection dbConnection;


    //****************** Constructor ******************//

    /**
     * Creates a new extended database connection.
     *
     * @param dbConnUrl the database connection URL (e.g. "jdbc:mysql://localhost/somedb")
     * @param dbConnInfo additional parameters of the connection (e.g. "user" and "password")
     * @param dbDriverClass class of the database driver to use (can be <tt>null</tt> if the driver is already registered)
     * @throws IllegalArgumentException if the connection url is <tt>null</tt> or the driver class cannot be registered
     * @throws SQLException if there was a problem connecting to the database
     */
    protected ExtendedDatabaseConnection(String dbConnUrl, Properties dbConnInfo, String dbDriverClass) throws IllegalArgumentException, SQLException {
        this.dbConnUrl = dbConnUrl;
        this.dbConnInfo = dbConnInfo;
        this.dbConnection = createConnection(dbConnUrl, dbConnInfo, dbDriverClass);
    }

    /**
     * Creates a new extended database connection with parameters taken from another connection.
     *
     * @param sourceConnection the database connection from which to get the connection string and info
     * @throws SQLException if there was a problem connecting to the database
     */
    protected ExtendedDatabaseConnection(ExtendedDatabaseConnection sourceConnection) throws SQLException {
        this.dbConnUrl = sourceConnection.dbConnUrl;
        this.dbConnInfo = sourceConnection.dbConnInfo;
        this.dbConnection = createConnection(dbConnUrl, dbConnInfo, null); // Driver is not needed, since it was registered by the previous connection
    }

    @Override
    public void finalize() throws Throwable {
        closeConnection();
        super.finalize();
    }


    //****************** SQL connection ******************//

    /**
     * Creates a new database connection.
     * @return the database connection
     * @param dbConnUrl the database connection URL (e.g. "jdbc:mysql://localhost/somedb")
     * @param dbConnInfo additional parameters of the connection (e.g. "user" and "password")
     * @param dbDriverClass class of the database driver to use (can be <tt>null</tt> if the driver is already registered)
     * @throws IllegalArgumentException if the connection url is <tt>null</tt> or the driver class cannot be registered
     * @throws SQLException if there was a problem connecting to the database
     */
    public static Connection createConnection(String dbConnUrl, Properties dbConnInfo, String dbDriverClass) throws IllegalArgumentException, SQLException {
        if (dbConnUrl == null)
            throw new IllegalArgumentException("Database connection URL cannot be null");
        if (dbDriverClass != null)
            try {
                DriverManager.registerDriver(Convert.getClassForName(dbDriverClass, Driver.class).newInstance());
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot register database driver " + dbDriverClass + ": " + e, e);
            }
        return DriverManager.getConnection(dbConnUrl, dbConnInfo);
    }

    /**
     * Returns the database connection of this storage.
     * @return the database connection
     * @throws SQLException if there was a problem connecting to the database
     */
    protected final Connection getConnection() throws SQLException {
        if (dbConnection != null && !dbConnection.isClosed())
            return dbConnection;
        return dbConnection = createConnection(dbConnUrl, dbConnInfo, null); // Driver should be already registered
    }

    /**
     * Closes connection to the database.
     * @throws SQLException if there was an error while closing the connection
     */
    protected void closeConnection() throws SQLException {
        if (dbConnection != null)
            dbConnection.close();
    }

    /**
     * Prepares and executes an SQL command using this storage's database connection.
     * The {@link ResultSet} returned by the execution can be retrieved by {@link PreparedStatement#getResultSet()}.
     * Note that if a {@link SQLRecoverableException} is thrown while executing,
     * the current connection is {@link #closeConnection() closed} and the command
     * retried.
     *
     * @param statement the previous cached statement that matches the given {@code sql} (can be <tt>null</tt>)
     * @param sql the SQL command to prepare and execute
     * @param parameters the values for the SQL parameters (denoted by "?" chars in the SQL command)
     * @return an executed prepared statement
     * @throws SQLException if there was an unrecoverable error when parsing or executing the SQL command
     */
    protected final PreparedStatement prepareAndExecute(PreparedStatement statement, String sql, Object... parameters) throws SQLException {
        for (;;) {
            // Prepare statement
            if (statement == null || statement.isClosed())
                statement = getConnection().prepareStatement(sql);

            // Map parameters
            if (parameters != null)
                for (int i = 0; i < parameters.length; i++)
                    statement.setObject(i + 1, parameters[i]);

            // Execute query and handle recoverable exception
            try {
                statement.execute();
                return statement;
            } catch (SQLRecoverableException e) {
                closeConnection();
                try {
                    Thread.sleep(connectionRetryTime);
                } catch (InterruptedException ex) {
                    throw new SQLException(e.toString(), ex);
                }
            }
        }
    }

    /**
     * Returns the first column of the first row returned by the given SQL command.
     * @param sql the SQL command to execute
     * @param parameters parameters for the "?" placeholders inside the SQL command
     * @return the value in the first column of the first row
     * @throws NoSuchElementException if the SQL command does not return any row
     * @throws SQLException if there was a problem parsing or executing the SQL command
     */
    protected final Object executeSingleValue(String sql, Object... parameters) throws NoSuchElementException, SQLException {
        ResultSet rs = prepareAndExecute(null, sql, parameters).getResultSet();
        try {
            if (!rs.next())
                throw new NoSuchElementException("No data for " + Arrays.toString(parameters) + " found");
            return rs.getObject(1);
        } finally {
            rs.close();
        }
    }


    //****************** Public wrapper ******************//

    /**
     * Provides a wrapper for the {@link ExtendedDatabaseConnection} that offers
     * all the methods publicly.
     */
    public static final class ExtendedDatabaseConnectionPublic extends ExtendedDatabaseConnection {
        /** class serial id for serialization */
        private static final long serialVersionUID = 1L;

        /**
         * Creates a new extended database connection.
         *
         * @param dbConnUrl the database connection URL (e.g. "jdbc:mysql://localhost/somedb")
         * @param dbConnInfo additional parameters of the connection (e.g. "user" and "password")
         * @param dbDriverClass class of the database driver to use (can be <tt>null</tt> if the driver is already registered)
         * @throws IllegalArgumentException if the connection url is <tt>null</tt> or the driver class cannot be registered
         * @throws SQLException if there was a problem connecting to the database
         */
        public ExtendedDatabaseConnectionPublic(String dbConnUrl, Properties dbConnInfo, String dbDriverClass) throws IllegalArgumentException, SQLException {
            super(dbConnUrl, dbConnInfo, dbDriverClass);
        }

        /**
         * Creates a new extended database connection with parameters taken from another connection.
         *
         * @param sourceConnection the database connection from which to get the connection string and info
         * @throws SQLException if there was a problem connecting to the database
         */
        public ExtendedDatabaseConnectionPublic(ExtendedDatabaseConnectionPublic sourceConnection) throws SQLException {
            super(sourceConnection);
        }

        @Override
        public void closeConnection() throws SQLException {
            super.closeConnection();
        }

        /**
         * Prepares and executes an SQL command using this storage's database connection.
         * The {@link ResultSet} returned by the execution can be retrieved by {@link PreparedStatement#getResultSet()}.
         * Note that if a {@link SQLRecoverableException} is thrown while executing,
         * the current connection is {@link #closeConnection() closed} and the command
         * retried.
         *
         * @param statement the previous cached statement that matches the given {@code sql} (can be <tt>null</tt>)
         * @param sql the SQL command to prepare and execute
         * @param parameters the values for the SQL parameters (denoted by "?" chars in the SQL command)
         * @return an executed prepared statement
         * @throws SQLException if there was an unrecoverable error when parsing or executing the SQL command
         */
        public PreparedStatement prepareAndExecuteStatement(PreparedStatement statement, String sql, Object... parameters) throws SQLException {
            return super.prepareAndExecute(statement, sql, parameters);
        }

        /**
         * Returns the first column of the first row returned by the given SQL command.
         * @param sql the SQL command to execute
         * @param parameters parameters for the "?" placeholders inside the SQL command
         * @return the value in the first column of the first row
         * @throws NoSuchElementException if the SQL command does not return any row
         * @throws SQLException if there was a problem parsing or executing the SQL command
         */
        public Object executeSingleValueSQL(String sql, Object... parameters) throws NoSuchElementException, SQLException {
            return super.executeSingleValue(sql, parameters);
        }

    }

}
