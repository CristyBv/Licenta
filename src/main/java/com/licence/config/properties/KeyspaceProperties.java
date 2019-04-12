package com.licence.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "keyspace")
public class KeyspaceProperties {
    private String creator;
    private String admin;
    private String editor;
    private String member;
}
