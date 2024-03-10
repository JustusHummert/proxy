package com.proxy.server.handler;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import jakarta.servlet.http.Cookie;
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

import java.util.List;
import java.util.function.Consumer;

public class ForwardHandler {

    //forward the request to the target url
    public static Mono<ResponseEntity<byte[]>> forwardRequest(HttpServletRequest request, String url, MultiValueMap<String, String> parameters){
        System.out.println("SessionId: " + request.getSession().getId());
        //print session id
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals("JSESSIONID")) {
                System.out.println("JSESSIONID: " + cookie.getValue());
            }
        }
        //Map to store the cookies
        if(request.getSession().getAttribute("cookiesMap") == null)
            request.getSession().setAttribute("cookiesMap", new HttpHeaders());
        //Convert the map into a String
        HttpHeaders cookiesMap = (HttpHeaders) request.getSession().getAttribute("cookiesMap");
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
                    MediaType contentType = responseEntity.getHeaders().getContentType();
                    byte[] body = responseEntity.getBody();
                    if(contentType != null && contentType.toString().contains("text"))
                        body = modifyAnswer(body, url, "https://" + request.getServerName());
                    //Get the Cookies from the response and store it in the session
                    List<String> cookiesList = responseEntity.getHeaders().get(HttpHeaders.SET_COOKIE);
                    if (cookiesList != null)
                        for(String cookie : cookiesList){
                            String[] cookieParts = cookie.split(";");
                            String[] cookieNameValue = cookieParts[0].split("=");
                            cookiesMap.add(cookieNameValue[0], cookieNameValue[1]);
                        }
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(contentType);
                    headers.setLocation(responseEntity.getHeaders().getLocation());
                    return Mono.just(ResponseEntity
                            .status(responseEntity.getStatusCode())
                            .headers(headers)
                            .body(body));
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

    //modify the Answer to change the urls to the proxy url
    public static byte[] modifyAnswer(byte[] body, String url, String proxy){
        String answer = new String(body);
        //replace the url with the proxy url
        answer = answer.replaceAll(url,proxy);
        return answer.getBytes();
    }
}
