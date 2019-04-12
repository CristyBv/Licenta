package com.licence.web.models.UDT;

import com.licence.web.models.Keyspace;
import jnr.ffi.annotations.In;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@UserDefinedType("user_keyspace")
public class UserKeyspace {

    public UserKeyspace(String name, String access, String creatorName) {
        this.name = name;
        this.access = access;
        this.creatorName = creatorName;
    }

    @Transient
    private Keyspace keyspace;
    private String name;
    private String access;
    private String creatorName;
}
