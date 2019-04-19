package com.licence.config.context;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.DriverException;
import com.licence.config.getters.ResourceGetter;
import com.licence.config.properties.QueryProperties;
import com.licence.web.models.Keyspace;
import com.licence.web.models.UDT.KeyspaceLog;
import com.licence.web.models.UDT.KeyspaceUser;
import com.licence.web.models.UDT.UserKeyspace;
import com.licence.web.models.User;
import com.licence.web.services.KeyspaceService;
import com.licence.web.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.MessageSource;
import org.springframework.data.cassandra.core.CassandraAdminOperations;
import org.springframework.data.cassandra.core.cql.RowMapper;
import org.springframework.data.cassandra.core.query.Query;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.data.cassandra.core.query.Criteria.where;

// after run functions starts from main class, this method (run) is called
@Component
public class CommandLineAppStartUpRunner implements CommandLineRunner {
    private final UserService userService;
    private final CassandraAdminOperations adminOperations;
    private final KeyspaceService keyspaceService;
    private final QueryProperties queryProperties;

    @Autowired
    public CommandLineAppStartUpRunner(UserService userService, @Qualifier("operations") CassandraAdminOperations adminOperations, KeyspaceService keyspaceService, QueryProperties queryProperties) {
        this.userService = userService;
        this.adminOperations = adminOperations;
        this.keyspaceService = keyspaceService;
        this.queryProperties = queryProperties;
    }

    @Override
    public void run(String... args) throws Exception {
        //insertAdmin();
        //insertKeyspaceAdmin();
        //executeStartUpScriptsAfter();
    }

    // after spring starts, some scripts are executed from a startUpScriptsAfter file
    private void executeStartUpScriptsAfter() {
        String[] startUpScripts = getScripts("static/cql/startUpScriptsAfter");
        Stream<String> stream = Arrays.stream(startUpScripts);
        // remove any other spaces
        stream.filter(s -> s.trim().length() > 0).forEach(s -> {
            adminOperations.getCqlOperations().execute(s.trim());
        });
        stream.close();
    }


    private void CqlBatch(String[] startUpCql, String logged, String timestamp) {
        // get batch format
        String batch = new ResourceGetter("static/cql/batch").getFileToString(null, null).replaceAll("(?m)^[ \t]*\r?\n", "");
        // prepare cql commands for batch
        Stream<String> stream = Arrays.stream(startUpCql);
        String scripts = stream.collect(Collectors.joining(";\n"));
        stream.close();
        // replace in batch format
        String executeBatch = String.format(batch, logged, timestamp, scripts);
    }

    private void insertAdmin() {
        User user = User.builder()
                .id("d2971b26-b13b-4f6d-9f25-70d685874550")
                .email("cristybv1@gmail.com")
                .userName("CristyBv")
                .password("exp112")
                .enabled(true)
                .keyspaces(Collections.singletonList(new UserKeyspace("admin", "FULL", "CristyBv")))
                .build();
        userService.registerNewAdmin(user);

        user = User.builder()
                .id("d2971b26-b13b-4f6d-9f25-70d685874551")
                .email("cristy_bv1@yahoo.com")
                .userName("Cristian")
                .password("exp112")
                .enabled(true)
                .keyspaces(new ArrayList<>())
                .build();
        userService.registerNewAdmin(user);

        user = User.builder()
                .id("d2971b26-b13b-4f6d-9f25-70d685874552")
                .email("abc@abc.com")
                .userName("Abcde")
                .password("exp112")
                .enabled(true)
                .keyspaces(new ArrayList<>())
                .build();
        userService.registerNewAdmin(user);
    }

    private void insertKeyspaceAdmin() {
        Keyspace keyspace = Keyspace.builder()
                .id("d2971b26-b13b-4f6d-9f25-70d685874551")
                .name("CristyBv_admin")
                .passwordEnabled(true)
                .password("exp112")
                .log(new ArrayList<>())
                .replicationFactor(3)
                .durableWrites(true)
                .users(Collections.singletonList(new KeyspaceUser("CristyBv","FULL")))
                .build();
        keyspaceService.save(keyspace, false);
    }

    private String[] getScripts(String path) {
        return new ResourceGetter(path).getFileToString("^(?:[\\t ]*(?:\\r?\\n|\\r))+", "").split(";");
    }
}
