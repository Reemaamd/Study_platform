package com.study.study_platform.controller;


import com.study.study_platform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/create")
    public String create() {
        userService.createUser();
        return "OK saved in MongoDB";
    }
}
