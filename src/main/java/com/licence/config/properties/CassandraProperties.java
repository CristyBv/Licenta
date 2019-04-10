package com.licence.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "spring.data.cassandra")
public class CassandraProperties {
    private String keyspaceName;
    private String clusterName;
    private String port;
    private String contactPoints;
    private String schemaAction;
    private String username;
    private String password;
}
