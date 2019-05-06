package com.licence.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "route")
public class RouteProperties {
    private String index;
    private String confirmation;
    private String recovery;
    private String register;
    private String login;
    private String logout;
    private String profile;
    private String changeEmail;
    private String changePassword;
    private String changeAvatar;
    private String myDatabase;
    private String createKeyspace;
    private String editKeyspace;
    private String connectKeyspace;
    private String disconnectKeyspace;
    private String changeDatabasePanel;
    private String addUserToKeyspace;
    private String removeUserFromKeyspace;
    private Map<String, String> searchLive;
    private String deleteKeyspace;
    private String changeKeyspacePassword;
    private String changeDatabaseViewEditPanel;
    private String getDatabaseContent;
    private String updateDeleteRowContent;
    private String changeMyKeyspacesPanel;
    private String insertRowContent;
    private String alterTableStructure;
    private String dropTable;
    private String createTableStructure;
    private String createIndex;
    private String dropIndex;
    private Map<String, String> create;
    private Map<String, String> drop;
}
