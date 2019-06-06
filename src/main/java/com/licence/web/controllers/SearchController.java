package com.licence.web.controllers;

import com.licence.web.models.UDT.KeyspaceUser;
import com.licence.web.models.UDT.UserKeyspace;
import com.licence.web.models.User;
import com.licence.web.models.pojo.KeyspaceContent;
import com.licence.web.services.KeyspaceService;
import com.licence.web.services.UserService;
import javafx.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
public class SearchController {

    private final UserService userService;
    private final KeyspaceService keyspaceService;

    @Autowired
    public SearchController(UserService userService, KeyspaceService keyspaceService) {
        this.userService = userService;
        this.keyspaceService = keyspaceService;
    }

    @GetMapping(value = "${route.searchLive[function]}", produces = "application/json")
    public Map<String, List<Map<String, String>>> searchFunctionLive(@RequestParam(name = "search", required = false) String search,
                                                                     HttpSession session) {
        List<Map<String, String>> options = new ArrayList<>();
        if (search != null && !search.isEmpty() && session.getAttribute("userKeyspace") != null) {
            // get the columns from the current active keyspace
            UserKeyspace userKeyspace = (UserKeyspace) session.getAttribute("userKeyspace");
            KeyspaceContent keyspaceContent = keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase());
            // filter columns by search parameter
            List<Pair<String, String>> functions = keyspaceContent.getFunctions().getContent().stream().filter(p -> p.get("function_name").toString().toLowerCase().contains(search.toLowerCase())).map(p -> new Pair<>(p.get("function_name").toString(), p.get("function_name").toString() + " " + p.get("argument_types").toString() + " -> " + p.get("return_type").toString())).collect(Collectors.toList());
            // prepare result for select2
            functions.forEach(p -> {
                options.add(new HashMap<>());
                options.get(options.size() - 1).put("id", p.getKey());
                options.get(options.size() - 1).put("text", p.getValue());
            });
        }
        Map<String, List<Map<String, String>>> result = new HashMap<>();
        result.put("results", options);
        return result;
    }

    @GetMapping(value = "${route.searchLive[column]}", produces = "application/json")
    public Map<String, List<Map<String, String>>> searchColumnLive(@RequestParam(name = "search", required = false) String search,
                                                                   @RequestParam(name = "tableName", required = false) String tableName,
                                                                   HttpSession session) {
        List<Map<String, String>> options = new ArrayList<>();
        if (search != null && !search.isEmpty() && session.getAttribute("userKeyspace") != null) {
            // get the columns from the current active keyspace
            UserKeyspace userKeyspace = (UserKeyspace) session.getAttribute("userKeyspace");
            KeyspaceContent keyspaceContent = keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase());
            // filter columns by search parameter
            List<Pair<String, String>> columns;
            if (tableName != null) {
                columns = keyspaceContent.getColumns().getContent().stream().filter(p -> p.get("table_name").toString().toLowerCase().equals(tableName.toLowerCase()) && p.get("column_name").toString().toLowerCase().contains(search.toLowerCase())).map(p -> new Pair<>(p.get("column_name").toString(), p.get("column_name") + " {table: " + p.get("table_name") + ", type: " + p.get("type") + ", kind: " + p.get("kind") + "}")).collect(Collectors.toList());
            } else {
                columns = keyspaceContent.getColumns().getContent().stream().filter(p -> p.get("column_name").toString().toLowerCase().contains(search.toLowerCase())).map(p -> new Pair<>(p.get("column_name").toString(), p.get("column_name") + " {table: " + p.get("table_name") + ", type: " + p.get("type") + ", kind: " + p.get("kind") + "}")).collect(Collectors.toList());
            }
            // prepare result for select2
            columns.forEach(p -> {
                options.add(new HashMap<>());
                options.get(options.size() - 1).put("id", p.getKey());
                options.get(options.size() - 1).put("text", p.getValue());
            });
        }
        Map<String, List<Map<String, String>>> result = new HashMap<>();
        result.put("results", options);
        return result;
    }

    @GetMapping(value = "${route.searchLive[table-view]}", produces = "application/json")
    public Map<String, List<Map<String, String>>> searchTableViewLive(@RequestParam(name = "search", required = false) String search,
                                                                      HttpSession session) {
        List<Map<String, String>> options = new ArrayList<>();
        if (search != null && !search.isEmpty() && session.getAttribute("userKeyspace") != null) {
            // get the tables from the current active keyspace
            UserKeyspace userKeyspace = (UserKeyspace) session.getAttribute("userKeyspace");
            KeyspaceContent keyspaceContent = keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase());
            // filter tables by search parameter
            List<String> tables = keyspaceContent.getTables().getContent().stream().filter(p -> p.get("table_name").toString().toLowerCase().contains(search.toLowerCase())).map(p -> p.get("table_name").toString()).collect(Collectors.toList());
            // prepare result for select2
            tables.forEach(p -> {
                options.add(new HashMap<>());
                options.get(options.size() - 1).put("id", p);
                options.get(options.size() - 1).put("text", p);
            });
            // filter views by search parameter
            List<String> views = keyspaceContent.getViews().getContent().stream().filter(p -> p.get("view_name").toString().toLowerCase().contains(search.toLowerCase())).map(p -> p.get("view_name").toString()).collect(Collectors.toList());
            // prepare result for select2
            views.forEach(p -> {
                options.add(new HashMap<>());
                options.get(options.size() - 1).put("id", p);
                options.get(options.size() - 1).put("text", p);
            });
        }
        Map<String, List<Map<String, String>>> result = new HashMap<>();
        result.put("results", options);
        return result;
    }

    @GetMapping(value = "${route.searchLive[table]}", produces = "application/json")
    public Map<String, List<Map<String, String>>> searchTableLive(@RequestParam(name = "search", required = false) String search,
                                                                  HttpSession session) {
        List<Map<String, String>> options = new ArrayList<>();
        if (search != null && !search.isEmpty() && session.getAttribute("userKeyspace") != null) {
            // get the tables from the current active keyspace
            UserKeyspace userKeyspace = (UserKeyspace) session.getAttribute("userKeyspace");
            KeyspaceContent keyspaceContent = keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase());
            // filter tables by search parameter
            List<String> tables = keyspaceContent.getTables().getContent().stream().filter(p -> p.get("table_name").toString().toLowerCase().contains(search.toLowerCase())).map(p -> p.get("table_name").toString()).collect(Collectors.toList());
            // prepare result for select2
            tables.forEach(p -> {
                options.add(new HashMap<>());
                options.get(options.size() - 1).put("id", p);
                options.get(options.size() - 1).put("text", p);
            });
        }
        Map<String, List<Map<String, String>>> result = new HashMap<>();
        result.put("results", options);
        return result;
    }

    @GetMapping(value = "${route.searchLive[user]}", produces = "application/json")
    public Map<String, List<Map<String, String>>> searchUserLive(@RequestParam(name = "search", required = false) String search,
                                                                 @RequestParam(name = "from", required = false) String from,
                                                                 HttpSession session) {
        List<Map<String, String>> options = new ArrayList<>();
        if (search != null && !search.isEmpty() && session.getAttribute("userKeyspace") != null) {
            // get the usernames from the current active keyspace
            UserKeyspace userActiveKeyspace = (UserKeyspace) session.getAttribute("userKeyspace");
            List<KeyspaceUser> activeKeyspaceUsers = userActiveKeyspace.getKeyspace().getUsers();
            List<String> activeKeyspaceUsernames = activeKeyspaceUsers.stream().map(KeyspaceUser::getUserName).collect(Collectors.toList());
            // if we need to search in database
            if (Objects.equals(from, "database")) {
                List<User> databaseUsersResult = userService.findByPartialUsername(search.toLowerCase());
                // eliminate the users who are already in the current active keyspace
                databaseUsersResult = databaseUsersResult.stream().filter(p -> !activeKeyspaceUsernames.contains(p.getUserName())).collect(Collectors.toList());
                // prepare result for select2
                databaseUsersResult.forEach(p -> {
                    options.add(new HashMap<>());
                    options.get(options.size() - 1).put("id", p.getUserName());
                    options.get(options.size() - 1).put("text", p.getUserName());
                });
                // if we need to search in the current keyspace for users
            } else if (Objects.equals(from, "keyspace")) {
                // filter usernames by search parameter
                List<KeyspaceUser> keyspaceUsers = activeKeyspaceUsers.stream().filter(p -> p.getUserName().toLowerCase().contains(search.toLowerCase())).collect(Collectors.toList());
                // prepare result for select2
                keyspaceUsers.forEach(p -> {
                    if (!Objects.equals(p.getUserName(), userActiveKeyspace.getCreatorName())) {
                        options.add(new HashMap<>());
                        options.get(options.size() - 1).put("id", p.getUserName());
                        options.get(options.size() - 1).put("text", p.getUserName());
                    }
                });
            }
        }
        Map<String, List<Map<String, String>>> result = new HashMap<>();
        result.put("results", options);
        return result;
    }
}
