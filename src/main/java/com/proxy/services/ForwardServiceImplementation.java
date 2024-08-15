package com.proxy.services;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Service
public class ForwardServiceImplementation implements ForwardService{

    //forward the request to the target url
    @Override
    public Mono<ResponseEntity<byte[]>> forwardRequest(HttpServletRequest request, String url, MultiValueMap<String, String> parameters){
        //don´t interact with favicon
        if(request.getRequestURI().equals("/favicon.ico")){
            return Mono.just(ResponseEntity.notFound().build());
        }
        String fullUrl = url + request.getRequestURI();
        String queryString = request.getQueryString();
        //remove the parameters that are in the queryString from the parameters
        if(queryString != null){
            String[] params = queryString.split("&");
            for(String param : params){
                String[] keyValue = param.split("=");
                parameters.remove(keyValue[0]);
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
                    .toEntity(byte[].class)
                .flatMap(responseEntity -> {
                    String proxyUrl = request.getRequestURL().toString();
                    //remove the path from the url
                    proxyUrl = proxyUrl.substring(0, proxyUrl.length() - request.getRequestURI().length());
                    //replace the url in the response with this url
                    if(responseEntity.getBody()!=null){
                        String body = new String(responseEntity.getBody());
                        //replace the url in the response with this url
                        body = body.replace(url, proxyUrl);
                        responseEntity = ResponseEntity.status(responseEntity.getStatusCode()).headers(responseEntity.getHeaders()).body(body.getBytes());
                    }
                    //do the same for the location header
                    if(responseEntity.getHeaders().containsKey(HttpHeaders.LOCATION)) {
                        HttpHeaders headers = new HttpHeaders();
                        headers.putAll(responseEntity.getHeaders());
                        String locationHeader = headers.getFirst(HttpHeaders.LOCATION).replace(url, proxyUrl);
                        headers.set(HttpHeaders.LOCATION, locationHeader);
                        responseEntity = ResponseEntity.status(responseEntity.getStatusCode()).headers(headers).body(responseEntity.getBody());
                    }
                    return Mono.just(responseEntity);
                });
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
