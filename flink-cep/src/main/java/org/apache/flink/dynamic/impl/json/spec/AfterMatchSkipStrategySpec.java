package org.apache.flink.dynamic.impl.json.spec;

import org.apache.flink.cep.nfa.aftermatch.AfterMatchSkipStrategy;
import org.apache.flink.cep.nfa.aftermatch.NoSkipStrategy;
import org.apache.flink.cep.nfa.aftermatch.SkipPastLastStrategy;
import org.apache.flink.cep.nfa.aftermatch.SkipToFirstStrategy;
import org.apache.flink.cep.nfa.aftermatch.SkipToLastStrategy;
import org.apache.flink.cep.nfa.aftermatch.SkipToNextStrategy;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * The util class to (de)serialize {@link AfterMatchSkipStrategy } in json format.
 */
public class AfterMatchSkipStrategySpec {
    private static final Map<String, String> MAP = new HashMap<>();
    private final AfterMatchSkipStrategyType type;
    private final @Nullable String patternName;

    static {
        MAP.put(NoSkipStrategy.class.getCanonicalName(), AfterMatchSkipStrategyType.NO_SKIP.name());
        MAP.put(SkipToNextStrategy.class.getCanonicalName(), AfterMatchSkipStrategyType.SKIP_TO_NEXT.name());
        MAP.put(SkipPastLastStrategy.class.getCanonicalName(), AfterMatchSkipStrategyType.SKIP_PAST_LAST_EVENT.name());
        MAP.put(SkipToFirstStrategy.class.getCanonicalName(), AfterMatchSkipStrategyType.SKIP_TO_FIRST.name());
        MAP.put(SkipToLastStrategy.class.getCanonicalName(), AfterMatchSkipStrategyType.SKIP_TO_LAST.name());
    }

    public AfterMatchSkipStrategySpec(
            @JsonProperty("type") AfterMatchSkipStrategyType type,
            @JsonProperty("patternName") @Nullable String patternName) {
        this.type = type;
        this.patternName = patternName;
    }

    public AfterMatchSkipStrategyType getType() {
        return type;
    }

    @Nullable
    public String getPatternName() {
        return patternName;
    }

    public static AfterMatchSkipStrategySpec fromAfterMatchSkipStrategy(
            AfterMatchSkipStrategy afterMatchSkipStrategy) {
        return new AfterMatchSkipStrategySpec(
                AfterMatchSkipStrategyType.valueOf(
                        MAP.get(
                                afterMatchSkipStrategy.getClass().getCanonicalName())),
                afterMatchSkipStrategy.getPatternName().orElse(null));
    }

    public AfterMatchSkipStrategy toAfterMatchSkipStrategy() {
        switch (this.type) {
            case NO_SKIP:
                return NoSkipStrategy.noSkip();
            case SKIP_TO_LAST:
                return SkipToLastStrategy.skipToLast(this.getPatternName());
            case SKIP_TO_NEXT:
                return SkipToNextStrategy.skipToNext();
            case SKIP_TO_FIRST:
                return SkipToFirstStrategy.skipToFirst(this.getPatternName());
            case SKIP_PAST_LAST_EVENT:
                return SkipPastLastStrategy.skipPastLastEvent();
            default:
                throw new IllegalStateException(
                        "The type of the AfterMatchSkipStrategySpec: "
                                + this.type
                                + " is invalid!");
        }
    }

    /**
     * The enum class to help serialization of AfterMatchSkipStrategy.
     */
    public enum AfterMatchSkipStrategyType {
        /**
         * NoSkipStrategy
         */
        NO_SKIP(NoSkipStrategy.class.getCanonicalName()),
        SKIP_TO_NEXT(SkipToNextStrategy.class.getCanonicalName()),
        SKIP_PAST_LAST_EVENT(SkipPastLastStrategy.class.getCanonicalName()),
        SKIP_TO_FIRST(SkipToFirstStrategy.class.getCanonicalName()),
        SKIP_TO_LAST(SkipToLastStrategy.class.getCanonicalName());

        public final String className;

        AfterMatchSkipStrategyType(String className) {
            this.className = className;
        }
    }
}
