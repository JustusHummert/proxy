package com.proxy.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

//Entity to store the connectors from ID to url
@Entity
public class Connector {
    //ID of the connector
    @Id
    private String subdomain;

    private String url;

    public Connector() {}
    public Connector(String subdomain, String url) {
        this.subdomain = subdomain;
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSubdomain() {
        return subdomain;
    }

    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
    }
}
