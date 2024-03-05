package com.proxy.server.controller;

import com.proxy.server.entities.Admin;
import com.proxy.server.entities.Connector;
import com.proxy.server.repositories.AdminRepository;
import com.proxy.server.repositories.ConnectorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private ConnectorRepository connectorRepository;
    @Autowired
    private AdminRepository adminRepository;

    private static final String proxy = "https://justushummert.com/";

    //Add a connector to the database
    @PostMapping("/addConnector")
    public @ResponseBody String addConnector(String id, String url, String password){
        Optional<Admin> optionalAdmin = adminRepository.findById(0);
        if(optionalAdmin.isEmpty())
            return "No admin password set";
        Admin admin = optionalAdmin.get();
        String hashedPassword = BCrypt.hashpw(password, admin.getSalt());
        if(!admin.getPassword().equals(hashedPassword))
            return "Wrong password";
        if (connectorRepository.existsById(id)) {
            return "ID already exists";
        }
        connectorRepository.save(new Connector(id, url));
        return id + " now connected to " + url;
    }

    //Remove a connector from the database
    @PostMapping("/removeConnector")
    public @ResponseBody String removeConnector(String id, String password){
        Optional<Admin> optionalAdmin = adminRepository.findById(0);
        if(optionalAdmin.isEmpty())
            return "No admin password set";
        Admin admin = optionalAdmin.get();
        String hashedPassword = BCrypt.hashpw(password, admin.getSalt());
        if(!admin.getPassword().equals(hashedPassword))
            return "Wrong password";
        if (!connectorRepository.existsById(id)) {
            return "ID does not exist";
        }
        connectorRepository.deleteById(id);
        return id + " removed";
    }

    //return the Links to the Connections except home
    @GetMapping("/getConnections")
    public @ResponseBody Set<String> getConnections(){
        Iterable<Connector> connectors = connectorRepository.findAll();
        Set<String> result = new HashSet<>();
        connectors.forEach(
                connector -> {
                if(!connector.getId().equals("home"))
                    result.add(proxy + connector.getId());
                }
        );
        return result;
    }

}
