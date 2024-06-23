package org.apache.flink.dynamic.impl.json.spec;


import lombok.Data;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.Quantifier.ConsumingStrategy;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Edge is used to describe the Event Selection Strategy between 2 Nodes(i.e {@link Pattern})
 * This class is to (de)serialize Edges in json format.
 */
@Data
public class EdgeSpec {
    private final String source;
    private final String target;
    private final ConsumingStrategy type;

    public EdgeSpec(
            @JsonProperty("source") String source,
            @JsonProperty("target") String target,
            @JsonProperty("type") ConsumingStrategy type) {
        this.source = source;
        this.target = target;
        this.type = type;
    }
}
