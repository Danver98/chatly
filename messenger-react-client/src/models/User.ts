import { NetworkStatus } from "../util/Constants";

interface IUser {
    id: number | string;
    name?: string;
    surname?: string;
    login?: string;
    avatar?: string;
    password?: string;
    networkStatus?: typeof NetworkStatus[keyof typeof NetworkStatus] | null;
    lastVisited?: Date | null;
}

class User implements IUser {
    id: number | string;
    name?: string;
    surname?: string;
    login?: string;
    avatar?: string;
    password?: string;
    networkStatus?: typeof NetworkStatus[keyof typeof NetworkStatus] | null;
    lastVisited?: Date | null;

    public static copy(user: User) {
        return new User(
            user.id,
            user.name,
            user.surname,
            user.login,
            user.avatar,
            user.password,
            user.networkStatus,
            user.lastVisited
        );
    };

    constructor(id: number | string, name?: string, surname?: string, login?: string, avatar?: string, password?: string,
         networkStatus?: typeof NetworkStatus[keyof typeof NetworkStatus] | null, lastVisited?: Date | null
    ) {

        this.id = id;
        this.name = name;
        this.surname = surname;
        this.login = login;
        this.avatar = avatar;
        this.password = password;
        this.networkStatus = networkStatus;
        this.lastVisited = lastVisited;
    }

    getFullName() {
        return `${this.name} ${this.surname}`
    }
}

export default User;