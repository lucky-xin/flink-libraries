package org.apache.flink.dynamic.pattern;

import java.util.List;

/**
 * This manager handles updated pattern processors and manages current pattern processors.
 *
 * @param <T> Base type of the elements appearing in the pattern.
 */
public interface PatternProcessorManager<T> {

    /**
     * Deals with the notification that pattern processors are updated.
     *
     * @param patternProcessors The updated pattern processors.
     */
    void onPatternProcessorsUpdated(List<DynamicPattern<T>> patternProcessors);
}
