package com.self.learnjava.web;

import java.time.LocalDateTime;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Order(1)
@Component
public class MvcInterceptor implements HandlerInterceptor {
	
	final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) {
		logger.info("postHandle {}.", request.getRequestURI());
		if (modelAndView != null) {
			modelAndView.addObject("user", request.getSession().getAttribute(UserController.KEY_USER));
			modelAndView.addObject("__time__", LocalDateTime.now());
		}
	}
	
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		logger.info("afterCompletion {}: exception = {}", request.getRequestURI(), ex);
	}
}
