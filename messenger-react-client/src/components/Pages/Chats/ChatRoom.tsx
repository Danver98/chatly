import { useCallback, useState, useEffect, useEffectEvent, useRef } from "react";
import MessageList from "./MessagesList";
import { CHATS_COMPONENT_MSG_UNREAD_COUNT_QUEUE, DIRECTION,
    CHATS_COMPONENT_MESSAGE_QUEUE } from "../../../util/Constants";
import Chat from "../../../models/Chat";
import Message, { MessageData, MessageDataType, MessageType } from "../../../models/Message";
import User from "../../../models/User";
import { useAuthStore } from "../../../stores/authStore";;
import { Box, Button, IconButton } from "@mui/material";
import CircularProgress from '@mui/material/CircularProgress';
import { IPublishParams, StompHeaders } from '@stomp/stompjs';
import { useStompClient } from "react-stomp-hooks";
import { useBus, useListener } from 'react-bus';
import MessengerService from "../../../services/MessengerService";
import { getType } from "../../../util/FileUtils";
import { Headers } from "../../../util/Constants";
import CloseIcon from '@mui/icons-material/Close';
import ChatRoomMenu from "./ChatRoomMenu";
import { ID } from "../../../util/Types";
import ChatRoomEdit from "./ChatRoomEdit";
import MessageSender from "./MessageSender";

import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import MessageMenu, { MENU_ACTIONS, MenuAction } from "./MessageMenu";

export interface MessageListHandle {
    getSentinelNode: () => HTMLLIElement | null;
}

const Circle = ({value, clickHandler }: {value?: string | number | null, clickHandler: () => void}) => (
    <Box
        className='chat-room-page__unreadMsgCounter_Box'
        sx={{
            position: "absolute",
            bottom: 80,
            right: 5
        }}
    >
        <Button
            type="submit"
            variant="contained"
            color="secondary"
            sx={{
                aspectRatio:"1/1",
                borderRadius: '50%',
            }}
        >
            {value}
        </Button>
        <IconButton
            className='chat-room-page__unreadMsgCounter_arrowDown'
            onClick={clickHandler}
        >
            <KeyboardArrowDownIcon
                fontSize="small"
                sx={{
                    boxShadow: 'none', filter: 'none'
                }}
                />
        </IconButton>
    </Box>
);

export interface PagingParams {
    chatId?: number | string | null;
    userId?: number | string | null;
    time?: Date | null;
    messageId?: number | string | null;
    direction?: number | null;
    count?: number | null;
    include?: boolean | null;
}

