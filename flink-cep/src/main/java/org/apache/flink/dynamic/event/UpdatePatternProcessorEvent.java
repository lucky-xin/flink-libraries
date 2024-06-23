package org.apache.flink.dynamic.event;

import org.apache.flink.dynamic.processor.PatternProcessor;
import org.apache.flink.runtime.operators.coordination.OperatorEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A CEP event that updates pattern processors.
 *
 * @param <T> Base type of the elements appearing in the pattern.
 */
public class UpdatePatternProcessorEvent<T> implements OperatorEvent {

    private static final long serialVersionUID = 1L;
    private final List<PatternProcessor<T>> patternProcessors;

    public UpdatePatternProcessorEvent(List<PatternProcessor<T>> patternProcessors)
            throws IOException {
        this.patternProcessors = new ArrayList<>();
        this.patternProcessors.addAll(patternProcessors);
    }

    public List<PatternProcessor<T>> patternProcessors() {
        return patternProcessors;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (PatternProcessor<T> patternProcessor : patternProcessors) {
            stringBuilder.append(patternProcessor.toString()).append("; ");
        }

        return String.format("UpdatePatternProcessorEvent[patternProcessors=%s]", stringBuilder);
    }
}
