package com.licence.config.cassandra;

import com.licence.config.getters.ResourceGetter;
import com.licence.config.properties.CassandraProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.CassandraClusterFactoryBean;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.core.CassandraAdminOperations;
import org.springframework.data.cassandra.core.CassandraAdminTemplate;
import org.springframework.data.cassandra.core.convert.CassandraCustomConversions;
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification;
import org.springframework.data.cassandra.core.cql.keyspace.KeyspaceOption;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import org.springframework.data.convert.CustomConversions;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Configuration
@EnableCassandraRepositories
public class CassandraConfig extends AbstractCassandraConfiguration {

    @Autowired
    private CassandraProperties properties;

    @Nullable
    @Override
    protected String getClusterName() {
        return properties.getClusterName();
    }

    @Bean
    @Override
    public CassandraClusterFactoryBean cluster() {
        CassandraClusterFactoryBean cluster = super.cluster();
        cluster.setUsername(properties.getUsername());
        cluster.setPassword(properties.getPassword());
        return cluster;
    }

    @Override
    protected int getPort() {
        return Integer.parseInt(properties.getPort());
    }

    @Override
    protected String getContactPoints() {
        return properties.getContactPoints();
    }

    @Override
    protected List<CreateKeyspaceSpecification> getKeyspaceCreations() {
        return Collections.singletonList(CreateKeyspaceSpecification
                .createKeyspace(properties.getKeyspaceName()).ifNotExists()
                .with(KeyspaceOption.DURABLE_WRITES, true)
                .withSimpleReplication(3));
    }

    @Override
    protected String getKeyspaceName() {
        return properties.getKeyspaceName();
    }

    @Override
    public SchemaAction getSchemaAction() {
        // If system_schema is RECREATED then materialized view must be dropped before recreating the tables
        return SchemaAction.CREATE_IF_NOT_EXISTS;
    }

    @Override
    @Bean(name = "operations")
    public CassandraAdminOperations cassandraTemplate() {
        return new CassandraAdminTemplate(session().getObject(), cassandraConverter());
    }

    @Override
    public String[] getEntityBasePackages() {
        return new String[] {"com.licence"};
    }

    @Override
    public CustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        return new CassandraCustomConversions(converters);
    }

    // this function is called before creating the tables
    @Override
    protected List<String> getStartupScripts() {
        List<String> scripts = getScripts("static/cql/startUpScripts");
        //return scripts;
        return super.getStartupScripts();
    }

    @Override
    protected List<String> getShutdownScripts() {
        List<String> scripts = getScripts("static/cql/shutDownScripts");
        //return scripts;
        return super.getShutdownScripts();
    }

    private List<String> getScripts(String path) {
        List<String> strings = new ArrayList<>();

        // get the content of the file removing new lines/tabs/etc and splits it by ";"
        String[] scripts = new ResourceGetter(path).getFileToString("^(?:[\\t ]*(?:\\r?\\n|\\r))+", "").split(";");
        Stream<String> stream = Arrays.stream(scripts);
        // remove any other spaces and put the content into a list of strings
        stream.filter(s -> s.trim().length() > 0).forEach(s -> {
            strings.add(s.trim());
        });
        stream.close();
        return strings;
    }
}
