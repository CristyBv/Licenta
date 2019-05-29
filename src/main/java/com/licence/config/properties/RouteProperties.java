package com.licence.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "route")
public class RouteProperties {
    private String index;
    private String confirmation;
    private String recovery;
    private String register;
    private String login;
    private String logout;
    private String profile;
    private String myDatabase;
    private String updateDeleteRowContent;
    private Map<String, String> get;
    private Map<String, String> keyspace;
    private Map<String, String> searchLive;
    private Map<String, String> search;
    private Map<String, String> delete;
    private Map<String, String> change;
    private Map<String, String> insert;
    private Map<String, String> alter;
    private Map<String, String> create;
    private Map<String, String> drop;
    private Map<String, String> console;
    private Map<String, String> export;
    private Map<String, String> backup;
    private Map<String, String> log;
}