export default function ChatRoom({ chat, closeChat }: { chat: Chat, closeChat?: () => void }) {
    const currentUser: User | null = useAuthStore((state) => state.currentLoggedUser);
    const bus = useBus();
    const [pagingParams, setPagingParams] = useState<PagingParams>(
        {
            chatId: chat.id,
            userId: currentUser?.id,
            direction: DIRECTION.PAST,
            count: 50,
        });
    const [draft, setDraft] = useState<boolean | null | undefined>(chat.draft);
    const messageListHandleRef = useRef<MessageListHandle | null>(null);
    const stompClient = useStompClient();
    const [messages, setMessages] = useState<Message[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [hasMore, setHasMore] = useState(false);
    const [permissions, setPermissions] = useState<string[]>([]);
    const [intersected, setIntersected] = useState<number>(0);
    const lastReadMsgRef = useRef<string| null>(chat.lastReadMsg ? chat.lastReadMsg.id : null);
    const messageListRef = useRef<HTMLUListElement>(null);
    const [unreadMsgCount, setUnreadMsgCount] = useState<number>(chat.unreadMsgCount || 0);

    const [editMenuOpen, setEditMenuOpen] = useState(false);

    const [contextMenuAnchorElement, setContextMenuAnchorElement] = useState<null | HTMLElement>(null);
    const [contextMenuMessage, setContextMenuMessage] = useState<Message | null>(null);
    const [contextMenuAnchorPosition, setContextMenuAnchorPosition] = useState<object|null>(null);

    const [replyMessage, setReplyMessage] = useState<Message | null>(null);

    const fetchMessages = async (params: PagingParams) => {
        const dto = {
            'chatId': params.chatId,
            'userId': params.userId,
            'time': params.time,
            'messageId': params.messageId,
            'direction': params.direction || DIRECTION.PAST,
            'count': params.count || 50
        };
        return await MessengerService.getMessages(dto);
    }

    /**
     * Setting permissions for authenticated user for given chat.
     */
    useEffect(() => {
        const fetchPermissions = async (chatId: ID) => {
            setPermissions([]);
            const permissions = await MessengerService.getChatPermissions(chatId);
            setPermissions(permissions);
        }
        fetchPermissions(chat.id);
    }, [chat.id]);

    const onMessageReceived = useCallback((dto: any) => {
        const data = dto.message;
        const isLastMessageVisible = messageListRef.current?.scrollTop === 0;
        const message = new Message(data.id, data.chatId, data.receiverId, data.type, data.data,
            data.author, data.time, isLastMessageVisible, data.replyTo, data.repliedMessage);
        const currentUserIsAuthor = message.author?.id === currentUser?.id;
        setMessages((prevMessages: Message[]) =>
            [message, ...prevMessages])
        if (isLastMessageVisible || currentUserIsAuthor) {
            // Newly added message is fully visible to user, so mark it as last read
            lastReadMsgRef.current = message.id;
        } else if (!currentUserIsAuthor) {
            // If user is not viewing the last message and he is not the author of the message,
            // increment unreadMsgCount
            setUnreadMsgCount(count => count + 1);
        }
    }, []);

    /** Scroll down to the top of messages list, when current user sends a new message */
    useEffect(() => {
        if (messageListRef.current?.scrollTop === 0 || messages.length && messages[0].author?.id !== currentUser?.id) return;
        scrollToLatestMessage(false, 'instant');
    }, [messages, currentUser?.id]);

    /**
     * Handles intersection of the next unread message
     * It updates the unreadMsgCount and current lastReadMsg id.
     * It's supposed handler is only fired on unread messages not to move pointer
     * to earlier read messages
     */
    const handleMsgIntersection = useEffectEvent((params?: any) => {
        const lastReadMsgIndex = messages.findIndex(m => m.id === lastReadMsgRef.current);
        const msgIndex = params.position ?? messages.findIndex(m => m.id === params.id);
        if (msgIndex === -1) return;
        if (lastReadMsgIndex === -1) {
            // User has opened chat for the first time and there are brand new messages or
            // current message portion doesn't include any unread messages
            lastReadMsgRef.current = params.id;
            const decrement = messages.length - msgIndex;
            setMessages((prevMessages) => prevMessages.map((message, index) => message.id === params.id || index > msgIndex ?
                    new Message(message.id, message.chatId, message.receiverId, message.type, message.data,
                        message.author, message.time, true, message.replyTo, message.repliedMessage)
                    : message));
            setUnreadMsgCount((prevCount) => prevCount === 0 ? prevCount : Math.max(0, prevCount - decrement));
            return;
        }
        // If message has already been read, do nothing
        if (msgIndex >= lastReadMsgIndex) return;
        lastReadMsgRef.current = params.id;
        setMessages((prevMessages) => prevMessages.map((message, index) => message.id === params.id || index > msgIndex?
                new Message(message.id, message.chatId, message.receiverId, message.type, message.data,
                    message.author, message.time, true, message.replyTo, message.repliedMessage) 
                : message));
        const decrement = lastReadMsgIndex - msgIndex;
        if (messages[msgIndex].author?.id !== currentUser?.id) {
            // If current user is not the author of the message, decrement unreadMsgCount
            setUnreadMsgCount((prevCount) => prevCount === 0 ? prevCount : Math.max(0, prevCount - decrement));
        };
    });

    const sendMessage = async (event: any) => {
        const user = currentUser as User;
        const formData = new FormData(event.currentTarget);
        event.preventDefault();
        const file = formData.get('fileData')
        let messageData = {
            type: MessageDataType.TEXT,
            data: formData.get('messageData')
        };

        if (file instanceof File && file.name) {
            setIsLoading(true);
            const url = await MessengerService.sendAttachment(file as File, chat.id, user.id);
            setIsLoading(false);
            if (url == null) {
                return alert(`Failed to upload "${file.name}" resource to the server`);
            }
            let _type = getType(file);
            messageData = {
                type: _type,
                data: url
            }
            _sendMessage(messageData);
        } else {
            _sendMessage(messageData);
        }
    }

    const _sendMessage = async (messageData: MessageData) => {
        const user = currentUser as User;
        const type = MessageType.CHAT;
        const author: User = {
            id: user.id,
            name: user.name,
            surname: user.surname,
            avatar: user.avatar
        };
        const receiverId = chat.private && chat.participants ?
            chat.participants[0] === user.id ?
                chat.participants[1]
                : chat.participants[0]
            : null;
        const destination = chat.private ? '/app/chats/private/send-message' : '/app/chats/public/send-message';

        if (draft && chat.private) {
            setDraft(false);
        }

        const message = new Message(
            null,
            chat.id,
            receiverId,
            type,
            messageData,
            author,
        );
        message.read = true;
        message.replyTo = replyMessage?.id;
        message.repliedMessage = replyMessage;

        const params: IPublishParams = {
            destination: destination,
            body: JSON.stringify({
                message: message,
                chat: chat
            }),
            headers: {
                [Headers.X_REQUEST_RESOURCE_OBJECT]: chat.id
            } as StompHeaders
        }
        stompClient?.publish(params);
        if (chat.private) {
            // TODO: in private chats we don't get back our messages, so we've to insert it manually
            setMessages((prevMessages: Message[]) =>
                [message, ...prevMessages]);
            // send message to chats component queue
            bus.emit(CHATS_COMPONENT_MESSAGE_QUEUE, {
                message: message,
                chat: chat
            });
        }
        if (replyMessage) {
            setReplyMessage(null);
        }
    }

    // Subscribe to new messages coming to chat
    useListener(`/chats/${chat.id}/messages`, onMessageReceived);

    /**
     * Starts fetching new chunk of messages on pagingParams change
     */
    useEffect(() => {
        if (!hasMore) return;
        const f = async () => {
            const newMessages = await fetchMessages(pagingParams);
            if (pagingParams.include) {
                setMessages((prevMessages) => [...prevMessages, ...newMessages]);
            } else {
                setMessages((prevMessages) => [...newMessages]);
            }
            setHasMore(newMessages && newMessages.length > 0);
        };
        f();
    }, [pagingParams, hasMore]);

    const pagingObserverRef = useRef<IntersectionObserver | null>(null);

    useEffect(() => {
        pagingObserverRef.current = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    setIntersected((n) => n + 1); // counter instead of toggle
                }
            });
        });
        return () => {
            pagingObserverRef.current?.disconnect();
            pagingObserverRef.current = null;
        };
    }, []);

    useEffect(() => {
        const observer = pagingObserverRef.current;
        const node = messageListHandleRef.current?.getSentinelNode() ?? null;
        if (!observer || !node) return;

        observer.observe(node);
        return () => {
            observer.unobserve(node);
        };
    }, [messages, chat.id]);


    useEffect(() => {
        bus.emit(CHATS_COMPONENT_MSG_UNREAD_COUNT_QUEUE, {
            chatId: chat.id,
            unreadMsgCount,
        });
    }, [bus, chat.id, unreadMsgCount]);

    useEffect(() => {
        return () => {
            if (chat.id && lastReadMsgRef && currentUser?.id) {
                if (chat.lastReadMsg?.id === lastReadMsgRef.current || !lastReadMsgRef.current) {
                    return;
                }
                MessengerService.updateLastReadMsg(
                    chat.id,
                    currentUser.id,
                    lastReadMsgRef.current!
                );
            }
        };
    }, [bus, chat.id, chat.lastReadMsg?.id, currentUser?.id]);

    useEffect(() => {
        setMessages([]);
        setHasMore(true);
        setUnreadMsgCount(chat.unreadMsgCount || 0);
        lastReadMsgRef.current = chat.lastReadMsg ? chat.lastReadMsg.id : null;
        setPagingParams({
            chatId: chat.id,
            userId: currentUser?.id,
            direction: DIRECTION.PAST,
            count: 50,
        });
    }, [chat.id, currentUser?.id]);

    /**
     * Fires when last message is intersected. Setting new paging params triggers messages fetching
     */
    useEffect(() => {
        if (!messages) return;
        const lastMsg = messages[messages.length - 1];
        if (!lastMsg) return;
        setPagingParams({
            chatId: chat.id,
            time: lastMsg.time,
            messageId: lastMsg.id,
            include: true
        })
    }, [intersected]);

    const scrollToLatestMessage = async (updateLastReadMsgOnServer: boolean = true, behavior: ScrollBehavior = 'smooth') => {
        if (!chat.id || !messages || !messages.length || !currentUser?.id ||
            !messageListRef.current || !lastReadMsgRef.current) return;
        
        const handleScrollFinished = () => {
            if (! chat.id || !messages || !messages.length || !currentUser?.id) return;
            setUnreadMsgCount(0);
            lastReadMsgRef.current = messages[0].id;
            if (updateLastReadMsgOnServer) {
                MessengerService.updateLastReadMsg(
                    chat.id,
                    currentUser.id,
                    messages[0].id!
                );
            };
            // Clean up the listener so it doesn't trigger on normal user scrolling later
            messageListRef.current?.removeEventListener('scrollend', handleScrollFinished);
        };

        messageListRef.current?.addEventListener('scrollend', handleScrollFinished);
        messageListRef.current?.scrollTo({
            top: 0,
            behavior
        });
    }

    const onContextMenu = useCallback((event: React.MouseEvent<HTMLLIElement>, message: Message) => {
        event.preventDefault();
        setContextMenuAnchorElement(event.currentTarget as HTMLElement);
        setContextMenuMessage(message);
        setContextMenuAnchorPosition({
            top: event.clientY,
            left: event.clientX
        })
    }, []);

    const handleClose = useCallback(() => {
        setContextMenuAnchorElement(null);
        setContextMenuAnchorPosition(null);
        setContextMenuMessage(null);
    }, []);

    const onContextMenuActionSelected = useCallback((action: MenuAction, message: Message) => {
        handleClose();
        switch (action) {
            case MENU_ACTIONS.DELETE:
                console.log('DELETE');
                break;
            case MENU_ACTIONS.REPLY:
                setReplyMessage(message);
                break;
            default:
                break;
        }
    }, [handleClose]);

    return (
        <div className="chat-room-page">
            <div className="chat-room-page-header">
                <div className="chat-room-page-header__ChatName" role="button"
                    tabIndex={0} onClick={() => {setEditMenuOpen(true)}}
                >
                    {chat.name}
                </div>
                {
                    <ChatRoomEdit isOpen={editMenuOpen} chatId={chat.id} user={currentUser}
                        permissions={permissions}
                        onResult={() => {setEditMenuOpen(false)}}
                    />
                }
                {
                !!permissions.length &&
                <ChatRoomMenu chat={chat} user={currentUser}
                    permissions={permissions} closeChat={closeChat}
                />
                }
            </div>
            <div className="chat-room-page__CentralBlock">
                <MessageList
                    chatId={chat.id}
                    messages={messages}
                    lastReadMsgIdOnOpen={chat.lastReadMsg?.id}
                    intersectionHandler={handleMsgIntersection}
                    user={currentUser}
                    ref={messageListHandleRef}
                    listRef={messageListRef}
                    lastReadMsgRef={lastReadMsgRef}
                    onContextMenu={onContextMenu}
                />
                {
                    isLoading && <CircularProgress />
                }
                <MessageMenu user={currentUser} chat={chat} message={contextMenuMessage}
                    anchorPosition={contextMenuAnchorPosition} onAction={onContextMenuActionSelected}
                    handleClose={handleClose}
                />
                {
                    unreadMsgCount > 0 &&
                    <Circle value={unreadMsgCount} clickHandler={scrollToLatestMessage}/>
                }
                <MessageSender handleSubmit={sendMessage} replyMessage={replyMessage} setReplyMessage={setReplyMessage}/>
                <IconButton
                    onClick={() => { closeChat?.() }}
                    sx={{
                        position: "absolute",
                        left: "105%",
                        top: "10px",
                        
                    }}
                >
                    <CloseIcon
                        fontSize="large"
                        />
                </IconButton>
            </div>
        </div>
    )
}