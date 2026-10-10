package com.danver.messengerserver.controllers;

import com.danver.messengerserver.exceptions.StorageException;
import com.danver.messengerserver.models.*;
import com.danver.messengerserver.services.interfaces.ChatService;
import com.danver.messengerserver.services.interfaces.MessageService;
import com.danver.messengerserver.services.interfaces.StorageService;
import com.danver.messengerserver.services.interfaces.UserService;
import com.danver.messengerserver.services.permission.PermissionService;
import com.danver.messengerserver.services.permission.ResourceType;
import com.danver.messengerserver.utils.Constants;
import com.danver.messengerserver.utils.FileStorageOptions;
import com.danver.messengerserver.utils.FileUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chats")
public class ChatController {
    private final ChatService chatService;
    private final MessageService messageService;
    private final StorageService storageService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    private final PermissionService permissionService;

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class.getName());

    @Autowired
    public ChatController(ChatService chatService, MessageService messageService, @Qualifier("s3Storage") StorageService storageService,
                          UserService userService, SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper,
                          PermissionService permissionService) {
        this.chatService = chatService;
        this.messageService = messageService;
        this.storageService = storageService;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.permissionService = permissionService;
    }

    @PostMapping("/")
    List<Chat> list(@RequestBody ChatPagingDTO dto) {
        return chatService.getChats(dto);
    }

    @PostMapping("/light-list")
    List<Chat> lightList(@RequestBody ChatPagingDTO dto) {
        return chatService.getChatsLight(dto);
    }

    @GetMapping("/{id}")
    Chat getChat(@RequestParam long userId, @PathVariable long id) {
        return chatService.getChat(id, userId);
    }

    @GetMapping("/{id}/users/permissions")
    List<String> getPermissions(@PathVariable("id") long chatId, @AuthenticationPrincipal UserDetails userDetails) {
        return permissionService.getPermissions(userDetails, chatId, ResourceType.CHAT.getValue());
    }

    @PostMapping("/create")
    Chat createChat(@RequestBody Chat chat, @AuthenticationPrincipal UserDetails userDetails) {
        Chat newChat = chatService.createChat(chat, (User) userDetails);
        List<Long> participants = chat.getParticipants();
        if (participants == null || participants.isEmpty()) {
            return newChat;
        }
        if (chat.isPrivate() || newChat.isPrivate()) {
            // We don't notify private chat creation, first sent message will do that
            return newChat;
        }
        notifyChatIsCreated(chat, (User) userDetails, newChat, participants);
        return newChat;
    }

    private void notifyChatIsCreated(Chat chat, User author, Chat newChat, List<Long> participants) {
        String privateDestination = Constants.MESSAGE_BROKER_QUEUE_PREFIX + "/chats/messages";
        MessageData messageData = MessageData.builder()
                .type(MessageDataType.DEFAULT)
                .value("%s created chat \"%s\"".formatted(author.getFullName(), chat.getName()))
                .build();
        Message messageDraft = Message.builder()
                .type(Message.MessageType.CREATION)
                .chatId(newChat.getId())
                .data(messageData)
                .author(author)
                // We are suffering problems when setting receiverId
                //.receiverId(newParticipant.getId())
                .build();
        Message message = this.messageService.createMessage(messageDraft);
        MessageDTO messageDTO = MessageDTO.builder()
                .message(message)
                .chat(chat)
                .chatName(chat.getName())
                .chatIsPrivate(chat.isPrivate())
                .build();

        for (Long participant : participants) {
            messagingTemplate.convertAndSendToUser(
                    Long.toString(participant),
                    privateDestination,
                    messageDTO
            );
        }
    }

    @PutMapping("/{id}")
    void updateChat( @RequestBody Chat chat) {
        chatService.updateChat(chat);
    }

    @PatchMapping("/{id}")
    void updateChatPatch(@PathVariable long id, @RequestBody Chat chat) {
        chatService.updateChat(chat);
    }

    @PatchMapping("/{id}/last-read-msg")
    void updateLastReadMsg(@RequestBody ChatRequestDTO dto,
                           @PathVariable("id") String id) {
        //@RequestBody long chatId, @RequestBody long userId, @RequestBody String messageId
        chatService.updateLastReadMsg(dto.getChatId(), dto.getUserId(), dto.getMessageId());
    }

    @DeleteMapping("/{id}")
    void deleteChat(@PathVariable long id) {
        chatService.deleteChat(id);
    }

    @GetMapping("/{id}/participants")
    List<User> getListOfParticipants(@RequestParam long userId, @PathVariable long id) {
        return chatService.getParticipants(id);
    }

    @PostMapping("/add")
    ResponseEntity<?> addParticipants(@RequestBody ChatRequestDTO dto, @AuthenticationPrincipal UserDetails userDetails) {
        String joinToken = dto.getJoinToken();
        if (joinToken != null) {
            // User joins chat himself
            dto = chatService.getJoinChatInfo((User) userDetails, joinToken);
        }
        List<User> oldParticipants = chatService.getParticipants(dto.getChatId());
        Set<Long> oldParticipantsIds = oldParticipants.stream().map(User::getId).collect(Collectors.toUnmodifiableSet());
        this.chatService.addParticipants(dto.getChatId(), dto.getUsers());
        if (dto.getUsers() == null) {
            return ResponseEntity.ok().build();
        }
        String privateDestination = Constants.MESSAGE_BROKER_QUEUE_PREFIX + "/chats/messages";
        UserRequestFilter userFilter = UserRequestFilter.builder()
                .ids(dto.getUsers())
                .build();
        UserRequestDTO userDto = UserRequestDTO.builder()
                .filter(userFilter)
                .build();
        List<User> newParticipants = userService.list(userDto);
        User user = (User) userDetails;
        Chat chat = chatService.getChat(dto.getChatId(), user.getId());
        Message.MessageType messageType = joinToken != null ? Message.MessageType.JOIN : Message.MessageType.INVITATION;

        // Notify newly added users
        for (User newParticipant : newParticipants) {
            if (oldParticipantsIds.contains(newParticipant.getId())) {
                continue;
            }
            String messageValue = joinToken != null ? "%s joined the chat via invitation link".formatted(user.getFullName())
                    : "%s added %s to chat".formatted(user.getFullName(), newParticipant.getFullName());
            MessageData messageData = MessageData.builder()
                    .type(MessageDataType.DEFAULT)
                    .value(messageValue)
                    .build();
            Message messageDraft = Message.builder()
                    .type(messageType)
                    .chatId(dto.getChatId())
                    .data(messageData)
                    .author(user)
                    .build();
            Message message = this.messageService.createMessage(messageDraft);
            MessageDTO messageDTO = MessageDTO.builder()
                    .message(message)
                    .chat(chat)
                    .chatName(chat.getName())
                    .chatIsPrivate(chat.isPrivate())
                    .build();
            messagingTemplate.convertAndSendToUser(
                    Long.toString(newParticipant.getId()),
                    privateDestination,
                    messageDTO
            );
            // Now notify users already present in the chat
            // TODO:  We suppose newly added users are not subscribed to this queue yet, so they won't get duplicated messages
            messagingTemplate.convertAndSend(
                    Constants.MESSAGE_BROKER_TOPIC_PREFIX + "/chats/" + dto.getChatId() + "/messages",
                    messageDTO
            );
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/users")
    ResponseEntity<?> deleteParticipants(@PathVariable long id, @RequestParam long[] userId, @AuthenticationPrincipal UserDetails userDetails) {
        this.chatService.deleteParticipants(id, userId);
        UserRequestFilter userFilter = UserRequestFilter.builder()
                .ids(userId)
                .build();
        UserRequestDTO userDto = UserRequestDTO.builder()
                .filter(userFilter)
                .build();
        List<User> oldParticipants = userService.list(userDto);

        User user = (User) userDetails;
        Chat chat = chatService.getChat(id, user.getId());

        // Notify participants and deleted users
        for (User participant : oldParticipants) {
            boolean leaveYourself = userId.length == 1 && participant.getId() == user.getId();
            String deleteMsg = leaveYourself ?
                    "%s left the chat".formatted(user.getFullName())
                    : "%s excluded %s from the chat".formatted(user.getFullName(), participant.getFullName());
            Message.MessageType messageType = leaveYourself ?
                    Message.MessageType.LEAVE
                    : Message.MessageType.EXCLUDE;

            MessageData messageData = MessageData.builder()
                    .type(MessageDataType.DEFAULT)
                    .value(deleteMsg)
                    .build();
            Message messageDraft = Message.builder()
                    .type(messageType)
                    .chatId(id)
                    .data(messageData)
                    .author(user)
                    .receiverId(participant.getId())
                    .build();
            Message message = this.messageService.createMessage(messageDraft);
            MessageDTO messageDTO = MessageDTO.builder()
                    .message(message)
                    .chat(chat)
                    .chatName(chat.getName())
                    .chatIsPrivate(chat.isPrivate())
                    .build();
            // Now notify users already present in the chat
            // TODO:  We suppose newly added users are not subscribed to this queue yet, so they won't get duplicated messages
            messagingTemplate.convertAndSend(
                    Constants.MESSAGE_BROKER_TOPIC_PREFIX + "/chats/" + id + "/messages",
                    messageDTO
            );
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/invitation-link")
    String getChatInvitationLink(@PathVariable long id, @AuthenticationPrincipal UserDetails userDetails,
                                 HttpServletRequest request) {
        String url = request.getRequestURL().toString();
        return chatService.generateChatInvitationLink(id, (User) userDetails, url);
    }

    @PostMapping("/{id}/invitation-link")
    ResponseEntity<?> joinChat(@PathVariable long id, @RequestParam String token, @RequestParam(required = false) String chatName,
                               @AuthenticationPrincipal UserDetails userDetails) {
        ChatRequestDTO dto = ChatRequestDTO.builder()
                .joinToken(token)
                .build();
        this.addParticipants(dto, userDetails);
        return ResponseEntity.ok().build();
    }

    /**
     * Method for uploading attachments. Used in conjunction with sendMessage/sendMessage private.
     * It's basically adjusted for uploading images only and needs further development to support other formats
     *
     * @param file
     * @return url of a created resource or null if failed
     */
    @PostMapping("/attachment")
    ResponseEntity<?> createAttachment(@RequestParam MultipartFile file,
                                       @RequestParam Long userId,
                                       @RequestParam Long chatId) {
        if (FileUtils.isImage(file) || FileUtils.isAudio(file) || FileUtils.isVideo(file) ||
                FileUtils.isValid(file)) {
            try {
                String path = "chats/" + chatId + "/attachments";
                FileStorageOptions options = FileStorageOptions
                        .builder()
                        .owner(userId)
                        .path(path)
                        .build();
                String url = storageService.store(file, options);
                return new ResponseEntity<>(url, HttpStatus.OK);
            } catch (StorageException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        }
        return null;
    }

    /*
        Former MessageController
    */

    @PostMapping("/{id}/messages")
    ResponseEntity<List<Message>> getMessagesPaged(@PathVariable long id, @RequestBody MessageRequestDTO dto) {
        List<Message> messages = messageService.getMessages(dto);
        return new ResponseEntity<>(messages, HttpStatus.OK);
    }

    @PostMapping("/{id}/messages/create")
    ResponseEntity<?> createMessageHTTP(@PathVariable long id, @RequestBody Message dto) {
        try {
            Message message = messageService.createMessage(dto);
            return new ResponseEntity<>(message, HttpStatus.OK);
        } catch (Exception ignored) {

        }
        return null;
    }

    @MessageMapping("/chats/private/send-message")
    ResponseEntity<?> sendMessagePrivate(@Payload String messageDTO) {
        try {
            MessageDTO dto = objectMapper.readValue(messageDTO, MessageDTO.class);
            Message message = messageService.createMessage(dto.getMessage());
            dto.setMessage(message);
            String destination = Constants.MESSAGE_BROKER_QUEUE_PREFIX + "/chats/messages";
            messagingTemplate.convertAndSendToUser(
                    Long.toString(dto.getMessage().getReceiverId()),
                    destination,
                    dto
            );
            // Send back message to the sender
            messagingTemplate.convertAndSendToUser(
                    Long.toString(dto.getMessage().getAuthor().getId()),
                    destination,
                    dto
            );
            return new ResponseEntity<>(dto, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @MessageMapping("/chats/public/send-message")
    ResponseEntity<?> sendMessage(@Payload String messageDTO) {
        try {
            MessageDTO dto = objectMapper.readValue(messageDTO, MessageDTO.class);
            Message message = messageService.createMessage(dto.getMessage());
            dto.setMessage(message);
            messagingTemplate.convertAndSend(
                    Constants.MESSAGE_BROKER_TOPIC_PREFIX + "/chats/" + dto.getMessage().getChatId() + "/messages",
                    dto
            );
            return new ResponseEntity<>(dto, HttpStatus.OK);
        } catch (JacksonException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/{id}/messages/delete")
    ResponseEntity<?> deleteMessage(@PathVariable("id") Long id, @AuthenticationPrincipal UserDetails userDetails,
                                    @RequestParam(name="id") String[] messageIds) {
        this.messageService.deleteMessages(((User) userDetails).getId(), messageIds);
        // Notify chat users about deleted message
        User user = (User) userDetails;
        Chat chat = chatService.getChat(id, user.getId());
        Message.MessageType messageType = Message.MessageType.DELETE;
        MessageData messageData = MessageData.builder()
                .type(MessageDataType.ARRAY)
                .value(messageIds)
                .build();
        Message messageDraft = Message.builder()
                .type(messageType)
                .chatId(id)
                .data(messageData)
                .author(user)
                .build();
        MessageDTO messageDTO = MessageDTO.builder()
                .message(messageDraft)
                .chat(chat)
                .chatName(chat.getName())
                .chatIsPrivate(chat.isPrivate())
                .build();
        var response = ResponseEntity.noContent().build();
        if (chat.isPrivate()) {
            List<User> participants = this.chatService.getParticipants(id);
            String destination = Constants.MESSAGE_BROKER_QUEUE_PREFIX + "/chats/messages";
            for (User participant: participants) {
                // We send message to both participants of given private chat
                messagingTemplate.convertAndSendToUser(
                        Long.toString(participant.getId()),
                        destination,
                        messageDTO
                );
            }
            return response;
        }
        messagingTemplate.convertAndSend(
                Constants.MESSAGE_BROKER_TOPIC_PREFIX + "/chats/" + id + "/messages",
                messageDTO
        );
        return response;
    }
}
