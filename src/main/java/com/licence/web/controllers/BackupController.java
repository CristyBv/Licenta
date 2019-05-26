package com.licence.web.controllers;

import com.licence.web.models.Backup;
import com.licence.web.models.UDT.UserKeyspace;
import com.licence.web.services.BackupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.server.WebSession;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class BackupController {

    private final BackupService backupService;

    @Autowired
    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    @ResponseBody
    @GetMapping(value = "${route.backup[json-download]}")
    public void downloadBackupJson(@RequestParam(required = false) String backupId,
                                   HttpServletResponse response,
                                   @SessionAttribute UserKeyspace userKeyspace) {
        //UserKeyspace userKeyspace = session.getAttribute("userKeyspace");
        if(userKeyspace != null) {
            Backup backup = backupService.getBackup(backupId);
            if(backup.getKeyspaceName().equals(userKeyspace.getKeyspace().getName())) {
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
