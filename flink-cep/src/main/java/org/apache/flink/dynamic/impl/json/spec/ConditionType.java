package org.apache.flink.dynamic.impl.json.spec;


import lombok.Getter;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;

import java.util.HashMap;
import java.util.Map;

/**
 * The type of {@link IterativeCondition } for dynamic cep.
 */
@Getter
public enum ConditionType {
    /**
     *
     */
    CLASS("CLASS"),
    AVIATOR("AVIATOR"),
    MVEL("MVEL"),
    ;

    private final String type;
    private static final Map<String, ConditionType> TYPE_MAP;

    ConditionType(String type) {
        this.type = type;
    }

    static {
        TYPE_MAP = new HashMap<>();
        for (ConditionType instance : ConditionType.values()) {
            TYPE_MAP.put(instance.type, instance);
        }
    }

    public static ConditionType get(String type) {
        return TYPE_MAP.get(type);
    }
}
