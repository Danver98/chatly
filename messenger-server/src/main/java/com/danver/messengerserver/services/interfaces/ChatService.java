package com.danver.messengerserver.services.interfaces;

import com.danver.messengerserver.models.*;

import java.time.Instant;
import java.util.List;

public interface ChatService {

     /**
      *
      * @param chat - chat object
      * @return newly created chat, null if error occurred
      */
     Chat createChat(Chat chat);

     Chat createChat(Chat chat, User author);

     List<Chat> getChats(ChatPagingDTO dto);

     default List<Chat> getChatsLight(ChatPagingDTO dto) {
          return this.getChats(dto);
     }

     Chat getChat(long id, long userId);

     List<User> getParticipants(long id);

     void updateChat(Chat chat);

     default void updateLastReadMsg(long chatId, long userId, String messageId) {

     }

     default void updateLastReadMsgForDeleted(List<Message> messages) {

     };

     void deleteChat(long id);


     /**
      * Check whether chat with given users exists
      * @return existing chat or null
      */
     Chat exists(Long[] userIds);

     void addParticipants(long chatId, long[] users);

     Chat getAllUsersChat();

     void deleteParticipants(long id, long[] userId);

     String generateChatInvitationLink(long id, User user, String baseUrl);

     ChatRequestDTO getJoinChatInfo(User user, String encryptedToken);
}
