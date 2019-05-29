package com.licence.web.models;

import com.datastax.driver.core.DataType;
import com.licence.config.validation.password.match.PasswordMatches;
import com.licence.web.models.UDT.KeyspaceLog;
import com.licence.web.models.UDT.KeyspaceUser;
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

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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
    @Size(min=3, max = 15)
    @Pattern(regexp = "[a-zA-Z][a-zA-Z0-9]+") // must start with a letter
    private String name;

    @Column("PASSWORD_ENABLED")
    private boolean passwordEnabled;

    @Column("PASSWORD")
    @Pattern(regexp = "^(?:.{5,20}|)$") // empty or min 5 max 20
    private String password;

    @Transient
    private String matchingPassword;

    @Column("CREATION_DATE")
    @DateTimeFormat
    private Date creationDate;

    @Column("REPLICATION_FACTOR")
    @Min(1)
    @Max(8)
    private Integer replicationFactor;

    @Column("DURABLE_WRITES")
    private boolean durableWrites;

    @CassandraType(type = DataType.Name.UDT, userTypeName = "keyspace_log")
    private List<KeyspaceLog> log;

    @CassandraType(type = DataType.Name.UDT, userTypeName = "keyspace_user")
    private List<KeyspaceUser> users;

    @Transient
    public void addLog(String type, String content, String username) {
        KeyspaceLog keyspaceLog = new KeyspaceLog();
        keyspaceLog.setDate(Calendar.getInstance().getTime());
        keyspaceLog.setType(type);
        keyspaceLog.setUsername(username);
        keyspaceLog.setContent(content);
        if(this.getLog() == null)
            this.setLog(new ArrayList<>());
        this.getLog().add(keyspaceLog);
    }
}
