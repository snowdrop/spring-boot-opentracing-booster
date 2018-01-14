package me.snowdrop;


import brave.Tracing;
import brave.opentracing.BraveTracer;
import io.opentracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.okhttp3.OkHttpSender;

@SpringBootApplication
@EnableConfigurationProperties(ZipkinProperties.class)
public class App {

    private static Logger LOG = LoggerFactory.getLogger(App.class);

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

    @Bean
    @Qualifier("app-tracer")
    public Tracer tracer(ZipkinProperties zipkinProperties) {
        final OkHttpSender sender =
                OkHttpSender.create(
                    String.format("http://%s:%d/api/v2/spans", zipkinProperties.getHost(), zipkinProperties.getPort())
                );

        final AsyncReporter<Span> spanReporter = AsyncReporter.create(sender);

        final Tracing braveTracing = Tracing.newBuilder()
                .localServiceName("spring-boot")
                .spanReporter(spanReporter)
                .build();

        return BraveTracer.create(braveTracing);
    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

}
