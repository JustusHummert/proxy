package com.proxy.server.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.proxy.server.entities.Connector;
import com.proxy.server.repositories.ConnectorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.http.MediaType;


@SpringBootTest
@AutoConfigureMockMvc
class WebControllerTest {
    @Autowired
    private ConnectorRepository connectorRepository;
    @Autowired
    private WebTestClient webTestClient;

    @Test
    void idProxy() {
        connectorRepository.save(new Connector("example", "https://example.com"));
        webTestClient.get().uri("https://example.localhost")
                .accept(MediaType.TEXT_HTML)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(response -> {
                    assertThat(response).contains("Example Domain");
                });
        connectorRepository.save(new Connector("url", "https://justushummert.com:2083"));
        webTestClient.get().uri("https://url.localhost")
                .accept(MediaType.TEXT_HTML)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(response -> {
                    assertThat(response).contains("URL");
                });
        connectorRepository.deleteById("example");
        webTestClient.get().uri("https://example.localhost")
                .accept(MediaType.ALL)
                .exchange()
                .expectStatus().isNotFound();
        connectorRepository.deleteById("url");
        connectorRepository.deleteById("grade");
    }
}