package org.apache.flink.table.examples.condition;

import org.apache.flink.cep.pattern.conditions.SimpleCondition;
import org.apache.flink.table.examples.event.Event;

public class EndCondition extends SimpleCondition<Event> {

    @Override
    public boolean filter(Event value) throws Exception {
        return value.getAction() != 1;
    }
}
