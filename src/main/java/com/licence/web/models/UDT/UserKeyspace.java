package com.licence.web.models.UDT;

import com.licence.web.models.Keyspace;
import jnr.ffi.annotations.In;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@UserDefinedType("user_keyspace")
public class UserKeyspace {

    public UserKeyspace(String name, Integer access, String creatorName, boolean active) {
        this.name = name;
        this.access = access;
        this.creatorName = creatorName;
        this.active = active;
    }

    @Transient
    private Keyspace keyspace;
    private String name;
    private Integer access;
    private String creatorName;
    private boolean active;
}
