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


import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
class WebControllerTest {
    @Autowired
    private ConnectorRepository connectorRepository;
    @Autowired
    private WebTestClient webTestClient;

    @Test
    void proxy() throws Exception{
        webTestClient.get().uri("/proxy?url=https://example.com")
                        .accept(MediaType.TEXT_HTML)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(String.class)
                        .value(response -> {
                            assertThat(response).contains("Example Domain");
                        });
        webTestClient.get().uri("/proxy?url=example")
                .accept(MediaType.ALL)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void idProxy() {
        connectorRepository.save(new Connector("example", "https://example.com"));
        webTestClient.get().uri("/example")
                .accept(MediaType.TEXT_HTML)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(response -> {
                    assertThat(response).contains("Example Domain");
                });
        connectorRepository.save(new Connector("url", "https://justushummert.com:2083"));
        webTestClient.get().uri("/url/URLShortener")
                .accept(MediaType.TEXT_HTML)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(response -> {
                    assertThat(response).contains("URL");
                });
        connectorRepository.save(new Connector("grade", "https://justushummert.com:2053"));
        webTestClient.get().uri("/grade")
                .accept(MediaType.TEXT_HTML)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(response -> {
                    assertThat(response).contains("username");
                });
        webTestClient.post().uri("/grade/login?username=test&password=test")
                .accept(MediaType.ALL)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(response -> {
                    assertThat(response).hasSize(32);
                    webTestClient.get().uri("/grade/main/subject?sessionId="+response+"&subjectId=102")
                            .accept(MediaType.TEXT_HTML)
                            .exchange()
                            .expectStatus().isOk()
                            .expectBody(String.class)
                            .value(response2 -> {
                                assertThat(response2).contains("average Grade=");
                            });
                });
        connectorRepository.deleteById("example");
        webTestClient.get().uri("/example")
                .accept(MediaType.ALL)
                .exchange()
                .expectStatus().isNotFound();
        connectorRepository.deleteById("url");
        connectorRepository.deleteById("grade");
    }
}