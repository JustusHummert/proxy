package com.proxy.services;

import com.proxy.repositories.AdminRepository;
import com.proxy.repositories.ConnectorRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;

public interface AdminService {
    //handle requests to /admin
    public Object handleRequest(HttpServletRequest request, MultiValueMap<String, String> parameters,
                                       Model model, ConnectorRepository connectorRepository,
                                       AdminRepository adminRepository);
}
