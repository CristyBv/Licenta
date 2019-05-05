package com.licence.web.controllers;

import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.LocalDate;
import com.licence.config.properties.KeyspaceProperties;
import com.licence.config.properties.RouteProperties;
import com.licence.config.security.CassandraUserDetails;
import com.licence.config.validation.password.match.PasswordMatches;
import com.licence.web.models.Keyspace;
import com.licence.web.models.UDT.KeyspaceUser;
import com.licence.web.models.UDT.UserKeyspace;
import com.licence.web.models.User;
import com.licence.web.models.pojo.KeyspaceContent;
import com.licence.web.models.pojo.KeyspaceContentObject;
import com.licence.web.services.KeyspaceService;
import com.licence.web.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.parameters.P;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class MyDatabaseController {

    private final RouteProperties routeProperties;
    private final KeyspaceService keyspaceService;
    private final UserService userService;
    private final MessageSource messages;
    private final KeyspaceProperties keyspaceProperties;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    public MyDatabaseController(RouteProperties routeProperties, KeyspaceService keyspaceService, UserService userService, @Qualifier("messageSource") MessageSource messages, KeyspaceProperties keyspaceProperties, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.routeProperties = routeProperties;
        this.keyspaceService = keyspaceService;
        this.userService = userService;
        this.messages = messages;
        this.keyspaceProperties = keyspaceProperties;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @RequestMapping(value = "${route.myDatabase}")
    public String myDatabase(Model model,
                             Authentication authentication,
                             HttpSession session) {
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);

        if (session.getAttribute("userKeyspace") != null) {
            session.setAttribute("userKeyspace", userKeyspace);
            if (userKeyspace == null) {
                model.addAttribute("keyspaceNotAvailable", "Keyspace not available anymore!");
            } else {
                String activePanel = (String) session.getAttribute("activePanel");
                KeyspaceContent keyspaceContent = keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase());
                if (Objects.equals(activePanel, keyspaceProperties.getPanel().get("manage")) || Objects.equals(activePanel, keyspaceProperties.getPanel().get("viewEdit"))) {
                    model.addAttribute("keyspaceContent", keyspaceContent);
                }
                if (Objects.equals(activePanel, keyspaceProperties.getPanel().get("viewEdit"))) {
                    session.setAttribute("systemSchemaContent", keyspaceService.getKeyspaceContent("system_schema"));
                    if (session.getAttribute("dataContent") != null) {
                        KeyspaceContentObject keyspaceContentObject = (KeyspaceContentObject) session.getAttribute("dataContent");
                        session.setAttribute("dataContent", getAllContent(userKeyspace.getKeyspace().getName().toLowerCase(), keyspaceContentObject.getTableName()));
                    }
                }
            }
        }
        model.addAttribute("keyspaceObject", new Keyspace());
        model.addAttribute("keyspaces", getUserKeyspaces(user));
        return routeProperties.getMyDatabase();
    }

    @PostMapping(value = "${route.dropType}")
    public String dropType(@RequestParam String type,
                           Authentication authentication,
                           HttpSession session,
                           Model model) {
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            try {
                keyspaceService.dropType(userKeyspace.getKeyspace().getName(), type);
                model.addAttribute("keyspaceViewEditSuccess", "Type deleted!");
                return "forward:" + routeProperties.getMyDatabase();
            } catch (Exception e) {
                model.addAttribute("keyspaceViewEditError", e.getMessage());
                return "forward:" + routeProperties.getMyDatabase();
            }
        }
        model.addAttribute("keyspaceViewEditError", "The type was not deleted! Please refresh and try again!");
        return "forward:" + routeProperties.getMyDatabase();
    }

    @PostMapping(value = "${route.alterType}")
    public String alterType(Authentication authentication,
                            WebRequest request,
                            HttpSession session,
                            Model model) {
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        String typeName = request.getParameter("type_name_readonly");
        String field = request.getParameter("field_names");
        if (userKeyspace != null) {
            if (field == null || field.isEmpty()) {
                model.addAttribute("keyspaceViewEditError", "No options selected for update!");
                return "forward:" + routeProperties.getMyDatabase();
            }
            if (field.substring(0, 1).contains("[") && field.substring(field.length() - 1, field.length()).contains("]")) {
                field = field.substring(1, field.length() - 1);
                String[] fields = field.split(",");
                StringBuilder fieldsList = new StringBuilder();
                for (String s : fields) {
                    if (s.split("@").length == 2) {
                        fieldsList.append(s.split("@")[0]).append(" TO ").append(s.split("@")[1]).append(" AND ");
                    }
                }
                if (fieldsList.length() != 0)
                    fieldsList.delete(fieldsList.length() - 4, fieldsList.length());
                try {
                    keyspaceService.alterTypeRenameFields(userKeyspace.getKeyspace().getName(), typeName, fieldsList.toString());
                    model.addAttribute("keyspaceViewEditSuccess", "Field(s) renamed!");
                    return "forward:" + routeProperties.getMyDatabase();
                } catch (Exception e) {
                    model.addAttribute("keyspaceViewEditError", e.getMessage());
                    return "forward:" + routeProperties.getMyDatabase();
                }
            } else if (field.split("@").length == 2) {
                try {
                    keyspaceService.alterTypeAddField(userKeyspace.getKeyspace().getName(), typeName, field.split("@")[0], field.split("@")[1]);
                    model.addAttribute("keyspaceViewEditSuccess", "Field added!");
                    return "forward:" + routeProperties.getMyDatabase();
                } catch (Exception e) {
                    model.addAttribute("keyspaceViewEditError", e.getMessage());
                    return "forward:" + routeProperties.getMyDatabase();
                }
            }
        }
        model.addAttribute("keyspaceViewEditError", "The type was not updated! Please refresh and try again!");
        return "forward:" + routeProperties.getMyDatabase();
    }

