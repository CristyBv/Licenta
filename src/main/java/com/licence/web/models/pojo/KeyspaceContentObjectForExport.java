package com.licence.web.models.pojo;

import com.datastax.driver.core.ColumnDefinitions;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KeyspaceContentObjectForExport {
    String tableName;
    ColumnDefinitions columnDefinitions;
    List<Map<String, String>> content;
}
