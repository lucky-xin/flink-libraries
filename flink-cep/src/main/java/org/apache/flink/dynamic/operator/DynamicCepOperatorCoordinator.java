package org.apache.flink.dynamic.operator;

import org.apache.flink.dynamic.pattern.PatternProcessorDiscovererFactory;
import org.apache.flink.runtime.jobgraph.OperatorID;
import org.apache.flink.runtime.operators.coordination.CoordinationRequest;
import org.apache.flink.runtime.operators.coordination.CoordinationRequestHandler;
import org.apache.flink.runtime.operators.coordination.CoordinationResponse;
import org.apache.flink.runtime.operators.coordination.OperatorCoordinator;
import org.apache.flink.runtime.operators.coordination.OperatorEvent;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * DynamicOperatorCoordinator
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-06-20
 */
public class DynamicCepOperatorCoordinator implements OperatorCoordinator, CoordinationRequestHandler {
    @Override
    public void start() throws Exception {

    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public void handleEventFromOperator(int subtask, int attemptNumber, OperatorEvent event) throws Exception {

    }

    @Override
    public void checkpointCoordinator(long checkpointId, CompletableFuture<byte[]> resultFuture) throws Exception {

    }

    @Override
    public void notifyCheckpointComplete(long checkpointId) {

    }

    @Override
    public void notifyCheckpointAborted(long checkpointId) {
        OperatorCoordinator.super.notifyCheckpointAborted(checkpointId);
    }

    @Override
    public void resetToCheckpoint(long checkpointId, @Nullable byte[] checkpointData) throws Exception {

    }

    @Override
    public void subtaskReset(int subtask, long checkpointId) {

    }

    @Override
    public void executionAttemptFailed(int subtask, int attemptNumber, @Nullable Throwable reason) {

    }

    @Override
    public void executionAttemptReady(int subtask, int attemptNumber, SubtaskGateway gateway) {

    }

    @Override
    public CompletableFuture<CoordinationResponse> handleCoordinationRequest(CoordinationRequest request) {
        return null;
    }

    public static class Provider<T> implements OperatorCoordinator.Provider {
        private String operatorName;
        private OperatorID operatorID;
        private PatternProcessorDiscovererFactory<T> discovererFactory;

        public Provider(String operatorName,
                        OperatorID operatorID,
                        PatternProcessorDiscovererFactory<T> discovererFactory
        ) {
            this.operatorName = operatorName;
            this.operatorID = operatorID;
            this.discovererFactory = discovererFactory;
        }

        @Override
        public OperatorID getOperatorId() {
            return this.operatorID;
        }

        @Override
        public OperatorCoordinator create(OperatorCoordinator.Context context) throws Exception {
            return null;
        }
    }
}
