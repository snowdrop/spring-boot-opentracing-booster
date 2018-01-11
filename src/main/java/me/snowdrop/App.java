package me.snowdrop;

import com.uber.jaeger.Configuration;
import com.uber.jaeger.samplers.ProbabilisticSampler;
import com.uber.jaeger.senders.HttpSender;
import com.uber.jaeger.senders.Sender;
import com.uber.jaeger.senders.UdpSender;
import io.opentracing.Tracer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class App {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

    @Bean
    @ConditionalOnExpression("#{systemEnvironment['HOSTNAME']==null}")
    public Tracer localJaegerTracer(Sender sender) {
        Configuration.SenderConfiguration senderConfiguration = new Configuration.SenderConfiguration.Builder().sender(sender).build();
        return new Configuration("spring-boot",
          new Configuration.SamplerConfiguration(ProbabilisticSampler.TYPE, 1),
          new Configuration.ReporterConfiguration(true, 10, 10, senderConfiguration))
          .getTracer();
    }

    @Bean
    @ConditionalOnExpression("#{systemEnvironment['HOSTNAME']!=null}")
    public Tracer jaegerTracer(Sender sender) {
        return new Configuration("spring-boot",
           new Configuration.SamplerConfiguration(ProbabilisticSampler.TYPE, 1),
           new Configuration.ReporterConfiguration(true, System.getenv("HOSTNAME"),null,null, null))
           .getTracer();
    }

    @Bean
    @ConditionalOnExpression("#{systemEnvironment['HOSTNAME']==null}")
    public Sender localSender(@Value("${http.sender}") String senderURL) {
        return new HttpSender(senderURL);
    }

    @Bean
    @ConditionalOnExpression("#{systemEnvironment['HOSTNAME']!=null}")
    public Sender sender() {
        return new UdpSender(UdpSender.DEFAULT_AGENT_UDP_HOST, UdpSender.DEFAULT_AGENT_UDP_COMPACT_PORT,  65507 );
    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

}
