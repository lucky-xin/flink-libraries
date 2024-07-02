package xyz.flink.data.gen;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.util.RandomUtil;
import com.mifmif.common.regex.Generex;
import io.confluent.connect.json.JsonSchemaData;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import org.apache.commons.math3.util.Precision;
import org.apache.kafka.connect.data.Decimal;
import org.apache.kafka.connect.data.Values;
import org.everit.json.schema.ArraySchema;
import org.everit.json.schema.BooleanSchema;
import org.everit.json.schema.CombinedSchema;
import org.everit.json.schema.ConstSchema;
import org.everit.json.schema.EmptySchema;
import org.everit.json.schema.EnumSchema;
import org.everit.json.schema.FalseSchema;
import org.everit.json.schema.FormatValidator;
import org.everit.json.schema.NotSchema;
import org.everit.json.schema.NullSchema;
import org.everit.json.schema.NumberSchema;
import org.everit.json.schema.ObjectSchema;
import org.everit.json.schema.ReferenceSchema;
import org.everit.json.schema.Schema;
import org.everit.json.schema.StringSchema;
import org.everit.json.schema.TrueSchema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

import static io.confluent.connect.json.JsonSchemaData.CONNECT_TYPE_PROP;

/**
 * JsonGenerator
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-06-25
 */
