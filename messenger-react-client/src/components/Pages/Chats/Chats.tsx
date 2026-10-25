import { useCallback, useEffect, useRef, useState } from "react";
import { Group, Panel, Separator } from "react-resizable-panels";
import { useFetchChats } from "./FetchData";
import "./Chats.css";
import ChatsList from "./ChatsList";
import {
    CHATS_COMPONENT_MESSAGE_QUEUE,
    CHATS_COMPONENT_MSG_UNREAD_COUNT_QUEUE,
    DIRECTION,
} from "../../../util/Constants";
import ChatRoom from "./ChatRoom";
import Chat from "../../../models/Chat";
import { Button, InputAdornment, TextField } from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";
import { useListener } from "react-bus";
import Message, { MessageType } from "../../../models/Message";
import ChatCreation from "../../Lists/UserList";
import { useAuthStore } from "../../../stores/authStore";
import { ID } from "../../../util/Types";
import { useChatStore } from "../../../stores/chatStore";

export interface ChatCreationParams {
    chatName: string | null;
    multiSelect: boolean | null;
}

const SearchBar = ({ onChange }: { onChange: (value: string) => any }) => (
    <TextField
        id="standard-basic"
        label="Search"
        variant="filled"
        type="search"
        size="small"
        sx={{
            mt: 1, mr: 1, ml: 1,
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
            },
        }}
        onChange={(e: any) => onChange(e.target.value)}
    />
);

