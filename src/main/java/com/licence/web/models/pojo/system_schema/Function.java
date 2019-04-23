package com.licence.web.models.pojo.system_schema;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Function {
    private String keyspace_name;
    private String function_name;
    private List<String> argument_types;
    private List<String> argument_names;
    private String body;
    private boolean called_on_null_input;
    private String language;
    private String return_type;
}
