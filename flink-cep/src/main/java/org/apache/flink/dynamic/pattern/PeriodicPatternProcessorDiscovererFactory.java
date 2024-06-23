package org.apache.flink.dynamic.pattern;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Implementation of the {@link PatternProcessorDiscovererFactory} that creates the {@link
 * PeriodicPatternDiscoverer} instance.
 *
 * @param <T> Base type of the elements appearing in the pattern.
 */
public abstract class PeriodicPatternProcessorDiscovererFactory<T>
        implements PatternProcessorDiscovererFactory<T> {

    @Nullable
    private final List<DynamicPattern<T>> initialPatternProcessors;
    private final Long intervalMillis;

    protected PeriodicPatternProcessorDiscovererFactory(
            @Nullable final List<DynamicPattern<T>> initialPatternProcessors,
            @Nullable final Long intervalMillis) {
        this.initialPatternProcessors = initialPatternProcessors;
        this.intervalMillis = intervalMillis;
    }

    @Nullable
    public List<DynamicPattern<T>> getInitialPatternProcessors() {
        return initialPatternProcessors;
    }

    @Override
    public abstract PatternDiscoverer<T> createPatternProcessorDiscoverer(
            ClassLoader userCodeClassLoader) throws Exception;

    public Long getIntervalMillis() {
        return intervalMillis;
    }
}
