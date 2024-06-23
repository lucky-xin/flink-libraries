package org.apache.flink.dynamic.pattern;

import java.io.Serializable;

/**
 * A factory for {@link PatternDiscoverer}. The created {@link PatternDiscoverer}
 * should notify {@link PatternProcessorManager} to deal with the pattern processor updates.
 *
 * @param <T> Base type of the elements appearing in the pattern.
 */
public interface PatternProcessorDiscovererFactory<T> extends Serializable {
    /**
     * Creates a {@link PatternDiscoverer}.
     *
     * @param userCodeClassloader used to create PatternProcessor objects dynamically
     * @return A {@link PatternDiscoverer} instance.
     */
    PatternDiscoverer<T> createPatternProcessorDiscoverer(ClassLoader userCodeClassloader) throws Exception;
}
