package com.proxy.controller;

import com.proxy.entities.Connector;
import com.proxy.services.AdminService;
import com.proxy.services.ForwardService;
import com.proxy.repositories.AdminRepository;
import com.proxy.repositories.ConnectorRepository;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Controller
public class WebController {
    @Autowired
    private ConnectorRepository connectorRepository;
    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private AdminService adminService;
    @Autowired
    private ForwardService forwardService;

    //redirect all request to a Method based on subdomain
    @RequestMapping(value="**")
    public Object redirect(HttpServletRequest request, @RequestParam MultiValueMap<String, String> parameters,
                           Model model){
        //Get the subdomain
        //If there is no subdomain the subdomain is the domain
        String subdomain = request.getServerName().split("\\.")[0];
        if (subdomain.equals("admin")) {
            //If the subdomain is admin the request is handled by the adminService
            return adminService.handleRequest(request, parameters, model, connectorRepository, adminRepository);
        }
        Optional<Connector> optionalConnector = connectorRepository.findById(subdomain);
        if (optionalConnector.isEmpty())
            return Mono.just(ResponseEntity.notFound().build());
        String url = optionalConnector.get().getUrl();
        //ForwardService is used to forward the request to the url
        try {
            return forwardService.forwardRequest(request, url, parameters);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }



}
