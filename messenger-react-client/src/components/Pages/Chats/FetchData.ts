import { useCallback, useEffect, useRef, useState } from "react";
import Chat from "../../../models/Chat";
import MessengerService from "../../../services/MessengerService";
import Message from "../../../models/Message";
import { PagingParams } from '../Chats/ChatRoom';
import { DIRECTION } from "../../../util/Constants";


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

export function useFetchChats(
    userId: number | string,
    direction: number | null = DIRECTION.PAST,
    count: number | null = 50,
) {
    const [chats, setChats] = useState<Chat[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [hasMore, setHasMore] = useState(true);
    const [error, setError] = useState<unknown>();
    const [refreshToken, setRefreshToken] = useState(0);

    // Cursor for the next page: derived from the last loaded chat.
    // We keep it in a ref so loadMore doesn't need to be recreated when chats change.
    const cursorRef = useRef<{ time: Date | null; chatId: number | string | null }>({
        time: null,
        chatId: null,
    });

    // Ref-based guard so loadMore doesn't fire twice for rapid intersections.
    const isLoadingRef = useRef(false);
    const hasMoreRef = useRef(true);
    const userIdRef = useRef(userId);

    useEffect(() => {
        userIdRef.current = userId;
    }, [userId]);

    const fetchPage = useCallback(
        async (cursor: { time: Date | null; chatId: number | string | null } | null) => {
            return await MessengerService.getChatsByUser({
                userId: userIdRef.current,
                time: cursor?.time ?? null,
                chatId: cursor?.chatId ?? null,
                direction,
                count,
            }) ?? [];
        },
        [direction, count],
    );

    // Initial load + refresh. Replaces the list, resets the cursor.
    useEffect(() => {
        let cancelled = false;
        const run = async () => {
            isLoadingRef.current = true;
            setIsLoading(true);
            try {
                const newChats = await fetchPage(null);
                if (cancelled) return;
                setChats(newChats);
                setHasMore(newChats.length > 0);
                hasMoreRef.current = newChats.length > 0;

                // Reset the cursor to the last (oldest) chat.
                const last = newChats[newChats.length - 1];
                cursorRef.current = {
                    time: last?.getTime() ?? null,
                    chatId: last?.id ?? null,
                };
            } catch (e) {
                if (!cancelled) setError(e);
            } finally {
                isLoadingRef.current = false;
                if (!cancelled) {
                    setIsLoading(false);
                }
            }
        };
        run();
        return () => { cancelled = true; };
    }, [userId, direction, refreshToken, fetchPage]);

    const refresh = useCallback(() => setRefreshToken(t => t + 1), []);

    const loadMore = useCallback(async () => {
        if (isLoadingRef.current || !hasMoreRef.current) return;
        if (cursorRef.current.chatId == null) return;   // no cursor yet

        isLoadingRef.current = true;
        setIsLoading(true);
        try {
            const newChats = await fetchPage(cursorRef.current);
            setChats(prev => [...prev, ...newChats]);
            const stillMore = newChats.length > 0;
            setHasMore(stillMore);
            hasMoreRef.current = stillMore;

            if (newChats.length > 0) {
                const last = newChats[newChats.length - 1];
                cursorRef.current = {
                    time: last?.getTime() ?? null,
                    chatId: last?.id ?? null,
                };
            }
        } catch (e) {
            setError(e);
        } finally {
            isLoadingRef.current = false;
            setIsLoading(false);
        }
    }, [fetchPage]);

    return { chats, setChats, loading: isLoading, hasMore, error, refresh, loadMore };
}