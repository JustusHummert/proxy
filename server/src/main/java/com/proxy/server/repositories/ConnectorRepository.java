package com.proxy.server.repositories;

import com.proxy.server.entities.Connector;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConnectorRepository extends CrudRepository<Connector, String>{
}
