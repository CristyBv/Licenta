package com.licence.web.models.pojo;

import com.licence.config.properties.QueryProperties;

import java.util.HashMap;
import java.util.Map;

public class VerifyQuery {

    private String keyspaceName;
    private QueryProperties queryProperties;

    public VerifyQuery(String keyspaceName, QueryProperties queryProperties) {
        this.keyspaceName = keyspaceName;
        this.queryProperties = queryProperties;
    }

    public Map<String, Object> detectQuery(String query) {
        query = query.trim().toLowerCase();
        String[] splitQuery = query.split("\\s+");
        Map<String, Object> mapResult = new HashMap<>();
        StringBuilder result = new StringBuilder();
        StringBuilder error = new StringBuilder("Syntax error or command unavailable!\n");
        Boolean validForQuery = false;
        try {
            String firstWord = splitQuery[0];
            if (firstWord.equals("select") || firstWord.equals("delete")) {
                if (firstWord.equals("select")) {
                    error.append(queryProperties.getSyntax().get("select"));
                    mapResult.put("type", "select");
                } else
                    error.append(queryProperties.getSyntax().get("delete"));
                for (String s : splitQuery) {
                    if (s.equals("from")) {
                        result.append(s).append(" ").append(keyspaceName).append(".");
                        validForQuery = true;
                    } else {
                        result.append(s).append(" ");
                    }
                }
            } else if (firstWord.equals("insert")) {
                error.append(queryProperties.getSyntax().get("insert"));
                for (String s : splitQuery) {
                    if (s.equals("into")) {
                        result.append(s).append(" ").append(keyspaceName).append(".");
                        validForQuery = true;
                    } else {
                        result.append(s).append(" ");
                    }
                }
            } else if (firstWord.equals("update")) {
                error.append(queryProperties.getSyntax().get("update"));
                result.append("UPDATE ").append(keyspaceName).append(".");
                validForQuery = true;
                for (int i = 1; i < splitQuery.length; i++)
                    result.append(splitQuery[i]).append(" ");
            } else if (firstWord.equals("create")) {
                result.append("CREATE ");
                String secondWord = splitQuery[1];
                if (secondWord.equals("table")) {
                    error.append(queryProperties.getSyntax().get("createTable"));
                    result.append("TABLE ");
                    if (splitQuery[2].equals("if") && splitQuery[3].equals("not") && splitQuery[4].equals("exists")) {
                        result.append("IF NOT EXISTS ").append(keyspaceName).append(".");
                        validForQuery = true;
                        for (int i = 5; i < splitQuery.length; i++)
                            result.append(splitQuery[i]).append(" ");
                    } else {
                        result.append(keyspaceName).append(".");
                        validForQuery = true;
                        for (int i = 2; i < splitQuery.length; i++)
                            result.append(splitQuery[i]).append(" ");
                    }
                } else if (secondWord.equals("index")) {
                    error.append(queryProperties.getSyntax().get("createIndex"));
                    result.append("INDEX ");
                    if (splitQuery[2].equals("if") && splitQuery[3].equals("not") && splitQuery[4].equals("exists") && splitQuery[6].equals("on")) {
                        result.append("IF NOT EXISTS ").append(splitQuery[5]).append(" ON ").append(keyspaceName).append(".");
                        validForQuery = true;
                        for (int i = 7; i < splitQuery.length; i++)
                            result.append(splitQuery[i]).append(" ");
                    } else if (splitQuery[3].equals("on")) {
                        result.append(splitQuery[2]).append(" ON ").append(keyspaceName).append(".");
                        validForQuery = true;
                        for (int i = 4; i < splitQuery.length; i++)
                            result.append(splitQuery[i]).append(" ");
                    }
                } else if (secondWord.equals("function") || (secondWord.equals("or") && splitQuery[3].equals("function"))) {
                    error.append(queryProperties.getSyntax().get("createFunction"));
                    Integer contor = 1;
                    if (splitQuery[1].equals("or") && splitQuery[2].equals("replace")) {
                        result.append("OR REPLACE ");
                        contor = 3;
                    }
                    if (splitQuery[contor].equals("function")) {
                        result.append("FUNCTION ");
                        contor++;
                    }
                    if ((contor == 2 || contor == 4) && splitQuery[contor].equals("if") && splitQuery[contor + 1].equals("not") && splitQuery[contor + 2].equals("exists")) {
                        result.append("IF NOT EXISTS ").append(keyspaceName).append(".");
                        contor = contor + 3;
                        validForQuery = true;
                        for (int i = contor; i < splitQuery.length; i++)
                            result.append(splitQuery[i]).append(" ");
                    } else if (contor == 2 || contor == 4) {
                        validForQuery = true;
                        result.append(keyspaceName).append(".");
                        for (int i = contor; i < splitQuery.length; i++)
                            result.append(splitQuery[i]).append(" ");
                    }
                } else if (secondWord.equals("materialized")) {
                    error.append(queryProperties.getSyntax().get("createMaterializedView"));
                    if (splitQuery[2].equals("view")) {
                        result.append("MATERIALIZED VIEW ");
                        if (splitQuery[3].equals("if") && splitQuery[4].equals("not") && splitQuery[5].equals("exists")) {
                            result.append("IF NOT EXISTS ").append(keyspaceName).append(".");
                            for (int i = 6; i < splitQuery.length; i++) {
                                if (splitQuery[i].equals("from")) {
                                    result.append(splitQuery[i]).append(" ").append(keyspaceName).append(".");
                                    validForQuery = true;
                                } else {
                                    result.append(splitQuery[i]).append(" ");
                                }
                            }
                        } else {
                            result.append(keyspaceName).append(".");
                            for (int i = 3; i < splitQuery.length; i++) {
                                if (splitQuery[i].equals("from")) {
                                    result.append(splitQuery[i]).append(" ").append(keyspaceName).append(".");
                                    validForQuery = true;
                                } else {
                                    result.append(splitQuery[i]).append(" ");
                                }
                            }
                        }
                    }
                } else if (secondWord.equals("trigger")) {
                    error.append(queryProperties.getSyntax().get("createTrigger"));
                    result.append("TRIGGER ");
                    Integer contor = 2;
                    if (splitQuery[contor].equals("if") && splitQuery[contor + 1].equals("not") && splitQuery[contor + 2].equals("exists")) {
                        result.append("IF NOT EXISTS ");
                        contor = contor + 3;
                    }
                    result.append(splitQuery[contor]).append(" ");
                    contor++;
                    if (splitQuery[contor].equals("on")) {
                        result.append("ON ").append(keyspaceName).append(".");
                        validForQuery = true;
                        for (int i = 4; i < splitQuery.length; i++)
                            result.append(splitQuery[i]).append(" ");
                    }
                } else if (secondWord.equals("type")) {
                    error.append(queryProperties.getSyntax().get("createType"));
                    result.append("TYPE ");
                    Integer contor = 2;
                    if (splitQuery[contor].equals("if") && splitQuery[contor + 1].equals("not") && splitQuery[contor + 2].equals("exists")) {
                        result.append("IF NOT EXISTS ").append(keyspaceName).append(".");
                        validForQuery = true;
                        for (int i = contor + 3; i < splitQuery.length; i++)
                            result.append(splitQuery[i]).append(" ");
                    } else {
                        result.append(keyspaceName).append(".");
                        validForQuery = true;
                        for (int i = contor; i < splitQuery.length; i++)
                            result.append(splitQuery[i]).append(" ");
                    }
                } else if (secondWord.equals("aggregate") || (secondWord.equals("or") && splitQuery[3].equals("aggregate"))) {
                    error.append(queryProperties.getSyntax().get("createAggregate"));
                    Integer contor = 1;
                    if (splitQuery[1].equals("or") && splitQuery[2].equals("replace")) {
                        result.append("OR REPLACE ");
                        contor = 3;
                    }
                    if (splitQuery[contor].equals("aggregate")) {
                        result.append("AGGREGATE ");
                        contor++;
                    }
                    if ((contor == 2 || contor == 4) && splitQuery[contor].equals("if") && splitQuery[contor + 1].equals("not") && splitQuery[contor + 2].equals("exists")) {
                        result.append("IF NOT EXISTS ").append(keyspaceName).append(".");
                        validForQuery = true;
                        for (int i = contor + 3; i < splitQuery.length; i++)
                            result.append(splitQuery[i]).append(" ");
                    } else if (contor == 2 || contor == 4) {
                        result.append(keyspaceName).append(".");
                        validForQuery = true;
                        for (int i = contor; i < splitQuery.length; i++)
                            result.append(splitQuery[i]).append(" ");
                    }
                }
            } else if (firstWord.equals("alter")) {
                result.append("ALTER ");
                if (splitQuery[1].equals("table") || splitQuery[1].equals("type")) {
                    if (splitQuery[1].equals("table"))
                        error.append(queryProperties.getSyntax().get("alterTable"));
                    else
                        error.append(queryProperties.getSyntax().get("createType"));
                    result.append(splitQuery[1]).append(" ").append(keyspaceName).append(".");
                    validForQuery = true;
                    for (int i = 2; i < splitQuery.length; i++)
                        result.append(splitQuery[i]).append(" ");
                } else if (splitQuery[1].equals("materialized") && splitQuery[2].equals("view")) {
                    error.append(queryProperties.getSyntax().get("alterMaterializedView"));
                    result.append("MATERIALIZED VIEW ").append(keyspaceName).append(".");
                    validForQuery = true;
                    for (int i = 3; i < splitQuery.length; i++)
                        result.append(splitQuery[i]).append(" ");
                }
            } else if (firstWord.equals("drop")) {
                result.append("DROP ");
                if (splitQuery[1].equals("aggregate") || splitQuery[1].equals("function") || splitQuery[1].equals("index") || splitQuery[1].equals("table") || splitQuery[1].equals("type") || splitQuery[1].equals("trigger")) {
                    switch (splitQuery[1]) {
                        case "aggregate":
                            error.append(queryProperties.getSyntax().get("dropAggregate"));
                            break;
                        case "function":
                            error.append(queryProperties.getSyntax().get("dropFunction"));
                            break;
                        case "index":
                            error.append(queryProperties.getSyntax().get("dropIndex"));
                            break;
                        case "table":
                            error.append(queryProperties.getSyntax().get("dropTable"));
                            break;
                        case "type":
                            error.append(queryProperties.getSyntax().get("dropType"));
                            break;
                        case "trigger":
                            error.append(queryProperties.getSyntax().get("dropTrigger"));
                            break;
                    }
                    result.append(splitQuery[1]).append(" ");
                    Integer contor = 2;
                    if (splitQuery[contor].equals("if") && splitQuery[contor + 1].equals("exists")) {
                        result.append("IF EXISTS ");
                        contor = 4;
                    }
                    if (splitQuery[1].equals("trigger")) {
                        result.append(splitQuery[contor]).append(" ");
                        contor++;
                        if (splitQuery[contor].equals("on")) {
                            result.append("on ").append(keyspaceName).append(".");
                            validForQuery = true;
                            for (int i = contor + 1; i < splitQuery.length; i++)
                                result.append(splitQuery[i]).append(" ");
                        }
                    } else {
                        result.append(keyspaceName).append(".");
                        validForQuery = true;
                        for (int i = contor; i < splitQuery.length; i++)
                            result.append(splitQuery[i]).append(" ");
                    }
                } else if (splitQuery[1].equals("materialized") && splitQuery[2].equals("view")) {
                    error.append(queryProperties.getSyntax().get("dropMaterializedView"));
                    result.append("MATERIALIZED VIEW ");
                    Integer contor = 3;
                    if (splitQuery[contor].equals("if") && splitQuery[contor + 1].equals("exists")) {
                        result.append("IF EXISTS ");
                        contor = 5;
                    }
                    result.append(keyspaceName).append(".");
                    validForQuery = true;
                    for (int i = contor; i < splitQuery.length; i++)
                        result.append(splitQuery[i]).append(" ");
                }
            } else if (firstWord.equals("truncate")) {
                error.append(queryProperties.getSyntax().get("truncate"));
                result.append("TRUNCATE ");
                if (splitQuery[1].equals("table")) {
                    result.append("TABLE ").append(keyspaceName).append(" ");
                    validForQuery = true;
                    for (int i = 2; i < splitQuery.length; i++)
                        result.append(splitQuery[i]).append(" ");
                } else {
                    result.append(keyspaceName).append(" ");
                    validForQuery = true;
                    for (int i = 1; i < splitQuery.length; i++)
                        result.append(splitQuery[i]).append(" ");
                }
            } else if (firstWord.equals("begin")) {
                error.append(queryProperties.getSyntax().get("batch"));
                result.append("BEGIN ");
                Integer contor = 1;
                Boolean isBatch = false;
                if ((splitQuery[contor].equals("unlogged") || splitQuery[contor].equals("logged")) && splitQuery[contor + 1].equals("batch")) {
                    result.append(splitQuery[contor]).append(" BATCH");
                    contor = 3;
                    isBatch = true;
                } else if (splitQuery[contor].equals("batch")) {
                    result.append("BATCH ");
                    contor = 2;
                    isBatch = true;
                }
                if (isBatch) {
                    if (splitQuery[contor].equals("using") && splitQuery[contor+1].equals("timestamp")) {
                        result.append("USING TIMESTAMP ").append(splitQuery[contor + 2]).append(" ");
                        contor = contor + 3;
                    }
                    result.append("\n");
                    StringBuilder dmlStatements = new StringBuilder();
                    for (int i = contor; i < splitQuery.length; i++)
                        dmlStatements.append(splitQuery[i]).append(" ");
                    if (dmlStatements.length() > 0)
                        dmlStatements.delete(dmlStatements.length() - 1, dmlStatements.length());
                    String[] dmlStatementsSplit = dmlStatements.toString().split(";");
                    Integer dmlLength = dmlStatementsSplit.length;
                    String[] splitLast = dmlStatementsSplit[dmlLength - 1].trim().split("\\s+");

                    if (splitLast[0].equals("apply") && splitLast[1].equals("batch")) {
                        Boolean bathComplete = true;
                        for (int i = 0; i < dmlLength - 1; i++) {
                            Map<String, Object> map = detectQuery(dmlStatementsSplit[i]);
                            if (map.get("error") != null) {
                                bathComplete = false;
                            } else {
                                result.append(map.get("success")).append(";\n");
                            }
                        }
                        if (bathComplete) {
                            result.append("APPLY BATCH;");
                            validForQuery = true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            mapResult.put("error", error.toString());
            mapResult.put("success", result.toString());
            return mapResult;
        }
        if (validForQuery) {
            mapResult.put("success", result.toString());
            return mapResult;
        }
        mapResult.put("error", error.toString());
        mapResult.put("success", result.toString());
        return mapResult;
    }
}
