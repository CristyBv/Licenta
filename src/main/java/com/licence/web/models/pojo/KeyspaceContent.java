package com.licence.web.models.pojo;

import com.licence.web.models.pojo.system_schema.Aggregate;
import com.licence.web.models.pojo.system_schema.Column;
import com.licence.web.models.pojo.system_schema.DroppedColumn;
import com.licence.web.models.pojo.system_schema.Function;
import com.licence.web.models.pojo.system_schema.Index;
import com.licence.web.models.pojo.system_schema.Table;
import com.licence.web.models.pojo.system_schema.Trigger;
import com.licence.web.models.pojo.system_schema.Type;
import com.licence.web.models.pojo.system_schema.View;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class KeyspaceContent {
    List<Table> tables;
    List<Column> columns;
    List<Aggregate> aggregates;
    List<Index> indexes;
    List<Function> functions;
    List<DroppedColumn> droppedColumns;
    List<Trigger> triggers;
    List<Type> types;
    List<View> views;
}
