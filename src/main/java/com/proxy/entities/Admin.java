package com.proxy.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Admin {
    //This is the id of the admin always 0 as there is only one admin
    @Id
    private Integer id = 0;

    //This is the password of the admin
    private String password;


    public Admin() {
    }

    public Admin(String password) {
        this.password = password;
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
}
