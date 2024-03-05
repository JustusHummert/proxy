package com.proxy.server.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import com.proxy.server.entities.Admin;
import com.proxy.server.entities.Connector;
import com.proxy.server.repositories.AdminRepository;
import com.proxy.server.repositories.ConnectorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import java.util.Optional;


@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    AdminRepository adminRepository;
    @Autowired
    ConnectorRepository connectorRepository;

    Admin prevAdmin;
    static final String testPassword = "password";

    @BeforeEach
    void setUp() {
        Optional<Admin> prevOptionalAdmin = adminRepository.findById(0);
        prevOptionalAdmin.ifPresent(admin -> prevAdmin = admin);
        String salt = BCrypt.gensalt();
        String password = BCrypt.hashpw(testPassword, salt);
        adminRepository.save(new Admin(password, salt));
    }

    @AfterEach
    void tearDown() {
        adminRepository.save(prevAdmin);
    }

    @Test
    void addConnector() throws Exception{
        //valid request
        mvc.perform(MockMvcRequestBuilders.post("/admin/addConnector")
                .param("id", "example")
                .param("url", "https://example.com")
                .param("password", testPassword)
                .accept(MediaType.ALL))
                .andExpect(content().string(equalTo("example now connected to https://example.com")));
        //id already exists
        mvc.perform(MockMvcRequestBuilders.post("/admin/addConnector")
                        .param("id", "example")
                        .param("url", "https://example.com")
                        .param("password", testPassword)
                        .accept(MediaType.ALL))
                .andExpect(content().string(equalTo("ID already exists")));
        //wrong password
        mvc.perform(MockMvcRequestBuilders.post("/admin/addConnector")
                        .param("id", "example2")
                        .param("url", "https://example.com")
                        .param("password", "wrongPassword")
                        .accept(MediaType.ALL))
                .andExpect(content().string(equalTo("Wrong password")));
        //delete admin password and try to add connector
        adminRepository.deleteById(0);
        mvc.perform(MockMvcRequestBuilders.post("/admin/addConnector")
                        .param("id", "example2")
                        .param("url", "https://example.com")
                        .param("password", testPassword)
                        .accept(MediaType.ALL))
                .andExpect(content().string(equalTo("No admin password set")));
        connectorRepository.deleteById("example");
    }

    @Test
    void removeConnector() throws Exception{
        //add connector and remove it
        connectorRepository.save(new Connector("example", "https://example.com"));
        mvc.perform(MockMvcRequestBuilders.post("/admin/removeConnector")
                .param("id", "example")
                .param("password", testPassword)
                .accept(MediaType.ALL))
                .andExpect(content().string(equalTo("example removed")));
        //try to remove it again
        mvc.perform(MockMvcRequestBuilders.post("/admin/removeConnector")
                        .param("id", "example")
                        .param("password", testPassword)
                        .accept(MediaType.ALL))
                .andExpect(content().string(equalTo("ID does not exist")));
        //wrong password
        mvc.perform(MockMvcRequestBuilders.post("/admin/removeConnector")
                        .param("id", "example")
                        .param("password", "wrongPassword")
                        .accept(MediaType.ALL))
                .andExpect(content().string(equalTo("Wrong password")));
        //delete admin password and try to remove connector
        adminRepository.deleteById(0);
        mvc.perform(MockMvcRequestBuilders.post("/admin/removeConnector")
                        .param("id", "example")
                        .param("password", testPassword)
                        .accept(MediaType.ALL))
                .andExpect(content().string(equalTo("No admin password set")));

    }
}