package com.licence.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "keyspace")
public class KeyspaceProperties {
    private String creator;
    private String admin;
    private String editor;
    private String member;
    private Map<String, String> panel;
    private Map<String, String> search;
    private Map<String, String> log;
    private Map<String, String> data;
}
