package com.licence.web.controllers;

import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.LocalDate;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.licence.config.properties.KeyspaceProperties;
import com.licence.config.properties.QueryProperties;
import com.licence.config.properties.RouteProperties;
import com.licence.config.security.CassandraUserDetails;
import com.licence.config.validation.password.match.PasswordMatches;
import com.licence.web.helpers.ExcelHelper;
import com.licence.web.models.Keyspace;
import com.licence.web.models.UDT.KeyspaceLog;
import com.licence.web.models.UDT.KeyspaceUser;
import com.licence.web.models.UDT.UserKeyspace;
import com.licence.web.models.UDT.UserNotification;
import com.licence.web.models.User;
import com.licence.web.models.pojo.KeyspaceContent;
import com.licence.web.models.pojo.KeyspaceContentObject;
import com.licence.web.models.pojo.VerifyQuery;
import com.licence.web.services.BackupService;
import com.licence.web.services.KeyspaceService;
import com.licence.web.services.UserService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Controller
public class MyDatabaseController {

    private final RouteProperties routeProperties;
    private final QueryProperties queryProperties;
    private final KeyspaceService keyspaceService;
    private final BackupService backupService;
    private final UserService userService;
    private final MessageSource messages;
    private final KeyspaceProperties keyspaceProperties;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    @Value("${app.domain.name}")
    private String appDomainName;

    @Autowired
    public MyDatabaseController(RouteProperties routeProperties, KeyspaceService keyspaceService, UserService userService, @Qualifier("messageSource") MessageSource messages, KeyspaceProperties keyspaceProperties, BCryptPasswordEncoder bCryptPasswordEncoder, QueryProperties queryProperties, BackupService backupService) {
        this.routeProperties = routeProperties;
        this.keyspaceService = keyspaceService;
        this.userService = userService;
        this.messages = messages;
        this.keyspaceProperties = keyspaceProperties;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.queryProperties = queryProperties;
        this.backupService = backupService;
    }

