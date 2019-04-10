package com.licence.web.models.UDT;

import com.licence.web.models.Keyspace;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

@Data
@AllArgsConstructor
@NoArgsConstructor
@UserDefinedType("user_keyspace")
public class UserKeyspace {

    public UserKeyspace(String name, String access, String creatorName, boolean active) {
        this.name = name;
        this.access = access;
        this.creatorName = creatorName;
        this.active = active;
    }

    @Transient

    private Keyspace keyspace;
    private String name;
    private String access;
    private String creatorName;
    private boolean active;
}
