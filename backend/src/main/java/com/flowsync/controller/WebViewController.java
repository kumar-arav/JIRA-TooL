package com.flowsync.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class WebViewController {
    @RequestMapping(value = { 
        "/{path:[^\\.]*}", 
        "/projects/**", 
        "/tickets/**", 
        "/sprints/**", 
        "/kanban/**", 
        "/notifications/**", 
        "/dashboard/**", 
        "/ai/**" 
    })
    public String redirect() {
        return "forward:/index.html";
    }
}
