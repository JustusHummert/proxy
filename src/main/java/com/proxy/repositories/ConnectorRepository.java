package com.proxy.repositories;

import com.proxy.entities.Connector;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConnectorRepository extends CrudRepository<Connector, String>{
}