export default function Chats() {
    const [lastElementRef, setLastElementRef] = useState<Element | null>(null);
    const currentUser = useAuthStore((s) => s.currentLoggedUser);
    const userId = currentUser?.id;
    const [search, setSearch] = useState<string|null>(null);

    const chats = useChatStore((s) => s.chats);
    const activeChat = useChatStore((s) => s.activeChat);
    const setChats = useChatStore((s) => s.setChats);
    const closeActiveChat = useChatStore((s) => s.closeActiveChat);
    const openChatById = useChatStore((s) => s.openChatById);
    const [creationDialogOpen, setCreationDialogOpen] = useState(false);

    const { loading, hasMore, error, refresh, loadMore } =
        useFetchChats(userId ?? 0, DIRECTION.PAST, 50);

    // Refs used by memoized handlers to see fresh values without re-subscribing.
    const activeChatIdRef = useRef<ID | null>(activeChat?.id ?? null);
    useEffect(() => { activeChatIdRef.current = activeChat?.id ?? null; });

    const chatsRef = useRef(chats);
    useEffect(() => { chatsRef.current = chats; }, [chats]);

    const loadMoreRef = useRef(loadMore);
    useEffect(() => { loadMoreRef.current = loadMore; }, [loadMore]);

    const onMessageReceived = useCallback(
        async (dto: any) => {
            const msg: Message = dto.message;
            const chat: Chat = dto.chat;

            // Case 0: delete of the last message → refresh from server.
            if (msg.type === MessageType.DELETE && msg.chatId === chat.id) {
                const rawPayload = msg.data?.data;
                if (rawPayload == null) return;
                const deleteIds: ID[] = Array.isArray(rawPayload) ? rawPayload : [rawPayload];
                if (deleteIds.length === 0) return;

                const chatToUpdate = chatsRef.current.find((c) => c.id === chat.id);
                if (!chatToUpdate) return;
                if (!deleteIds.includes(chatToUpdate.lastMessage?.id as ID)) return;
                refresh();
                return;
            }

            const message = new Message(
                msg.id, msg.chatId, msg.receiverId,
                msg.type, msg.data, msg.author, msg.time,
            );

            // Case 1: user is being added to a chat they don't yet have.
            if (
                (msg.type === MessageType.CREATION ||
                    (msg.type === MessageType.JOIN && message.author?.id === userId)) &&
                chat != null
            ) {
                const newChat = new Chat(
                    chat.id, chat.name, chat.private, chat.avatar, chat.time,
                    chat.participants, message, chat.draft,
                    dto.unreadMsgCount != null
                        ? dto.unreadMsgCount + 1
                        : chat.unreadMsgCount != null
                            ? chat.unreadMsgCount + 1
                            : null,
                    message,
                );
                setChats((prev) => [newChat, ...prev]);

                if (msg.type === MessageType.JOIN && chat.id != null && userId != null) {
                    await openChatById(chat.id, userId);
                }
                return;
            }

            // Case 2: update existing chat + move to top.
            setChats((prev) => {
                const rest: Chat[] = [];
                let updated: Chat | null = null;
                for (const el of prev) {
                    if (el.id === msg.chatId) {
                        updated = new Chat(
                            el.id, el.name, el.private, el.avatar, el.time,
                            el.participants, message, el.draft,
                            dto.unreadMsgCount != null
                                ? dto.unreadMsgCount + 1
                                : el.unreadMsgCount != null
                                    ? el.unreadMsgCount + 1
                                    : el.id !== activeChatIdRef.current
                                        ? 1
                                        : null,
                        );
                    } else {
                        rest.push(el);
                    }
                }
                if (updated == null) return prev;
                return [updated, ...rest];
            });
        },
        [userId, refresh, setChats, openChatById],
    );

    useListener(CHATS_COMPONENT_MESSAGE_QUEUE, onMessageReceived);

    const handleUnreadCount = useCallback(
        (dto: any) => {
            if (dto.chatId == null) return;
            setChats((prev) =>
                prev.map((el) =>
                    el.id !== dto.chatId
                        ? el
                        : new Chat(
                            el.id, el.name, el.private, el.avatar, el.time,
                            el.participants, el.lastMessage, el.draft,
                            dto.unreadMsgCount === 0 ? null : dto.unreadMsgCount,
                        ),
                ),
            );
        },
        [setChats],
    );

    useListener(CHATS_COMPONENT_MSG_UNREAD_COUNT_QUEUE, handleUnreadCount);

    useEffect(() => {
        if (!lastElementRef) return;
        const observer = new IntersectionObserver((entries) => {
            if (entries[0].isIntersecting) loadMoreRef.current();
        });
        observer.observe(lastElementRef);
        return () => observer.disconnect();
    }, [lastElementRef]);

    const handleClick = useCallback(
        (chat: Chat) => {
            if (userId && chat.id != null) openChatById(chat.id, userId);
        },
        [openChatById, userId],
    );

    const handleChatCreation = useCallback(
        (users: any[], params?: ChatCreationParams | null) => {
            if (!currentUser?.id) return;
            useChatStore
                .getState()
                .createChatFromParticipants(currentUser.id, users, params ?? null);
        },
        [currentUser?.id],
    );

    const onSearchChange = useCallback((value: string | null) => {
        const s = value ? value : null;
        if (!s || s.length === 0) setSearch(s);
        if (s && s.length < 3) return;
        setSearch(s);
    }, []);

    return (
        <div className="chat-page">
            <div className="chat-dashboard">
                <Group orientation="horizontal" className="chat-dashboard-group">
                    <Panel defaultSize="30%" minSize="10%" maxSize="80%">
                        <div className="chat-dashboard__left">
                            <SearchBar onChange={onSearchChange} />
                            <ChatsList
                                chats={chats}
                                ref={setLastElementRef}
                                itemClickHandler={handleClick}
                            />
                            <ChatCreation
                                user={currentUser}
                                open={creationDialogOpen}
                                onClose={() => setCreationDialogOpen(false)}
                                onResult={(elements, params) => handleChatCreation(elements, params)}
                            />
                        </div>
                    </Panel>
                    <Separator className="chat-resize-handle" />
                    <Panel>
                        <div className="chat-dashboard__right">
                            {activeChat && (
                                <ChatRoom chat={activeChat} closeChat={closeActiveChat} />
                            )}
                            {activeChat == null && (
                                <div className="chat-room__empty" style={{ position: 'relative' }}>
                                    <div>Select chat to start messaging</div>
                                    <Button
                                        variant="contained"
                                        onClick={() => setCreationDialogOpen(true)}
                                        sx={{
                                            position: "absolute",
                                            bottom: 20,
                                            right: 20,
                                            width: '60px',
                                            height: '60px',
                                            borderRadius: '50%',
                                            minWidth: 0, // override MUI's default Button min-width for round buttons
                                        }}
                                    >
                                        Go
                                    </Button>
                                </div>
                            )}
                        </div>
                    </Panel>
                </Group>
            </div>
        </div>
    );
}