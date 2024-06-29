package org.apache.flink.streaming.test.examples.parquet;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.ConfigurationUtils;
import org.apache.flink.connector.file.src.FileSource;
import org.apache.flink.connector.file.src.reader.StreamFormat;
import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.parquet.avro.AvroParquetReaders;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.junit.jupiter.api.Test;

import java.util.Properties;

/**
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-06-27
 */
class ParquetTest {

    @Test
    void test() {
        Schema schema = null;
        StreamFormat<GenericRecord> streamFormat = AvroParquetReaders.forGenericRecord(schema);
        String s3Path = "s3a://yourbucket/yourkey/";
        Path fp = new Path(s3Path);
        Properties props = new Properties();
        props.put("s3.access-key", "test");
        props.put("s3.secret-key", "test");
        props.put("s3.endpoint", "http://localstack:4566");
        props.put("s3.endpoint.region", "us-east-1");
        props.put("s3.path.style.access", "true");
        Configuration configuration = ConfigurationUtils.createConfiguration(props);
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(configuration);
        // Parquet rows are decoded in batches
        FileSource.FileSourceBuilder<GenericRecord> fileSourceBuilder = FileSource.forRecordStreamFormat(streamFormat, fp);
        FileSource<GenericRecord> fileSource = fileSourceBuilder.build();
        DataStreamSource<GenericRecord> stream = env.fromSource(
                fileSource,
                WatermarkStrategy.noWatermarks(),
                "Parquet Source"
        );

    }
}
