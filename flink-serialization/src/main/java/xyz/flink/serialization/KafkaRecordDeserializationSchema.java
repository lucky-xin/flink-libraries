package xyz.flink.serialization;

import lombok.AllArgsConstructor;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import xyz.flink.model.IKafkaRecord;

/**
 * KafkaRecordDeserializationSchema
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-06-22
 */
@AllArgsConstructor
public class KafkaRecordDeserializationSchema<K, V extends IKafkaRecord> implements KafkaDeserializationSchema<V> {
    private static final long serialVersionUID = 2651665280744549932L;

    private final DeserializationSchema<K> keyDeserializationSchema;
    private final DeserializationSchema<V> valueDeserializationSchema;

    @Override
    public void open(DeserializationSchema.InitializationContext context) throws Exception {
        this.keyDeserializationSchema.open(context);
        this.valueDeserializationSchema.open(context);
    }

    @Override
    public V deserialize(ConsumerRecord<byte[], byte[]> cr) throws Exception {
        V v = this.valueDeserializationSchema.deserialize(cr.value());
        if (v == null) {
            return null;
        }
        v.setOffset(cr.offset());
        v.setPartition(cr.partition());
        K k = this.keyDeserializationSchema.deserialize(cr.key());
        if (k != null) {
            v.setKey(k);
        }
        return v;
    }

    @Override
    public boolean isEndOfStream(V nextElement) {
        return this.valueDeserializationSchema.isEndOfStream(nextElement);
    }

    @Override
    public TypeInformation<V> getProducedType() {
        return this.valueDeserializationSchema.getProducedType();
    }
}
