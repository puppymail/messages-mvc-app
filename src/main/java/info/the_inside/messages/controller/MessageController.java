package info.the_inside.messages.controller;

import info.the_inside.messages.dto.MessageRequest;
import org.springframework.http.ResponseEntity;

import javax.security.auth.login.AccountNotFoundException;
import javax.security.auth.message.AuthException;
import javax.servlet.http.HttpServletRequest;

public interface MessageController {

    ResponseEntity<?> handleMessage(MessageRequest request, HttpServletRequest httpServletRequest)
            throws AccountNotFoundException, AuthException;

}
