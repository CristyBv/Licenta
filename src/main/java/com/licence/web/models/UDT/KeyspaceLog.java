package com.licence.web.models.UDT;

import org.springframework.data.annotation.Transient;
import com.licence.web.models.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@UserDefinedType("keyspace_log")
public class KeyspaceLog {
    private String content;
    private Date date;
    private String username;
    private String type;
    @Transient
    private User user;
}
