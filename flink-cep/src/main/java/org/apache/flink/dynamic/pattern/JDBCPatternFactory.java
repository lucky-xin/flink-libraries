package org.apache.flink.dynamic.pattern;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Builder;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.dynamic.impl.json.util.CepConverter;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

/**
 * PatternFactory
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-06-19
 */
@Builder
public class JDBCPatternFactory<T, F extends T> implements PatternFactory<T, F> {

    /**
     * 数据库连接
     */
    private final String url;
    /**
     * 驱动
     */
    private final String driverName;
    /**
     * 用户名
     */
    private final String username;
    /**
     * 密码
     */
    private final String password;
    /**
     * 规则表
     */
    private final String tableName;
    /**
     * 规则id
     */
    private final Long pid;

    private DataSource ds;

    @Override
    public Pattern<T, F> create() throws SQLException {
        tryInit();
        try (Connection connection = ds.getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT * FROM " + tableName + " WHERE id = ?")) {
            stmt.setLong(1, pid);
            try (ResultSet rs = stmt.executeQuery();) {
                if (rs.next()) {
                    return CepConverter.toPattern(requireNonNull(rs.getString("pattern")));
                }
            }
        }
        return null;
    }

    @Override
    public void close() throws IOException {

    }

    private void tryInit() {
        if (ds == null) {
            this.ds = createDataSource();
        }
    }

    private DataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setIdleTimeout(5000);
        config.setMaximumPoolSize(5000);
        config.setDriverClassName(driverName);
        config.setMinimumIdle(500);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        return new HikariDataSource(config);
    }
}
