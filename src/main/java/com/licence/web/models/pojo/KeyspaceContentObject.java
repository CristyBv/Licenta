package com.licence.web.models.pojo;

import com.datastax.driver.core.ColumnDefinitions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeyspaceContentObject {
    String tableName;
    ColumnDefinitions columnDefinitions;
    List<Map<String, Object>> content;
}
