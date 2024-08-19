package com.proxy.services;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import reactor.core.publisher.Mono;

import java.io.IOError;
import java.io.IOException;

public interface ForwardService {
    //forward the request to the target url
    public Mono<ResponseEntity<byte[]>> forwardRequest(HttpServletRequest request, String url, MultiValueMap<String, String> parameters)
    throws IOException;
}