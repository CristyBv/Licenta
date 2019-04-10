package com.licence.web.models.UDT;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@UserDefinedType("keyspace_log")
public class KeyspaceLog {
    private String content;
    private Date date;
    private String username;
}
