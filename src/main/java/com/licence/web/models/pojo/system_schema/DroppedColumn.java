package com.licence.web.models.pojo.system_schema;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class DroppedColumn {
    private String keyspace_name;
    private String table_name;
    private String column_name;
    private Date dropped_time;
    private String type;
}
