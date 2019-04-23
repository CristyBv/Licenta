package com.licence.web.models.pojo.system_schema;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class Index {
    private String keyspace_name;
    private String table_name;
    private String index_name;
    private String kind;
    private Map<String, String> options;
}
