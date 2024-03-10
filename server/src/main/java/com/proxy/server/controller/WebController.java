package com.proxy.server.controller;

import com.proxy.server.entities.Connector;
import com.proxy.server.handler.AdminHandler;
import com.proxy.server.handler.ForwardHandler;
import com.proxy.server.repositories.AdminRepository;
import com.proxy.server.repositories.ConnectorRepository;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.util.Optional;

@Controller
public class WebController {
    @Autowired
    private ConnectorRepository connectorRepository;

    @Autowired
    private AdminRepository adminRepository;

    //redirect all request to a Method based on subdomain
    @RequestMapping(value="**")
    public Object redirect(HttpServletRequest request, @RequestParam MultiValueMap<String, String> parameters,
                                                 Model model){
        //Get the subdomain
        //If there is no subdomain the subdomain is the domain
        String subdomain = request.getServerName().split("\\.")[0];
        if(subdomain.equals("admin"))
            //AdminHandler will handle this
            return AdminHandler.handleRequest(request, parameters, model, connectorRepository, adminRepository);
        if(subdomain.equals("proxy"))
            //ProxyHandler will handle this
            return null;
        Optional<Connector> optionalConnector = connectorRepository.findById(subdomain);
        if(optionalConnector.isEmpty())
            return Mono.just(ResponseEntity.notFound().build());
        String url = optionalConnector.get().getUrl();
        //ForwardHandler will handle this
        return ForwardHandler.forwardRequest(request, url, parameters);
    }



    //return the (modified) answer of the target url
    public Mono<ResponseEntity<byte[]>> proxy(@RequestParam String[] url, @RequestParam MultiValueMap<String, String> parameters,
                                              HttpServletRequest request) throws IOException {
        //Get the target url and remove it from the parameters
        String targetUrl = url[0];
        parameters.get("url").remove(0);
        if(parameters.get("url").isEmpty())
            parameters.remove("url");
        //Split target url
        String[] split = targetUrl.split("/");
        if(split.length < 3)
            return Mono.just(ResponseEntity.badRequest().build());
        //create the proxy url
        String proxy = request.getRequestURL().toString() + "?url=" +  split[0] + "//" + split[2];;
        WebClient webClient = createWebClient(targetUrl);
        //forward the request to the target url
        return forwardRequest(webClient, HttpMethod.valueOf(request.getMethod()), parameters, targetUrl, proxy);
    }

    //return the proxyInterface page
    public String proxyInterface(Model model){
        return "proxyInterface";
    }

    //nothing to home so homepage can be accessed without id
    public Mono<ResponseEntity<byte[]>> nothingToHome(@RequestParam MultiValueMap<String, String> parameters,
                                                      HttpServletRequest request) throws IOException {
        return idProxy("home", parameters, request);
    }

    //return the (modified) answer of the url the id is linked to
    public Mono<ResponseEntity<byte[]>> idProxy(@PathVariable String id, @RequestParam MultiValueMap<String, String> parameters,
                                              HttpServletRequest request) throws IOException{
        //Get the target url
        Optional<Connector> optionalConnector = connectorRepository.findById(id);
        if(optionalConnector.isEmpty())
            return Mono.just(ResponseEntity.notFound().build());
        Connector connector = optionalConnector.get();
        String url = connector.getUrl();

        //Get the path
        String[] path = request.getRequestURI().split("/");
        StringBuilder targetUrlBuilder = new StringBuilder(url);
        //combine the path after id with the target url
        for(int i =2; i < path.length; i++) {
            targetUrlBuilder.append("/").append(path[i]);
        }
        String targetUrl = targetUrlBuilder.toString();
        //create the proxy url
        String[] split = request.getRequestURL().toString().split("/");
        String proxy = split[0] + "//" + split[2] + "/" + id;
        WebClient webClient = createWebClient(targetUrl);
        //forward the request to the target url
        return forwardRequest(webClient, HttpMethod.valueOf(request.getMethod()), parameters, targetUrl, proxy);
    }


    //forward the request to the target url and modify the answer
    private Mono<ResponseEntity<byte[]>> forwardRequest(WebClient webClient, HttpMethod method,
                                                MultiValueMap<String, String> parameters, String targetUrl, String proxy){
        return webClient.method(method)
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
        String[] split = url.split("/");
        url = split[0] + "//" + split[2];
        //change partial urls to absolute urls
        answer = answer.replaceAll("src\\s*=\\s*\"/?(?!https?://|http://)","src=\""+url+"/");
        answer = answer.replaceAll("href\\s*=\\s*\"/?(?!https?://|http://)","href=\""+url+"/");
        answer = answer.replaceAll("url\\s*:\\s*\"/?(?!https?://|http://)","url: \""+url+"/");
        //if proxy has ? in it replace ? in urls with & to avoid overwriting the url parameters
        if(proxy.contains("?"))
            answer = answer.replaceAll("(" + url + "[^\"]*)\\?", "$1&");
        //replace the url with the proxy url
        answer = answer.replaceAll(url,proxy);
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
