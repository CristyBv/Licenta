package com.licence.web.models.pojo.system_schema;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Aggregate {
    private String keyspace_name;
    private String aggregate_name;
    private List<String> argument_types;
    private String final_func;
    private String initcond;
    private String return_type;
    private String state_func;
    private String state_type;
}
