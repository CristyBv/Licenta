package com.licence.web.services;

import com.licence.config.properties.QueryProperties;
import com.licence.web.models.Keyspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.cassandra.core.CassandraAdminOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

@Service
public class KeyspaceService {

    private final CassandraAdminOperations adminOperations;
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    private final QueryProperties queryProperties;

    @Autowired
    public KeyspaceService(@Qualifier("operations") CassandraAdminOperations adminOperations, BCryptPasswordEncoder bCryptPasswordEncoder, QueryProperties queryProperties) {
        this.adminOperations = adminOperations;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.queryProperties = queryProperties;
    }
    
    public void save(Keyspace keyspace, Boolean create) {
        if(keyspace.getId() == null)
            keyspace.setId(UUID.randomUUID().toString());
        if(keyspace.getPassword().length() <= 40)
            keyspace.setPassword(bCryptPasswordEncoder.encode(keyspace.getPassword()));
        if(keyspace.getCreationDate() == null)
            keyspace.setCreationDate(Calendar.getInstance().getTime());
        if(keyspace.getLog() == null)
            keyspace.setLog(new ArrayList<>());
        adminOperations.insert(keyspace);
        if(create)
            createKeyspace(keyspace);
    }

    public Keyspace findKeyspaceByName(String keyspaceName) {
        String query = String.format(queryProperties.getSelectKeyspace().get("by_name"), "*", keyspaceName);
        return adminOperations.selectOne(query, Keyspace.class);
    }

    private void createKeyspace(Keyspace keyspace) {
        String query = String.format(queryProperties.getCreateKeyspace(), keyspace.getName(), keyspace.getReplicationFactor(), keyspace.isDurableWrites());
        System.out.println(query);
        adminOperations.getCqlOperations().execute(query);
    }
}
