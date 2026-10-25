import { create } from 'zustand';
import Chat from '../models/Chat';
import Message from '../models/Message';
import MessengerService from '../services/MessengerService';
import { ID } from '../util/Types';

/**
 * Ensure a value is a real Chat instance. If it's already a Chat, keep it;
 * otherwise rebuild it through the Chat constructor.
 */
function toChat(raw: any): Chat {
    if (raw instanceof Chat) return raw;
    return new Chat(
        raw.id ?? null,
        raw.name ?? null,
        raw.private ?? null,
        raw.avatar ?? null,
        raw.time ?? null,
        raw.participants ?? undefined,
        raw.lastMessage ?? null,
        raw.draft ?? null,
        raw.unreadMsgCount ?? null,
        raw.lastReadMsg ?? null,
    );
}

interface ChatStoreState {
    /** Full chat list. Single source of truth — useFetchChats loads into this. */
    chats: Chat[];
    /** Currently open chat, or null when nothing is selected. */
    activeChat: Chat | null;

    /** Monotonic request id so stale getChat responses are dropped. */
    _activeChatRequestId: number;

    // ---- state setters -------------------------------------------------
    /** Replace or functionally update the list. */
    setChats: (updater: Chat[] | ((prev: Chat[]) => Chat[])) => void;
    /** Append a page (used by loadMore). */
    appendChats: (page: Chat[]) => void;
    /** Replace the whole list (used by initial fetch / refresh). */
    replaceChats: (page: Chat[]) => void;
    /** Set / clear the active chat (also bumps the race guard). */
    setActiveChat: (chat: Chat | null) => void;
    closeActiveChat: () => void;

    // ---- async actions -------------------------------------------------
    /** Fetch a chat by id and make it active, guarding against stale responses. */
    openChatById: (chatId: ID, userId: ID) => Promise<void>;

    /**
     * Find-or-create a 1:1 private chat with `otherUserId` and make it active.
     * This is what UserProfileDialog's "Message" button calls.
     */
    openPrivateChatWith: (currentUserId: ID, otherUserId: ID) => Promise<void>;

    /** Existing creation flow (from UserList). */
    createChatFromParticipants: (
        currentUserId: ID,
        participantIds: ID[],
        params?: { chatName: string | null; multiSelect: boolean | null } | null,
    ) => Promise<void>;
}

const isSamePrivateChat = (chat: Chat, a: ID, b: ID) =>
    chat.private === true &&
    Array.isArray(chat.participants) &&
    chat.participants.length === 2 &&
    chat.participants.includes(a) &&
    chat.participants.includes(b);

export const useChatStore = create<ChatStoreState>((set, get) => ({
    chats: [],
    activeChat: null,
    _activeChatRequestId: 0,

    setChats: (updater) =>
        set((state) => ({
            chats:
                typeof updater === 'function'
                    ? (updater as (prev: Chat[]) => Chat[])(state.chats)
                    : updater,
        })),

    appendChats: (page) =>
        set((state) => ({ chats: [...state.chats, ...page] })),

    replaceChats: (page) => set({ chats: page }),

    setActiveChat: (chat) =>
        set((state) => ({
            activeChat: chat,
            _activeChatRequestId: state._activeChatRequestId + 1,
        })),

    closeActiveChat: () =>
        set((state) => ({
            activeChat: null,
            _activeChatRequestId: state._activeChatRequestId + 1,
        })),

    openChatById: async (chatId, userId) => {
        const requestId = ++get()._activeChatRequestId;
        const fetched = await MessengerService.getChat(chatId, userId);
        if (requestId !== get()._activeChatRequestId) return; // stale — ignore
        // Only the request id changes here; don't bump it twice via setActiveChat.
        set({ activeChat: fetched });
    },

    openPrivateChatWith: async (currentUserId, otherUserId) => {
        if (currentUserId === otherUserId) return;

        // Fast path: reuse a 1:1 chat we already know about.
        const existing = get().chats.find((c) =>
            isSamePrivateChat(c, currentUserId, otherUserId),
        );
        if (existing?.id != null) {
            await get().openChatById(existing.id, currentUserId);
            return;
        }

        // Slow path: create on the server, register in the list, then open.
        // If your backend has getOrCreatePrivateChat, call that instead —
        // it's race-safe server-side.
        const draft = new Chat(
            null,
            null,
            true, // private
            null,
            null,
            [currentUserId, otherUserId],
            null,
            true, // draft
        );
        const created = toChat(await MessengerService.createChat(draft));

        // Server may have deduped and returned an existing chat.
        set((state) => {
            const already = state.chats.some((c) => c.id === created.id);
            return { chats: already ? state.chats : [created, ...state.chats] };
        });

        if (created.id != null) {
            await get().openChatById(created.id, currentUserId);
        }
    },

    createChatFromParticipants: async (currentUserId, participantIds, params) => {
        const draft = new Chat(
            null,
            params?.chatName ?? null,
            !params?.multiSelect,
            null,
            null,
            [currentUserId, ...participantIds],
            null,
            true,
        );
        const created = toChat(await MessengerService.createChat(draft));
        set((state) => ({ chats: [created, ...state.chats] }));
        if (created.id != null) {
            await get().openChatById(created.id, currentUserId);
        }
    },
}));