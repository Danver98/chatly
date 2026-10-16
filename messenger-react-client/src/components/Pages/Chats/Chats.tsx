import { useCallback, useEffect, useState, useRef } from "react";
import { useFetchChats } from "./FetchData";
import "./Chats.css"
import ChatsList from "./ChatsList";
import { CHATS_COMPONENT_MESSAGE_QUEUE, CHATS_COMPONENT_MSG_UNREAD_COUNT_QUEUE, DIRECTION } from "../../../util/Constants";
import ChatRoom from "./ChatRoom";
import Chat from "../../../models/Chat";
import { InputAdornment, TextField } from "@mui/material";
import SearchIcon from '@mui/icons-material/Search';
import { useListener } from 'react-bus';
import Message, { MessageType } from "../../../models/Message";
import MessengerService from "../../../services/MessengerService";
import ChatCreation from "../../Lists/UserList";
import { useAuthStore } from "../../../stores/authStore";
import { ID } from "../../../util/Types";

export interface ChatCreationParams {
    chatName: string | null;
    multiSelect: boolean | null;
}

const SearchBar = ({ onChange }: { onChange: (value: string) => any }) => {
    return (
        <TextField
            id="standard-basic"
            label="Search"
            variant="filled"
            type="search"
            size="small"
            sx={{
                mt: 1,
                mr: 1,
                ml: 1,
                "& .MuiFilledInput-root": {
                    borderTopLeftRadius: "25px",
                    borderTopRightRadius: "25px",
                    borderBottomLeftRadius: "25px",
                    borderBottomRightRadius: "25px",

                },
            }}
            slotProps={{
                input: {
                    disableUnderline: true,
                    endAdornment: (
                        <InputAdornment position="end">
                            <SearchIcon />
                        </InputAdornment>
                    ),
                }
            }}
            onChange={(e: any) => {
                onChange(e.target.value)
            }}
        />
    )
}


