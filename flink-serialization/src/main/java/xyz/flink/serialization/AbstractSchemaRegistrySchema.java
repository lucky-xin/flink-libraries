package xyz.flink.serialization;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.SchemaProvider;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializerConfig;
import io.confluent.kafka.serializers.subject.strategy.SubjectNameStrategy;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import org.apache.kafka.common.config.SslConfigs;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.KEY_SUBJECT_NAME_STRATEGY;
import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY;
import static io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializerConfig.JSON_KEY_TYPE;
import static io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializerConfig.JSON_VALUE_TYPE;

/**
 * SchemaRegistrySchema
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-06-21
 */
@SuperBuilder
public abstract class AbstractSchemaRegistrySchema<T> implements Serializable, Closeable {
    private static final long serialVersionUID = -1671641202177852775L;
    /**
     * class of record
     */
    protected final Class<T> type;
    protected static final byte MAGIC_BYTE = 0x0;
    protected static final int ID_SIZE = 4;

    /**
     * schemaType of schema registry to produce
     */
    @NonNull
    private final String schemaType;
    /**
     * subject of schema registry to produce
     */
    @Getter
    @NonNull
    private final String subject;
    /**
     * URL of schema registry to connect
     */
    @NonNull
    private final String schemaRegistryUrl;
    /**
     * map with additional schema registry configs (for example SSL properties)
     */
    @Getter
    @NonNull
    private Map<String, Serializable> registryConfigs;
    private final Map<String, String> registryHttpHeaders;
    @Builder.Default
    private int identityMapCapacity = 1000;
    protected final boolean key;


    protected transient ParsedSchema schema;
    protected transient CachedSchemaRegistryClient schemaRegistryClient;

    protected CachedSchemaRegistryClient createSchemaRegistryClient() {
        Map<String, Object> restConfigs = this.registryConfigs.entrySet()
                .stream()
                .collect(Collectors.toMap(
                                e -> e.getKey().startsWith(SchemaRegistryClientConfig.CLIENT_NAMESPACE)
                                        ? e.getKey().substring(SchemaRegistryClientConfig.CLIENT_NAMESPACE.length())
                                        : e.getKey(),
                                Map.Entry::getValue,
                                (existing, replacement) -> replacement
                        )
                );
        RestService restService = new RestService(schemaRegistryUrl);
        restService.configure(restConfigs);

        List<SchemaProvider> providers = new LinkedList<>();
        switch (this.schemaType) {
            case AvroSchema.TYPE:
                providers.add(new AvroSchemaProvider());
                break;
            case ProtobufSchema.TYPE:
                providers.add(new ProtobufSchemaProvider());
                break;
            case JsonSchema.TYPE:
                providers.add(new JsonSchemaProvider());
                break;
            default:
                throw new IllegalArgumentException("Unsupported schema type: " + this.schemaType);
        }
        CachedSchemaRegistryClient client = new CachedSchemaRegistryClient(
                restService,
                identityMapCapacity,
                providers,
                registryConfigs,
                registryHttpHeaders
        );
        if (!restConfigs.containsKey(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG)) {
            restService.setSslSocketFactory(createSSLSocketFactory());
            restService.setHostnameVerifier(getHostnameVerifier(restConfigs));
        }
        return client;
    }

    private HostnameVerifier getHostnameVerifier(Map<String, Object> config) {
        String sslEndpointIdentificationAlgo =
                (String) config.get(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG);

        if (sslEndpointIdentificationAlgo == null
                || sslEndpointIdentificationAlgo.equals("none")
                || sslEndpointIdentificationAlgo.isEmpty()) {
            return (hostname, session) -> true;
        }

        return null;
    }

    protected void checkInitialized() throws IOException, RestClientException {
        if (this.schemaRegistryClient != null) {
            return;
        }
        this.registryConfigs = new HashMap<>(registryConfigs);
        this.registryConfigs.putIfAbsent(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        this.registryConfigs.putIfAbsent(KafkaJsonSchemaDeserializerConfig.FAIL_UNKNOWN_PROPERTIES, false);
        this.registryConfigs.putIfAbsent(KafkaJsonSchemaDeserializerConfig.FAIL_INVALID_SCHEMA, true);
        if (this.key) {
            this.registryConfigs.putIfAbsent(KEY_SUBJECT_NAME_STRATEGY, IdentifySubjectNameStrategy.class.getName());
            this.registryConfigs.putIfAbsent(JSON_KEY_TYPE, this.type.getName());
        } else {
            this.registryConfigs.putIfAbsent(VALUE_SUBJECT_NAME_STRATEGY, IdentifySubjectNameStrategy.class.getName());
            this.registryConfigs.putIfAbsent(JSON_VALUE_TYPE, this.type.getName());
        }
        this.schemaRegistryClient = createSchemaRegistryClient();
        try {
            SchemaMetadata metadata = this.schemaRegistryClient.getLatestSchemaMetadata(this.subject);
            this.schema = this.schemaRegistryClient.parseSchema(
                            metadata.getSchemaType(),
                            metadata.getSchema(),
                            metadata.getReferences()
                    )
                    .orElseThrow(() -> new IllegalStateException("Failed to parse schema"));
        } catch (RestClientException e) {
            throw new IOException("get schema error,subject:" + subject, e);
        }
    }

    private SSLSocketFactory createSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManager[] trustManagers = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                            //  document why this method is empty
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                            // document why this method is empty
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };
            sslContext.init(null, trustManagers, new java.security.SecureRandom());
            return sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IllegalStateException(e);
        }
    }

    public ParsedSchema getSchema() {
        if (this.schema != null) {
            return this.schema;
        }
        if (this.schemaRegistryClient == null) {
            this.schemaRegistryClient = createSchemaRegistryClient();
        }
        try {
            SchemaMetadata metadata = this.schemaRegistryClient.getLatestSchemaMetadata(this.subject);
            this.schema = this.schemaRegistryClient.parseSchema(
                    schemaType,
                    metadata.getSchema(),
                    metadata.getReferences()
            ).orElseThrow(() -> new IllegalStateException("parse schema failed"));
        } catch (RestClientException e) {
            throw new IllegalStateException("get latest schema metadata failed", e);
        } catch (IOException e) {
            throw new IllegalStateException("Internal HTTP error", e);
        }
        return this.schema;
    }

    @Override
    public void close() throws IOException {
        if (this.schemaRegistryClient != null) {
            this.schemaRegistryClient.close();
        }
    }

    public static class IdentifySubjectNameStrategy implements SubjectNameStrategy {

        /**
         * For a given topic and message, returns the subject name under which the
         * schema should be registered in the schema registry.
         *
         * @param topic  The Kafka topic name to which the message is being published.
         * @param isKey  True when encoding a message key, false for a message value.
         * @param schema the schema of the record being serialized/deserialized
         * @return The subject name under which the schema should be registered.
         */
        public String subjectName(String topic, boolean isKey, ParsedSchema schema) {
            return subjectName(topic, isKey);
        }

        /**
         * For a given topic and message, returns the subject name under which the
         * schema should be registered in the schema registry.
         *
         * @param topic The Kafka topic name to which the message is being published.
         * @param isKey True when encoding a message key, false for a message value.
         * @return The subject name under which the schema should be registered.
         */
        public String subjectName(String topic, boolean isKey) {
            return topic;
        }

        @Override
        public void configure(Map<String, ?> map) {

        }
    }
}
