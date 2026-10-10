package com.danver.messengerserver.services.interfaces;


import com.danver.messengerserver.models.Message;
import com.danver.messengerserver.models.MessageRequestDTO;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

public interface MessageService {


    List<Message> getMessages(MessageRequestDTO dto);

    Message getLastMessage(MessageRequestDTO dto);

    /**
     *
     * @return message with updated absent fields
     */
    Message createMessage(Message message);

    void deleteMessages(long userId, List<Message> messageIds) throws AccessDeniedException;

    void deleteMessages(long userId, String [] messageIds) throws AccessDeniedException;
}
