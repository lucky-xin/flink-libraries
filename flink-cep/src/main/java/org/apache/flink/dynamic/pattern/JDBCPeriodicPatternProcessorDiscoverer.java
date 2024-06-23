package org.apache.flink.dynamic.pattern;

import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.cep.functions.PatternProcessFunction;
import org.apache.flink.util.CollectionUtil;
import org.apache.flink.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * The JDBC implementation of the {@link PeriodicPatternDiscoverer} that periodically
 * discovers the rule updates from the database by using JDBC.
 *
 * @param <T> Base type of the elements appearing in the pattern.
 */
public class JDBCPeriodicPatternProcessorDiscoverer<T> extends PeriodicPatternDiscoverer<T> {

    private static final Logger LOG =
            LoggerFactory.getLogger(JDBCPeriodicPatternProcessorDiscoverer.class);

    private final String tableName;
    private final String jdbcUrl;
    private final List<DynamicPattern<T>> initialPatternProcessors;
    private final ClassLoader userCodeClassLoader;
    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;
    private Map<String, Tuple4<Long, Integer, String, String>> latestPatternProcessors;

    /**
     * Creates a new using the given initial {@link DynamicPattern} and the time interval how
     * often to check the pattern processor updates.
     *
     * @param jdbcUrl                  The JDBC url of the database.
     * @param jdbcDriver               The JDBC driver of the database.
     * @param initialPatternProcessors The list of the initial {@link DynamicPattern}.
     * @param intervalMillis           Time interval in milliseconds how often to check updates.
     */
    public JDBCPeriodicPatternProcessorDiscoverer(
            final String jdbcUrl,
            final String jdbcDriver,
            final String tableName,
            final ClassLoader userCodeClassLoader,
            @Nullable final List<DynamicPattern<T>> initialPatternProcessors,
            @Nullable final Long intervalMillis)
            throws Exception {
        super(intervalMillis);
        this.tableName = requireNonNull(tableName);
        this.initialPatternProcessors = initialPatternProcessors;
        this.userCodeClassLoader = userCodeClassLoader;
        this.jdbcUrl = jdbcUrl;
        Class.forName(requireNonNull(jdbcDriver));
        this.connection = DriverManager.getConnection(requireNonNull(jdbcUrl));
        this.statement = this.connection.createStatement();
    }

    @Override
    public boolean arePatternProcessorsUpdated() {
        if (latestPatternProcessors == null
                && !CollectionUtil.isNullOrEmpty(initialPatternProcessors)) {
            return true;
        }

        if (statement == null) {
            return false;
        }
        try {
            resultSet = statement.executeQuery("SELECT * FROM " + tableName);
            Map<String, Tuple4<Long, Integer, String, String>> currentPatternProcessors =
                    new HashMap<>();
            while (resultSet.next()) {
                String id = resultSet.getString("id");
                if (currentPatternProcessors.containsKey(id)
                        && currentPatternProcessors.get(id).f1 >= resultSet.getInt("version")) {
                    continue;
                }
                currentPatternProcessors.put(
                        id,
                        new Tuple4<>(
                                requireNonNull(resultSet.getLong("id")),
                                resultSet.getInt("version"),
                                requireNonNull(resultSet.getString("pattern")),
                                resultSet.getString("function")));
            }
            if (latestPatternProcessors == null
                    || isPatternProcessorUpdated(currentPatternProcessors)) {
                latestPatternProcessors = currentPatternProcessors;
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            LOG.warn(
                    "Pattern processor discoverer failed to check rule changes, will recreate connection - "
                            + e.getMessage());
            try {
                statement.close();
                connection.close();
                connection = DriverManager.getConnection(requireNonNull(this.jdbcUrl));
                statement = connection.createStatement();
            } catch (SQLException ex) {
                throw new RuntimeException("Cannot recreate connection to database.");
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<DynamicPattern<T>> getLatestPatternProcessors() throws Exception {
        return latestPatternProcessors.values().stream()
                .map(
                        patternProcessor -> {
                            try {
                                String patternStr = patternProcessor.f2;
                                PatternProcessFunction<T, ?> patternProcessFunction = null;
                                Long id = patternProcessor.f0;
                                int version = patternProcessor.f1;
                                if (!StringUtils.isNullOrWhitespaceOnly(patternProcessor.f3)) {
                                    patternProcessFunction =
                                            (PatternProcessFunction<T, ?>)
                                                    this.userCodeClassLoader
                                                            .loadClass(patternProcessor.f3)
                                                            .getConstructor(Long.class, int.class)
                                                            .newInstance(id, version);
                                }
                                return new DefaultDynamicPattern<>(
                                        patternProcessor.f0,
                                        patternProcessor.f1,
                                        patternStr,
                                        patternProcessFunction,
                                        this.userCodeClassLoader);
                            } catch (Exception e) {
                                LOG.error("Get the latest pattern processors of the discoverer failure. - ", e);
                            }
                            return null;
                        })
                .collect(Collectors.toList());
    }

    @Override
    public void close() throws IOException {
        super.close();
        try {
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (SQLException e) {
            LOG.warn("ResultSet of the pattern processor discoverer couldn't be closed", e);
        } finally {
            resultSet = null;
        }
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            LOG.warn("Statement of the pattern processor discoverer couldn't be closed", e);
        } finally {
            statement = null;
        }
    }

    private boolean isPatternProcessorUpdated(
            Map<String, Tuple4<Long, Integer, String, String>> currentPatternProcessors) {
        return latestPatternProcessors.size() != currentPatternProcessors.size()
                || !currentPatternProcessors.equals(latestPatternProcessors);
    }
}