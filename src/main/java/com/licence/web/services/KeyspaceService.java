package com.licence.web.services;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.DriverException;
import com.licence.config.properties.QueryProperties;
import com.licence.web.models.Keyspace;
import com.licence.web.models.pojo.KeyspaceContent;
import com.licence.web.models.pojo.KeyspaceContentObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.cassandra.core.CassandraAdminOperations;
import org.springframework.data.cassandra.core.cql.RowMapper;
import org.springframework.lang.Nullable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
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
                .tables(getKeyspaceContentObject(queryTables, "tables"))
                .aggregates(getKeyspaceContentObject(queryAggregates, "aggregates"))
                .functions(getKeyspaceContentObject(queryFunctions, "functions"))
                .types(getKeyspaceContentObject(queryTypes, "types"))
                .views(getKeyspaceContentObject(queryViews, "views"))
                .columns(new KeyspaceContentObject("columns", null, new ArrayList<>()))
                .droppedColumns(new KeyspaceContentObject("dropped columns", null, new ArrayList<>()))
                .indexes(new KeyspaceContentObject("indexes", null, new ArrayList<>()))
                .triggers(new KeyspaceContentObject("triggers", null, new ArrayList<>()))
                .build();
        keyspaceContent.getTables().getContent().forEach(m -> {
            String queryTriggers = String.format(queryProperties.getSelectKeyspace().get("tableTriggers"), "*", keyspaceName, m.get("table_name"));
            String queryIndexes = String.format(queryProperties.getSelectKeyspace().get("tableIndexes"), "*", keyspaceName, m.get("table_name"));
            String queryDroppedColumns = String.format(queryProperties.getSelectKeyspace().get("tableDroppedColumns"), "*", keyspaceName, m.get("table_name"));
            String queryColumns = String.format(queryProperties.getSelectKeyspace().get("tableColumns"), "*", keyspaceName, m.get("table_name"));
            KeyspaceContentObject columnsObj = getKeyspaceContentObject(queryColumns, "columns");
            KeyspaceContentObject droppedColumnsObj = getKeyspaceContentObject(queryDroppedColumns, "dropped columns");
            KeyspaceContentObject indexesObj = getKeyspaceContentObject(queryIndexes, "indexes");
            KeyspaceContentObject triggersObj = getKeyspaceContentObject(queryTriggers, "triggers");
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
        row.getColumnDefinitions().forEach(p -> {
            map.put(p.getName(), row.getObject(p.getName()));
        });
        return map;
    }

    public KeyspaceContentObject getKeyspaceContentObject(String query, String name) {
        KeyspaceContentObject keyspaceContentObject = new KeyspaceContentObject();
        keyspaceContentObject.setTableName(name);
        List<Map<String, Object>> contentList = adminOperations.getCqlOperations().query(query, new RowMapper<Map<String, Object>>() {
            @Nullable
            @Override
            public Map<String, Object> mapRow(Row row, int rowNum) throws DriverException {
                keyspaceContentObject.setColumnDefinitions(row.getColumnDefinitions());
                return getRowMap(row);
            }
        });
        keyspaceContentObject.setContent(contentList);
        return keyspaceContentObject;
    }

    public KeyspaceContentObject getSelectSimple(String keyspace, String table, String whatToSelect) {
        String query = String.format(queryProperties.getSelect().get("simple"), whatToSelect, keyspace + "." + table);
        return getKeyspaceContentObject(query, table);
    }

    public void update(String keyspace, String table, String options, String set, String where) throws Exception {
        String query = String.format(queryProperties.getUpdate(), keyspace + "." + table, options, set, where);
        System.out.println(query);
        try {
            adminOperations.getCqlOperations().execute(query);
        } catch (Exception e) {
            throw new Exception(query + " ---> " + e.getCause().getMessage());
        }

    }

    public void delete(String delete, String keyspace, String table, String options, String where) throws Exception {
        String query = String.format(queryProperties.getDelete(), delete, keyspace + "." + table, options, where);
        System.out.println(query);
        try {
            adminOperations.getCqlOperations().execute(query);
        } catch (Exception e) {
            throw new Exception(query + " ---> " + e.getCause().getMessage());
        }
    }

    public void insert(String keyspace, String table, String insertColumns, String insertValues, String options) throws Exception {
        String query = String.format(queryProperties.getInsert(), keyspace + "." + table, insertColumns, insertValues, options);
        System.out.println(query);
        try {
            adminOperations.getCqlOperations().execute(query);
        } catch (Exception e) {
            throw new Exception(query + " ---> " + e.getCause().getMessage());
        }
    }

    public void alterOptions(String keyspace, String table, String with) throws Exception {
        String query = String.format(queryProperties.getAlter().get("options"), keyspace + "." + table, with);
        System.out.println(query);
        try {
            adminOperations.getCqlOperations().execute(query);
        } catch (Exception e) {
            throw new Exception(query + " ---> " + e.getCause().getMessage());
        }
    }

    public void dropTable(String keyspace, String table) throws Exception {
        String query = String.format(queryProperties.getDrop().get("table"), keyspace + "." + table);
        System.out.println(query);
        try {
            adminOperations.getCqlOperations().execute(query);
        } catch (Exception e) {
            throw new Exception(query + " ---> " + e.getCause().getMessage());
        }
    }

    public void createTable(String keyspace, String table, String columnsDefinitions, String keys) throws Exception {
        String query = String.format(queryProperties.getCreate().get("table"), keyspace+"."+table, columnsDefinitions, keys);
        System.out.println(query);
        try {
            adminOperations.getCqlOperations().execute(query);
        } catch (Exception e) {
            throw new Exception(query + " ---> " + e.getCause().getMessage());
        }
    }
}
