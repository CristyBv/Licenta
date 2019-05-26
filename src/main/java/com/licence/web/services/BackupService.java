package com.licence.web.services;

import com.licence.config.properties.RouteProperties;
import com.licence.web.models.Backup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.cassandra.core.CassandraAdminOperations;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.UUID;

@Service
public class BackupService {
    private final CassandraAdminOperations adminOperations;
    private final RouteProperties routeProperties;

    @Autowired
    public BackupService(@Qualifier("operations") CassandraAdminOperations adminOperations, RouteProperties routeProperties) {
        this.adminOperations = adminOperations;
        this.routeProperties = routeProperties;
    }

    public Backup save(Backup backup) {
        if (backup.getId() == null)
            backup.setId(UUID.randomUUID().toString());
        if (backup.getDate() == null)
            backup.setDate(Calendar.getInstance().getTime());
        adminOperations.insert(backup);
        return backup;
    }

    public Backup getBackup(String id) {
        return adminOperations.selectOneById(id, Backup.class);
    }

    public String getUrlBackupSave(String content, String keyspaceName, String contextPath) {
        if(content == null)
            content = "Json parse error!";
        Backup backup = new Backup(content, keyspaceName);
        return contextPath + routeProperties.getBackup().get("json-download") + "?backupId=" + this.save(backup).getId();
    }
}
