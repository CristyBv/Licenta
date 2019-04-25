package com.licence.web.services;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.DriverException;
import com.licence.config.properties.QueryProperties;
import com.licence.web.models.Keyspace;
import com.licence.web.models.pojo.KeyspaceContent;
import com.licence.web.models.pojo.KeyspaceContentObject;
import com.licence.web.models.pojo.system_schema.Aggregate;
import com.licence.web.models.pojo.system_schema.Column;
import com.licence.web.models.pojo.system_schema.DroppedColumn;
import com.licence.web.models.pojo.system_schema.Function;
import com.licence.web.models.pojo.system_schema.Index;
import com.licence.web.models.pojo.system_schema.Table;
import com.licence.web.models.pojo.system_schema.Trigger;
import com.licence.web.models.pojo.system_schema.Type;
import com.licence.web.models.pojo.system_schema.View;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.cassandra.core.CassandraAdminOperations;
import org.springframework.data.cassandra.core.cql.RowMapper;
import org.springframework.lang.Nullable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    public void save(Keyspace keyspace, Boolean physicCreate, Boolean physicEdit) {
        if (keyspace.getId() == null)
            keyspace.setId(UUID.randomUUID().toString());
        if (keyspace.getPassword().length() <= 40)
            keyspace.setPassword(bCryptPasswordEncoder.encode(keyspace.getPassword()));
        if (keyspace.getCreationDate() == null)
            keyspace.setCreationDate(Calendar.getInstance().getTime());
        if (keyspace.getLog() == null)
            keyspace.setLog(new ArrayList<>());
        adminOperations.insert(keyspace);
        if (physicCreate)
            createKeyspace(keyspace);
        if (physicEdit)
            alterKeyspace(keyspace);
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

    private void alterKeyspace(Keyspace keyspace) {
        String query = String.format(queryProperties.getAlterKeyspace(), keyspace.getName(), keyspace.getReplicationFactor(), keyspace.isDurableWrites());
        System.out.println(query);
        adminOperations.getCqlOperations().execute(query);
    }

    public void deleteKeyspace(Keyspace keyspace, boolean physicDelete) {
        adminOperations.delete(keyspace);
        if (physicDelete) {
            String query = String.format(queryProperties.getDropKeyspace(), keyspace.getName());
            System.out.println(query);
            adminOperations.getCqlOperations().execute(query);
        }
    }

    public String getAdminKeyspaceName() {
        return adminOperations.getKeyspaceMetadata().getName();
    }

    public KeyspaceContent getKeyspaceContent(String keyspaceName) {
        String queryTables = String.format(queryProperties.getSelectKeyspace().get("tables"), "*", keyspaceName);

        String queryFunctions = String.format(queryProperties.getSelectKeyspace().get("functions"), "*", keyspaceName);
        String queryAggregates = String.format(queryProperties.getSelectKeyspace().get("aggregates"), "*", keyspaceName);
        String queryViews = String.format(queryProperties.getSelectKeyspace().get("views"), "*", keyspaceName);
        String queryTypes = String.format(queryProperties.getSelectKeyspace().get("types"), "*", keyspaceName);

        KeyspaceContent keyspaceContent = KeyspaceContent.builder()
                .tables(getKeyspaceContentObject(queryTables))
                .aggregates(getKeyspaceContentObject(queryAggregates))
                .functions(getKeyspaceContentObject(queryFunctions))
                .types(getKeyspaceContentObject(queryTypes))
                .views(getKeyspaceContentObject(queryViews))
                .columns(new KeyspaceContentObject(null, new ArrayList<>()))
                .droppedColumns(new KeyspaceContentObject(null, new ArrayList<>()))
                .indexes(new KeyspaceContentObject(null, new ArrayList<>()))
                .triggers(new KeyspaceContentObject(null, new ArrayList<>()))
                .build();
        keyspaceContent.getTables().getContent().forEach(m -> {
            String queryTriggers = String.format(queryProperties.getSelectKeyspace().get("tableTriggers"), "*", keyspaceName, m.get("table_name"));
            String queryIndexes = String.format(queryProperties.getSelectKeyspace().get("tableIndexes"), "*", keyspaceName, m.get("table_name"));
            String queryDroppedColumns = String.format(queryProperties.getSelectKeyspace().get("tableDroppedColumns"), "*", keyspaceName, m.get("table_name"));
            String queryColumns = String.format(queryProperties.getSelectKeyspace().get("tableColumns"), "*", keyspaceName, m.get("table_name"));
            KeyspaceContentObject columnsObj = getKeyspaceContentObject(queryColumns);
            KeyspaceContentObject droppedColumnsObj = getKeyspaceContentObject(queryDroppedColumns);
            KeyspaceContentObject indexesObj = getKeyspaceContentObject(queryIndexes);
            KeyspaceContentObject triggersObj = getKeyspaceContentObject(queryTriggers);
            keyspaceContent.getColumns().getContent().addAll(columnsObj.getContent());
            keyspaceContent.getColumns().setColumnDefinitions(columnsObj.getColumnDefinitions());
            keyspaceContent.getDroppedColumns().getContent().addAll(droppedColumnsObj.getContent());
            keyspaceContent.getDroppedColumns().setColumnDefinitions(droppedColumnsObj.getColumnDefinitions());
            keyspaceContent.getIndexes().getContent().addAll(indexesObj.getContent());
            keyspaceContent.getIndexes().setColumnDefinitions(indexesObj.getColumnDefinitions());
            keyspaceContent.getTriggers().getContent().addAll(triggersObj.getContent());
            keyspaceContent.getTriggers().setColumnDefinitions(triggersObj.getColumnDefinitions());
        });
        return keyspaceContent;
    }

    private Map<String, Object> getRowMap(Row row) {
        Map<String, Object> map = new LinkedHashMap<>();
        row.getColumnDefinitions().forEach(p ->  {
            map.put(p.getName(), row.getObject(p.getName()));
        });
        return map;
    }

    public KeyspaceContentObject getKeyspaceContentObject(String query) {
        KeyspaceContentObject keyspaceContentObject = new KeyspaceContentObject();
        List<Map<String, Object>> contentList = adminOperations.getCqlOperations().query(query, new RowMapper<Map<String,Object>>() {
            @Nullable
            @Override
            public Map<String,Object> mapRow(Row row, int rowNum) throws DriverException {
                keyspaceContentObject.setColumnDefinitions(row.getColumnDefinitions());
                return getRowMap(row);
            }
        });
        keyspaceContentObject.setContent(contentList);
        return keyspaceContentObject;
    }

}
