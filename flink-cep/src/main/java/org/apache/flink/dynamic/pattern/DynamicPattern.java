package org.apache.flink.dynamic.pattern;

import org.apache.flink.cep.functions.PatternProcessFunction;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.core.io.Versioned;

import java.io.Serializable;

/**
 * Base class for a pattern definition.
 *
 * <p>A pattern defines a certain {@link Pattern}, how to match the pattern, and how to process the found matches.
 *
 * @param <I> Base type of the elements appearing in the pattern.
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-06-19
 */
public interface DynamicPattern<I> extends Serializable, Versioned {

    /**
     * Returns the ID of the pattern .
     *
     * @return The ID of the pattern.
     */
    Long getId();

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
     * @return The pattern of the pattern .
     */
    <F extends I> Pattern<I, F> toPattern(ClassLoader classLoader);

    /**
     * Returns the {@link PatternProcessFunction} to process the found matches for the pattern.
     *
     * @return The pattern process function of the pattern .
     */
    PatternProcessFunction<I, ?> getPatternProcessFunction();
}
