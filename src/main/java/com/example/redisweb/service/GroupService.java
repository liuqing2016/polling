package com.example.redisweb.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GroupService {

    @Autowired
    PollingService pollingService;

    static String optionExpland = "expland";

    public static List<String> groups = new ArrayList();
    static{
        groups.add("g1");
        groups.add("g2");
        groups.add("g3");
    }

    public List<String> getGroup(){
        return groups;
    }

    public boolean addGroup(){
        groups.add("g4");
        return true;
    }

    public boolean delGroup(){
        groups.remove(1);
        return true;
    }

    public boolean clear(){
       return pollingService.clear(optionExpland);
    }

    public boolean show(){
        return pollingService.showRedis(optionExpland);
    }

    public String polling() throws Exception {
        return pollingService.getGroup(optionExpland,getGroup());
    }


}
