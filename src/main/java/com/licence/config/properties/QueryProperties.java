package com.licence.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "query")
public class QueryProperties {
    private Map<String,String> selectUser;
    private Map<String,String> selectKeyspace;
    private String createKeyspace;
}
