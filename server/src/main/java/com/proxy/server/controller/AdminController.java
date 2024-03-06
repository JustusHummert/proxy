package com.proxy.server.controller;

import com.proxy.server.entities.Admin;
import com.proxy.server.entities.Connector;
import com.proxy.server.repositories.AdminRepository;
import com.proxy.server.repositories.ConnectorRepository;
import com.proxy.server.sessionManagement.SessionManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.function.client.WebClient;

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
    public @ResponseBody String addConnector(String id, String url, HttpServletRequest request){
        //check if url starts with http:// or https://  otherwise add https://
        if(!url.startsWith("http://") && !url.startsWith("https://"))
            url = "https://" + url;
        //check if url is valid
        String response =  WebClient.create(url).get().accept(MediaType.ALL).retrieve().bodyToMono(String.class)
                .onErrorReturn("error").block();
        //Invalid Url
        if(response == null || response.equals("error"))
            return "invalid url";
        //Check if the sessionId is correct
        if(!SessionManager.getInstance().valid(request.getSession().getId()))
            return "Invalid session";
        //Check if the id already exists
        if (connectorRepository.existsById(id)) {
            return "ID already exists";
        }
        //Add the connector to the database
        connectorRepository.save(new Connector(id, url));
        return id + " now connected to " + url;
    }

    //Remove a connector from the database
    @PostMapping("/removeConnector")
    public @ResponseBody String removeConnector(String id, HttpServletRequest request){
        //Check if the sessionId is correct
        if(!SessionManager.getInstance().valid(request.getSession().getId()))
            return "Invalid session";
        if (!connectorRepository.existsById(id)) {
            return "ID does not exist";
        }
        connectorRepository.deleteById(id);
        return id + " removed";
    }

    @PostMapping("/login")
    public @ResponseBody String login(String password, HttpServletRequest request){
        Optional<Admin> admin = adminRepository.findById(0);
        if(admin.isEmpty())
            return "No Admin";
        if(!BCrypt.checkpw(password, admin.get().getPassword()))
            return "Wrong Password";
        HttpSession session = request.getSession();
        SessionManager.getInstance().addSession(session.getId());
        return "logged in";
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
