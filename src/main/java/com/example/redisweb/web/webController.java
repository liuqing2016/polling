package com.example.redisweb.web;

import com.example.redisweb.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class webController {

    @Autowired
    GroupService groupService;

    @GetMapping("/")
    public @ResponseBody Object index(){
        return "ok";
    }

    @GetMapping("/polling")
    public @ResponseBody Object polling() throws Exception {
        return groupService.polling();
    }

    @GetMapping("/addGroup")
    public @ResponseBody Object add(){
        return groupService.addGroup();
    }

    @GetMapping("/delGroup")
    public @ResponseBody Object del(){
        return groupService.delGroup();
    }

    @GetMapping("/groups")
    public @ResponseBody Object groups(){
        return groupService.getGroup();
    }

    @GetMapping("/show")
    public @ResponseBody Object show(){
        return groupService.show();
    }

    @GetMapping("/clear")
    public @ResponseBody Object clear(){
        return groupService.clear();
    }

}
