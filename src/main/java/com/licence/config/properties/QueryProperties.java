package com.licence.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "query")
public class QueryProperties {
    private Map<String, String> selectUser;
    private Map<String, String> selectKeyspace;
    private String selectKeyspaces;
    private String createKeyspace;
    private String alterKeyspace;
    private String dropKeyspace;
    private String selectUserByPartialUsername;
    private Map<String, String> select;
    private String update;
    private String delete;
    private String insert;
    private Map<String, String> alter;
    private Map<String, String> drop;
    private Map<String, String> create;
    private Map<String, String> syntax;
}
