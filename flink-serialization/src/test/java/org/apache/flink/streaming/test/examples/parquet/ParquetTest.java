package org.apache.flink.streaming.test.examples.parquet;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.flink.connector.file.src.FileSource;
import org.apache.flink.connector.file.src.reader.StreamFormat;
import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.parquet.avro.AvroParquetReaders;
import org.junit.jupiter.api.Test;

/**
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-06-27
 */
public class ParquetTest {

    @Test
    public void test() {
        Schema schema = null;
        StreamFormat<GenericRecord> streamFormat = AvroParquetReaders.forGenericRecord(schema);
        String s3Path = "s3a://yourbucket/yourkey/";
        Path fp = new Path(s3Path);
        // Parquet rows are decoded in batches
        FileSource.FileSourceBuilder<GenericRecord> fs = FileSource.forRecordStreamFormat(streamFormat, fp);
    }
}
