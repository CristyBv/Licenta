package com.licence.web.models.pojo.system_schema;

import lombok.Builder;
import lombok.Data;

import java.nio.ByteBuffer;

@Data
@Builder
public class Column {
    private String keyspace_name;
    private String table_name;
    private String column_name;
    private String clustering_order;
    private ByteBuffer column_name_bytes;
    private String kind;
    private Integer position;
    private String type;
}