    @RequestMapping(value = "${route.myDatabase}")
    public String myDatabase(Model model,
                             Authentication authentication,
                             HttpSession session) {
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        // if a userKeyspace exist, we update it
        if (session.getAttribute("userKeyspace") != null) {
            session.setAttribute("userKeyspace", userKeyspace);
            // if after the update, it is null
            if (userKeyspace == null) {
                model.addAttribute("keyspaceNotAvailable", "Keyspace not available anymore!");
            } else {
                String activePanel = (String) session.getAttribute("activePanel");
                // if the panel is manage or view/edit
                if (Objects.equals(activePanel, keyspaceProperties.getPanel().get("manage")) || Objects.equals(activePanel, keyspaceProperties.getPanel().get("viewEdit"))) {
                    // take all the data for this keyspace (tables, columns, ...)
                    KeyspaceContent keyspaceContent = keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase());
                    model.addAttribute("keyspaceContent", keyspaceContent);
                }
                // if the panel is  view/edit
                if (Objects.equals(activePanel, keyspaceProperties.getPanel().get("viewEdit"))) {
                    // take all the data from system_schema keyspace (tables, columns, ...)
                    session.setAttribute("systemSchemaContent", keyspaceService.getKeyspaceContent("system_schema"));
                    if (session.getAttribute("dataContent") != null) {
                        // if there is a table in dataContent, then we update it
                        KeyspaceContentObject keyspaceContentObject = (KeyspaceContentObject) session.getAttribute("dataContent");
                        session.setAttribute("dataContent", keyspaceService.getSelectSimple(userKeyspace.getKeyspace().getName().toLowerCase(), keyspaceContentObject.getTableName(), "*"));
                    }
                }
                if (Objects.equals(activePanel, keyspaceProperties.getPanel().get("consoleScript"))) {
                    if (session.getAttribute("consoleScriptContent") == null) {
                        Map<String, Object> consoleScriptMap = new HashMap<>();
                        consoleScriptMap.put("consoleViewContent", null);
                        consoleScriptMap.put("scriptContent", "");
                        consoleScriptMap.put("active", "console");
                        session.setAttribute("consoleScriptContent", consoleScriptMap);
                    }
                }
                if (Objects.equals(activePanel, keyspaceProperties.getPanel().get("log"))) {
                    List<KeyspaceLog> keyspaceLogs = getLogs(userKeyspace, null, null);
                    if (keyspaceLogs != null)
                        model.addAttribute("todayLog", keyspaceLogs);
                    else
                        model.addAttribute("todayLog", userKeyspace.getKeyspace().getLog().stream().collect(lastN(20)));
                }
            }
        }
        model.addAttribute("keyspaceObject", new Keyspace());
        model.addAttribute("keyspaces", getUserKeyspaces(user));
        return routeProperties.getMyDatabase();
    }

    private List<KeyspaceLog> getLogs(UserKeyspace userKeyspace, Date date, String userName) {
        DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        try {
            if (date == null) {
                date = formatter.parse(formatter.format(new Date()));
            } else {
                date = formatter.parse(formatter.format(date));
            }
            List<KeyspaceLog> keyspaceLogs = new ArrayList<>();
            for (KeyspaceLog keyspaceLog : userKeyspace.getKeyspace().getLog()) {
                Date date2 = formatter.parse(formatter.format(keyspaceLog.getDate()));
                if (date.equals(date2) && (userName == null || keyspaceLog.getUsername().toLowerCase().equals(userName.toLowerCase())))
                    keyspaceLogs.add(keyspaceLog);
            }

            return keyspaceLogs.stream().sorted(Comparator.comparing(KeyspaceLog::getDate)).collect(Collectors.toList());
        } catch (ParseException e) {
            return null;
        }
    }

    // get last N elements from a list for stream
    private static <T> Collector<T, ?, List<T>> lastN(int n) {
        return Collector.<T, Deque<T>, List<T>>of(ArrayDeque::new, (acc, t) -> {
            if (acc.size() == n)
                acc.pollFirst();
            acc.add(t);
        }, (acc1, acc2) -> {
            while (acc2.size() < n && !acc1.isEmpty()) {
                acc2.addFirst(acc1.pollLast());
            }
            return acc2;
        }, ArrayList::new);
    }

    @ResponseBody
    @PostMapping(value = "${route.log[filter]}", produces = "application/json")
    public List<KeyspaceLog> logFilter(@RequestBody Map<String, Object> map,
                                       Authentication authentication,
                                       HttpSession session,
                                       HttpServletResponse response) {
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
            if (map.get("date") != null && map.get("username") != null) {
                String date = map.get("date").toString();
                String username = map.get("username").toString();
                if (date.equals("today"))
                    return getLogs(userKeyspace, null, username);
                else {
                    try {
                        return getLogs(userKeyspace, formatter.parse(date), username);
                    } catch (ParseException e) {
                        return null;
                    }
                }
            } else if (map.get("date") != null) {
                String date = map.get("date").toString();
                if (date.equals("today"))
                    return getLogs(userKeyspace, null, null);
                else {
                    try {
                        return getLogs(userKeyspace, formatter.parse(date), null);
                    } catch (ParseException e) {
                        return new ArrayList<>();
                    }
                }
            }
        }
        return new ArrayList<>();
    }

    @ResponseBody
    @GetMapping(value = "${route.export[word-logs]}")
    public void exportWordLogs(Authentication authentication,
                               HttpSession session,
                               HttpServletResponse response,
                               HttpServletRequest request) {
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            XWPFDocument document = new XWPFDocument();
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setBold(true);
            run.setText("Keyspace: " + userKeyspace.getKeyspace().getName());
            userKeyspace.getKeyspace().getLog().forEach(p -> {
                XWPFParagraph paragraphContent = document.createParagraph();
                XWPFRun runContent = paragraphContent.createRun();
                runContent.setBold(true);
                runContent.setItalic(true);
                runContent.setText(p.getUsername() + " ");

                runContent = paragraphContent.createRun();
                DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                runContent.setItalic(true);
                runContent.setText("- " + formatter.format(p.getDate()));

                paragraphContent = document.createParagraph();
                runContent = paragraphContent.createRun();
                runContent.setItalic(true);
                if (p.getType().equals(keyspaceProperties.getLog().get("typeCreate"))) {
                    runContent.setColor("33cc33");
                } else if (p.getType().equals(keyspaceProperties.getLog().get("typeUpdate"))) {
                    runContent.setColor("0000ff");
                } else if (p.getType().equals(keyspaceProperties.getLog().get("typeDelete"))) {
                    runContent.setColor("ff0000");
                }
                runContent.setText(p.getContent());
            });
            response.setHeader("Content-disposition", "attachment; filename=Log" + userKeyspace.getKeyspace().getName() + ".docx");
            try {
                document.write(response.getOutputStream());
            } catch (IOException e) {
                request.setAttribute("keyspaceImportExportError", "Export logs failed! Please refresh and try again!");
                try {
                    request.getRequestDispatcher(routeProperties.getMyDatabase()).forward(request, response);
                } catch (ServletException | IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    @ResponseBody
    @GetMapping(value = "${route.export[json-keyspace]}")
    public void exportJsonKeyspace(Authentication authentication,
                                   HttpSession session,
                                   HttpServletResponse response,
                                   HttpServletRequest request) {
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            Boolean error = false;
            try {
                ObjectMapper mapper = new ObjectMapper();
                response.setHeader("Content-disposition", "attachment; filename=" + userKeyspace.getKeyspace().getName() + ".json");
                mapper.writeValue(response.getOutputStream(), getAllKeyspaceContent(userKeyspace));
            } catch (JsonGenerationException | JsonMappingException e) {
                error = true;
            } catch (IOException e) {
                error = true;
            }
            if (error) {
                request.setAttribute("keyspaceImportExportError", "Export keyspace failed! Please refresh and try again!");
                try {
                    request.getRequestDispatcher(routeProperties.getMyDatabase()).forward(request, response);
                } catch (ServletException | IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    private Map<String, Object> getAllKeyspaceContent(UserKeyspace userKeyspace) {
        Map<String, Object> map = new HashMap<>();
        KeyspaceContent keyspaceContent = keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase());
        map.put("Keyspace", userKeyspace.getKeyspace());
        map.put("KeyspaceContent", keyspaceContent);
        List<KeyspaceContentObject> list = new ArrayList<>();
        keyspaceContent.getTables().getContent().forEach(p -> {
            list.add(keyspaceService.getSelectSimple(userKeyspace.getKeyspace().getName(), p.get("table_name").toString(), "*"));
        });
        map.put("TablesContent", list);
        return map;
    }

    @ResponseBody
    @GetMapping(value = "${route.export[json-table-view]}")
    public void exportJsonTableView(@RequestParam String tableName,
                                    Authentication authentication,
                                    HttpSession session,
                                    HttpServletResponse response,
                                    HttpServletRequest request) {
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            Boolean error = false;
            KeyspaceContentObject table = keyspaceService.getSelectSimple(userKeyspace.getKeyspace().getName(), tableName, "*");
            try {
                ObjectMapper mapper = new ObjectMapper();
                response.setHeader("Content-disposition", "attachment; filename=" + tableName + ".json");
                mapper.writeValue(response.getOutputStream(), table.getContent());
            } catch (JsonGenerationException | JsonMappingException e) {
                error = true;
            } catch (IOException e) {
                error = true;
            }
            if (error) {
                request.setAttribute("keyspaceImportExportError", "Export failed! Please refresh and try again!");
                try {
                    request.getRequestDispatcher(routeProperties.getMyDatabase()).forward(request, response);
                } catch (ServletException | IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    @ResponseBody
    @GetMapping(value = "${route.export[excel-table-view]}")
    public void exportExcelTableView(@RequestParam String tableName,
                                     Authentication authentication,
                                     HttpSession session,
                                     HttpServletResponse response,
                                     HttpServletRequest request) {
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            KeyspaceContentObject table = keyspaceService.getSelectSimple(userKeyspace.getKeyspace().getName(), tableName, "*");
            ExcelHelper excelHelper = new ExcelHelper();
            XSSFWorkbook workbook = excelHelper.createTable(table);
            response.setHeader("Content-disposition", "attachment; filename=" + tableName + ".xlsx");
            try {
                workbook.write(response.getOutputStream());
            } catch (IOException e) {
                request.setAttribute("keyspaceImportExportError", "Export failed! Please refresh and try again!");
                try {
                    request.getRequestDispatcher(routeProperties.getMyDatabase()).forward(request, response);
                } catch (ServletException | IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    @GetMapping(value = "${route.search[all]}")
    public String searchAll(@RequestParam(required = false, name = "search") String search,
                            Model model,
                            Authentication authentication,
                            HttpSession session) {
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            KeyspaceContent keyspaceContent = keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase());
            Map<String, KeyspaceContentObject> result = new HashMap<>();
            if (search != null && !search.trim().isEmpty()) {
                Set<String> setSearch = Arrays.stream(search.trim().toLowerCase().split("@")).filter(p -> !p.trim().isEmpty()).collect(Collectors.toSet());
                keyspaceContent.getTables().getContent().forEach(p -> {
                    String tableName = p.get("table_name").toString();
                    KeyspaceContentObject content = keyspaceService.getSelectSimple(userKeyspace.getKeyspace().getName(), tableName, "*");
                    KeyspaceContentObject keyspaceContentObject = new KeyspaceContentObject();
                    keyspaceContentObject.setTableName(tableName);
                    keyspaceContentObject.setColumnDefinitions(content.getColumnDefinitions());
                    keyspaceContentObject.setContent(new ArrayList<>());
                    Boolean[] ok = {false};
                    for (Map<String, Object> objectMap : content.getContent()) {
                        for (Map.Entry<String, Object> entry : objectMap.entrySet()) {
                            Boolean rowResultFound = false;
                            for (String s : setSearch) {
                                if (!s.trim().isEmpty() && entry.getValue() != null && entry.getValue().toString().toLowerCase().contains(s.trim())) {
                                    ok[0] = true;
                                    rowResultFound = true;
                                    keyspaceContentObject.getContent().add(objectMap);
                                    break;
                                }
                            }
                            if (rowResultFound)
                                break;
                        }
                        Integer maxRowsPerTable = Integer.parseInt(keyspaceProperties.getSearch().get("maxRowsPerTable"));
                        if (keyspaceContentObject.getContent().size() == maxRowsPerTable) {
                            model.addAttribute("keyspaceSearchError", "Some results are not shown because some tables contains more then " + maxRowsPerTable + " rows! Please search by non-ambiguous parameters!");
                            break;
                        }
                    }
                    if (ok[0]) {
                        result.put(tableName, keyspaceContentObject);
                    }
                });
            }
            model.addAttribute("searchResult", result);
        } else {
            model.addAttribute("keyspaceSearchError", "The search failed! Please refresh and try again!");
        }
        model.addAttribute("searchParam", search);
        return "forward:" + routeProperties.getMyDatabase();
    }

    @ResponseBody
    @PostMapping(value = "${route.script[active]}")
    public String changeToScript(HttpSession session) {
        try {
            Map<String, Object> consoleScriptContent = (Map<String, Object>) session.getAttribute("consoleScriptContent");
            if (consoleScriptContent != null) {
                consoleScriptContent.put("active", "script");
            }
        } catch (Exception e) {
            return JSONObject.quote("Script content has not changed!");
        }
        return JSONObject.quote("Script content changed!");
    }

    @ResponseBody
    @PostMapping(value = "${route.console[active]}")
    public String changeToConsole(HttpSession session) {
        try {
            Map<String, Object> consoleScriptContent = (Map<String, Object>) session.getAttribute("consoleScriptContent");
            if (consoleScriptContent != null) {
                consoleScriptContent.put("active", "console");
            }
        } catch (Exception e) {
            return JSONObject.quote("Console content has not changed!");
        }
        return JSONObject.quote("Console content changed!");
    }


    @ResponseBody
    @PostMapping(value = "${route.script[content]}")
    public String changeScriptContent(@RequestBody Map<String, Object> map,
                                      HttpSession session) {
        try {
            Map<String, Object> consoleScriptContent = (Map<String, Object>) session.getAttribute("consoleScriptContent");
            if (consoleScriptContent != null) {
                consoleScriptContent.put("scriptContent", map.get("content"));
                consoleScriptContent.put("active", "script");
            }
        } catch (Exception e) {
            return JSONObject.quote("Script content has not changed!");
        }
        return JSONObject.quote("Script content changed!");
    }

    @ResponseBody
    @PostMapping(value = "${route.console[content]}")
    public String changeConsoleContent(@RequestBody Map<String, Object> map,
                                       HttpSession session) {
        try {
            Map<String, Object> consoleScriptContent = (Map<String, Object>) session.getAttribute("consoleScriptContent");
            if (consoleScriptContent != null) {
                consoleScriptContent.put("consoleViewContent", map.get("content"));
            }
        } catch (Exception e) {
            return JSONObject.quote("Console content has not changed!");
        }
        return JSONObject.quote("Console content changed!");
    }


    @ResponseBody
    @PostMapping(value = "${route.console[interpretor]}", produces = "application/json")
    public Map<String, Object> consoleInterpretor(@RequestBody Map<String, Object> map,
                                                  Authentication authentication,
                                                  HttpSession session) {
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            try {
                String query = (String) map.get("query");
                if (query != null) {
                    // eliminate comments from query
                    query = String.join("", query.split("/\\*(.|\\n)*?\\*/"));
                    if (query.trim().length() > 0) {
                        VerifyQuery verifyQuery = new VerifyQuery(userKeyspace.getKeyspace().getName(), queryProperties);
                        Map<String, Object> detectedQuery = verifyQuery.detectQuery(query);
                        if (detectedQuery.get("error") != null) {
                            detectedQuery.put("error", detectedQuery.get("error").toString().replaceAll("]", ")").replaceAll("\\[", "("));
                        } else {
                            // if the type is != null that means the command is a select
                            if (detectedQuery.get("type").equals("select")) {
                                try {
                                    KeyspaceContentObject content = keyspaceService.select(detectedQuery.get("success").toString());
                                    detectedQuery.put("content", prepareRowForView(content));
                                } catch (Exception e) {
                                    detectedQuery.put("error", e.getMessage());
                                }
                            } else {
                                try {
                                    keyspaceService.execute(detectedQuery.get("success").toString());
                                    String type = detectedQuery.get("type").toString();
                                    // prepare and add log
                                    String logContent = "Executed a query in this keyspace!\n     Query: " + detectedQuery.get("success");
                                    if (type.contains("update") || type.contains("alter") || type.contains("batch")) {
                                        userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeUpdate"), logContent, user.getUserName());
                                    } else if (type.contains("drop") || type.contains("truncate") || type.contains("delete")) {
                                        userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeDelete"), logContent, user.getUserName());
                                    } else if (type.contains("create") || type.contains("insert")) {
                                        userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeCreate"), logContent, user.getUserName());
                                    }
                                    keyspaceService.save(userKeyspace.getKeyspace(), false, false);
                                } catch (Exception e) {
                                    detectedQuery.put("error", e.getMessage());
                                }
                            }
                        }
                        return detectedQuery;
                    } else {
                        return new HashMap<>();
                    }
                }
            } catch (Exception e) {
                return new HashMap<>();
            }
        }
        return null;
    }

    @PostMapping(value = "${route.drop[trigger]}")
    public String dropTrigger(@RequestParam(required = false) String triggerName,
                              @RequestParam(required = false) String tableName,
                              Authentication authentication,
                              HttpSession session,
                              Model model,
                              HttpServletRequest request) {
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            try {
                // prepare backup for this trigger structure
                KeyspaceContent keyspaceContent = keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase());
                String backupContent;
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> triggerMap = keyspaceContent.getTriggers().getContent().stream().filter(p -> p.get("trigger_name").toString().equals(triggerName)).findAny().orElse(null);
                    backupContent = objectMapper.writeValueAsString(triggerMap);
                } catch (JsonProcessingException e) {
                    backupContent = "Json parse error for trigger structure!";
                }

                keyspaceService.dropTrigger(userKeyspace.getKeyspace().getName(), tableName, triggerName);

                // prepare and add log with backup url
                String logContent = "Dropped a trigger from this keyspace!\n     Name: " + triggerName + "\n     Backup-trigger: " + backupService.getUrlBackupSave(backupContent, userKeyspace.getKeyspace().getName(), appDomainName);
                userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeDelete"), logContent, user.getUserName());
                keyspaceService.save(userKeyspace.getKeyspace(), false, false);
                model.addAttribute("keyspaceViewEditSuccess", "Trigger deleted!");
                return "forward:" + routeProperties.getMyDatabase();
            } catch (Exception e) {
                model.addAttribute("keyspaceViewEditError", e.getMessage());
                return "forward:" + routeProperties.getMyDatabase();
            }
        }
        model.addAttribute("keyspaceViewEditError", "The trigger was not deleted! Please refresh and try again!");
        return "forward:" + routeProperties.getMyDatabase();
    }

    @PostMapping(value = "${route.create[trigger]}")
    public String createTrigger(@RequestParam(required = false) String tableName,
                                @RequestParam(required = false) String triggerName,
                                @RequestParam(required = false) String triggerClass,
                                Authentication authentication,
                                HttpSession session,
                                Model model) {
        //request.getParameterMap().forEach((k, v) -> System.out.println(k + " --- " + Arrays.toString(v)));
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            try {
                keyspaceService.createTrigger(userKeyspace.getKeyspace().getName(), tableName, triggerName, triggerClass);
                // prepare and add log with backup url
                String logContent = "Created a trigger in this keyspace!\n     Name: " + triggerName;
                userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeCreate"), logContent, user.getUserName());
                keyspaceService.save(userKeyspace.getKeyspace(), false, false);
                model.addAttribute("keyspaceViewEditSuccess", "Trigger created!");
                return "forward:" + routeProperties.getMyDatabase();
            } catch (Exception e) {
                model.addAttribute("keyspaceViewEditError", e.getMessage());
                return "forward:" + routeProperties.getMyDatabase();
            }
        }
        model.addAttribute("keyspaceViewEditError", "The trigger was not created! Please refresh and try again!");
        return "forward:" + routeProperties.getMyDatabase();
    }

    @PostMapping(value = "${route.drop[aggregate]}")
    public String dropAggregate(@RequestParam(required = false) String aggregateName,
                                Authentication authentication,
                                HttpSession session,
                                Model model,
                                HttpServletRequest request) {
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            try {
                // prepare backup for this aggregate structure
                KeyspaceContent keyspaceContent = keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase());
                String backupContent;
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> aggregateMap = keyspaceContent.getAggregates().getContent().stream().filter(p -> p.get("aggregate_name").toString().equals(aggregateName)).findAny().orElse(null);
                    backupContent = objectMapper.writeValueAsString(aggregateMap);
                } catch (JsonProcessingException e) {
                    backupContent = "Json parse error for aggregate structure!";
                }

                keyspaceService.dropAggregate(userKeyspace.getKeyspace().getName(), aggregateName);

                // prepare and add log with backup url
                String logContent = "Dropped an aggregate from this keyspace!\n     Name: " + aggregateName + "\n     Backup-aggregate: " + backupService.getUrlBackupSave(backupContent, userKeyspace.getKeyspace().getName(), appDomainName);
                userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeDelete"), logContent, user.getUserName());
                keyspaceService.save(userKeyspace.getKeyspace(), false, false);
                model.addAttribute("keyspaceViewEditSuccess", "Aggregate deleted!");
                return "forward:" + routeProperties.getMyDatabase();
            } catch (Exception e) {
                model.addAttribute("keyspaceViewEditError", e.getMessage());
                return "forward:" + routeProperties.getMyDatabase();
            }
        }
        model.addAttribute("keyspaceViewEditError", "The aggregate was not deleted! Please refresh and try again!");
        return "forward:" + routeProperties.getMyDatabase();
    }

    @PostMapping(value = "${route.create[aggregate]}")
    public String createAggregate(@RequestParam(required = false) String replace,
                                  @RequestParam(required = false) String aggregateName,
                                  @RequestParam(required = false) String returnType,
                                  @RequestParam(required = false) String stateFunction,
                                  @RequestParam(required = false) String stateType,
                                  @RequestParam(required = false) String finalFunction,
                                  @RequestParam(required = false) String initialCondition,
                                  Authentication authentication,
                                  HttpSession session,
                                  WebRequest request,
                                  Model model) {
        //request.getParameterMap().forEach((k, v) -> System.out.println(k + " --- " + Arrays.toString(v)));
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            if (replace == null)
                replace = "";
            try {
                keyspaceService.createAggregate(userKeyspace.getKeyspace().getName(), aggregateName, replace, returnType, stateFunction, stateType, finalFunction, initialCondition);
                // prepare and add log with backup url
                String logContent = "Created an aggregate in this keyspace!\n     Name: " + aggregateName;
                userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeCreate"), logContent, user.getUserName());
                keyspaceService.save(userKeyspace.getKeyspace(), false, false);
                model.addAttribute("keyspaceViewEditSuccess", "Aggregate created!");
                return "forward:" + routeProperties.getMyDatabase();
            } catch (Exception e) {
                model.addAttribute("keyspaceViewEditError", e.getMessage());
                return "forward:" + routeProperties.getMyDatabase();
            }
        }
        model.addAttribute("keyspaceViewEditError", "The aggregate was not created! Please refresh and try again!");
        return "forward:" + routeProperties.getMyDatabase();
    }

    @PostMapping(value = "${route.drop[function]}")
    public String dropFunction(@RequestParam(required = false) String functionName,
                               Authentication authentication,
                               HttpSession session,
                               Model model,
                               HttpServletRequest request) {
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            try {
                // prepare backup for this function structure
                KeyspaceContent keyspaceContent = keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase());
                String backupContent;
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> functionMap = keyspaceContent.getFunctions().getContent().stream().filter(p -> p.get("function_name").toString().equals(functionName)).findAny().orElse(null);
                    backupContent = objectMapper.writeValueAsString(functionMap);
                } catch (JsonProcessingException e) {
                    backupContent = "Json parse error for function structure!";
                }

                keyspaceService.dropFunction(userKeyspace.getKeyspace().getName(), functionName);

                // prepare and add log with backup url
                String logContent = "Dropped a function from this keyspace!\n     Name: " + functionName + "\n     Backup-function: " + backupService.getUrlBackupSave(backupContent, userKeyspace.getKeyspace().getName(), appDomainName);
                userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeDelete"), logContent, user.getUserName());
                keyspaceService.save(userKeyspace.getKeyspace(), false, false);
                model.addAttribute("keyspaceViewEditSuccess", "Function deleted!");
                return "forward:" + routeProperties.getMyDatabase();
            } catch (Exception e) {
                model.addAttribute("keyspaceViewEditError", e.getMessage());
                return "forward:" + routeProperties.getMyDatabase();
            }
        }
        model.addAttribute("keyspaceViewEditError", "The function was not deleted! Please refresh and try again!");
        return "forward:" + routeProperties.getMyDatabase();
    }

    @PostMapping(value = "${route.create[function]}")
    public String createFunction(@RequestParam(required = false) String replace,
                                 @RequestParam(required = false) String functionName,
                                 @RequestParam(required = false) String inputs,
                                 @RequestParam(required = false) String onNullInput,
                                 @RequestParam(required = false) String returns,
                                 @RequestParam(required = false) String language,
                                 @RequestParam(required = false) String codeBlock,
                                 Authentication authentication,
                                 HttpSession session,
                                 WebRequest request,
                                 Model model) {
        //request.getParameterMap().forEach((k, v) -> System.out.println(k + " --- " + Arrays.toString(v)));
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            if (replace == null)
                replace = "";
            try {
                keyspaceService.createFunction(userKeyspace.getKeyspace().getName(), functionName, replace, inputs, onNullInput, returns, language, codeBlock);
                // prepare and add log with backup url
                String logContent = "Created a function in this keyspace!\n     Name: " + functionName;
                userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeCreate"), logContent, user.getUserName());
                keyspaceService.save(userKeyspace.getKeyspace(), false, false);
                model.addAttribute("keyspaceViewEditSuccess", "Function created!");
                return "forward:" + routeProperties.getMyDatabase();
            } catch (Exception e) {
                model.addAttribute("keyspaceViewEditError", e.getMessage());
                return "forward:" + routeProperties.getMyDatabase();
            }
        }
        model.addAttribute("keyspaceViewEditError", "The function was not created! Please refresh and try again!");
        return "forward:" + routeProperties.getMyDatabase();
    }

    @PostMapping(value = "${route.drop[type]}")
    public String dropType(@RequestParam(required = false) String type,
                           Authentication authentication,
                           HttpSession session,
                           Model model,
                           HttpServletRequest request) {
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            try {
                // prepare backup for this type structure
                KeyspaceContent keyspaceContent = keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase());
                String backupContent;
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> typeMap = keyspaceContent.getTypes().getContent().stream().filter(p -> p.get("type_name").toString().equals(type)).findAny().orElse(null);
                    backupContent = objectMapper.writeValueAsString(typeMap);
                } catch (JsonProcessingException e) {
                    backupContent = "Json parse error for type structure!";
                }

                keyspaceService.dropType(userKeyspace.getKeyspace().getName(), type);

                // prepare and add log with backup url
                String logContent = "Dropped a type from this keyspace!\n     Name: " + type + "\n     Backup-type: " + backupService.getUrlBackupSave(backupContent, userKeyspace.getKeyspace().getName(), appDomainName);
                userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeDelete"), logContent, user.getUserName());
                keyspaceService.save(userKeyspace.getKeyspace(), false, false);
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

    @PostMapping(value = "${route.alter[type]}")
    public String alterType(Authentication authentication,
                            WebRequest request,
                            HttpSession session,
                            Model model) {
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        String typeName = request.getParameter("type_name_readonly");
        String field = request.getParameter("field_names");
        if (userKeyspace != null) {
            if (field == null || field.isEmpty()) {
                model.addAttribute("keyspaceViewEditError", "No options selected for update!");
                return "forward:" + routeProperties.getMyDatabase();
            }
            // if the field content starts with [ and end with ] (it's a list), then the alter is for renaming column(s)
            if (field.substring(0, 1).contains("[") && field.substring(field.length() - 1, field.length()).contains("]")) {
                // eliminate [ and ]
                field = field.substring(1, field.length() - 1);
                // split by ,
                String[] fields = field.split(",");
                StringBuilder fieldsList = new StringBuilder();
                // the format must be fieldName@newFieldName, so we split by @ and construct a string for query
                for (String s : fields) {
                    if (s.split("@").length == 2) {
                        fieldsList.append(s.split("@")[0]).append(" TO ").append(s.split("@")[1]).append(" AND ");
                    }
                }
                // eliminate the last AND from construction
                if (fieldsList.length() != 0)
                    fieldsList.delete(fieldsList.length() - 4, fieldsList.length());
                try {
                    keyspaceService.alterTypeRenameFields(userKeyspace.getKeyspace().getName(), typeName, fieldsList.toString());
                    // prepare and add log with backup url
                    String logContent = "Altered a type (rename field(s)) in this keyspace!\n     Name: " + typeName + "\n     Rename: " + fieldsList.toString();
                    userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeUpdate"), logContent, user.getUserName());
                    keyspaceService.save(userKeyspace.getKeyspace(), false, false);
                    model.addAttribute("keyspaceViewEditSuccess", "Field(s) renamed!");
                    return "forward:" + routeProperties.getMyDatabase();
                } catch (Exception e) {
                    model.addAttribute("keyspaceViewEditError", e.getMessage());
                    return "forward:" + routeProperties.getMyDatabase();
                }
                // else, if the field split by @ has a length of 2, means the alter is for adding a new field with format field@type
            } else if (field.split("@").length == 2) {
                try {
                    keyspaceService.alterTypeAddField(userKeyspace.getKeyspace().getName(), typeName, field.split("@")[0], field.split("@")[1]);
                    // prepare and add log with backup url
                    String logContent = "Altered a type (add field) in this keyspace!\n     Name: " + typeName + "\n     Add: " + field.split("@")[0] + " " + field.split("@")[1];
                    userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeUpdate"), logContent, user.getUserName());
                    keyspaceService.save(userKeyspace.getKeyspace(), false, false);
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

    @PostMapping(value = "${route.create[type]}")
    public String createType(@RequestParam(required = false) String typeName,
                             @RequestParam(required = false) String fields,
                             Authentication authentication,
                             HttpSession session,
                             Model model) {
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            try {
                keyspaceService.createType(userKeyspace.getKeyspace().getName(), typeName, fields);
                // prepare and add log with backup url
                String logContent = "Created a type in this keyspace!\n     Name: " + typeName;
                userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeCreate"), logContent, user.getUserName());
                keyspaceService.save(userKeyspace.getKeyspace(), false, false);
                model.addAttribute("keyspaceViewEditSuccess", "Type created!");
                return "forward:" + routeProperties.getMyDatabase();
            } catch (Exception e) {
                model.addAttribute("keyspaceViewEditError", e.getMessage());
                return "forward:" + routeProperties.getMyDatabase();
            }
        }
        model.addAttribute("keyspaceViewEditError", "The type was not created! Please refresh and try again!");
        return "forward:" + routeProperties.getMyDatabase();
    }

    @PostMapping(value = "${route.create[index]}")
    public String createIndex(@RequestParam(required = false) String tableName,
                              @RequestParam(required = false) String indexName,
                              @RequestParam(required = false) String columnName,
                              @RequestParam(required = false) String custom,
                              @RequestParam(required = false) String options,
                              Authentication authentication,
                              HttpSession session,
                              WebRequest request,
                              Model model) {
        //request.getParameterMap().forEach((k, v) -> System.out.println(k + " --- " + Arrays.toString(v)));
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            try {
                if (custom == null) {
                    keyspaceService.createIndex(userKeyspace.getKeyspace().getName(), indexName, tableName, columnName);
                } else {
                    keyspaceService.createCustomIndex(userKeyspace.getKeyspace().getName(), indexName, tableName, columnName, options);
                }
                // prepare and add log with backup url
                String logContent = "Created an index in this keyspace!\n     Name: " + indexName + "\n     Table: " + tableName;
                userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeCreate"), logContent, user.getUserName());
                keyspaceService.save(userKeyspace.getKeyspace(), false, false);
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

    @PostMapping(value = "${route.drop[index]}")
    public String dropIndex(@RequestParam(required = false) String indexName,
                            Authentication authentication,
                            HttpSession session,
                            Model model,
                            HttpServletRequest request) {
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            try {
                // prepare backup for this index structure
                KeyspaceContent keyspaceContent = keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase());
                String backupContent;
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> indexMap = keyspaceContent.getIndexes().getContent().stream().filter(p -> p.get("index_name").toString().equals(indexName)).findAny().orElse(null);
                    backupContent = objectMapper.writeValueAsString(indexMap);
                } catch (JsonProcessingException e) {
                    backupContent = "Json parse error for index structure!";
                }

                keyspaceService.dropIndex(userKeyspace.getKeyspace().getName(), indexName);

                // prepare and add log with backup url
                String logContent = "Dropped an index from this keyspace!\n     Name: " + indexName + "\n     Backup-index: " + backupService.getUrlBackupSave(backupContent, userKeyspace.getKeyspace().getName(), appDomainName);
                userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeDelete"), logContent, user.getUserName());
                keyspaceService.save(userKeyspace.getKeyspace(), false, false);
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

    @PostMapping(value = "${route.create[table]}")
    public String createTable(@RequestParam(required = false) String tableName,
                              @RequestParam(required = false) String columnsDefinitions,
                              @RequestParam(required = false) String keys,
                              @RequestParam(required = false) String clusteringOrder,
                              Authentication authentication,
                              HttpSession session,
                              Model model) {
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            try {
                if (clusteringOrder.isEmpty())
                    keyspaceService.createTable(userKeyspace.getKeyspace().getName(), tableName, columnsDefinitions, keys, null);
                else
                    keyspaceService.createTable(userKeyspace.getKeyspace().getName(), tableName, columnsDefinitions, keys, clusteringOrder);
                // prepare and add log with backup url
                String logContent = "Created a table in this keyspace!\n     Name: " + tableName;
                userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeCreate"), logContent, user.getUserName());
                keyspaceService.save(userKeyspace.getKeyspace(), false, false);
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

    @PostMapping(value = "${route.create[view]}")
    public String createView(@RequestParam(required = false) String viewName,
                             @RequestParam(required = false) String baseTableName,
                             @RequestParam(required = false) String columnsSelected,
                             @RequestParam(required = false) String whereClause,
                             @RequestParam(required = false) String keys,
                             @RequestParam(required = false) String clusteringOrder,
                             Authentication authentication,
                             HttpSession session,
                             Model model) {
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            try {
                if (clusteringOrder.isEmpty())
                    keyspaceService.createView(userKeyspace.getKeyspace().getName(), baseTableName, viewName, columnsSelected, whereClause, keys, null);
                else
                    keyspaceService.createView(userKeyspace.getKeyspace().getName(), baseTableName, viewName, columnsSelected, whereClause, keys, clusteringOrder);
                // prepare and add log with backup url
                String logContent = "Created a view in this keyspace!\n     Name: " + viewName + "\n     Base Table Name: " + baseTableName;
                userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeCreate"), logContent, user.getUserName());
                keyspaceService.save(userKeyspace.getKeyspace(), false, false);
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

    @PostMapping(value = "${route.drop[view]}")
    public String dropView(@RequestParam String view,
                           Authentication authentication,
                           HttpSession session,
                           Model model,
                           HttpServletRequest request) {
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            try {
                // prepare backup for this view structure
                KeyspaceContent keyspaceContent = keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase());
                String backupContent;
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> viewMap = keyspaceContent.getViews().getContent().stream().filter(p -> p.get("view_name").toString().equals(view)).findAny().orElse(null);
                    backupContent = objectMapper.writeValueAsString(viewMap);
                } catch (JsonProcessingException e) {
                    backupContent = "Json parse error for view structure!";
                }

                keyspaceService.dropView(userKeyspace.getKeyspace().getName(), view);

                // prepare and add log with backup url
                String logContent = "Dropped a view from this keyspace!\n     Name: " + view + "\n     Backup-view: " + backupService.getUrlBackupSave(backupContent, userKeyspace.getKeyspace().getName(), appDomainName);
                userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeDelete"), logContent, user.getUserName());
                keyspaceService.save(userKeyspace.getKeyspace(), false, false);
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

    @PostMapping(value = "${route.drop[table]}")
    public String dropTable(@RequestParam(required = false) String table,
                            Authentication authentication,
                            HttpSession session,
                            Model model,
                            HttpServletRequest request) {
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            try {
                // prepare backup for this table structure
                KeyspaceContent keyspaceContent = keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase());
                String backupContent;
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> tableMap = keyspaceContent.getTables().getContent().stream().filter(p -> p.get("table_name").toString().equals(table)).findAny().orElse(null);
                    backupContent = objectMapper.writeValueAsString(tableMap);
                } catch (JsonProcessingException e) {
                    backupContent = "Json parse error for table structure!";
                }
                // prepare backup for this table content
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    KeyspaceContentObject content = keyspaceService.getSelectSimple(userKeyspace.getKeyspace().getName().toLowerCase(), table, "*");
                    backupContent += "\nTable Content:\n" + objectMapper.writeValueAsString(content.getContent());
                } catch (JsonProcessingException e) {
                    backupContent += "\n\n Json parse error for table content!";
                }

                keyspaceService.dropTable(userKeyspace.getKeyspace().getName(), table);

                // prepare and add log with backup url
                String logContent = "Dropped a table from this keyspace!\n     Name: " + table + "\n     Backup-table: " + backupService.getUrlBackupSave(backupContent, userKeyspace.getKeyspace().getName(), appDomainName);
                userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeDelete"), logContent, user.getUserName());
                keyspaceService.save(userKeyspace.getKeyspace(), false, false);
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

    @PostMapping(value = "${route.alter[table]}")
    public String alterTable(@RequestParam String requestType,
                             Authentication authentication,
                             HttpServletRequest request,
                             HttpSession session,
                             Model model) {
        //request.getParameterMap().forEach((k, v) -> System.out.println(k + " --- " + Arrays.toString(v)));
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            KeyspaceContent keyspaceContent = keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase());
            // if request type contains options that means we will alter the options for the table or view
            if (requestType.contains("options") || (requestType.contains("views") && requestType.contains("options"))) {
                Map<String, Object> map;
                String structureName;
                // if it contains views, that means we will alter the options for a view and we search for that view
                if (requestType.contains("views")) {
                    map = keyspaceContent.getViews().getContent().stream().filter(p -> p.get("view_name").toString().equals(request.getParameter("view_name_readonly"))).findAny().orElse(null);
                    structureName = request.getParameter("view_name_readonly");
                }
                // else or a table and we search for that table
                else {
                    map = keyspaceContent.getTables().getContent().stream().filter(p -> p.get("table_name").toString().equals(request.getParameter("table_name_readonly"))).findAny().orElse(null);
                    structureName = request.getParameter("table_name_readonly");
                }
                if (map != null) {
                    // prepare backup for this row
                    String backupContent;
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        backupContent = objectMapper.writeValueAsString(map);
                    } catch (JsonProcessingException e) {
                        backupContent = null;
                    }
                    // if the table/view exists, we contruct the with statement for the query
                    StringBuilder with = new StringBuilder();
                    final Boolean[] somethingToUpdate = {false};
                    map.forEach((k, v) -> {
                        String param = request.getParameter(k);
                        if (param != null && !param.isEmpty()) {
                            with.append(k).append("=").append(param).append(" AND ");
                            somethingToUpdate[0] = true;
                        }
                    });
                    // we eliminate the last AND from the string
                    if (with.length() != 0)
                        with.delete(with.length() - 4, with.length());
                    // if we don't have anything for update
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
                        // prepare and add log with backup url
                        String logContent = "Altered " + structureName + "\n     With: " + with.toString() + "\n     Backup-row: " + backupService.getUrlBackupSave(backupContent, userKeyspace.getKeyspace().getName(), appDomainName);
                        userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeUpdate"), logContent, user.getUserName());
                        keyspaceService.save(userKeyspace.getKeyspace(), false, false);
                        model.addAttribute("keyspaceViewEditSuccess", "Table options updated!");
                        return "forward:" + routeProperties.getMyDatabase();
                    } catch (Exception e) {
                        model.addAttribute("keyspaceViewEditError", e.getMessage());
                        return "forward:" + routeProperties.getMyDatabase();
                    }
                }
                // else if the requstType contains columns, that means we will alter the columns for a table
            } else if (requestType.contains("columns")) {
                String columnName = request.getParameter("column_name_readonly");
                // prepare backup for this column
                String backupContent;
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> columnMap = keyspaceContent.getColumns().getContent().stream().filter(p -> p.get("column_name").toString().equals(columnName)).findAny().orElse(null);
                    backupContent = objectMapper.writeValueAsString(columnMap);
                } catch (JsonProcessingException e) {
                    backupContent = null;
                }
                // we split the requestType by @ and take the second value
                String columnsRequestType = requestType.split("@").length > 1 ? requestType.split("@")[1] : null;
                // take the tableName and columnName from request
                String tableName = request.getParameter("table_name_readonly");
                String oldType = request.getParameter("type_readonly");
                // if the request is for update the column
                if (Objects.equals(columnsRequestType, "update")) {
                    // we find the column
                    Map<String, Object> map = keyspaceContent.getColumns().getContent().stream().filter(p -> p.get("table_name").toString().equals(tableName) && p.get("column_name").toString().equals(columnName)).findAny().orElse(null);
                    if (map != null) {
                        // we take the column name and the type wrote by the user
                        String columnNameToUpdate = request.getParameter("column_name");
                        String typeToUpdate = request.getParameter("type");
                        if ((columnNameToUpdate != null && columnNameToUpdate.isEmpty()) && (typeToUpdate != null && typeToUpdate.isEmpty())) {
                            model.addAttribute("keyspaceViewEditError", "No fields selected for update!");
                            return "forward:" + routeProperties.getMyDatabase();
                        } else {
                            StringBuilder messageSuccess = new StringBuilder();
                            StringBuilder messageError = new StringBuilder();
                            // if we must rename a column
                            if (columnNameToUpdate != null && !columnNameToUpdate.isEmpty()) {
                                try {
                                    keyspaceService.alterColumnName(userKeyspace.getKeyspace().getName(), tableName, columnName, columnNameToUpdate);
                                    // prepare and add log with backup url
                                    String logContent = "Renamed column from " + tableName + "\n     Name: " + columnName + "\n     Renamed: " + columnNameToUpdate;
                                    userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeUpdate"), logContent, user.getUserName());
                                    keyspaceService.save(userKeyspace.getKeyspace(), false, false);
                                    messageSuccess.append("Column renamed!");
                                    model.addAttribute("keyspaceViewEditSuccess", messageSuccess.toString());
                                } catch (Exception e) {
                                    messageError.append(e.getMessage());
                                    model.addAttribute("keyspaceViewEditError", messageError.toString());
                                }
                            }
                            // if the must change the type of a column
                            if (typeToUpdate != null && !typeToUpdate.isEmpty()) {
                                try {
                                    keyspaceService.alterColumnType(userKeyspace.getKeyspace().getName(), tableName, columnName, typeToUpdate);
                                    // prepare and add log with backup url
                                    String logContent = "Changed type from" + tableName + "\n     Name: " + columnName + "\n     Old Type: " + oldType + "\n     New Type: " + typeToUpdate;
                                    userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeUpdate"), logContent, user.getUserName());
                                    keyspaceService.save(userKeyspace.getKeyspace(), false, false);
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
                    // else if the second parameter is delete, we will drop the column
                } else if (Objects.equals(columnsRequestType, "delete")) {
                    try {
                        keyspaceService.alterDropColumn(userKeyspace.getKeyspace().getName(), tableName, columnName);
                        // prepare and add log with backup url
                        String logContent = "Dropped a column from " + tableName + "\n     Name: " + columnName + "\n     Backup-column: " + backupService.getUrlBackupSave(backupContent, userKeyspace.getKeyspace().getName(), appDomainName);
                        userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeDelete"), logContent, user.getUserName());
                        keyspaceService.save(userKeyspace.getKeyspace(), false, false);
                        model.addAttribute("keyspaceViewEditSuccess", "Column deleted!");
                        return "forward:" + routeProperties.getMyDatabase();
                    } catch (Exception e) {
                        model.addAttribute("keyspaceViewEditError", e.getMessage());
                        return "forward:" + routeProperties.getMyDatabase();
                    }
                    // else we will add a new column
                } else if (Objects.equals(columnsRequestType, "add")) {
                    try {
                        keyspaceService.alterAddColumn(userKeyspace.getKeyspace().getName(), request.getParameter("tableName"), request.getParameter("columnsDefinitions"));
                        // prepare and add log with backup url
                        String logContent = "Added a column in " + tableName + "\n     Definition(s): " + request.getParameter("columnsDefinitions");
                        userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeDelete"), logContent, user.getUserName());
                        keyspaceService.save(userKeyspace.getKeyspace(), false, false);
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
    @PostMapping(value = "${route.get[tableStructure]}", produces = "application/json")
    public Map<String, Object> getDataStructure(@RequestBody Map<String, String> data,
                                                Authentication authentication,
                                                HttpSession session) {
        // this function will return the structure data from the system_schema tables for the current keyspace
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

    @PostMapping(value = "${route.insert[row]}")
    public String insertRow(@RequestParam String tableName,
                            Authentication authentication,
                            WebRequest request,
                            HttpSession session,
                            Model model) {
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        KeyspaceContentObject keyspaceContentObject = (KeyspaceContentObject) session.getAttribute("dataContent");
        if (userKeyspace != null && keyspaceContentObject != null && tableName.equals(keyspaceContentObject.getTableName())) {
            StringBuilder insertColumns = new StringBuilder("");
            StringBuilder insertValues = new StringBuilder("");
            // if the table has no data, we don't have the columns definitions in the keyspceContentObject, so we iterate
            // in all the columns from this keyspace
            if (keyspaceContentObject.getColumnDefinitions() == null) {
                KeyspaceContent keyspaceContent = keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase());
                keyspaceContent.getColumns().getContent().forEach(p -> {
                    if (p.get("table_name").toString().equals(keyspaceContentObject.getTableName())) {
                        String param = request.getParameter(p.get("column_name").toString());
                        // if the column insertion value exists, we put the column name and value in the query
                        if (param != null && !param.isEmpty()) {
                            insertColumns.append(p.get("column_name").toString()).append(",");
                            insertValues.append(param).append(",");
                        }
                    }
                });
                // else we iterate in the columnDefinitions
            } else {
                keyspaceContentObject.getColumnDefinitions().forEach(d -> {
                    String param = request.getParameter(d.getName());
                    // if the column insertion value exists, we put the column name in query
                    if (param != null && !param.isEmpty()) {
                        insertColumns.append(d.getName()).append(",");
                        insertValues.append(param).append(",");
                    }
                });
            }
            // we eliminate the last , from construction
            if (insertColumns.length() != 0 && insertValues.length() != 0) {
                insertColumns.deleteCharAt(insertColumns.length() - 1);
                insertValues.deleteCharAt(insertValues.length() - 1);
                try {
                    KeyspaceContent keyspaceContent = keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase());
                    // if the table is a view, we will insert the values in the base table
                    Map<String, Object> map = keyspaceContent.getViews().getContent().stream().filter(p -> p.get("view_name").toString().equals(keyspaceContentObject.getTableName())).findAny().orElse(null);
                    if (map != null) {
                        keyspaceService.insert(userKeyspace.getKeyspace().getName(), map.get("base_table_name").toString(), insertColumns.toString(), insertValues.toString(), "");
                    } else {
                        keyspaceService.insert(userKeyspace.getKeyspace().getName(), keyspaceContentObject.getTableName(), insertColumns.toString(), insertValues.toString(), "");
                    }
                    // prepare and add log with backup url
                    String logContent = "Inserted a row in " + keyspaceContentObject.getTableName() + "\n     Columns: " + insertColumns.toString() + "\n     Values: " + insertValues.toString();
                    userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeUpdate"), logContent, user.getUserName());
                    keyspaceService.save(userKeyspace.getKeyspace(), false, false);
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
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        KeyspaceContentObject keyspaceContentObject = (KeyspaceContentObject) session.getAttribute("dataContent");
        if (userKeyspace != null && keyspaceContentObject != null && tableName.equals(keyspaceContentObject.getTableName())) {
            KeyspaceContent keyspaceContent = keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase());
            // if the table we want to update/delete is a view, we will set the base table for the name
            Map<String, Object> map = keyspaceContent.getViews().getContent().stream().filter(p -> p.get("view_name").toString().equals(keyspaceContentObject.getTableName())).findAny().orElse(null);
            if (map != null)
                keyspaceContentObject.setTableName(map.get("base_table_name").toString());
            // take the primary keys for this table
            List<Map<String, Object>> primaryKeys = getPrimaryKeysFromTable(keyspaceContent, keyspaceContentObject);
            // find the row we want to update/delete using the primary keys
            Map<String, Object> findRow = findRowFromRequest(keyspaceContentObject, primaryKeys, request);
            // after we set the table name as it was
            keyspaceContentObject.setTableName(tableName);
            // if the row exists
            if (findRow != null) {
                // prepare backup for this row
                String backupContent;
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    backupContent = objectMapper.writeValueAsString(findRow);
                } catch (JsonProcessingException e) {
                    backupContent = null;
                }
                // construct the where statement using the primary keys
                StringBuilder where = new StringBuilder("");
                primaryKeys.forEach(p -> {
                    DataType dataType = keyspaceContentObject.getColumnDefinitions().getType(p.get("column_name").toString());
                    where.append(p.get("column_name")).append("=").append(databaseCorrespondence(findRow.get(p.get("column_name").toString()), dataType)).append(" AND ");
                });
                // eliminate the last AND
                if (where.length() != 0)
                    where.delete(where.length() - 4, where.length());
                try {
                    // if the request is for update, we contruct the set statement
                    if (requestType.equals("update")) {
                        StringBuilder set = new StringBuilder("");
                        keyspaceContentObject.getColumnDefinitions().forEach(d -> {
                            String param = request.getParameter(d.getName());
                            // if the value exists
                            if (param != null && !param.isEmpty()) {
                                set.append(d.getName()).append("=").append(param).append(",");
                            }
                        });
                        // eliminate the last ,
                        if (set.length() != 0)
                            set.deleteCharAt(set.length() - 1);
                        if (set.length() == 0 || where.length() == 0) {
                            model.addAttribute("keyspaceViewEditError", "No columns selected for update!");
                            return "forward:" + routeProperties.getMyDatabase();
                        }
                        // if the table is a view we will update the base table
                        if (map != null) {
                            keyspaceService.update(userKeyspace.getKeyspace().getName(), map.get("base_table_name").toString(), "", set.toString(), where.toString());
                        } else {
                            keyspaceService.update(userKeyspace.getKeyspace().getName(), keyspaceContentObject.getTableName(), "", set.toString(), where.toString());
                        }
                        // prepare and add log with backup url
                        String logContent = "Updated a row from " + keyspaceContentObject.getTableName() + "\n     Where: " + where.toString() + "\n     Set: " + set.toString() + "\n     Backup-row: " + backupService.getUrlBackupSave(backupContent, userKeyspace.getKeyspace().getName(), appDomainName);
                        userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeUpdate"), logContent, user.getUserName());
                        keyspaceService.save(userKeyspace.getKeyspace(), false, false);
                        model.addAttribute("keyspaceViewEditSuccess", "Row updated!");
                        return "forward:" + routeProperties.getMyDatabase();
                        // else if the request is for delete, we construct the delete statement
                    } else if (requestType.equals("delete")) {
                        // if there is no values in request that means we will delete the entire row
                        final Boolean[] entireRow = {true};
                        StringBuilder delete = new StringBuilder("");
                        keyspaceContentObject.getColumnDefinitions().forEach(d -> {
                            String param = request.getParameter(d.getName());
                            if (param != null && !param.isEmpty() && param.equals("null")) {
                                entireRow[0] = false;
                                delete.append(d.getName()).append(",");
                            }
                        });
                        // if we must delete specific columns
                        if (!entireRow[0]) {
                            if (delete.length() != 0)
                                delete.deleteCharAt(delete.length() - 1);
                            // if the table is a view we will delete the base table
                            if (map != null) {
                                keyspaceService.delete(delete.toString(), userKeyspace.getKeyspace().getName(), map.get("base_table_name").toString(), "", where.toString());
                            } else {
                                keyspaceService.delete(delete.toString(), userKeyspace.getKeyspace().getName(), keyspaceContentObject.getTableName(), "", where.toString());
                            }
                            // prepare and add log with backup url
                            String logContent = "Deleted from " + keyspaceContentObject.getTableName() + "\n     Where: " + where.toString() + "\n     Delete: " + delete.toString() + "\n     Backup-row: " + backupService.getUrlBackupSave(backupContent, userKeyspace.getKeyspace().getName(), appDomainName);
                            userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeDelete"), logContent, user.getUserName());
                            keyspaceService.save(userKeyspace.getKeyspace(), false, false);
                            model.addAttribute("keyspaceViewEditSuccess", "Columns deleted!");
                            // else if we must delete entire row
                        } else {
                            // if the table is a view we will delete the base table
                            if (map != null) {
                                keyspaceService.delete("", userKeyspace.getKeyspace().getName(), map.get("base_table_name").toString(), "", where.toString());
                            } else {
                                keyspaceService.delete("", userKeyspace.getKeyspace().getName(), keyspaceContentObject.getTableName(), "", where.toString());
                            }
                            // prepare and add log with backup url
                            String logContent = "Deleted a row from " + keyspaceContentObject.getTableName() + "\n     Where: " + where.toString() + "\n     Delete: entire row\n     Backup-row: " + backupService.getUrlBackupSave(backupContent, userKeyspace.getKeyspace().getName(), appDomainName);
                            userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeDelete"), logContent, user.getUserName());
                            keyspaceService.save(userKeyspace.getKeyspace(), false, false);
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

    @GetMapping(value = "${route.get[databaseContent]}")
    public String getDatabaseContent(@RequestParam(name = "table", required = false) String table,
                                     HttpSession session,
                                     Authentication authentication) {
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            if (!table.isEmpty()) {
                session.setAttribute("dataContent", keyspaceService.getSelectSimple(userKeyspace.getKeyspace().getName().toLowerCase(), table, "*"));
            }
        }
        return "redirect:" + routeProperties.getMyDatabase() + "#keyspace-data-content";
    }


    @ResponseBody
    @GetMapping(value = "${route.get[tableData]}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getTableData(WebRequest request,
                                            HttpSession session) {
        //request.getParameterMap().forEach((k,v) -> System.out.println(k + " - " + Arrays.toString(v)));
        Map<String, Object> map = new HashMap<>();
        // we take the tableData parameters from request
        String draw = request.getParameter("draw");
        String start = request.getParameter("start");
        String length = request.getParameter("length");
        String orderColumn = request.getParameter("order[0][column]");
        String order = request.getParameter("order[0][dir]");
        String search = request.getParameter("search[value]");
        UserKeyspace userKeyspace = (UserKeyspace) session.getAttribute("userKeyspace");
        KeyspaceContentObject keyspaceContentObject = (KeyspaceContentObject) session.getAttribute("dataContent");
        if (userKeyspace != null && keyspaceContentObject != null) {
            // we update the table content
            KeyspaceContentObject update = keyspaceService.getSelectSimple(userKeyspace.getKeyspace().getName().toLowerCase(), keyspaceContentObject.getTableName(), "*");
            session.setAttribute("dataContent", update);
            //update.getContent().forEach(p -> p.put("DT_RowId", update.getTableName() + "#" + update.getContent().indexOf(p)));
            if (draw != null)
                map.put("draw", Integer.parseInt(draw));
            // put in the result the total records size
            map.put("recordsTotal", update.getContent().size());
            // we filter the data by the search parameter
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
            // put in the result the filtered records size
            map.put("recordsFiltered", update.getContent().size());
            // order the data by the order parameter
            if (orderColumn != null && order != null) {
                Integer ordCol = Integer.parseInt(orderColumn);
                update.setContent(update.getContent().stream().sorted((p, q) -> {
                    int count = 0;
                    // we search for the right column
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
                    // if the columns exists
                    if (ent1 != null && ent2 != null) {
                        // if one of the values is null
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
                            // if both values are not null
                        } else if (ent1.getValue() != null && ent2.getValue() != null) {
                            Map.Entry<String, Object> finalEnt = ent1;
                            // we take the column definition for the columns
                            ColumnDefinitions.Definition definition = update.getColumnDefinitions().asList().stream().filter(w -> w.getName().equals(finalEnt.getKey())).findAny().orElse(null);
                            if (definition != null) {
                                // if the type is timestamp, we will cast the value to date and order by that
                                if (definition.getType().getName().toString().equals("timestamp")) {
                                    Date timestamp1 = (Date) ent1.getValue();
                                    Date timestamp2 = (Date) ent2.getValue();
                                    if (order.equals("asc"))
                                        return timestamp1.compareTo(timestamp2);
                                    else if (order.equals("desc"))
                                        return timestamp2.compareTo(timestamp1);
                                }
                                // else we will order by the result string
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
            // now we will take the data by the given interval
            if (start != null && length != null) {
                Integer startVal = Integer.parseInt(start);
                Integer lengthVal = Integer.parseInt(length);
                if (lengthVal != -1) {
                    List<Map<String, Object>> updateNew = new ArrayList<>();
                    // if the interval is correct
                    if (update.getContent().size() > startVal) {
                        for (int i = startVal; i < startVal + lengthVal; i++) {
                            // if there are no data left, we stop
                            if (i >= update.getContent().size())
                                break;
                            updateNew.add(update.getContent().get(i));
                        }
                    }
                    update.setContent(updateNew);
                }
            }
            // after we prepare the values from the data for the frontend

            map.put("data", prepareRowForView(update));
        }
        return map;
    }

    private List<Map<String, String>> prepareRowForView(KeyspaceContentObject update) {
        List<Map<String, String>> updateString = new ArrayList<>();
        update.getContent().forEach(p -> {
            Map<String, String> map1 = new HashMap<>();
            p.forEach((k, v) -> {
                if (v != null) {
                    DataType type = update.getColumnDefinitions().getType(k);
                    // if the type is not viewable (date/time/byte), we cast it and convert it
                    if (type.getName().toString().equals("blob") || type.getName().toString().equals("time") || type.isCollection() || type.getName().toString().equals("timestamp")) {
                        String obj = databaseCorrespondence(v, type);
                        // eliminate the string format (the first and last ')
                        if (obj != null && obj.indexOf('\'') == 0 && obj.lastIndexOf('\'') == obj.length() - 1)
                            obj = obj.substring(1, obj.length() - 1);
                        map1.put(k, obj);
                    } else
                        map1.put(k, v.toString());
                } else
                    map1.put(k, null);
            });
            updateString.add(map1);
        });
        return updateString;
    }

    @ResponseBody
    @PostMapping(value = "${route.change[myKeyspacesPanel]}")
    public String changeMyKeyspacesPanel(@RequestBody Map<String, String> position,
                                         HttpSession session) {
        if (position.get("position").equals("open")) {
            session.setAttribute("myKeyspacesPanelPosition", "open");
        } else if (position.get("position").equals("close")) {
            session.setAttribute("myKeyspacesPanelPosition", "close");
        }
        return JSONObject.quote("success");
    }


    @PostMapping(value = "${route.keyspace[edit]}")
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
                CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
                User user = userDetails.getUser();
                userKeyspace.getKeyspace().setDurableWrites(keyspace.isDurableWrites());
                userKeyspace.getKeyspace().setReplicationFactor(keyspace.getReplicationFactor());
                userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeUpdate"), "Altered this keyspace:\n     Replication Factor: " + keyspace.getReplicationFactor() + "\n     Durable Writes: " + keyspace.isDurableWrites(), user.getUserName());
                keyspaceService.save(userKeyspace.getKeyspace(), false, true);
                model.addAttribute("keyspaceManageSuccess",
                        messages.getMessage("database.keyspaces.edit.success", null,
                                request.getLocale()));
            }

        }
        return "forward:" + routeProperties.getMyDatabase();
    }

    @PostMapping(value = "${route.keyspace[removeUser]}")
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
            CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();
            UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
            User removeUser = userService.findUserByUsername(userName);
            if (userKeyspace != null && removeUser != null && removeUser.getKeyspaces() != null) {
                // we verify that the keyspaceUser and the userKeyspace that we want to remove from keyspace table and
                // users table, exists and we save them
                KeyspaceUser keyspaceUserToRemove = userKeyspace.getKeyspace().getUsers().stream().filter(p -> Objects.equals(p.getUserName(), userName)).findFirst().orElse(null);
                UserKeyspace userKeyspaceToRemove = removeUser.getKeyspaces().stream().filter(p -> Objects.equals(p.getCreatorName(), userKeyspace.getCreatorName()) && Objects.equals(p.getName(), userKeyspace.getName())).findFirst().orElse(null);
                // if exists then we remove them and save the changes into database and session
                if (keyspaceUserToRemove != null) {
                    userKeyspace.getKeyspace().getUsers().remove(keyspaceUserToRemove);
                    userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeDelete"), "Removed a user from this keyspace!\n     Username: " + keyspaceUserToRemove.getUserName() + "\n     Access: " + keyspaceUserToRemove.getAccess(), user.getUserName());
                } else {
                    userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeDelete"), "Removed a user from this keyspace!\n     Username: " + userName, user.getUserName());
                }
                if (userKeyspaceToRemove != null)
                    removeUser.getKeyspaces().remove(userKeyspaceToRemove);

                // send a notification to the current user
                if (removeUser.getNotifications() == null)
                    removeUser.setNotifications(new ArrayList<>());
                String removeUserKeyspaceMessage = String.format(messages.getMessage("database.keyspaces.remove-user.notification", null, request.getLocale()), user.getUserName(), userKeyspace.getName());
                removeUser.getNotifications().add(new UserNotification("System", removeUserKeyspaceMessage));

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

    @PostMapping(value = "${route.keyspace[addUser]}")
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
            CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();
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
                userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeCreate"), "Added a new user to this keyspace!\n     Username: " + newKeyspaceUser.getUserName() + "\n     Access: " + newKeyspaceUser.getAccess(), user.getUserName());

                // send a notification to the added user
                if (addUser.getNotifications() == null)
                    addUser.setNotifications(new ArrayList<>());
                String addUserKeyspaceMessage = String.format(messages.getMessage("database.keyspaces.add-user.notification", null, request.getLocale()), user.getUserName(), userKeyspace.getName(), access);
                addUser.getNotifications().add(new UserNotification("System", addUserKeyspaceMessage));

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

    @PostMapping(value = "${route.delete[keyspace]}")
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

                try {
                    ObjectMapper mapper = new ObjectMapper();
                    String keyspaceBackup = mapper.writeValueAsString(getAllKeyspaceContent(userKeyspace));
                    final String[] deleteKeyspaceMessage = {String.format(messages.getMessage("database.keyspaces.delete.notification", null, request.getLocale()), user.getUserName(), userKeyspace.getName(), backupService.getUrlBackupSave(keyspaceBackup, userKeyspace.getKeyspace().getName(), appDomainName))};

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

                                        // send a notification to this user
                                        if (userFromKeyspace.getNotifications() == null)
                                            userFromKeyspace.setNotifications(new ArrayList<>());
                                        String deleteMessage = deleteKeyspaceMessage[0];
                                        if (userKeyspaceOpt2.get().getAccess().equals(keyspaceProperties.getMember())) {
                                            deleteMessage = String.format(messages.getMessage("database.keyspaces.delete.notification.viewAccess", null, request.getLocale()), user.getUserName(), userKeyspace.getName());
                                        }
                                        userFromKeyspace.getNotifications().add(new UserNotification("System", deleteMessage));
                                        // save changes
                                        userService.save(userFromKeyspace);
                                    }
                                }
                            }
                        }
                    });
                    // send a notification to the current user
                    if (user.getNotifications() == null)
                        user.setNotifications(new ArrayList<>());
                    user.getNotifications().add(new UserNotification("System", deleteKeyspaceMessage[0]));
                    // after we delete it from the keyspaces table and from cassandra too
                    keyspaceService.deleteKeyspace(userKeyspace.getKeyspace(), true);
                    // update the logged user too
                    user.getKeyspaces().remove(userKeyspace);
                    userService.save(user);
                    model.addAttribute("deleteKeyspaceSuccess",
                            messages.getMessage("database.keyspaces.delete.success", null,
                                    request.getLocale()));

                } catch (JsonProcessingException e) {
                    model.addAttribute("keyspaceManageError",
                            messages.getMessage("database.keyspaces.delete.backupError", null,
                                    request.getLocale()));
                }
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

    @PostMapping(value = "${route.change[keyspacePassword]}")
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
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null && userKeyspace.getKeyspace() != null) {
            // if keyspace is password enabled
            if (userKeyspace.getKeyspace().isPasswordEnabled()) {
                // if the old password matches with the current active keyspace
                if (bCryptPasswordEncoder.matches(keyspace.getPassword(), userKeyspace.getKeyspace().getPassword())) {
                    // update the database, the context and the session too
                    userKeyspace.getKeyspace().setPasswordEnabled(false);
                    userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeUpdate"), "Disabled password protection for this keyspace!", user.getUserName());
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
                    userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeUpdate"), "Enabled password protection for this keyspace!", user.getUserName());
                    keyspaceService.save(userKeyspace.getKeyspace(), false, false);
                    model.addAttribute("keyspaceManageSuccess",
                            messages.getMessage("database.keyspaces.password.change", null,
                                    request.getLocale()));
                }
            }
        }

        return "forward:" + routeProperties.getMyDatabase();
    }


    @PostMapping(value = "${route.keyspace[create]}")
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
                // put the log for creation
                keyspace.addLog(keyspaceProperties.getLog().get("typeCreate"), "Created this keyspace!", user.getUserName());
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

    @PostMapping(value = "${route.keyspace[connect]}")
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
                userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeCreate"), "Connected to this keyspace!", user.getUserName());
                keyspaceService.save(userKeyspace.getKeyspace(), false, false);
                session.setAttribute("userKeyspace", userKeyspace);
                session.setAttribute("activePanel", keyspaceProperties.getPanel().get("manage"));
                session.setAttribute("dataContent", null);
                session.setAttribute("myKeyspacesPanelPosition", "close");
                session.setAttribute("consoleScriptContent", null);
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

    @GetMapping(value = "${route.keyspace[disconnect]}")
    public String disconnectKeyspace(HttpSession session,
                                     Authentication authentication) {
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserKeyspace userKeyspace = updateUserKeyspaces(authentication, session);
        if (userKeyspace != null) {
            userKeyspace.getKeyspace().addLog(keyspaceProperties.getLog().get("typeDelete"), "Disconnected from this keyspace!", user.getUserName());
            keyspaceService.save(userKeyspace.getKeyspace(), false, false);
        }
        session.setAttribute("userKeyspace", null);
        session.setAttribute("dataContent", null);
        session.setAttribute("activePanel", null);
        session.setAttribute("consoleScriptContent", null);
        session.setAttribute("myKeyspacesPanelPosition", "open");
        return "redirect:" + routeProperties.getMyDatabase();
    }

    @GetMapping(value = "${route.change[databasePanel]}")
    public String changeDatabasePanel(@RequestParam String panel,
                                      HttpSession session) {
        session.setAttribute("activePanel", panel);
        if (panel.equals(keyspaceProperties.getPanel().get("viewEdit"))) {
            session.setAttribute("activeViewEditPanel", keyspaceProperties.getPanel().get("tables"));
        }
        return "redirect:" + routeProperties.getMyDatabase();
    }

    @GetMapping(value = "${route.change[databaseViewEditPanel]}")
    public String changeDatabaseViewEditPanel(@RequestParam String panel,
                                              HttpSession session) {
        session.setAttribute("activeViewEditPanel", panel);
        return "redirect:" + routeProperties.getMyDatabase();
    }

    private List<Map<String, Object>> getPrimaryKeysFromTable(KeyspaceContent
                                                                      keyspaceContent, KeyspaceContentObject keyspaceContentObject) {
        List<Map<String, Object>> primaryKeys = new ArrayList<>();
        // for each column type, we take the ones with the kind different from regular
        keyspaceContent.getColumns().getContent().forEach(p -> {
            if (p.get("table_name").toString().equals(keyspaceContentObject.getTableName()) && !p.get("kind").toString().equals("regular"))
                primaryKeys.add(p);
        });
        return primaryKeys;
    }

    private Map<String, Object> findRowFromRequest(KeyspaceContentObject
                                                           keyspaceContentObject, List<Map<String, Object>> primaryKeys, WebRequest request) {
        Map<String, Object> map;
        // search in the table
        for (Map<String, Object> p : keyspaceContentObject.getContent()) {
            Boolean ok = true;
            // for each row
            for (Map.Entry<String, Object> entry : p.entrySet()) {
                DataType dataType = keyspaceContentObject.getColumnDefinitions().getType(entry.getKey());
                final Boolean[] primaryKey = {false};
                // we test if the primary keys contains the current column from the row
                primaryKeys.forEach(q -> {
                    if (q.get("column_name").toString().equals(entry.getKey()))
                        primaryKey[0] = true;
                });
                // if the column is a primary key and the value from the current column, converted from object to string is
                // not equal with the value of this column from the request, converted from the view format to backend
                // format, that means the current row is not the one we search
                if (primaryKey[0] && !Objects.equals(databaseCorrespondence(entry.getValue(), dataType), databaseCorrespondenceByString(request.getParameter(entry.getKey() + "_readonly"), dataType.getName().toString()))) {
                    ok = false;
                }
            }
            // if ok remained true, we row is found
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
            // if the object is a string, we append '
            if (object.getClass().equals(String.class)) {
                stringBuilder.append("\'").append(object.toString()).append("\'");
                // if the object is timestamp, we cast it to Date and format it to string
            } else if (object.getClass().equals(Date.class)) {
                Date date = (Date) object;
                if (type.equals("timestamp")) {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    stringBuilder.append("\'").append(dateFormat.format(date)).append("\'");
                }
                // if the object is date, we cast it and format it to string
            } else if (object.getClass().equals(LocalDate.class)) {
                LocalDate localDate = (LocalDate) object;
                if (type.equals("date")) {
                    stringBuilder.append("\'").append(localDate.toString()).append("\'");
                }
                // if the object is boolean, we cast it to it
            } else if (object.getClass().equals(Boolean.class)) {
                Boolean bool = (Boolean) object;
                stringBuilder.append(bool);
                // if the object is blob, ww cast it and convert to array, after that we format it to hexa
            } else if (dataType.getName().toString().equals("blob")) {
                ByteBuffer byteBuffer = (ByteBuffer) object;
                byte[] bytes = byteBuffer.array();
                for (byte b : bytes) {
                    stringBuilder.append(String.format("%02x", b));
                }
                // if the object is time, we cast it and format it
            } else if (object.getClass().equals(Long.class) && type.equals("time")) {
                Long lg = (Long) object;
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                // the value is is nano seconds, so we convert it to dateFormatter
                stringBuilder.append("\'").append(LocalTime.ofNanoOfDay(lg).format(dateTimeFormatter)).append("\'");
                // if the object is a collection, we will edit every part of it
            } else if (dataType.isCollection()) {
                Object obj = editNDimensionCollectionObject(object, dataType);
                if (obj != null)
                    stringBuilder.append(obj.toString());
                // else we append the string value of the object
            } else {
                stringBuilder.append(object.toString());
            }
        } catch (ClassCastException e) {
            return null;
        }
        return stringBuilder.toString();
    }

    private Object editNDimensionCollectionObject(Object object, DataType dataType) {
        // if we find a primitive value in the collection, we convert it
        if (!dataType.isCollection() && !dataType.isFrozen()) {
            object = databaseCorrespondence(object, dataType);
            return object;
            // else we test what type of collection it is and cast the object to it
            // for each part of that collection, we repeat the process
        } else if (dataType.isCollection() || dataType.isFrozen()) {
            try {
                switch (dataType.getName().toString()) {
                    case "list":
                        List<Object> list = (ArrayList<Object>) object;
                        List<Object> list2 = new ArrayList<>();
                        for (Object o : list) {
                            list2.add(editNDimensionCollectionObject(o, dataType.getTypeArguments().get(0)));
                        }
                        return list2;
                    //break;
                    case "set":
                        Set<Object> set = (LinkedHashSet<Object>) object;
                        Set<Object> set2 = new LinkedHashSet<>();
                        for (Object o : set) {
                            set2.add(editNDimensionCollectionObject(o, dataType.getTypeArguments().get(0)));
                        }
                        return set2;
                    case "map":
                        Map<Object, Object> map = (LinkedHashMap<Object, Object>) object;
                        Map<Object, Object> map2 = new LinkedHashMap<>();
                        for (Map.Entry<Object, Object> entry : map.entrySet()) {
                            map2.put(editNDimensionCollectionObject(entry.getKey(), dataType.getTypeArguments().get(0)), editNDimensionCollectionObject(entry.getValue(), dataType.getTypeArguments().get(1)));
                        }
                        return map2;
                }
            } catch (ClassCastException e) {
                return object;
            }
        }
        // in any other case (frozen types), we return the object
        return object;
    }

    private String databaseCorrespondenceByString(String object, String type) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (object == null)
            return null;
        // if the type of the object can be a string, we append the
        if (type.equals("date") || type.equals("inet") || type.equals("text") || type.equals("time") || type.equals("varchar")) {
            stringBuilder.append("\'").append(object).append("\'");
        } else {
            stringBuilder.append(object);
        }
        return stringBuilder.toString();
    }

    private Map<String, List<UserKeyspace>> getUserKeyspaces(User user) {
        Map<String, List<UserKeyspace>> map;
        List<UserKeyspace> userKeyspaces = user.getKeyspaces();
        if (userKeyspaces != null) {
            // group the keyspaces by creator
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
        // we update the context user keyspaces
        authUser.setKeyspaces(dbUser.getKeyspaces());
        // we set the keyspaces
        updateKeyspaces(authUser);
        // if there is a keyspaces connected, we search for it in the updated keyspaces and return it
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
