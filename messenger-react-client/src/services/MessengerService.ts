import HttpService from "./HttpService";
import Chat from "../models/Chat";
import Message from "../models/Message";
import { Headers, RANDOM_CHAT_AVATAR_URL } from "../util/Constants";
import { ID } from "../util/Types";

export interface ChatRequestDTO {
    userId: number | string | null;
    time?: Date | null;
    chatId?: number | string | null;
    direction?: number | null;
    count?: number | null;
}

export interface MessageRequestDTO {
    chatId?: number | string | null;
    userId?: number | string | null;
    time?: Date | null;
    messageId?: number | string | null;
    direction?: number | null;
    count?: number | null;
}

class MessengerService {
    // TODO: coordinate backend and frontend API

    private static _instance: MessengerService;
    private static readonly CHAT_URL = '/chats';
    // Temp variables
    private readonly TEMP_MAX_CHAT_PAGES_COUNT = 5;
    private readonly TEMP_MAX_MESSAGE_PAGES_COUNT = 10;

    private constructor() {

    }

    static get Instance() {
        // Do you need arguments? Make it a regular static method instead.
        return this._instance || (this._instance = new this());
    }

    /**
     * 
     * @param id 
     * @returns header for server authorization purposes.
     * It should be attached to EVERY authorized request
     */
    private getRequestResourceObjectHeader(id: ID): HeadersInit {
        return {
            [Headers.X_REQUEST_RESOURCE_OBJECT]: id ? id : null
        } as HeadersInit;
    }

    private randomIntFromInterval(min: number, max: number) { // min and max included 
        return Math.floor(Math.random() * (max - min + 1) + min);
    }

    async getChat(id?: string | number | null, userId?: string | number): Promise<Chat | null> {
        const headers: HeadersInit = this.getRequestResourceObjectHeader(id);
        const data = await HttpService.getJson(MessengerService.CHAT_URL + `/${id}?userId=${userId}`, undefined, headers);
        if (data == null) return null;
        let lastReadMsg: Message | null;
        if (data.lastReadMsg) {
            lastReadMsg = new Message(data.lastReadMsg.id);
            lastReadMsg.time = new Date(data.lastReadMsg.time);
        } else {
            lastReadMsg = null;
        }
        const chat = new Chat(
            data.id,
            data.name,
            data.private,
            data.avatar,
            data.time,
            data.participants,
            data.lastMessage,
            data.draft,
            data.unreadMsgCount,
            lastReadMsg,
            data.authorId,
            data.canAddUsers);
        return chat;
    }

    async getChatPermissions(chatId?: ID): Promise<any> {
        const headers: HeadersInit = this.getRequestResourceObjectHeader(chatId);
        return HttpService.getJson(MessengerService.CHAT_URL + `/${chatId}/users/permissions`, undefined, headers);
    }

    async getChatsByUser(dto: ChatRequestDTO): Promise<any> {
        const headers: HeadersInit = this.getRequestResourceObjectHeader(dto.chatId);
        const rawChats: Chat[] = await HttpService.postJson(MessengerService.CHAT_URL + '/', dto, headers);
        return rawChats?.map((chat, index) => {
            const image = this.randomIntFromInterval(80, 120);
            return new Chat(
                chat.id,
                chat.name,
                chat.private,
                chat.avatar || (RANDOM_CHAT_AVATAR_URL + `${image}/${image}`),
                chat.time,
                chat.participants,
                chat.lastMessage == null ? null :
                    new Message(
                        chat.lastMessage.id,
                        chat.lastMessage.chatId,
                        chat.lastMessage.receiverId,
                        chat.lastMessage.type,
                        chat.lastMessage.data,
                        chat.lastMessage.author,
                        chat.lastMessage.time),
                chat.draft,
                chat.unreadMsgCount
            );
        }
        )
    }

    async getChatsByUserLight(dto: ChatRequestDTO): Promise<any> {
        const headers: HeadersInit = this.getRequestResourceObjectHeader(dto.chatId);
        const rawChats: Chat[] = await HttpService.postJson(MessengerService.CHAT_URL + '/light-list', dto, headers);
        return rawChats?.map((chat, index) => {
            const c = new Chat(chat.id);
            c.private = chat.private;
            c.draft = chat.draft
            return c
        }
        )
    }

