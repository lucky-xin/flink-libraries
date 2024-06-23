package org.apache.flink.dynamic.processor;

import org.apache.flink.cep.functions.PatternProcessFunction;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.core.io.Versioned;

import java.io.Serializable;

/**
 * Base class for a pattern processor definition.
 *
 * <p>A pattern processor defines a certain {@link Pattern}, how to match the pattern, and how to
 * process the found matches.
 *
 * @param <IN> Base type of the elements appearing in the pattern.
 */
public interface PatternProcessor<IN> extends Serializable, Versioned {

    /**
     * Returns the ID of the pattern processor.
     *
     * @return The ID of the pattern processor.
     */
    String getId();

    /**
     * Returns the scheduled time at which the pattern processor should take effective.
     *
     * <p>If the scheduled time is earlier than current event/processing time, the pattern processor
     * will immediately become effective.
     *
     * <p>If the pattern processor should always become effective immediately, the scheduled time
     * can be set to {@code Long.MIN_VALUE}: {@value Long#MIN_VALUE}.
     *
     * @return The scheduled time.
     */
    default Long getTimestamp() {
        return Long.MIN_VALUE;
    }

    /**
     * Returns the {@link Pattern} to be matched.
     *
     * @return The pattern of the pattern processor.
     */
    Pattern<IN, ?> getPattern(ClassLoader classLoader);

    /**
     * Returns the {@link PatternProcessFunction} to process the found matches for the pattern.
     *
     * @return The pattern process function of the pattern processor.
     */
    PatternProcessFunction<IN, ?> getPatternProcessFunction();
}
