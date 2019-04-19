package com.licence.web.controllers;

import com.licence.web.models.UDT.KeyspaceUser;
import com.licence.web.models.UDT.UserKeyspace;
import com.licence.web.models.User;
import com.licence.web.services.UserService;
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

    @Autowired
    public SearchController(UserService userService) {
        this.userService = userService;
    }


    @GetMapping(value = "${route.searchUserLive}", produces = "application/json")
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
                    if(!Objects.equals(p.getUserName(), userActiveKeyspace.getCreatorName())) {
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
