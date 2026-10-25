import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Chat from "../../../models/Chat";
import MessengerService from "../../../services/MessengerService";
import Message from "../../../models/Message";
import { PagingParams } from '../Chats/ChatRoom';
import { DIRECTION } from "../../../util/Constants";
import UserService, { INavigation, UserFilter, UserRequestDTO } from "../../../services/UserService";
import User from "../../../models/User";
import { ID } from "../../../util/Types";
import { useChatStore } from "../../../stores/chatStore";


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
    const replaceChats = useChatStore((s) => s.replaceChats);
    const appendChats = useChatStore((s) => s.appendChats);

    const [isLoading, setIsLoading] = useState(false);
    const [hasMore, setHasMore] = useState(true);
    const [error, setError] = useState<unknown>();
    const [refreshToken, setRefreshToken] = useState(0);

    const cursorRef = useRef<{ time: Date | null; chatId: number | string | null }>({
        time: null,
        chatId: null,
    });
    const isLoadingRef = useRef(false);
    const hasMoreRef = useRef(true);
    const userIdRef = useRef(userId);

    useEffect(() => {
        userIdRef.current = userId;
    }, [userId]);

    const fetchPage = useCallback(
        async (cursor: { time: Date | null; chatId: number | string | null } | null) => {
            return (
                (await MessengerService.getChatsByUser({
                    userId: userIdRef.current,
                    time: cursor?.time ?? null,
                    chatId: cursor?.chatId ?? null,
                    direction,
                    count,
                })) ?? []
            );
        },
        [direction, count],
    );

    // Initial load + refresh → replace the store list.
    useEffect(() => {
        let cancelled = false;
        const run = async () => {
            isLoadingRef.current = true;
            setIsLoading(true);
            try {
                const page = await fetchPage(null);
                if (cancelled) return;
                replaceChats(page);
                setHasMore(page.length > 0);
                hasMoreRef.current = page.length > 0;

                const last = page[page.length - 1];
                cursorRef.current = {
                    time: last?.getTime() ?? null,
                    chatId: last?.id ?? null,
                };
            } catch (e) {
                if (!cancelled) setError(e);
            } finally {
                isLoadingRef.current = false;
                if (!cancelled) setIsLoading(false);
            }
        };
        run();
        return () => { cancelled = true; };
    }, [userId, direction, refreshToken, fetchPage, replaceChats]);

    const refresh = useCallback(() => setRefreshToken((t) => t + 1), []);

    const loadMore = useCallback(async () => {
        if (isLoadingRef.current || !hasMoreRef.current) return;
        if (cursorRef.current.chatId == null) return;

        isLoadingRef.current = true;
        setIsLoading(true);
        try {
            const page = await fetchPage(cursorRef.current);
            appendChats(page);
            const stillMore = page.length > 0;
            setHasMore(stillMore);
            hasMoreRef.current = stillMore;

            if (page.length > 0) {
                const last = page[page.length - 1];
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
    }, [fetchPage, appendChats]);

    return { loading: isLoading, hasMore, error, refresh, loadMore };
}

export function useFetchUsers(filter: UserFilter, sorting: object | null, navigation: INavigation) {
    const [isLoading, setIsLoading] = useState(false);
    const [hasMore, setHasMore] = useState(true);
    const [error, setError] = useState<unknown>();
    const [users, setUsers] = useState<User[]>([]);
    const [refreshToken, setRefreshToken] = useState(0);
    const abortController = useMemo(() => {
        return new AbortController();
    }, []);

    // Cursor for the next page: derived from the last loaded user.
    // We keep it in a ref so loadMore doesn't need to be recreated when users change.
    const cursorRef =useRef<ID|null>(navigation?.cursor);
    // Ref-based guard so loadMore doesn't fire twice for rapid intersections.
    const isLoadingRef = useRef(false);
    const hasMoreRef = useRef(true);

    // Initial load + refresh. Replaces the list, resets the cursor.
    useEffect(() => {
        let cancelled = false;
        hasMoreRef.current = true;
        setHasMore(true);
        const fetchData = async () => {
            isLoadingRef.current = true;
            setIsLoading(true);
            const dto = {
                filter,
                sorting,
                navigation: {
                    ...navigation
                }
            }
            try {
                const newUsers = await UserService.list(dto, abortController);
                if (cancelled) return;
                // Check if answer contains data
                setUsers(newUsers);
                setHasMore(newUsers && newUsers.length > 0);
                hasMoreRef.current = newUsers && newUsers.length > 0;
                if (newUsers.length > 0)
                    cursorRef.current = newUsers[newUsers.length - 1].id;
            } catch (e) {
                if (!cancelled) setError(e);
            } finally {
                isLoadingRef.current = false;
                if (!cancelled) {
                    setIsLoading(false);
                }
            }
        }
        fetchData();
        return () => { cancelled = true; };
    }, [filter, sorting, navigation, refreshToken, abortController]);

    const loadMore = useCallback(async () => {
        if (isLoadingRef.current || !hasMoreRef.current) return;
        if (!cursorRef.current) return;
        isLoadingRef.current = true;
        setIsLoading(true);
        const dto = { 
            filter,
            sorting,
            navigation: {
                ...navigation,
                cursor: cursorRef.current 
            }
        };
        try {
            const newUsers = await UserService.list(dto, abortController);
            // Check if answer contains data
            setUsers((prevUsers) => [...prevUsers, ...newUsers]);
            setHasMore(newUsers && newUsers.length > 0);
            hasMoreRef.current = newUsers && newUsers.length > 0;
            if (newUsers.length > 0)
                cursorRef.current = newUsers[newUsers.length - 1].id;
        } catch (e) {
            setError(e);
        } finally {
            isLoadingRef.current = false;
            setIsLoading(false);
        }
    }, [filter, sorting, navigation, abortController]);

    const refresh = useCallback(() => setRefreshToken(t => t + 1), []);

    return { users, setUsers, isLoading, hasMore, error, loadMore, refresh };
}