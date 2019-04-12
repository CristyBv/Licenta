package com.licence.web.models.UDT;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

@Data
@AllArgsConstructor
@UserDefinedType("keyspace_user")
public class KeyspaceUser {
    private String userName;
    private String access;
}
