import HttpService from "./HttpService";
import User from "../models/User";
import { ID } from "../util/Types";

export interface UserFilter {
    /**
     * Includes name or/and surname of the user
     */
    search?: string | null;
    /**
     * A chat to take/exclude users from
     */
    chatId?: ID;
    /**
     * Whether to exclude users who are in chat with chatId from the search
     */
    exclude?: boolean;
    /**
     * Array of user identifiers
     */
    ids?: ID[];
}

export interface INavigation {
    pageToken?: number | string | null;
    maxPageSize?: number;
    direction?: number | null;
    cursor?: ID | null;
}

export interface UserRequestDTO {
    filter?: UserFilter,
    id?: string | number | null,
    surname?: string | null,
    direction?: number | null;
    sorting?: object | null;
    navigation?: INavigation | null;
}

class UserService {
    private static _instance: UserService;

    private constructor() {

    }

    static get Instance() {
        // Do you need arguments? Make it a regular static method instead.
        return this._instance || (this._instance = new this());      
    }

    async get(id: number | string): Promise<User|null> {
        const data: any = (await HttpService.getJson(`/users/${id}`));
        if (data == null) return null;
        // Store user somewhere to retrieve in the app
        return new User(data.id, data.name, data.surname, data.login, data.avatar,
            data.password, data.status, typeof data.lastVisited === 'string' ? new Date(data.lastVisited) : data.lastVisited);
    }

    async create(user: object): Promise<any> {
        return HttpService.postJson(`/users`, user);
    }

    async update(user?: User | null): Promise<any> {
        if (user == null) return;
        return HttpService.putJson(`/users`, user);
    }

    async delete(id: number | string): Promise<any> {
        return HttpService.delete(`/users/${id}`);
    }

    async list(dto: UserRequestDTO, controller?: AbortController | null): Promise<User[]> {
        const rawUsers = await HttpService.postJson(`/users/`, dto, undefined, controller?.signal);
        return rawUsers.map((user: any) => new User(user.id, user.name, user.surname, user.login,
            user.avatar, user.password, user.status,
            typeof user.lastVisited === 'string' ? new Date(user.lastVisited) : user.lastVisited));
    }

    async setAvatar(id: number | string, image: File): Promise<any> {
        const data = {
            userId: id,
            file: image
        }
        return HttpService.postFile('/users/avatar', data);
    }
}

export default UserService.Instance;