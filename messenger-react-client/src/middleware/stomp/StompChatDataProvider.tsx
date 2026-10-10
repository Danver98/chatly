import { createContext, useContext, useEffect } from "react";
import { useAuthStore } from "../../stores/authStore";
import { useStompClient } from "react-stomp-hooks";
import { StompSubscription } from "@stomp/stompjs";
import Message, { MessageType } from "../../models/Message";
import MessengerService, { ChatRequestDTO } from "../../services/MessengerService";
import Chat from "../../models/Chat";
import User from "../../models/User";
import { useBus } from 'react-bus';
import { CHATS_COMPONENT_MESSAGE_QUEUE } from "../../util/Constants";


const StompChatDataProvider = ({ children }: { children: any }) => {
  const currentUser = useAuthStore((state) => state.currentLoggedUser);
  const hasToken = useAuthStore((state) => !!state.accessToken);

  if (!hasToken || !currentUser) {
    return <>{children}</>;
  }

  return (
    <StompChatDataProviderInner currentUser={currentUser}>
      {children}
    </StompChatDataProviderInner>
  );
};

const StompChatDataProviderInner = ({ children, currentUser }: { children: any, currentUser: User | null }) => {

  const bus = useBus();;
  const stompClient = useStompClient();

  function OnPublicMessageReceived(payload: any, user: User): void {
    const dto = JSON.parse(payload.body);
    const data = dto.message;
    const chatData = dto.chat;
    const message = new Message(data.id, data.chatId, data.receiverId, data.type, data.data, data.author, data.time);
    message.replyTo = data.replyTo;
    if (data.replyTo && data.repliedMessage) {
      message.repliedMessage = new Message(data.repliedMessage.id, data.repliedMessage.chatId,
        data.repliedMessage.receiverId, data.repliedMessage.type, data.repliedMessage.data,
        data.repliedMessage.author, data.repliedMessage.time);
    }
    if (data.type === MessageType.JOIN && message.author?.id === user?.id) {
      // Current user has joined the chat. This message should have already been processed by private message handler
      // TODO: why this happens? User should only get message in his private queue,
      // but it seems he also gets it in public queue he just subscribed to
      return;
    }
    const chat = new Chat(chatData.id, chatData.name, chatData.private, chatData.avatar, chatData.time,
      chatData.participants, chatData.lastMessage, chatData.draft, chatData.unreadMsgCount, chatData.lastReadMsg,
      chatData.authorId, chatData.canAddUsers);
    if (message.type === MessageType.EXCLUDE && message.receiverId === user?.id) {
      // Current user is deleted from chat, we should unsubscribe
      stompClient?.unsubscribe(`/topic/chats/${chat.id}/messages`);
    };

    bus.emit(`/chats/${data.chatId}/messages`, {
      message: message,
      chat: chat
    });
    // send message to chats component queue
    bus.emit(CHATS_COMPONENT_MESSAGE_QUEUE, {
      message: message,
      chat: chat
    });
  }

  function OnPrivateMessageReceived(payload: any, user: User): void {
    const dto = JSON.parse(payload.body);
    const data = dto.message;
    const chat = dto.chat;
    if (data.type === MessageType.CREATION || data.type === MessageType.INVITATION || data.type === MessageType.JOIN) {
      if (chat != null && !chat.private) {
        // If given public chat, subscribe
        stompClient?.subscribe(`/topic/chats/${data.chatId}/messages`, (payload: any) => {
          OnPublicMessageReceived(payload, user);
        }, { userId: user.id.toString(), customHeader: 'CUSTOM_HEADER_VALUE' });
      }
    }
    const message = new Message(data.id, data.chatId, data.receiverId, data.type, data.data, data.author, data.time);
    message.replyTo = data.replyTo;
    if (data.replyTo && data.repliedMessage) {
      message.repliedMessage = new Message(data.repliedMessage.id, data.repliedMessage.chatId,
        data.repliedMessage.receiverId, data.repliedMessage.type, data.repliedMessage.data,
        data.repliedMessage.author, data.repliedMessage.time);
    }
    bus.emit(`/chats/${data.chatId}/messages`, {
      message: message,
      chat: chat
    });
    // send message to chats component queue
    bus.emit(CHATS_COMPONENT_MESSAGE_QUEUE, {
      message: message,
      chat: chat
    });
  }

  useEffect(() => {
    const subscriptions: (StompSubscription|undefined)[] = [];
    const subscribe = async (user: User | null) => {
      if (!user) return;
      const subId = stompClient?.subscribe(`/user/${user.id}/queue/chats/messages`, (payload: any) => {
        OnPrivateMessageReceived(payload, user);
      }, { userId: user.id.toString(), customHeader: 'CUSTOM_HEADER_VALUE' });
      subscriptions.push(subId);
      // Subscribe to public chats
      const dto: ChatRequestDTO = {
        userId: user.id
      };
      const chats = await MessengerService.getChatsByUserLight(dto);
      chats?.forEach((chat: Chat) => {
        if (chat.private) return;
        const subId = stompClient?.subscribe(`/topic/chats/${chat.id}/messages`, (payload: any) => {
          OnPublicMessageReceived(payload, user);
        }, { userId: user.id.toString(), customHeader: 'CUSTOM_HEADER_VALUE' });
        subscriptions.push(subId);
      });
    }

    subscribe(currentUser);
    return () => {
      subscriptions.forEach((s) => s?.unsubscribe());
    }

  }, [currentUser, stompClient]);

  return <>{children}</>;
};

export default StompChatDataProvider;