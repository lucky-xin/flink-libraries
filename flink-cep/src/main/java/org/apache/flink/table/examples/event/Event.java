package org.apache.flink.table.examples.event;

import lombok.Data;

@Data
/** Exemplary event for usage in tests of CEP. */
public class Event {
    private final int id;
    private final String name;

    private final int productionId;
    private final int action;
    private final long eventTime;

    public Event(int id, String name, int action, int productionId, long timestamp) {
        this.id = id;
        this.name = name;
        this.action = action;
        this.productionId = productionId;
        this.eventTime = timestamp;
    }

    public static Event fromString(String eventStr) {
        String[] split = eventStr.split(",");
        return new Event(
                Integer.parseInt(split[0]),
                split[1],
                Integer.parseInt(split[2]),
                Integer.parseInt(split[3]),
                Long.parseLong(split[4]));
    }
}
