package org.apache.flink.dynamic.impl.json.spec;

import lombok.Getter;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.WithinType;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.util.Map;

/**
 * This class is to (de)serialize WindowTime of a {@link Pattern} in json format.
 */
@Getter
public class WindowSpec {
    private final WithinType type;

    private final Duration duration;

    public WindowSpec(@JsonProperty("type") WithinType type, @JsonProperty("duration") Duration duration) {
        this.type = type;
        this.duration = duration;
    }

    public static WindowSpec fromWindowTime(Map<WithinType, Duration> window) {
        if (window.containsKey(WithinType.FIRST_AND_LAST)) {
            return new WindowSpec(WithinType.FIRST_AND_LAST, window.get(WithinType.FIRST_AND_LAST));
        } else {
            return new WindowSpec(WithinType.PREVIOUS_AND_CURRENT, window.get(WithinType.FIRST_AND_LAST));
        }
    }
}
