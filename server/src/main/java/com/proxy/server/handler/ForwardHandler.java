package com.proxy.server.handler;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.util.List;


public class ForwardHandler {

    //forward the request to the target url
    public static Mono<ResponseEntity<byte[]>> forwardRequest(HttpServletRequest request, String url, MultiValueMap<String, String> parameters){
        final String cookiesMapKey = url + "-cookiesMap";
        //Map to store the cookies
        if(request.getSession().getAttribute(cookiesMapKey) == null)
            request.getSession().setAttribute(cookiesMapKey, new HttpHeaders());
        //Convert the map into a String
        HttpHeaders cookiesMap = (HttpHeaders) request.getSession().getAttribute(cookiesMapKey);
        StringBuilder cookies = new StringBuilder();
        for(String key : cookiesMap.keySet()){
            List<String> values = cookiesMap.get(key);
            for(String value : values){
                cookies.append(key).append("=").append(value).append(";");
            }
        }
        String fullUrl = url + request.getRequestURI();
        //Create WebClient
        WebClient webClient = createWebClient(fullUrl);
        return webClient.method(HttpMethod.valueOf(request.getMethod()))
                .uri(uriBuilder -> uriBuilder.queryParams(parameters).build())
                .header("Cookie", cookies.toString())
                .accept(MediaType.ALL)
                .retrieve()
                .toEntity(byte[].class)
                .flatMap(responseEntity -> {
                    //Get the Cookies from the response and store it in the session
                    List<String> cookiesList = responseEntity.getHeaders().get(HttpHeaders.SET_COOKIE);
                    if (cookiesList != null)
                        for(String cookie : cookiesList){
                            String[] cookieParts = cookie.split(";");
                            String[] cookieNameValue = cookieParts[0].split("=");
                            cookiesMap.add(cookieNameValue[0], cookieNameValue[1]);
                        }
                    //Create the headers
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(responseEntity.getHeaders().getContentType());
                    headers.setLocation(responseEntity.getHeaders().getLocation());
                    return Mono.just(ResponseEntity
                            .status(responseEntity.getStatusCode())
                            .headers(headers)
                            .body(responseEntity.getBody()));
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
