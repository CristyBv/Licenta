package com.licence.web.models.pojo.system_schema;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class    Type {
    private String keyspace_name;
    private String type_name;
    private List<String> field_names;
    private List<String> field_types;
}
