package org.jabref.logic.shared;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBMSConnection implements DatabaseConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBMSConnection.class);

    private final Connection connection;
    private final DBMSConnectionProperties properties;

    public DBMSConnection(DBMSConnectionProperties connectionProperties) throws SQLException, InvalidDBMSConnectionPropertiesException {
        if (!connectionProperties.isValid()) {
            throw new InvalidDBMSConnectionPropertiesException();
        }
        this.properties = connectionProperties;

        try {
            DriverManager.setLoginTimeout(3);
            // ensure that all SQL drivers are loaded - source: http://stackoverflow.com/a/22384826/873282
            // we use the side effect of getAvailableDBMSTypes() - it loads all available drivers
            DBMSConnection.getAvailableDBMSTypes();

            if (connectionProperties.isUseExpertMode()) {
                this.connection = DriverManager.getConnection(connectionProperties.getJdbcUrl(), connectionProperties.asProperties());
            } else {
                this.connection = DriverManager.getConnection(connectionProperties.getUrl(), connectionProperties.asProperties());
            }
        } catch (SQLException e) {
            // Some systems like PostgreSQL retrieves 0 to every exception.
            // Therefore a stable error determination is not possible.
            LOGGER.error("Could not connect to database: {} - Error code: {}", e.getMessage(), e.getErrorCode(), e);
            throw e;
        }
    }

    @Override
    public Connection getConnection() {
        return this.connection;
    }

    @Override
    public DBMSConnectionProperties getProperties() {
        return this.properties;
    }

    /**
     * Returns a Set of {@link DBMSType} which is supported by available drivers.
     */
    public static Set<DBMSType> getAvailableDBMSTypes() {
        Set<DBMSType> dbmsTypes = new HashSet<>();

        for (DBMSType dbms : DBMSType.values()) {
            try {
                Class.forName(dbms.getDriverClassPath());
                dbmsTypes.add(dbms);
            } catch (ClassNotFoundException e) {
                // In case that the driver is not available do not perform tests for this system.
                LOGGER.info(Localization.lang("%0 driver not available.", dbms.toString()));
            }
        }
        return dbmsTypes;
    }
}
