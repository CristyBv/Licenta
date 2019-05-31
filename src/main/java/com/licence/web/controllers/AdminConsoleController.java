package com.licence.web.controllers;

import com.licence.config.properties.KeyspaceProperties;
import com.licence.config.properties.RouteProperties;
import com.licence.config.security.CassandraUserDetails;
import com.licence.web.models.User;
import com.licence.web.models.pojo.KeyspaceContentObject;
import com.licence.web.services.KeyspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

import static com.licence.web.controllers.MyDatabaseController.prepareRowForView;
import static com.licence.web.controllers.MyDatabaseController.testKeyspaceRole;

@Controller
public class AdminConsoleController {

    private final RouteProperties routeProperties;
    private final KeyspaceService keyspaceService;
    private final KeyspaceProperties keyspaceProperties;

    @Autowired
    public AdminConsoleController(RouteProperties routeProperties, KeyspaceService keyspaceService, KeyspaceProperties keyspaceProperties) {
        this.routeProperties = routeProperties;
        this.keyspaceService = keyspaceService;
        this.keyspaceProperties = keyspaceProperties;
    }

    @RequestMapping(value = {"${route.adminConsole[index]}"})
    public String adminConsole(Model model,
                               HttpSession session,
                               Authentication authentication) {
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        if (!userDetails.getUser().getRoles().contains("ADMIN")) {
            model.addAttribute("indexError", "Access denied!");
            return "forward:" + routeProperties.getIndex();
        }
        if (session.getAttribute("adminConsoleContent") == null) {
            Map<String, Object> adminConsoleMap = new HashMap<>();
            adminConsoleMap.put("adminConsoleContent", null);
            session.setAttribute("adminConsoleContent", adminConsoleMap);
        }
        return routeProperties.getAdminConsole();
    }

    @ResponseBody
    @PostMapping(value = "${route.adminConsole[interpretor]}", produces = "application/json")
    public Map<String, Object> adminConsoleInterpretor(@RequestBody Map<String, Object> map,
                                                       Authentication authentication) {
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        if (!userDetails.getUser().getRoles().contains("ADMIN")) {
            return null;
        }
        String query = (String) map.get("query");
        Map<String, Object> result = new HashMap<>();
        if (query != null) {
            // eliminate comments from query
            query = String.join("", query.split("/\\*(.|\\n)*?\\*/")).trim();
            if (query.length() > 6 && !query.substring(0, 3).toLowerCase().equals("use")) {
                if (query.substring(0, 6).toLowerCase().equals("select")) {
                    result.put("type", "select");
                    KeyspaceContentObject content;
                    try {
                        content = keyspaceService.select(query);
                        result.put("success", prepareRowForView(content));
                    } catch (Exception e) {
                        result.put("error", e.getMessage());
                        ;
                    }
                } else {
                    try {
                        keyspaceService.execute(query);
                        result.put("success", query);
                    } catch (Exception e) {
                        result.put("error", e.getMessage());
                    }
                }
            } else {
                result.put("error", "Invalid command!");
            }
        }
        return result;
    }

    @ResponseBody
    @PostMapping(value = "${route.adminConsole[content]}")
    public String changeConsoleContent(@RequestBody Map<String, Object> map,
                                       HttpSession session) {
        try {
            Map<String, Object> adminConsoleContent = (Map<String, Object>) session.getAttribute("adminConsoleContent");
            if (adminConsoleContent != null) {
                adminConsoleContent.put("adminConsoleContent", map.get("content"));
            }
        } catch (Exception e) {
            return JSONObject.quote("Console content has not changed!");
        }
        return JSONObject.quote("Console content changed!");
    }
}
