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
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLRecoverableException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
     * @param lazyConnection flag whether to open the database connection immediately (<tt>false</tt>)
     *          or when a first command is executed (<tt>true</tt>)
     * @throws IllegalArgumentException if the connection url is <tt>null</tt> or the driver class cannot be registered
     * @throws SQLException if there was a problem connecting to the database
     */
    protected ExtendedDatabaseConnection(String dbConnUrl, Properties dbConnInfo, String dbDriverClass, boolean lazyConnection) throws IllegalArgumentException, SQLException {
        this.dbConnUrl = dbConnUrl;
        this.dbConnInfo = dbConnInfo;
        if (!lazyConnection)
            this.dbConnection = createConnection(dbConnUrl, dbConnInfo, dbDriverClass);
        else if (dbDriverClass != null)
            DriverManager.registerDriver(createDriver(dbDriverClass));            
    }

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
        this(dbConnUrl, dbConnInfo, dbDriverClass, false);
    }

    /**
     * Creates a new extended database connection with parameters taken from another connection.
     *
     * @param sourceConnection the database connection from which to get the connection string and info
     * @param lazyConnection flag whether to open the database connection immediately (<tt>false</tt>)
     *          or when a first command is executed (<tt>true</tt>)
     * @throws SQLException if there was a problem connecting to the database
     */
    protected ExtendedDatabaseConnection(ExtendedDatabaseConnection sourceConnection, boolean lazyConnection) throws SQLException {
        this(sourceConnection.dbConnUrl, sourceConnection.dbConnInfo, null, lazyConnection);
    }

    /**
     * Creates a new extended database connection with parameters taken from another connection.
     *
     * @param sourceConnection the database connection from which to get the connection string and info
     * @throws SQLException if there was a problem connecting to the database
     */
    protected ExtendedDatabaseConnection(ExtendedDatabaseConnection sourceConnection) throws SQLException {
        this(sourceConnection, false);
    }

    @Override
    public void finalize() throws Throwable {
        closeConnection();
        super.finalize();
    }


    //****************** SQL connection ******************//

    /**
     * Creates a new instance of a database driver for the given driver class.
     * Note that the driver should automatically register itself to {@link DriverManager},
     * so use of the driver directly should be avoided.
     * @param dbDriverClass class of the database driver to use
     * @return a new instance of a database driver
     * @throws IllegalArgumentException if the driver class cannot be created
     */
    public static Driver createDriver(String dbDriverClass) throws IllegalArgumentException {
        if (dbDriverClass == null)
            return null;
        try {
            // Create an instance of the new driver (it should register itself automatically) and return the connection
            return (Convert.getClassForName(dbDriverClass, Driver.class).newInstance());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cannot create database driver " + dbDriverClass + ": " + ex, ex);
        }
    }

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
        try {
            return DriverManager.getConnection(dbConnUrl, dbConnInfo);
        } catch (SQLException e) {
            // If the driver is not provided, we cannot establish a connection
            if (dbDriverClass == null)
                throw e;
            return createDriver(dbDriverClass).connect(dbConnUrl, dbConnInfo);
        }
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
     * @param returnGeneratedKeys flag whether to set the {@link Statement#RETURN_GENERATED_KEYS} on the prepared statement
     * @param parameters the values for the SQL parameters (denoted by "?" chars in the SQL command)
     * @return an executed prepared statement
     * @throws SQLFeatureNotSupportedException if the {@link Statement#RETURN_GENERATED_KEYS} is not supported by the driver
     * @throws SQLException if there was an unrecoverable error when parsing or executing the SQL command
     */
    protected final PreparedStatement prepareAndExecute(PreparedStatement statement, String sql, boolean returnGeneratedKeys, Object... parameters) throws SQLFeatureNotSupportedException, SQLException {
        for (;;) {
            // Prepare statement
            if (statement == null || statement.isClosed())
                statement = getConnection().prepareStatement(sql, returnGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);

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
        PreparedStatement stmt = prepareAndExecute(null, sql, false, parameters);
        ResultSet rs = stmt.getResultSet();
        try {
            if (!rs.next())
                throw new NoSuchElementException("No data for " + Arrays.toString(parameters) + " found");
            return rs.getObject(1);
        } finally {
            rs.close();
            stmt.close();
        }
    }

    /**
     * Executes a data modification SQL command (i.e. INSERT, UPDATE or DELETE).
     * The returned value is the first auto-generated column if {@code generatedKeys}
     * is <tt>true</tt> or the number of affected rows {@link PreparedStatement#getUpdateCount()} if available.
     * @param sql the SQL command to execute
     * @param returnGeneratedKeys the flag whether to retrieve the keys generated automatically
     *          by the database (e.g. when inserting)
     * @param parameters the values for the parameters in the SQL statement;
     *          there must be one value for every ? in the SQL statement
     * @return the first auto-generated column or the number of affected rows
     * @throws NoSuchElementException if the SQL command does not return any row
     * @throws SQLException if there was a problem parsing or executing the SQL command
     */
    protected final Object executeDataManipulation(String sql, boolean returnGeneratedKeys, Object... parameters) throws NoSuchElementException, SQLException {
        PreparedStatement stmt = prepareAndExecute(null, sql, returnGeneratedKeys, parameters);
        try {
            if (returnGeneratedKeys) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (!rs.next())
                    throw new NoSuchElementException("No data for " + Arrays.toString(parameters) + " found");
                return rs.getObject(1);
            } else {
                return stmt.getUpdateCount();
            }
        } finally {
            stmt.close();
        }
    }

    /**
     * Creates a map of key-value pairs from a given result set.
     * The keys are read from the result set's {@code keyColumn} and
     * values from the {@code valueColumn}.
     * @param <K> the class of the keys
     * @param <V> the class of the values
     * @param rs the result set to get the key-value pairs from
     * @param keyColumn the identification of the column that contains the keys (1 means the first column, 2 the second, etc.)
     * @param keyClass the class of the keys
     * @param valueColumn the identification of the column that contains the values (1 means the first column, 2 the second, etc.)
     * @param valueClass the class of the values
     * @return a map of key-value pairs
     * @throws SQLException if there was an error reading values from the result set
     */
    protected final <K, V> Map<K, V> resultSetToMap(ResultSet rs, int keyColumn, Class<? extends K> keyClass, int valueColumn, Class<? extends V> valueClass) throws SQLException {
        Map<K, V> ret = new HashMap<K, V>();
        while (rs.next())
            ret.put(keyClass.cast(rs.getObject(keyColumn)), valueClass.cast(rs.getObject(valueColumn)));
        return ret;
    }

    @Override
    public String toString() {
        return "ExtendedDatabaseConnection for " + dbConnUrl;
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
         * @param lazyConnection flag whether to open the database connection immediately (<tt>false</tt>)
         *          or when a first command is executed (<tt>true</tt>)
         * @throws IllegalArgumentException if the connection url is <tt>null</tt> or the driver class cannot be registered
         * @throws SQLException if there was a problem connecting to the database
         */
        public ExtendedDatabaseConnectionPublic(String dbConnUrl, Properties dbConnInfo, String dbDriverClass, boolean lazyConnection) throws IllegalArgumentException, SQLException {
            super(dbConnUrl, dbConnInfo, dbDriverClass, lazyConnection);
        }

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
         * @param lazyConnection flag whether to open the database connection immediately (<tt>false</tt>)
         *          or when a first command is executed (<tt>true</tt>)
         * @throws SQLException if there was a problem connecting to the database
         */
        public ExtendedDatabaseConnectionPublic(ExtendedDatabaseConnectionPublic sourceConnection, boolean lazyConnection) throws SQLException {
            super(sourceConnection, lazyConnection);
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
         * @param returnGeneratedKeys flag whether to set the {@link Statement#RETURN_GENERATED_KEYS} on the prepared statement
         * @param parameters the values for the SQL parameters (denoted by "?" chars in the SQL command)
         * @return an executed prepared statement
         * @throws SQLException if there was an unrecoverable error when parsing or executing the SQL command
         */
        public PreparedStatement prepareAndExecuteStatement(PreparedStatement statement, String sql, boolean returnGeneratedKeys, Object... parameters) throws SQLException {
            return super.prepareAndExecute(statement, sql, returnGeneratedKeys, parameters);
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

        /**
        * Executes a data modification SQL command (i.e. INSERT, UPDATE or DELETE).
        * The returned value is the first auto-generated column if {@code generatedKeys}
        * is <tt>true</tt> or the number of affected rows {@link PreparedStatement#getUpdateCount()} if available.
        * @param sql the SQL command to execute
        * @param returnGeneratedKeys the flag whether to retrieve the keys generated automatically
        *          by the database (e.g. when inserting)
        * @param parameters the values for the parameters in the SQL statement;
        *          there must be one value for every ? in the SQL statement
        * @return the first auto-generated column or the number of affected rows
        * @throws NoSuchElementException if the SQL command does not return any row
        * @throws SQLException if there was a problem parsing or executing the SQL command
        */
        public Object executeDataManipulationSQL(String sql, boolean returnGeneratedKeys, Object... parameters) throws NoSuchElementException, SQLException {
            return super.executeDataManipulation(sql, returnGeneratedKeys, parameters);
        }
    }

}