//    @PostMapping(value = "${route.createType}")
//    public String createType() {
//
//    }

    @PostMapping(value = "${route.createIndex}")
    public String createIndex(@RequestParam String tableName,
                              @RequestParam String indexName,
                              @RequestParam String columnName,
                              @RequestParam(required = false) String custom,
                              @RequestParam String options,
                              Authentication authentication,
                              HttpSession session,
                              Model model) {
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            try {
                if (custom == null) {
                    keyspaceService.createIndex(userKeyspace.getKeyspace().getName(), indexName, tableName, columnName);
                } else {
                    keyspaceService.createCustomIndex(userKeyspace.getKeyspace().getName(), indexName, tableName, columnName, options);
                }
                model.addAttribute("keyspaceViewEditSuccess", "Index created!");
                return "forward:" + routeProperties.getMyDatabase();
            } catch (Exception e) {
                model.addAttribute("keyspaceViewEditError", e.getMessage());
                return "forward:" + routeProperties.getMyDatabase();
            }
        }
        model.addAttribute("keyspaceViewEditError", "The index was not created! Please refresh and try again!");
        return "forward:" + routeProperties.getMyDatabase();
    }

    @PostMapping(value = "${route.dropIndex}")
    public String dropIndex(@RequestParam String tableName,
                            @RequestParam String indexName,
                            Authentication authentication,
                            HttpSession session,
                            Model model) {
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            try {
                keyspaceService.dropIndex(userKeyspace.getKeyspace().getName(), indexName);
                model.addAttribute("keyspaceViewEditSuccess", "Index deleted!");
                return "forward:" + routeProperties.getMyDatabase();
            } catch (Exception e) {
                model.addAttribute("keyspaceViewEditError", e.getMessage());
                return "forward:" + routeProperties.getMyDatabase();
            }
        }
        model.addAttribute("keyspaceViewEditError", "The index was not deleted! Please refresh and try again!");
        return "forward:" + routeProperties.getMyDatabase();
    }

    @PostMapping(value = "${route.createTableStructure}")
    public String createTable(@RequestParam String tableName,
                              @RequestParam String columnsDefinitions,
                              @RequestParam String keys,
                              @RequestParam String clusteringOrder,
                              Authentication authentication,
                              HttpSession session,
                              Model model) {
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            try {
                if (clusteringOrder.isEmpty())
                    keyspaceService.createTable(userKeyspace.getKeyspace().getName(), tableName, columnsDefinitions, keys, null);
                else
                    keyspaceService.createTable(userKeyspace.getKeyspace().getName(), tableName, columnsDefinitions, keys, clusteringOrder);
                model.addAttribute("keyspaceViewEditSuccess", "Table created!");
                return "forward:" + routeProperties.getMyDatabase();
            } catch (Exception e) {
                model.addAttribute("keyspaceViewEditError", e.getMessage());
                return "forward:" + routeProperties.getMyDatabase();
            }
        }
        model.addAttribute("keyspaceViewEditError", "The table was not created! Please refresh and try again!");
        return "forward:" + routeProperties.getMyDatabase();
    }

    @PostMapping(value = "${route.createView}")
    public String createView(@RequestParam String viewName,
                             @RequestParam String baseTableName,
                             @RequestParam String columnsSelected,
                             @RequestParam String whereClause,
                             @RequestParam String keys,
                             @RequestParam String clusteringOrder,
                             Authentication authentication,
                             HttpSession session,
                             Model model) {
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            try {
                if (clusteringOrder.isEmpty())
                    keyspaceService.createView(userKeyspace.getKeyspace().getName(), baseTableName, viewName, columnsSelected, whereClause, keys, null);
                else
                    keyspaceService.createView(userKeyspace.getKeyspace().getName(), baseTableName, viewName, columnsSelected, whereClause, keys, clusteringOrder);
                model.addAttribute("keyspaceViewEditSuccess", "View created!");
                return "forward:" + routeProperties.getMyDatabase();
            } catch (Exception e) {
                model.addAttribute("keyspaceViewEditError", e.getMessage());
                return "forward:" + routeProperties.getMyDatabase();
            }
        }
        model.addAttribute("keyspaceViewEditError", "The View was not created! Please refresh and try again!");
        return "forward:" + routeProperties.getMyDatabase();
    }

    @PostMapping(value = "${route.dropView}")
    public String dropView(@RequestParam String view,
                           Authentication authentication,
                           HttpSession session,
                           Model model) {
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            try {
                keyspaceService.dropView(userKeyspace.getKeyspace().getName(), view);
                model.addAttribute("keyspaceViewEditSuccess", "View deleted!");
                return "forward:" + routeProperties.getMyDatabase();
            } catch (Exception e) {
                model.addAttribute("keyspaceViewEditError", e.getMessage());
                return "forward:" + routeProperties.getMyDatabase();
            }
        }
        model.addAttribute("keyspaceViewEditError", "The view was not deleted! Please refresh and try again!");
        return "forward:" + routeProperties.getMyDatabase();
    }

    @PostMapping(value = "${route.dropTable}")
    public String dropTable(@RequestParam String table,
                            Authentication authentication,
                            HttpSession session,
                            Model model) {
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            try {
                keyspaceService.dropTable(userKeyspace.getKeyspace().getName(), table);
                model.addAttribute("keyspaceViewEditSuccess", "Table deleted!");
                return "forward:" + routeProperties.getMyDatabase();
            } catch (Exception e) {
                model.addAttribute("keyspaceViewEditError", e.getMessage());
                return "forward:" + routeProperties.getMyDatabase();
            }
        }
        model.addAttribute("keyspaceViewEditError", "The table was not deleted! Please refresh and try again!");
        return "forward:" + routeProperties.getMyDatabase();
    }

    @PostMapping(value = "${route.alterTableStructure}")
    public String alterTableStructure(@RequestParam String requestType,
                                      Authentication authentication,
                                      WebRequest request,
                                      HttpSession session,
                                      Model model) {
        //request.getParameterMap().forEach((k, v) -> System.out.println(k + " --- " + Arrays.toString(v)));
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            KeyspaceContent keyspaceContent = keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase());
            if (requestType.contains("options") || (requestType.contains("views") && requestType.contains("options"))) {
                Map<String, Object> map;
                if (requestType.contains("views"))
                    map = keyspaceContent.getViews().getContent().stream().filter(p -> p.get("view_name").toString().equals(request.getParameter("view_name_readonly"))).findAny().orElse(null);
                else
                    map = keyspaceContent.getTables().getContent().stream().filter(p -> p.get("table_name").toString().equals(request.getParameter("table_name_readonly"))).findAny().orElse(null);
                if (map != null) {
                    StringBuilder with = new StringBuilder();
                    final Boolean[] somethingToUpdate = {false};
                    map.forEach((k, v) -> {
                        String param = request.getParameter(k);
                        if (param != null && !param.isEmpty()) {
                            with.append(k).append("=").append(param).append(" AND ");
                            somethingToUpdate[0] = true;
                        }
                    });
                    if (with.length() != 0)
                        with.delete(with.length() - 4, with.length());
                    if (with.length() == 0 && !somethingToUpdate[0]) {
                        model.addAttribute("keyspaceViewEditError", "No options selected for update!");
                        return "forward:" + routeProperties.getMyDatabase();
                    }
                    try {
                        if (requestType.contains("views")) {
                            keyspaceService.alterViewOptions(userKeyspace.getKeyspace().getName(), request.getParameter("view_name_readonly"), with.toString());
                            model.addAttribute("keyspaceViewEditSuccess", "View options updated!");
                        } else {
                            keyspaceService.alterOptions(userKeyspace.getKeyspace().getName(), request.getParameter("table_name_readonly"), with.toString());
                            model.addAttribute("keyspaceViewEditSuccess", "Table options updated!");
                        }
                        model.addAttribute("keyspaceViewEditSuccess", "Table options updated!");
                        return "forward:" + routeProperties.getMyDatabase();
                    } catch (Exception e) {
                        model.addAttribute("keyspaceViewEditError", e.getMessage());
                        return "forward:" + routeProperties.getMyDatabase();
                    }
                }
            } else if (requestType.contains("columns")) {
                String columnsRequestType = requestType.split("@").length > 1 ? requestType.split("@")[1] : null;
                String tableName = request.getParameter("table_name_readonly");
                String columnName = request.getParameter("column_name_readonly");
                if (Objects.equals(columnsRequestType, "update")) {
                    Map<String, Object> map = keyspaceContent.getColumns().getContent().stream().filter(p -> p.get("table_name").toString().equals(tableName) && p.get("column_name").toString().equals(columnName)).findAny().orElse(null);
                    if (map != null) {
                        String columnNameToUpdate = request.getParameter("column_name");
                        String typeToUpdate = request.getParameter("type");
                        if ((columnNameToUpdate != null && columnNameToUpdate.isEmpty()) && (typeToUpdate != null && typeToUpdate.isEmpty())) {
                            model.addAttribute("keyspaceViewEditError", "No fields selected for update!");
                            return "forward:" + routeProperties.getMyDatabase();
                        } else {
                            StringBuilder messageSuccess = new StringBuilder();
                            StringBuilder messageError = new StringBuilder();
                            if (columnNameToUpdate != null && !columnNameToUpdate.isEmpty()) {
                                try {
                                    keyspaceService.alterColumnName(userKeyspace.getKeyspace().getName(), tableName, columnName, columnNameToUpdate);
                                    messageSuccess.append("Column renamed!");
                                    model.addAttribute("keyspaceViewEditSuccess", messageSuccess.toString());
                                } catch (Exception e) {
                                    messageError.append(e.getMessage());
                                    model.addAttribute("keyspaceViewEditError", messageError.toString());
                                }
                            }
                            if (typeToUpdate != null && !typeToUpdate.isEmpty()) {
                                try {
                                    keyspaceService.alterColumnType(userKeyspace.getKeyspace().getName(), tableName, columnName, typeToUpdate);
                                    messageSuccess.append("Column type changed!");
                                    model.addAttribute("keyspaceViewEditSuccess", messageSuccess.toString());
                                } catch (Exception e) {
                                    messageError.append(e.getMessage());
                                    model.addAttribute("keyspaceViewEditError", messageError.toString());
                                }
                            }
                            return "forward:" + routeProperties.getMyDatabase();
                        }
                    }
                } else if (Objects.equals(columnsRequestType, "delete")) {
                    try {
                        keyspaceService.alterDropColumn(userKeyspace.getKeyspace().getName(), tableName, columnName);
                        model.addAttribute("keyspaceViewEditSuccess", "Column deleted!");
                        return "forward:" + routeProperties.getMyDatabase();
                    } catch (Exception e) {
                        model.addAttribute("keyspaceViewEditError", e.getMessage());
                        return "forward:" + routeProperties.getMyDatabase();
                    }
                } else if (Objects.equals(columnsRequestType, "add")) {
                    try {
                        keyspaceService.alterAddColumn(userKeyspace.getKeyspace().getName(), request.getParameter("tableName"), request.getParameter("columnsDefinitions"));
                        model.addAttribute("keyspaceViewEditSuccess", "Column(s) added!");
                        return "forward:" + routeProperties.getMyDatabase();
                    } catch (Exception e) {
                        model.addAttribute("keyspaceViewEditError", e.getMessage());
                        return "forward:" + routeProperties.getMyDatabase();
                    }
                }
            }
        }
        model.addAttribute("keyspaceViewEditError", "The table was not updated! Please refresh and try again!");
        return "forward:" + routeProperties.getMyDatabase();
    }

    @ResponseBody
    @PostMapping(value = "/data-structure", produces = "application/json")
    public Map<String, Object> getDataStructure(@RequestBody Map<String, String> data,
                                                Authentication authentication,
                                                WebRequest request,
                                                HttpSession session) {
        String[] dataValues = data.get("data").split("@");
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        Map<String, Object> map = null;
        if (dataValues[0] != null && userKeyspace != null) {
            KeyspaceContent keyspaceContent = keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase());
            switch (dataValues[0]) {
                case "tables":
                    if (dataValues.length > 1) {
                        String tableName = dataValues[1];
                        map = keyspaceContent.getTables().getContent().stream().filter(p -> p.get("table_name").toString().equals(tableName)).findAny().orElse(null);
                    }
                    break;
                case "columns":
                    if (dataValues.length > 2) {
                        String tableName = dataValues[1];
                        String columnName = dataValues[2];
                        map = keyspaceContent.getColumns().getContent().stream().filter(p -> p.get("table_name").toString().equals(tableName) && p.get("column_name").toString().equals(columnName)).findAny().orElse(null);
                    }
                    break;
                case "types":
                    if (dataValues.length > 1) {
                        String typeName = dataValues[1];
                        map = keyspaceContent.getTypes().getContent().stream().filter(p -> p.get("type_name").toString().equals(typeName)).findAny().orElse(null);
                    }
                    break;
                case "views":
                    if (dataValues.length > 1) {
                        String viewName = dataValues[1];
                        map = keyspaceContent.getViews().getContent().stream().filter(p -> p.get("view_name").toString().equals(viewName)).findAny().orElse(null);
                    }
            }
        }
        return map;
    }

    @PostMapping(value = "${route.insertRowContent}")
    public String insertRowContent(@RequestParam String tableName,
                                   Authentication authentication,
                                   WebRequest request,
                                   HttpSession session,
                                   Model model) {
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        KeyspaceContentObject keyspaceContentObject = (KeyspaceContentObject) session.getAttribute("dataContent");
        if (userKeyspace != null && keyspaceContentObject != null && tableName.equals(keyspaceContentObject.getTableName())) {

            StringBuilder insertColumns = new StringBuilder("");
            StringBuilder insertValues = new StringBuilder("");
            if (keyspaceContentObject.getColumnDefinitions() == null) {
                KeyspaceContent keyspaceContent = keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase());
                keyspaceContent.getColumns().getContent().forEach(p -> {
                    if (p.get("table_name").toString().equals(keyspaceContentObject.getTableName())) {
                        String param = request.getParameter(p.get("column_name").toString());
                        if (param != null && !param.isEmpty()) {
                            insertColumns.append(p.get("column_name").toString()).append(",");
                            insertValues.append(param).append(",");
                        }
                    }
                });
            } else {
                keyspaceContentObject.getColumnDefinitions().forEach(d -> {
                    String param = request.getParameter(d.getName());
                    if (param != null && !param.isEmpty()) {
                        insertColumns.append(d.getName()).append(",");
                        insertValues.append(param).append(",");
                    }
                });
            }

            if (insertColumns.length() != 0 && insertValues.length() != 0) {
                insertColumns.deleteCharAt(insertColumns.length() - 1);
                insertValues.deleteCharAt(insertValues.length() - 1);
                try {
                    KeyspaceContent keyspaceContent = keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase());
                    Map<String, Object> map = keyspaceContent.getViews().getContent().stream().filter(p -> p.get("view_name").toString().equals(keyspaceContentObject.getTableName())).findAny().orElse(null);
                    if (map != null) {
                        keyspaceService.insert(userKeyspace.getKeyspace().getName(), map.get("base_table_name").toString(), insertColumns.toString(), insertValues.toString(), "");
                    } else {
                        keyspaceService.insert(userKeyspace.getKeyspace().getName(), keyspaceContentObject.getTableName(), insertColumns.toString(), insertValues.toString(), "");
                    }

                    model.addAttribute("keyspaceViewEditSuccess", "Row inserted!");
                    return "forward:" + routeProperties.getMyDatabase();
                } catch (Exception e) {
                    model.addAttribute("keyspaceViewEditError", e.getMessage());
                    return "forward:" + routeProperties.getMyDatabase();
                }
            }
        }
        model.addAttribute("keyspaceViewEditError", "The row was not inserted! Please refresh and try again!");
        return "forward:" + routeProperties.getMyDatabase();
    }

    @PostMapping(value = "${route.updateDeleteRowContent}")
    public String updateDeleteRowContent(@RequestParam String tableName,
                                         @RequestParam String requestType,
                                         Authentication authentication,
                                         WebRequest request,
                                         HttpSession session,
                                         Model model) {
        //request.getParameterMap().forEach((k, v) -> System.out.println(k + " --- " + Arrays.toString(v)));
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        KeyspaceContentObject keyspaceContentObject = (KeyspaceContentObject) session.getAttribute("dataContent");
        if (userKeyspace != null && keyspaceContentObject != null && tableName.equals(keyspaceContentObject.getTableName())) {
            KeyspaceContent keyspaceContent = keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase());
            Map<String, Object> map = keyspaceContent.getViews().getContent().stream().filter(p -> p.get("view_name").toString().equals(keyspaceContentObject.getTableName())).findAny().orElse(null);
            if(map != null )
                keyspaceContentObject.setTableName(map.get("base_table_name").toString());
            List<Map<String, Object>> primaryKeys = getPrimaryKeysFromTable(keyspaceContent, keyspaceContentObject);
            Map<String, Object> findRow = findRowFromRequest(keyspaceContentObject, primaryKeys, request);
            keyspaceContentObject.setTableName(tableName);
            if (findRow != null) {
                StringBuilder where = new StringBuilder("");
                primaryKeys.forEach(p -> {
                    DataType dataType = keyspaceContentObject.getColumnDefinitions().getType(p.get("column_name").toString());
                    where.append(p.get("column_name")).append("=").append(databaseCorrespondence(findRow.get(p.get("column_name").toString()), dataType)).append(" AND ");
                });
                if (where.length() != 0)
                    where.delete(where.length() - 4, where.length());
                try {
                    if (requestType.equals("update")) {
                        StringBuilder set = new StringBuilder("");
                        keyspaceContentObject.getColumnDefinitions().forEach(d -> {
                            String param = request.getParameter(d.getName());
                            if (param != null && !param.isEmpty()) {
                                DataType dataType = keyspaceContentObject.getColumnDefinitions().getType(d.getName());
                                set.append(d.getName()).append("=").append(param).append(",");
                            }
                        });
                        if (set.length() != 0)
                            set.deleteCharAt(set.length() - 1);
                        if (set.length() == 0 || where.length() == 0) {
                            model.addAttribute("keyspaceViewEditError", "No columns selected for update!");
                            return "forward:" + routeProperties.getMyDatabase();
                        }
                        if(map != null) {
                            keyspaceService.update(userKeyspace.getKeyspace().getName(), map.get("base_table_name").toString(), "", set.toString(), where.toString());
                        } else {
                            keyspaceService.update(userKeyspace.getKeyspace().getName(), keyspaceContentObject.getTableName(), "", set.toString(), where.toString());
                        }
                        model.addAttribute("keyspaceViewEditSuccess", "Row updated!");
                        return "forward:" + routeProperties.getMyDatabase();
                    } else if (requestType.equals("delete")) {
                        final Boolean[] entireRow = {true};
                        StringBuilder delete = new StringBuilder("");
                        keyspaceContentObject.getColumnDefinitions().forEach(d -> {
                            String param = request.getParameter(d.getName());
                            if (param != null && !param.isEmpty() && param.equals("null")) {
                                entireRow[0] = false;
                                delete.append(d.getName()).append(",");
                            }
                        });
                        if (!entireRow[0]) {
                            if (delete.length() != 0)
                                delete.deleteCharAt(delete.length() - 1);
                            if (delete.length() == 0 || where.length() == 0) {
                                model.addAttribute("keyspaceViewEditError", "No columns selected for delete!");
                                return "forward:" + routeProperties.getMyDatabase();
                            }
                            if(map != null) {
                                keyspaceService.delete(delete.toString(), userKeyspace.getKeyspace().getName(), map.get("base_table_name").toString(), "", where.toString());
                            } else {
                                keyspaceService.delete(delete.toString(), userKeyspace.getKeyspace().getName(), keyspaceContentObject.getTableName(), "", where.toString());
                            }

                            model.addAttribute("keyspaceViewEditSuccess", "Columns deleted!");
                        } else {
                            if(map != null) {
                                keyspaceService.delete("", userKeyspace.getKeyspace().getName(), map.get("base_table_name").toString(), "", where.toString());
                            } else {
                                keyspaceService.delete("", userKeyspace.getKeyspace().getName(), keyspaceContentObject.getTableName(), "", where.toString());
                            }
                            model.addAttribute("keyspaceViewEditSuccess", "Row deleted!");
                        }
                        return "forward:" + routeProperties.getMyDatabase();
                    }
                } catch (Exception e) {
                    model.addAttribute("keyspaceViewEditError", e.getMessage());
                    return "forward:" + routeProperties.getMyDatabase();
                }

            }
        }
        model.addAttribute("keyspaceViewEditError", "The row was not updated! Please refresh and try again!");
        return "forward:" + routeProperties.getMyDatabase();
    }

    private List<Map<String, Object>> getPrimaryKeysFromTable(KeyspaceContent
                                                                      keyspaceContent, KeyspaceContentObject keyspaceContentObject) {
        List<Map<String, Object>> primaryKeys = new ArrayList<>();
        keyspaceContent.getColumns().getContent().forEach(p -> {
            if (p.get("table_name").toString().equals(keyspaceContentObject.getTableName()) && !p.get("kind").toString().equals("regular"))
                primaryKeys.add(p);
        });
        return primaryKeys;
    }

    private Map<String, Object> findRowFromRequest(KeyspaceContentObject
                                                           keyspaceContentObject, List<Map<String, Object>> primaryKeys, WebRequest request) {
        Map<String, Object> map;
        for (Map<String, Object> p : keyspaceContentObject.getContent()) {
            Boolean ok = true;
            for (Map.Entry<String, Object> entry : p.entrySet()) {
                DataType dataType = keyspaceContentObject.getColumnDefinitions().getType(entry.getKey());
                final Boolean[] primaryKey = {false};
                primaryKeys.forEach(q -> {
                    if (q.get("column_name").toString().equals(entry.getKey()))
                        primaryKey[0] = true;
                });
                if (primaryKey[0] && !Objects.equals(databaseCorrespondence(entry.getValue(), dataType), databaseCorrespondenceByString(request.getParameter(entry.getKey() + "_readonly"), dataType.getName().toString()))) {
                    ok = false;
                }
            }
            if (ok) {
                map = p;
                return map;
            }
        }
        return null;
    }

    private String databaseCorrespondence(Object object, DataType dataType) {
        String type = dataType.getName().toString();
        StringBuilder stringBuilder = new StringBuilder("");
        if (object == null)
            return null;
        try {
            if (object.getClass().equals(String.class)) {
                stringBuilder.append("\'").append(object.toString()).append("\'");
            } else if (object.getClass().equals(Date.class)) {
                Date date = (Date) object;
                if (type.equals("timestamp")) {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    stringBuilder.append("\'").append(dateFormat.format(date)).append("\'");
                }
            } else if (object.getClass().equals(LocalDate.class)) {
                LocalDate localDate = (LocalDate) object;
                if (type.equals("date")) {
                    stringBuilder.append("\'").append(localDate.toString()).append("\'");
                }
            } else if (object.getClass().equals(Boolean.class)) {
                Boolean bool = (Boolean) object;
                stringBuilder.append(bool);
            } else if (dataType.getName().toString().equals("blob")) {
                ByteBuffer byteBuffer = (ByteBuffer) object;
                byte[] bytes = byteBuffer.array();
                for (byte b : bytes) {
                    stringBuilder.append(String.format("%02x", b));
                }
            } else if (object.getClass().equals(Long.class) && type.equals("time")) {
                Long lg = (Long) object;
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                stringBuilder.append("\'").append(LocalTime.ofNanoOfDay(lg).format(dateTimeFormatter)).append("\'");
            } else if (dataType.isCollection()) {
                Object obj = editNDimentionCollectionObject(object, dataType);
                if (obj != null)
                    stringBuilder.append(obj.toString());
            } else {
                stringBuilder.append(object.toString());
            }
        } catch (ClassCastException e) {
            return null;
        }
        return stringBuilder.toString();
    }

    private Object editNDimentionCollectionObject(Object object, DataType dataType) {
        if (!dataType.isCollection() && !dataType.isFrozen()) {
            object = databaseCorrespondence(object, dataType);
            return object;
        } else if (dataType.isCollection() || dataType.isFrozen()) {
            List<DataType> dataTypes = dataType.getTypeArguments();
            try {
                switch (dataType.getName().toString()) {
                    case "list":
                        List<Object> list = (ArrayList<Object>) object;
                        List<Object> list2 = new ArrayList<>();
                        for (Object o : list) {
                            list2.add(editNDimentionCollectionObject(o, dataType.getTypeArguments().get(0)));
                        }
                        return list2;
                    //break;
                    case "set":
                        Set<Object> set = (LinkedHashSet<Object>) object;
                        Set<Object> set2 = new LinkedHashSet<>();
                        for (Object o : set) {
                            set2.add(editNDimentionCollectionObject(o, dataType.getTypeArguments().get(0)));
                        }
                        return set2;
                    case "map":
                        Map<Object, Object> map = (LinkedHashMap<Object, Object>) object;
                        Map<Object, Object> map2 = new LinkedHashMap<>();
                        for (Map.Entry<Object, Object> entry : map.entrySet()) {
                            map2.put(editNDimentionCollectionObject(entry.getKey(), dataType.getTypeArguments().get(0)), editNDimentionCollectionObject(entry.getValue(), dataType.getTypeArguments().get(1)));
                        }
                        return map2;
                }
            } catch (ClassCastException e) {
                return object;
            }
        }
        return object;
    }

    private String databaseCorrespondenceByString(String object, String type) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (object == null)
            return null;
        if (type.equals("date") || type.equals("inet") || type.equals("text") || type.equals("time") || type.equals("varchar")) {
            stringBuilder.append("\'").append(object).append("\'");
        } else {
            stringBuilder.append(object);
        }
        return stringBuilder.toString();
    }

    private KeyspaceContentObject getAllContent(String keyspaceName, String tableName) {
        return keyspaceService.getSelectSimple(keyspaceName, tableName, "*");
    }

    private Map<String, List<UserKeyspace>> getUserKeyspaces(User user) {
        Map<String, List<UserKeyspace>> map;
        List<UserKeyspace> userKeyspaces = user.getKeyspaces();
        if (userKeyspaces != null) {
            map = userKeyspaces.stream().collect(Collectors.groupingBy(UserKeyspace::getCreatorName));
        } else {
            return new HashMap<>();
        }
        return map;
    }

    private UserKeyspace updateUserKeyspaces(Authentication authentication, HttpSession session) {
        User authUser = ((CassandraUserDetails) authentication.getPrincipal()).getUser();
        User dbUser = userService.findById(authUser.getId());
        UserKeyspace userKeyspace = (UserKeyspace) session.getAttribute("userKeyspace");
        authUser.setKeyspaces(dbUser.getKeyspaces());
        updateKeyspaces(authUser);
        if (userKeyspace != null) {
            if (authUser.getKeyspaces() != null) {
                return authUser.getKeyspaces().stream().filter(p -> Objects.equals(p.getCreatorName(), userKeyspace.getCreatorName()) && Objects.equals(p.getName(), userKeyspace.getName())).findFirst().orElse(null);
            }
        }
        return null;
    }

    private void updateKeyspaces(User authUser) {
        if (authUser.getKeyspaces() != null) {
            authUser.getKeyspaces().forEach(p -> {
                Keyspace keyspace = keyspaceService.findKeyspaceByName(p.getCreatorName() + "_" + p.getName());
                p.setKeyspace(keyspace);
            });
        }
    }

    @GetMapping(value = "${route.getDatabaseContent}")
    public String getDatabaseContent(@RequestParam(name = "table", required = false) String table,
                                     HttpSession session,
                                     Authentication authentication,
                                     WebRequest request) {
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            if (!table.isEmpty()) {
                session.setAttribute("dataContent", getAllContent(userKeyspace.getKeyspace().getName().toLowerCase(),
                        table));
            }
        }
        return "redirect:" + routeProperties.getMyDatabase() + "#keyspace-data-content";
    }


    @ResponseBody
    @GetMapping(value = "/table-data", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getTableData(WebRequest request,
                                            HttpSession session) {
        //request.getParameterMap().forEach((k,v) -> System.out.println(k + " - " + Arrays.toString(v)));
        Map<String, Object> map = new HashMap<>();
        String draw = request.getParameter("draw");
        String start = request.getParameter("start");
        String length = request.getParameter("length");
        String orderColumn = request.getParameter("order[0][column]");
        String order = request.getParameter("order[0][dir]");
        String search = request.getParameter("search[value]");
        UserKeyspace userKeyspace = (UserKeyspace) session.getAttribute("userKeyspace");
        KeyspaceContentObject keyspaceContentObject = (KeyspaceContentObject) session.getAttribute("dataContent");
        if (userKeyspace != null && keyspaceContentObject != null) {
            KeyspaceContentObject update = getAllContent(userKeyspace.getKeyspace().getName().toLowerCase(), keyspaceContentObject.getTableName());
            session.setAttribute("dataContent", update);
            //update.getContent().forEach(p -> p.put("DT_RowId", update.getTableName() + "#" + update.getContent().indexOf(p)));
            if (draw != null)
                map.put("draw", Integer.parseInt(draw));
            map.put("recordsTotal", update.getContent().size());
            if (search != null && !search.isEmpty()) {
                update.setContent(update.getContent().stream().filter(p -> {
                    final Boolean[] ok = {false};
                    p.forEach((key, value) -> {
                        if (value != null && value.toString().toLowerCase().contains(search.toLowerCase()))
                            ok[0] = true;
                    });
                    return ok[0];
                }).collect(Collectors.toList()));
            }
            map.put("recordsFiltered", update.getContent().size());
            if (orderColumn != null && order != null) {
                Integer ordCol = Integer.parseInt(orderColumn);
                update.setContent(update.getContent().stream().sorted((p, q) -> {
                    int count = 0;
                    Map.Entry<String, Object> ent1 = null;
                    for (Map.Entry<String, Object> entry : p.entrySet()) {
                        if (count == ordCol) {
                            ent1 = entry;
                            break;
                        }
                        count++;
                    }
                    count = 0;
                    Map.Entry<String, Object> ent2 = null;
                    for (Map.Entry<String, Object> entry : q.entrySet()) {
                        if (count == ordCol) {
                            ent2 = entry;
                            break;
                        }
                        count++;
                    }
                    if (ent1 != null && ent2 != null) {
                        if (ent1.getValue() == null && ent2.getValue() != null) {
                            if (order.equals("asc"))
                                return 1;
                            else if (order.equals("desc"))
                                return -1;
                        } else if (ent1.getValue() != null && ent2.getValue() == null) {
                            if (order.equals("asc"))
                                return -1;
                            else if (order.equals("desc"))
                                return 1;
                        } else if (ent1.getValue() != null && ent2.getValue() != null) {
                            Map.Entry<String, Object> finalEnt = ent1;
                            ColumnDefinitions.Definition definition = update.getColumnDefinitions().asList().stream().filter(w -> w.getName().equals(finalEnt.getKey())).findAny().orElse(null);
                            if (definition != null) {
                                if (definition.getType().getName().toString().equals("timestamp")) {
                                    Date timestamp1 = (Date) ent1.getValue();
                                    Date timestamp2 = (Date) ent2.getValue();
                                    if (order.equals("asc"))
                                        return timestamp1.compareTo(timestamp2);
                                    else if (order.equals("desc"))
                                        return timestamp2.compareTo(timestamp1);
                                }
                                if (order.equals("asc"))
                                    return ent1.getValue().toString().compareTo(ent2.getValue().toString());
                                else if (order.equals("desc"))
                                    return ent2.getValue().toString().compareTo(ent1.getValue().toString());
                            }
                        }
                    }
                    return 0;
                }).collect(Collectors.toList()));
            }
            if (start != null && length != null) {
                Integer startVal = Integer.parseInt(start);
                Integer lengthVal = Integer.parseInt(length);
                if (lengthVal != -1) {
                    List<Map<String, Object>> updateNew = new ArrayList<>();
                    if (update.getContent().size() > startVal) {
                        for (int i = startVal; i < startVal + lengthVal; i++) {
                            if (i >= update.getContent().size())
                                break;
                            updateNew.add(update.getContent().get(i));
                        }
                    }
                    update.setContent(updateNew);
                }
            }
            List<Map<String, String>> updateString = new ArrayList<>();
            update.getContent().forEach(p -> {
                Map<String, String> map1 = new HashMap<>();
                p.forEach((k, v) -> {
                    if (v != null) {
                        DataType type = update.getColumnDefinitions().getType(k);
                        if (type.getName().toString().equals("blob") || type.getName().toString().equals("time") || type.isCollection() || type.getName().toString().equals("timestamp")) {
                            String obj = databaseCorrespondence(v, type);
                            if (obj != null && obj.indexOf('\'') == 0 && obj.lastIndexOf('\'') == obj.length() - 1)
                                obj = obj.substring(1, obj.length() - 1);
//                            if(type.isCollection()) {
//                                try {
//                                    map1.put(k, new JSONObject(obj).toString());
//                                } catch (JSONException e) {
//                                    map1.put(k, obj);
//                                }
//                            } else
                            map1.put(k, obj);
                        } else
                            map1.put(k, v.toString());
                    } else
                        map1.put(k, null);
                });
                updateString.add(map1);
            });
            map.put("data", updateString);
        }
        return map;
    }

    @ResponseBody
    @PostMapping(value = "${route.changeMyKeyspacesPanel}")
    public String changeMyKeyspacesPanel(@RequestBody Map<String, String> position,
                                         HttpSession session) {
        if (position.get("position").equals("open")) {
            session.setAttribute("myKeyspacesPanelPosition", "open");
        } else if (position.get("position").equals("close")) {
            session.setAttribute("myKeyspacesPanelPosition", "close");
        }
        return JSONObject.quote("success");
    }


    @PostMapping(value = "${route.editKeyspace}")
    public String editKeyspace(Keyspace keyspace,
                               Authentication authentication,
                               HttpSession session,
                               WebRequest request,
                               Model model) {
        if (!testKeyspaceRole(session, keyspaceProperties.getCreator())) {
            model.addAttribute("keyspaceManageError", "Access denied!");
            return "forward:" + routeProperties.getMyDatabase();
        }
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            if (userKeyspace.getKeyspace().isDurableWrites() == keyspace.isDurableWrites() && Objects.equals(userKeyspace.getKeyspace().getReplicationFactor(), keyspace.getReplicationFactor())) {
                model.addAttribute("keyspaceManageError",
                        messages.getMessage("database.keyspaces.edit.no-change", null,
                                request.getLocale()));
            } else {
                userKeyspace.getKeyspace().setDurableWrites(keyspace.isDurableWrites());
                userKeyspace.getKeyspace().setReplicationFactor(keyspace.getReplicationFactor());
                keyspaceService.save(userKeyspace.getKeyspace(), false, true);
                model.addAttribute("keyspaceManageSuccess",
                        messages.getMessage("database.keyspaces.edit.success", null,
                                request.getLocale()));
            }

        }
        return "forward:" + routeProperties.getMyDatabase();
    }

    @PostMapping(value = "${route.removeUserFromKeyspace}")
    public String removeUserFromKeyspace(@RequestParam(name = "userName", required = false) String userName,
                                         Authentication authentication,
                                         HttpSession session,
                                         WebRequest request,
                                         Model model) {
        if (!testKeyspaceRole(session, keyspaceProperties.getCreator()) && !testKeyspaceRole(session, keyspaceProperties.getAdmin())) {
            model.addAttribute("keyspaceManageError", "Access denied!");
            return "forward:" + routeProperties.getMyDatabase();
        }
        if (userName != null) {
            UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
            User removeUser = userService.findUserByUsername(userName);
            if (userKeyspace != null && removeUser != null && removeUser.getKeyspaces() != null) {
                // we verify that the keyspaceUser and the userKeyspace that we want to remove from keyspace table and
                // users table, exists and we save them
                KeyspaceUser keyspaceUserToRemove = userKeyspace.getKeyspace().getUsers().stream().filter(p -> Objects.equals(p.getUserName(), userName)).findFirst().orElse(null);
                UserKeyspace userKeyspaceToRemove = removeUser.getKeyspaces().stream().filter(p -> Objects.equals(p.getCreatorName(), userKeyspace.getCreatorName()) && Objects.equals(p.getName(), userKeyspace.getName())).findFirst().orElse(null);
                // if exists then we remove them and save the changes into database and session
                if (keyspaceUserToRemove != null)
                    userKeyspace.getKeyspace().getUsers().remove(keyspaceUserToRemove);
                if (userKeyspaceToRemove != null)
                    removeUser.getKeyspaces().remove(userKeyspaceToRemove);
                keyspaceService.save(userKeyspace.getKeyspace(), false, false);
                userService.save(removeUser);
                model.addAttribute("keyspaceManageSuccess",
                        messages.getMessage("database.keyspaces.remove-user.success", null,
                                request.getLocale()));
            }
        } else {
            model.addAttribute("keyspaceManageError", "Please complete all fields!");
        }
        return "forward:" + routeProperties.getMyDatabase();
    }

    @PostMapping(value = "${route.addUserToKeyspace}")
    public String addUserToKeyspace(@RequestParam(name = "userName", required = false) String userName,
                                    @RequestParam(name = "access", required = false) String access,
                                    Authentication authentication,
                                    HttpSession session,
                                    WebRequest request,
                                    Model model) {
        if (!testKeyspaceRole(session, keyspaceProperties.getCreator()) && !testKeyspaceRole(session, keyspaceProperties.getAdmin())) {
            model.addAttribute("keyspaceManageError", "Access denied!");
            return "forward:" + routeProperties.getMyDatabase();
        }
        if (userName != null && access != null) {
            UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
            User addUser = userService.findUserByUsername(userName);
            if (userKeyspace != null && addUser != null) {
                // if the user exists, we create the userKeyspace that we want to add
                UserKeyspace newUserKeyspace = UserKeyspace.builder()
                        .creatorName(userKeyspace.getCreatorName())
                        .name(userKeyspace.getName())
                        .access(access)
                        .build();
                // if it is not already added, we add it into the users table
                if (addUser.getKeyspaces() != null && !addUser.getKeyspaces().contains(newUserKeyspace)) {
                    addUser.getKeyspaces().add(newUserKeyspace);
                    // if the userKeyspaces list is null, we create it
                } else if (addUser.getKeyspaces() == null) {
                    addUser.setKeyspaces(Collections.singletonList(newUserKeyspace));
                }
                // we add the keyspaceUser information into the keyspace table too
                KeyspaceUser newKeyspaceUser = new KeyspaceUser(addUser.getUserName(), access);
                if (!userKeyspace.getKeyspace().getUsers().contains(newKeyspaceUser))
                    userKeyspace.getKeyspace().getUsers().add(newKeyspaceUser);
                keyspaceService.save(userKeyspace.getKeyspace(), false, false);
                userService.save(addUser);
                model.addAttribute("keyspaceManageSuccess",
                        messages.getMessage("database.keyspaces.add-user.success", null,
                                request.getLocale()));
            }
        } else {
            model.addAttribute("keyspaceManageError", "Please complete all fields!");
        }
        return "forward:" + routeProperties.getMyDatabase();
    }

    @PostMapping(value = "${route.deleteKeyspace}")
    public String deleteKeyspace(@RequestParam("confirmDelete") boolean confirm,
                                 @RequestParam(name = "password", required = false) String password,
                                 Authentication authentication,
                                 HttpSession session,
                                 Model model,
                                 WebRequest request) {
        if (!testKeyspaceRole(session, keyspaceProperties.getCreator())) {
            model.addAttribute("keyspaceManageError", "Access denied!");
            return "forward:" + routeProperties.getMyDatabase();
        }
        // if the checkbox for confirmation is true
        if (confirm) {
            // we take the corespondent userKeyspace form session to auth context
            CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();
            UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
            if (userKeyspace != null) {
                // if the keyspace is admin type, we can not delete it
                if (keyspaceService.getAdminKeyspaceName().equals(userKeyspace.getKeyspace().getName().toLowerCase())) {
                    model.addAttribute("keyspaceManageError", "You cannot delete the admin keyspace!");
                    return "forward:" + routeProperties.getMyDatabase();
                }
                // if the keyspace is password enabled and the password field is not empty and not matches to the keyspace password
                if (userKeyspace.getKeyspace().isPasswordEnabled() && password != null) {
                    if (!bCryptPasswordEncoder.matches(password, userKeyspace.getKeyspace().getPassword())) {
                        model.addAttribute("keyspaceManageError", "Invalid password!");
                        return "forward:" + routeProperties.getMyDatabase();
                    }
                }
                // if the password is not enabled or the passwords matched
                // for each user from users list from the keyspace, we delete his userKeyspace for the one we will delete
                userKeyspace.getKeyspace().getUsers().forEach(p -> {
                    // if the user is not the logged one
                    if (!Objects.equals(p.getUserName(), user.getUserName())) {
                        User userFromKeyspace = userService.findUserByUsername(p.getUserName());
                        if (userFromKeyspace != null) {
                            // we search into his userKeyspace list for the one we want to delete
                            Optional<UserKeyspace> userKeyspaceOpt2 = userFromKeyspace.getKeyspaces().stream().filter(q -> Objects.equals(q.getCreatorName(), userKeyspace.getCreatorName()) && Objects.equals(q.getName(), userKeyspace.getName())).findFirst();
                            if (userKeyspaceOpt2.isPresent()) {
                                // if it exists, we remove it and update the database
                                if (userFromKeyspace.getKeyspaces() != null) {
                                    userFromKeyspace.getKeyspaces().remove(userKeyspaceOpt2.get());
                                    userService.save(userFromKeyspace);
                                }
                            }
                        }
                    }
                });
                // after we delete it from the keyspaces table and from cassandra too
                keyspaceService.deleteKeyspace(userKeyspace.getKeyspace(), true);
                // update the logged user too
                user.getKeyspaces().remove(userKeyspace);
                userService.save(user);
                model.addAttribute("deleteKeyspaceSuccess",
                        messages.getMessage("database.keyspaces.delete.success", null,
                                request.getLocale()));
            }
        }
        return "forward:" + routeProperties.getMyDatabase();
    }

    private boolean testKeyspaceRole(HttpSession session, String role) {
        if (session.getAttribute("userKeyspace") != null) {
            UserKeyspace activeUserKeyspace = (UserKeyspace) session.getAttribute("userKeyspace");
            if (Objects.equals(activeUserKeyspace.getAccess(), role))
                return true;
        }
        return false;
    }

    @PostMapping(value = "${route.changeKeyspacePassword}")
    public String changeKeyspacePassword(@Valid Keyspace keyspace,
                                         BindingResult bindingResult,
                                         HttpSession session,
                                         Model model,
                                         WebRequest request,
                                         Authentication authentication) {

        if (!testKeyspaceRole(session, keyspaceProperties.getCreator())) {
            model.addAttribute("keyspaceManageError", "Access denied!");
            return "forward:" + routeProperties.getMyDatabase();
        }
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null && userKeyspace.getKeyspace() != null) {
            // if keyspace is password enabled
            if (userKeyspace.getKeyspace().isPasswordEnabled()) {
                // if the old password matches with the current active keyspace
                if (bCryptPasswordEncoder.matches(keyspace.getPassword(), userKeyspace.getKeyspace().getPassword())) {
                    // update the database, the context and the session too
                    userKeyspace.getKeyspace().setPasswordEnabled(false);
                    keyspaceService.save(userKeyspace.getKeyspace(), false, false);
                    model.addAttribute("keyspaceManageSuccess",
                            messages.getMessage("database.keyspaces.password.disable", null,
                                    request.getLocale()));
                } else {
                    model.addAttribute("keyspaceManageError",
                            messages.getMessage("database.keyspaces.password.wrong-old-password", null,
                                    request.getLocale()));
                }
            } else {
                // if the keyspace has no password, we validate it
                Map<String, ArrayList<String>> errors = getErrorsMap();
                errors = getErrors(bindingResult, errors);
                if (!errors.get("passwordErrors").isEmpty()) {
                    String message = errors.get("passwordErrors").toString();
                    if (!errors.get("matchingPasswordErrors").isEmpty()) {
                        message = message + '\n' + errors.get("matchingPasswordErrors").toString();
                    }
                    model.addAttribute("keyspaceManageError", message);
                } else {
                    // the password is enabled and saved in database
                    // update the database, the context and the session too
                    userKeyspace.getKeyspace().setPasswordEnabled(true);
                    userKeyspace.getKeyspace().setPassword(keyspace.getPassword());
                    keyspaceService.save(userKeyspace.getKeyspace(), false, false);
                    model.addAttribute("keyspaceManageSuccess",
                            messages.getMessage("database.keyspaces.password.change", null,
                                    request.getLocale()));
                }
            }
        }

        return "forward:" + routeProperties.getMyDatabase();
    }


    @PostMapping(value = "${route.createKeyspace}")
    public String createKeyspace(@ModelAttribute @Valid Keyspace keyspace,
                                 BindingResult bindingResult,
                                 Authentication authentication,
                                 WebRequest request,
                                 Model model) {
        // initialize errors map
        Map<String, ArrayList<String>> errors = getErrorsMap();
        // if the keyspace has errors, we sort them into a map
        if (bindingResult.hasErrors()) {
            errors = getErrors(bindingResult, errors);
            errors.entrySet().stream().map(Map.Entry::getValue).filter(x -> !x.isEmpty()).forEach(System.out::println);
        } else {
            // if there are no errors
            CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();
            // if the keyspace already exists
            if (keyspaceService.findKeyspaceByName(user.getUserName() + "_" + keyspace.getName()) != null) {
                errors.get("nameErrors").add("Name: already exists!");
            } else {
                // else, we set the full name and put the current user as creator
                String name = keyspace.getName();
                keyspace.setName(user.getUserName() + "_" + name);
                keyspace.setUsers(Collections.singletonList(new KeyspaceUser(user.getUserName(), keyspaceProperties.getCreator())));
                // save it in the database
                keyspaceService.save(keyspace, true, false);
                // save add it in the users database as user keyspace
                UserKeyspace userKeyspace = UserKeyspace.builder()
                        .keyspace(keyspace)
                        .name(name)
                        .access(keyspaceProperties.getCreator())
                        .creatorName(user.getUserName())
                        .build();
                user.getKeyspaces().add(userKeyspace);
                userService.save(user);
                String messageValue = messages.getMessage("database.keyspaces.create.success", null, request.getLocale());
                model.addAttribute("createKeyspaceSuccess", messageValue);
                System.out.println(keyspace.getName() + " - keyspace created!");
                return "forward:" + routeProperties.getMyDatabase();
            }
        }
        model.addAttribute("createKeyspaceError", true);
        model.addAttribute("validateErrors", errors);
        model.addAttribute("keyspaceObject", keyspace);
        return "forward:" + routeProperties.getMyDatabase();
    }

    @PostMapping(value = "${route.connectKeyspace}")
    public String connectKeyspace(Keyspace keyspace,
                                  @RequestParam String creatorName,
                                  Authentication authentication,
                                  Model model,
                                  WebRequest request,
                                  HttpSession session) {
        String message;
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        // we search for the keyspace in the auth context
        Optional<UserKeyspace> userKeyspaceOpt = user.getKeyspaces().stream().filter(p -> Objects.equals(p.getCreatorName(), creatorName) && Objects.equals(p.getName(), keyspace.getName())).findFirst();
        // if we find it
        if (userKeyspaceOpt.isPresent()) {
            UserKeyspace userKeyspace = userKeyspaceOpt.get();
            // if it is password protected and both passwords match
            if (!keyspace.isPasswordEnabled() || bCryptPasswordEncoder.matches(keyspace.getPassword(), userKeyspace.getKeyspace().getPassword())) {
                // we save the user keyspace in the session
                session.setAttribute("userKeyspace", userKeyspace);
                session.setAttribute("activePanel", keyspaceProperties.getPanel().get("manage"));
                session.setAttribute("dataContent", null);
                session.setAttribute("myKeyspacesPanelPosition", "close");
                return "redirect:" + routeProperties.getMyDatabase();
            } else {
                message = "Access denied! Incorect password!";
            }
        } else {
            message = messages.getMessage("database.keyspaces.connect.notFound", null, request.getLocale());
        }
        model.addAttribute("connectKeyspaceError", message);
        return "forward:" + routeProperties.getMyDatabase();
    }

    @GetMapping(value = "${route.disconnectKeyspace}")
    public String disconnectKeyspace(HttpSession session) {
        session.setAttribute("userKeyspace", null);
        session.setAttribute("dataContent", null);
        session.setAttribute("activePanel", null);
        return "redirect:" + routeProperties.getMyDatabase();
    }

    @GetMapping(value = "${route.changeDatabasePanel}")
    public String changeDatabasePanel(@RequestParam String panel,
                                      HttpSession session) {
        session.setAttribute("activePanel", panel);
        if (panel.equals(keyspaceProperties.getPanel().get("viewEdit"))) {
            session.setAttribute("activeViewEditPanel", keyspaceProperties.getPanel().get("tables"));
        }
        return "redirect:" + routeProperties.getMyDatabase();
    }

    @GetMapping(value = "${route.changeDatabaseViewEditPanel}")
    public String changeDatabaseViewEditPanel(@RequestParam String panel,
                                              HttpSession session) {
        session.setAttribute("activeViewEditPanel", panel);
        return "redirect:" + routeProperties.getMyDatabase();
    }

    private Map<String, ArrayList<String>> getErrors(BindingResult bindingResult, Map<String, ArrayList<String>> errors) {
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        for (FieldError fieldError : fieldErrors) {
            if (Objects.equals(fieldError.getField(), "name"))
                if (Objects.equals(fieldError.getCode(), "Pattern"))
                    errors.get("nameErrors").add("Name: incorrect pattern. Use a valid name");
                else errors.get("nameErrors").add("Name: " + fieldError.getDefaultMessage());
            else if (Objects.equals(fieldError.getField(), "password"))
                errors.get("passwordErrors").add("Password: incorrect pattern");
            else if (Objects.equals(fieldError.getField(), "matchingPassword"))
                errors.get("matchingPasswordErrors").add("Password: " + fieldError.getDefaultMessage());
        }
        List<ObjectError> list = bindingResult.getAllErrors();
        for (ObjectError obj : list) {
            if (Objects.equals(obj.getCode(), PasswordMatches.class.getSimpleName())) {
                errors.get("matchingPasswordErrors").add("Password: " + obj.getDefaultMessage());
            }
        }
        return errors;
    }

    private Map<String, ArrayList<String>> getErrorsMap() {
        Map<String, ArrayList<String>> errors = new HashMap<>();
        errors.put("nameErrors", new ArrayList<>());
        errors.put("passwordErrors", new ArrayList<>());
        errors.put("matchingPasswordErrors", new ArrayList<>());
        return errors;
    }
}
