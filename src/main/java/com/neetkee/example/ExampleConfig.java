package com.neetkee.example;

import brave.Tracing;
import brave.kafka.clients.KafkaTracing;
import brave.sampler.Sampler;
import org.apache.kafka.streams.KafkaClientSupplier;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.kafka11.KafkaSender;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@EnableKafkaStreams
public class ExampleConfig {
    private String bootStrapServers = "localhost:9092";

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_BUILDER_BEAN_NAME)
    public StreamsBuilderFactoryBean myKStreamBuilder(KafkaStreamsConfiguration streamsConfig) throws Exception {
        final KafkaSender sender = KafkaSender.newBuilder()
                .bootstrapServers(bootStrapServers).build();
        final AsyncReporter<Span> reporter = AsyncReporter.builder(sender).build();
        final Tracing tracing = Tracing.newBuilder().localServiceName("example-service")
                .sampler(Sampler.ALWAYS_SAMPLE).spanReporter(reporter).build();
        KafkaTracing kafkaTracing = KafkaTracing.create(tracing);

        Class<?> aClass = Class.forName("brave.kafka.streams.TracingKafkaClientSupplier");
        Constructor<?> declaredConstructor = aClass.getDeclaredConstructor(KafkaTracing.class);
        declaredConstructor.setAccessible(true);
        KafkaClientSupplier kafkaClientSupplier = (KafkaClientSupplier) declaredConstructor.newInstance(kafkaTracing);

        StreamsBuilderFactoryBean streamsBuilderFactoryBean = new StreamsBuilderFactoryBean(streamsConfig);
        streamsBuilderFactoryBean.setClientSupplier(kafkaClientSupplier);
        return streamsBuilderFactoryBean;
    }

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration configuration() {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "example");

        return new KafkaStreamsConfiguration(props);
    }
}