export default function Chats() {
    const [lastElementRef, setLastElementRef] = useState(null);
    const currentUser = useAuthStore(state => state.currentLoggedUser);
    const userId = currentUser?.id;
    const [activeChat, setActiveChat] = useState<Chat | null>(null);
    // TODO: direction

    const { chats, setChats, loading, hasMore, error, refresh, loadMore } =
        useFetchChats(userId ?? 0, DIRECTION.PAST, 50);

    const activeChatIdRef = useRef<number | string | null>(activeChat?.id);
    useEffect(() => {
        activeChatIdRef.current = activeChat?.id;
    });

    const chatsRef = useRef(chats);
    useEffect(() => {
        chatsRef.current = chats;
    });

    const loadMoreRef = useRef(loadMore);
    useEffect(() => {
        loadMoreRef.current = loadMore;
    });

    // New message's been sent
    const onMessageReceived = useCallback(async (dto: any) => {
        const msg: Message = dto.message;
        const chat: Chat = dto.chat;
        // Case 0: deleting last sent message for a chat
        // We need to update chats list by simple refreshing
        if (msg.type === MessageType.DELETE && msg.chatId === chat.id) {
            const rawPayload = msg.data?.data;
            if (rawPayload == null) return;
            const deleteIds: ID[] = Array.isArray(rawPayload) ? rawPayload : [rawPayload];
            if (deleteIds.length === 0) return;

            const chatToUpdate = chatsRef.current.find(c => c.id === chat.id);
            if (!chatToUpdate) return;
            if (!deleteIds.includes(chatToUpdate.lastMessage?.id)) return;
            refresh();
            return;
        }
        const message = new Message(
            msg.id, msg.chatId, msg.receiverId,
            msg.type, msg.data, msg.author, msg.time
        );

        // Case 1: user is being added to a chat they don't yet have in their list.
        if (
            (msg.type === MessageType.CREATION ||
                (msg.type === MessageType.JOIN && message.author?.id === userId)) &&
            chat != null
        ) {
            const newChat = new Chat(
                chat.id,
                chat.name,
                chat.private,
                chat.avatar,
                chat.time,
                chat.participants,
                message,
                chat.draft,
                dto.unreadMsgCount != null ? dto.unreadMsgCount + 1 :
                    chat.unreadMsgCount != null ? chat.unreadMsgCount + 1 : null,
                message // last read message
            );
            setChats((prevChats) => [newChat, ...prevChats]);

            if (msg.type === MessageType.JOIN) {
                // Server-side fetch to get the fully-hydrated chat object.
                const joinedChat = await MessengerService.getChat(chat.id, userId);
                setActiveChat(joinedChat);
            }
            return;
        }

        // Case 2: the message belongs to an existing chat in the list.
        // Everything is computed inside the functional updater so `prevChats`
        // is guaranteed to be the latest committed state.
        setChats((prevChats) => {
            const filteredChats: Chat[] = [];
            let updatedChat: Chat | null = null;

            for (const element of prevChats) {
                if (element.id === msg.chatId) {
                    updatedChat = new Chat(
                        element.id,
                        element.name,
                        element.private,
                        element.avatar,
                        element.time,
                        element.participants,
                        message,
                        element.draft,
                        dto.unreadMsgCount != null
                            ? dto.unreadMsgCount + 1
                            : element.unreadMsgCount != null
                                ? element.unreadMsgCount + 1
                                : element.id !== activeChatIdRef.current
                                    ? 1
                                    : null
                    );
                } else {
                    filteredChats.push(element);
                }
            }

            // Chat isn't in the list and isn't a CREATION/JOIN → nothing to do.
            if (updatedChat == null) {
                return prevChats;
            }

            return [updatedChat, ...filteredChats];
        });
    }, [userId, refresh]);

    // New message's been sent
    useListener(CHATS_COMPONENT_MESSAGE_QUEUE, onMessageReceived);

    const handleUnreadCount = useCallback((dto: any) => {
        if (dto.chatId == null) {
            return;
        }
        setChats((prevChats) =>
            prevChats.map((element: Chat) => {
                if (element.id !== dto.chatId) {
                    return element;
                }
                return new Chat(
                    element.id,
                    element.name,
                    element.private,
                    element.avatar,
                    element.time,
                    element.participants,
                    element.lastMessage,
                    element.draft,
                    dto.unreadMsgCount === 0 ? null : dto.unreadMsgCount
                );
            })
        );  
    }, []);

    // Update chat list unread messages counter when chat room is open
    useListener(CHATS_COMPONENT_MSG_UNREAD_COUNT_QUEUE, handleUnreadCount);

    useEffect(() => {
        if (!lastElementRef) return;

        const observer = new IntersectionObserver((entries) => {
            if (entries[0].isIntersecting) {
                loadMoreRef.current();
            }
        });

        observer.observe(lastElementRef);
        return () => observer.disconnect();
    }, [lastElementRef]);

    const activeChatRequestIdRef = useRef(0);

    const handleClick = useCallback(async (chat: Chat) => {
        const requestId = ++activeChatRequestIdRef.current;
        const fetchedChat = await MessengerService.getChat(chat.id, userId);
        if (requestId !== activeChatRequestIdRef.current) return;
        setActiveChat(fetchedChat);
    }, [userId]);

    const handleChatCreation = useCallback(async (users: any[], params?: ChatCreationParams | null) => {
        // Create private chat if doesn't exist or redirect to it
        const newChat = new Chat(
            null,
            params?.chatName || null,
            !params?.multiSelect, //private
            null,
            null,
            [currentUser?.id, ...users], // participants
            null,
            true //draft
        );
        const fetchedChat = await MessengerService.createChat(newChat);
        setActiveChat(fetchedChat);
    }, [currentUser?.id]);

    return (
        <>
            <div className="chat-page">
                {/* <div> Last chat info: {chats && chats.length ? chats[chats.length - 1].toString() : null}</div> */}
                <div className="chat-dashboard">
                    <div className="chat-dashboard__left">
                        <SearchBar
                            onChange={() => { }}
                        />
                        <ChatsList
                            chats={chats}
                            ref={setLastElementRef}
                            itemClickHandler={handleClick}
                        />
                        <ChatCreation
                            user={currentUser}
                            onResult={(elements: any[], params?: ChatCreationParams | null) => { handleChatCreation(elements, params) }}
                        />
                    </div>
                    <div className="chat-dashboard__right">
                        {
                            activeChat &&
                            <ChatRoom chat={activeChat} closeChat={() => { setActiveChat(null) }} />
                        }
                        {
                            activeChat == null &&
                            <div className="chat-room__empty">
                                Select chat to start messaging
                            </div>
                        }
                    </div>
                </div>
            </div>
        </>
    )
}