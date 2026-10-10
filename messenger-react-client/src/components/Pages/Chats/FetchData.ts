import { useCallback, useEffect, useState } from "react";
import Chat from "../../../models/Chat";
import MessengerService from "../../../services/MessengerService";
import Message from "../../../models/Message";
import { PagingParams } from '../Chats/ChatRoom';

export function FetchChats(userId: number | string, time?: Date | null, chatId?: number | string | null,
    direction?: number | null, count?: number | null) {
    const [chats, setChats] = useState<Chat[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [hasMore, setHasMore] = useState(true);
    const [error, setError] = useState();

    useEffect(() => {
        const fetchChats = async () => {
            if (isLoading) return;
            setIsLoading(true);
            const dto = {
                'userId': userId,
                'time': time,
                'chatId': chatId,
                'direction': direction,
                'count': count
            };
            const newChats = await MessengerService.getChatsByUser(dto) || [];
            setChats([...chats, ...newChats]);
            setIsLoading(false);
            setHasMore(newChats && newChats.length > 0);
        };
        fetchChats();
    }, []); // once {time, chatId}

    return { chats, setChats, loading: isLoading, hasMore, error };
}

export function FetchMessages(pagingParams: PagingParams) {
    const [messages, setMessages] = useState<Message[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [hasMore, setHasMore] = useState(true);
    const [error, setError] = useState();
    useEffect(() => {
        const fetchMessages = async () => {
            if (isLoading) return;
            setIsLoading(true);
            const dto = {
                'chatId': pagingParams.chatId,
                'userId': pagingParams.userId,
                'time': pagingParams.time,
                'messageId': pagingParams.messageId,
                'direction': pagingParams.direction,
                'count': pagingParams.count
            };
            const newMessages = await MessengerService.getMessages(dto);
            setMessages((prevMessages) => [...newMessages]);
            //setMessages([...messages, ...newMessages]);
            setIsLoading(false);
            setHasMore(newMessages && newMessages.length > 0);
        };
        fetchMessages();
    }, [pagingParams]); // once {time, messageId}

    return { messages, setMessages, loading: isLoading, hasMore, error };
}