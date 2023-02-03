package com.self.learnjava.web;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.self.learnjava.entity.User;
import com.self.learnjava.messaging.LoginMessage;
import com.self.learnjava.messaging.RegistrationMessage;
import com.self.learnjava.service.MessagingService;
import com.self.learnjava.service.UserService;

@Controller
public class UserController {
	public static final String KEY_USER = "__user__";
	
	public static final String KEY_USERS = "__users__";
	
	public static final String KEY_USER_ID = "__userid__";
	
	final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	UserService userService;
	
	@Autowired
	ObjectMapper objectMapper;
	
	@Autowired
	MessagingService messagingService;
	
	
	@ExceptionHandler(RuntimeException.class)
	public ModelAndView handleUnknowException(Exception ex) {
		Map<String, String> info = new HashMap<String, String>();
		info.put("error", ex.getClass().getSimpleName());
		info.put("message", ex.getMessage());
		return new ModelAndView("500.html", info);
	}
	
	@GetMapping("/")
	public ModelAndView index(HttpSession session) {
		logger.info("Spring Boot提供了一个开发阶段非常有用的spring-boot-devtools，能自动检测classpath路径上文件修改并自动重启。");
		User user = (User)session.getAttribute(KEY_USER);
		Map<String, Object> model = new HashMap<>();
		if (user != null) {
			model.put("user", model);
		}
		return new ModelAndView("index.html", model);
	}
	
	@GetMapping("/register")
	public ModelAndView register() {
		return new ModelAndView("register.html");
	}
	
	//用户登录成功后，把ID放入Session，把User实例放入Redis：
	@PostMapping("/register")
	public ModelAndView doRegister(@RequestParam("email")String email, @RequestParam("password")String password,
			@RequestParam("name")String name) {
		try {
			User user = userService.register(email, password, name);
			logger.info("user registered: {}", user.getEmail());
			messagingService.sendRegistrationMessage(RegistrationMessage.of(user.getEmail(), user.getName()));
		} catch (Exception e) {
			e.printStackTrace();
			Map<String, String> info = new HashMap<String, String>();
			info.put("email", email);
			info.put("error", "Register failed");
			return new ModelAndView("register.html", info);
		}
		return new ModelAndView("redirect:/signin");
	}
	
	@GetMapping("/signin")
	public ModelAndView signin(HttpSession session) {
		User user = (User)session.getAttribute(KEY_USER);
		if (user != null) {
			return new ModelAndView("redirect:profile");
		}
		return new ModelAndView("signin.html");
	}
	
	@PostMapping("/signin")
	public ModelAndView doSignin(@RequestParam("email")String email, @RequestParam("password")String password, HttpSession session) throws JsonProcessingException {
		try {
			User user = userService.signin(email, password);
			messagingService.sendLoginMessage(LoginMessage.of(user.getEmail(), user.getName(), true));
			session.setAttribute(KEY_USER, user);
		} catch (Exception e) {
			messagingService.sendLoginMessage(LoginMessage.of(email, "(unknown)", false));
			e.printStackTrace();
			Map<String, String> info = new HashMap<>();
			info.put("email", email);
			info.put("error", "Signin failed");
			return new ModelAndView("signin.html", info);
		}
		return new ModelAndView("redirect:/profile");
	}
	
	@GetMapping("/profile")
	public ModelAndView profile(HttpSession session) throws JsonMappingException, JsonProcessingException {
		User user = (User)session.getAttribute(KEY_USER);
		if (user == null) {
			return new ModelAndView("redirect:/signin");
		}
		Map<String, Object> info = new HashMap<>();
		user = userService.getUserByEmail(user.getEmail());
		info.put("user", user);
		return new ModelAndView("profile.html", info);
	}
	
	@GetMapping("/signout")
	public String signout(HttpSession session) {
		session.removeAttribute(KEY_USER);
		return "redirect:/signin";
	}
	
	@GetMapping("/resetPassword")
	public ModelAndView resetPassword() {
		throw new UnsupportedOperationException("Not supported yet!");
	}
}
