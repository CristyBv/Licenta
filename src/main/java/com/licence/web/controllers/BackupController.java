package com.licence.web.controllers;

import com.licence.web.models.Backup;
import com.licence.web.models.Keyspace;
import com.licence.web.models.UDT.UserKeyspace;
import com.licence.web.services.BackupService;
import com.licence.web.services.KeyspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class BackupController {

    private final BackupService backupService;
    private final KeyspaceService keyspaceService;

    @Autowired
    public BackupController(BackupService backupService, KeyspaceService keyspaceService) {
        this.backupService = backupService;
        this.keyspaceService = keyspaceService;
    }

    @ResponseBody
    @GetMapping(value = "${route.backup[json-download]}")
    public void downloadBackupJson(@RequestParam(required = false) String backupId,
                                   HttpServletResponse response,
                                   @SessionAttribute(required = false) UserKeyspace userKeyspace) {
        //UserKeyspace userKeyspace = session.getAttribute("userKeyspace");
        Backup backup = backupService.getBackup(backupId);
        if (userKeyspace != null) {
            if (backup != null && backup.getKeyspaceName().equals(userKeyspace.getKeyspace().getName())) {
                response.setHeader("Content-disposition", "attachment; filename=backup.json");
                try {
                    response.getOutputStream().print(backup.getContent());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (backup != null) {
                Keyspace keyspace = keyspaceService.findKeyspaceByName(backup.getKeyspaceName());
                if (keyspace == null) {
                    response.setHeader("Content-disposition", "attachment; filename=backup.json");
                    try {
                        response.getOutputStream().print(backup.getContent());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
