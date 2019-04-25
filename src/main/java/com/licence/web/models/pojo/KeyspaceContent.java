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
import java.util.Map;

@Data
@Builder
public class KeyspaceContent {
    KeyspaceContentObject tables;;
    KeyspaceContentObject columns;
    KeyspaceContentObject aggregates;
    KeyspaceContentObject indexes;
    KeyspaceContentObject functions;
    KeyspaceContentObject droppedColumns;
    KeyspaceContentObject triggers;
    KeyspaceContentObject types;
    KeyspaceContentObject views;
}
