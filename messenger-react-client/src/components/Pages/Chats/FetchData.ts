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

export function useFetchChats(userId, time, chatId, direction, count) {
    const [chats, setChats] = useState<Chat[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [hasMore, setHasMore] = useState(true);
    const [error, setError] = useState<unknown>();
    const [refreshToken, setRefreshToken] = useState(0);

    // Effect: initial load + refresh
    useEffect(() => {
        let cancelled = false;
        const run = async () => {
            setIsLoading(true);
            try {
                const newChats = await MessengerService.getChatsByUser({
                    userId, time, chatId, direction, count,
                }) ?? [];
                if (cancelled) return;
                setChats(newChats);           // replace
                setHasMore(newChats.length > 0);
            } catch (e) {
                if (!cancelled) setError(e);
            } finally {
                if (!cancelled) setIsLoading(false);
            }
        };
        run();
        return () => { cancelled = true; };
    }, [userId, direction, refreshToken]); // cursor params omitted for clarity

    const refresh = useCallback(() => setRefreshToken(t => t + 1), []);

    // Imperative pagination — appends
    const loadMore = useCallback(async (cursor: { time?: Date | null; chatId?: number | string | null }) => {
        if (isLoading || !hasMore) return;
        setIsLoading(true);
        try {
            const newChats = await MessengerService.getChatsByUser({
                userId, direction, ...cursor, count,
            }) ?? [];
            setChats(prev => [...prev, ...newChats]);
            setHasMore(newChats.length > 0);
        } catch (e) {
            setError(e);
        } finally {
            setIsLoading(false);
        }
    }, [userId, direction, count, hasMore, isLoading]);

    return { chats, setChats, loading: isLoading, hasMore, error, refresh, loadMore };
}