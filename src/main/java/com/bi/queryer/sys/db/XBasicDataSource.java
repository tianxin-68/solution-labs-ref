package com.bi.queryer.sys.db;

import java.sql.DriverManager;
import java.sql.Driver;
import java.sql.SQLException;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringUtils;

public class XBasicDataSource extends BasicDataSource {

    @Override
    public synchronized void close() throws SQLException {
        SQLException closeException = null;

        try {
            String url = this.getUrl();

            if (StringUtils.isNotBlank(url)) {
                try {
                    Driver driver = DriverManager.getDriver(url);

                    if (driver != null) {
                        DriverManager.deregisterDriver(driver);
                    }
                } catch (SQLException e) {
                    // Ignore shutdown-time deregistration failures when no matching driver is registered.
                }
            }

            super.close();
        } catch (SQLException e) {
            closeException = e;
        }

        if (closeException != null) {
            throw closeException;
        }
    }
}
