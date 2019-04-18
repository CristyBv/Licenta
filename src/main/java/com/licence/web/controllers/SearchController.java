package com.licence.web.controllers;

import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class SearchController {

    @GetMapping(value = "${route.searchUserLive}", produces = "application/json")
    public Map<String,List<Map<String,String>>> searchUserLive(@RequestParam("search") String search) {
        System.out.println(search);
        Map<String,List<Map<String,String>>> result = new HashMap<>();
        List<Map<String,String>> options = new ArrayList<>();
        options.add(new HashMap<>());
        options.add(new HashMap<>());
        options.get(0).put("id","1");
        options.get(0).put("text","text123");
        options.get(1).put("id","2");
        options.get(1).put("text","text12345");
        result.put("results", options);
        JSONObject obj = new JSONObject(result);
        System.out.println(obj.toString());
        return result;
    }
}
