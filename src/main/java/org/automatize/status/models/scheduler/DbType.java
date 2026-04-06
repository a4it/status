package org.automatize.status.models.scheduler;

/**
 * Supported relational database types for the scheduler JDBC datasource.
 * Each constant carries driver class, URL template, and default port information.
 */
public enum DbType {

    POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql://%s:%d/%s", 5432),
    MYSQL("com.mysql.cj.jdbc.Driver", "jdbc:mysql://%s:%d/%s", 3306),
    MARIADB("org.mariadb.jdbc.Driver", "jdbc:mariadb://%s:%d/%s", 3306),
    H2("org.h2.Driver", "jdbc:h2:tcp://%s:%d/%s", 9092);

    private final String driverClass;
    private final String urlTemplate;
    private final int defaultPort;

    DbType(String driverClass, String urlTemplate, int defaultPort) {
        this.driverClass = driverClass;
        this.urlTemplate = urlTemplate;
        this.defaultPort = defaultPort;
    }

    /**
     * Returns the fully-qualified JDBC driver class name for this database type.
     *
     * @return the driver class name
     */
    public String getDriverClass() {
        return driverClass;
    }

    /**
     * Returns the default TCP port used by this database engine.
     *
     * @return the default port number
     */
    public int getDefaultPort() {
        return defaultPort;
    }

    /**
     * Builds a standard JDBC URL for the given host, port, and database name.
     *
     * @param host the database host name or IP address
     * @param port the TCP port
     * @param db   the database / catalogue name
     * @return a formatted JDBC URL string
     */
    public String buildJdbcUrl(String host, int port, String db) {
        return String.format(urlTemplate, host, port, db);
    }
}
