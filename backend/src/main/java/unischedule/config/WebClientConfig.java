package unischedule.config;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    private static final String EVERYTIME_BASE_URL = "https://api.everytime.kr";
    private static final String OPENAI_BASE_URL = "https://api.openai.com/v1";

    private static final int MAX_MEMORY_SIZE = 1024 * 1024;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    private HttpClient httpClient(Duration responseTimeout, Duration connectTimeout) {
        return HttpClient.create()
                .responseTimeout(responseTimeout)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis())
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(responseTimeout.toMillis(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(responseTimeout.toMillis(), TimeUnit.MILLISECONDS))
                );
    }

    @Bean(name = "everytimeWebClient")
    public WebClient everytimeWebClient() {
        return WebClient.builder()
                .baseUrl(EVERYTIME_BASE_URL)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> {
                            // XML 디코더 추가 및 메모리 사이즈 조정 (1MB), Jackson2XmlDecoder가 별도로 제공되지 않는 거 같음..
                            configurer.customCodecs().register(
                                    new Jackson2JsonDecoder(new XmlMapper(), MediaType.APPLICATION_XML));
                            configurer.defaultCodecs().maxInMemorySize(MAX_MEMORY_SIZE);
                        }).build())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                // 타임아웃 관련 설정
                .clientConnector(new ReactorClientHttpConnector(
                        httpClient(Duration.ofSeconds(10), Duration.ofSeconds(5))))
                .build();
    }

    @Bean(name = "openAiWebClient")
    public WebClient openAiWebClient() {
        return WebClient.builder()
                .baseUrl(OPENAI_BASE_URL)
                .clientConnector(new ReactorClientHttpConnector(
                        httpClient(Duration.ofSeconds(30), Duration.ofSeconds(5))))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
