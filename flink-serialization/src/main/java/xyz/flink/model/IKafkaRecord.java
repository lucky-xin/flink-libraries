package xyz.flink.model;

import java.io.Serializable;

/**
 * Kafka 数据接口
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-06-21
 */
public interface IKafkaRecord extends Serializable {

    /**
     * 设置偏移量
     *
     * @param offset
     */
    default void setOffset(long offset) {

    }

    /**
     * 获取偏移量
     *
     * @return
     */
    default long getOffset() {
        return 0;
    }

    /**
     * 设置分区
     *
     * @param partition
     */
    default void setPartition(int partition) {

    }

    /**
     * 获取分区
     *
     * @return
     */
    default int getPartition() {
        return 0;
    }

    /**
     * 设置key
     *
     * @param key
     */
    default <K> void setKey(K key) {

    }
}
