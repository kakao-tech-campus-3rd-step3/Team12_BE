package unischedule.config;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
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
    private static final int TIMEOUT_SECONDS = 10;
    private static final int CONNECT_TIMEOUT_MILLIS = 5000;
    private static final int MAX_MEMORY_SIZE = 1024 * 1024;

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
                        HttpClient.create()
                                .responseTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MILLIS)
                                .doOnConnected(conn -> conn
                                        .addHandlerLast(new ReadTimeoutHandler(TIMEOUT_SECONDS, TimeUnit.SECONDS))
                                        .addHandlerLast(new WriteTimeoutHandler(TIMEOUT_SECONDS))
                                )
                ))
                .build();
    }
}
