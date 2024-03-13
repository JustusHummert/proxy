package com.proxy.handler;

import com.proxy.entities.Admin;
import com.proxy.entities.Connector;
import com.proxy.repositories.AdminRepository;
import com.proxy.repositories.ConnectorRepository;
import com.proxy.sessionManagement.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;


@SpringBootTest
@AutoConfigureMockMvc
class AdminHandlerTest {
    @Autowired
    AdminRepository adminRepository;
    @Autowired
    ConnectorRepository connectorRepository;

    @Autowired
    private WebTestClient webTestClient;

    MockHttpSession mockSession;

    @BeforeEach
    void setUp() throws Exception {
        // Create a new MockHttpSession
        mockSession = new MockHttpSession();

        // Add the mock session to the SessionManager
        SessionManager.getInstance().addSession(mockSession.getId());
    }

    @SuppressWarnings("unchecked")
    @Test
    void addConnector() throws Exception{
        MockHttpServletRequest request = new MockHttpServletRequest();
        connectorRepository.deleteById("example");
        request.setServerName("admin.localhost");
        request.setRequestURI("/addConnector");
        request.setSession(mockSession);
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("subdomain", "example");
        parameters.add("url", "https://example.com");
        //valid request
        Mono<ResponseEntity<String>> validConnectorResponse = (Mono<ResponseEntity<String>>) AdminHandler.handleRequest(
                request, parameters, null, connectorRepository, adminRepository);
        validConnectorResponse.flatMap(response -> {
            assert response.getBody().equals("example now connected to https://example.com");
            return Mono.just(response);
        });
        //subdomain already exists
        Mono<ResponseEntity<String>> invalidConnectorResponse = (Mono<ResponseEntity<String>>) AdminHandler.handleRequest(
                request, parameters, null, connectorRepository, adminRepository);
        invalidConnectorResponse.flatMap(response -> {
            assert response.getBody().equals("subdomain already exists");
            return Mono.just(response);
        });
        //wrong session
        request.setSession(new MockHttpSession());
        Mono<ResponseEntity<String>> invalidSessionResponse = (Mono<ResponseEntity<String>>) AdminHandler.handleRequest(
                request, parameters, null, connectorRepository, adminRepository);
        invalidSessionResponse.flatMap(response -> {
            assert response.getBody().equals("Invalid session");
            return Mono.just(response);
        });
        connectorRepository.deleteById("example");
        //url without http
        request.setSession(mockSession);
        parameters.set("url", "example.com");
        Mono<ResponseEntity<String>> noHttpResponse = (Mono<ResponseEntity<String>>) AdminHandler.handleRequest(
                request, parameters, null, connectorRepository, adminRepository);
        noHttpResponse.flatMap(response -> {
            assert response.getBody().equals("example now connected to https://example.com");
            return Mono.just(response);
        });
        //invalid url
        parameters.set("url", "invalid");
        Mono<ResponseEntity<String>> invalidUrlResponse = (Mono<ResponseEntity<String>>) AdminHandler.handleRequest(
                request, parameters, null, connectorRepository, adminRepository);
        invalidUrlResponse.flatMap(response -> {
            assert response.getBody().equals("invalid url");
            return Mono.just(response);
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    void removeConnector() throws Exception{
        MockHttpServletRequest request = new MockHttpServletRequest();
        connectorRepository.deleteById("example");
        request.setServerName("admin.localhost");
        request.setRequestURI("/removeConnector");
        request.setSession(mockSession);
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("subdomain", "example");
        //add connector and remove it
        connectorRepository.save(new Connector("example", "https://example.com"));
        Mono<ResponseEntity<String>> validConnectorResponse = (Mono<ResponseEntity<String>>) AdminHandler.handleRequest(
                request, parameters, null, connectorRepository, adminRepository);
        assert validConnectorResponse.block().getBody().equals("example removed");
        //try to remove it again
        Mono<ResponseEntity<String>> invalidConnectorResponse = (Mono<ResponseEntity<String>>) AdminHandler.handleRequest(
                request, parameters, null, connectorRepository, adminRepository);
        assert invalidConnectorResponse.block().getBody().equals("subdomain does not exist");
        //wrong Session
        request.setSession(new MockHttpSession());
        Mono<ResponseEntity<String>> invalidSessionResponse = (Mono<ResponseEntity<String>>) AdminHandler.handleRequest(
                request, parameters, null, connectorRepository, adminRepository);
        assert invalidSessionResponse.block().getBody().equals("Invalid session");
    }

    @Test
    void login() throws Exception{
        adminRepository.save(new Admin(BCrypt.hashpw("testPassword", BCrypt.gensalt())));
        webTestClient.post().uri("https://admin.localhost/login?password=testPassword")
                .accept(MediaType.ALL)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(response -> {
                    assert response.equals("logged in");
                });

        //wrong password
        webTestClient.post().uri("https://admin.localhost/login?password=wrong")
                .accept(MediaType.ALL)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(response -> {
                    assert response.equals("Wrong Password");
                });
        //no admin
        adminRepository.deleteAll();
        webTestClient.post().uri("https://admin.localhost/login?password=wrong")
                .accept(MediaType.ALL)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(response -> {
                    assert response.equals("No Admin");
                });
    }

    @Test
    void getConnections() throws Exception{
        connectorRepository.deleteAll();
        connectorRepository.save(new Connector("example", "https://example.com"));
        connectorRepository.save(new Connector("gradeCalculator", "https://localhost:2053"));
        connectorRepository.save(new Connector("urlShortener", "https://localhost:2083"));
        webTestClient.get().uri("https://admin.localhost/getConnections")
                .accept(MediaType.ALL)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(response -> {
                    assert response.contains("example");
                    assert response.contains("gradeCalculator");
                    assert response.contains("urlShortener");
                });

    }
}