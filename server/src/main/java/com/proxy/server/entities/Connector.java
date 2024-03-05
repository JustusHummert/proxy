package com.proxy.server.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

//Entity to store the connectors from ID to url
@Entity
public class Connector {
    //ID of the connector
    @Id
    private String id;

    private String url;

    public Connector() {}
    public Connector(String id, String url) {
        this.id = id;
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
