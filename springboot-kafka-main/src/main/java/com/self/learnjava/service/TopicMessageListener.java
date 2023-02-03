package com.self.learnjava.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.self.learnjava.messaging.LoginMessage;
import com.self.learnjava.messaging.RegistrationMessage;

@Component
public class TopicMessageListener {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	ObjectMapper objectMapper;
	
	@KafkaListener(topics="topic_registration", groupId="group1")
	public void onRegistrationMessage(@Payload String message, @Header("type")String type) throws JsonMappingException, JsonProcessingException {
		RegistrationMessage msg = objectMapper.readValue(message, getType(type));
		logger.info("received registration message: {}", msg);
	}
	
	@KafkaListener(topics="topic_login", groupId="group1")
	public void onLoginMessage(@Payload String message, @Header("type")String type) throws JsonMappingException, JsonProcessingException {
		LoginMessage msg = objectMapper.readValue(message, getType(type));
		logger.info("received login message: {}", msg);
	}
	
	@KafkaListener(topics="topic_login", groupId="group2")
	public void processLoginMessage(@Payload String message, @Header("type")String type) throws JsonMappingException, JsonProcessingException {
		LoginMessage msg = objectMapper.readValue(message, getType(type));
		logger.info("process login message: {}", msg);
	}
	
	@SuppressWarnings("unchecked")
	private static <T> Class<T> getType(String type){
		try {
			return (Class<T>)Class.forName(type);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
