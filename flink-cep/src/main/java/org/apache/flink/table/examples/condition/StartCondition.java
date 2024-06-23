package org.apache.flink.table.examples.condition;


import org.apache.flink.dynamic.condition.AviatorCondition;
import org.apache.flink.table.examples.event.Event;

public class StartCondition extends AviatorCondition<Event> {

    public StartCondition(String expression) {
        super(expression);
    }
}
