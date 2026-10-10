package com.danver.messengerserver.services.implementations;

import com.danver.messengerserver.auth.JwtUtil;
import com.danver.messengerserver.models.*;
import com.danver.messengerserver.repositories.interfaces.ChatRepository;
import com.danver.messengerserver.repositories.interfaces.MessageRepository;
import com.danver.messengerserver.services.interfaces.ChatService;
import com.danver.messengerserver.services.interfaces.UserService;
import com.danver.messengerserver.services.permission.PermissionService;
import com.danver.messengerserver.services.permission.PermissionType;
import com.danver.messengerserver.services.permission.ResourceType;
import com.danver.messengerserver.utils.Encryption;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.*;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final PermissionService permissionService;
    private final MessageRepository messageRepository;
    private final Environment env;
    private final Encryption encryption;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Autowired
    public ChatServiceImpl(ChatRepository chatRepository, PermissionService permissionService, MessageRepository messageRepository, Environment env, Encryption encryption, JwtUtil jwtUtil,
                           UserService userService) {
        this.chatRepository = chatRepository;
        this.permissionService = permissionService;
        this.messageRepository = messageRepository;
        this.env = env;
        this.encryption = encryption;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    public Chat createChat(Chat chat) {
        Chat newChat = chatRepository.getOrCreate(chat);
        List<Long> participants = chat.getParticipants();
        if (participants != null && !participants.isEmpty()) {
            permissionService.grantAuthority(participants, newChat.getId(), ResourceType.CHAT.getValue(),
                    PermissionType.Chat.DEFAULT.getValue());
        }
        return newChat;
    }

    @Override
    @Transactional
    public Chat createChat(Chat chat, User author) {
        chat.setAuthorId(author.getId());
        Chat newChat = chatRepository.getOrCreate(chat);
        List<Long> participants = chat.getParticipants();
        if (participants == null || participants.isEmpty()) {
            return newChat;
        }
        participants.remove(author.getId());
        // Chat creator is assigned admin rights
        permissionService.grantAuthority(author.getId(), newChat.getId(), ResourceType.CHAT.getValue(),
                PermissionType.Chat.ADMIN.getValue());
        if (participants.isEmpty()) {
            return newChat;
        }
        // Other participants get default rights
        permissionService.grantAuthority(participants, newChat.getId(), ResourceType.CHAT.getValue(),
                PermissionType.Chat.DEFAULT.getValue());

        boolean canAddUsers = chat.isCanAddUsers();
        if (canAddUsers) {
            // grant authorities to add participants
            permissionService.grantAuthority(participants, chat.getId(), ResourceType.CHAT.getValue(),
                    PermissionType.Chat.User.ADD.getValue());
        }
        return newChat;
    }

    @Override
    public List<Chat> getChats(ChatPagingDTO dto) {
        //return chatRepository.getChatsWithParticipants(userId);

        return chatRepository.getChats(dto.getUserId(), dto.getTime(), dto.getChatId(),
                dto.getDirection().ordinal(), dto.getCount());
    }

    @Override
    public List<Chat> getChatsLight(ChatPagingDTO dto) {
        return chatRepository.getChatsLight(dto.getUserId());
    }

    @Override
    public Chat getChat(long id, long userId) {
        return chatRepository.getChat(id, userId);
    }

    @Override
    public List<User> getParticipants(long id) {
        return chatRepository.getParticipants(id);
    }

    @Override
    @Transactional
    public void updateChat(Chat chat) {
        Chat oldChat = chatRepository.getChat(chat.getId(), null);
        chatRepository.updateChat(chat);
        boolean couldAddUsers = oldChat.isCanAddUsers();
        boolean canAddUsers = chat.isCanAddUsers();
        if (canAddUsers && !couldAddUsers) {
            // grant authorities to add participants
            List<Long> participants = chatRepository.getParticipants(chat.getId()).stream().map(User::getId).toList();
            permissionService.grantAuthority(participants, chat.getId(), ResourceType.CHAT.getValue(),
                    PermissionType.Chat.User.ADD.getValue());
        } else if (!canAddUsers && couldAddUsers) {
            // revoke permission
            List<Long> participants = chatRepository.getParticipants(chat.getId()).stream().map(User::getId).toList();
            permissionService.revokeAuthority(participants, chat.getId(), ResourceType.CHAT.getValue(),
                    PermissionType.Chat.User.ADD.getValue());
        }
    }

    @Override
    public void updateLastReadMsg(long chatId, long userId, String messageId) {
        chatRepository.updateLastReadMsg(chatId, userId, messageId);
    }

    @Override
    public void updateLastReadMsgForDeleted(List<Message> messages) {
        chatRepository.updateLastReadMsgForDeleted(messages);
    }

    @Override
    @Transactional
    public void deleteChat(long id) {
        // TODO: revoke permissions for users in this chat
        //permissionService.revokeAuthority();
        List<User> participants = chatRepository.getParticipants(id);
        long[] userIds = participants.stream().mapToLong(User::getId).toArray();
        permissionService.revokeAuthority(userIds, id, ResourceType.CHAT.getValue());
        chatRepository.deleteChat(id);
    }

    @Override
    public Chat exists(Long[] userIds) {
        return chatRepository.exists(userIds);
    }

    @Override
    @Transactional
    public void addParticipants(long chatId, long[] users) {
        MessageRequestDTO dto = MessageRequestDTO.builder()
                .chatId(chatId)
                .build();
        Message lastMessage = messageRepository.getLastMessage(dto);
        this.chatRepository.addParticipants(chatId, users, lastMessage.getId());
        Chat chat = chatRepository.getChat(chatId, null);
        List<String> permissions =new ArrayList<>(List.of(PermissionType.Chat.DEFAULT.getValue()));
        if (chat.isCanAddUsers()) {
            permissions.add(PermissionType.Chat.User.ADD.getValue());
        }
        permissionService.grantAuthority(users, chatId, ResourceType.CHAT.getValue(), permissions);
    }

    @Override
    public Chat getAllUsersChat() {
        return this.chatRepository.getAllUsersChat();
    }

    @Override
    @Transactional
    public void deleteParticipants(long chatId, long[] userIds) {
        this.chatRepository.deleteParticipants(chatId, userIds);
        this.permissionService.revokeAuthority(userIds, chatId, ResourceType.CHAT.getValue());
    }

    @Override
    public String generateChatInvitationLink(long id, User user, String baseUrl) {
        List<String> permissions = permissionService.getPermissions(user, id, ResourceType.CHAT.getValue());
        if (!permissions.contains(PermissionType.Chat.ADMIN.getValue()) ||
                !permissions.contains(PermissionType.Chat.User.ADD.getValue())) {
            throw new AccessDeniedException("Insufficient permissions for this operation");
        }
        Map<String, Object> claims = new HashMap<>();
        String issuer = env.getProperty("jwt.chat-invitation.iss");
        Long expirationMillis = Long.valueOf(Objects.requireNonNull(env.getProperty("jwt.chat-invitation.exp-in-millis")));
        String subject = String.valueOf(user.getId());
        claims.put("chatId", id);
        String token = jwtUtil.generateToken(claims, issuer, subject, expirationMillis,
                Objects.requireNonNull(env.getProperty("jwt.chat-invitation.secret")));
        String encryptedToken;
        try {
            encryptedToken = encryption.encrypt(token);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't encrypt invitation link");
        }
        Chat chat = chatRepository.getChat(id, null);
        return baseUrl + "?token=" + encryptedToken + "&chatName=" + chat.getName();
    }

    @Override
    public ChatRequestDTO getJoinChatInfo(User user, String encryptedToken) {
        String token;
        try {
            token = encryption.decrypt(encryptedToken);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't decrypt invitation link");
        }
        JwtUtil jwtUtil = new JwtUtil(env, "jwt.chat-invitation.secret");
        boolean valid = jwtUtil.validateToken(token);
        if (!valid) {
            throw new AccessDeniedException("Chat invitation link is invalid");
        }

        Claims claims = jwtUtil.getClaims(token);
        long chatId = claims.get("chatId", Long.class);
        long linkAuthorId = Long.parseLong(claims.getSubject());
        User author = userService.getUser(linkAuthorId);
        // Check whether author of the link has right to generate it
        List<String> permissions = permissionService.getPermissions(author, chatId, ResourceType.CHAT.getValue());
        if (!permissions.contains(PermissionType.Chat.ADMIN.getValue()) ||
                !permissions.contains(PermissionType.Chat.User.ADD.getValue())) {
            throw new AccessDeniedException("Invitation link author doesn't have enough permissions to create invitation link");
        }

        return ChatRequestDTO.builder()
                .chatId(chatId)
                .users(new long[]{user.getId()})
                .build();
    }
}
