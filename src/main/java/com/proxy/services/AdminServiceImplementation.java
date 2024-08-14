package com.proxy.services;

import com.proxy.repositories.AdminRepository;
import com.proxy.repositories.ConnectorRepository;
import com.proxy.entities.Admin;
import com.proxy.entities.Connector;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import org.springframework.web.servlet.view.RedirectView;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class AdminServiceImplementation implements AdminService {
    //handle the request based on the path
    @Override
    public Object handleRequest(HttpServletRequest request, MultiValueMap<String, String> parameters,
                                                             Model model, ConnectorRepository connectorRepository,
                                                             AdminRepository adminRepository) {
        //use Method according to the path
        switch (request.getRequestURI()) {
            case "/addConnector" -> {
                return addConnector(parameters.get("subdomain").get(0), parameters.get("url").get(0), request, connectorRepository, adminRepository);
            }
            case "/removeConnector" -> {
                return removeConnector(parameters.get("subdomain").get(0), request, connectorRepository);
            }
            case "/login" -> {
                return login(parameters.get("password").get(0), request, adminRepository);
            }
            case "/getConnections" -> {
                Set<String> connections = getConnections(connectorRepository);
                return Mono.just(ResponseEntity.ok().body(connections));
            }
            default -> {
                return admin(model, connectorRepository);
            }
        }
    }

    //Add a connector to the database
    private String addConnector(String subdomain, String url, HttpServletRequest request,
                                                    ConnectorRepository connectorRepository, AdminRepository adminRepository){
        //check if url starts with http:// or https://  otherwise add https://
        if(!url.startsWith("http://") && !url.startsWith("https://"))
            url = "https://" + url;
        //check if url is valid
        String response =  createWebClient(url).get().accept(MediaType.ALL).retrieve().bodyToMono(String.class)
                .onErrorReturn("error").block();
        //Invalid Url
        if(response == null || response.equals("error"))
            return "redirect:/?invalidUrl";
        //Check if the sessionId is correct
        if(invalidSession(request.getSession()))
            return "redirect:/?invalidSession";
        //Check if the subdomain already exists
        if (connectorRepository.existsById(subdomain)) {
            return "redirect:/?subdomainExists";
        }
        //Add the connector to the database
        connectorRepository.save(new Connector(subdomain, url));
        return "redirect:/";
    }

    //Remove a connector from the database
    private String removeConnector(String subdomain, HttpServletRequest request, ConnectorRepository connectorRepository){
        //Check if the sessionId is correct
        if(invalidSession(request.getSession()))
            return "redirect:/?invalidSession";
        //Check if the subdomain exists
        if (!connectorRepository.existsById(subdomain)) {
            return "redirect:/?subdomainDoesNotExist";
        }
        connectorRepository.deleteById(subdomain);
        return "redirect:/";
    }

    private String login(String password, HttpServletRequest request, AdminRepository adminRepository){
        Optional<Admin> admin = adminRepository.findById(0);
        if(admin.isEmpty())
            return "redirect:/?invalidSession&noAdmin";
        if(!BCrypt.checkpw(password, admin.get().getPassword()))
            return "redirect:/?invalidSession&invalidPassword";
        request.getSession().setAttribute("admin", true);
        return "redirect:/";
    }

    //return the Subdomains of all connectors
    private Set<String> getConnections(ConnectorRepository connectorRepository){
        Iterable<Connector> connectors = connectorRepository.findAll();
        Set<String> result = new HashSet<>();
        connectors.forEach(
                connector -> {
                    result.add(connector.getSubdomain());
                }
        );
        return result;
    }

    //return the admin page
    private String admin(Model model, ConnectorRepository connectorRepository){
        model.addAttribute("connectors", connectorRepository.findAll());
        return "admin";
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

    //check if the session is valid
    private boolean invalidSession(HttpSession session){
        return session.getAttribute("admin") == null || !session.getAttribute("admin").equals(true);
    }



}
