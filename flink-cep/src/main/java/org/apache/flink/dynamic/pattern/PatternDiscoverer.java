package org.apache.flink.dynamic.pattern;

import java.io.Closeable;

/**
 * Interface that discovers pattern processor changes, notifies {@link PatternProcessorManager} of
 * pattern processor updates and provides the initial pattern processor.
 *
 * @param <T> Base type of the elements appearing in the pattern.
 */
public interface PatternDiscoverer<T> extends Closeable {

    /**
     * Discover the pattern processor changes.
     *
     * <p>In dynamic pattern processor changing case, this function should be a continuous process
     * to check pattern processor updates and notify the {@link PatternProcessorManager} of pattern
     * processor updates.
     */
    void discoverPatternProcessorUpdates(PatternProcessorManager<T> patternProcessorManager);
}
