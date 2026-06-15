package com.demo.controller;

import com.demo.service.UserService;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/**
 * File 1/6 - Entry point (taint source)
 * Spring @RequestParam is a known taint source for both tools
 */
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/user")
    public String getUser(@RequestParam String id) {
        return userService.findUser(id);
    }

    @GetMapping("/report")
    public String getReport(@RequestParam String name, HttpServletRequest request) {
        String token = request.getHeader("X-Auth-Token");
        return userService.generateReport(name, token);
    }
}