    /**
     * Creates new chat or returns existing
     */
    async createChat(chat: Chat): Promise<any> {
        const headers: HeadersInit = this.getRequestResourceObjectHeader(null);
        return await HttpService.postJson(MessengerService.CHAT_URL + '/create', chat, headers);
    }

    async deleteChat(id: string | number): Promise<void> {
        const headers: HeadersInit = this.getRequestResourceObjectHeader(id);
        await HttpService.delete(MessengerService.CHAT_URL + `/${id}`, undefined, headers);
    }

    async updateChat(chat: Chat): Promise<void> {
        const headers: HeadersInit = this.getRequestResourceObjectHeader(chat.id);
        await HttpService.putJson(MessengerService.CHAT_URL + `/${chat.id}`, chat, headers);
    }

    async updateChatPatch(chat: Chat): Promise<void> {
        const headers: HeadersInit = this.getRequestResourceObjectHeader(chat.id);
        await HttpService.patchJson(MessengerService.CHAT_URL + `/${chat.id}`, chat, headers);
    }

    async updateLastReadMsg(
        chatId: string | number,
        userId: number | string,
        messageId: number | string): Promise<any> {
        const headers: HeadersInit = this.getRequestResourceObjectHeader(chatId);
        const dto = {
            chatId,
            userId,
            messageId
        };
        return HttpService.patchJson(MessengerService.CHAT_URL + `/${chatId}/last-read-msg`, dto, headers);
    }

    async getMessages(dto: MessageRequestDTO): Promise<Message[]> {
        const headers: HeadersInit = this.getRequestResourceObjectHeader(dto.chatId);
        const messages: Message[] = await HttpService.postJson(
            MessengerService.CHAT_URL + `/${dto.chatId}/messages`, dto, headers);
        return messages?.map((message: Message, index) => {
            const repliedMessage = message.replyTo && message.repliedMessage ?
            new Message(
                message.repliedMessage.id,
                message.repliedMessage.chatId,
                message.repliedMessage.receiverId,
                message.repliedMessage.type,
                message.repliedMessage.data,
                message.repliedMessage.author,
                message.repliedMessage.time,
                message.repliedMessage.read)
            : null;
            return new Message(
                message.id,
                message.chatId,
                message.receiverId,
                message.type,
                message.data,
                message.author,
                message.time,
                message.read,
                message.replyTo,
                repliedMessage
            );
        })
    }

    async sendMessage(message: Message): Promise<any> {
        const headers: HeadersInit = this.getRequestResourceObjectHeader(message.chatId);
        return HttpService.postJson(MessengerService.CHAT_URL + `/${message.chatId}/messages/create`, message, headers);
    }

    async addUsersToChat(chatId?: number | string | null, users?: ID[] | null): Promise<any> {
        const headers: HeadersInit = this.getRequestResourceObjectHeader(chatId);
        const dto = {
            chatId: chatId,
            users: users
        }
        return HttpService.postJson(MessengerService.CHAT_URL + `/add`, dto, headers);
    }

    async deleteUsersFromChat(chatId?: number | string | null, users?: ID[] | null): Promise<any> {
        const headers: HeadersInit = this.getRequestResourceObjectHeader(chatId);
        return HttpService.delete(MessengerService.CHAT_URL + `/${chatId}/users?userId=${users}`, undefined, headers);
    }

    async sendAttachment(attachment: File, chatId?: ID, userId?: ID): Promise<any> {
        const headers: HeadersInit = this.getRequestResourceObjectHeader(chatId);
        const dto = {
            file: attachment,
            chatId,
            userId
        };
        return HttpService.postFile(MessengerService.CHAT_URL + '/attachment', dto, headers);
    }

    async generateChatInvitationLink(chatId: ID): Promise<string> {
        const headers: HeadersInit = this.getRequestResourceObjectHeader(chatId);
        return HttpService.get(MessengerService.CHAT_URL + `/${chatId}/invitation-link`, undefined, headers);
    }

    async joinChatByLink(chatId: ID, link: string): Promise<any> {
        const headers: HeadersInit = this.getRequestResourceObjectHeader(chatId);
        return HttpService.postByUrl(link, undefined, headers);
    }
}
export default MessengerService.Instance;