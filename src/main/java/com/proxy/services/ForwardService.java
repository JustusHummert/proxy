package com.proxy.services;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

public interface ForwardService {
    //forward the request to the target url
    public Mono<ResponseEntity<byte[]>> forwardRequest(HttpServletRequest request, String url, MultiValueMap<String, String> parameters);
}