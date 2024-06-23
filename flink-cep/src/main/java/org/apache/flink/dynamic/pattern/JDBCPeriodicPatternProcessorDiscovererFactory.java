package org.apache.flink.dynamic.pattern;

import javax.annotation.Nullable;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * The JDBC implementation of the {@link PeriodicPatternProcessorDiscovererFactory} that creates the
 * {@link JDBCPeriodicPatternProcessorDiscoverer} instance.
 *
 * @param <T> Base type of the elements appearing in the pattern.
 */
public class JDBCPeriodicPatternProcessorDiscovererFactory<T>
        extends PeriodicPatternProcessorDiscovererFactory<T> {

    private final String jdbcUrl;
    private final String jdbcDriver;
    private final String tableName;

    public JDBCPeriodicPatternProcessorDiscovererFactory(
            final String jdbcUrl,
            final String jdbcDriver,
            final String tableName,
            @Nullable final List<DynamicPattern<T>> initialPatternProcessors,
            @Nullable final Long intervalMillis) {
        super(initialPatternProcessors, intervalMillis);
        this.jdbcUrl = requireNonNull(jdbcUrl);
        this.jdbcDriver = requireNonNull(jdbcDriver);
        this.tableName = requireNonNull(tableName);
    }

    @Override
    public PatternDiscoverer<T> createPatternProcessorDiscoverer(
            ClassLoader userCodeClassLoader) throws Exception {
        return new JDBCPeriodicPatternProcessorDiscoverer<>(
                jdbcUrl,
                jdbcDriver,
                tableName,
                userCodeClassLoader,
                this.getInitialPatternProcessors(),
                getIntervalMillis());
    }
}
