import { useCallback, useEffect, useState, useRef } from "react";
import { FetchChats } from "./FetchData";
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
    const [{ time, chatId }, setThreshold] = useState<{ time?: Date | null, chatId: number | string | null }>({ time: null, chatId: null });
    const [lastElementRef, setLastElementRef] = useState(null);
    const observerRef = useRef<IntersectionObserver | null>(null);
    const [counter, setCounter] = useState(0);
    const currentUser = useAuthStore(state => state.currentLoggedUser);
    const userId = currentUser?.id;
    const [activeChat, setActiveChat] = useState<Chat | null>(null);
    // TODO: direction

    const {
        chats,
        setChats,
        loading,
        hasMore,
        error
    } = FetchChats(userId ? userId : 0, time, chatId, DIRECTION.PAST);

    const activeChatIdRef = useRef<number | string | null>(activeChat?.id);
    useEffect(() => {
        activeChatIdRef.current = activeChat?.id;
    });

    // New message's been sent
    const onMessageReceived = useCallback(async (dto: any) => {
        const msg = dto.message;
        const chat = dto.chat;
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
    }, [userId]);

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
        observerRef.current = new IntersectionObserver(
            (entries) => {
                if (entries[0].isIntersecting) {
                    setCounter(prev => prev + 1);
                }
            }
        );
    }, []); // Runs on start only

    useEffect(() => {
        const observerCurrent = observerRef.current;

        if (lastElementRef) {
            observerCurrent?.observe(lastElementRef);
        }

        return () => {
            if (lastElementRef) {
                observerCurrent?.disconnect();
            }
        };
    }, [lastElementRef]);

    const handleClick = async (chat: Chat) => {
        // make a fetch
        const fetchedChat = await MessengerService.getChat(chat.id, userId);
        // This is a chat with participants
        setActiveChat(fetchedChat);
    }

    const handleChatCreation = async (users: any[], params?: ChatCreationParams | null) => {
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
    }

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