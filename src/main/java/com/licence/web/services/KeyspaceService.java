package com.licence.web.services;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.DriverException;
import com.licence.config.properties.QueryProperties;
import com.licence.web.models.Keyspace;
import com.licence.web.models.pojo.KeyspaceContent;
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
import java.util.List;
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
        KeyspaceContent keyspaceContent = KeyspaceContent.builder()
                .tables(getTables(keyspaceName))
                .aggregates(getAggregates(keyspaceName))
                .functions(getFunctions(keyspaceName))
                .types(getTypes(keyspaceName))
                .views(getViews(keyspaceName))
                .columns(new ArrayList<>())
                .droppedColumns(new ArrayList<>())
                .indexes(new ArrayList<>())
                .triggers(new ArrayList<>())
                .build();
        keyspaceContent.getTables().forEach(p -> {
            keyspaceContent.getColumns().addAll(getColumns(keyspaceName, p.getTable_name()));
            keyspaceContent.getDroppedColumns().addAll(getDroppedColumns(keyspaceName, p.getTable_name()));
            keyspaceContent.getIndexes().addAll(getIndexes(keyspaceName, p.getTable_name()));
            keyspaceContent.getTriggers().addAll(getTriggers(keyspaceName, p.getTable_name()));
        });
        return keyspaceContent;
    }

    public List<Table> getTables(String keyspaceName) {
        String query = String.format(queryProperties.getSelectKeyspace().get("tables"), "*", keyspaceName);
        return adminOperations.getCqlOperations().query(query, new RowMapper<Table>() {
            @Nullable
            @Override
            public Table mapRow(Row row, int rowNum) throws DriverException {
                return Table.builder()
                        .table_name(row.getString("table_name"))
                        .keyspace_name(row.getString("keyspace_name"))
                        .bloom_filter_fp_chance(row.getDouble("bloom_filter_fp_chance"))
                        .caching(row.getMap("caching", String.class, String.class))
                        .cdc(row.getBool("cdc"))
                        .comment(row.getString("comment"))
                        .compaction(row.getMap("compaction", String.class, String.class))
                        .compression(row.getMap("compression", String.class, String.class))
                        .crc_check_chance(row.getDouble("crc_check_chance"))
                        .dclocal_read_repair_chance(row.getDouble("dclocal_read_repair_chance"))
                        .default_time_to_live(row.getInt("default_time_to_live"))
                        .extensions(row.getMap("extensions", String.class, ByteBuffer.class))
                        .flags(row.getSet("flags", String.class))
                        .gc_grace_seconds(row.getInt("gc_grace_seconds"))
                        .id(row.getUUID("id"))
                        .max_index_interval(row.getInt("max_index_interval"))
                        .memtable_flush_period_in_ms(row.getInt("memtable_flush_period_in_ms"))
                        .min_index_interval(row.getInt("min_index_interval"))
                        .read_repair_chance(row.getDouble("read_repair_chance"))
                        .speculative_retry(row.getString("speculative_retry"))
                        .build();
            }
        });
    }

    public List<Trigger> getTriggers(String keyspaceName, String tableName) {
        String query = String.format(queryProperties.getSelectKeyspace().get("tableTriggers"), "*", keyspaceName, tableName);
        return adminOperations.getCqlOperations().query(query, new RowMapper<Trigger>() {
            @Nullable
            @Override
            public Trigger mapRow(Row row, int rowNum) throws DriverException {
                return Trigger.builder()
                        .keyspace_name(row.getString("keyspace_name"))
                        .table_name(row.getString("table_name"))
                        .trigger_name(row.getString("trigger_name"))
                        .options(row.getMap("options", String.class, String.class))
                        .build();
            }
        });
    }

    public List<Function> getFunctions(String keyspaceName) {
        String query = String.format(queryProperties.getSelectKeyspace().get("functions"), "*", keyspaceName);
        return adminOperations.getCqlOperations().query(query, new RowMapper<Function>() {
            @Nullable
            @Override
            public Function mapRow(Row row, int rowNum) throws DriverException {
                return Function.builder()
                        .keyspace_name(row.getString("keyspace_name"))
                        .function_name(row.getString("function_name"))
                        .argument_types(row.getList("argument_types", String.class))
                        .argument_names(row.getList("argument_names", String.class))
                        .body(row.getString("body"))
                        .called_on_null_input(row.getBool("called_on_null_input"))
                        .language(row.getString("language"))
                        .return_type(row.getString("return_type"))
                        .build();
            }
        });
    }

    public List<Aggregate> getAggregates(String keyspaceName) {
        String query = String.format(queryProperties.getSelectKeyspace().get("aggregates"), "*", keyspaceName);
        return adminOperations.getCqlOperations().query(query, new RowMapper<Aggregate>() {
            @Nullable
            @Override
            public Aggregate mapRow(Row row, int rowNum) throws DriverException {
                return Aggregate.builder()
                        .keyspace_name(row.getString("keyspace_name"))
                        .aggregate_name(row.getString("aggregate_name"))
                        .argument_types(row.getList("argument_types", String.class))
                        .final_func(row.getString("final_func"))
                        .initcond(row.getString("initcond"))
                        .return_type(row.getString("return_type"))
                        .state_func(row.getString("state_func"))
                        .state_type(row.getString("state_type"))
                        .build();
            }
        });
    }

    public List<View> getViews(String keyspaceName) {
        String query = String.format(queryProperties.getSelectKeyspace().get("views"), "*", keyspaceName);
        return adminOperations.getCqlOperations().query(query, new RowMapper<View>() {
            @Nullable
            @Override
            public View mapRow(Row row, int rowNum) throws DriverException {
                return View.builder()
                        .keyspace_name(row.getString("keyspace_name"))
                        .view_name(row.getString("view_name"))
                        .base_table_id(row.getUUID("base_table_id"))
                        .base_table_name(row.getString("base_table_name"))
                        .bloom_filter_fp_chance(row.getDouble("bloom_filter_fp_chance"))
                        .caching(row.getMap("caching", String.class, String.class))
                        .cdc(row.getBool("cdc"))
                        .comment(row.getString("comment"))
                        .compaction(row.getMap("compaction", String.class, String.class))
                        .compression(row.getMap("compression", String.class, String.class))
                        .crc_check_chance(row.getDouble("crc_check_chance"))
                        .dclocal_read_repair_chance(row.getDouble("dclocal_read_repair_chance"))
                        .default_time_to_live(row.getInt("default_time_to_live"))
                        .extensions(row.getMap("extensions", String.class, ByteBuffer.class))
                        .gc_grace_seconds(row.getInt("gc_grace_seconds"))
                        .id(row.getUUID("id"))
                        .include_all_columns(row.getBool("include_all_columns"))
                        .max_index_interval(row.getInt("max_index_interval"))
                        .memtable_flush_period_in_ms(row.getInt("memtable_flush_period_in_ms"))
                        .min_index_interval(row.getInt("min_index_interval"))
                        .read_repair_chance(row.getDouble("read_repair_chance"))
                        .speculative_retry(row.getString("speculative_retry"))
                        .where_clause(row.getString("where_clause"))
                        .build();
            }
        });
    }

    public List<Index> getIndexes(String keyspaceName, String tableName) {
        String query = String.format(queryProperties.getSelectKeyspace().get("tableIndexes"), "*", keyspaceName, tableName);
        return adminOperations.getCqlOperations().query(query, new RowMapper<Index>() {
            @Nullable
            @Override
            public Index mapRow(Row row, int rowNum) throws DriverException {
                return Index.builder()
                        .keyspace_name(row.getString("keyspace_name"))
                        .table_name(row.getString("table_name"))
                        .index_name(row.getString("index_name"))
                        .kind(row.getString("kind"))
                        .options(row.getMap("options", String.class, String.class))
                        .build();
            }
        });
    }

    public List<Type> getTypes(String keyspaceName) {
        String query = String.format(queryProperties.getSelectKeyspace().get("types"), "*", keyspaceName);
        return adminOperations.getCqlOperations().query(query, new RowMapper<Type>() {
            @Nullable
            @Override
            public Type mapRow(Row row, int rowNum) throws DriverException {
                return Type.builder()
                        .keyspace_name(row.getString("keyspace_name"))
                        .type_name(row.getString("type_name"))
                        .field_names(row.getList("field_names", String.class))
                        .field_types(row.getList("field_types", String.class))
                        .build();
            }
        });
    }

    public List<DroppedColumn> getDroppedColumns(String keyspaceName, String tableName) {
        String query = String.format(queryProperties.getSelectKeyspace().get("tableDroppedColumns"), "*", keyspaceName, tableName);
        return adminOperations.getCqlOperations().query(query, new RowMapper<DroppedColumn>() {
            @Nullable
            @Override
            public DroppedColumn mapRow(Row row, int rowNum) throws DriverException {
                return DroppedColumn.builder()
                        .keyspace_name(row.getString("keyspace_name"))
                        .table_name(row.getString("table_name"))
                        .column_name(row.getString("column_name"))
                        .dropped_time(row.getTimestamp("dropped_time"))
                        .type(row.getString("type"))
                        .build();
            }
        });
    }

    public List<Column> getColumns(String keyspaceName, String tableName) {
        String query = String.format(queryProperties.getSelectKeyspace().get("tableColumns"), "*", keyspaceName, tableName);
        return adminOperations.getCqlOperations().query(query, new RowMapper<Column>() {
            @Nullable
            @Override
            public Column mapRow(Row row, int rowNum) throws DriverException {
                return Column.builder()
                        .keyspace_name(row.getString("keyspace_name"))
                        .table_name(row.getString("table_name"))
                        .column_name(row.getString("column_name"))
                        .clustering_order(row.getString("clustering_order"))
                        .column_name_bytes(row.getBytes("column_name_bytes"))
                        .kind(row.getString("kind"))
                        .position(row.getInt("position"))
                        .type(row.getString("type"))
                        .build();
            }
        });
    }
}