public class JsonDataGenerator {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DatePattern.UTC_MS_PATTERN);

    private final Schema schema;
    private final Random random;
    private final Map<Schema, Generex> generexCache = new HashMap<>();
    private static final Map<String, Function<Random, String>> FORMAT_REGEXES = new HashMap<>();
    private static final Map<Class<? extends Schema>, BiFunction<Schema, JsonDataGenerator, Object>> GENERATORS = new HashMap<>();

    static {
        FORMAT_REGEXES.put("date", random ->
                DateTimeFormatter.ISO_DATE.format(LocalDate.now())
        );
        FORMAT_REGEXES.put("date-time", random ->
                DATE_TIME_FORMATTER.format(LocalDateTime.now().atZone(ZoneId.systemDefault()))
        );
        FORMAT_REGEXES.put("time", random ->
                DateTimeFormatter.ISO_TIME.format(LocalTime.now())
        );
        FORMAT_REGEXES.put("email", random -> {
            Generex generex = new Generex(RegexPool.EMAIL, random);
            return generex.random();
        });
        FORMAT_REGEXES.put("host-name", random -> {
            Generex generex = new Generex("^[a-zA-Z0-9-]{2,10}\\.[a-z]{2,3}$", random);
            return generex.random();
        });
        FORMAT_REGEXES.put("hostname", random -> {
            Generex generex = new Generex("^[a-zA-Z0-9-]{2,10}\\.[a-z]{2,3}$", random);
            return generex.random();
        });
        FORMAT_REGEXES.put("idn-email", random -> {
            Generex generex = new Generex("^[a-zA-Z_.]{2,10}@[a-zA-Z0-9-]{2,10}\\.[a-z]{2,3}$", random);
            return generex.random();
        });
        FORMAT_REGEXES.put("idn-hostname", random -> {
            Generex generex = new Generex("^[a-zA-Z0-9-]{2,10}\\.[a-z]{2,3}$", random);
            return generex.random();
        });
        FORMAT_REGEXES.put("ip-address", random -> {
            Generex generex = new Generex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$", random);
            return generex.random();
        });
        FORMAT_REGEXES.put("ipv4", random -> {
            Generex generex = new Generex(RegexPool.IPV4, random);
            return generex.random();
        });
        FORMAT_REGEXES.put("ipv6", random -> {
            Generex generex = new Generex(RegexPool.IPV6, random);
            return generex.random();
        });
        FORMAT_REGEXES.put("iri", random -> {
            Generex generex = new Generex("^http:\\/\\/[a-zA-Z0-9_\\-]+\\.[a-zA-Z0-9_\\-]+\\.[a-zA-Z0-9_\\-]+$", random);
            return generex.random();
        });
        FORMAT_REGEXES.put("json-pointer", random -> {
            Generex generex = new Generex("^/[a-zA-Z0-9_/-]{2,40}$", random);
            return generex.random();
        });
        FORMAT_REGEXES.put("relative-json-pointer", random -> {
            Generex generex = new Generex("^\\d{4}/[a-zA-Z0-9_/-]{2,40}$", random);
            return generex.random();
        });
        FORMAT_REGEXES.put("uri", random -> {
            Generex generex = new Generex("^http:\\/\\/[a-zA-Z0-9_\\-]+\\.[a-zA-Z0-9_\\-]+\\.[a-zA-Z0-9_\\-]+$", random);
            return generex.random();
        });
        FORMAT_REGEXES.put("uri-reference", random -> {
            Generex generex = new Generex("^http:\\/\\/[a-zA-Z0-9_\\-]+\\.[a-zA-Z0-9_\\-]+\\.[a-zA-Z0-9_\\-]+$", random);
            return generex.random();
        });
        FORMAT_REGEXES.put("regex", random -> {
            Generex generex = new Generex("\\/([^()]*)?\\/([i|g|m]+)?", random);
            return generex.random();
        });
        FORMAT_REGEXES.put("uuid", random -> UUID.randomUUID().toString());

        GENERATORS.put(ArraySchema.class, (schema, generator) -> generator.generateArray((ArraySchema) schema));
        GENERATORS.put(BooleanSchema.class, (schema, generator) -> generator.generateBoolean((BooleanSchema) schema));
        GENERATORS.put(NumberSchema.class, (schema, generator) -> generator.generateNumber((NumberSchema) schema));
        GENERATORS.put(StringSchema.class, (schema, generator) -> generator.generateString((StringSchema) schema));
        GENERATORS.put(ConstSchema.class, (schema, generator) -> generator.generateConst((ConstSchema) schema));
        GENERATORS.put(EnumSchema.class, (schema, generator) -> generator.generateEnum((EnumSchema) schema));
        GENERATORS.put(FalseSchema.class, (schema, generator) -> generator.generateTrueOrFalse(schema));
        GENERATORS.put(TrueSchema.class, (schema, generator) -> generator.generateTrueOrFalse(schema));
        GENERATORS.put(NotSchema.class, (schema, generator) -> generator.generateNot((NotSchema) schema));
        GENERATORS.put(NullSchema.class, (schema, generator) -> generator.generateNull((NullSchema) schema));
        GENERATORS.put(CombinedSchema.class, (schema, generator) -> {
            CombinedSchema combinedSchema = (CombinedSchema) schema;
            NumberSchema numberSchema = null;
            StringSchema patterSchema = null;
            Schema randomSchema = null;
            for (Schema t : combinedSchema.getSubschemas()) {
                if (numberSchema == null && t instanceof NumberSchema) {
                    numberSchema = (NumberSchema) t;
                }
                if (patterSchema == null && t instanceof StringSchema && ((StringSchema) t).getPattern() != null) {
                    patterSchema = (StringSchema) t;
                    randomSchema = patterSchema;
                }
                if (randomSchema == null) {
                    randomSchema = t;
                }
            }
            if (randomSchema == null) {
                randomSchema = combinedSchema.getSubschemas().iterator().next();
            }
            Object o = generator.generateObject(randomSchema);
            if (numberSchema != null) {
                String connectType = (String) combinedSchema.getUnprocessedProperties().get(CONNECT_TYPE_PROP);
                return generator.toNumber(o, connectType);
            }
            return generator.generateObject(randomSchema);
        });
        GENERATORS.put(ObjectSchema.class, (schema, generator) -> {
            ObjectSchema objectSchema = (ObjectSchema) schema;
            String connectType = (String) objectSchema.getUnprocessedProperties().get(CONNECT_TYPE_PROP);
            if (JsonSchemaData.CONNECT_TYPE_MAP.equals(connectType)) {
                Schema soap = objectSchema.getSchemaOfAdditionalProperties();
                int size = RandomUtil.randomInt(1, 5);
                Map<String, Object> result = new HashMap<>(size);
                for (int i = 0; i < size; i++) {
                    result.put(RandomUtil.randomString(6), generator.generateObject(soap));
                }
                return result;
            }

            Map<String, Schema> propertySchemas = objectSchema.getPropertySchemas();
            JSONObject result = new JSONObject();
            List<String> requiredProperties = objectSchema.getRequiredProperties();
            for (Map.Entry<String, Schema> entry : propertySchemas.entrySet()) {
                if (requiredProperties.contains(entry.getKey())) {
                    result.put(entry.getKey(), generator.generateObject(entry.getValue()));
                    continue;
                }
                if (generator.random.nextBoolean()) {
                    result.put(entry.getKey(), generator.generateObject(entry.getValue()));
                }
            }
            return result;
        });
        GENERATORS.put(ReferenceSchema.class, (schema, generator) ->
                generator.generateReference((ReferenceSchema) schema)
        );
        GENERATORS.put(EmptySchema.class, (schema, generator) ->
                generator.generateEmpty((EmptySchema) schema)
        );
    }

    /**
     * Creates a generator out of an already-parsed {@link Schema}.
     *
     * @param schema The schema to generate values for.
     * @param random The object to use for generating randomness when producing values.
     */
    public JsonDataGenerator(Schema schema, Random random) {
        this.schema = schema;
        this.random = random;
    }

    /**
     * Creates a generator out of the yet-to-be-parsed Schema string.
     *
     * @param schemaString An Avro Schema represented as a string.
     * @param random       The object to use for generating randomness when producing values.
     */
    public JsonDataGenerator(String schemaString, Random random) {
        this(new JsonSchema(schemaString).rawSchema(), random);
    }

    /**
     * @return The schema that the generator produces values for.
     */
    public Schema schema() {
        return schema;
    }

    /**
     * Generate an object that matches the given schema and its specified properties.
     */
    public Object generate() {
        return generateObject(schema);
    }

    private Object generateObject(Schema schema) {
        BiFunction<Schema, JsonDataGenerator, Object> function = GENERATORS.get(schema.getClass());
        if (function == null) {
            throw new IllegalStateException("Unrecognized schema type: " + schema.getClass());
        }
        return function.apply(schema, this);
    }

    private Object toNumber(Object val, String connectType) {
        if (connectType != null) {
            org.apache.kafka.connect.data.Schema inferSchema = Values.inferSchema(val);
            switch (connectType) {
                case JsonSchemaData.CONNECT_TYPE_INT8:
                    return Values.convertToByte(inferSchema, val);
                case JsonSchemaData.CONNECT_TYPE_INT16:
                    return Values.convertToShort(inferSchema, val);
                case JsonSchemaData.CONNECT_TYPE_INT32:
                    return Values.convertToInteger(inferSchema, val);
                case JsonSchemaData.CONNECT_TYPE_INT64:
                    return Values.convertToLong(inferSchema, val);
                case JsonSchemaData.CONNECT_TYPE_FLOAT32:
                    return Values.convertToFloat(inferSchema, val);
                case JsonSchemaData.CONNECT_TYPE_FLOAT64:
                    return Values.convertToDouble(inferSchema, val);
                case JsonSchemaData.CONNECT_TYPE_BYTES:
                    return Values.convertToDecimal(inferSchema, val, 6);
                default:
            }
        }
        return val;
    }

    @SuppressWarnings("unchecked")
    private Object generateEmpty(EmptySchema schema) {
        if (schema.getUnprocessedProperties().containsKey("prefixItems")) {
            return generatePrefixItems(schema);
        }
        return null;
    }

    private Object generateReference(ReferenceSchema schema) {
        return generateObject(schema.getReferredSchema());
    }

    private Object generateNot(NotSchema schema) {
        return null;
    }

    @SuppressWarnings("unchecked")
    private Object generateTrueOrFalse(Schema schema) {
        if (schema.getUnprocessedProperties().containsKey("prefixItems")) {
            return generatePrefixItems(schema);
        }
        return schema.toString();
    }

    private Object generateEnum(EnumSchema schema) {
        List<Object> possibleValuesAsList = schema.getPossibleValuesAsList();
        if (possibleValuesAsList != null) {
            return possibleValuesAsList.get(random.nextInt(possibleValuesAsList.size()));
        }
        throw new IllegalStateException("No possible values for enum schema");
    }

    private Object generateConst(ConstSchema schema) {
        return schema.getPermittedValue();
    }

    private Object generateNumber(NumberSchema schema) {
        Map<String, Object> properties = schema.getUnprocessedProperties();
        String connectType = (String) properties.get("connect.type");
        if (connectType != null) {
            switch (connectType) {
                case JsonSchemaData.CONNECT_TYPE_INT8:
                    return genNumber(schema, random).byteValue();
                case JsonSchemaData.CONNECT_TYPE_INT16:
                    return genNumber(schema, random).shortValue();
                case JsonSchemaData.CONNECT_TYPE_INT32:
                    return genNumber(schema, random).intValue();
                case JsonSchemaData.CONNECT_TYPE_INT64:
                    return genNumber(schema, random).longValue();
                case JsonSchemaData.CONNECT_TYPE_FLOAT32:
                    return genNumber(schema, random).floatValue();
                case JsonSchemaData.CONNECT_TYPE_FLOAT64:
                    return genNumber(schema, random).doubleValue();
                case JsonSchemaData.CONNECT_TYPE_BYTES:
                    BigDecimal val = BigDecimal.valueOf(genNumber(schema, random).doubleValue());
                    return Decimal.fromLogical(Decimal.schema(6), val);
                default:
            }
        }
        Number val = genNumber(schema, random);
        if (schema.requiresInteger()) {
            return val.intValue();
        }
        return val;
    }

    static Number genNumber(NumberSchema schema,
                            Random random) {
        Number minimum = schema.getMinimum();
        Number exclusiveMinimumLimit = schema.getExclusiveMinimumLimit();
        double eps = 0.001D;
        double max = Double.MAX_VALUE;
        double min = Double.MAX_VALUE;
        if (minimum == null && exclusiveMinimumLimit == null) {
            min = Byte.MIN_VALUE;
        } else if (minimum != null && exclusiveMinimumLimit != null) {
            if (Precision.compareTo(minimum.doubleValue(), exclusiveMinimumLimit.doubleValue(), eps) < 0) {
                min = exclusiveMinimumLimit.doubleValue();
            }
        } else if (exclusiveMinimumLimit != null) {
            min = exclusiveMinimumLimit.doubleValue();
        }

        Number maximum = schema.getMaximum();
        Number exclusiveMaximumLimit = schema.getExclusiveMaximumLimit();
        if (maximum == null && exclusiveMaximumLimit == null) {
            max = Byte.MAX_VALUE;
        } else if (maximum != null && exclusiveMaximumLimit != null) {
            if (Precision.compareTo(maximum.doubleValue(), exclusiveMaximumLimit.doubleValue(), eps) > 0) {
                max = exclusiveMaximumLimit.doubleValue();
            }
        } else if (exclusiveMaximumLimit != null) {
            max = exclusiveMaximumLimit.doubleValue();
        }
        double multipleOf = Optional.ofNullable(schema.getMultipleOf())
                .map(Number::doubleValue)
                .orElse(1D);
        // 计算c在区间[a, b)内能覆盖的整数倍的数量

        if (schema.isExclusiveMinimum()) {
            min = min - 1;
        }
        long multipleCount = Math.round((max - min) / multipleOf);
        if (multipleCount < 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid range: cannot find any multiple of c in the interval [%f, %f)", min, max)
            );
        }
        // 生成介于0到multipleCount-1之间的随机索引
        double randomIndex = random.nextDouble() * multipleCount;
        // 根据索引计算最终的随机double数
        return min + randomIndex * max;
    }

    @SuppressWarnings("unchecked")
    private JSONArray generateArray(ArraySchema schema) {
        Map<String, Object> properties = schema.getUnprocessedProperties();
        if (properties.containsKey("prefixItems")) {
            return generatePrefixItems(schema);
        }
        Integer minContains = Optional.ofNullable(properties.get("minContains"))
                .map(t -> (Integer) t)
                .orElse(0);
        Integer maxContains = Optional.ofNullable(properties.get("maxContains"))
                .map(t -> (Integer) t)
                .orElse(2);
        Schema contains = Optional.ofNullable(properties.get("contains"))
                .map(t -> new JSONObject(((Map<String, Object>) t)))
                .map(SchemaLoader::load)
                .orElse(null);
        boolean uniqueItems = schema.needsUniqueItems();
        if (contains != null) {
            return generateContains(minContains, maxContains, uniqueItems, contains);
        }

        int length = RandomUtil.randomInt(
                Optional.ofNullable(schema.getMinItems()).orElse(1),
                Optional.ofNullable(schema.getMaxItems()).orElse(4)
        );


        JSONArray result = new JSONArray();
        if (schema.getAllItemSchema() != null) {
            for (int i = 0; i < length; i++) {
                result.put(generateObject(schema.getAllItemSchema()));
            }
            return result;
        } else if (schema.getContainedItemSchema() != null) {
            return generateContains(minContains, maxContains, uniqueItems, schema.getContainedItemSchema());
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private JSONArray generatePrefixItems(Schema schema) {
        Map<String, Object> properties = schema.getUnprocessedProperties();
        List<Map<String, Object>> prefixItems = (List<Map<String, Object>>) properties.get("prefixItems");
        JSONArray array = new JSONArray(prefixItems.size());
        prefixItems.stream()
                .map(JSONObject::new)
                .map(SchemaLoader::load)
                .map(this::generateObject)
                .forEach(array::put);
        if (schema instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) schema;
            Schema allItemSchema = arraySchema.getAllItemSchema();
            if (allItemSchema != null && !(allItemSchema instanceof FalseSchema)) {
                array.put(generateObject(allItemSchema));
            }
        }
        return array;
    }

    private JSONArray generateContains(
            Integer minContains,
            Integer maxContains,
            boolean uniqueItems,
            Schema contains) {
        int length = RandomUtil.randomInt(minContains, maxContains);
        Collection<Object> objects = new ArrayList<>(length);
        if (uniqueItems) {
            objects = new HashSet<>(length);
        }
        for (int i = 0; i < length; i++) {
            while (uniqueItems && !objects.add(generateObject(contains))) {
                // do nothing
            }
        }
        return new JSONArray(objects);
    }

    private Boolean generateBoolean(BooleanSchema schema) {
        return random.nextBoolean();
    }

    private Object generateNull(NullSchema schema) {
        return null;
    }

    private String generateString(StringSchema schema) {
        FormatValidator formatValidator = schema.getFormatValidator();
        if (formatValidator != null) {
            Function<Random, String> function = FORMAT_REGEXES.get(formatValidator.formatName());
            if (function != null) {
                return function.apply(random);
            }
        }
        String pattern = Optional.ofNullable(schema.getPattern())
                .map(Pattern::pattern)
                .orElse(RegexPool.WORD);
        int minLength = 0;
        int maxLength = Integer.MAX_VALUE;
        if (schema.getMinLength() != null) {
            minLength = schema.getMinLength();
        }
        if (schema.getMaxLength() != null) {
            maxLength = schema.getMaxLength();
        }
        if (pattern.charAt(0) == '^') {
            pattern = pattern.substring(1);
        }
        if (pattern.charAt(pattern.length() - 1) == '$') {
            pattern = pattern.substring(0, pattern.length() - 1);
        }
        String finalPattern = pattern;
        Generex generex = generexCache.computeIfAbsent(schema, s -> new Generex(finalPattern, random));
        return generex.random(minLength, maxLength);
    }
}
