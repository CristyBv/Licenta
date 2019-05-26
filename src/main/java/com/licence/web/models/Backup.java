package com.licence.web.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.Indexed;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.Date;

@Data
@Builder
@Table(value = "backups")
@NoArgsConstructor
@AllArgsConstructor
public class Backup {

    public Backup(String content, String keyspaceName) {
        this.content = content;
        this.keyspaceName = keyspaceName;
    }

    @PrimaryKey("BACKUP_ID")
    private String id;

    @Column("CONTENT")
    private String content;

    @Column("DATE")
    private Date date;

    @Indexed
    @Column("KEYSPACE_NAME")
    private String keyspaceName;
}
