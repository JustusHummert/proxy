package com.proxy.server.controller;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
@Controller
public class WebController {

    @RequestMapping("/proxy")
    public Mono<ResponseEntity<byte[]>> proxy(@RequestParam String[] url, @RequestParam MultiValueMap<String, String> parameters,
                                              HttpServletRequest request) throws IOException {
        //Get the target url and remove it from the parameters
        String targetUrl = url[0];
        parameters.get("url").remove(0);
        if(parameters.get("url").isEmpty())
            parameters.remove("url");
        String proxy = request.getRequestURL().toString();
        WebClient webClient = createWebClient(targetUrl);
        //forward the request to the target url
        return webClient.method(HttpMethod.valueOf(request.getMethod()))
                .uri(uriBuilder -> uriBuilder.queryParams(parameters).build())
                .accept(MediaType.ALL)
                .retrieve()
                .toEntity(byte[].class)
                .flatMap(responseEntity -> {
                    MediaType contentType = responseEntity.getHeaders().getContentType();
                    byte[] body = responseEntity.getBody();
                    //modify the Answer to change the urls to the proxy url
                    if(contentType != null && contentType.getType().equals("text"))
                        body = modifyAnswer(body, targetUrl, proxy);
                    assert contentType != null;
                    return Mono.just(ResponseEntity
                            .status(responseEntity.getStatusCode())
                            .contentType(contentType)
                            .location(responseEntity.getHeaders().getLocation())
                            .body(body));
                });
    }

    //modify the html to change partial urls to full urls
    private byte[] modifyAnswer(byte[] body, String url, String proxy){
        String answer = new String(body);
        //remove everything after the first / in the url
        url = url.split("/")[0] + "//" + url.split("/")[2];
        //change partial urls to absolute urls
        answer = answer.replaceAll("src\\s*=\\s*\"/?(?!https?://|http://)","src=\""+url+"/");
        answer = answer.replaceAll("href\\s*=\\s*\"/?(?!https?://|http://)","href=\""+url+"/");
        answer = answer.replaceAll("url\\s*:\\s*\"/?(?!https?://|http://)","url: \""+url+"/");
        //replace ? in urls with & to avoid overwriting the url parameters
        answer = answer.replaceAll("(" + url + "[^\"]*)\\?", "$1&");
        //replace the url with the proxy url
        answer = answer.replaceAll(url,proxy + "?url=" + url);
        return answer.getBytes();
    }

    //create WebClient
    private WebClient createWebClient(String url){
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
