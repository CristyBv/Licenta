package com.licence.web.controllers;

import com.datastax.driver.core.ColumnDefinitions;
import com.licence.config.properties.KeyspaceProperties;
import com.licence.config.properties.RouteProperties;
import com.licence.config.security.CassandraUserDetails;
import com.licence.config.validation.password.match.PasswordMatches;
import com.licence.web.models.Keyspace;
import com.licence.web.models.UDT.KeyspaceUser;
import com.licence.web.models.UDT.UserKeyspace;
import com.licence.web.models.User;
import com.licence.web.models.pojo.KeyspaceContentObject;
import com.licence.web.services.KeyspaceService;
import com.licence.web.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
                if (Objects.equals(activePanel, keyspaceProperties.getPanel().get("manage")) || Objects.equals(activePanel, keyspaceProperties.getPanel().get("viewEdit"))) {
                    model.addAttribute("keyspaceContent", keyspaceService.getKeyspaceContent(userKeyspace.getKeyspace().getName().toLowerCase()));
                }
                if (Objects.equals(activePanel, keyspaceProperties.getPanel().get("viewEdit"))) {
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
            update.getContent().forEach(p -> {
                for (ColumnDefinitions.Definition definition : update.getColumnDefinitions()) {
                    if (p.get(definition.getName()) != null) {
                        if (definition.getType().getName().toString().equals("timestamp")) {
                            p.put(definition.getName(), new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(p.get(definition.getName())));
                        }
                    }
                }
//                Map<String,String> rowData = new HashMap<>();
//                rowData.put("test", "dada");
//                p.put("DT_RowData", rowData);
            });

            map.put("data", update.getContent().stream().map(Map::values).collect(Collectors.toList()));
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

    // find the corespondent of session userKeyspace into auth
//    private UserKeyspace getUserKeyspaceFromContext(Authentication authentication, HttpSession session) {
//        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
//        User user = userDetails.getUser();
//        UserKeyspace activeUserKeyspace = (UserKeyspace) session.getAttribute("userKeyspace");
//        Optional<UserKeyspace> userKeyspaceOpt = user.getKeyspaces().stream().filter(p -> Objects.equals(p.getCreatorName(), activeUserKeyspace.getCreatorName()) && Objects.equals(p.getName(), activeUserKeyspace.getName())).findFirst();
//        return userKeyspaceOpt.orElse(null);
//    }
}
