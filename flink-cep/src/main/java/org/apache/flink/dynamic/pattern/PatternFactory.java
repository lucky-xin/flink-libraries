package org.apache.flink.dynamic.pattern;

import org.apache.flink.cep.pattern.Pattern;

import java.io.Closeable;
import java.sql.SQLException;

/**
 * PatternFactory
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-06-19
 */
public interface PatternFactory<T, F extends T> extends Closeable {

    /**
     * create pattern
     *
     * @return
     */
    Pattern<T, F> create() throws SQLException;
}
