package com.itranswarp.learnjava.web;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itranswarp.learnjava.entity.User;
import com.itranswarp.learnjava.service.RedisService;
import com.itranswarp.learnjava.service.UserService;

@Controller
public class UserController {

    public static final String KEY_USER_ID = "__userid__";

    public static final String KEY_USERS = "__users__";

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserService userService;

    @Autowired
    RedisService redisService;

    @ExceptionHandler(RuntimeException.class)
    public ModelAndView handleUnknowException(Exception ex) {
        return new ModelAndView("500.html", Map.of("error", ex.getClass().getSimpleName(), "message", ex.getMessage()));
    }

    private void putUserIntoRedis(User user) throws Exception {
        redisService.hset(KEY_USERS, user.getId().toString(), objectMapper.writeValueAsString(user));
    }

    private User getUserFromRedis(HttpSession session) throws Exception {
        Long id = (Long) session.getAttribute(KEY_USER_ID);
        if (id != null) {
            String s = redisService.hget(KEY_USERS, id.toString());
            if (s != null) {
                return objectMapper.readValue(s, User.class);
            }
        }
        return null;
    }

    @GetMapping("/")
    public ModelAndView index(HttpSession session) throws Exception {
        User user = getUserFromRedis(session);
        Map<String, Object> model = new HashMap<>();
        if (user != null) {
            model.put("user", user);
        }
        return new ModelAndView("index.html", model);
    }

    @GetMapping("/register")
    public ModelAndView register() {
        return new ModelAndView("register.html");
    }

    @PostMapping("/register")
    public ModelAndView doRegister(@RequestParam("email") String email, @RequestParam("password") String password, @RequestParam("name") String name) {
        try {
            User user = userService.register(email, password, name);
            logger.info("user registered: {}", user.getEmail());
        } catch (RuntimeException e) {
            return new ModelAndView("register.html", Map.of("email", email, "error", "Register failed"));
        }
        return new ModelAndView("redirect:/signin");
    }

    @GetMapping("/signin")
    public ModelAndView signin(HttpSession session) throws Exception {
        User user = getUserFromRedis(session);
        if (user != null) {
            return new ModelAndView("redirect:/profile");
        }
        return new ModelAndView("signin.html");
    }

    @PostMapping("/signin")
    public ModelAndView doSignin(@RequestParam("email") String email, @RequestParam("password") String password, HttpSession session) throws Exception {
        try {
            User user = userService.signin(email, password);
            session.setAttribute(KEY_USER_ID, user.getId());
            putUserIntoRedis(user);
        } catch (RuntimeException e) {
            return new ModelAndView("signin.html", Map.of("email", email, "error", "Signin failed"));
        }
        return new ModelAndView("redirect:/profile");
    }

    @GetMapping("/profile")
    public ModelAndView profile(HttpSession session) throws Exception {
        User user = getUserFromRedis(session);
        if (user == null) {
            return new ModelAndView("redirect:/signin");
        }
        return new ModelAndView("profile.html", Map.of("user", user));
    }

    @GetMapping("/signout")
    public String signout(HttpSession session) {
        session.removeAttribute(KEY_USER_ID);
        return "redirect:/signin";
    }

    @GetMapping("/resetPassword")
    public ModelAndView resetPassword() {
        throw new UnsupportedOperationException("Not supported yet!");
    }
}
