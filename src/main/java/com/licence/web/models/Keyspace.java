package com.licence.web.models;

import com.datastax.driver.core.DataType;
import com.licence.config.validation.password.match.PasswordMatches;
import com.licence.web.models.UDT.KeyspaceLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.List;

@Data
@Builder
@Table(value = "keyspaces")
@NoArgsConstructor
@AllArgsConstructor
@PasswordMatches
public class Keyspace {

    @PrimaryKey("KEYSPACE_ID")
    private String id;

    @NotEmpty
    @Column("NAME")
    private String name;

    @Column("PASSWORD_ENABLED")
    private boolean passwordEnabled;

    @Column("PASSWORD")
    @Size(min = 5)
    private String password;

    @Transient
    private String matchingPassword;

    @Column("CREATION_DATE")
    @DateTimeFormat
    private Date creationDate;

    @CassandraType(type = DataType.Name.UDT, userTypeName = "keyspace_log")
    private List<KeyspaceLog> log;

}
