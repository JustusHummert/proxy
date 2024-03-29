package com.proxy.handler;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.Iterator;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


public class ForwardHandler {

    //forward the request to the target url
    public static Mono<ResponseEntity<byte[]>> forwardRequest(HttpServletRequest request, String url, MultiValueMap<String, String> parameters){
        String fullUrl = url + request.getRequestURI();
        String queryString = request.getQueryString();
        //remove the parameters that are in the queryString from the parameters
        if(queryString != null){
            String[] params = queryString.split("&");
            for(String param : params){
                String[] keyValue = param.split("=");
                if(parameters.containsKey(keyValue[0])){
                    parameters.get(keyValue[0]).remove(keyValue[1]);
                }
            }
        }
        //Create WebClient
        WebClient webClient = createWebClient(fullUrl);
        return webClient.method(HttpMethod.valueOf(request.getMethod()))
                    .uri(uriBuilder -> uriBuilder.query(request.getQueryString()).build())
                    .header("Cookie", request.getHeader("Cookie"))
                    .header("X-Forwarded-Host", request.getHeader("Host"))
                    .accept(MediaType.ALL)
                    .bodyValue(parameters)
                    .retrieve()
                    .toEntity(byte[].class);
        }
    //create WebClient
    private static WebClient createWebClient(String url){
        //Configute custom ExchangeStrategies to avoid buffer limit exception
        //16MB buffer size
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)).build();
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().secure(sslContextSpec -> sslContextSpec
                        .sslContext(SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE)
                        ))))
                .exchangeStrategies(exchangeStrategies)
                .baseUrl(url)
                .build();
    }
}
