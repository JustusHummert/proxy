package com.proxy.server.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Admin {
    //This is the id of the admin always 0 as there is only one admin
    @Id
    private Integer id = 0;

    //This is the password of the admin
    private String password;

    //This is the salt of the password
    private String salt;

    public Admin() {
    }

    public Admin(String password, String salt) {
        this.password = password;
        this.salt = salt;
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }
}
