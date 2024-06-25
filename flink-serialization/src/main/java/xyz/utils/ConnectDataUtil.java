package xyz.utils;

import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Values;

/**
 * ConnectDataUtil
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-06-25
 */
public class ConnectDataUtil {

    @SuppressWarnings("unchecked")
    public static <T> T copy(Object val, Schema dstSchema) {
        if (dstSchema == null || val == null) {
            return null;
        }
        switch (dstSchema.type()) {
            case BOOLEAN:
                return (T) Values.convertToBoolean(dstSchema, val);
            case INT8:
                return (T) Values.convertToByte(dstSchema, val);
            case INT16:
                return (T) Values.convertToShort(dstSchema, val);
            case INT32:
                return (T) Values.convertToInteger(dstSchema, val);
            case INT64:
                return (T) Values.convertToLong(dstSchema, val);
            case FLOAT32:
                return (T) Values.convertToFloat(dstSchema, val);
            case FLOAT64:
                return (T) Values.convertToDouble(dstSchema, val);
            case STRING:
                return (T) Values.convertToString(dstSchema, val);
            case BYTES:
                return (T) Values.convertToString(dstSchema, val);
            case STRUCT:
                Struct surStruct = (Struct) val;
                Struct dstStruct = new Struct(dstSchema);
                for (Field field : dstSchema.fields()) {
                    Object value = copy(surStruct.get(field.name()), field.schema());
                    dstStruct.put(field, value);
                }
                return (T) val;
            default:
        }
        return null;
    }
}
