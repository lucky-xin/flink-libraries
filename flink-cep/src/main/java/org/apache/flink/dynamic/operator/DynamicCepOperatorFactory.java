package org.apache.flink.dynamic.operator;

import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.dynamic.pattern.PatternProcessorDiscovererFactory;
import org.apache.flink.runtime.jobgraph.OperatorID;
import org.apache.flink.runtime.operators.coordination.OperatorCoordinator;
import org.apache.flink.streaming.api.operators.AbstractStreamOperatorFactory;
import org.apache.flink.streaming.api.operators.CoordinatedOperatorFactory;
import org.apache.flink.streaming.api.operators.OneInputStreamOperatorFactory;
import org.apache.flink.streaming.api.operators.StreamOperator;
import org.apache.flink.streaming.api.operators.StreamOperatorParameters;
import org.apache.flink.streaming.runtime.tasks.ProcessingTimeServiceAware;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * The Factory class for {@link DynamicCepOperator}.
 */
public class DynamicCepOperatorFactory<IN, OUT> extends AbstractStreamOperatorFactory<OUT>
        implements OneInputStreamOperatorFactory<IN, OUT>,
        CoordinatedOperatorFactory<OUT>,
        ProcessingTimeServiceAware {

    private static final long serialVersionUID = 1L;

    private final PatternProcessorDiscovererFactory<IN> discovererFactory;

    private final TypeSerializer<IN> inputSerializer;

    private final TimeBehaviour timeBehaviour;

    public DynamicCepOperatorFactory(
            final PatternProcessorDiscovererFactory<IN> discovererFactory,
            final TypeSerializer<IN> inputSerializer,
            final TimeBehaviour timeBehaviour) {
        this.discovererFactory = checkNotNull(discovererFactory);
        this.inputSerializer = checkNotNull(inputSerializer);
        this.timeBehaviour = timeBehaviour;
    }

    @Override
    public <T extends StreamOperator<OUT>> T createStreamOperator(StreamOperatorParameters<OUT> parameters) {
        final OperatorID operatorId = parameters.getStreamConfig().getOperatorID();
        final DynamicCepOperator<IN, ?, OUT> dynamicCepOperator = new DynamicCepOperator<>(
                inputSerializer,
                timeBehaviour == TimeBehaviour.PROCESSING_TIME,
                parameters.getProcessingTimeService()
        );
        dynamicCepOperator.setup(
                parameters.getContainingTask(),
                parameters.getStreamConfig(),
                parameters.getOutput()
        );
        parameters.getOperatorEventDispatcher().registerEventHandler(operatorId, dynamicCepOperator);
        @SuppressWarnings("unchecked") final T castedOperator = (T) dynamicCepOperator;
        return castedOperator;
    }

    @Override
    public OperatorCoordinator.Provider getCoordinatorProvider(String operatorName, OperatorID operatorID) {
        return new DynamicCepOperatorCoordinator.Provider<>(operatorName, operatorID, discovererFactory);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class<? extends StreamOperator> getStreamOperatorClass(ClassLoader classLoader) {
        return DynamicCepOperator.class;
    }

    /**
     * The time behaviour enum defines how the system determines time for time-dependent order and
     * operations that depend on time.
     */
    public enum TimeBehaviour {
        /**
         * The system uses processing time.
         */
        PROCESSING_TIME,

        /**
         * The system uses event time.
         */
        EVENT_TIME
    }
}
