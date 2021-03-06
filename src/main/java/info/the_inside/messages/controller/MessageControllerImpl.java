package info.the_inside.messages.controller;

import static info.the_inside.messages.controller.ControllerConstants.ROOT_URL;
import static info.the_inside.messages.exception.ErrorMessages.INVALID_TOKEN_EX;
import static java.lang.Integer.parseInt;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import info.the_inside.messages.dto.MessageDto;
import info.the_inside.messages.dto.MessageHistoryResponse;
import info.the_inside.messages.dto.MessageMapper;
import info.the_inside.messages.dto.MessageRequest;
import info.the_inside.messages.model.Message;
import info.the_inside.messages.model.Sender;
import info.the_inside.messages.service.AuthService;
import info.the_inside.messages.service.MessageService;
import info.the_inside.messages.service.SenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.security.auth.login.AccountNotFoundException;
import javax.security.auth.message.AuthException;
import javax.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping(ROOT_URL)
public class MessageControllerImpl implements MessageController {

    private static final String MESSAGE_URL = "/message";

    private static final String REGEX = "^history \\d+$";
    private static final Pattern PATTERN = Pattern.compile(REGEX, CASE_INSENSITIVE);

    private final MessageService messageService;
    private final SenderService senderService;
    private final AuthService authService;
    private final MessageMapper messageMapper;

    @Autowired
    public MessageControllerImpl(MessageService messageService,
                                 SenderService senderService,
                                 AuthService authService,
                                 MessageMapper messageMapper) {
        this.messageService = messageService;
        this.senderService = senderService;
        this.authService = authService;
        this.messageMapper = messageMapper;
    }

    @RequestMapping(value = MESSAGE_URL, consumes = APPLICATION_JSON_VALUE)
    // MessageRequest consists of name and message fields
    public ResponseEntity<MessageHistoryResponse> handleMessage(@RequestBody MessageRequest request,
                                                                HttpServletRequest httpServletRequest)
            throws AccountNotFoundException, AuthException {
        String message = request.getMessage();
        // Fetch sender from db by name, if not found - throw ex
        Sender sender = senderService.getSenderByName(request.getName());
        // If token is valid, then proceed
        if ( authService.validateBearerToken(httpServletRequest.getHeader("Authorization"), sender) ) {
            // Check if "message" field conforms to pattern "history XX"
            if ( PATTERN.matcher(message).matches() ) {
                int historySize = parseInt(message.substring(8));
                // Fetch N messages from repo, where N is historySize, and convert them to DTO
                List<MessageDto> messages = convertMessagesToDtos( messageService.getLatestMessages(historySize) );

                // Return JSON with messages
                return ResponseEntity.status(OK)
                        .contentType(APPLICATION_JSON)
                        .body( new MessageHistoryResponse(messages) );
            } else {
                // If "message" doesn't conform, then save the message
                messageService.createNewMessage(message, request.getName());

                return ResponseEntity.status(CREATED)
                        .build();
            }
        }

        // If token is invalid, throw ex to be caught by handlers
        throw new AuthException(INVALID_TOKEN_EX);
    }

    private List<MessageDto> convertMessagesToDtos(List<Message> messages) {
        return messages.stream()
                .map(messageMapper::messageToDto)
                .collect(toList());
    }

}
