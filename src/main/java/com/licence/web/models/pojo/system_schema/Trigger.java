package com.licence.web.models.pojo.system_schema;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class Trigger {
    private String keyspace_name;
    private String table_name;
    private String trigger_name;
    private Map<String, String> options;
}
